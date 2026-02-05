<template>
  <div class="chat-input">
    <el-input
      v-model="inputText"
      type="textarea"
      :rows="rows"
      :placeholder="placeholder"
      :disabled="disabled"
      @keydown="handleKeydown"
      resize="none"
      maxlength="5000"
      show-word-limit
    />
    <div class="input-actions">
      <span class="input-hint">{{ inputHint }}</span>
      <el-button
        type="primary"
        :icon="Promotion"
        :disabled="!canSend"
        :loading="loading"
        @click="handleSend"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Promotion } from '@element-plus/icons-vue'

interface Props {
  disabled?: boolean
  loading?: boolean
  placeholder?: string
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  loading: false,
  placeholder: '请输入消息...（Ctrl+Enter 发送）'
})

interface Emits {
  (e: 'send', content: string): void
}

const emit = defineEmits<Emits>()

const inputText = ref('')

const rows = computed(() => {
  const text = inputText.value
  const lineCount = Math.max(1, text.split('\n').length)
  return Math.min(lineCount, 6)
})

const canSend = computed(() => {
  return !props.disabled && !props.loading && inputText.value.trim().length > 0
})

const inputHint = computed(() => {
  if (props.loading) return 'AI正在思考...'
  if (props.disabled) return '连接已断开'
  return ''
})

const handleSend = () => {
  const content = inputText.value.trim()
  if (!content || props.disabled || props.loading) return

  emit('send', content)
  inputText.value = ''
}

const handleKeydown = (e: KeyboardEvent) => {
  // Ctrl+Enter 发送消息
  if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
    e.preventDefault()
    handleSend()
  }
}

// 暴露方法
defineExpose({
  focus: () => {
    // 聚焦输入框（可通过ref访问）
  },
  clear: () => {
    inputText.value = ''
  }
})
</script>

<style scoped>
.chat-input {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
  background-color: var(--el-bg-color);
}

.chat-input :deep(.el-textarea__inner) {
  border-radius: 8px;
  resize: none;
  font-family: inherit;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.input-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
