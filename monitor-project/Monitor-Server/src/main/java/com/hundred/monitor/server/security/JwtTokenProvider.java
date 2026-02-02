package com.hundred.monitor.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT令牌提供者 - 负责JWT的生成和解析
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret:monitor-server-secret-key}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    /**
     * 生成JWT令牌
     */
    public String generateToken(String username) {
        // TODO: 实现JWT令牌生成逻辑
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        // TODO: 添加自定义声明

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
        log.info("生成JWT令牌，用户名：{}，过期时间：{}", username, expiryDate);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        // TODO: 实现从令牌中解析用户名
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseClaimsJws(token)
            .getBody();

        return claims.getSubject();
    }

    /**
     * 验证令牌
     */
    public boolean validateToken(String token) {
        // TODO: 实现令牌验证逻辑
        try {
            Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // TODO: 处理各种异常（过期、格式错误等）
            log.error("令牌验证失败", e);
            return false;
        }
    }

    /**
     * 获取令牌过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        // TODO: 实现获取过期时间逻辑
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseClaimsJws(token)
            .getBody();

        return claims.getExpiration();
    }

    /**
     * 检查令牌是否已过期
     */
    public boolean isTokenExpired(String token) {
        // TODO: 实现检查过期逻辑
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 获取JWT过期时间（毫秒）
     */
    public long getJwtExpirationInMs() {
        return jwtExpirationInMs;
    }
}
