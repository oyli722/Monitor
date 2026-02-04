package com.hundred.monitor.server.ai.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket聊天消息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsChatMessage {

    /**
     * 消息类型：chat, reply, error, ping
     */
    private String type;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 错误码（仅type=error时使用）
     */
    private String errorCode;

    /**
     * 创建聊天消息
     */
    public static WsChatMessage chat(String content) {
        return WsChatMessage.builder()
                .type("chat")
                .content(content)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建回复消息
     */
    public static WsChatMessage reply(String content) {
        return WsChatMessage.builder()
                .type("reply")
                .content(content)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建错误消息
     */
    public static WsChatMessage error(String errorCode, String content) {
        return WsChatMessage.builder()
                .type("error")
                .errorCode(errorCode)
                .content(content)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建心跳消息
     */
    public static WsChatMessage ping() {
        return WsChatMessage.builder()
                .type("ping")
                .content("pong")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
