package com.hundred.monitor.ai.security;

import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

/**
 * Monitor-AI Reactive Security配置
 * 使用JWT进行认证，所有端点都需要认证
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class MonitorAISecurityConfig {

    @Value("${jwt.secret:monitor-server-jwt-secret-key-for-hmac-sha256-algorithm-at-least-32-bytes}")
    private String jwtSecret;

    private final ReactiveAuthenticationEntryPoint authenticationEntryPoint;
    private final ReactiveAccessDeniedHandler accessDeniedHandler;
    private final ReactiveJwtAuthenticationConverter jwtAuthenticationConverter;

    public MonitorAISecurityConfig(
            ReactiveAuthenticationEntryPoint authenticationEntryPoint,
            ReactiveAccessDeniedHandler accessDeniedHandler,
            ReactiveJwtAuthenticationConverter jwtAuthenticationConverter) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    /**
     * 配置Security过滤器链
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        log.info("配置Monitor-AI Security过滤器链");

        http
                // 禁用CSRF（使用JWT无需CSRF保护）
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // 禁用formLogin（使用JWT认证）
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // 禁用httpBasic（使用JWT认证）
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                // 配置CORS（由独立的CorsWebFilter处理）
                .cors(ServerHttpSecurity.CorsSpec::disable)

                // 配置JWT资源服务器
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)
                                .jwtDecoder(jwtDecoder())
                        )
                        .authenticationEntryPoint(authenticationEntryPoint)
                )

                // 配置异常处理
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                // 配置授权规则 - OPTIONS请求允许CORS预检，其他需要认证
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()  // 允许CORS预检请求
                        .anyExchange().authenticated()
                );

        return http.build();
    }

    /**
     * JWT解码器 - 使用对称密钥
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
    }

    /**
     * CORS过滤器
     * 提供跨域支持
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:8080"
        ));
        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE",
                "OPTIONS", "PATCH"
        ));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
