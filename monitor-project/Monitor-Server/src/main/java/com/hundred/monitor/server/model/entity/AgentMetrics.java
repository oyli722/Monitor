package com.hundred.monitor.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agent监控数据实体类
 * 对应 agent_metrics 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent_metrics")
public class AgentMetrics {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Agent ID
     */
    @TableField("agent_id")
    private String agentId;

    /**
     * CPU使用率(%)
     */
    @TableField("cpu_percent")
    private BigDecimal cpuPercent;

    /**
     * 内存使用率(%)
     */
    @TableField("memory_percent")
    private BigDecimal memoryPercent;

    /**
     * 磁盘使用信息(JSON数组)
     */
    @TableField("disk_usages")
    private String diskUsages;

    /**
     * 网络上行速率(Mbps)
     */
    @TableField("network_up_mbps")
    private BigDecimal networkUpMbps;

    /**
     * 网络下行速率(Mbps)
     */
    @TableField("network_down_mbps")
    private BigDecimal networkDownMbps;

    /**
     * SSH进程是否运行
     */
    @TableField("ssh_running")
    private Boolean sshRunning;

    /**
     * SSH端口是否监听
     */
    @TableField("ssh_port_listening")
    private Boolean sshPortListening;

    /**
     * SSH端口
     */
    @TableField("ssh_port")
    private Integer sshPort;

    /**
     * 采集时间戳
     */
    @TableField("timestamp")
    private LocalDateTime timestamp;
}
