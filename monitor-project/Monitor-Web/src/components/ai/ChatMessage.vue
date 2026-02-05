<template>
  <div class="chat-message" :class="messageClass">
    <div v-if="showRole || showAvatar" class="message-avatar">
      <el-icon v-if="message.role === 'user'" :size="24">
        <User />
      </el-icon>
      <el-icon v-else :size="24" color="#409eff">
        <ChatDotRound />
      </el-icon>
    </div>
    <div class="message-content">
      <div v-if="showRole" class="message-header">
        <span class="message-role">{{ roleDisplayName }}</span>
        <span class="message-time">{{ formatTime }}</span>
      </div>
      <div class="message-body">
        <template v-if="isCommandOutput">
          <div class="command-output">
            <pre>{{ message.content }}</pre>
          </div>
        </template>
        <template v-else>
          <MarkdownRenderer :content="message.content" />
        </template>
        <el-icon v-if="message.isStreaming" class="streaming-indicator" :size="16">
          <Loading />
        </el-icon>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { User, ChatDotRound, Loading } from '@element-plus/icons-vue'
import MarkdownRenderer from './MarkdownRenderer.vue'
import type { ChatMessage as ChatMessageType } from '@/types/ai'

interface Props {
  message: ChatMessageType
  isCommandOutput?: boolean
  showRole?: boolean
  showAvatar?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isCommandOutput: false,
  showRole: true,
  showAvatar: true
})

const messageClass = computed(() => ({
  'is-user': props.message.role === 'user',
  'is-assistant': props.message.role === 'assistant',
  'is-system': props.message.role === 'system',
  'is-streaming': props.message.isStreaming
}))

const roleDisplayName = computed(() => {
  switch (props.message.role) {
    case 'user':
      return '您'
    case 'assistant':
      return 'AI助手'
    case 'system':
      return '系统'
    default:
      return ''
  }
})

const formatTime = computed(() => {
  const date = new Date(props.message.timestamp)
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  const seconds = date.getSeconds().toString().padStart(2, '0')
  return `${hours}:${minutes}:${seconds}`
})
</script>

<style scoped>
.chat-message {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.chat-message:hover {
  background-color: var(--el-fill-color-lighter);
}

.chat-message.is-user {
  flex-direction: row-reverse;
}

.chat-message.is-user .message-content {
  align-items: flex-end;
}

.message-avatar {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: var(--el-fill-color);
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.message-header {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.chat-message.is-user .message-header {
  flex-direction: row-reverse;
}

.message-role {
  font-weight: 600;
}

.message-time {
  opacity: 0.7;
}

.message-body {
  position: relative;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.5;
  word-wrap: break-word;
  word-break: break-word;
}

.chat-message.is-user .message-body {
  background-color: var(--el-color-primary);
  color: white;
}

.chat-message.is-assistant .message-body {
  background-color: var(--el-fill-color);
}

.chat-message.is-system .message-body {
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-style: italic;
}

.command-output {
  background-color: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 6px;
  font-family: 'Courier New', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.4;
  overflow-x: auto;
}

.command-output pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.streaming-indicator {
  display: inline-block;
  margin-left: 4px;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}
</style>
