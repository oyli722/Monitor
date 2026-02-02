<template>
  <div class="ai-assistant">
    <div class="ai-sidebar">
      <div class="sidebar-header">
        <h3>对话历史</h3>
        <el-button :icon="Plus" circle size="small" @click="createNewChat" />
      </div>
      <div class="chat-list">
        <div
          v-for="session in chatSessions"
          :key="session.sessionId"
          class="chat-item"
          :class="{ active: currentSessionId === session.sessionId }"
          @click="switchSession(session.sessionId)"
        >
          <div class="chat-title">{{ session.title }}</div>
          <div class="chat-time">{{ formatTime(session.updatedAt) }}</div>
          <el-icon
            class="chat-delete"
            @click.stop="deleteSession(session.sessionId)"
          >
            <Delete />
          </el-icon>
        </div>
      </div>
    </div>

    <div class="ai-main">
      <div class="ai-header">
        <div class="ai-title">🤖 AI运维助手</div>
        <div class="ai-status">
          <span class="status-dot"></span>
          <span>在线</span>
        </div>
      </div>

      <ChatInterface
        :messages="currentMessages"
        :loading="loading"
        @send="handleSendMessage"
        ref="chatInterfaceRef"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ChatInterface from '@/components/ai/ChatInterface.vue'
import type { ChatMessage as ChatMessageType } from '@/api/ai'

interface ChatSession {
  sessionId: string
  title: string
  createdAt: number
  updatedAt: number
}

const chatSessions = ref<ChatSession[]>([
  {
    sessionId: '1',
    title: '主机监控查询',
    createdAt: Date.now() - 3600000,
    updatedAt: Date.now() - 1800000,
  },
  {
    sessionId: '2',
    title: '服务重启',
    createdAt: Date.now() - 86400000,
    updatedAt: Date.now() - 86400000,
  },
])

const messagesMap = ref<Map<string, ChatMessageType[]>>(new Map())
const currentSessionId = ref('')
const loading = ref(false)
const chatInterfaceRef = ref<InstanceType<typeof ChatInterface>>()

const currentMessages = computed(() =>
  messagesMap.value.get(currentSessionId.value) || [],
)

// 初始化消息数据
messagesMap.value.set('1', [
  {
    id: '1',
    role: 'user',
    content: '检查所有主机的CPU使用率',
    timestamp: Date.now() - 3600000,
  },
  {
    id: '2',
    role: 'assistant',
    content: '好的，正在检查所有主机的CPU使用率...\n\n当前共有12台主机在线，平均CPU使用率为42%。\n\n主机详情：\n- Web服务器-01: 35%\n- 数据库服务器-01: 52%\n- 应用服务器-01: 28%\n- 备份服务器-01: 离线\n\n没有主机触发告警阈值。',
    timestamp: Date.now() - 3550000,
  },
])

messagesMap.value.set('2', [
  {
    id: '1',
    role: 'user',
    content: '重启Web服务器-01的Nginx服务',
    timestamp: Date.now() - 86400000,
  },
  {
    id: '2',
    role: 'assistant',
    content: '正在重启Web服务器-01的Nginx服务...\n\n✓ 停止Nginx服务\n✓ 启动Nginx服务\n✓ 检查服务状态：running\n\nNginx服务已成功重启。',
    timestamp: Date.now() - 8635000,
  },
])

// 设置默认会话
if (chatSessions.value.length > 0) {
  currentSessionId.value = chatSessions.value[0].sessionId
}

function formatTime(timestamp: number): string {
  const now = Date.now()
  const diff = now - timestamp

  if (diff < 60000) {
    return '刚刚'
  } else if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  } else if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  } else if (diff < 604800000) {
    return `${Math.floor(diff / 86400000)}天前`
  } else {
    const date = new Date(timestamp)
    return `${date.getMonth() + 1}/${date.getDate()}`
  }
}

function createNewChat() {
  const sessionId = Date.now().toString()
  chatSessions.value.unshift({
    sessionId,
    title: '新对话',
    createdAt: Date.now(),
    updatedAt: Date.now(),
  })
  messagesMap.value.set(sessionId, [])
  currentSessionId.value = sessionId
}

function switchSession(sessionId: string) {
  currentSessionId.value = sessionId
}

async function deleteSession(sessionId: string) {
  await ElMessageBox.confirm('确定要删除该对话吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })

  const index = chatSessions.value.findIndex((s) => s.sessionId === sessionId)
  if (index !== -1) {
    chatSessions.value.splice(index, 1)
    messagesMap.value.delete(sessionId)

    if (currentSessionId.value === sessionId) {
      currentSessionId.value = chatSessions.value[0]?.sessionId || ''
    }
  }

  ElMessage.success('删除成功')
}

async function handleSendMessage(message: string) {
  if (!currentSessionId.value) {
    createNewChat()
  }

  const userMessage: ChatMessageType = {
    id: Date.now().toString(),
    role: 'user',
    content: message,
    timestamp: Date.now(),
  }

  const messages = messagesMap.value.get(currentSessionId.value) || []
  messages.push(userMessage)
  messagesMap.value.set(currentSessionId.value, messages)

  // 更新会话标题（如果是第一条消息）
  const session = chatSessions.value.find((s) => s.sessionId === currentSessionId.value)
  if (session && messages.length === 1) {
    session.title = message.slice(0, 20) + (message.length > 20 ? '...' : '')
  }

  loading.value = true

  // 模拟AI回复
  setTimeout(() => {
    const assistantMessage: ChatMessageType = {
      id: (Date.now() + 1).toString(),
      role: 'assistant',
      content: `收到指令：${message}\n\n正在处理中...`,
      timestamp: Date.now(),
    }

    messages.push(assistantMessage)
    messagesMap.value.set(currentSessionId.value, messages)
    loading.value = false

    // 滚动到底部
    chatInterfaceRef.value?.scrollToBottom()
  }, 1000)
}
</script>

<style scoped>
.ai-assistant {
  display: flex;
  height: 100%;
}

.ai-sidebar {
  width: 280px;
  background-color: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
}

.sidebar-header h3 {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.chat-item {
  position: relative;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
  margin-bottom: 4px;
}

.chat-item:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.chat-item.active {
  background-color: rgba(37, 99, 235, 0.1);
}

.chat-title {
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.chat-delete {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 14px;
  color: var(--text-tertiary);
  opacity: 0;
  transition: all 0.2s;
}

.chat-item:hover .chat-delete {
  opacity: 1;
}

.chat-delete:hover {
  color: var(--color-danger);
}

.ai-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.ai-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}

.ai-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-primary);
}

.ai-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--color-success);
}
</style>
