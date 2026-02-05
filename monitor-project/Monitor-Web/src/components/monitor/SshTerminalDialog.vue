<template>
  <el-dialog
    v-model="visible"
    title="SSH 终端"
    width="900px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="ssh-terminal-dialog">
      <div class="terminal-info">
        <span class="info-item">
          <span class="label">主机:</span>
          <span class="value">{{ host?.hostname || host?.agentName }}</span>
        </span>
        <span class="info-item">
          <span class="label">IP:</span>
          <span class="value">{{ host?.ip }}</span>
        </span>
        <el-button
          type="success"
          :icon="ChatDotRound"
          size="small"
          @click="handleAiAssistantClick"
        >
          AI助手
        </el-button>
      </div>
      <TerminalPanel
        v-if="sessionId && host"
        :session-id="sessionId"
        :host="host"
        @close="handleTerminalClose"
      />
    </div>

    <!-- AI助手对话框 -->
    <AiAssistantDialog
      ref="aiAssistantDialogRef"
      :ssh-session-id="sessionId || undefined"
      :agent-id="host?.agentId || ''"
      @connected="handleAiConnected"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ChatDotRound } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import TerminalPanel from './TerminalPanel.vue'
import AiAssistantDialog from '../ai/AiAssistantDialog.vue'
import type { Agent } from '@/types/monitor'

interface Props {
  modelValue: boolean
  sessionId: string | null
  host: Agent | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  close: []
}>()

// 响应式数据
const visible = ref(props.modelValue)
const aiAssistantDialogRef = ref<InstanceType<typeof AiAssistantDialog> | null>(null)

// 监听 modelValue 变化
watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
  },
)

// 监听 visible 变化同步给父组件
watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 终端关闭回调
function handleTerminalClose() {
  visible.value = false
  emit('close')
}

// 对话框关闭
function handleClose() {
  visible.value = false
  emit('close')
}

// AI助手按钮点击
function handleAiAssistantClick() {
  aiAssistantDialogRef.value?.open()
}

// AI助手连接成功回调
function handleAiConnected(sessionId: string) {
  ElMessage.success('AI助手已连接')
}
</script>

<style scoped>
.ssh-terminal-dialog {
  display: flex;
  flex-direction: column;
}

.terminal-info {
  display: flex;
  gap: 24px;
  padding: 12px 16px;
  background-color: var(--bg-secondary);
  border-radius: 6px;
  margin-bottom: 16px;
  align-items: center;
}

.info-item {
  display: flex;
  gap: 8px;
}

.label {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.value {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 600;
}

/* AI助手按钮样式 - 放在最右侧 */
.terminal-info .el-button {
  margin-left: auto;
}
</style>
