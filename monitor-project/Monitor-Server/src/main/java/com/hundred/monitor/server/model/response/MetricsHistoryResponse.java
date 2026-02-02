package com.hundred.monitor.server.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 监控指标历史数据响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsHistoryResponse {

    /**
     * 时间点数组
     */
    private List<String> timestamps;

    /**
     * 指标值数组
     */
    private List<Double> values;

    /**
     * 聚合间隔
     */
    private String interval;
}
