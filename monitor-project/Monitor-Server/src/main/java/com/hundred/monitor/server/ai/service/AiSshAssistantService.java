package com.hundred.monitor.server.ai.service;

import com.hundred.monitor.server.ai.context.SshSessionContext;
import com.hundred.monitor.server.ai.entity.Message;
import com.hundred.monitor.server.ai.entity.SshSessionBinding;
import com.hundred.monitor.server.ai.utils.AiSshRedisUtils;
import com.hundred.monitor.server.service.AgentService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
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

/**
 * 主机详情页AI助手服务
 * 处理AI对话逻辑、上下文构建、消息管理
 */
@Slf4j
@Service
public class AiSshAssistantService {

    @Resource
    private AiSshRedisUtils aiSshRedisUtils;

    @Resource(name = "getOllamaAiChatModel")
    private OpenAiChatModel ollamaChatModel;

    @Resource(name = "getGlmAiChatModel")
    private OpenAiChatModel glmChatModel;

    @Resource
    private AgentService agentService;

    @Value("${ai.monitor-agent.default-model-name:ollama}")
    private String defaultModelName;

    // 消息数量阈值（超过此数量触发总结）
    private static final int MESSAGE_THRESHOLD = 20;

    // 系统提示词
    private static final String SYSTEM_PROMPT = """
            你是一个专业的服务器运维AI助手，服务于Monitor监控系统。
            你的核心能力是通过自然语言理解，帮助用户完成服务器集群的监控、诊断和运维操作。

            ## 核心特性
            - 自然语言理解：理解用户的运维意图，解析为具体操作
            - 上下文感知：理解当前操作的主机信息
            - 安全优先：高风险操作必须确认，防止误操作
            - 结果反馈：清晰展示操作过程和结果

            ## 你能做的
            - 解析自然语言运维指令
            - 调用工具执行SSH命令（executeCommand工具）
            - 查询主机状态和监控数据
            - 提供运维建议
            - 分析日志和监控数据

            ## 可用工具
            - executeCommand(command): 在当前SSH终端执行命令，返回命令已发送提示
            - getCurrentAgentId(): 获取当前SSH会话关联的主机ID
            - testToolIfAvailable(): 测试SSH执行工具是否可用

            ## 你不能做的
            - 未经用户确认执行高风险操作
            - 访问用户权限之外的主机
            - 修改系统核心配置
            - 删除重要数据
            """;

    // ==================== 核心对话方法 ====================

    /**
     * 发送消息并获取AI回复
     *
     * @param aiSessionId AI会话ID
     * @param userMessage 用户消息内容
     * @return AI回复内容
     */
    public String sendMessage(String aiSessionId, String userMessage) {
        // 1. 获取绑定关系
        SshSessionBinding binding = aiSshRedisUtils.getBinding(aiSessionId);
        if (binding == null) {
            throw new IllegalArgumentException("AI会话不存在或已过期: " + aiSessionId);
        }

        // 2. 保存用户消息
        Message userMsg = Message.builder()
                .role("user")
                .content(userMessage)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        aiSshRedisUtils.addMessage(aiSessionId, userMsg);

        // 3. 构建AI上下文
        List<ChatMessage> context = buildContext(binding);

        // 4. 设置ThreadLocal上下文（供SshExecuteTool使用）
        SshSessionContext.setSshSessionId(binding.getSshSessionId());
        SshSessionContext.setAgentId(binding.getAgentId());

        String aiReply;
        try {
            // 5. 调用AI模型（内部可能调用SshExecuteTool）
            aiReply = callAI(context, binding.getAgentId());
        } finally {
            // 6. 清理ThreadLocal上下文（防止内存泄漏）
            SshSessionContext.clear();
        }

        // 7. 保存AI回复
        Message aiMsg = Message.builder()
                .role("assistant")
                .content(aiReply)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        aiSshRedisUtils.addMessage(aiSessionId, aiMsg);

        // 8. 检查是否需要总结压缩
        checkAndCompress(aiSessionId);

        log.debug("AI对话: aiSessionId={}, agentId={}, userMsg={}",
                aiSessionId, binding.getAgentId(), userMessage);

        return aiReply;
    }

    /**
     * 获取会话的消息历史
     *
     * @param aiSessionId AI会话ID
     * @return 消息列表
     */
    public List<Message> getMessages(String aiSessionId) {
        return aiSshRedisUtils.getMessages(aiSessionId);
    }

    /**
     * 清空会话消息
     *
     * @param aiSessionId AI会话ID
     */
    public void clearMessages(String aiSessionId) {
        aiSshRedisUtils.clearMessages(aiSessionId);
        log.info("清空AI会话消息: aiSessionId={}", aiSessionId);
    }

    // ==================== 上下文构建 ====================

    /**
     * 构建AI对话上下文
     *
     * @param binding 绑定关系
     * @return 上下文消息列表
     */
    private List<ChatMessage> buildContext(SshSessionBinding binding) {
        List<ChatMessage> context = new ArrayList<>();

        // 1. 添加系统提示词（带主机上下文）
        String systemPromptWithContext = buildSystemPrompt(binding);
        context.add(new SystemMessage(systemPromptWithContext));

        // 2. 添加历史消息（最近10条）
        List<Message> recentMessages = aiSshRedisUtils.getRecentMessages(
                binding.getAiSessionId(), 10);

        for (Message msg : recentMessages) {
            if ("user".equals(msg.getRole())) {
                context.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                context.add(new AiMessage(msg.getContent()));
            }
        }

        return context;
    }

    /**
     * 构建带主机上下文的系统提示词
     *
     * @param binding 绑定关系
     * @return 系统提示词
     */
    private String buildSystemPrompt(SshSessionBinding binding) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_PROMPT);
        sb.append("\n\n## 当前上下文\n");
        sb.append("- 主机ID: ").append(binding.getAgentId()).append("\n");

        // 尝试获取主机详细信息
        try {
            var agent = agentService.getAgentById(binding.getAgentId());
            if (agent != null) {
                sb.append("- 主机名: ").append(agent.getHostname()).append("\n");
                sb.append("- IP地址: ").append(agent.getIp()).append("\n");
            }
        } catch (Exception e) {
            log.warn("获取主机信息失败: agentId={}", binding.getAgentId(), e);
        }

        sb.append("\n请在回复时考虑当前主机的信息。\n");

        return sb.toString();
    }

    // ==================== AI调用 ====================

    /**
     * 调用AI模型生成回复
     *
     * @param context  上下文消息
     * @param agentId  主机ID（用于选择模型）
     * @return AI回复内容
     */
    private String callAI(List<ChatMessage> context, String agentId) {
        try {
            ChatLanguageModel model = selectModel(defaultModelName);

            // 调用LangChain4j的chat方法
            var response = model.chat(context);

            return response.aiMessage().text();

        } catch (Exception e) {
            log.error("AI调用失败: agentId={}", agentId, e);
            return "抱歉，AI服务暂时不可用，请稍后再试。错误: " + e.getMessage();
        }
    }

    /**
     * 选择AI模型
     *
     * @param modelName 模型名称（ollama或glm）
     * @return ChatLanguageModel实例
     */
    private ChatLanguageModel selectModel(String modelName) {
        if ("glm".equalsIgnoreCase(modelName)) {
            return glmChatModel;
        } else {
            return ollamaChatModel; // 默认使用Ollama
        }
    }

    // ==================== 总结压缩 ====================

    /**
     * 检查并执行消息压缩
     *
     * @param aiSessionId AI会话ID
     */
    private void checkAndCompress(String aiSessionId) {
        if (aiSshRedisUtils.needsSummary(aiSessionId, MESSAGE_THRESHOLD)) {
            try {
                performSummary(aiSessionId);
            } catch (Exception e) {
                log.error("消息压缩失败: aiSessionId={}", aiSessionId, e);
            }
        }
    }

    /**
     * 执行会话总结压缩
     *
     * @param aiSessionId AI会话ID
     */
    private void performSummary(String aiSessionId) {
        log.info("开始执行会话总结: aiSessionId={}", aiSessionId);

        // 1. 获取所有消息
        List<Message> allMessages = aiSshRedisUtils.getMessages(aiSessionId);

        // 2. 构建总结用的对话文本
        StringBuilder conversation = new StringBuilder();
        for (Message msg : allMessages) {
            String role = "user".equals(msg.getRole()) ? "用户" : "助手";
            conversation.append(role).append(": ").append(msg.getContent()).append("\n");
        }

        // 3. 调用AI生成总结
        String summaryPrompt = "请将以下对话内容总结为一段简洁的摘要，保留关键信息（用户意图、重要操作、结论）：\n\n" + conversation;
        List<ChatMessage> summaryContext = List.of(new UserMessage(summaryPrompt));

        String summary = selectModel(defaultModelName).chat(summaryContext).aiMessage().text();

        // 4. 保存总结到会话信息
        var sessionInfo = aiSshRedisUtils.getSessionInfo(aiSessionId);
        if (sessionInfo != null) {
            sessionInfo.setSummary(summary);
            sessionInfo.setLastSummaryAt(Instant.now().toEpochMilli());
            aiSshRedisUtils.saveSessionInfo(sessionInfo);
        }

        // 5. 清空消息列表
        aiSshRedisUtils.clearMessages(aiSessionId);

        log.info("会话总结完成: aiSessionId={}, summary={}", aiSessionId, summary);
    }

    // ==================== 工具方法 ====================

    /**
     * 获取绑定关系
     *
     * @param aiSessionId AI会话ID
     * @return 绑定关系，不存在返回null
     */
    public SshSessionBinding getBinding(String aiSessionId) {
        return aiSshRedisUtils.getBinding(aiSessionId);
    }

    /**
     * 检查会话是否存在
     *
     * @param aiSessionId AI会话ID
     * @return true表示存在
     */
    public boolean sessionExists(String aiSessionId) {
        return aiSshRedisUtils.bindingExists(aiSessionId);
    }
}
