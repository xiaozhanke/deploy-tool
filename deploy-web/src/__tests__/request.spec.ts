import AxiosMockAdapter from 'axios-mock-adapter'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// 真实的 request.ts 通过 unplugin-auto-import 在编译期注入 ElNotification / ElLoading，
// vitest 跑的是源码不会经过那一层，所以这里把它们注册到 globalThis 上，让模块加载阶段不抛 ReferenceError
const elLoadingClose = vi.fn()
const elLoadingService = vi.fn(() => ({ close: elLoadingClose }))
const elNotificationError = vi.fn()
vi.stubGlobal('ElLoading', { service: elLoadingService })
vi.stubGlobal('ElNotification', { error: elNotificationError })

const handleSessionExpired = vi.fn().mockResolvedValue(undefined)
vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    accessToken: '',
    sessionAbortController: null,
    handleSessionExpired,
  }),
}))

vi.mock('@/utils/nprogress', () => ({
  default: { start: vi.fn(), done: vi.fn() },
}))

describe('request interceptor 401 处理', () => {
  let mockAdapter: AxiosMockAdapter
  let instance: import('axios').AxiosInstance

  beforeEach(async () => {
    setActivePinia(createPinia())
    vi.resetModules()
    handleSessionExpired.mockClear()
    elLoadingClose.mockClear()
    elLoadingService.mockClear()
    // dynamic import 让 vi.mock 与 stubGlobal 在请求模块解析前生效
    const mod = await import('@/api/request')
    instance = mod.default
    mockAdapter = new AxiosMockAdapter(instance)
  })

  afterEach(() => {
    mockAdapter.restore()
  })

  it('在收到 401 时应让调用方 catch 到拒绝，而不是永久挂起 loading 计数', async () => {
    mockAdapter.onGet('/api/anywhere').reply(401, {
      error: { status: 'UNAUTHENTICATED', message: '会话已失效' },
    })

    await expect(instance.get('/api/anywhere')).rejects.toMatchObject({
      code: 401,
      status: 'UNAUTHENTICATED',
    })
    expect(handleSessionExpired).toHaveBeenCalledOnce()
    // 全局 loading 已经打开过一次（请求开始），且应该被关闭
    expect(elLoadingService).toHaveBeenCalled()
    expect(elLoadingClose).toHaveBeenCalled()
  })

  it('调用方的 finally 能在 401 之后执行', async () => {
    mockAdapter.onGet('/api/another').reply(401)

    const finallyHandler = vi.fn()
    await expect(instance.get('/api/another').finally(finallyHandler)).rejects.toBeDefined()
    expect(finallyHandler).toHaveBeenCalledOnce()
  })
})
