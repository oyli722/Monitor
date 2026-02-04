package com.hundred.monitor.server.ai.websocket;

import com.hundred.monitor.server.ai.websocket.handler.AiSshAssistantHandler;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * AI助手WebSocket配置类
 * 配置主机详情页AI助手的WebSocket路由
 */
@EnableWebSocket
@Configuration
public class AgentModelWebSocketConfig implements WebSocketConfigurer {

    @Resource
    private AiSshAssistantHandler aiSshAssistantHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册AI助手WebSocket处理器
        // 前端连接: ws://server/ws/ai/ssh-assistant/{aiSessionId}
        registry.addHandler(aiSshAssistantHandler, "/ws/ai/ssh-assistant/**")
                .setAllowedOrigins("*");  // TODO: 生产环境应限制具体域名
    }
}
