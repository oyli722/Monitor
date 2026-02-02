package com.hundred.monitor.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 运行时数据上报请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
     * 监控指标
     */
    private Metrics metrics;

    /**
     * 监控指标
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metrics {

        /**
         * CPU使用率（百分比）
         */
        private Double cpuPercent;

        /**
         * 内存使用率（百分比）
         */
        private Double memoryPercent;

        /**
         * 磁盘使用率列表
         */
        private List<DiskUsageInfo> diskUsages;

        /**
         * 网络上行速率（Mbps）
         */
        private Double networkUpMbps;

        /**
         * 网络下行速率（Mbps）
         */
        private Double networkDownMbps;

        /**
         * SSH服务状态
         */
        private SshStatus sshStatus;

        /**
         * SSH状态
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SshStatus {
            private Boolean running;
            private Boolean portListening;
            private Integer port;
        }

        /**
         * 磁盘使用信息
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DiskUsageInfo {
            private String mount;
            private Double usedPercent;
            private Long usedGb;
            private Long totalGb;
        }
    }
}
