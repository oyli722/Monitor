/**
 * WebSocket相关组合式函数
 */

import { ref, onUnmounted } from 'vue'

export type WebSocketMessageHandler = (data: any) => void

export function useWebSocket(url: string) {
  const ws = ref<WebSocket | null>(null)
  const connected = ref<boolean>(false)
  const error = ref<Error | null>(null)
  const messageHandlers = ref<Map<string, WebSocketMessageHandler>>(new Map())

  // 连接WebSocket
  function connect() {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      return
    }

    try {
      ws.value = new WebSocket(url)

      ws.value.onopen = () => {
        connected.value = true
        error.value = null
      }

      ws.value.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          const { type, payload } = data

          // 调用对应类型的处理器
          const handler = messageHandlers.value.get(type)
          if (handler) {
            handler(payload)
          }
        } catch (e) {
          console.error('解析WebSocket消息失败:', e)
        }
      }

      ws.value.onerror = (err) => {
        error.value = err as Error
        connected.value = false
      }

      ws.value.onclose = () => {
        connected.value = false
        // 自动重连（可选）
        // setTimeout(connect, 5000)
      }
    } catch (err) {
      error.value = err as Error
      connected.value = false
    }
  }

  // 断开连接
  function disconnect() {
    if (ws.value) {
      ws.value.close()
      ws.value = null
      connected.value = false
    }
  }

  // 发送消息
  function send(type: string, payload: any) {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify({ type, payload }))
    } else {
      console.warn('WebSocket未连接，无法发送消息')
    }
  }

  // 注册消息处理器
  function onMessage(type: string, handler: WebSocketMessageHandler) {
    messageHandlers.value.set(type, handler)
  }

  // 移除消息处理器
  function offMessage(type: string) {
    messageHandlers.value.delete(type)
  }

  // 组件卸载时断开连接
  onUnmounted(() => {
    disconnect()
  })

  return {
    connected,
    error,
    connect,
    disconnect,
    send,
    onMessage,
    offMessage,
  }
}

/**
 * 监控数据WebSocket连接
 */
export function useMonitorWebSocket() {
  const wsUrl = import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8080/ws'
  const ws = useWebSocket(wsUrl)

  // 连接监控WebSocket
  function connectMonitorWebSocket() {
    ws.connect()
  }

  return {
    ...ws,
    connectMonitorWebSocket,
  }
}
