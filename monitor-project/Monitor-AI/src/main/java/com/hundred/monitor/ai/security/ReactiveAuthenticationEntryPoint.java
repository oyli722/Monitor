package com.hundred.monitor.ai.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundred.monitor.commonlibrary.common.BaseResponse;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 认证入口点 - 处理401未授权响应
 */
@Slf4j
@Component
public class ReactiveAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException authException) {
        log.warn("认证失败: {}", authException.getMessage());

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        BaseResponse<Void> errorResponse = BaseResponse.error(
                HttpStatus.UNAUTHORIZED.value(),
                "未授权访问，请提供有效的JWT token"
        );

        return getVoidMono(response, errorResponse, objectMapper, log);
    }

    @NotNull
    static Mono<Void> getVoidMono(ServerHttpResponse response, BaseResponse<Void> errorResponse, ObjectMapper objectMapper, Logger log) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("构建错误响应失败", e);
            return response.setComplete();
        }
    }
}
