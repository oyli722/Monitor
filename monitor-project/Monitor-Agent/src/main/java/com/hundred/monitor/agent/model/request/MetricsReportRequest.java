package com.hundred.monitor.agent.model.request;

import com.hundred.monitor.agent.model.entity.Metrics;
import lombok.Data;

/**
 * 运行时数据上报请求
 */
@Data
public class MetricsReportRequest {

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * 时间戳
     */
    private String timestamp;

    /**
     * 运行时指标数据
     */
    private Metrics metrics;
}
