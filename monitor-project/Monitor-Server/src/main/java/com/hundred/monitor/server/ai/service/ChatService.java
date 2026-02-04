package com.hundred.monitor.server.ai.service;

import com.hundred.monitor.server.ai.entity.Message;
import com.hundred.monitor.server.ai.entity.SessionInfo;
import com.hundred.monitor.server.ai.utils.ModelRedisUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * AI聊天服务
 * 提供会话管理、消息发送、上下文管理和智能总结压缩功能
 */
@Slf4j
@Service
public class ChatService {

    @Resource
    private ModelRedisUtils redisUtils;

    @Resource(name = "defaultOpenAiChatModel")
    private OpenAiChatModel defaultOpenAiChatModel;

    @Resource(name = "getGlmAiChatModel")
    private OpenAiChatModel getGlmAiChatModel;

    @Resource(name = "getOllamaAiChatModel")
    private OpenAiChatModel getOllamaAiChatModel;

    @Value("${ai.monitor-agent.default-model-name:ollama}")
    private String defaultModelName;

    // 系统提示词
    private static final String SYSTEM_PROMPT = """
            你是一个专业的服务器运维AI助手，服务于Monitor监控系统。
            你的核心能力是通过自然语言理解，帮助用户完成服务器集群的监控、诊断和运维操作。

            ## 核心特性
            - 自然语言理解：理解用户的运维意图，解析为具体操作
            - 上下文感知：理解当前页面、选中主机、历史对话
            - 安全优先：高风险操作必须确认，防止误操作
            - 结果反馈：清晰展示操作过程和结果

            ## 你能做的
            - 解析自然语言运维指令
            - 查询主机状态和监控数据
            - 提供运维建议
            - 分析日志和监控数据

            ## 你不能做的
            - 未经用户确认执行高风险操作
            - 访问用户权限之外的主机
            - 修改系统核心配置
            - 删除重要数据
            """;

    // 总结提示词
    private static final String SUMMARY_PROMPT = """
            请将以下对话内容总结为一段简洁的摘要，保留关键信息（用户意图、重要操作、结论）：
            """;

    // ==================== 会话管理 ====================

    /**
     * 创建新会话
     *
     * @param userId    用户ID
     * @param firstMessage 首条用户消息（用于生成标题）
     * @return 会话ID
     */
    public String createSession(String userId, String firstMessage) {
        String sessionId = UUID.randomUUID().toString();
        // 生成标题（取前20个字符）
        String title = firstMessage.length() > 20
                ? firstMessage.substring(0, 20) + "..."
                : firstMessage;

        redisUtils.createSession(userId, sessionId, title);
        log.info("创建会话: userId={}, sessionId={}, title={}", userId, sessionId, title);
        return sessionId;
    }

    /**
     * 获取会话信息
     *
     * @param userId       用户ID
     * @param firstMessage 首条用户消息（用于生成标题）
     * @param sessionId    前端传入的会话ID，获取方式：前端已经和SSH建立连接
     */
    public void createSession(String userId, String firstMessage, String sessionId) {
        // 生成标题（取前20个字符）
        String title = firstMessage.length() > 20
                ? firstMessage.substring(0, 20) + "..."
                : firstMessage;

        redisUtils.createSession(userId, sessionId, title);
        log.info("创建会话: userId={}, sessionId={}, title={}", userId, sessionId, title);
    }
    /**
     * 获取会话信息
     *
     * @param sessionId 会话ID
     * @return 会话信息
     */
    public SessionInfo getSession(String sessionId) {
        return redisUtils.getSessionInfo(sessionId);
    }

    /**
     * 获取用户的所有会话
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    public List<SessionInfo> getUserSessions(String userId) {
        return redisUtils.getUserSessions(userId);
    }

    /**
     * 删除会话
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     */
    public void deleteSession(String userId, String sessionId) {
        redisUtils.deleteSession(userId, sessionId);
        log.info("删除会话: userId={}, sessionId={}", userId, sessionId);
    }

    // ==================== 消息处理 ====================

    /**
     * 发送消息并获取AI回复
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @param userMessage 用户消息
     * @param modelName 模型名称（可选，null使用默认）
     * @return AI回复
     */
    public String sendMessage(String sessionId, String userId, String userMessage, String modelName) {
        // 检查会话是否存在
        if (!redisUtils.sessionExists(sessionId)) {
            throw new IllegalArgumentException("会话不存在: " + sessionId);
        }

        // 构建用户消息
        Message userMsg = Message.builder()
                .role("user")
                .content(userMessage)
                .timestamp(Instant.now().toEpochMilli())
                .build();

        // 保存用户消息
        redisUtils.addMessage(sessionId, userMsg);

        // 刷新会话时间
        redisUtils.refreshSessionTime(userId, sessionId);

        // 获取上下文消息
        List<ChatMessage> contextMessages = buildContext(sessionId);

        // 调用AI模型
        String aiResponse = callAI(contextMessages, modelName);

        // 构建AI消息
        Message aiMsg = Message.builder()
                .role("assistant")
                .content(aiResponse)
                .timestamp(Instant.now().toEpochMilli())
                .build();

        // 保存AI消息
        redisUtils.addMessage(sessionId, aiMsg);

        // 检查是否需要总结压缩
        if (redisUtils.needsSummary(sessionId)) {
            performSummary(sessionId);
        }

        return aiResponse;
    }

    /**
     * 获取会话的消息历史
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    public List<Message> getMessages(String sessionId) {
        return redisUtils.getMessages(sessionId);
    }

    /**
     * 清空会话消息
     *
     * @param sessionId 会话ID
     */
    public void clearMessages(String sessionId) {
        redisUtils.clearMessages(sessionId);
        log.info("清空会话消息: sessionId={}", sessionId);
    }

    // ==================== 上下文管理 ====================

    /**
     * 构建对话上下文（包括总结和近期消息）
     *
     * @param sessionId 会话ID
     * @return 上下文消息列表
     */
    private List<ChatMessage> buildContext(String sessionId) {
        List<ChatMessage> messages = new ArrayList<>();

        // 添加系统提示词
        messages.add(new UserMessage(SYSTEM_PROMPT));

        // 获取会话总结
        String summary = redisUtils.getSummary(sessionId);
        if (summary != null && !summary.isEmpty()) {
            // 将总结作为系统消息添加
            messages.add(new UserMessage("[历史对话总结]\n" + summary + "\n[结束总结，以下是后续对话]"));
        }

        // 获取近期消息（最多10条）
        List<Message> recentMessages = redisUtils.getRecentMessages(sessionId, 10);
        for (Message msg : recentMessages) {
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(new AiMessage(msg.getContent()));
            }
        }

        return messages;
    }

    /**
     * 调用AI模型生成回复
     *
     * @param messages  上下文消息
     * @param modelName 模型名称
     * @return AI回复
     */
    private String callAI(List<ChatMessage> messages, String modelName) {
        try {
            ChatLanguageModel model = selectModel(modelName);
            return model.chat(messages).aiMessage().text();
        } catch (Exception e) {
            log.error("AI调用失败", e);
            return "抱歉，AI服务暂时不可用，请稍后再试。";
        }
    }

    /**
     * 选择AI模型
     *
     * @param modelName 模型名称（ollama或glm）
     * @return ChatLanguageModel实例
     */
    private ChatLanguageModel selectModel(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            modelName = defaultModelName;
        }

        return switch (modelName.toLowerCase()) {
            case "glm" -> getGlmAiChatModel;
            case "ollama" -> getOllamaAiChatModel;
            default -> getOllamaAiChatModel;
        };
    }

    // ==================== 智能总结压缩 ====================

    /**
     * 执行会话总结压缩
     *
     * @param sessionId 会话ID
     */
    private void performSummary(String sessionId) {
        try {
            log.info("开始执行会话总结: sessionId={}", sessionId);

            // 获取所有消息
            List<Message> allMessages = redisUtils.getMessages(sessionId);

            // 构建总结用的对话文本
            StringBuilder conversation = new StringBuilder();
            for (Message msg : allMessages) {
                String role = "user".equals(msg.getRole()) ? "用户" : "助手";
                conversation.append(role).append(": ").append(msg.getContent()).append("\n");
            }

            // 调用AI生成总结
            String summary = generateSummary(conversation.toString());

            // 执行压缩
            redisUtils.compressMessages(sessionId, summary);

            log.info("会话总结完成: sessionId={}, summary={}", sessionId, summary);
        } catch (Exception e) {
            log.error("会话总结失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 生成对话总结
     *
     * @param conversation 对话文本
     * @return 总结内容
     */
    private String generateSummary(String conversation) {
        try {
            // 使用默认模型生成总结
            List<ChatMessage> messages = List.of(
                    new UserMessage(SUMMARY_PROMPT + "\n\n" + conversation)
            );
            return selectModel(null).chat(messages).aiMessage().text();
        } catch (Exception e) {
            log.error("生成总结失败", e);
            return "对话总结生成失败";
        }
    }

    // ==================== 主机关联 ====================

    /**
     * 关联主机到会话
     *
     * @param sessionId 会话ID
     * @param agentId   主机ID
     */
    public void linkAgent(String sessionId, String agentId) {
        redisUtils.linkAgent(sessionId, agentId);
        log.info("关联主机到会话: sessionId={}, agentId={}", sessionId, agentId);
    }

    /**
     * 获取会话关联的主机ID
     *
     * @param sessionId 会话ID
     * @return 主机ID
     */
    public String getLinkedAgent(String sessionId) {
        return redisUtils.getLinkedAgent(sessionId);
    }
}
