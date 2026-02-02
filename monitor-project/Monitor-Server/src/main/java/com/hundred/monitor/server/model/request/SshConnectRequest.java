package com.hundred.monitor.server.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSH连接请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SshConnectRequest {

    /**
     * Agent ID
     */
    @NotBlank(message = "Agent ID不能为空")
    private String agentId;

    /**
     * SSH用户名
     */
    @NotBlank(message = "SSH用户名不能为空")
    private String username;

    /**
     * SSH密码
     */
    @NotBlank(message = "SSH密码不能为空")
    private String password;
}
