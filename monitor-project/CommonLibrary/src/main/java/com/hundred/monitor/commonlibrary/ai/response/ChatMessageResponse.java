package com.hundred.monitor.commonlibrary.ai.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天消息响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

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
