package com.hundred.monitor.server.model.request;

import lombok.*;

/**
 * 监控指标历史数据查询请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsHistoryRequest {

    /**
     * 指标类型：cpu/memory/disk
     */
    private String metricType;

    /**
     * 时间范围：5MIN/1H/6H/24H/7D/1M
     */
    private String timeRange;

    /**
     * 指标类型枚举
     */
    @Getter
    public enum MetricType {
        CPU("cpu"),
        MEMORY("memory"),
        DISK("disk");

        private final String value;

        MetricType(String value) {
            this.value = value;
        }

        public static MetricType fromValue(String value) {
            for (MetricType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid metric type: " + value);
        }
    }

    /**
     * 时间范围枚举
     */
    @Getter
    public enum TimeRange {
        FIVE_MIN("5MIN", 5, "10s"),
        ONE_HOUR("1H", 60, "1m"),
        SIX_HOURS("6H", 360, "5m"),
        ONE_DAY("24H", 1440, "15m"),
        SEVEN_DAYS("7D", 10080, "1h"),
        ONE_MONTH("1M", 43200, "1d");

        private final String value;
        private final int minutes;
        private final String interval;

        TimeRange(String value, int minutes, String interval) {
            this.value = value;
            this.minutes = minutes;
            this.interval = interval;
        }

        public static TimeRange fromValue(String value) {
            for (TimeRange range : values()) {
                if (range.value.equals(value)) {
                    return range;
                }
            }
            throw new IllegalArgumentException("Invalid time range: " + value);
        }
    }
}
