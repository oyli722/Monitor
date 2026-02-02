package com.hundred.monitor.agent.model.response;

import lombok.Data;

/**
 * 注册响应模型
 */
@Data
public class RegisterResponse {

    /**
     * 请求是否成功
     */
    private Boolean success;

    /**
     * 分配的Agent ID
     */
    private String agentId;

    /**
     * 分配的Agent名称
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
