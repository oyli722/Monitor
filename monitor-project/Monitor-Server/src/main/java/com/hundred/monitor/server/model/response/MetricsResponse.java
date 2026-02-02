package com.hundred.monitor.server.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 监控指标响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsResponse {

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * CPU使用率(%)
     */
    private Double cpuPercent;

    /**
     * 内存使用率(%)
     */
    private Double memoryPercent;

    /**
     * 磁盘使用信息列表
     */
    private List<DiskUsage> diskUsages;

    /**
     * 网络上行速率(Mbps)
     */
    private Double networkUpMbps;

    /**
     * 网络下行速率(Mbps)
     */
    private Double networkDownMbps;

    /**
     * SSH进程是否运行
     */
    private Boolean sshRunning;

    /**
     * SSH端口是否监听
     */
    private Boolean sshPortListening;

    /**
     * SSH端口
     */
    private Integer sshPort;

    /**
     * 采集时间戳
     */
    private String timestamp;

    /**
     * 磁盘使用信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiskUsage {
        /**
         * 挂载点
         */
        private String mount;

        /**
         * 总容量(GB)
         */
        private Long totalGb;

        /**
         * 已用容量(GB)
         */
        private Long usedGb;

        /**
         * 使用率(%)
         */
        private Double usagePercent;

        /**
         * 分区名称
         */
        private String name;
    }
}
