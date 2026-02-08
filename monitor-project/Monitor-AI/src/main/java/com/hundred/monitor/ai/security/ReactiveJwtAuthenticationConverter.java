package com.hundred.monitor.ai.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reactive JWT认证转换器
 * 将JWT token转换为Spring Security Authentication对象
 */
@Slf4j
@Component
public class ReactiveJwtAuthenticationConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    @Value("${jwt.secret:monitor-server-jwt-secret-key-for-hmac-sha256-algorithm-at-least-32-bytes}")
    private String jwtSecret;

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        log.debug("转换JWT为认证对象: {}", jwt.getSubject());

        // 注意：JWT签名验证由ReactiveJwtDecoder完成，这里不需要重复验证

        // 提取用户信息
        String username = jwt.getSubject();

        // 提取角色/权限
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        // 创建认证对象
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt,
                authorities,
                username
        );

        return Mono.just(authentication);
    }

    /**
     * 从JWT中提取权限/角色
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        try {
            // 尝试从scope claim获取权限
            Object scopes = jwt.getClaims().get("scope");
            if (scopes instanceof String scope) {
                String[] scopeArray = scope.split(" ");
                for (String s : scopeArray) {
                    authorities.add(new SimpleGrantedAuthority("SCOPE_" + s));
                }
                return authorities;
            }

            // 尝试从roles claim获取角色
            Object roles = jwt.getClaims().get("roles");
            if (roles instanceof List<?> roleList) {
                for (Object role : roleList) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toString()));
                }
                return authorities;
            }

            // 默认返回空权限列表
            return authorities;
        } catch (Exception e) {
            log.warn("提取JWT权限失败", e);
            return authorities;
        }
    }
}
