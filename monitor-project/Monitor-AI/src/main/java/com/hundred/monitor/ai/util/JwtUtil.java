package com.hundred.monitor.ai.util;

import com.hundred.monitor.ai.constant.ChatConstants;
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

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    private SecretKey key;

    /**
     * 初始化密钥
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成Token
     */
    public String generateToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 从Token中获取用户ID
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims != null ? claims.getSubject() : null;
        } catch (Exception e) {
            log.warn("解析Token失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证Token
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims != null && !isTokenExpired(claims);
        } catch (Exception e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从Authorization头中提取Token
     */
    public String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(ChatConstants.BEARER_PREFIX)) {
            return authHeader.substring(ChatConstants.BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 从Authorization头中获取用户ID
     */
    public String getUserIdFromAuth(String authHeader) {
        String token = extractToken(authHeader);
        if (token != null && validateToken(token)) {
            return getUserIdFromToken(token);
        }
        return ChatConstants.DEFAULT_USER_ID;
    }

    /**
     * 从JWT令牌中获取Claims
     */
    private Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.warn("解析JWT令牌失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查令牌是否过期
     */
    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }
}
