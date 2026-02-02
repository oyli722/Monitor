package com.hundred.monitor.server.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSH凭证查询响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SshCredentialResponse {

    /**
     * 是否有保存的凭证
     */
    private Boolean hasCredential;

    /**
     * SSH用户名（有凭证时返回）
     */
    private String username;
}
