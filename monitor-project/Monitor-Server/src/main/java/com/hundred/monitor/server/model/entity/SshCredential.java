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
 * SSH凭证实体类
 * 对应 ssh_credential 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ssh_credential")
public class SshCredential {

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
     * SSH用户名
     */
    @TableField("username")
    private String username;

    /**
     * SSH密码（加密存储）
     */
    @TableField("password")
    private String password;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
