<template>
  <div class="terminal-panel">
    <div class="terminal-header">
      <div class="terminal-status">
        <span class="status-dot" :class="{ connected: connected, connecting: connecting }" />
        <span class="status-text">{{ statusText }}</span>
      </div>
      <el-button size="small" type="danger" :icon="CircleClose" @click="handleDisconnect">
        断开连接
      </el-button>
    </div>
    <div ref="terminalRef" class="terminal-container" @click="handleTerminalClick" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Terminal } from '@xterm/xterm'
import { FitAddon } from '@xterm/addon-fit'
import { CircleClose } from '@element-plus/icons-vue'
import type { Agent } from '@/types/monitor'
import { sshDisconnect } from '@/api/monitor'

interface Props {
  sessionId: string
  host: Agent | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  close: []
}>()

// 响应式数据
const terminalRef = ref<HTMLDivElement>()
const connected = ref(false)
const connecting = ref(false)

// 终端实例
let terminal: Terminal | null = null
let fitAddon: FitAddon | null = null
let ws: WebSocket | null = null

// 状态文本
const statusText = ref('未连接')

// WebSocket URL
const wsUrl = import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8080/ws'

// 初始化终端
onMounted(() => {
  initTerminal()
  connectWebSocket()
})

// 组件卸载时清理
onUnmounted(() => {
  cleanup()
})

// 初始化终端
function initTerminal() {
  if (!terminalRef.value) return

  // 创建终端实例
  terminal = new Terminal({
    cursorBlink: true,
    fontSize: 14,
    fontFamily: 'Courier New, monospace',
    theme: {
      background: '#1e1e1e',
      foreground: '#d4d4d4',
      cursor: '#4af626',
    },
  })

  // 创建自适应插件
  fitAddon = new FitAddon()
  terminal.loadAddon(fitAddon)

  // 挂载到 DOM
  terminal.open(terminalRef.value)
  fitAddon.fit()

  // 命令缓冲区
  let commandBuffer = ''

  // 监听终端输入
  terminal.onData((data) => {
    if (data === '\r') {
      // 回车键：先清空本地显示的命令，然后发送
      // 使用 \r 回到行首，然后用空格清除
      terminal?.write('\r' + ' '.repeat(commandBuffer.length) + '\r')
      // 发送完整命令
      sendToWebSocket(commandBuffer + '\r')
      commandBuffer = ''
    } else if (data === '\u007F') {
      // 退格键 (DEL/Backspace)
      if (commandBuffer.length > 0) {
        commandBuffer = commandBuffer.slice(0, -1)
        // 本地处理：删除光标前一个字符
        terminal?.write('\b \b')
      }
    } else if (data === '\u001b') {
      // ESC键或方向键开头，直接发送（由SSH服务器处理）
      sendToWebSocket(data)
    } else if (data.length === 1 && data >= ' ') {
      // 可打印字符：添加到缓冲区并本地显示
      commandBuffer += data
      terminal?.write(data)
    } else {
      // 其他控制字符，直接发送
      sendToWebSocket(data)
    }
  })



  // 监听窗口大小变化
  // window.addEventListener('resize', handleResize)
}

// 连接 WebSocket
function connectWebSocket() {
  connecting.value = true
  statusText.value = '连接中...'

  try {
    ws = new WebSocket(`${wsUrl}/api/v1/ssh/terminal/${props.sessionId}`)

    ws.onopen = () => {
      connected.value = true
      connecting.value = false
      statusText.value = '已连接'
      terminal?.focus()
    }

    ws.onmessage = (event) => {
      terminal?.write(event.data)
    }

    ws.onerror = () => {
      connected.value = false
      connecting.value = false
      statusText.value = '连接错误'
      terminal?.writeln('\x1b[31mSSH 连接错误\x1b[0m')
    }

    ws.onclose = () => {
      connected.value = false
      connecting.value = false
      statusText.value = '已断开'
      terminal?.writeln('\r\n\x1b[33mSSH 连接已断开\x1b[0m\r\n')
    }
  } catch (error) {
    console.error('WebSocket 连接失败:', error)
    connected.value = false
    connecting.value = false
    statusText.value = '连接失败'
  }
}

// 发送数据到 WebSocket
function sendToWebSocket(data: string) {
  console.log('发送WebSocket数据:', JSON.stringify(data), '长度:', data.length, 'readyState:', ws?.readyState)
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(data)
    console.log('数据已发送')
  } else {
    console.error('WebSocket未就绪:', ws?.readyState)
  }
}

// 处理终端点击，确保获取焦点
function handleTerminalClick() {
  terminal?.focus()
}

// 处理窗口大小变化
function handleResize() {
  // if (fitAddon) {
  //   fitAddon.fit()
  // }
}

// 断开连接
async function handleDisconnect() {
  try {
    await sshDisconnect(props.sessionId)
  } catch (error) {
    console.error('断开SSH连接失败:', error)
  }

  if (ws) {
    ws.close()
  }

  emit('close')
}

// 清理资源
function cleanup() {
  // 关闭 WebSocket
  if (ws) {
    ws.close()
    ws = null
  }

  // 销毁终端
  if (terminal) {
    terminal.dispose()
    terminal = null
  }

  if (fitAddon) {
    fitAddon.dispose()
    fitAddon = null
  }

  // 移除事件监听
  window.removeEventListener('resize', handleResize)
}
</script>

<style scoped>
.terminal-panel {
  display: flex;
  flex-direction: column;
  height: 400px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
}

.terminal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background-color: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
}

.terminal-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--text-tertiary);
}

.status-dot.connected {
  background-color: var(--color-success);
}

.status-dot.connecting {
  background-color: var(--color-warning);
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}

.status-text {
  font-size: 14px;
  color: var(--text-secondary);
}

.terminal-container {
  flex: 1;
  background-color: #1e1e1e;
  padding: 8px;
  overflow: auto;
}

.terminal-container :deep(.xterm-viewport) {
  background-color: #1e1e1e;
}
</style>
