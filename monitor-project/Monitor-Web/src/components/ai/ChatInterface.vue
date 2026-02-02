<template>
  <div class="chat-interface">
    <!-- 消息列表 -->
    <div ref="messagesRef" class="chat-messages">
      <ChatMessage
        v-for="message in messages"
        :key="message.id"
        :message="message"
      />
      <div v-if="loading" class="chat-loading">
        <span>AI正在思考...</span>
      </div>
    </div>

    <!-- 快捷指令 -->
    <div class="chat-quick-actions" v-if="showQuickActions && messages.length === 0">
      <QuickActions @select="handleQuickAction" />
    </div>

    <!-- 输入框 -->
    <div class="chat-input-wrapper">
      <el-input
        v-model="inputMessage"
        type="textarea"
        :rows="1"
        :autosize="{ minRows: 1, maxRows: 4 }"
        placeholder="输入指令或问题..."
        @keydown.enter.prevent="handleEnter"
      />
      <el-button
        type="primary"
        :icon="Promotion"
        :loading="loading"
        :disabled="!inputMessage.trim()"
        @click="handleSend"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ChatMessage from './ChatMessage.vue'
import QuickActions from './QuickActions.vue'
import type { ChatMessage as ChatMessageType } from '@/api/ai'

interface Props {
  messages: ChatMessageType[]
  loading?: boolean
  showQuickActions?: boolean
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const props = withDefaults(defineProps<Props>(), {
  loading: false,
  showQuickActions: true,
})

const emit = defineEmits<{
  send: [message: string]
}>()

const inputMessage = ref('')
const messagesRef = ref<HTMLElement>()

function handleEnter(e: KeyboardEvent) {
  if (e.shiftKey) {
    // Shift+Enter 换行
    return
  }
  handleSend()
}

function handleSend() {
  const message = inputMessage.value.trim()
  if (!message) {
    return
  }

  emit('send', message)
  inputMessage.value = ''
  scrollToBottom()
}

function handleQuickAction(template: string) {
  inputMessage.value = template
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

// 暴露滚动方法
defineExpose({
  scrollToBottom,
})
</script>

<style scoped>
.chat-interface {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.chat-loading {
  display: flex;
  justify-content: center;
  padding: 16px;
  color: var(--text-tertiary);
}

.chat-quick-actions {
  padding: 0 16px;
}

.chat-input-wrapper {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-top: 1px solid var(--border-color);
}

.chat-input-wrapper :deep(.el-textarea) {
  flex: 1;
}

.chat-input-wrapper :deep(.el-textarea__inner) {
  resize: none;
}
</style>
