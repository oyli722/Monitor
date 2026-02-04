package com.hundred.monitor.server.ai.entity.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantConnectResponse {
    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 会话ID（用于WebSocket连接）
     */
    private String sessionId;
}
