package com.hundred.monitor.ai.handler;

import com.hundred.monitor.ai.exception.AiServiceException;
import com.hundred.monitor.ai.exception.BaseException;
import com.hundred.monitor.ai.exception.BusinessException;
import com.hundred.monitor.ai.exception.ChatSessionNotFoundException;
import com.hundred.monitor.ai.constant.ErrorConstants;
import com.hundred.monitor.commonlibrary.common.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Mono<BaseResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Mono.just(e.toResponse());
    }

    /**
     * 会话不存在异常
     */
    @ExceptionHandler(ChatSessionNotFoundException.class)
    public Mono<BaseResponse<Void>> handleChatSessionNotFoundException(ChatSessionNotFoundException e) {
        log.warn("会话不存在: {}", e.getMessage());
        return Mono.just(e.toResponse());
    }

    /**
     * AI服务异常
     */
    @ExceptionHandler(AiServiceException.class)
    public Mono<BaseResponse<Void>> handleAiServiceException(AiServiceException e) {
        log.error("AI服务异常", e);
        return Mono.just(e.toResponse());
    }

    /**
     * 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<BaseResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return Mono.just(BaseResponse.badRequest(e.getMessage()));
    }

    /**
     * 通用异常
     */
    @ExceptionHandler(Exception.class)
    public Mono<BaseResponse<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return Mono.just(BaseResponse.error(ErrorConstants.AI_SERVICE_UNAVAILABLE));
    }
}
