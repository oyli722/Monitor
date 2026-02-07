package com.hundred.monitor.server.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent信息DTO（供AI服务调用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentInfoDTO {

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
     * 在线状态
     */
    private Boolean online;

    /**
     * 注册时间
     */
    private Long registeredAt;
}
