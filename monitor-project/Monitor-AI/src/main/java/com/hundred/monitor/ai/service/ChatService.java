package com.hundred.monitor.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundred.monitor.ai.constant.ChatConstants;
import com.hundred.monitor.ai.constant.ErrorConstants;
import com.hundred.monitor.ai.exception.AiServiceException;
import com.hundred.monitor.ai.exception.ChatSessionNotFoundException;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * AI聊天服务（侧边栏AI助手 - HTTP REST API）
 * 提供会话管理、消息发送、上下文管理和智能总结压缩功能
 *
 * 类型说明：
 * - ChatMessage (无前缀) = 项目自己的消息类，用于Redis存储
 * - dev.langchain4j.data.message.ChatMessage = LangChain4j消息类，用于AI调用
 */
@Slf4j
@Service
public class ChatService {

    @Resource
    private ChatSessionRedisReactiveUtils redisUtils;

    @Resource(name = "defaultOpenAiChatAssistant")
    private ChatAssistant defaultChatAssistant;

    @Resource(name = "glmAiChatAssistant")
    private ChatAssistant glmAiChatAssistant;

    @Resource(name = "ollamaAiChatAssistant")
    private ChatAssistant ollamaAiChatAssistant;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${ai.monitor-agent.default-model-name:ollama}")
    private String defaultModelName;

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
        String title = generateTitle(firstMessage);

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
        String title = generateTitle(firstMessage);

        return redisUtils.createSession(userId, sessionId, title)
                .doOnSuccess(v -> log.info("指定会话ID创建会话: userId={}, sessionId={}, title={}", userId, sessionId, title))
                .doOnError(e -> log.error("创建会话失败: userId={}, sessionId={}", userId, sessionId, e))
                .then();
    }

    /**
     * 生成会话标题
     *
     * @param message 首条消息
     * @return 标题
     */
    private String generateTitle(String message) {
        if (message.length() > ChatConstants.SESSION_TITLE_MAX_LENGTH) {
            return message.substring(0, ChatConstants.SESSION_TITLE_MAX_LENGTH) + ChatConstants.SESSION_TITLE_SUFFIX;
        }
        return message;
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
        log.info("发送消息: sessionId={}, userId={}, messageLength={}", sessionId, userId, userMessage.length());

        return validateAndPrepareSession(sessionId)
                .flatMapMany(validId -> executeChatFlow(validId, userId, userMessage, modelName))
                .doOnError(e -> log.error("发送消息失败: sessionId={}, userId={}", sessionId, userId, e));
    }

    /**
     * 验证会话并返回会话ID
     */
    private Mono<String> validateAndPrepareSession(String sessionId) {
        return redisUtils.sessionExists(sessionId)
                .flatMap(exists -> {
                    if (Boolean.FALSE.equals(exists)) {
                        return Mono.error(new ChatSessionNotFoundException(sessionId));
                    }
                    return Mono.just(sessionId);
                });
    }

    /**
     * 执行聊天流程：保存消息 -> 刷新会话 -> 调用AI -> 触发总结
     */
    private Flux<String> executeChatFlow(String sessionId, String userId, String userMessage, String modelName) {
        ChatMessage userMsg = createUserMessage(userMessage);

        return redisUtils.addMessage(sessionId, userMsg)
                .then(redisUtils.refreshSessionTime(userId, sessionId))  // 使用then忽略Boolean返回值
                .thenMany(Flux.defer(() -> callAiWithContext(sessionId, modelName)))
                .doOnComplete(() -> triggerAsyncSummary(sessionId));
    }

    /**
     * 创建用户消息
     */
    private ChatMessage createUserMessage(String content) {
        return ChatMessage.builder()
                .role("user")
                .content(content)
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }

    /**
     * 构建上下文并调用AI
     */
    private Flux<String> callAiWithContext(String sessionId, String modelName) {
        return buildContext(sessionId)
                .flatMapMany(contextMessages -> {
                    log.info("AI调用: sessionId={}, modelName={}, contextSize={}", sessionId, modelName, contextMessages.size());
                    return callAI(contextMessages, modelName);
                });
    }

    /**
     * 触发异步总结（不影响主流程）
     */
    private void triggerAsyncSummary(String sessionId) {
        redisUtils.needsSummary(sessionId)
                .filter(needSummary -> needSummary)
                .flatMap(needSummary -> performSummary(sessionId))
                .subscribe();
    }

    /**
     * 发送消息并自动保存AI回复（封装版本，供Controller使用）
     *
     * @param sessionId   会话ID
     * @param userId      用户ID
     * @param userMessage 用户消息
     * @param modelName   模型名称
     * @return AI回复流
     */
    public Flux<String> sendMessageStreamWithSave(String sessionId, String userId, String userMessage, String modelName) {
        StringBuilder fullResponse = new StringBuilder();

        return sendMessage(sessionId, userId, userMessage, modelName)
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> saveAiResponse(sessionId, fullResponse.toString()))
                .doOnError(e -> log.error("发送消息流失败: sessionId={}", sessionId, e));
    }

    /**
     * 保存AI回复消息
     */
    private void saveAiResponse(String sessionId, String content) {
        log.debug("保存AI回复: sessionId={}, length={}", sessionId, content.length());

        ChatMessage aiMsg = ChatMessage.builder()
                .role("assistant")
                .content(content)
                .timestamp(Instant.now().toEpochMilli())
                .build();

        redisUtils.addMessage(sessionId, aiMsg)
                .doOnSuccess(count -> log.debug("AI消息保存成功: sessionId={}, count={}", sessionId, count))
                .doOnError(e -> log.error("AI消息保存失败: sessionId={}", sessionId, e))
                .subscribe();
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
     * @return LangChain4j上下文消息列表
     */
    private Mono<List<dev.langchain4j.data.message.ChatMessage>> buildContext(String sessionId) {
        return redisUtils.getSummary(sessionId)
                .switchIfEmpty(Mono.just(""))
                .map(this::createBaseContextMessages)
                .flatMap(baseMessages -> appendRecentMessages(baseMessages, sessionId))
                .doOnError(e -> log.error("构建上下文失败: sessionId={}", sessionId, e))
                .onErrorResume(e -> Mono.just(createFallbackContext()));
    }

    /**
     * 创建基础上下文消息（系统提示词 + 总结）
     */
    private List<dev.langchain4j.data.message.ChatMessage> createBaseContextMessages(String summary) {
        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage(SystemPrompt.getSystemPrompt()));

        if (summary != null && !summary.isEmpty()) {
            messages.add(new UserMessage("[历史对话总结]\n" + summary + "\n[结束总结，以下是后续对话]"));
        }

        return messages;
    }

    /**
     * 添加近期消息到上下文
     */
    private Mono<List<dev.langchain4j.data.message.ChatMessage>> appendRecentMessages(
            List<dev.langchain4j.data.message.ChatMessage> baseMessages, String sessionId) {

        return redisUtils.getRecentMessagesAsList(sessionId, ChatConstants.RECENT_MESSAGE_COUNT)
                .map(recentMessages -> {
                    for (ChatMessage msg : recentMessages) {
                        baseMessages.add(convertToLangChainMessage(msg));
                    }
                    return baseMessages;
                });
    }

    /**
     * 将项目ChatMessage转换为LangChain4j消息
     */
    private dev.langchain4j.data.message.ChatMessage convertToLangChainMessage(ChatMessage msg) {
        return switch (msg.getRole()) {
            case "user" -> new UserMessage(msg.getContent());
            case "assistant" -> new AiMessage(msg.getContent());
            default -> new UserMessage(msg.getContent());
        };
    }

    /**
     * 创建降级上下文（仅包含系统提示词）
     */
    private List<dev.langchain4j.data.message.ChatMessage> createFallbackContext() {
        List<dev.langchain4j.data.message.ChatMessage> fallback = new ArrayList<>();
        fallback.add(new SystemMessage(SystemPrompt.getSystemPrompt()));
        return fallback;
    }

    /**
     * 调用AI模型生成回复
     *
     * @param messages  LangChain4j上下文消息
     * @param modelName 模型名称
     * @return AI回复流
     */
    private Flux<String> callAI(List<dev.langchain4j.data.message.ChatMessage> messages, String modelName) {
        try {
            String messagesJson = convertMessagesToJson(messages);
            log.debug("发送消息到AI: length={}, modelName={}", messagesJson.length(), modelName);

            ChatAssistant model = selectModel(modelName);
            return model.chat(messagesJson)
                    .doOnComplete(() -> log.info("AI回复完成: modelName={}", modelName))
                    .doOnError(e -> log.error("AI调用异常: modelName={}", modelName, e));
        } catch (Exception e) {
            log.error("AI调用失败: modelName={}", modelName, e);
            throw new AiServiceException(ErrorConstants.AI_MODEL_ERROR);
        }
    }

    /**
     * 将 LangChain4j 消息列表转换为 JSON 字符串
     * 格式：[{"role":"system","message":"..."},{"role":"user","message":"..."}]
     *
     * @param messages LangChain4j 消息列表
     * @return JSON 字符串
     */
    private String convertMessagesToJson(List<dev.langchain4j.data.message.ChatMessage> messages) {
        try {
            List<MessageJson> jsonMessages = new ArrayList<>();

            for (dev.langchain4j.data.message.ChatMessage msg : messages) {
                String role;
                String content;

                if (msg instanceof SystemMessage) {
                    role = "system";
                    content = ((SystemMessage) msg).text();
                } else if (msg instanceof UserMessage) {
                    role = "user";
                    content = ((UserMessage) msg).singleText();
                } else if (msg instanceof AiMessage) {
                    role = "assistant";
                    content = ((AiMessage) msg).text();
                } else {
                    role = "unknown";
                    content = "未知消息类型";
                }

                jsonMessages.add(new MessageJson(role, content));
            }

            return objectMapper.writeValueAsString(jsonMessages);
        } catch (Exception e) {
            log.error("消息转换为JSON失败", e);
            return "[]";
        }
    }

    /**
     * 消息JSON格式（内部类）
     */
    private record MessageJson(String role, String message) {
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
            case "glm" -> glmAiChatAssistant;
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
                .map(this::buildConversationText)
                .flatMap(this::generateSummary)
                .flatMap(summary -> redisUtils.compressMessages(sessionId, summary))
                .doOnSuccess(v -> log.info("会话总结完成: sessionId={}", sessionId))
                .doOnError(e -> log.error("会话总结失败: sessionId={}", sessionId, e));
    }

    /**
     * 构建对话文本（用于总结）
     *
     * @param messages 项目消息列表
     * @return 对话文本
     */
    private String buildConversationText(List<ChatMessage> messages) {
        StringBuilder conversation = new StringBuilder();
        for (ChatMessage msg : messages) {
            String role = "user".equals(msg.getRole()) ? "用户" : "助手";
            conversation.append(role).append(": ").append(msg.getContent()).append("\n");
        }
        return conversation.toString();
    }

    /**
     * 生成对话总结（完全异步实现）
     *
     * @param conversation 对话文本
     * @return 总结内容
     */
    private Mono<String> generateSummary(String conversation) {
        // 构建总结消息列表
        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage("你是一个对话总结助手，请将以下对话内容总结为一段简洁的摘要。"));
        messages.add(new UserMessage(ChatConstants.SUMMARY_PROMPT + "\n\n" + conversation));

        // 转换为JSON字符串
        String messagesJson = convertMessagesToJson(messages);

        // 调用AI生成总结（异步操作）
        ChatAssistant assistant = selectModel(null);

        return assistant.chat(messagesJson)
                .collectList()
                .map(chunks -> String.join("", chunks))
                .map(summary -> summary.isEmpty() ? "对话总结生成失败" : summary)
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
