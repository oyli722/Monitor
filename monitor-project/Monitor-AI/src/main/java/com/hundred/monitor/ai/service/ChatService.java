package com.hundred.monitor.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundred.monitor.ai.model.ChatAssistant;
import com.hundred.monitor.ai.utils.ChatSessionRedisReactiveUtils;
import com.hundred.monitor.commonlibrary.ai.model.ChatMessage;
import com.hundred.monitor.commonlibrary.ai.model.ChatSessionInfo;
import com.hundred.monitor.commonlibrary.ai.model.SystemPrompt;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * AI聊天服务（侧边栏AI助手 - HTTP REST API）
 * 提供会话管理、消息发送、上下文管理和智能总结压缩功能
 */
@Slf4j
@Service
public class ChatService {

    @Resource
    private ChatSessionRedisReactiveUtils redisUtils;

    @Resource(name = "defaultOpenAiChatAssistant")
    private ChatAssistant defaultChatAssistant;

    @Resource(name = "glmAiChatAssistant")
    private ChatAssistant jlmAiChatAssistant;

    @Resource(name = "ollamaAiChatAssistant")
    private ChatAssistant ollamaAiChatAssistant;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${ai.monitor-agent.default-model-name:ollama}")
    private String defaultModelName;

    // 总结提示词
    private static final String SUMMARY_PROMPT = """
            请将以下对话内容总结为一段简洁的摘要，保留关键信息（用户意图、重要操作、结论）：
            """;

    // ==================== 会话管理 ====================

    /**
     * 创建新会话
     *
     * @param userId        用户ID
     * @param firstMessage  首条用户消息（用于生成标题）
     * @return 会话ID
     */
    public Mono<String> createSession(String userId, String firstMessage) {
        String sessionId = UUID.randomUUID().toString();
        // 生成标题（取前20个字符）
        String title = firstMessage.length() > 20
                ? firstMessage.substring(0, 20) + "..."
                : firstMessage;

        return redisUtils.createSession(userId, sessionId, title)
                .thenReturn(sessionId)
                .doOnNext(id -> log.info("创建会话: userId={}, sessionId={}, title={}", userId, id, title))
                .doOnError(e -> log.error("创建会话失败: userId={}, sessionId={}", userId, sessionId, e));
    }

    /**
     * 创建新会话（指定会话ID）
     *
     * @param userId        用户ID
     * @param firstMessage  首条用户消息（用于生成标题）
     * @param sessionId     前端传入的会话ID
     */
    public Mono<Void> createSession(String userId, String firstMessage, String sessionId) {
        // 生成标题（取前20个字符）
        String title = firstMessage.length() > 20
                ? firstMessage.substring(0, 20) + "..."
                : firstMessage;

        return redisUtils.createSession(userId, sessionId, title)
                .doOnSuccess(v -> log.info("创建会话: userId={}, sessionId={}, title={}", userId, sessionId, title))
                .doOnError(e -> log.error("创建会话失败: userId={}, sessionId={}", userId, sessionId, e))
                .then();
    }

    /**
     * 获取会话信息
     *
     * @param sessionId 会话ID
     * @return 会话信息
     */
    public Mono<ChatSessionInfo> getSession(String sessionId) {
        return redisUtils.getSessionInfo(sessionId)
                .doOnError(e -> log.error("获取会话信息失败: sessionId={}", sessionId, e));
    }

    /**
     * 获取用户的所有会话
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    public Mono<List<ChatSessionInfo>> getUserSessions(String userId) {
        return redisUtils.getUserSessions(userId)
                .doOnError(e -> log.error("获取用户会话列表失败: userId={}", userId, e));
    }

    /**
     * 删除会话
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     */
    public Mono<Void> deleteSession(String userId, String sessionId) {
        return redisUtils.deleteSession(userId, sessionId)
                .doOnSuccess(v -> log.info("删除会话: userId={}, sessionId={}", userId, sessionId))
                .doOnError(e -> log.error("删除会话失败: userId={}, sessionId={}", userId, sessionId, e));
    }

    // ==================== 消息处理 ====================

    /**
     * 发送消息并获取AI回复
     *
     * @param sessionId   会话ID
     * @param userId      用户ID
     * @param userMessage 用户消息
     * @param modelName   模型名称（可选，null使用默认）
     * @return AI回复流
     */
    public Flux<String> sendMessage(String sessionId, String userId, String userMessage, String modelName) {
        // 检查会话是否存在
        return redisUtils.sessionExists(sessionId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new IllegalArgumentException("会话不存在: " + sessionId));
                    }
                    return Mono.just(sessionId);
                })
                .flatMapMany(id -> {
                    // 构建用户消息
                    ChatMessage userMsg = ChatMessage.builder()
                            .role("user")
                            .content(userMessage)
                            .timestamp(Instant.now().toEpochMilli())
                            .build();

                    // 保存用户消息并刷新会话时间
                    return redisUtils.addMessage(sessionId, userMsg)
                            .then(redisUtils.refreshSessionTime(userId, sessionId))
                            .thenMany(Flux.defer(() -> {
                                // 获取上下文消息
                                return buildContext(sessionId)
                                        .flatMapMany(contextMessages -> {
                                            // 调用AI模型
                                            return callAI(contextMessages, modelName)
                                                    // 收集完整回复并保存
                                                    .collectList()
                                                    .flatMapMany(chunks -> {
                                                        String content = String.join("", chunks);
                                                        log.info("AI回复: {}", content);
                                                        ChatMessage aiMsg = ChatMessage.builder()
                                                                .role("assistant")
                                                                .content(content)
                                                                .timestamp(Instant.now().toEpochMilli())
                                                                .build();

                                                        return redisUtils.addMessage(sessionId, aiMsg)
                                                                .thenMany(Flux.fromIterable(chunks));
                                                    });
                                        });
                            }))
                            // 检查是否需要总结压缩（异步执行，不影响主流程）
                            .doOnComplete(() -> {
                                redisUtils.needsSummary(sessionId)
                                        .filter(needSummary -> needSummary)
                                        .flatMap(needSummary -> performSummary(sessionId))
                                        .subscribe();
                            });
                })
                .doOnError(e -> log.error("发送消息失败: sessionId={}, userId={}, userMessage={}", sessionId, userId, userMessage, e));
    }

    /**
     * 获取会话的消息历史
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    public Flux<ChatMessage> getMessages(String sessionId) {
        return redisUtils.getMessages(sessionId)
                .doOnError(e -> log.error("获取消息历史失败: sessionId={}", sessionId, e));
    }

    /**
     * 获取会话的消息历史（返回 Mono<List>）
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    public Mono<List<ChatMessage>> getMessagesAsList(String sessionId) {
        return redisUtils.getMessagesAsList(sessionId)
                .doOnError(e -> log.error("获取消息历史失败: sessionId={}", sessionId, e));
    }

    /**
     * 清空会话消息
     *
     * @param sessionId 会话ID
     */
    public Mono<Void> clearMessages(String sessionId) {
        return redisUtils.clearMessages(sessionId)
                .doOnSuccess(v -> log.info("清空会话消息: sessionId={}", sessionId))
                .doOnError(e -> log.error("清空消息失败: sessionId={}", sessionId, e));
    }

    // ==================== 上下文管理 ====================

    /**
     * 构建对话上下文（包括总结和近期消息）
     *
     * @param sessionId 会话ID
     * @return 上下文消息列表
     */
    private Mono<List<dev.langchain4j.data.message.ChatMessage>> buildContext(String sessionId) {
        // 获取会话总结
        return redisUtils.getSummary(sessionId)
                .flatMap(summary -> {
                    List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();

                    // 添加系统提示词
                    messages.add(new SystemMessage(SystemPrompt.getSystemPrompt()));

                    // 添加总结（如果存在）
                    if (summary != null && !summary.isEmpty()) {
                        messages.add(new UserMessage("[历史对话总结]\n" + summary + "\n[结束总结，以下是后续对话]"));
                    }

                    return Mono.just(messages);
                })
                .flatMap(messages ->
                    // 获取近期消息（最多10条）并添加到上下文
                    redisUtils.getRecentMessagesAsList(sessionId, 10)
                        .map(recentMessages -> {
                            for (ChatMessage msg : recentMessages) {
                                if ("user".equals(msg.getRole())) {
                                    messages.add(new UserMessage(msg.getContent()));
                                } else if ("assistant".equals(msg.getRole())) {
                                    messages.add(new AiMessage(msg.getContent()));
                                }
                            }
                            return messages;
                        })
                )
                .doOnError(e -> log.error("构建上下文失败: sessionId={}", sessionId, e))
                .onErrorResume(e -> {
                    // 返回只有系统提示词的最小上下文
                    List<dev.langchain4j.data.message.ChatMessage> fallback = new ArrayList<>();
                    fallback.add(new SystemMessage(SystemPrompt.getSystemPrompt()));
                    return Mono.just(fallback);
                });
    }

    /**
     * 调用AI模型生成回复
     *
     * @param messages  上下文消息
     * @param modelName 模型名称
     * @return AI回复流
     */
    private Flux<String> callAI(List<dev.langchain4j.data.message.ChatMessage> messages, String modelName) {
        return Mono.fromCallable(() -> selectModel(modelName))
                .flatMapMany(model -> {
                    try {
                        // 调用 ChatAssistant.chat()，传递消息列表
                        return model.chat(messages);
                    } catch (Exception e) {
                        log.error("AI调用失败: modelName={}", modelName, e);
                        return Flux.just("抱歉，AI服务暂时不可用，请稍后再试。");
                    }
                })
                .doOnError(e -> log.error("AI流式输出失败: modelName={}", modelName, e));
    }

    /**
     * 选择AI模型
     *
     * @param modelName 模型名称（ollama或glm）
     * @return ChatAssistant 实例
     */
    private ChatAssistant selectModel(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            modelName = defaultModelName;
        }

        return switch (modelName.toLowerCase()) {
            case "glm" -> jlmAiChatAssistant;
            case "ollama" -> ollamaAiChatAssistant;
            default -> defaultChatAssistant;
        };
    }

    // ==================== 智能总结压缩 ====================

    /**
     * 执行会话总结压缩
     *
     * @param sessionId 会话ID
     */
    private Mono<Void> performSummary(String sessionId) {
        return redisUtils.getMessagesAsList(sessionId)
                .flatMap(allMessages -> {
                    // 构建总结用的对话文本
                    StringBuilder conversation = new StringBuilder();
                    for (ChatMessage msg : allMessages) {
                        String role = "user".equals(msg.getRole()) ? "用户" : "助手";
                        conversation.append(role).append(": ").append(msg.getContent()).append("\n");
                    }

                    // 调用AI生成总结
                    return generateSummary(conversation.toString());
                })
                .flatMap(summary -> redisUtils.compressMessages(sessionId, summary))
                .doOnSuccess(v -> log.info("会话总结完成: sessionId={}", sessionId))
                .doOnError(e -> log.error("会话总结失败: sessionId={}", sessionId, e));
    }

    /**
     * 生成对话总结
     *
     * @param conversation 对话文本
     * @return 总结内容
     */
    private Mono<String> generateSummary(String conversation) {
        return Mono.fromCallable(() -> {
            // 构建总结消息
            List<dev.langchain4j.data.message.ChatMessage> messages = List.of(
                    new UserMessage(SUMMARY_PROMPT + "\n\n" + conversation)
            );

            // 调用AI生成总结，收集完整响应
            ChatAssistant assistant = selectModel(null);
            StringBuilder summaryBuilder = new StringBuilder();

            // 注意：这里需要同步等待结果，因为是 Mono.fromCallable 内部
            assistant.chat(messages)
                    .doOnNext(summaryBuilder::append)
                    .blockLast(); // 阻塞等待完成

            String summary = summaryBuilder.toString();
            if (summary == null || summary.isEmpty()) {
                return "对话总结生成失败";
            }
            return summary;
        })
                .doOnError(e -> log.error("生成总结失败", e));
    }

    // ==================== 主机关联 ====================

    /**
     * 关联主机到会话
     *
     * @param sessionId 会话ID
     * @param agentId   主机ID
     */
    public Mono<Void> linkAgent(String sessionId, String agentId) {
        return redisUtils.linkAgent(sessionId, agentId)
                .doOnSuccess(v -> log.info("关联主机到会话: sessionId={}, agentId={}", sessionId, agentId))
                .doOnError(e -> log.error("关联主机失败: sessionId={}, agentId={}", sessionId, agentId, e));
    }

    /**
     * 获取会话关联的主机ID
     *
     * @param sessionId 会话ID
     * @return 主机ID
     */
    public Mono<String> getLinkedAgent(String sessionId) {
        return redisUtils.getLinkedAgent(sessionId)
                .doOnError(e -> log.error("获取关联主机失败: sessionId={}", sessionId, e));
    }
}
