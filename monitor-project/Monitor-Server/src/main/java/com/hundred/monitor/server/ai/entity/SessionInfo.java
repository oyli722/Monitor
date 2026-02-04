package com.hundred.monitor.server.ai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话信息实体（Redis映射）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfo {
    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 会话标题（首条消息摘要）
     */
    private String title;

    /**
     * 创建时间（时间戳）
     */
    private Long createdAt;

    /**
     * 更新时间（时间戳）
     */
    private Long updatedAt;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * 总结内容（超量后生成）
     */
    private String summary;

    /**
     * 关联的主机ID（可选）
     */
    private String linkedAgentId;

    /**
     * 上次总结时间（时间戳）
     */
    private Long lastSummaryAt;
}
