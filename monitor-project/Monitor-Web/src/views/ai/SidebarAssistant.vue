<template>
  <div class="sidebar-assistant">
    <!-- 左侧会话列表 -->
    <div class="session-panel">
      <div class="session-header">
        <h3>AI对话</h3>
        <el-button type="primary" :icon="Plus" size="small" @click="handleNewChat">
          新对话
        </el-button>
      </div>

      <div class="session-list">
        <div
          v-for="session in sessions"
          :key="session.sessionId || session.title || session.updatedAt"
          class="session-item"
          :class="{ active: session.sessionId === currentSessionId }"
          @click="handleSelectSession(session.sessionId)"
        >
          <div class="session-title">{{ session.title || '未命名对话' }}</div>
          <div class="session-meta">
            <span class="session-time">{{ formatTime(session.updatedAt) }}</span>
            <el-popconfirm
              title="确认删除此对话？"
              confirm-button-text="确认"
              cancel-button-text="取消"
              @confirm="handleDeleteSession(session.sessionId)"
            >
              <template #reference>
                <el-button
                  type="danger"
                  :icon="Delete"
                  size="small"
                  text
                  @click.stop
                />
              </template>
            </el-popconfirm>
          </div>
        </div>

        <el-empty v-if="sessions.length === 0" description="暂无对话记录" :image-size="80" />
      </div>
    </div>

    <!-- 右侧聊天界面 -->
    <div class="chat-panel">
      <div v-if="!currentSessionId" class="welcome-view">
        <div class="welcome-content">
          <el-icon :size="64" color="#409eff"><ChatDotRound /></el-icon>
          <h2>AI智能助手</h2>
          <p>点击上方"新对话"按钮或选择已有对话开始聊天</p>
          <div class="welcome-tips">
            <p>💡 提示：直接在下方输入框中输入消息即可开始新对话</p>
          </div>
        </div>
        <!-- 始终显示输入框，方便开始新对话 -->
        <div class="input-container-welcome">
          <ChatInput
            :disabled="isLoading"
            placeholder="输入消息开始新对话... (Ctrl+Enter 发送)"
            @send="handleSendMessage"
          />
        </div>
      </div>

      <div v-else class="chat-view">
        <!-- 聊天消息区域 -->
        <div class="messages-container" ref="messagesContainerRef">
          <div
            v-for="message in messages"
            :key="message.id"
            class="message-item"
            :class="`message-${message.role}`"
          >
            <div class="message-content">
              <ChatMessage
                :message="message"
                :show-role="true"
              />
            </div>
          </div>

          <!-- 加载中提示 -->
          <div v-if="isLoading" class="message-item message-assistant">
            <div class="message-content">
              <div class="loading-indicator">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>AI正在思考...</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-container">
          <ChatInput
            :disabled="isLoading"
            placeholder="输入消息... (Ctrl+Enter 发送)"
            @send="handleSendMessage"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete, ChatDotRound, Loading } from '@element-plus/icons-vue'
import ChatMessage from '@/components/ai/ChatMessage.vue'
import ChatInput from '@/components/ai/ChatInput.vue'
import type { ChatMessage as ChatMessageType, SessionInfo } from '@/types/ai'
import {
  createChatSession,
  getChatSessions,
  deleteChatSession,
  getChatMessages,
  sendChatMessage
} from '@/api/ai'
import { ChatSessionAPI } from '@/api/ai'

// 会话列表
const sessions = ref<SessionInfo[]>([])

// 当前会话ID
const currentSessionId = ref<string>('')

// 当前会话信息
const currentSession = ref<SessionInfo | null>(null)

// 消息列表
const messages = ref<ChatMessageType[]>([])

// 加载状态
const isLoading = ref(false)

// 消息容器引用（用于滚动）
const messagesContainerRef = ref<HTMLElement>()

// 生成消息ID
const generateMessageId = (): string => {
  return `msg_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`
}

// 格式化时间
const formatTime = (timestamp: number | undefined | null): string => {
  if (!timestamp) return '未知时间'

  const date = new Date(timestamp)
  const now = new Date()

  // 检查日期是否有效
  if (isNaN(date.getTime())) {
    return '未知时间'
  }

  const diff = now.getTime() - date.getTime()

  // 时间戳在未来（时钟不同步）
  if (diff < 0) {
    return '刚刚'
  }

  // 小于1小时显示分钟
  if (diff < 3600000) {
    const minutes = Math.floor(diff / 60000)
    return minutes === 0 ? '刚刚' : `${minutes}分钟前`
  }
  // 小于24小时显示小时
  if (diff < 86400000) {
    const hours = Math.floor(diff / 3600000)
    return `${hours}小时前`
  }
  // 小于7天显示天数
  if (diff < 604800000) {
    const days = Math.floor(diff / 86400000)
    return `${days}天前`
  }
  // 否则显示日期
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainerRef.value) {
      messagesContainerRef.value.scrollTop = messagesContainerRef.value.scrollHeight
    }
  })
}

// 加载会话列表
const loadSessions = async () => {
  console.log('[SidebarAssistant] loadSessions called')
  try {
    const data = await getChatSessions()
    console.log('[SidebarAssistant] loadSessions result:', data)

    // 确保 data 是有效的数组
    if (!Array.isArray(data)) {
      console.warn('[SidebarAssistant] loadSessions received non-array data:', data)
      sessions.value = []
      return
    }

    // 过滤掉无效的会话数据
    const validSessions = data.filter(session => {
      return session && session.sessionId && session.title
    })

    if (validSessions.length !== data.length) {
      console.warn('[SidebarAssistant] Filtered out invalid sessions:', data.length - validSessions.length)
    }

    // 使用 nextTick 确保 Vue 响应式系统正确更新
    await nextTick()
    sessions.value = validSessions
  } catch (error: any) {
    console.error('[SidebarAssistant] loadSessions error:', error)
    ElMessage.error('加载会话列表失败: ' + (error.message || '未知错误'))
    sessions.value = []
  }
}

// 新建对话
const handleNewChat = () => {
  console.log('[SidebarAssistant] handleNewChat called')
  currentSessionId.value = ''
  messages.value = []
  currentSession.value = null
  ElMessage.info('已创建新对话，请输入消息开始')
}

// 选择会话
const handleSelectSession = async (sessionId: string) => {
  currentSessionId.value = sessionId
  await loadMessages(sessionId)
}

// 删除会话
const handleDeleteSession = async (sessionId: string) => {
  try {
    await deleteChatSession(sessionId)
    await loadSessions()

    // 如果删除的是当前会话，清空聊天区域
    if (sessionId === currentSessionId.value) {
      currentSessionId.value = ''
      messages.value = []
      currentSession.value = null
    }

    ElMessage.success('删除成功')
  } catch (error: any) {
    ElMessage.error('删除失败: ' + (error.message || '未知错误'))
  }
}

// 加载消息历史
const loadMessages = async (sessionId: string) => {
  try {
    const data = await getChatMessages(sessionId)
    messages.value = data.map((msg, index) => ({
      id: `history_${sessionId}_${index}`,
      role: msg.role,
      content: msg.content,
      timestamp: msg.timestamp
    }))

    // 更新当前会话信息
    const session = sessions.value.find(s => s.sessionId === sessionId)
    if (session) {
      currentSession.value = session
    }

    scrollToBottom()
  } catch (error: any) {
    ElMessage.error('加载消息失败: ' + (error.message || '未知错误'))
  }
}

// 发送消息（流式）
const handleSendMessage = async (content: string) => {
  console.log('[SidebarAssistant] handleSendMessage called, content:', content)

  // 如果没有会话ID，先创建会话
  if (!currentSessionId.value) {
    console.log('[SidebarAssistant] Creating new session')
    try {
      const result = await createChatSession({
        firstMessage: content
      })
      console.log('[SidebarAssistant] Session created:', result)
      currentSessionId.value = result.sessionId

      // 初始化消息列表（为创建会话的消息预留位置）
      messages.value = []

      // 延迟加载会话列表，避免与添加消息操作冲突
      await nextTick()
      await loadSessions()
    } catch (error: any) {
      console.error('[SidebarAssistant] Create session error:', error)
      ElMessage.error('创建会话失败: ' + (error.message || '未知错误'))
      isLoading.value = false
      return
    }
  }

  // 添加用户消息
  const userMessage: ChatMessageType = {
    id: generateMessageId(),
    role: 'user',
    content,
    timestamp: Date.now()
  }
  messages.value.push(userMessage)
  scrollToBottom()

  // 创建AI消息（用于流式显示）
  const assistantMessageId = generateMessageId()
  const assistantMessage: ChatMessageType = {
    id: assistantMessageId,
    role: 'assistant',
    content: '',
    timestamp: Date.now()
  }
  messages.value.push(assistantMessage)
  scrollToBottom()

  // 发送流式消息
  isLoading.value = true
  try {
    console.log('[SidebarAssistant] Sending stream message to session:', currentSessionId.value)

    await ChatSessionAPI.sendMessageStream(
      {
        sessionId: currentSessionId.value,
        message: content
      },
      // onChunk: 接收流式数据
      (chunk: string) => {
        console.log('[SidebarAssistant] Received chunk:', chunk)
        // 更新AI消息内容
        const msgIndex = messages.value.findIndex(m => m.id === assistantMessageId)
        if (msgIndex !== -1 && messages.value[msgIndex]) {
          messages.value[msgIndex].content += chunk
          // 自动滚动到底部
          scrollToBottom()
        }
      },
      // onComplete: 流式传输完成
      () => {
        console.log('[SidebarAssistant] Stream completed')
        isLoading.value = false
        // 延迟刷新会话列表，避免与消息更新冲突
        nextTick(() => {
          loadSessions().catch(err => {
            console.warn('[SidebarAssistant] Failed to refresh sessions:', err)
          })
        })
      },
      // onError: 错误处理
      (error: Error) => {
        console.error('[SidebarAssistant] Stream error:', error)
        isLoading.value = false

        // 更新AI消息为错误提示
        const msgIndex = messages.value.findIndex(m => m.id === assistantMessageId)
        if (msgIndex !== -1 && messages.value[msgIndex]) {
          messages.value[msgIndex].content = '抱歉，发生了错误：' + error.message
        }

        ElMessage.error('发送消息失败: ' + error.message)
      }
    )
  } catch (error: any) {
    console.error('[SidebarAssistant] Send message error:', error)
    isLoading.value = false
    ElMessage.error('发送消息失败: ' + (error.message || '未知错误'))
    // 移除用户消息和AI消息
    messages.value.pop()
    messages.value.pop()
  }
}

// 组件挂载时加载会话列表
onMounted(() => {
  loadSessions()
})
</script>

<style scoped>
.sidebar-assistant {
  display: flex;
  height: calc(100vh - 60px);
  background-color: #f5f5f5;
}

/* 左侧会话面板 */
.session-panel {
  width: 280px;
  background-color: #fff;
  border-right: 1px solid var(--el-border-color-lighter);
  display: flex;
  flex-direction: column;
}

.session-header {
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.session-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
  margin-bottom: 4px;
}

.session-item:hover {
  background-color: var(--el-fill-color-light);
}

.session-item.active {
  background-color: var(--el-color-primary-light-9);
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 右侧聊天面板 */
.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.welcome-view {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.welcome-content {
  text-align: center;
}

.welcome-content h2 {
  margin: 16px 0 8px;
  font-size: 24px;
  color: var(--el-text-color-primary);
}

.welcome-content p {
  margin: 0 0 24px;
  color: var(--el-text-color-regular);
}

.welcome-tips {
  margin-top: 32px;
  padding: 16px 24px;
  background-color: var(--el-fill-color-light);
  border-radius: 8px;
  text-align: left;
}

.welcome-tips p {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.input-container-welcome {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px;
  background-color: #fff;
  border-top: 1px solid var(--el-border-color-lighter);
}

.welcome-view {
  position: relative;
}

.chat-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message-item {
  margin-bottom: 20px;
}

.message-user {
  display: flex;
  justify-content: flex-end;
}

.message-assistant {
  display: flex;
  justify-content: flex-start;
}

.message-content {
  max-width: 70%;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background-color: var(--el-fill-color-light);
  border-radius: 8px;
  color: var(--el-text-color-regular);
}

.input-container {
  padding: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  background-color: #fff;
}
</style>
