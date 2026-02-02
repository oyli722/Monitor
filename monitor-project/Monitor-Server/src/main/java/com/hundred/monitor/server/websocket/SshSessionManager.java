package com.hundred.monitor.server.websocket;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSH会话管理器
 * 单例模式，管理所有SSH会话
 */
@Slf4j
public class SshSessionManager {

    /**
     * 单例实例
     */
    private static final SshSessionManager INSTANCE = new SshSessionManager();

    /**
     * 会话存储：sessionId -> SshSession
     */
    private final Map<String, SshSession> sessions = new ConcurrentHashMap<>();

    /**
     * 获取单例实例
     */
    public static SshSessionManager getInstance() {
        return INSTANCE;
    }

    /**
     * 添加会话
     */
    public void addSession(String sessionId, SshSession session) {
        sessions.put(sessionId, session);
    }

    /**
     * 获取会话
     */
    public SshSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 移除会话
     */
    public void removeSession(String sessionId) {
        SshSession session = sessions.remove(sessionId);
        if (session != null) {
            log.info("SSH会话已移除: sessionId={}", sessionId);
        }
    }

    /**
     * 获取所有会话
     */
    public Map<String, SshSession> getAllSessions() {
        return new ConcurrentHashMap<>(sessions);
    }
}
