package com.hundred.monitor.server.ai.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundred.monitor.server.ai.entity.Message;
import com.hundred.monitor.server.ai.entity.SessionInfo;
import com.hundred.monitor.server.ai.entity.SshSessionBinding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 主机详情页AI助手Redis工具类
 * 管理SSH与AI会话绑定关系、消息存储、会话信息
 */
@Slf4j
@Component
public class AiSshRedisUtils {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // Redis Key前缀
    private static final String BINDING_PREFIX = "ai:ssh:binding:";
    private static final String MESSAGES_PREFIX = "ai:ssh:messages:";
    private static final String INFO_PREFIX = "ai:ssh:info:";
    private static final String WS_SESSIONS_KEY = "ai:ssh:ws:sessions";

    // TTL设置（24小时）
    private static final long BINDING_TTL_HOURS = 24;
    private static final long MESSAGES_TTL_HOURS = 24;
    private static final long INFO_TTL_HOURS = 24;

    // ==================== 绑定关系操作 ====================

    /**
     * 保存绑定关系
     *
     * @param binding 绑定关系对象
     */
    public void saveBinding(SshSessionBinding binding) {
        try {
            String key = BINDING_PREFIX + binding.getAiSessionId();
            String json = objectMapper.writeValueAsString(binding);
            redisTemplate.opsForValue().set(key, json, BINDING_TTL_HOURS, TimeUnit.HOURS);
            log.debug("保存绑定关系: aiSessionId={}, sshSessionId={}",
                    binding.getAiSessionId(), binding.getSshSessionId());
        } catch (Exception e) {
            log.error("保存绑定关系失败: aiSessionId={}", binding.getAiSessionId(), e);
            throw new RuntimeException("保存绑定关系失败", e);
        }
    }

    /**
     * 获取绑定关系
     *
     * @param aiSessionId AI会话ID
     * @return 绑定关系对象，不存在返回null
     */
    public SshSessionBinding getBinding(String aiSessionId) {
        try {
            String key = BINDING_PREFIX + aiSessionId;
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, SshSessionBinding.class);
        } catch (Exception e) {
            log.error("获取绑定关系失败: aiSessionId={}", aiSessionId, e);
            return null;
        }
    }

    /**
     * 删除绑定关系
     *
     * @param aiSessionId AI会话ID
     */
    public void deleteBinding(String aiSessionId) {
        try {
            String key = BINDING_PREFIX + aiSessionId;
            redisTemplate.delete(key);
            log.info("删除绑定关系: aiSessionId={}", aiSessionId);
        } catch (Exception e) {
            log.error("删除绑定关系失败: aiSessionId={}", aiSessionId, e);
        }
    }

    /**
     * 检查绑定是否存在
     *
     * @param aiSessionId AI会话ID
     * @return true表示存在
     */
    public boolean bindingExists(String aiSessionId) {
        try {
            String key = BINDING_PREFIX + aiSessionId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查绑定存在失败: aiSessionId={}", aiSessionId, e);
            return false;
        }
    }

    // ==================== 消息列表操作 ====================

    /**
     * 添加消息到会话
     *
     * @param aiSessionId AI会话ID
     * @param message     消息对象
     */
    public void addMessage(String aiSessionId, Message message) {
        try {
            String key = MESSAGES_PREFIX + aiSessionId;
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.expire(key, MESSAGES_TTL_HOURS, TimeUnit.HOURS);
            log.debug("添加消息: aiSessionId={}, role={}", aiSessionId, message.getRole());
        } catch (Exception e) {
            log.error("添加消息失败: aiSessionId={}", aiSessionId, e);
            throw new RuntimeException("添加消息失败", e);
        }
    }

    /**
     * 获取会话的所有消息
     *
     * @param aiSessionId AI会话ID
     * @return 消息列表
     */
    public List<Message> getMessages(String aiSessionId) {
        try {
            String key = MESSAGES_PREFIX + aiSessionId;
            List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
            if (jsonList == null || jsonList.isEmpty()) {
                return new ArrayList<>();
            }

            List<Message> messages = new ArrayList<>();
            for (String json : jsonList) {
                messages.add(objectMapper.readValue(json, Message.class));
            }
            return messages;
        } catch (Exception e) {
            log.error("获取消息列表失败: aiSessionId={}", aiSessionId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取会话的最近N条消息
     *
     * @param aiSessionId AI会话ID
     * @param count       消息数量
     * @return 消息列表
     */
    public List<Message> getRecentMessages(String aiSessionId, int count) {
        try {
            String key = MESSAGES_PREFIX + aiSessionId;
            long size = redisTemplate.opsForList().size(key);
            long start = Math.max(0, size - count);

            List<String> jsonList = redisTemplate.opsForList().range(key, start, -1);
            if (jsonList == null || jsonList.isEmpty()) {
                return new ArrayList<>();
            }

            List<Message> messages = new ArrayList<>();
            for (String json : jsonList) {
                messages.add(objectMapper.readValue(json, Message.class));
            }
            return messages;
        } catch (Exception e) {
            log.error("获取最近消息失败: aiSessionId={}", aiSessionId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 清空会话消息
     *
     * @param aiSessionId AI会话ID
     */
    public void clearMessages(String aiSessionId) {
        try {
            String key = MESSAGES_PREFIX + aiSessionId;
            redisTemplate.delete(key);
            log.info("清空消息: aiSessionId={}", aiSessionId);
        } catch (Exception e) {
            log.error("清空消息失败: aiSessionId={}", aiSessionId, e);
            throw new RuntimeException("清空消息失败", e);
        }
    }

    /**
     * 获取消息数量
     *
     * @param aiSessionId AI会话ID
     * @return 消息数量
     */
    public long getMessageCount(String aiSessionId) {
        try {
            String key = MESSAGES_PREFIX + aiSessionId;
            Long size = redisTemplate.opsForList().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("获取消息数量失败: aiSessionId={}", aiSessionId, e);
            return 0;
        }
    }

    /**
     * 检查是否需要总结（消息数量超过阈值）
     *
     * @param aiSessionId AI会话ID
     * @param threshold   阈值
     * @return true表示需要总结
     */
    public boolean needsSummary(String aiSessionId, int threshold) {
        long count = getMessageCount(aiSessionId);
        return count >= threshold;
    }

    // ==================== 会话信息操作 ====================

    /**
     * 保存会话信息
     *
     * @param sessionInfo 会话信息对象
     */
    public void saveSessionInfo(SessionInfo sessionInfo) {
        try {
            String key = INFO_PREFIX + sessionInfo.getSessionId();
            String json = objectMapper.writeValueAsString(sessionInfo);
            redisTemplate.opsForValue().set(key, json, INFO_TTL_HOURS, TimeUnit.HOURS);
            log.debug("保存会话信息: sessionId={}", sessionInfo.getSessionId());
        } catch (Exception e) {
            log.error("保存会话信息失败: sessionId={}", sessionInfo.getSessionId(), e);
            throw new RuntimeException("保存会话信息失败", e);
        }
    }

    /**
     * 获取会话信息
     *
     * @param aiSessionId AI会话ID
     * @return 会话信息对象，不存在返回null
     */
    public SessionInfo getSessionInfo(String aiSessionId) {
        try {
            String key = INFO_PREFIX + aiSessionId;
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, SessionInfo.class);
        } catch (Exception e) {
            log.error("获取会话信息失败: sessionId={}", aiSessionId, e);
            return null;
        }
    }

    /**
     * 删除会话信息
     *
     * @param aiSessionId AI会话ID
     */
    public void deleteSessionInfo(String aiSessionId) {
        try {
            String key = INFO_PREFIX + aiSessionId;
            redisTemplate.delete(key);
            log.info("删除会话信息: sessionId={}", aiSessionId);
        } catch (Exception e) {
            log.error("删除会话信息失败: sessionId={}", aiSessionId, e);
        }
    }

    // ==================== WebSocket会话管理 ====================

    /**
     * 添加活跃的WebSocket会话
     *
     * @param aiSessionId AI会话ID
     */
    public void addActiveSession(String aiSessionId) {
        try {
            redisTemplate.opsForSet().add(WS_SESSIONS_KEY, aiSessionId);
            log.debug("添加活跃会话: aiSessionId={}", aiSessionId);
        } catch (Exception e) {
            log.error("添加活跃会话失败: aiSessionId={}", aiSessionId, e);
        }
    }

    /**
     * 移除WebSocket会话
     *
     * @param aiSessionId AI会话ID
     */
    public void removeActiveSession(String aiSessionId) {
        try {
            redisTemplate.opsForSet().remove(WS_SESSIONS_KEY, aiSessionId);
            log.debug("移除活跃会话: aiSessionId={}", aiSessionId);
        } catch (Exception e) {
            log.error("移除活跃会话失败: aiSessionId={}", aiSessionId, e);
        }
    }

    /**
     * 检查会话是否活跃（WebSocket连接存在）
     *
     * @param aiSessionId AI会话ID
     * @return true表示活跃
     */
    public boolean isSessionActive(String aiSessionId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(WS_SESSIONS_KEY, aiSessionId));
        } catch (Exception e) {
            log.error("检查会话活跃状态失败: aiSessionId={}", aiSessionId, e);
            return false;
        }
    }

    /**
     * 获取所有活跃会话ID
     *
     * @return 会话ID集合
     */
    public Set<String> getActiveSessions() {
        try {
            Set<String> members = redisTemplate.opsForSet().members(WS_SESSIONS_KEY);
            return members != null ? members : Set.of();
        } catch (Exception e) {
            log.error("获取活跃会话列表失败", e);
            return Set.of();
        }
    }

    // ==================== 批量清理操作 ====================

    /**
     * 清理会话的所有数据（绑定、消息、会话信息）
     *
     * @param aiSessionId AI会话ID
     */
    public void cleanupSession(String aiSessionId) {
        try {
            deleteBinding(aiSessionId);
            clearMessages(aiSessionId);
            deleteSessionInfo(aiSessionId);
            removeActiveSession(aiSessionId);
            log.info("清理会话数据: aiSessionId={}", aiSessionId);
        } catch (Exception e) {
            log.error("清理会话数据失败: aiSessionId={}", aiSessionId, e);
        }
    }
}
