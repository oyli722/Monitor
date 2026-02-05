<template>
  <div class="chat-interface">
    <!-- 消息列表区域 -->
    <div ref="messagesContainer" class="messages-container">
      <template v-if="messages.length === 0">
        <div class="empty-state">
          <el-icon :size="64" color="#909399">
            <ChatDotRound />
          </el-icon>
          <p class="empty-text">开始与AI助手对话</p>
          <p class="empty-hint">您可以让AI帮您执行SSH命令、分析系统状态等</p>
        </div>
      </template>

      <template v-else>
        <ChatMessage
          v-for="message in messages"
          :key="message.id"
          :message="message"
          :is-command-output="isCommandOutputMessage(message)"
        />
      </template>
    </div>

    <!-- 输入区域 -->
    <ChatInput
      :disabled="!isConnected"
      :loading="isProcessing"
      @send="handleSend"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { ChatDotRound } from '@element-plus/icons-vue'
import ChatMessage from './ChatMessage.vue'
import ChatInput from './ChatInput.vue'
import type { ChatMessage as ChatMessageType } from '@/types/ai'

interface Props {
  messages: ChatMessageType[]
  isConnected?: boolean
  isProcessing?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isConnected: true,
  isProcessing: false
})

interface Emits {
  (e: 'send', content: string): void
}

const emit = defineEmits<Emits>()

const messagesContainer = ref<HTMLElement>()

// 判断是否为命令输出消息
const isCommandOutputMessage = (message: ChatMessageType): boolean => {
  // 命令输出消息通常包含大量文本或特殊格式
  // 这里可以根据实际需要调整判断逻辑
  return message.content.includes('## COMMAND_') ||
         message.content.length > 1000 ||
         message.content.includes('\n')
}

// 发送消息
const handleSend = (content: string) => {
  emit('send', content)
}

// 自动滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 监听消息变化，自动滚动
watch(() => props.messages, () => {
  scrollToBottom()
}, { deep: true })
</script>

<style scoped>
.chat-interface {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--el-text-color-secondary);
  text-align: center;
}

.empty-text {
  font-size: 18px;
  font-weight: 500;
  margin-top: 16px;
  margin-bottom: 8px;
}

.empty-hint {
  font-size: 14px;
  color: var(--el-text-color-placeholder);
}

/* 自定义滚动条 */
.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-track {
  background: transparent;
}

.messages-container::-webkit-scrollbar-thumb {
  background-color: var(--el-border-color-darker);
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background-color: var(--el-border-color-dark);
}
</style>
