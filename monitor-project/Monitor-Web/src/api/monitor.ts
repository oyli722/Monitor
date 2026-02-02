/**
 * 监控相关API
 */

import type {
  SystemStats,
  AlertRule,
  AlertRecord,
  Agent,
  AgentMetrics,
  MetricsHistoryResponse,
  TimeRange,
  MetricType,
  SshCredential,
  SshConnectRequest,
  SshConnectResponse,
} from '@/types/monitor'
import request from '@/utils/request'

/**
 * 获取监控主机列表响应
 */
interface MonitorListResponse {
  success: boolean
  basicInfoList: Agent[]
}

/**
 * 获取监控主机列表
 */
export function getMonitorList(): Promise<Agent[]> {
  return request.get<MonitorListResponse>('/monitor/getMonitorList').then(res => res.basicInfoList)
}

/**
 * 获取系统统计
 */
export function getSystemStats(): Promise<SystemStats> {
  return request.get<SystemStats>('/monitor/stats')
}

/**
 * 获取告警规则列表
 */
export function getAlertRules(): Promise<AlertRule[]> {
  return request.get<AlertRule[]>('/monitor/alert-rules')
}

/**
 * 创建告警规则
 */
export function createAlertRule(rule: Partial<AlertRule>): Promise<{ ruleId: string }> {
  return request.post<{ ruleId: string }>('/monitor/alert-rules', rule)
}

/**
 * 更新告警规则
 */
export function updateAlertRule(id: string, rule: Partial<AlertRule>): Promise<void> {
  return request.put<void>(`/monitor/alert-rules/${id}`, rule)
}

/**
 * 删除告警规则
 */
export function deleteAlertRule(id: string): Promise<void> {
  return request.delete<void>(`/monitor/alert-rules/${id}`)
}

/**
 * 获取告警记录
 */
export function getAlertRecords(params: {
  pageNum: number
  pageSize: number
  resolved?: boolean
}): Promise<{ list: AlertRecord[]; total: number }> {
  return request.get('/monitor/alert-records', { params })
}

/**
 * 获取主机最新监控指标
 */
export function getLatestMetrics(agentId: string): Promise<AgentMetrics> {
  return request.get<AgentMetrics>(`/monitor/${agentId}/metrics/latest`)
}

/**
 * 获取主机历史监控指标
 */
export function getMetricsHistory(
  agentId: string,
  metricType: MetricType,
  timeRange: TimeRange,
): Promise<MetricsHistoryResponse> {
  return request.get<MetricsHistoryResponse>(`/monitor/${agentId}/metrics/history`, {
    params: { metricType, timeRange },
  })
}

/**
 * 获取SSH凭证
 */
export function getSshCredential(agentId: string): Promise<SshCredential> {
  return request.get<SshCredential>(`/v1/ssh/credential/${agentId}`)
}

/**
 * SSH连接
 */
export function sshConnect(params: SshConnectRequest): Promise<SshConnectResponse> {
  return request.post<SshConnectResponse>('/v1/ssh/connect', params)
}

/**
 * SSH断开
 */
export function sshDisconnect(sessionId: string): Promise<void> {
  return request.post<void>('/v1/ssh/disconnect', { sessionId })
}

