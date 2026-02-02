/**
 * 监控相关类型定义
 */

// 系统统计
export interface SystemStats {
  totalHosts: number
}

// Agent 主机信息
export interface Agent {
  agentId: string
  agentName: string
  hostname: string
  ip: string
  cpuModel: string
  cpuCores: number
  memoryGb: number
  gpuInfo: string
  networkInterfaces: string
  registeredAt: string
  updatedAt: string
}

// 监控图表配置
export interface ChartConfig {
  title: string
  type: 'line' | 'bar' | 'pie' | 'gauge'
  data: any[]
  options?: any
}

// 告警规则
export interface AlertRule {
  id: string
  name: string
  type: 'cpu' | 'memory' | 'disk' | 'network'
  threshold: number
  condition: '>' | '<' | '=' | '>=' | '<='
  enabled: boolean
  hostIds?: string[]
  createdAt: string
}

// 告警记录
export interface AlertRecord {
  id: string
  ruleId: string
  hostId: string
  hostName: string
  metric: string
  value: number
  threshold: number
  level: 'info' | 'warning' | 'critical'
  message: string
  resolved: boolean
  createdAt: string
  resolvedAt?: string
}

// 磁盘使用信息
export interface DiskUsage {
  name: string
  mount: string
  totalGb: number
  usedGb: number
  usagePercent: number
}

// Agent 监控指标
export interface AgentMetrics {
  agentId: string
  cpuPercent: number
  memoryPercent: number
  diskUsages: DiskUsage[]
  networkUpMbps: number
  networkDownMbps: number
  sshRunning: boolean
  sshPortListening: boolean
  sshPort: number
  timestamp: string
}

// 时间范围枚举
export type TimeRange = '5MIN' | '1H' | '6H' | '24H' | '7D' | '1M'

// 指标类型枚举
export type MetricType = 'cpu' | 'memory' | 'disk'

// 历史数据响应
export interface MetricsHistoryResponse {
  timestamps: string[]
  values: number[]
  interval: string
}

// SSH 凭证
export interface SshCredential {
  hasCredential: boolean
  username?: string
}

// SSH 连接请求
export interface SshConnectRequest {
  agentId: string
  username: string
  password: string
  saveCredential: boolean
}

// SSH 连接响应
export interface SshConnectResponse {
  success: boolean
  sessionId: string
}

