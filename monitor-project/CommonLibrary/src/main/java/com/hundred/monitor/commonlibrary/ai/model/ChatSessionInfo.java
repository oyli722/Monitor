package com.hundred.monitor.commonlibrary.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话信息实体（侧边栏AI助手 - HTTP REST API）
 *
 * 用于记录AI聊天会话的基本信息和状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionInfo {
    /**
     * 会话ID（UUID格式）
     */
    private String sessionId;

    /**
     * 会话标题（首条消息摘要，最多20字符）
     */
    private String title;

    /**
     * 创建时间（时间戳，毫秒）
     */
    private Long createdAt;

    /**
     * 更新时间（时间戳，毫秒）
     */
    private Long updatedAt;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * 总结内容（消息超量后AI生成的对话总结）
     */
    private String summary;

    /**
     * 关联的主机ID（可选，用于上下文关联）
     */
    private String linkedAgentId;

    /**
     * 上次总结时间（时间戳，毫秒）
     */
    private Long lastSummaryAt;
}
