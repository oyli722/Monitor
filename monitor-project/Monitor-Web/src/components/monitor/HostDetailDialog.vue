<template>
  <el-dialog
    v-model="visible"
    :title="`${host?.hostname || host?.agentName} - 主机详情`"
    width="900px"
    @close="handleClose"
  >
    <div v-if="host" class="host-detail">
      <!-- 主机基本信息 -->
      <div class="basic-info-section">
        <h4 class="section-title">基本信息</h4>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="主机名">{{ host.hostname }}</el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ host.ip }}</el-descriptions-item>
          <el-descriptions-item label="Agent ID">{{ host.agentId }}</el-descriptions-item>
          <el-descriptions-item label="CPU型号">{{ host.cpuModel }}</el-descriptions-item>
          <el-descriptions-item label="CPU核心">{{ host.cpuCores }} 核</el-descriptions-item>
          <el-descriptions-item label="内存">{{ host.memoryGb }} GB</el-descriptions-item>
          <el-descriptions-item label="注册时间" :span="3">
            {{ formatDate(host.registeredAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 实时监控指标 -->
      <div class="metrics-section">
        <h4 class="section-title">实时监控</h4>
        <div class="metrics-grid">
          <!-- CPU 使用率 -->
          <div class="metric-card">
            <div class="metric-header">
              <span class="metric-label">CPU 使用率</span>
              <span class="metric-value">{{ metrics?.cpuPercent.toFixed(1) || 0 }}%</span>
            </div>
            <el-progress
              :percentage="metrics?.cpuPercent || 0"
              :color="getProgressColor(metrics?.cpuPercent || 0)"
              :stroke-width="8"
            />
          </div>

          <!-- 内存使用率 -->
          <div class="metric-card">
            <div class="metric-header">
              <span class="metric-label">内存使用率</span>
              <span class="metric-value">{{ metrics?.memoryPercent.toFixed(1) || 0 }}%</span>
            </div>
            <el-progress
              :percentage="metrics?.memoryPercent || 0"
              :color="getProgressColor(metrics?.memoryPercent || 0)"
              :stroke-width="8"
            />
          </div>

          <!-- 磁盘使用率 -->
          <div class="metric-card">
            <div class="metric-header">
              <span class="metric-label">磁盘使用率</span>
              <span class="metric-value">{{ getHighestDiskUsage()?.toFixed(1) || 0 }}%</span>
            </div>
            <div class="disk-info">{{ getHighestDiskMount() || '-' }}</div>
            <el-progress
              :percentage="getHighestDiskUsage() || 0"
              :color="getProgressColor(getHighestDiskUsage() || 0)"
              :stroke-width="8"
            />
          </div>

          <!-- 网络速率 -->
          <div class="metric-card">
            <div class="metric-header">
              <span class="metric-label">网络速率</span>
            </div>
            <div class="network-info">
              <div class="network-item">
                <span class="network-label">上传:</span>
                <span class="network-value">{{ metrics?.networkUpMbps?.toFixed(2) || 0 }} Mbps</span>
              </div>
              <div class="network-item">
                <span class="network-label">下载:</span>
                <span class="network-value">{{ metrics?.networkDownMbps?.toFixed(2) || 0 }} Mbps</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 历史趋势 -->
      <div class="history-section">
        <h4 class="section-title">
          历史趋势
          <el-radio-group v-model="currentTimeRange" size="small" class="time-range-selector">
            <el-radio-button value="5MIN">5分钟</el-radio-button>
            <el-radio-button value="1H">1小时</el-radio-button>
            <el-radio-button value="6H">6小时</el-radio-button>
            <el-radio-button value="24H">24小时</el-radio-button>
            <el-radio-button value="7D">7天</el-radio-button>
            <el-radio-button value="1M">1个月</el-radio-button>
          </el-radio-group>
        </h4>
        <div class="charts-grid">
          <!-- CPU 历史图表 -->
          <div class="chart-container">
            <div class="chart-title">CPU 使用率历史</div>
            <div ref="cpuChartRef" class="chart" />
          </div>
          <!-- 内存 历史图表 -->
          <div class="chart-container">
            <div class="chart-title">内存使用率历史</div>
            <div ref="memoryChartRef" class="chart" />
          </div>
          <!-- 磁盘 历史图表 -->
          <div class="chart-container">
            <div class="chart-title">磁盘使用率历史</div>
            <div ref="diskChartRef" class="chart" />
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="actions-section">
        <el-button type="primary" :icon="Monitor" @click="handleSshClick">连接 SSH</el-button>
        <el-button type="success" :icon="ChatLineSquare" @click="handleAiClick">AI 帮助</el-button>
      </div>
    </div>

    <div v-else class="loading">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- SSH 凭证对话框 -->
    <SshCredentialDialog
      v-model="sshCredentialDialogVisible"
      :agent-id="host?.agentId || ''"
      :hostname="host?.hostname || ''"
      @connect="handleSshConnected"
    />

    <!-- SSH 终端对话框 -->
    <SshTerminalDialog
      v-model="sshTerminalDialogVisible"
      :session-id="sshSessionId"
      :host="host"
      @close="handleSshClose"
    />

    <!-- AI 助手对话框 -->
    <AiAssistantDialog
      ref="aiAssistantDialogRef"
      :ssh-session-id="sshSessionId || undefined"
      :agent-id="host?.agentId || ''"
      @connected="handleAiConnected"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted, nextTick, shallowRef } from 'vue'
import * as echarts from 'echarts'
import { Monitor, ChatLineSquare } from '@element-plus/icons-vue'
import SshCredentialDialog from './SshCredentialDialog.vue'
import SshTerminalDialog from './SshTerminalDialog.vue'
import AiAssistantDialog from '../ai/AiAssistantDialog.vue'
import type { Agent, AgentMetrics, TimeRange, MetricType, MetricsHistoryResponse } from '@/types/monitor'
import { getLatestMetrics, getMetricsHistory } from '@/api/monitor'
import { ElMessage } from 'element-plus'

interface Props {
  modelValue: boolean
  host: Agent | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

// 响应式数据
const visible = ref(props.modelValue)
const metrics = ref<AgentMetrics | null>(null)
const currentTimeRange = ref<TimeRange>('5MIN')
const loadingHistory = ref<MetricType | null>(null)

// SSH 相关状态
const sshCredentialDialogVisible = ref(false)
const sshSessionId = ref<string | null>(null)
const sshTerminalDialogVisible = ref(false)

// AI 助手相关状态
const aiAssistantDialogRef = ref<InstanceType<typeof AiAssistantDialog> | null>(null)
const aiSessionId = ref<string>('')

// 图表引用
const cpuChartRef = ref<HTMLDivElement>()
const memoryChartRef = ref<HTMLDivElement>()
const diskChartRef = ref<HTMLDivElement>()

// 图表实例（使用 shallowRef 避免深层响应式）
const cpuChart = shallowRef<echarts.ECharts | null>(null)
const memoryChart = shallowRef<echarts.ECharts | null>(null)
const diskChart = shallowRef<echarts.ECharts | null>(null)

// 定时器
let pollTimer: ReturnType<typeof setInterval> | null = null

// 监听 modelValue 变化
watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
    if (val && props.host) {
      // 对话框打开时初始化
      initDialog()
    } else {
      // 对话框关闭时清理
      cleanup()
    }
  },
)

// 监听 visible 变化同步给父组件
watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 监听时间范围变化
watch(currentTimeRange, () => {
  loadAllHistory()
})

// 初始化对话框
async function initDialog() {
  await loadLatestMetrics()
  await nextTick()
  initCharts()
  await loadAllHistory()
  startPolling()
}

// 加载最新监控指标
async function loadLatestMetrics() {
  if (!props.host) return

  try {
    metrics.value = await getLatestMetrics(props.host.agentId)
  } catch (error) {
    console.error('获取最新监控指标失败:', error)
    ElMessage.warning('获取监控数据失败')
  }
}

// 加载历史数据
async function loadHistory(metricType: MetricType) {
  if (!props.host) return

  loadingHistory.value = metricType
  try {
    const data: MetricsHistoryResponse = await getMetricsHistory(
      props.host.agentId,
      metricType,
      currentTimeRange.value,
    )
    updateChart(metricType, data)
  } catch (error) {
    console.error(`获取${metricType}历史数据失败:`, error)
    ElMessage.warning(`获取${metricType}历史数据失败`)
  } finally {
    loadingHistory.value = null
  }
}

// 加载所有历史数据
async function loadAllHistory() {
  await Promise.all([loadHistory('cpu'), loadHistory('memory'), loadHistory('disk')])
}

// 初始化图表
function initCharts() {
  if (cpuChartRef.value) {
    cpuChart.value = echarts.init(cpuChartRef.value)
  }
  if (memoryChartRef.value) {
    memoryChart.value = echarts.init(memoryChartRef.value)
  }
  if (diskChartRef.value) {
    diskChart.value = echarts.init(diskChartRef.value)
  }

  // 窗口大小改变时重绘图表
  window.addEventListener('resize', handleResize)
}

// 更新图表
function updateChart(metricType: MetricType, data: MetricsHistoryResponse) {
  const chart = metricType === 'cpu' ? cpuChart.value : metricType === 'memory' ? memoryChart.value : diskChart.value
  if (!chart) return

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const param = params[0]
        return `${param.axisValue}<br/>${metricType.toUpperCase()}: ${param.value}%`
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: data.timestamps.map(formatTime),
      boundaryGap: false,
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      name: '使用率 (%)',
    },
    series: [
      {
        name: metricType.toUpperCase(),
        type: 'line',
        smooth: true,
        data: data.values,
        lineStyle: { width: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(37, 99, 235, 0.3)' },
            { offset: 1, color: 'rgba(37, 99, 235, 0.05)' },
          ]),
        },
      },
    ],
  }

  chart.setOption(option)
}

// 处理窗口大小改变
function handleResize() {
  cpuChart.value?.resize()
  memoryChart.value?.resize()
  diskChart.value?.resize()
}

// 开始定时轮询
function startPolling() {
  // 每15秒获取最新指标
  pollTimer = setInterval(() => {
    loadLatestMetrics()
  }, 15000)
}

// 清理资源
function cleanup() {
  // 停止定时轮询
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }

  // 销毁图表
  cpuChart.value?.dispose()
  cpuChart.value = null
  memoryChart.value?.dispose()
  memoryChart.value = null
  diskChart.value?.dispose()
  diskChart.value = null

  // 移除事件监听
  window.removeEventListener('resize', handleResize)

  // 重置数据
  metrics.value = null
}

// 对话框关闭
function handleClose() {
  visible.value = false
  cleanup()
}

// SSH 按钮点击
function handleSshClick() {
  sshCredentialDialogVisible.value = true
}

// SSH 连接成功回调
function handleSshConnected(sessionId: string) {
  sshSessionId.value = sessionId
  sshTerminalDialogVisible.value = true
  sshCredentialDialogVisible.value = false
}

// SSH 断开回调
function handleSshClose() {
  sshSessionId.value = null
  sshTerminalDialogVisible.value = false
}

// AI 帮助按钮点击
function handleAiClick() {
  if (!sshSessionId.value) {
    ElMessage.warning('请先连接 SSH 终端')
    return
  }
  aiAssistantDialogRef.value?.open()
}

// AI 助手连接成功回调
function handleAiConnected(sessionId: string) {
  aiSessionId.value = sessionId
  ElMessage.success('AI 助手已连接')
}

// 获取进度条颜色
function getProgressColor(percentage: number): string {
  if (percentage < 50) return '#67C23A'
  if (percentage < 80) return '#E6A23C'
  return '#F56C6C'
}

// 获取最高磁盘使用率
function getHighestDiskUsage(): number | null {
  if (!metrics.value?.diskUsages || metrics.value.diskUsages.length === 0) return null
  return Math.max(...metrics.value.diskUsages.map((d) => d.usagePercent))
}

// 获取最高磁盘使用率的挂载点
function getHighestDiskMount(): string | null {
  if (!metrics.value?.diskUsages || metrics.value.diskUsages.length === 0) return null
  const highest = metrics.value.diskUsages.reduce((prev, curr) =>
    curr.usagePercent > prev.usagePercent ? curr : prev,
  )
  return highest.mount
}

// 格式化日期
function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

// 格式化时间
function formatTime(timeStr: string): string {
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  // 小于1小时显示 HH:mm:ss
  if (diff < 3600000) {
    return date.toLocaleTimeString('zh-CN')
  }
  // 小于1天显示 MM-DD HH:mm
  if (diff < 86400000) {
    return `${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  }
  // 否则显示 MM-DD
  return `${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`
}

// 组件卸载时清理
onUnmounted(() => {
  cleanup()
})
</script>

<style scoped>
.host-detail {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 基本信息 */
.basic-info-section {
  padding: 0;
}

/* 实时监控指标 */
.metrics-section {
  padding: 0;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.metric-card {
  background-color: var(--bg-secondary);
  border-radius: 8px;
  padding: 16px;
}

.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.metric-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.metric-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}

.disk-info {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-bottom: 4px;
}

.network-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.network-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.network-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.network-value {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
}

/* 历史趋势 */
.history-section {
  padding: 0;
}

.time-range-selector {
  font-size: 12px;
}

.charts-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.chart-container {
  background-color: var(--bg-secondary);
  border-radius: 8px;
  padding: 16px;
}

.chart-title {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.chart {
  width: 100%;
  height: 200px;
}

/* 操作按钮 */
.actions-section {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.loading {
  padding: 20px 0;
}
</style>
