package com.hundred.monitor.server.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent详情DTO（供AI服务调用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentDetailDTO {

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * 主机名
     */
    private String hostname;

    /**
     * IP地址
     */
    private String ip;

    /**
     * CPU型号
     */
    private String cpuModel;

    /**
     * CPU核心数
     */
    private Integer cpuCores;

    /**
     * 内存大小（GB）
     */
    private Double memoryGb;

    /**
     * GPU信息（JSON格式）
     */
    private String gpuInfo;

    /**
     * 网络接口（JSON格式）
     */
    private String networkInterfaces;

    /**
     * 在线状态
     */
    private Boolean online;

    /**
     * SSH运行状态
     */
    private Boolean sshRunning;

    /**
     * SSH监听端口
     */
    private Integer sshPort;

    /**
     * 注册时间
     */
    private Long registeredAt;

    /**
     * 更新时间
     */
    private Long updatedAt;
}
