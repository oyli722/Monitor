package com.hundred.monitor.ai.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundred.monitor.commonlibrary.common.BaseResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.hundred.monitor.ai.security.ReactiveAuthenticationEntryPoint.getVoidMono;

/**
 * 访问拒绝处理器 - 处理403权限不足响应
 */
@Slf4j
@Component
public class ReactiveAccessDeniedHandler implements ServerAccessDeniedHandler {

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException deniedException) {
        log.warn("访问被拒绝: {}", deniedException.getMessage());

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        BaseResponse<Void> errorResponse = BaseResponse.error(
                HttpStatus.FORBIDDEN.value(),
                "权限不足，无法访问此资源"
        );

        return getVoidMono(response, errorResponse, objectMapper, log);
    }
}
