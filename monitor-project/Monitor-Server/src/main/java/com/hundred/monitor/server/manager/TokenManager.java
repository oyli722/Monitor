package com.hundred.monitor.server.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token管理器 - 管理用户令牌的生命周期
 */
@Component
@Slf4j
public class TokenManager {

    /**
     * 用户名到Token的映射
     */
    private final Map<String, String> userTokenMap = new ConcurrentHashMap<>();

    /**
     * Token黑名单
     */
    private final Set<String> tokenBlacklist = ConcurrentHashMap.newKeySet();

    /**
     * 添加Token
     *
     * @param username 用户名
     * @param token    JWT令牌
     */
    public void addToken(String username, String token) {
        // TODO: 实现添加token逻辑
        userTokenMap.put(username, token);
        log.info("用户 {} 的token已添加", username);
    }

    /**
     * 移除Token
     *
     * @param username 用户名
     */
    public void removeToken(String username) {
        // TODO: 实现移除token逻辑
        String token = userTokenMap.remove(username);
        if (token != null) {
            tokenBlacklist.add(token);
            log.info("用户 {} 的token已移除并加入黑名单", username);
        }
    }

    /**
     * 更新Token
     *
     * @param username   用户名
     * @param newToken 新的JWT令牌
     */
    public void updateToken(String username, String newToken) {
        // TODO: 实现更新token逻辑
        String oldToken = userTokenMap.put(username, newToken);
        if (oldToken != null) {
            tokenBlacklist.add(oldToken);
            log.info("用户 {} 的token已更新，旧token已加入黑名单", username);
        }
    }

    /**
     * 获取用户的Token
     *
     * @param username 用户名
     * @return JWT令牌
     */
    public String getToken(String username) {
        // TODO: 实现获取token逻辑
        return userTokenMap.get(username);
    }

    /**
     * 检查Token是否在黑名单中
     *
     * @param token JWT令牌
     * @return 在黑名单中返回true
     */
    public boolean isTokenBlacklisted(String token) {
        // TODO: 实现检查黑名单逻辑
        return tokenBlacklist.contains(token);
    }

    /**
     * 将Token加入黑名单
     *
     * @param token JWT令牌
     */
    public void blacklistToken(String token) {
        // TODO: 实现加入黑名单逻辑
        tokenBlacklist.add(token);
        log.info("Token已加入黑名单: {}", token.substring(0, Math.min(20, token.length())));
    }

    /**
     * 清理过期的黑名单Token
     */
    public void cleanExpiredBlacklistTokens() {
        // TODO: 实现清理过期黑名单逻辑
        // TODO: 可以定期调用此方法清理过期的token
    }

    /**
     * 获取当前活跃用户数
     *
     * @return 活跃用户数
     */
    public int getActiveUserCount() {
        // TODO: 实现获取活跃用户数逻辑
        return userTokenMap.size();
    }

    /**
     * 清空所有Token（用于测试或重启）
     */
    public void clearAllTokens() {
        // TODO: 实现清空所有token逻辑
        userTokenMap.clear();
        tokenBlacklist.clear();
        log.info("所有token已清空");
    }
}
