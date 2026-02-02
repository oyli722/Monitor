package com.hundred.monitor.server.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * SSH WebSocket配置类
 */
@Configuration
@EnableWebSocket
public class SshWebSocketConfig implements WebSocketConfigurer {

    private final SshWebSocketHandler sshWebSocketHandler;

    public SshWebSocketConfig(SshWebSocketHandler sshWebSocketHandler) {
        this.sshWebSocketHandler = sshWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // TODO: 注册WebSocket处理器，路径为 /api/v1/ssh/terminal/**
        registry.addHandler(sshWebSocketHandler, "/api/v1/ssh/terminal/**")
                .setAllowedOrigins("*");
    }
}
