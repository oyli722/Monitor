<template>
  <el-dialog
    v-model="dialogVisible"
    title="AI终端助手"
    :width="800"
    :modal="false"
    :close-on-click-modal="false"
    :close-on-press-escape="true"
    append-to-body
    @closed="handleClosed"
    class="ai-assistant-dialog"
    draggable
  >
    <!-- 连接状态栏 -->
    <div class="status-bar">
      <div class="status-indicator">
        <span class="status-dot" :class="statusClass"></span>
        <span class="status-text">{{ statusText }}</span>
      </div>
      <div class="connection-info">
        <el-tag size="small" type="info">Agent: {{ agentId }}</el-tag>
        <el-tag v-if="sshSessionId" size="small" type="success">SSH已连接</el-tag>
      </div>
    </div>

    <!-- 聊天界面 -->
    <div class="chat-container">
      <ChatInterface
        :messages="messages"
        :is-connected="isConnected"
        :is-processing="isProcessing"
        @send="handleSendMessage"
      />
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import ChatInterface from './ChatInterface.vue'
import type { ChatMessage, ChatSession } from '@/types/ai'
import { connectAIAssistant, disconnectAIAssistant } from '@/api/ai'
import { useAiChat } from '@/composables/useAiChat'

interface Props {
  sshSessionId?: string
  agentId: string
}

const props = defineProps<Props>()

interface Emits {
  (e: 'open'): void
  (e: 'close'): void
  (e: 'connected', aiSessionId: string): void
}

const emit = defineEmits<Emits>()

// 对话框可见性
const dialogVisible = ref(false)

// 会话状态
const aiSessionId = ref<string>('')
const isConnected = ref(false)
const isConnecting = ref(false)
const isProcessing = ref(false)
const connectionError = ref<string | null>(null)

// 消息列表
const messages = ref<ChatMessage[]>([])

// 当前会话
const currentSession = ref<ChatSession | null>(null)

// useAiChat composable (将在连接后初始化)
let aiChat: ReturnType<typeof useAiChat> | null = null

// 当前流式消息ID（用于更新流式回复）
let currentStreamingMessageId: string | null = null

// 生成消息ID
const generateMessageId = (): string => {
  return `msg_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`
}

// 创建新会话
const createSession = (): ChatSession => {
  const sessionId = `session_${Date.now()}`
  return {
    sessionId,
    messages: [],
    createdAt: Date.now(),
    updatedAt: Date.now()
  }
}

// 状态样式类
const statusClass = computed(() => ({
  'is-connecting': isConnecting.value,
  'is-connected': isConnected.value,
  'is-error': connectionError.value !== null
}))

// 状态文本
const statusText = computed(() => {
  if (connectionError.value) return `连接失败: ${connectionError.value}`
  if (isConnecting.value) return '正在连接...'
  if (isConnected.value) return '已连接'
  return '未连接'
})

// 打开对话框
const open = () => {
  dialogVisible.value = true
  emit('open')
}

// 关闭对话框
const close = () => {
  dialogVisible.value = false
}

// 对话框关闭后处理
const handleClosed = () => {
  // 断开连接
  if (aiSessionId.value) {
    disconnect()
  }
  // 清空消息（页面刷新即重置，设计为单次会话）
  messages.value = []
  currentSession.value = null
  emit('close')
}

// 连接AI助手
const connect = async () => {
  if (!props.sshSessionId) {
    connectionError.value = 'SSH会话未建立'
    ElMessage.error('请先建立SSH连接')
    return
  }

  isConnecting.value = true
  connectionError.value = null

  try {
    const response = await connectAIAssistant({
      sshSessionId: props.sshSessionId,
      agentId: props.agentId
    })

    aiSessionId.value = response.aiSessionId
    isConnected.value = true

    // 创建新会话
    currentSession.value = createSession()

    // 添加系统欢迎消息
    addSystemMessage(response.message || 'AI助手已连接，可以开始对话了')

    // 初始化WebSocket连接
    initWebSocket(response.aiSessionId)

    emit('connected', aiSessionId.value)
    ElMessage.success('AI助手连接成功')
  } catch (error: any) {
    const errorMsg = error.response?.data?.message || error.message || '连接失败'
    connectionError.value = errorMsg
    ElMessage.error(errorMsg)
  } finally {
    isConnecting.value = false
  }
}

// 初始化WebSocket连接
const initWebSocket = (sessionId: string) => {
  console.log('[AiAssistantDialog] 初始化WebSocket, aiSessionId=', sessionId)

  aiChat = useAiChat({
    aiSessionId: sessionId,
    onMessage: handleAiMessage,
    onCommandOutput: handleCommandOutput,
    onCommandComplete: handleCommandComplete,
    onError: handleAiError,
    onStatusChange: handleStatusChange
  })

  // 连接WebSocket
  console.log('[AiAssistantDialog] 开始连接WebSocket')
  aiChat.connect()
}

// 处理AI消息
const handleAiMessage = (message: ChatMessage) => {
  console.log('[AiAssistantDialog] handleAiMessage, isStreaming=', message.isStreaming, ', currentStreamingMessageId=', currentStreamingMessageId)

  // 如果有当前流式消息ID，更新该消息
  if (currentStreamingMessageId) {
    const existingMessage = messages.value.find(m => m.id === currentStreamingMessageId)
    if (existingMessage) {
      // 更新现有消息内容
      existingMessage.content = message.content
      existingMessage.timestamp = message.timestamp

      // 如果消息完成，清除流式状态
      if (!message.isStreaming) {
        existingMessage.isStreaming = false
        currentStreamingMessageId = null
        isProcessing.value = false
        console.log('[AiAssistantDialog] 消息完成，停止加载')
      }

      updateSession()
      return
    }
  }

  // 没有当前流式消息，创建新消息
  if (message.isStreaming) {
    currentStreamingMessageId = message.id
    console.log('[AiAssistantDialog] 开始新的流式消息, id=', message.id)
  } else {
    // 非流式消息完成
    isProcessing.value = false
    console.log('[AiAssistantDialog] 非流式消息完成')
  }

  messages.value.push(message)
  updateSession()
}

// 处理命令输出（作为特殊消息添加）
const handleCommandOutput = (output: string) => {
  const commandMessage: ChatMessage = {
    id: generateMessageId(),
    role: 'assistant',
    content: `**命令输出:**\n\`\`\`\n${output}\n\`\`\``,
    timestamp: Date.now()
  }
  messages.value.push(commandMessage)
  updateSession()
}

// 处理命令完成
const handleCommandComplete = (exitCode: number) => {
  const statusMsg = exitCode === 0
    ? '✅ 命令执行成功'
    : `❌ 命令执行失败 (退出码: ${exitCode})`

  const message: ChatMessage = {
    id: generateMessageId(),
    role: 'system',
    content: statusMsg,
    timestamp: Date.now()
  }
  messages.value.push(message)
  updateSession()
  isProcessing.value = false
  currentStreamingMessageId = null
}

// 处理AI错误
const handleAiError = (error: string) => {
  connectionError.value = error
  ElMessage.error(error)
}

// 处理连接状态变化
const handleStatusChange = (newStatus: string) => {
  console.log('[AiAssistantDialog] WebSocket状态变化:', newStatus)

  if (newStatus === 'connected') {
    isConnected.value = true
    connectionError.value = null
  } else if (newStatus === 'error') {
    isConnected.value = false
  } else if (newStatus === 'disconnected') {
    isConnected.value = false
  }
}

// 断开连接
const disconnect = async () => {
  if (!aiSessionId.value) return

  // 先断开WebSocket
  if (aiChat) {
    aiChat.disconnect()
    aiChat = null
  }

  try {
    await disconnectAIAssistant(aiSessionId.value)
  } catch (error) {
    console.error('断开AI助手失败:', error)
  } finally {
    isConnected.value = false
    aiSessionId.value = ''
    currentStreamingMessageId = null
  }
}

// 添加系统消息
const addSystemMessage = (content: string) => {
  const message: ChatMessage = {
    id: generateMessageId(),
    role: 'system',
    content,
    timestamp: Date.now()
  }
  messages.value.push(message)
  updateSession()
}

// 发送消息
const handleSendMessage = async (content: string) => {
  console.log('[AiAssistantDialog] 准备发送消息, isConnected=', isConnected.value, ', aiChat=', !!aiChat)

  if (!isConnected.value || !aiChat) {
    console.warn('[AiAssistantDialog] AI助手未连接')
    ElMessage.warning('AI助手未连接，请稍候')
    return
  }

  // 添加用户消息
  const userMessage: ChatMessage = {
    id: generateMessageId(),
    role: 'user',
    content,
    timestamp: Date.now()
  }
  messages.value.push(userMessage)
  updateSession()

  // 通过WebSocket发送消息
  console.log('[AiAssistantDialog] 调用 sendChatMessage')
  const success = aiChat.sendChatMessage(content)
  console.log('[AiAssistantDialog] sendChatMessage 返回:', success)

  if (!success) {
    console.error('[AiAssistantDialog] 发送消息失败')
    ElMessage.error('发送消息失败')
    return
  }

  // 创建助手回复占位消息（用于流式显示）
  const assistantMessage: ChatMessage = {
    id: generateMessageId(),
    role: 'assistant',
    content: '',
    timestamp: Date.now(),
    isStreaming: true
  }
  messages.value.push(assistantMessage)
  updateSession()

  // 记录当前流式消息ID
  currentStreamingMessageId = assistantMessage.id

  isProcessing.value = true
}

// 更新会话
const updateSession = () => {
  if (currentSession.value) {
    currentSession.value.messages = [...messages.value]
    currentSession.value.updatedAt = Date.now()
  }
}

// 监听对话框打开，自动连接
watch(dialogVisible, (visible) => {
  if (visible && props.sshSessionId && !isConnected.value) {
    connect()
  }
})

// 组件卸载时清理
onUnmounted(() => {
  if (aiChat) {
    aiChat.disconnect()
    aiChat = null
  }
})

// 暴露方法供父组件调用
defineExpose({
  open,
  close,
  connect,
  disconnect,
  aiSessionId,
  isConnected
})
</script>

<style scoped>
.ai-assistant-dialog :deep(.el-dialog__body) {
  padding: 0;
  display: flex;
  flex-direction: column;
}

.status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background-color: var(--el-fill-color-light);
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--el-text-color-placeholder);
}

.status-dot.is-connecting {
  background-color: var(--el-color-warning);
  animation: pulse 1.5s ease-in-out infinite;
}

.status-dot.is-connected {
  background-color: var(--el-color-success);
}

.status-dot.is-error {
  background-color: var(--el-color-danger);
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.4;
  }
}

.status-text {
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.connection-info {
  display: flex;
  gap: 8px;
}

.chat-container {
  height: 500px;
  display: flex;
  flex-direction: column;
}
</style>
