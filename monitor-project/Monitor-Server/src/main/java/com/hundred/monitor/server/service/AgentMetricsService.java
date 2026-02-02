package com.hundred.monitor.server.service;

import com.hundred.monitor.server.model.request.MetricsReportRequest;

/**
 * Agent监控数据服务接口
 */
public interface AgentMetricsService {

    /**
     * 保存监控数据
     *
     * @param request 监控数据上报请求
     */
    void saveMetrics(MetricsReportRequest request);
}
