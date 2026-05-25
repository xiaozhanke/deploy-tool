import router from '@/router'
import { defineStore } from 'pinia'
import { ElMessageBox, ElNotification } from 'element-plus'
import { useWebSocketStore } from './websocket'
import type { User } from 'oidc-client-ts'
import { oidcService } from '@/services/oidcService'
import type { LoginRequest, UserProfile } from '@/types/auth'
import { authLogin, authLogout, authUserCurrent } from '@/api/api'

interface AuthStore {
  oidcUser: User | null
  userProfile: UserProfile | null
  isLoading: boolean
  oidcEventsInitialized: boolean
  sessionAbortController: AbortController | null
}

const getWebSocketUrl = () => {
  if (import.meta.env.VITE_WEBSOCKET_URL) {
    return import.meta.env.VITE_WEBSOCKET_URL
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host
  return `${protocol}//${host}/websocket`
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthStore => ({
    oidcUser: null,
    userProfile: null,
    isLoading: true, // 应用启动时，默认为加载中
    oidcEventsInitialized: false, // oidc 事件已初始化
    sessionAbortController: null, // 会话中止控制器
  }),

  getters: {
    // !!state.user 检查用户对象是否存在
    // !state.user.expired 检查 token 是否过期
    isAuthenticated: (state) => !!state.oidcUser && !state.oidcUser.expired,
    // 直接从 user.profile 中获取用户信息
    // userProfile: (state) => state.oidcUser?.profile,
    // 获取 access token，用于 API 请求
    accessToken: (state) => state.oidcUser?.access_token,
    userAuthorities: (state): string[] => {
      // 你可以根据后端返回的 claim 自定义权限字段，比如 'authorities'
      // 用 Array.isArray 守卫替代 `as string[]` 强转，避免后端意外返回字符串 / 对象时整个 store 取值崩溃
      const claim = state.oidcUser?.profile?.authorities
      return Array.isArray(claim) ? claim.filter((item): item is string => typeof item === 'string') : []
    },
    profile: (state): UserProfile | null => state.userProfile,
  },

  actions: {
    // 内部 action，用于处理用户成功加载后的所有逻辑
    async handleUserLoaded(user: User | null) {
      // 重置会话中止控制器
      this.sessionAbortController = null
      this.oidcUser = user
      if (user && !user.expired) {
        await this.fetchUserProfile()
        const websocketStore = useWebSocketStore()
        // 每次 userLoaded（含初次登录与静默续签）都重连：connect 内部先 disconnect 再用新 token 重新握手，
        // 避免续签后服务器收到带过期 Authorization 的 STOMP 帧
        await websocketStore.connect(getWebSocketUrl())
      }
    },

    // 内部 action，用于处理用户登出或会话失效后的所有清理逻辑
    async handleUserUnloaded() {
      const websocketStore = useWebSocketStore()
      await websocketStore.disconnect()
      this.oidcUser = null
      this.userProfile = null
    },

    // 初始化 OIDC 事件监听器
    initOidcEvents() {
      if (this.oidcEventsInitialized) return
      this.oidcEventsInitialized = true

      // 静默刷新或通过回调获取到新用户时触发
      oidcService.events.addUserLoaded(async (user) => {
        console.log('OIDC 事件：用户加载完成')
        await this.handleUserLoaded(user)
      })

      // 用户登出或会话丢失时触发
      oidcService.events.addUserUnloaded(async () => {
        console.log('OIDC 事件：用户卸载')
        await this.handleUserUnloaded()
      })

      // access token 过期且静默刷新失败时触发
      oidcService.events.addAccessTokenExpired(async () => {
        console.error('访问令牌已过期，无法续订。正在注销。')
        await this.logout()
      })
    },

    async fetchUserProfile() {
      if (!this.isAuthenticated) {
        this.userProfile = null
        return
      }
      try {
        const user = await authUserCurrent()
        this.userProfile = user
      } catch (error) {
        ElNotification.error('获取用户信息失败:' + String(error))
        this.userProfile = null
      }
    },

    // 应用加载时，从 authService 加载用户信息
    async loadUser() {
      this.initOidcEvents()
      this.isLoading = true
      try {
        const user = await oidcService.getUser()
        await this.handleUserLoaded(user)
      } catch (error) {
        console.error('加载用户信息失败:', error)
        await this.handleUserUnloaded()
      } finally {
        this.isLoading = false
      }
    },

    // 用户名密码登录
    async passwordLogin(params: LoginRequest): Promise<void> {
      // 重置会话中止控制器
      this.sessionAbortController = null
      // 调用后端登录接口
      await authLogin(params)
      // 成功后，后端会在浏览器中设置会话 Cookie
    },

    // 触发 OAuth2 授权码流程
    async oauth2Authorize() {
      // 重置会话中止控制器
      this.sessionAbortController = null
      // 在重定向前保存目标路径
      const redirectPath = router.currentRoute.value.query.fullPath
      await oidcService.handleAuthorizationRedirect({ state: { redirectPath } })
    },

    // 登出
    async logout() {
      // 重置会话中止控制器
      this.sessionAbortController = null
      try {
        // 先调用后端的登出API，让会话失效
        await authLogout()
      } catch {
      } finally {
        // 清理本地状态
        await this.handleUserUnloaded()
        // 触发 OIDC 的登出重定向
        await oidcService.handleLogoutRedirect()
      }
    },

    // OAuth2 授权回调处理
    async handleOAuth2Callback() {
      try {
        const user = await oidcService.handleAuthorizationCallback()
        if (user && !user.expired) {
          // user.state 是 oidcAuthorize 写入的回调态，类型不可信，先 narrow 再读 redirectPath
          const state =
            user.state && typeof user.state === 'object' && 'redirectPath' in user.state
              ? (user.state as { redirectPath?: unknown })
              : null
          const redirectPath = typeof state?.redirectPath === 'string' ? state.redirectPath : '/'
          await router.push(redirectPath)
        } else {
          throw new Error('登录成功，但获取到的用户信息无效或已过期。')
        }
      } catch (error) {
        console.error('登录回调处理失败:', error)
        await this.handleUserUnloaded()
        await router.push('/login')
      }
    },

    // 会话过期处理
    async handleSessionExpired() {
      if (this.sessionAbortController) {
        console.warn('会话过期处理已在进行中，忽略重复调用。')
        return
      }
      // 创建会话中止控制器并中止后续请求
      this.sessionAbortController = new AbortController()
      this.sessionAbortController.abort('由于会话过期，请求被取消。')
      // 清理用户
      await this.handleUserUnloaded()
      await oidcService.removeUser()
      await ElMessageBox.alert('未授权或登录已过期，请重新登录', '认证失败', {
        confirmButtonText: '确定',
      })
      // 重定向到登录页
      await router.push('/login')
    },
  },
})
