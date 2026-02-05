/**
 * AI助手聊天 Composable
 * 处理WebSocket连接和消息通信
 */

import { ref, computed } from 'vue'
import type {
  ChatMessage,
  WsMessage,
  WsServerMessage,
  WsClientMessage,
  WebSocketStatus
} from '@/types/ai'

interface UseAiChatOptions {
  aiSessionId: string
  onMessage?: (message: ChatMessage) => void
  onCommandOutput?: (output: string) => void
  onCommandComplete?: (exitCode: number) => void
  onError?: (error: string) => void
  onStatusChange?: (status: WebSocketStatus) => void
}

interface WebSocketResponse {
  data: string
}

export function useAiChat(options: UseAiChatOptions) {
  const { aiSessionId, onMessage, onCommandOutput, onCommandComplete, onError, onStatusChange } = options

  // WebSocket实例
  let ws: WebSocket | null = null

  // 连接状态
  const status = ref<WebSocketStatus>('disconnected')
  const isConnected = computed(() => status.value === 'connected')
  const isConnecting = computed(() => status.value === 'connecting')

  // 心跳定时器
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null

  // 重连配置
  const maxReconnectAttempts = 3
  let reconnectAttempts = 0
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null

  /**
   * 生成消息ID
   */
  const generateMessageId = (): string => {
    return `msg_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`
  }

  /**
   * 构建WebSocket URL
   */
  const buildWsUrl = (sessionId: string): string => {
    // 使用环境变量配置的WebSocket地址
    const wsBaseUrl = import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8080'
    return `${wsBaseUrl}/ws/ai/ssh-assistant/${sessionId}`
  }

  /**
   * 更新连接状态
   */
  const updateStatus = (newStatus: WebSocketStatus) => {
    status.value = newStatus
    onStatusChange?.(newStatus)
  }

  /**
   * 发送WebSocket消息
   */
  const send = (message: WsClientMessage) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      console.log('[useAiChat] 发送消息:', message)
      ws.send(JSON.stringify(message))
      return true
    }
    console.warn('[useAiChat] 发送失败: WebSocket未连接, readyState=', ws?.readyState)
    return false
  }

  /**
   * 发送聊天消息
   */
  const sendChatMessage = (content: string): boolean => {
    const message: WsClientMessage = {
      type: 'chat',
      content,
      timestamp: Date.now()
    }
    return send(message)
  }

  /**
   * 发送心跳
   */
  const sendHeartbeat = () => {
    const message: WsClientMessage = {
      type: 'ping',
      timestamp: Date.now()
    }
    send(message)
  }

  /**
   * 启动心跳
   */
  const startHeartbeat = () => {
    stopHeartbeat()
    heartbeatTimer = setInterval(() => {
      sendHeartbeat()
    }, 30000) // 每30秒发送一次心跳
  }

  /**
   * 停止心跳
   */
  const stopHeartbeat = () => {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  /**
   * 处理服务器消息
   */
  const handleMessage = (event: MessageEvent<string>) => {
    try {
      const data: WsServerMessage = JSON.parse(event.data)
      console.log('[useAiChat] 收到消息:', data.type, data)

      switch (data.type) {
        case 'reply':
          // AI回复消息
          handleReplyMessage(data)
          break

        case 'command_output':
          // 命令输出
          handleCommandOutput(data)
          break

        case 'command_complete':
          // 命令执行完成
          handleCommandComplete(data)
          break

        case 'error':
          // 错误消息
          handleError(data)
          break

        case 'ping':
          // 心跳响应，忽略
          break

        default:
          console.warn('[useAiChat] 未知消息类型:', data)
      }
    } catch (error) {
      console.error('[useAiChat] 解析消息失败:', error)
    }
  }

  /**
   * 处理AI回复消息
   */
  const handleReplyMessage = (data: WsServerMessage & { type: 'reply' }) => {
    const message: ChatMessage = {
      id: generateMessageId(),
      role: 'assistant',
      content: data.content,
      timestamp: data.timestamp,
      isStreaming: !data.isComplete
    }
    onMessage?.(message)
  }

  /**
   * 处理命令输出
   */
  const handleCommandOutput = (data: WsServerMessage & { type: 'command_output' }) => {
    onCommandOutput?.(data.content)
  }

  /**
   * 处理命令完成
   */
  const handleCommandComplete = (data: WsServerMessage & { type: 'command_complete' }) => {
    onCommandComplete?.(data.exitCode)
  }

  /**
   * 处理错误消息
   */
  const handleError = (data: WsServerMessage & { type: 'error' }) => {
    const errorMsg = `[${data.errorCode}] ${data.message}`
    onError?.(errorMsg)

    // 同时添加为系统消息
    const message: ChatMessage = {
      id: generateMessageId(),
      role: 'system',
      content: errorMsg,
      timestamp: data.timestamp
    }
    onMessage?.(message)
  }

  /**
   * 处理WebSocket打开事件
   */
  const handleOpen = () => {
    console.log('[useAiChat] WebSocket连接成功')
    updateStatus('connected')
    reconnectAttempts = 0 // 重置重连计数
    startHeartbeat()
  }

  /**
   * 处理WebSocket关闭事件
   */
  const handleClose = (event: CloseEvent) => {
    console.log('[useAiChat] WebSocket连接关闭, wasClean=', event.wasClean, ', code=', event.code, ', reason=', event.reason)
    updateStatus('disconnected')
    stopHeartbeat()

    // 非正常关闭时尝试重连
    if (!event.wasClean && reconnectAttempts < maxReconnectAttempts) {
      reconnectAttempts++
      const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 10000) // 指数退避

      reconnectTimer = setTimeout(() => {
        console.log(`[useAiChat] 尝试重连 (${reconnectAttempts}/${maxReconnectAttempts})...`)
        connect()
      }, delay)
    }
  }

  /**
   * 处理WebSocket错误事件
   */
  const handleErrorEvent = (event: Event) => {
    console.error('[useAiChat] WebSocket错误事件:', event)
    updateStatus('error')
    onError?.('WebSocket连接发生错误')
  }

  /**
   * 连接WebSocket
   */
  const connect = () => {
    if (ws?.readyState === WebSocket.OPEN) {
      console.warn('[useAiChat] WebSocket已连接')
      return
    }

    updateStatus('connecting')

    try {
      const url = buildWsUrl(aiSessionId)
      console.log('[useAiChat] 正在连接WebSocket:', url)

      ws = new WebSocket(url)

      ws.onopen = handleOpen
      ws.onmessage = handleMessage
      ws.onclose = handleClose
      ws.onerror = handleErrorEvent

      console.log('[useAiChat] WebSocket连接请求已发送, 当前readyState=', ws.readyState)
    } catch (error) {
      updateStatus('error')
      console.error('[useAiChat] 创建WebSocket失败:', error)
      onError?.('创建WebSocket连接失败')
    }
  }

  /**
   * 断开连接
   */
  const disconnect = () => {
    // 清除重连定时器
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }

    // 停止心跳
    stopHeartbeat()

    // 关闭WebSocket
    if (ws) {
      ws.close()
      ws = null
    }

    updateStatus('disconnected')
  }

  /**
   * 重置重连计数（用于手动重连场景）
   */
  const resetReconnectAttempts = () => {
    reconnectAttempts = 0
  }

  // 返回公共接口
  return {
    // 状态
    status,
    isConnected,
    isConnecting,

    // 方法
    connect,
    disconnect,
    sendChatMessage,
    resetReconnectAttempts
  }
}
