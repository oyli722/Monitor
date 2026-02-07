/**
 * AI助手API接口
 */

import request from '@/utils/request'
import aiRequest from '@/utils/ai-request'
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
 * SSH绑定AI助手API类（使用Server服务）
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
 * 侧边栏AI会话API类（使用AI服务 8081端口）
 * 调用Monitor-AI独立服务的ChatController接口
 */
export class ChatSessionAPI {
  /**
   * 创建新会话
   * @param data 创建会话请求参数
   * @returns 会话ID和标题
   */
  static async createSession(data: CreateSessionRequest): Promise<CreateSessionResponse> {
    return aiRequest.post<CreateSessionResponse>('/chat/sessions', data)
  }

  /**
   * 获取所有会话列表
   * @returns 会话列表
   */
  static async getSessions(): Promise<SessionInfo[]> {
    return aiRequest.get<SessionInfo[]>('/chat/sessions')
  }

  /**
   * 获取会话详情
   * @param sessionId 会话ID
   * @returns 会话信息
   */
  static async getSession(sessionId: string): Promise<SessionInfo> {
    return aiRequest.get<SessionInfo>(`/chat/sessions/${sessionId}`)
  }

  /**
   * 删除会话
   * @param sessionId 会话ID
   */
  static async deleteSession(sessionId: string): Promise<void> {
    return aiRequest.delete<void>(`/chat/sessions/${sessionId}`)
  }

  /**
   * 获取会话的消息历史
   * @param sessionId 会话ID
   * @returns 消息列表
   */
  static async getMessages(sessionId: string): Promise<ChatMessageResponse[]> {
    return aiRequest.get<ChatMessageResponse[]>(`/chat/sessions/${sessionId}/messages`)
  }

  /**
   * 发送消息并获取AI回复（流式SSE）
   * @param data 发送消息请求参数
   * @param onChunk 接收流式数据的回调函数
   * @param onComplete 完成回调函数
   * @param onError 错误回调函数
   */
  static async sendMessageStream(
    data: SendMessageRequest,
    onChunk: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: Error) => void
  ): Promise<void> {
    try {
      // 获取 AI 服务的 base URL
      const baseURL = import.meta.env.VITE_AI_API_BASE_URL || 'http://localhost:8081/api'
      const url = `${baseURL}/chat/messages`

      console.log('[sendMessageStream] Sending request to:', url)
      console.log('[sendMessageStream] Request data:', data)

      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream'
        },
        body: JSON.stringify(data)
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error('Response body is not readable')
      }

      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      let chunkCount = 0

      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          console.log('[sendMessageStream] Stream done, calling onComplete')
          break
        }

        const rawText = decoder.decode(value, { stream: true })
        console.log('[sendMessageStream] Received raw data:', JSON.stringify(rawText))

        // 后端直接发送原始 chunk，每行是一个完整的消息
        buffer += rawText
        const lines = buffer.split('\n')
        buffer = lines.pop() || '' // 保留最后一个不完整的行

        console.log('[sendMessageStream] Processing lines:', lines.length)

        for (const line of lines) {
          if (line.trim().length === 0) continue // 跳过空行

          console.log('[sendMessageStream] Processing line:', JSON.stringify(line))

          // 处理 SSE 格式（Spring 自动添加的）
          if (line.startsWith('data:')) {
            const data = line.substring(5).trim() // 移除 "data:" 前缀
            console.log('[sendMessageStream] Extracted data:', JSON.stringify(data))

            if (data === '[DONE]') {
              console.log('[sendMessageStream] Received [DONE], calling onComplete')
              onComplete()
              return
            }
            if (data.startsWith('[ERROR]')) {
              onError(new Error(data.substring(7)))
              return
            }

            // 正常消息内容
            if (data && data.length > 0) {
              chunkCount++
              console.log('[sendMessageStream] Calling onChunk, count:', chunkCount, 'content:', data)
              onChunk(data)
            }
          }
        }
      }

      console.log('[sendMessageStream] While loop ended, calling onComplete')
      onComplete()
    } catch (error) {
      console.error('[sendMessageStream] Error:', error)
      onError(error as Error)
    }
  }

  /**
   * 发送消息并获取AI回复（非流式，兼容旧接口）
   * @param data 发送消息请求参数
   * @returns AI回复
   */
  static async sendMessage(data: SendMessageRequest): Promise<ChatResponse> {
    return aiRequest.post<ChatResponse>('/chat/messages', data)
  }

  /**
   * 清空会话消息
   * @param sessionId 会话ID
   */
  static async clearMessages(sessionId: string): Promise<void> {
    return aiRequest.delete<void>(`/chat/sessions/${sessionId}/messages`)
  }

  /**
   * 关联主机到会话
   * @param sessionId 会话ID
   * @param agentId 主机ID
   */
  static async linkAgent(sessionId: string, agentId: string): Promise<void> {
    return aiRequest.post<void>(`/chat/sessions/${sessionId}/link`, null, {
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
