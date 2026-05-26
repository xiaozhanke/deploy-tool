import { Client, type IMessage, StompHeaders } from '@stomp/stompjs'
import { defineStore } from 'pinia'
import { useAuthStore } from './auth'

interface WebSocketState {
  client: Client | null
  errorHandler: ((error: object) => void) | null
}

export const useWebSocketStore = defineStore('websocket', {
  state: (): WebSocketState => ({
    client: null,
    errorHandler: null,
  }),

  actions: {
    /**
     * 设置错误处理回调
     * @param handler 错误处理函数
     */
    setErrorHandler(handler: (error: object) => void) {
      this.errorHandler = handler
    },

    /**
     * 清除错误处理回调
     */
    clearErrorHandler() {
      this.errorHandler = null
    },

    /**
     * 初始化 WebSocket 连接
     * @param brokerURL WebSocket 服务器地址 (e.g. 'ws://localhost:60606/websocket')
     * @param headers 连接头信息
     */
    connect(brokerURL: string, headers?: StompHeaders): Promise<void> {
      return new Promise((resolve, reject) => {
        this.disconnect()
          .then(() => {
            // 自动添加认证 token, 并合并 headers
            const authStore = useAuthStore()
            const token = authStore.accessToken
            const connectHeaders: StompHeaders = {
              Authorization: `Bearer ${token}`,
              ...headers,
            }

            this.client = new Client({
              brokerURL,
              connectHeaders,
              reconnectDelay: 5000,
              heartbeatIncoming: 4000,
              heartbeatOutgoing: 4000,
              connectionTimeout: 5000,
              debug: (str) => console.debug('[STOMP]', str),
              onConnect: () => {
                // 订阅用户异常频道
                this.client?.subscribe('/user/queue/errors', (message: IMessage) => {
                  const error = JSON.parse(message.body)
                  ElNotification.error({
                    title: 'WebSocket 异常',
                    message: `${error.message}`,
                  })
                  if (this.errorHandler) {
                    this.errorHandler(error)
                  }
                })
                resolve()
              },
              onStompError: (frame) => {
                const errorMessage = frame.headers?.message || 'STOMP协议错误'
                if (this.errorHandler) {
                  this.errorHandler({ message: errorMessage })
                } else {
                  ElNotification.error({
                    title: 'WebSocket 连接异常',
                    message: errorMessage,
                  })
                }
                reject(new Error(errorMessage))
              },
            })

            this.client.activate()
          })
          .catch(reject)
      })
    },

    /**
     * 断开连接
     */
    async disconnect() {
      if (this.client?.active) {
        await this.client.deactivate()
      }
      this.client = null
    },

    /**
     * 订阅频道
     * @param destination 频道地址 (e.g. '/topic/notifications')
     * @param callback 消息处理器
     * @param onSubscribed 可选，订阅完成的回调
     */
    subscribe(destination: string, callback: (message: string) => void, onSubscribed?: () => void) {
      if (!this.client?.connected) {
        throw new Error('WebSocket 未连接')
      }

      const subscription = this.client.subscribe(destination, (message: IMessage) => {
        try {
          callback(message.body)
        } catch (error) {
          ElNotification.error('消息解析错误: ' + extractErrorMessage(error))
        }
      })

      if (onSubscribed) {
        onSubscribed()
      }

      return subscription
    },

    /**
     * 发送消息
     * @param destination 目标地址 (e.g. '/app/chat')
     * @param body 消息内容
     * @param headers 附加头信息
     */
    send<T = object>(destination: string, body: T, headers: StompHeaders = {}) {
      if (!this.client?.connected) {
        throw new Error('WebSocket 未连接')
      }

      this.client.publish({
        destination,
        body: JSON.stringify(body),
        headers: {
          'content-type': 'application/json',
          ...headers,
        },
      })
    },
  },
})
