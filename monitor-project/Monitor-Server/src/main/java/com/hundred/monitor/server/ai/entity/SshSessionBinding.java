package com.hundred.monitor.server.ai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSH会话与AI会话绑定关系实体
 * 存储在Redis中，表示AI助手与SSH终端的关联
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SshSessionBinding {

    /**
     * AI会话ID（唯一标识）
     * Redis key格式: ai:ssh:binding:{aiSessionId}
     */
    private String aiSessionId;

    /**
     * SSH会话ID
     * 对应SSH终端WebSocket的session ID
     */
    private String sshSessionId;

    /**
     * 主机ID
     * 用于AI获取主机上下文信息
     */
    private String agentId;

    /**
     * 用户ID
     * 用于权限控制和会话隔离
     */
    private String userId;

    /**
     * 创建时间戳
     * 用于会话排序和过期清理
     */
    private Long createdAt;

    /**
     * 创建新的绑定关系
     *
     * @param aiSessionId  AI会话ID
     * @param sshSessionId SSH会话ID
     * @param agentId      主机ID
     * @param userId       用户ID
     * @return 绑定关系对象
     */
    public static SshSessionBinding create(String aiSessionId, String sshSessionId, String agentId, String userId) {
        return SshSessionBinding.builder()
                .aiSessionId(aiSessionId)
                .sshSessionId(sshSessionId)
                .agentId(agentId)
                .userId(userId)
                .createdAt(System.currentTimeMillis())
                .build();
    }

    /**
     * 检查绑定是否过期
     *
     * @param ttlMillis 过期时间（毫秒）
     * @return true表示已过期
     */
    public boolean isExpired(long ttlMillis) {
        return System.currentTimeMillis() - createdAt > ttlMillis;
    }
}
