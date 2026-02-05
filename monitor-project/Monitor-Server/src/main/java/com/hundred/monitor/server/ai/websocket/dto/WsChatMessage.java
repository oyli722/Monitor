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
     * 消息类型：chat, reply, error, ping, command_output, command_complete
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
     * 回复是否完成（仅type=reply时使用）
     * true: 消息完整，false: 流式消息中的片段
     */
    private Boolean isComplete;

    /**
     * 退出码（仅type=command_complete时使用）
     */
    private Integer exitCode;

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
     * 创建回复消息（默认完整消息）
     */
    public static WsChatMessage reply(String content) {
        return reply(content, true);
    }

    /**
     * 创建回复消息（指定是否完整）
     */
    public static WsChatMessage reply(String content, boolean isComplete) {
        return WsChatMessage.builder()
                .type("reply")
                .content(content)
                .timestamp(System.currentTimeMillis())
                .isComplete(isComplete)
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

    /**
     * 创建命令输出消息
     */
    public static WsChatMessage commandOutput(String content) {
        return WsChatMessage.builder()
                .type("command_output")
                .content(content)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建命令完成消息
     */
    public static WsChatMessage commandComplete(int exitCode) {
        return WsChatMessage.builder()
                .type("command_complete")
                .exitCode(exitCode)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
