<template>
  <div class="chat-message" :class="`chat-message--${message.role}`">
    <div class="chat-message__avatar">
      <template v-if="message.role === 'user'">
        <el-avatar :size="36">U</el-avatar>
      </template>
      <template v-else>
        <div class="ai-avatar">🤖</div>
      </template>
    </div>
    <div class="chat-message__content">
      <div class="chat-message__role">
        {{ message.role === 'user' ? '我' : 'AI助手' }}
      </div>
      <div class="chat-message__text">{{ message.content }}</div>
      <div class="chat-message__time">
        {{ formatTime(message.timestamp) }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ChatMessage } from '@/api/ai'

interface Props {
  message: ChatMessage
}

defineProps<Props>()

function formatTime(timestamp: number): string {
  const date = new Date(timestamp)
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}
</script>

<style scoped>
.chat-message {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.chat-message__avatar {
  flex-shrink: 0;
}

.ai-avatar {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background-color: var(--bg-secondary);
  border-radius: 50%;
}

.chat-message__content {
  flex: 1;
  max-width: 80%;
}

.chat-message--user .chat-message__content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.chat-message__role {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-bottom: 4px;
}

.chat-message__text {
  padding: 12px 16px;
  border-radius: 8px;
  line-height: 1.6;
  word-wrap: break-word;
}

.chat-message--user .chat-message__text {
  background-color: var(--color-primary);
  color: white;
  border-radius: 8px 8px 0 8px;
}

.chat-message--assistant .chat-message__text {
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  border-radius: 8px 8px 8px 0;
}

.chat-message__time {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 4px;
}
</style>
