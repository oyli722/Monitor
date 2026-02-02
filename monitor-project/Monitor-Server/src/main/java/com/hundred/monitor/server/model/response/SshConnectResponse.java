package com.hundred.monitor.server.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSH连接响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SshConnectResponse {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 会话ID（用于WebSocket连接）
     */
    private String sessionId;
}
