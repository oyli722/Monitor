/**
 * AI助手API接口
 */

import request from '@/utils/request'
import type {
  ConnectAIRequest,
  ConnectAIResponse,
  SshSessionBinding,
  CreateSessionRequest,
  CreateSessionResponse,
  SendMessageRequest,
  ChatResponse,
  SessionInfo,
  ChatMessageResponse
} from '@/types/ai'

/**
 * AI助手API类
 */
export class AIAssistantAPI {
  /**
   * 连接AI助手
   * @param data 连接请求参数
   * @returns 连接响应（包含aiSessionId）
   */
  static async connect(data: ConnectAIRequest): Promise<ConnectAIResponse> {
    return request.post<ConnectAIResponse>('/ai/ssh-assistant/connect', data)
  }

  /**
   * 断开AI助手连接
   * @param aiSessionId AI会话ID
   */
  static async disconnect(aiSessionId: string): Promise<void> {
    return request.delete<void>(`/ai/ssh-assistant/disconnect/${aiSessionId}`)
  }

  /**
   * 获取SSH会话绑定信息
   * @param aiSessionId AI会话ID
   * @returns SSH会话绑定信息
   */
  static async getBinding(aiSessionId: string): Promise<SshSessionBinding> {
    return request.get<SshSessionBinding>(`/ai/ssh-assistant/binding/${aiSessionId}`)
  }

  /**
   * 检查会话是否活跃
   * @param aiSessionId AI会话ID
   * @returns 是否活跃
   */
  static async isActive(aiSessionId: string): Promise<boolean> {
    return request.get<{ active: boolean }>(`/ai/ssh-assistant/active/${aiSessionId}`)
      .then(result => result.active)
  }
}

/**
 * 导出便捷函数
 */
export const connectAIAssistant = AIAssistantAPI.connect
export const disconnectAIAssistant = AIAssistantAPI.disconnect
export const getAIAssistantBinding = AIAssistantAPI.getBinding
export const isAIAssistantActive = AIAssistantAPI.isActive

// ==================== 侧边栏AI助手API ====================

/**
 * 侧边栏AI会话API类
 * 复用ChatController接口
 */
export class ChatSessionAPI {
  /**
   * 创建新会话
   * @param data 创建会话请求参数
   * @returns 会话ID和标题
   */
  static async createSession(data: CreateSessionRequest): Promise<CreateSessionResponse> {
    return request.post<CreateSessionResponse>('/chat/sessions', data)
  }

  /**
   * 获取所有会话列表
   * @returns 会话列表
   */
  static async getSessions(): Promise<SessionInfo[]> {
    return request.get<SessionInfo[]>('/chat/sessions')
  }

  /**
   * 获取会话详情
   * @param sessionId 会话ID
   * @returns 会话信息
   */
  static async getSession(sessionId: string): Promise<SessionInfo> {
    return request.get<SessionInfo>(`/chat/sessions/${sessionId}`)
  }

  /**
   * 删除会话
   * @param sessionId 会话ID
   */
  static async deleteSession(sessionId: string): Promise<void> {
    return request.delete<void>(`/chat/sessions/${sessionId}`)
  }

  /**
   * 获取会话的消息历史
   * @param sessionId 会话ID
   * @returns 消息列表
   */
  static async getMessages(sessionId: string): Promise<ChatMessageResponse[]> {
    return request.get<ChatMessageResponse[]>(`/chat/sessions/${sessionId}/messages`)
  }

  /**
   * 发送消息并获取AI回复
   * @param data 发送消息请求参数
   * @returns AI回复
   */
  static async sendMessage(data: SendMessageRequest): Promise<ChatResponse> {
    return request.post<ChatResponse>('/chat/messages', data)
  }

  /**
   * 清空会话消息
   * @param sessionId 会话ID
   */
  static async clearMessages(sessionId: string): Promise<void> {
    return request.delete<void>(`/chat/sessions/${sessionId}/messages`)
  }

  /**
   * 关联主机到会话
   * @param sessionId 会话ID
   * @param agentId 主机ID
   */
  static async linkAgent(sessionId: string, agentId: string): Promise<void> {
    return request.post<void>(`/chat/sessions/${sessionId}/link`, null, {
      params: { agentId }
    })
  }
}

/**
 * 导出侧边栏AI助手便捷函数
 */
export const createChatSession = ChatSessionAPI.createSession
export const getChatSessions = ChatSessionAPI.getSessions
export const getChatSession = ChatSessionAPI.getSession
export const deleteChatSession = ChatSessionAPI.deleteSession
export const getChatMessages = ChatSessionAPI.getMessages
export const sendChatMessage = ChatSessionAPI.sendMessage
export const clearChatMessages = ChatSessionAPI.clearMessages
export const linkAgentToSession = ChatSessionAPI.linkAgent
