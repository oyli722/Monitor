package com.hundred.monitor.server.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundred.monitor.server.model.response.BaseResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 认证失败处理器 - 处理未认证用户访问受保护资源的情况
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        // TODO: 实现未认证异常处理逻辑
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // TODO: 构建响应对象
        BaseResponse<?> baseResponse = BaseResponse.error(401, "未认证，请先登录");

        // TODO: 写入响应
        response.getWriter().write(objectMapper.writeValueAsString(baseResponse));
    }
}
