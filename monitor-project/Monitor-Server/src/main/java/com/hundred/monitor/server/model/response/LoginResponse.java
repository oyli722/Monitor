package com.hundred.monitor.server.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * JWT令牌
     */
    private String token;

    /**
     * 令牌类型
     */
    private String tokenType;

    /**
     * 用户名
     */
    private String username;

    /**
     * 过期时间（时间戳）
     */
    private Long expirationTime;

    /**
     * 用户角色
     */
    private String role;

    /**
     * 权限列表
     */
    private String[] permissions;
}
