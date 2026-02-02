package com.hundred.monitor.server.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSH命令请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SshCommandRequest {

    /**
     * 会话ID（WebSocket会话标识）
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /**
     * SSH命令
     */
    private String command;
}
