package com.hundred.monitor.commonlibrary.ai.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天消息实体（侧边栏AI助手 - HTTP REST API）
 *
 * 用于记录用户和AI助手之间的对话消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    /**
     * 消息角色：user 或 assistant
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息时间戳（毫秒）
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Long timestamp;
}
