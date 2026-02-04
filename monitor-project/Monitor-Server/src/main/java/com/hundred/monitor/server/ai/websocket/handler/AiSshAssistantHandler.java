package com.hundred.monitor.server.ai.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundred.monitor.server.ai.service.AiSshAssistantService;
import com.hundred.monitor.server.ai.utils.TerminalChatRedisUtils;
import com.hundred.monitor.server.ai.websocket.dto.WsChatMessage;
import com.hundred.monitor.server.ai.websocket.manager.AiSshAssistantManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 主机详情页AI助手WebSocket处理器
 * 处理与SSH终端绑定的AI助手WebSocket连接
 */
@Slf4j
@Component
public class AiSshAssistantHandler implements WebSocketHandler {

    @Resource
    private AiSshAssistantManager aiSshAssistantManager;

    @Resource
    private AiSshAssistantService aiSshAssistantService;

    @Resource
    private TerminalChatRedisUtils aiSshRedisUtils;

    @Resource
    private ObjectMapper objectMapper;



    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 1. 从URI路径提取aiSessionId
        // URI格式: ws://server/ws/ai/ssh-assistant/{aiSessionId}
        String aiSessionId = extractAiSessionId(session);

        if (aiSessionId == null || aiSessionId.isEmpty()) {
            log.warn("AI会话ID为空，拒绝连接");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("AI会话ID不能为空"));
            return;
        }

        // 2. 验证绑定关系是否存在
        if (!aiSshAssistantService.sessionExists(aiSessionId)) {
            log.warn("AI会话不存在: aiSessionId={}", aiSessionId);
            sendMessage(session, WsChatMessage.error("SESSION_NOT_FOUND", "AI会话不存在，请重新连接"));
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("AI会话不存在"));
            return;
        }

        aiSshAssistantManager.addSession(aiSessionId, session);

        // 4. 标记会话为活跃
        aiSshRedisUtils.addActiveSession(aiSessionId);

        log.info("AI助手WebSocket连接建立: aiSessionId={}", aiSessionId);

        // 5. 发送连接成功消息
        sendMessage(session, WsChatMessage.reply("AI助手已连接，您可以开始对话了"));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String aiSessionId = extractAiSessionId(session);

        try {
            // 1. 解析消息
            String payload = message.getPayload().toString();
            WsChatMessage wsMessage = objectMapper.readValue(payload, WsChatMessage.class);

            log.debug("收到AI消息: aiSessionId={}, type={}", aiSessionId, wsMessage.getType());

            // 2. 处理不同类型的消息
            switch (wsMessage.getType()) {
                case "chat":
                    handleChatMessage(session, aiSessionId, wsMessage);
                    break;

                case "ping":
                    sendMessage(session, WsChatMessage.ping());
                    break;

                default:
                    log.warn("未知消息类型: {}", wsMessage.getType());
                    sendMessage(session, WsChatMessage.error("UNKNOWN_TYPE", "未知消息类型"));
            }

        } catch (Exception e) {
            log.error("处理消息失败: aiSessionId={}", aiSessionId, e);
            sendMessage(session, WsChatMessage.error("MESSAGE_ERROR", "消息处理失败: " + e.getMessage()));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String aiSessionId = extractAiSessionId(session);
        log.error("WebSocket传输错误: aiSessionId={}", aiSessionId, exception);

        try {
            sendMessage(session, WsChatMessage.error("TRANSPORT_ERROR", "连接传输错误"));
        } catch (Exception e) {
            log.error("发送错误消息失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String aiSessionId = extractAiSessionId(session);

        aiSshAssistantManager.removeSession(aiSessionId);

        // 2. 移除活跃标记
        aiSshRedisUtils.removeActiveSession(aiSessionId);

        log.info("AI助手WebSocket连接关闭: aiSessionId={}, status={}", aiSessionId, closeStatus);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    // ==================== 私有方法 ====================

    /**
     * 处理聊天消息
     */
    private void handleChatMessage(WebSocketSession session, String aiSessionId, WsChatMessage wsMessage) {
        try {
            String userMessage = wsMessage.getContent();

            if (userMessage == null || userMessage.trim().isEmpty()) {
                sendMessage(session, WsChatMessage.error("EMPTY_MESSAGE", "消息内容不能为空"));
                return;
            }

            // 调用Service获取AI回复
            String aiReply = aiSshAssistantService.sendMessage(aiSessionId, userMessage);

            // 发送AI回复
            sendMessage(session, WsChatMessage.reply(aiReply));

        } catch (IllegalArgumentException e) {
            log.warn("处理聊天消息失败: aiSessionId={}, error={}", aiSessionId, e.getMessage());
            sendMessage(session, WsChatMessage.error("INVALID_PARAM", e.getMessage()));
        } catch (Exception e) {
            log.error("处理聊天消息异常: aiSessionId={}", aiSessionId, e);
            sendMessage(session, WsChatMessage.error("CHAT_ERROR", "处理消息失败: " + e.getMessage()));
        }
    }

    /**
     * 发送消息到客户端
     */
    private void sendMessage(WebSocketSession session, WsChatMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }

    /**
     * 从URI路径提取aiSessionId
     * URI格式: ws://server/ws/ai/ssh-assistant/{aiSessionId}
     */
    private String extractAiSessionId(WebSocketSession session) {
        String path = session.getUri().getPath();
        // 路径格式: /ws/ai/ssh-assistant/{aiSessionId}
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : null;
    }

    /**
     * 获取WebSocket会话
     */
    public WebSocketSession getSession(String aiSessionId) {
        return aiSshAssistantManager.getSession(aiSessionId);
    }

    /**
     * 向指定会话发送消息
     */
    public void sendToSession(String aiSessionId, WsChatMessage message) {
        WebSocketSession session = aiSshAssistantManager.getSession(aiSessionId);
        if (session != null && session.isOpen()) {
            sendMessage(session, message);
        }
    }
}
