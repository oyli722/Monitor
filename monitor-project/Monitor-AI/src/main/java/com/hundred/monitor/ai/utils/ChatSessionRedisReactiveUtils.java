package com.hundred.monitor.ai.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundred.monitor.ai.constant.ChatConstants;
import com.hundred.monitor.ai.constant.ErrorConstants;
import com.hundred.monitor.ai.exception.BusinessException;
import com.hundred.monitor.commonlibrary.ai.model.ChatMessage;
import com.hundred.monitor.commonlibrary.ai.model.ChatSessionInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 全局聊天会话Redis工具类（侧边栏AI助手 - HTTP REST API）
 * 管理全局聊天消息和会话的Redis存储，支持智能总结压缩机制
 */
@Slf4j
@Component
public class ChatSessionRedisReactiveUtils {

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // Redis Key前缀
    private static final String CHAT_MESSAGES_PREFIX = "assistant:chat:messages:";
    private static final String CHAT_INFO_PREFIX = "assistant:chat:info:";
    private static final String USER_SESSIONS_PREFIX = "assistant:user:sessions:";

    // ==================== 会话操作 ====================

    /**
     * 创建新会话
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param title     会话标题
     * @return 创建的会话信息
     */
    public Mono<ChatSessionInfo> createSession(String userId, String sessionId, String title) {
        return Mono.fromCallable(() -> {
                    long now = Instant.now().toEpochMilli();
                    return ChatSessionInfo.builder()
                            .sessionId(sessionId)
                            .title(title)
                            .createdAt(now)
                            .updatedAt(now)
                            .messageCount(0)
                            .summary(null)
                            .linkedAgentId(null)
                            .lastSummaryAt(null)
                            .build();
                })
                .flatMap(sessionInfo ->
                        Mono.fromCallable(() -> objectMapper.writeValueAsString(sessionInfo))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(sessionInfoJson ->
                                        Mono.zip(
                                                        reactiveRedisTemplate.opsForValue().set(
                                                                CHAT_INFO_PREFIX + sessionId,
                                                                sessionInfoJson,
                                                                Duration.ofDays(ChatConstants.SESSION_TTL_DAYS)
                                                        ),
                                                        reactiveRedisTemplate.opsForZSet().add(
                                                                USER_SESSIONS_PREFIX + userId,
                                                                sessionId,
                                                                sessionInfo.getUpdatedAt()
                                                        )
                                                )
                                                .thenReturn(sessionInfo)
                                )
                )
                .doOnSuccess(info -> log.info("创建会话成功: sessionId={}, userId={}, title={}",
                        info.getSessionId(), userId, title))
                .doOnError(e -> log.error("创建会话失败: sessionId={}, userId={}, title={}",
                        sessionId, userId, title, e))
                .onErrorResume(e -> Mono.error(new BusinessException(ErrorConstants.SESSION_CREATE_FAILED, e)));
    }

    /**
     * 获取会话信息
     *
     * @param sessionId 会话ID
     * @return 会话信息，不存在返回空Mono
     */
    public Mono<ChatSessionInfo> getSessionInfo(String sessionId) {
        return reactiveRedisTemplate.opsForValue().get(CHAT_INFO_PREFIX + sessionId)
                .flatMap(json -> {
                    if (json == null || json.isEmpty()) {
                        return Mono.empty();
                    }

                    return Mono.fromCallable(() ->
                                    objectMapper.readValue(json, ChatSessionInfo.class)
                            )
                            .subscribeOn(Schedulers.boundedElastic())
                            .onErrorResume(JsonProcessingException.class, e -> {
                                log.error("会话信息反序列化失败: sessionId={}", sessionId, e);
                                return Mono.empty();
                            });
                })
                .doOnSuccess(info -> log.debug("获取会话信息成功: sessionId={}", sessionId))
                .doOnError(e -> log.error("获取会话信息异常: sessionId={}", sessionId, e));
    }

    /**
     * 更新会话信息
     *
     * @param sessionInfo 会话信息
     */
    public Mono<Void> updateSessionInfo(ChatSessionInfo sessionInfo) {
        return Mono.fromCallable(() -> {
                    sessionInfo.setUpdatedAt(Instant.now().toEpochMilli());
                    return objectMapper.writeValueAsString(sessionInfo);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(json -> reactiveRedisTemplate.opsForValue()
                        .set(
                                CHAT_INFO_PREFIX + sessionInfo.getSessionId(),
                                json,
                                Duration.ofDays(ChatConstants.SESSION_TTL_DAYS)
                        ))
                .doOnError(JsonProcessingException.class, e ->
                        log.error("更新会话信息失败: sessionId={}", sessionInfo.getSessionId(), e))
                .onErrorResume(JsonProcessingException.class, e ->
                        Mono.error(new BusinessException(ErrorConstants.SESSION_GET_FAILED, e)))
                .then();
    }

    /**
     * 删除会话及其所有消息
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     */
    public Mono<Void> deleteSession(String userId, String sessionId) {
        return reactiveRedisTemplate.delete(CHAT_MESSAGES_PREFIX + sessionId)
                .then(reactiveRedisTemplate.delete(CHAT_INFO_PREFIX + sessionId))
                .then(reactiveRedisTemplate.opsForZSet().remove(USER_SESSIONS_PREFIX + userId, sessionId))
                .doOnError(e -> log.error("删除会话失败: sessionId={}, userId={}", sessionId, userId, e))
                .onErrorResume(e -> Mono.error(new BusinessException(ErrorConstants.SESSION_DELETE_FAILED, e)))
                .doOnSuccess(v -> log.info("删除会话成功: sessionId={}, userId={}", sessionId, userId))
                .then();
    }

    /**
     * 获取用户的所有会话列表（按更新时间倒序）
     *
     * @param userId 用户ID
     * @return 会话ID列表
     */
    public Mono<List<String>> getUserSessionIds(String userId) {
        return reactiveRedisTemplate.opsForZSet().reverseRange(
                        USER_SESSIONS_PREFIX + userId,
                        Range.unbounded()
                ).collectList()
                .doOnSuccess(sessionIds -> log.debug("获取用户会话列表成功: userId={}", userId))
                .doOnError(e -> log.error("获取用户会话列表失败: userId={}", userId, e))
                .onErrorResume(e -> Mono.just(Collections.emptyList()));
    }

    /**
     * 获取用户的所有会话详细信息列表
     *
     * @param userId 用户ID
     * @return 会话信息列表
     */
    public Mono<List<ChatSessionInfo>> getUserSessions(String userId) {
        return getUserSessionIds(userId)
                .flatMapMany(Flux::fromIterable)
                .flatMapSequential(this::getSessionInfo)
                .collectList();
    }

    /**
     * 更新会话的最后更新时间（刷新排序）
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     */
    public Mono<Boolean> refreshSessionTime(String userId, String sessionId) {
        double score = Instant.now().toEpochMilli();
        return reactiveRedisTemplate.opsForZSet().add(USER_SESSIONS_PREFIX + userId, sessionId, score);
    }

    // ==================== 消息操作 ====================

    /**
     * 添加消息到会话
     *
     * @param sessionId 会话ID
     * @param message 消息
     */
    public Mono<Long> addMessage(String sessionId, ChatMessage message) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(message))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(messageJson -> reactiveRedisTemplate.opsForList().rightPush(CHAT_MESSAGES_PREFIX + sessionId, messageJson))
                .flatMap(msgCount -> getSessionInfo(sessionId)
                        .flatMap(sessionInfo -> {
                            sessionInfo.setMessageCount(sessionInfo.getMessageCount() + 1);
                            return updateSessionInfo(sessionInfo);
                        })
                        .thenReturn(msgCount)
                )
                .doOnSuccess(count -> log.debug("添加消息成功: sessionId={}, role={}", sessionId, message.getRole()))
                .doOnError(e -> log.error("添加消息失败: sessionId={}", sessionId, e));
    }

    /**
     * 获取会话的所有消息
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    public Flux<ChatMessage> getMessages(String sessionId) {
        return reactiveRedisTemplate.opsForList().range(CHAT_MESSAGES_PREFIX + sessionId, 0, -1)
                .switchIfEmpty(Flux.empty())
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, ChatMessage.class);
                    } catch (JsonProcessingException e) {
                        log.error("消息反序列化失败: sessionId={}", sessionId, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .doOnComplete(() -> log.debug("获取消息列表成功: sessionId={}", sessionId))
                .doOnError(e -> log.error("获取消息列表失败: sessionId={}", sessionId, e))
                .onErrorResume(e -> Flux.empty());
    }

    /**
     * 获取会话的最近N条消息
     *
     * @param sessionId 会话ID
     * @param count     消息数量
     * @return 消息列表
     */
    public Flux<ChatMessage> getRecentMessages(String sessionId, int count) {
        return reactiveRedisTemplate.opsForList().size(CHAT_MESSAGES_PREFIX + sessionId)
                .flatMapMany(size -> {
                    long start = Math.max(0, size - count);
                    return reactiveRedisTemplate.opsForList().range(CHAT_MESSAGES_PREFIX + sessionId, start, -1);
                })
                .switchIfEmpty(Flux.empty())
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, ChatMessage.class);
                    } catch (JsonProcessingException e) {
                        log.error("最近消息反序列化失败: sessionId={}", sessionId, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .doOnComplete(() -> log.debug("获取最近消息成功: sessionId={}, count={}", sessionId, count))
                .doOnError(e -> log.error("获取最近消息失败: sessionId={}, count={}", sessionId, count, e))
                .onErrorResume(e -> Flux.empty());
    }

    /**
     * 获取会话的所有消息（返回 Mono<List>，用于需要 List 的场景）
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    public Mono<List<ChatMessage>> getMessagesAsList(String sessionId) {
        return getMessages(sessionId).collectList();
    }

    /**
     * 获取会话的最近N条消息（返回 Mono<List>，用于需要 List 的场景）
     *
     * @param sessionId 会话ID
     * @param count     消息数量
     * @return 消息列表
     */
    public Mono<List<ChatMessage>> getRecentMessagesAsList(String sessionId, int count) {
        return getRecentMessages(sessionId, count).collectList();
    }

    /**
     * 清空会话的所有消息
     *
     * @param sessionId 会话ID
     * @return 清空完成的信号
     */
    public Mono<Void> clearMessages(String sessionId) {
        return reactiveRedisTemplate.delete(CHAT_MESSAGES_PREFIX + sessionId)
                .doOnSuccess(v -> log.info("清空消息成功: sessionId={}", sessionId))
                .doOnError(e -> log.error("清空消息失败: sessionId={}", sessionId, e))
                .onErrorResume(e -> Mono.error(new BusinessException(ErrorConstants.MESSAGE_CLEAR_FAILED, e)))
                .then();
    }

    /**
     * 检查是否需要总结（消息数量超过阈值）
     *
     * @param sessionId 会话ID
     * @return true表示需要总结
     */
    public Mono<Boolean> needsSummary(String sessionId) {
        return reactiveRedisTemplate.opsForList().size(CHAT_MESSAGES_PREFIX + sessionId)
                .map(count -> count != null && count >= ChatConstants.MESSAGE_THRESHOLD)
                .onErrorReturn(false);
    }

    /**
     * 获取消息数量
     *
     * @param sessionId 会话ID
     * @return 消息数量
     */
    public Mono<Long> getMessageCount(String sessionId) {
        return reactiveRedisTemplate.opsForList().size(CHAT_MESSAGES_PREFIX + sessionId)
                .map(count -> count != null ? count : 0L)
                .onErrorReturn(0L);
    }

    // ==================== 总结操作 ====================

    /**
     * 设置会话总结
     *
     * @param sessionId 会话ID
     * @param summary   总结内容
     * @return 设置完成的信号
     */
    public Mono<Void> setSummary(String sessionId, String summary) {
        return getSessionInfo(sessionId)
                .flatMap(sessionInfo -> {
                    if (sessionInfo != null) {
                        sessionInfo.setSummary(summary);
                        sessionInfo.setLastSummaryAt(Instant.now().toEpochMilli());
                        return updateSessionInfo(sessionInfo);
                    }
                    return Mono.empty();
                })
                .doOnSuccess(v -> log.info("设置总结成功: sessionId={}", sessionId))
                .doOnError(e -> log.error("设置总结失败: sessionId={}", sessionId, e))
                .then();
    }

    /**
     * 获取会话总结
     *
     * @param sessionId 会话ID
     * @return 总结内容，无总结返回空Mono
     */
    public Mono<String> getSummary(String sessionId) {
        return getSessionInfo(sessionId)
                .flatMap(sessionInfo -> {
                    String summary = sessionInfo != null ? sessionInfo.getSummary() : null;
                    return summary != null ? Mono.just(summary) : Mono.empty();
                })
                .doOnError(e -> log.error("获取总结失败: sessionId={}", sessionId, e))
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * 执行消息压缩：生成总结后清空旧消息
     *
     * @param sessionId   会话ID
     * @param newSummary  新的总结内容
     * @return 压缩完成的信号
     */
    public Mono<Void> compressMessages(String sessionId, String newSummary) {
        return setSummary(sessionId, newSummary)
                .then(clearMessages(sessionId))
                .then(getSessionInfo(sessionId))
                .flatMap(sessionInfo -> {
                    if (sessionInfo != null) {
                        sessionInfo.setMessageCount(0);
                        return updateSessionInfo(sessionInfo);
                    }
                    return Mono.empty();
                })
                .doOnSuccess(v -> log.info("消息压缩成功: sessionId={}", sessionId))
                .doOnError(e -> log.error("消息压缩失败: sessionId={}", sessionId, e))
                .onErrorResume(e -> Mono.error(new BusinessException("消息压缩失败: sessionId=" + sessionId, e)))
                .then();
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话ID
     * @return true表示存在
     */
    public Mono<Boolean> sessionExists(String sessionId) {
        return reactiveRedisTemplate.hasKey(CHAT_INFO_PREFIX + sessionId)
                .map(exists -> exists != null && exists);
    }

    /**
     * 设置会话关联的主机
     *
     * @param sessionId  会话ID
     * @param agentId    主机ID
     * @return 设置完成的信号
     */
    public Mono<Void> linkAgent(String sessionId, String agentId) {
        return getSessionInfo(sessionId)
                .flatMap(sessionInfo -> {
                    if (sessionInfo != null) {
                        sessionInfo.setLinkedAgentId(agentId);
                        return updateSessionInfo(sessionInfo);
                    }
                    return Mono.empty();
                })
                .doOnSuccess(v -> log.debug("关联主机成功: sessionId={}, agentId={}", sessionId, agentId))
                .doOnError(e -> log.error("关联主机失败: sessionId={}, agentId={}", sessionId, agentId, e))
                .then();
    }

    /**
     * 获取会话关联的主机ID
     *
     * @param sessionId 会话ID
     * @return 主机ID，无关联返回空Mono
     */
    public Mono<String> getLinkedAgent(String sessionId) {
        return getSessionInfo(sessionId)
                .flatMap(sessionInfo -> {
                    String agentId = sessionInfo != null ? sessionInfo.getLinkedAgentId() : null;
                    return agentId != null ? Mono.just(agentId) : Mono.empty();
                })
                .doOnError(e -> log.error("获取关联主机失败: sessionId={}", sessionId, e))
                .onErrorResume(e -> Mono.empty());
    }
}
