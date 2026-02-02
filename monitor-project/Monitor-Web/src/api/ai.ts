/**
 * AI助手相关API
 */

import request from '@/utils/request'

/**
 * 聊天消息
 */
export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: number
}

/**
 * 发送聊天消息
 */
export function sendChatMessage(message: string, sessionId?: string): Promise<{ reply: string; sessionId: string }> {
  return request.post<{ reply: string; sessionId: string }>('/ai/chat', { message, sessionId })
}

/**
 * 获取聊天历史
 */
export function getChatHistory(sessionId: string): Promise<ChatMessage[]> {
  return request.get<ChatMessage[]>(`/ai/chat/${sessionId}/history`)
}

/**
 * 创建新聊天会话
 */
export function createChatSession(): Promise<{ sessionId: string }> {
  return request.post<{ sessionId: string }>('/ai/sessions')
}

/**
 * 获取会话列表
 */
export function getChatSessions(): Promise<
  Array<{ sessionId: string; title: string; createdAt: string; updatedAt: string }>
> {
  return request.get('/ai/sessions')
}

/**
 * 删除聊天会话
 */
export function deleteChatSession(sessionId: string): Promise<void> {
  return request.delete<void>(`/ai/sessions/${sessionId}`)
}

/**
 * 获取快捷指令模板
 */
export function getQuickActions(): Promise<
  Array<{ id: string; title: string; template: string; category: string }>
> {
  return request.get('/ai/quick-actions')
}
