package com.hundred.monitor.server.controller;

import com.hundred.monitor.server.model.request.EmailCodeRequest;
import com.hundred.monitor.server.model.request.ForgetPasswordRequest;
import com.hundred.monitor.server.model.request.LoginRequest;
import com.hundred.monitor.server.model.request.RegisterRequest;
import com.hundred.monitor.server.model.response.BaseResponse;
import com.hundred.monitor.server.model.response.ForgetPasswordResponse;
import com.hundred.monitor.server.model.response.LoginResponse;
import com.hundred.monitor.server.model.response.RegisterResponse;
import com.hundred.monitor.server.service.AuthService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.function.Supplier;

/**
 * 认证控制器 - 处理用户登录、登出等认证相关请求
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @Resource
    AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public BaseResponse<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = authService.login(loginRequest);
        return BaseResponse.success(loginResponse);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public BaseResponse<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return BaseResponse.success();
    }

    /**
     *  用户注册
     */
    @PostMapping("/register")
    public BaseResponse<RegisterResponse> register(@RequestBody @Valid RegisterRequest registerRequest) {
        RegisterResponse registerResponse = authService.register(registerRequest);
        return registerResponse.getSuccess()?BaseResponse.success(registerResponse):BaseResponse.error(registerResponse.getMessage());
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    public BaseResponse<LoginResponse> refreshToken(@RequestHeader("Authorization") String token) {
        // TODO: 实现令牌刷新逻辑
        LoginResponse loginResponse = authService.refreshToken(token);
        return BaseResponse.success(loginResponse);
    }

    /**
     * 验证令牌
     */
    @GetMapping("/validate")
    public BaseResponse<Boolean> validateToken(@RequestHeader("Authorization") String token) {
        boolean isValid = authService.validateToken(token);
        return BaseResponse.success(isValid);
    }

    @PostMapping("/reset-password")
    public BaseResponse<ForgetPasswordResponse> resetPassword(@RequestBody @Valid ForgetPasswordRequest forgetPasswordRequest) {
        ForgetPasswordResponse forgetPasswordResponse = authService.resetPassword(forgetPasswordRequest);
        return forgetPasswordResponse.getSuccess()?BaseResponse.success(forgetPasswordResponse):BaseResponse.error(forgetPasswordResponse.getMessage());
    }

    /**
     * 请求邮件验证码
     * @param emailCodeRequest 请求邮件
     *  类型 1-注册 2-修改密码
     * @param request 请求
     * @return 是否请求成功
     */
    @PostMapping("/ask-code")
    public BaseResponse<Void> askVerifyCode(@RequestBody @Valid EmailCodeRequest emailCodeRequest,
                                            HttpServletRequest request){
        return this.messageHandle(() ->
                authService.registerEmailVerifyCode(emailCodeRequest.getType(), String.valueOf(emailCodeRequest.getEmail()), request.getRemoteAddr()));
    }



    /**
     * 针对于返回值为String作为错误信息的方法进行统一处理
     * @param action 具体操作
     * @return 响应结果
     * @param <T> 响应结果类型
     */
    private <T> BaseResponse<T> messageHandle(Supplier<String> action){
        String message = action.get();
        if(message == null)
            return BaseResponse.success();
        else
            return BaseResponse.error(400, message);
    }

}
