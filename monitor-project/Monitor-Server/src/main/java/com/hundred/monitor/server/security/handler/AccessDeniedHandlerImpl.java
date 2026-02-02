package com.hundred.monitor.server.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundred.monitor.server.model.response.BaseResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 访问拒绝处理器 - 处理已认证用户无权限访问资源的情况
 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                      HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        BaseResponse<?> baseResponse = BaseResponse.error(403, "无权限访问该资源");

        response.getWriter().write(objectMapper.writeValueAsString(baseResponse));
    }
}
