package com.hundred.monitor.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent实体类
 * 对应 agent 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent")
public class Agent {

    /**
     * Agent ID，主键
     */
    @TableId(value = "agent_id", type = IdType.INPUT)
    private String agentId;

    /**
     * Agent名称
     */
    @TableField("agent_name")
    private String agentName;

    /**
     * 主机名
     */
    @TableField("hostname")
    private String hostname;

    /**
     * IP地址
     */
    @TableField("ip")
    private String ip;

    /**
     * CPU型号
     */
    @TableField("cpu_model")
    private String cpuModel;

    /**
     * CPU核心数
     */
    @TableField("cpu_cores")
    private Integer cpuCores;

    /**
     * 内存总量(GB)
     */
    @TableField("memory_gb")
    private Long memoryGb;

    /**
     * GPU信息(JSON数组)
     */
    @TableField("gpu_info")
    private String gpuInfo;

    /**
     * 网络接口列表(JSON数组)
     */
    @TableField("network_interfaces")
    private String networkInterfaces;

    /**
     * 注册时间
     */
    @TableField("registered_at")
    private LocalDateTime registeredAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
