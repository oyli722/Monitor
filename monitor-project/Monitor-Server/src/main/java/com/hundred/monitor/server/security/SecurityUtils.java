package com.hundred.monitor.server.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        // TODO: 实现获取当前用户名逻辑
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }

        return null;
    }

    /**
     * 获取当前认证对象
     */
    public static Authentication getCurrentAuthentication() {
        // TODO: 实现获取当前认证对象逻辑
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 检查当前用户是否已认证
     */
    public static boolean isAuthenticated() {
        // TODO: 实现检查认证状态逻辑
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            return authentication.isAuthenticated() &&
                   !"anonymousUser".equals(authentication.getPrincipal());
        }

        return false;
    }

    /**
     * 清除当前用户认证信息
     */
    public static void clearAuthentication() {
        // TODO: 实现清除认证信息逻辑
        SecurityContextHolder.clearContext();
    }

    /**
     * 检查用户是否具有指定角色
     */
    public static boolean hasRole(String role) {
        // TODO: 实现检查角色权限逻辑
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
        }

        return false;
    }
}
