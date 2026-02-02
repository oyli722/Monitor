package com.hundred.monitor.server.service;

import com.hundred.monitor.server.model.response.AgentBasicInfoResponse;
import com.hundred.monitor.server.model.response.MetricsHistoryResponse;
import com.hundred.monitor.server.model.response.MetricsResponse;
import com.hundred.monitor.server.model.request.MetricsHistoryRequest;

public interface MonitorService {
    AgentBasicInfoResponse getMonitorList();
    MetricsResponse getLatestMetrics(String agentId);
    MetricsHistoryResponse getMetricsHistory(String agentId, MetricsHistoryRequest request);
}
