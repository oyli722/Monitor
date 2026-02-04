package com.hundred.monitor.server.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI助手连接请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectRequest {

    /**
     * SSH会话ID
     * 从SSH终端WebSocket连接中获取
     */
    @NotBlank(message = "SSH会话ID不能为空")
    private String sshSessionId;

    /**
     * 主机ID
     * 用于AI获取主机上下文信息
     */
    @NotBlank(message = "主机ID不能为空")
    private String agentId;
}
