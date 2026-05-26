import axios from 'axios'
import NProgress from '@/utils/nprogress'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/types/error'

// 拓展 AxiosRequestConfig 类型
declare module 'axios' {
  interface AxiosRequestConfig {
    showLoading?: boolean
    noAuth?: boolean
  }
}

const instance = axios.create({
  timeout: 30000,
  withCredentials: true,
  baseURL: import.meta.env.VITE_API_ROOT,
})

// 加载状态变量
let loadingInstance: ReturnType<typeof ElLoading.service>
let activeRequests = 0

// 请求拦截器
instance.interceptors.request.use(
  (config) => {
    // 开始进度条
    NProgress.start()
    // 显示 loading
    if (config.showLoading !== false) {
      activeRequests++
      if (activeRequests === 1) {
        loadingInstance = ElLoading.service({
          lock: true,
          fullscreen: true,
          text: '加载中...',
          background: 'rgba(0, 0, 0, 0.7)',
        })
      }
    }
    const authStore = useAuthStore()
    if (authStore.sessionAbortController) {
      // 将中止信号附加到请求上
      config.signal = authStore.sessionAbortController.signal
    }
    if (config.noAuth !== true) {
      const token = authStore.accessToken
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
    }
    return config
  },
  (error) => {
    // 结束进度条
    NProgress.done()
    if (error.config?.showLoading !== false) {
      activeRequests--
      if (activeRequests <= 0) {
        activeRequests = 0
        loadingInstance?.close()
      }
    }
    return Promise.reject(error instanceof Error ? error : new Error(String(error)))
  },
)

// 响应拦截器
instance.interceptors.response.use(
  (response) => {
    // 结束进度条
    NProgress.done()
    // 关闭 loading
    if (response.config.showLoading !== false) {
      activeRequests--
      if (activeRequests <= 0) {
        activeRequests = 0
        loadingInstance?.close()
      }
    }
    // 如果是下载文件的响应，直接返回
    if (response.config.responseType === 'blob') {
      return response
    }
    return response.data
  },
  async (error) => {
    // 结束进度条
    NProgress.done()
    // 关闭 loading
    if (error.config?.showLoading !== false) {
      activeRequests--
      if (activeRequests <= 0) {
        activeRequests = 0
        loadingInstance?.close()
      }
    }

    // 优先处理中止错误
    if (axios.isCancel(error)) {
      return Promise.reject(error)
    }

    // 异常信息对象，用于承载处理过的异常信息
    const errorInfo = {
      // HTTP 状态码
      code: 0,
      // 业务状态码
      status: 'UNKNOWN_ERROR',
      // 用于通知的标题
      title: '请求异常',
      // 用于通知和业务代码 catch 的消息
      message: '发生未知错误',
    }

    if (error.response) {
      // 服务器返回了响应，但 HTTP 状态码非 2xx
      const { status: httpStatusCode, data: { error: restError = {} } = {} } = error.response
      const { status: backendStatus, message: backendMessage } = restError
      errorInfo.code = httpStatusCode
      errorInfo.status = backendStatus ?? 'HTTP_ERROR'
      // 根据 HTTP 状态码进行异常处理
      switch (httpStatusCode) {
        case 400:
          errorInfo.title = '参数错误'
          errorInfo.message = backendMessage || '请求参数错误'
          break
        case 401:
          // 401 触发 session 失效流程后，仍走标准 reject 让调用方的 .catch 与 finally 执行完毕；
          // 之前用 new Promise(() => {}) 永不结算阻塞了外部组件自己的 loading 计数器。
          // sessionAbortController 已经在 handleSessionExpired 内取消了后续请求。
          errorInfo.title = '登录失效'
          errorInfo.message = backendMessage || '登录状态已失效，请重新登录'
          await useAuthStore().handleSessionExpired()
          return Promise.reject(new ApiError(errorInfo.code, errorInfo.status, errorInfo.message))
        case 403:
          errorInfo.title = '禁止访问'
          errorInfo.message = backendMessage || '您没有权限执行此操作'
          break
        case 404:
          errorInfo.title = '资源未找到'
          errorInfo.message = backendMessage || '请求的资源未找到'
          break
        case 500:
          errorInfo.title = '服务器错误'
          errorInfo.message = backendMessage || '服务器内部错误，请联系管理员'
          break
        default:
          errorInfo.title = '请求错误'
          errorInfo.message = backendMessage || `HTTP 请求错误，状态码: ${httpStatusCode}`
      }
    } else if (error.request) {
      // 请求已发出，但没有收到响应 (网络错误)
      errorInfo.status = error.code || 'NETWORK_ERROR'
      errorInfo.title = '网络异常'
      switch (error.code) {
        case 'ECONNABORTED':
          errorInfo.title = '请求超时'
          errorInfo.message = '请求超时，请检查您的网络或联系管理员'
          break
        case 'ERR_NETWORK':
          errorInfo.message = '网络连接中断，请检查您的网络设置'
          if (error.message && error.message.toLowerCase().includes('cors')) {
            errorInfo.message += ' (可能存在跨域问题)'
          }
          break
        case 'ECONNREFUSED':
          errorInfo.title = '连接被拒绝'
          errorInfo.message = '无法连接到服务器，请确认服务是否正常运行'
          break
        default:
          errorInfo.message = '无法发送请求，请检查您的网络连接'
      }
    } else {
      // 请求在准备阶段就失败了 (例如，配置错误)
      errorInfo.status = 'SETUP_ERROR'
      errorInfo.title = '请求设置错误'
      errorInfo.message = error.message
    }

    // 全局异常通知
    ElNotification.error({
      title: errorInfo.title,
      message: errorInfo.message,
    })

    // 将处理过的异常信息封装成 ApiError 抛出，供业务代码 catch 使用
    return Promise.reject(new ApiError(errorInfo.code, errorInfo.status, errorInfo.message))
  },
)

export default instance
