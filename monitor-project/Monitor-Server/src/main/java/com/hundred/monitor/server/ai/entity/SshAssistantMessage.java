package com.hundred.monitor.server.ai.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天消息实体（场景B：SSH绑定助手）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SshAssistantMessage {
    /**
     * 消息角色：user 或 assistant
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息时间戳
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Long timestamp;
}
