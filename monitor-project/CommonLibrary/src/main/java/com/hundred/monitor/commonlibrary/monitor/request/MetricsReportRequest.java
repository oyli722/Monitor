package com.hundred.monitor.commonlibrary.monitor.request;

import com.hundred.monitor.commonlibrary.monitor.model.Metrics;
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
