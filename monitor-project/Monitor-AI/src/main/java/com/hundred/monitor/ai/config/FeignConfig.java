package com.hundred.monitor.ai.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Retryer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign配置类
 * 配置Feign客户端的行为
 */
@Slf4j
@Configuration
public class FeignConfig {

    /**
     * Feign日志级别
     * NONE: 不显示日志
     * BASIC: 显示请求方法、URL、响应状态码、执行时间
     * HEADERS: 显示请求头和响应头
     * FULL: 显示全部信息（包括请求体、响应体）
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Feign重试策略
     * 不使用重试，避免服务间调用延迟
     */
    @Bean
    public Retryer feignRetryer() {
        return Retryer.NEVER_RETRY;
    }

    /**
     * Feign请求拦截器
     * 可以在这里添加统一的请求头（如认证Token）
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // 添加Content-Type
            template.header("Content-Type", "application/json");

            // 这里可以添加从请求上下文中获取的认证Token
            // String token = getTokenFromContext();
            // if (token != null) {
            //     template.header("Authorization", "Bearer " + token);
            // }

            log.debug("Feign请求: {} {}", template.method(), template.url());
        };
    }
}
