package com.hundred.monitor.server.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * AI回复内容
     */
    private String reply;

    /**
     * AI回复消息（完整格式）
     */
    private ChatMessageResponse message;
}
