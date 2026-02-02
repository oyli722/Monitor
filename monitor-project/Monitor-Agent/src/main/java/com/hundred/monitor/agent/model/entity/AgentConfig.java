package com.hundred.monitor.agent.model.entity;

import lombok.Data;

/**
 * 客户端配置模型
 * 对应 agent-config.yaml
 */
@Data
public class AgentConfig {

    /**
     * 服务端配置
     */
    private ServerConfig server;

    /**
     * Agent配置（注册后由服务端填充）
     */
    private AgentInfo agent;

    /**
     * 认证配置
     */
    private AuthConfig auth;

    /**
     * 上报配置
     */
    private ReportingConfig reporting;

    @Data
    public static class ServerConfig {
        /**
         * 服务端地址列表
         */
        private String[] endpoints;
    }

    @Data
    public static class AgentInfo {
        /**
         * Agent ID
         */
        private String id;

        /**
         * Agent名称
         */
        private String name;

        /**
         * 注册时间
         */
        private String registeredAt;
    }

    @Data
    public static class AuthConfig {
        /**
         * 认证Token
         */
        private String token;

        /**
         * Token过期时间
         */
        private String tokenExpires;
    }

    @Data
    public static class ReportingConfig {
        /**
         * 基本数据上报间隔（秒）
         */
        private Long basicIntervalSec = 600L;

        /**
         * 运行时数据上报间隔（秒）
         */
        private Long metricsIntervalSec = 15L;

        /**
         * 请求超时时间（秒）
         */
        private Long timeoutSec = 5L;
    }
}
