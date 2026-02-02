package com.hundred.monitor.server.service;

import com.hundred.monitor.server.model.request.ForgetPasswordRequest;
import com.hundred.monitor.server.model.request.LoginRequest;
import com.hundred.monitor.server.model.request.RegisterRequest;
import com.hundred.monitor.server.model.response.ForgetPasswordResponse;
import com.hundred.monitor.server.model.response.LoginResponse;
import com.hundred.monitor.server.model.response.RegisterResponse;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求对象
     * @return 登录响应对象，包含token等信息
     */
    LoginResponse login(LoginRequest loginRequest);

    /**
     * 注册邮件验证码
     * @param type 验证码类型
     * @param email 邮箱
     * @param address 请求IP
     * @return 验证码
     */
    String registerEmailVerifyCode(String type, String email, String address);

    /**
     * 用户登出
     *
     * @param token JWT令牌
     */
    void logout(String token);

    /**
     * 刷新令牌
     *
     * @param token 旧的JWT令牌
     * @return 新的登录响应对象
     */
    LoginResponse refreshToken(String token);

    /**
     * 验证令牌
     *
     * @param token JWT令牌
     * @return 验证结果，true表示有效
     */
    boolean validateToken(String token);

    /**
     * 检查用户是否存在
     *
     * @param username 用户名
     * @return 存在返回true
     */
    boolean userExists(String username);

    /**
     * 验证用户密码
     *
     * @param username 用户名
     * @param password 密码
     * @return 验证结果，true表示正确
     */
    boolean validatePassword(String username, String password);

    RegisterResponse register(RegisterRequest registerRequest);

    ForgetPasswordResponse resetPassword(ForgetPasswordRequest forgetPasswordRequest);
}
