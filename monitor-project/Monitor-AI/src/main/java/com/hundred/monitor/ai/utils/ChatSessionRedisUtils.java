package com.hundred.monitor.ai.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundred.monitor.commonlibrary.ai.model.ChatMessage;
import com.hundred.monitor.commonlibrary.ai.model.ChatSessionInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 全局聊天会话Redis工具类（侧边栏AI助手 - HTTP REST API）
 * 管理全局聊天消息和会话的Redis存储，支持智能总结压缩机制
 */
@Slf4j
@Component
public class ChatSessionRedisUtils {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // Redis Key前缀
    private static final String CHAT_MESSAGES_PREFIX = "assistant:chat:messages:";
    private static final String CHAT_INFO_PREFIX = "assistant:chat:info:";
    private static final String USER_SESSIONS_PREFIX = "assistant:user:sessions:";

    // 消息数量阈值（超过此数量触发总结）
    private static final int MESSAGE_THRESHOLD = 20;

    // 过期时间（30天）
    private static final long SESSION_TTL_DAYS = 30;

    // ==================== 会话操作 ====================

    /**
     * 创建新会话
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param title     会话标题
     * @return 创建的会话信息
     */
    public ChatSessionInfo createSession(String userId, String sessionId, String title) {
        long now = Instant.now().toEpochMilli();

        ChatSessionInfo sessionInfo = ChatSessionInfo.builder()
                .sessionId(sessionId)
                .title(title)
                .createdAt(now)
                .updatedAt(now)
                .messageCount(0)
                .summary(null)
                .linkedAgentId(null)
                .lastSummaryAt(null)
                .build();

        try {
            // 存储会话信息
            String sessionInfoJson = objectMapper.writeValueAsString(sessionInfo);
            redisTemplate.opsForValue().set(
                    CHAT_INFO_PREFIX + sessionId,
                    sessionInfoJson,
                    SESSION_TTL_DAYS,
                    TimeUnit.DAYS
            );

            // 添加到用户的会话集合（按更新时间排序）
            redisTemplate.opsForZSet().add(USER_SESSIONS_PREFIX + userId, sessionId, now);

            log.info("创建会话成功: sessionId={}, title={}, userId={}", sessionId, title, userId);
            return sessionInfo;
        } catch (Exception e) {
            log.error("创建会话失败: sessionId={}", sessionId, e);
            throw new RuntimeException("创建会话失败", e);
        }
    }

    /**
     * 获取会话信息
     *
     * @param sessionId 会话ID
     * @return 会话信息，不存在返回null
     */
    public ChatSessionInfo getSessionInfo(String sessionId) {
        try {
            String json = redisTemplate.opsForValue().get(CHAT_INFO_PREFIX + sessionId);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, ChatSessionInfo.class);
        } catch (Exception e) {
            log.error("获取会话信息失败: sessionId={}", sessionId, e);
            return null;
        }
    }

    /**
     * 更新会话信息
     *
     * @param sessionInfo 会话信息
     */
    public void updateSessionInfo(ChatSessionInfo sessionInfo) {
        try {
            sessionInfo.setUpdatedAt(Instant.now().toEpochMilli());
            String json = objectMapper.writeValueAsString(sessionInfo);
            redisTemplate.opsForValue().set(
                    CHAT_INFO_PREFIX + sessionInfo.getSessionId(),
                    json,
                    SESSION_TTL_DAYS,
                    TimeUnit.DAYS
            );
        } catch (Exception e) {
            log.error("更新会话信息失败: sessionId={}", sessionInfo.getSessionId(), e);
            throw new RuntimeException("更新会话信息失败", e);
        }
    }

    /**
     * 删除会话及其所有消息
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     */
    public void deleteSession(String userId, String sessionId) {
        try {
            // 删除会话信息
            redisTemplate.delete(CHAT_INFO_PREFIX + sessionId);
            // 删除消息列表
            redisTemplate.delete(CHAT_MESSAGES_PREFIX + sessionId);
            // 从用户会话集合中移除
            redisTemplate.opsForZSet().remove(USER_SESSIONS_PREFIX + userId, sessionId);

            log.info("删除会话成功: sessionId={}, userId={}", sessionId, userId);
        } catch (Exception e) {
            log.error("删除会话失败: sessionId={}", sessionId, e);
            throw new RuntimeException("删除会话失败", e);
        }
    }

    /**
     * 获取用户的所有会话列表（按更新时间倒序）
     *
     * @param userId 用户ID
     * @return 会话ID列表
     */
    public List<String> getUserSessionIds(String userId) {
        try {
            Set<String> sessionIds = redisTemplate.opsForZSet().reverseRange(
                    USER_SESSIONS_PREFIX + userId,
                    0,
                    -1
            );
            return sessionIds != null ? new ArrayList<>(sessionIds) : new ArrayList<>();
        } catch (Exception e) {
            log.error("获取用户会话列表失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取用户的所有会话详细信息列表
     *
     * @param userId 用户ID
     * @return 会话信息列表
     */
    public List<ChatSessionInfo> getUserSessions(String userId) {
        List<String> sessionIds = getUserSessionIds(userId);
        List<ChatSessionInfo> sessions = new ArrayList<>();

        for (String sessionId : sessionIds) {
            ChatSessionInfo info = getSessionInfo(sessionId);
            if (info != null) {
                sessions.add(info);
            }
        }

        return sessions;
    }

    /**
     * 更新会话的最后更新时间（刷新排序）
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     */
    public void refreshSessionTime(String userId, String sessionId) {
        try {
            double score = Instant.now().toEpochMilli();
            redisTemplate.opsForZSet().add(USER_SESSIONS_PREFIX + userId, sessionId, score);
        } catch (Exception e) {
            log.error("刷新会话时间失败: sessionId={}", sessionId, e);
        }
    }

    // ==================== 消息操作 ====================

    /**
     * 添加消息到会话
     *
     * @param sessionId 会话ID
     * @param message   消息
     */
    public void addMessage(String sessionId, ChatMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().rightPush(CHAT_MESSAGES_PREFIX + sessionId, messageJson);

            // 更新会话的消息计数
            ChatSessionInfo sessionInfo = getSessionInfo(sessionId);
            if (sessionInfo != null) {
                sessionInfo.setMessageCount(sessionInfo.getMessageCount() + 1);
                updateSessionInfo(sessionInfo);
            }

            log.debug("添加消息成功: sessionId={}, role={}", sessionId, message.getRole());
        } catch (Exception e) {
            log.error("添加消息失败: sessionId={}", sessionId, e);
            throw new RuntimeException("添加消息失败", e);
        }
    }

    /**
     * 获取会话的所有消息
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    public List<ChatMessage> getMessages(String sessionId) {
        try {
            List<String> jsonList = redisTemplate.opsForList().range(
                    CHAT_MESSAGES_PREFIX + sessionId,
                    0,
                    -1
            );

            if (jsonList == null || jsonList.isEmpty()) {
                return new ArrayList<>();
            }

            List<ChatMessage> messages = new ArrayList<>();
            for (String json : jsonList) {
                ChatMessage message = objectMapper.readValue(json, ChatMessage.class);
                messages.add(message);
            }

            return messages;
        } catch (Exception e) {
            log.error("获取消息列表失败: sessionId={}", sessionId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取会话的最近N条消息
     *
     * @param sessionId 会话ID
     * @param count     消息数量
     * @return 消息列表
     */
    public List<ChatMessage> getRecentMessages(String sessionId, int count) {
        try {
            long size = redisTemplate.opsForList().size(CHAT_MESSAGES_PREFIX + sessionId);
            long start = Math.max(0, size - count);

            List<String> jsonList = redisTemplate.opsForList().range(
                    CHAT_MESSAGES_PREFIX + sessionId,
                    start,
                    -1
            );

            if (jsonList == null || jsonList.isEmpty()) {
                return new ArrayList<>();
            }

            List<ChatMessage> messages = new ArrayList<>();
            for (String json : jsonList) {
                ChatMessage message = objectMapper.readValue(json, ChatMessage.class);
                messages.add(message);
            }

            return messages;
        } catch (Exception e) {
            log.error("获取最近消息失败: sessionId={}", sessionId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 清空会话的所有消息
     *
     * @param sessionId 会话ID
     */
    public void clearMessages(String sessionId) {
        try {
            redisTemplate.delete(CHAT_MESSAGES_PREFIX + sessionId);
            log.info("清空消息成功: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("清空消息失败: sessionId={}", sessionId, e);
            throw new RuntimeException("清空消息失败", e);
        }
    }

    /**
     * 检查是否需要总结（消息数量超过阈值）
     *
     * @param sessionId 会话ID
     * @return true表示需要总结
     */
    public boolean needsSummary(String sessionId) {
        ChatSessionInfo sessionInfo = getSessionInfo(sessionId);
        if (sessionInfo == null) {
            return false;
        }

        Long messageCount = redisTemplate.opsForList().size(CHAT_MESSAGES_PREFIX + sessionId);
        return messageCount != null && messageCount >= MESSAGE_THRESHOLD;
    }

    /**
     * 获取消息数量
     *
     * @param sessionId 会话ID
     * @return 消息数量
     */
    public long getMessageCount(String sessionId) {
        Long size = redisTemplate.opsForList().size(CHAT_MESSAGES_PREFIX + sessionId);
        return size != null ? size : 0;
    }

    // ==================== 总结操作 ====================

    /**
     * 设置会话总结
     *
     * @param sessionId 会话ID
     * @param summary   总结内容
     */
    public void setSummary(String sessionId, String summary) {
        try {
            ChatSessionInfo sessionInfo = getSessionInfo(sessionId);
            if (sessionInfo != null) {
                sessionInfo.setSummary(summary);
                sessionInfo.setLastSummaryAt(Instant.now().toEpochMilli());
                updateSessionInfo(sessionInfo);
            }
            log.info("设置总结成功: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("设置总结失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 获取会话总结
     *
     * @param sessionId 会话ID
     * @return 总结内容，无总结返回null
     */
    public String getSummary(String sessionId) {
        ChatSessionInfo sessionInfo = getSessionInfo(sessionId);
        return sessionInfo != null ? sessionInfo.getSummary() : null;
    }

    /**
     * 执行消息压缩：生成总结后清空旧消息
     *
     * @param sessionId   会话ID
     * @param newSummary  新的总结内容
     */
    public void compressMessages(String sessionId, String newSummary) {
        try {
            // 设置总结
            setSummary(sessionId, newSummary);

            // 清空消息列表
            clearMessages(sessionId);

            // 重置消息计数（保留在ChatSessionInfo中，但清空实际消息）
            ChatSessionInfo sessionInfo = getSessionInfo(sessionId);
            if (sessionInfo != null) {
                sessionInfo.setMessageCount(0);
                updateSessionInfo(sessionInfo);
            }

            log.info("消息压缩成功: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("消息压缩失败: sessionId={}", sessionId, e);
            throw new RuntimeException("消息压缩失败", e);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话ID
     * @return true表示存在
     */
    public boolean sessionExists(String sessionId) {
        Boolean exists = redisTemplate.hasKey(CHAT_INFO_PREFIX + sessionId);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 设置会话关联的主机
     *
     * @param sessionId  会话ID
     * @param agentId    主机ID
     */
    public void linkAgent(String sessionId, String agentId) {
        ChatSessionInfo sessionInfo = getSessionInfo(sessionId);
        if (sessionInfo != null) {
            sessionInfo.setLinkedAgentId(agentId);
            updateSessionInfo(sessionInfo);
        }
    }

    /**
     * 获取会话关联的主机ID
     *
     * @param sessionId 会话ID
     * @return 主机ID，无关联返回null
     */
    public String getLinkedAgent(String sessionId) {
        ChatSessionInfo sessionInfo = getSessionInfo(sessionId);
        return sessionInfo != null ? sessionInfo.getLinkedAgentId() : null;
    }
}
