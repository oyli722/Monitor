package com.hundred.monitor.server.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent注册响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRegisterResponse {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * Agent名称
     */
    private String agentName;

    /**
     * 认证Token
     */
    private String authToken;

    /**
     * Token过期时间
     */
    private String tokenExpires;
}
