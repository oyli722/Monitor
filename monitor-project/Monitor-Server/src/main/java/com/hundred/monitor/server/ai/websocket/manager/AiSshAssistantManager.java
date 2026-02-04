package com.hundred.monitor.server.ai.websocket.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundred.monitor.server.ai.websocket.dto.WsChatMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 主机详情页AI助手WebSocket会话管理器
 * 管理AI助手WebSocket连接与会话的映射关系
 */
@Slf4j
@Component
public class AiSshAssistantManager {

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 存储WebSocket会话映射：aiSessionId -> WebSocketSession
     */
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // ==================== 会话管理 ====================

    /**
     * 添加会话
     *
     * @param aiSessionId AI会话ID
     * @param session     WebSocket会话
     */
    public void addSession(String aiSessionId, WebSocketSession session) {
        sessions.put(aiSessionId, session);
        log.info("添加AI助手会话: aiSessionId={}, 当前活跃数={}", aiSessionId, sessions.size());
    }

    /**
     * 移除会话
     *
     * @param aiSessionId AI会话ID
     */
    public void removeSession(String aiSessionId) {
        WebSocketSession removed = sessions.remove(aiSessionId);
        if (removed != null) {
            log.info("移除AI助手会话: aiSessionId={}, 当前活跃数={}", aiSessionId, sessions.size());
        }
    }

    /**
     * 获取会话
     *
     * @param aiSessionId AI会话ID
     * @return WebSocket会话，不存在返回null
     */
    public WebSocketSession getSession(String aiSessionId) {
        return sessions.get(aiSessionId);
    }

    /**
     * 检查会话是否存在
     *
     * @param aiSessionId AI会话ID
     * @return true表示存在
     */
    public boolean hasSession(String aiSessionId) {
        return sessions.containsKey(aiSessionId);
    }

    /**
     * 获取所有会话
     *
     * @return 会话集合
     */
    public Collection<WebSocketSession> getAllSessions() {
        return sessions.values();
    }

    /**
     * 获取所有会话ID
     *
     * @return 会话ID集合
     */
    public Collection<String> getAllSessionIds() {
        return sessions.keySet();
    }

    /**
     * 获取活跃会话数量
     *
     * @return 会话数量
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * 清空所有会话
     */
    public void clearAllSessions() {
        int count = sessions.size();
        sessions.clear();
        log.info("清空所有AI助手会话，清空数量={}", count);
    }

    // ==================== 消息发送 ====================

    /**
     * 向指定会话发送消息
     *
     * @param aiSessionId AI会话ID
     * @param message     消息对象
     * @return true表示发送成功
     */
    public boolean sendToSession(String aiSessionId, WsChatMessage message) {
        WebSocketSession session = sessions.get(aiSessionId);
        if (session == null) {
            log.warn("会话不存在，无法发送消息: aiSessionId={}", aiSessionId);
            return false;
        }

        if (!session.isOpen()) {
            log.warn("会话已关闭，无法发送消息: aiSessionId={}", aiSessionId);
            removeSession(aiSessionId);
            return false;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
            log.debug("发送消息到会话: aiSessionId={}, type={}", aiSessionId, message.getType());
            return true;
        } catch (Exception e) {
            log.error("发送消息失败: aiSessionId={}", aiSessionId, e);
            return false;
        }
    }

    /**
     * 广播消息到所有会话
     *
     * @param message 消息对象
     * @return 成功发送的会话数量
     */
    public int broadcast(WsChatMessage message) {
        int successCount = 0;

        for (String aiSessionId : sessions.keySet()) {
            if (sendToSession(aiSessionId, message)) {
                successCount++;
            }
        }

        log.info("广播消息完成，目标数={}, 成功数={}", sessions.size(), successCount);
        return successCount;
    }

    /**
     * 广播消息到所有会话（排除指定会话）
     *
     * @param message          消息对象
     * @param excludeSessionId 要排除的会话ID
     * @return 成功发送的会话数量
     */
    public int broadcastExclude(WsChatMessage message, String excludeSessionId) {
        int successCount = 0;

        for (String aiSessionId : sessions.keySet()) {
            if (aiSessionId.equals(excludeSessionId)) {
                continue;
            }
            if (sendToSession(aiSessionId, message)) {
                successCount++;
            }
        }

        log.info("广播消息完成（排除会话），目标数={}, 成功数={}", sessions.size() - 1, successCount);
        return successCount;
    }

    // ==================== 会话统计 ====================

    /**
     * 获取会话统计信息
     *
     * @return 统计信息字符串
     */
    public String getStatistics() {
        return String.format("AI助手会话统计: 活跃数=%d", sessions.size());
    }

    /**
     * 打印所有会话信息（用于调试）
     */
    public void printAllSessions() {
        log.info("=== AI助手会话列表 ===");
        log.info("活跃会话数: {}", sessions.size());
        for (String aiSessionId : sessions.keySet()) {
            WebSocketSession session = sessions.get(aiSessionId);
            log.info("  - aiSessionId={}, isOpen={}, id={}",
                    aiSessionId, session.isOpen(), session.getId());
        }
        log.info("===================");
    }

    // ==================== 健康检查 ====================

    /**
     * 清理已关闭的会话
     *
     * @return 清理的会话数量
     */
    public int cleanupClosedSessions() {
        int cleanedCount = 0;

        for (String aiSessionId : sessions.keySet()) {
            WebSocketSession session = sessions.get(aiSessionId);
            if (session != null && !session.isOpen()) {
                sessions.remove(aiSessionId);
                cleanedCount++;
            }
        }

        if (cleanedCount > 0) {
            log.info("清理已关闭的会话，清理数量={}", cleanedCount);
        }

        return cleanedCount;
    }

    /**
     * 检查会话是否活跃（连接正常）
     *
     * @param aiSessionId AI会话ID
     * @return true表示活跃
     */
    public boolean isSessionActive(String aiSessionId) {
        WebSocketSession session = sessions.get(aiSessionId);
        return session != null && session.isOpen();
    }
}
