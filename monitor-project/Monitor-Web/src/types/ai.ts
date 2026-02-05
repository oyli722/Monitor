/**
 * AI助手相关类型定义
 */

/**
 * 聊天消息角色
 */
export type ChatRole = 'user' | 'assistant' | 'system'

/**
 * 聊天消息
 */
export interface ChatMessage {
  id: string
  role: ChatRole
  content: string
  timestamp: number
  isStreaming?: boolean
}

/**
 * 聊天会话
 */
export interface ChatSession {
  sessionId: string
  messages: ChatMessage[]
  createdAt: number
  updatedAt: number
}

/**
 * WebSocket消息类型
 */
export type WsMessageType =
  | 'chat'           // 客户端发送用户消息
  | 'reply'          // 服务器返回AI回复
  | 'command_output' // 命令实时输出
  | 'command_complete' // 命令执行完成
  | 'error'          // 错误通知
  | 'ping'           // 心跳

/**
 * WebSocket聊天消息（客户端→服务器）
 */
export interface WsChatMessage {
  type: 'chat'
  content: string
  timestamp?: number
}

/**
 * WebSocket回复消息（服务器→客户端）
 */
export interface WsReplyMessage {
  type: 'reply'
  content: string
  timestamp: number
  isComplete: boolean
}

/**
 * WebSocket命令输出消息（服务器→客户端）
 */
export interface WsCommandOutputMessage {
  type: 'command_output'
  content: string
  timestamp: number
}

/**
 * WebSocket命令完成消息（服务器→客户端）
 */
export interface WsCommandCompleteMessage {
  type: 'command_complete'
  exitCode: number
  timestamp: number
}

/**
 * WebSocket错误消息（服务器→客户端）
 */
export interface WsErrorMessage {
  type: 'error'
  errorCode: string
  message: string
  timestamp: number
}

/**
 * WebSocket心跳消息
 */
export interface WsPingMessage {
  type: 'ping'
  timestamp?: number
}

/**
 * 服务器→客户端的所有消息类型联合
 */
export type WsServerMessage =
  | WsReplyMessage
  | WsCommandOutputMessage
  | WsCommandCompleteMessage
  | WsErrorMessage
  | WsPingMessage

/**
 * 客户端→服务器的所有消息类型联合
 */
export type WsClientMessage = WsChatMessage | WsPingMessage

/**
 * WebSocket消息（双向）
 */
export type WsMessage = WsClientMessage | WsServerMessage

/**
 * AI助手连接请求
 */
export interface ConnectAIRequest {
  sshSessionId: string
  agentId: string
}

/**
 * AI助手连接响应
 */
export interface ConnectAIResponse {
  aiSessionId: string
  message: string
}

/**
 * SSH会话绑定信息
 */
export interface SshSessionBinding {
  aiSessionId: string
  sshSessionId: string
  agentId: string
  connectedAt: number
  lastActivityAt: number
}

/**
 * WebSocket连接状态
 */
export type WebSocketStatus =
  | 'connecting'  // 连接中
  | 'connected'   // 已连接
  | 'disconnected' // 已断开
  | 'error'       // 连接错误

/**
 * AI助手状态
 */
export interface AIAssistantState {
  isConnected: boolean
  isConnecting: boolean
  currentSession: ChatSession | null
  wsStatus: WebSocketStatus
  error: string | null
}

// ==================== 侧边栏AI助手类型 ====================

/**
 * 会话信息
 */
export interface SessionInfo {
  sessionId: string
  title: string
  createdAt: number
  updatedAt: number
  messageCount: number
  linkedAgentId?: string
}

/**
 * 创建会话请求
 */
export interface CreateSessionRequest {
  firstMessage: string
  agentId?: string
}

/**
 * 创建会话响应
 */
export interface CreateSessionResponse {
  sessionId: string
  title: string
}

/**
 * 发送消息请求
 */
export interface SendMessageRequest {
  sessionId: string
  message: string
  modelName?: string
}

/**
 * 聊天响应
 */
export interface ChatResponse {
  sessionId: string
  reply: string
  message: ChatMessageResponse
}

/**
 * 聊天消息响应（后端返回）
 */
export interface ChatMessageResponse {
  role: ChatRole
  content: string
  timestamp: number
}

/**
 * API基础响应
 */
export interface BaseResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}
