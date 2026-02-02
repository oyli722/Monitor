package com.hundred.monitor.agent.model.entity;

import lombok.Data;

import java.util.List;

/**
 * 运行时数据模型
 * 包含CPU、内存、磁盘、网络等实时指标
 */
@Data
public class Metrics {

    /**
     * CPU使用率（百分比）
     */
    private Double cpuPercent;

    /**
     * 内存使用率（百分比）
     */
    private Double memoryPercent;

    /**
     * 磁盘使用率列表（每块磁盘单独上报）
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
    public static class SshStatus {
        private Boolean running;
        private Boolean portListening;
        private Integer port;
    }

    /**
     * 磁盘使用信息
     */
    @Data
    public static class DiskUsageInfo {
        private String mount;
        private Double usedPercent;
        private Long usedGb;
        private Long totalGb;
    }
}
