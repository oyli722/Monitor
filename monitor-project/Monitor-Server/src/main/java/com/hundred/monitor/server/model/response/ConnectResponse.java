package com.hundred.monitor.server.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI助手连接响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectResponse {

    /**
     * AI会话ID
     * 用于建立WebSocket连接和后续通信
     */
    private String aiSessionId;

    /**
     * 连接状态提示
     */
    private String message;
}
