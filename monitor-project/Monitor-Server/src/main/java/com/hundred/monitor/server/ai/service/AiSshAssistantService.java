package com.hundred.monitor.server.ai.service;

import com.hundred.monitor.server.ai.context.SshSessionContext;
import com.hundred.monitor.server.ai.entity.Assistant;
import com.hundred.monitor.server.ai.entity.SshAssistantMessage;
import com.hundred.monitor.server.ai.entity.SshAssistantSessionInfo;
import com.hundred.monitor.server.ai.entity.SshSessionBinding;
import com.hundred.monitor.server.ai.entity.SystemPrompt;
import com.hundred.monitor.server.ai.utils.TerminalChatRedisUtils;
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
    private TerminalChatRedisUtils aiSshRedisUtils;

    @Resource(name = "getOllamaAiChatModel")
    private OpenAiChatModel ollamaChatModel;

    @Resource(name = "getGlmAiChatModel")
    private OpenAiChatModel glmChatModel;

    @Resource
    private AgentService agentService;

    @Resource
    private Assistant defaultAssistant;

    @Resource(name = "getDefaultOllamaAssistant")
    private Assistant ollamaAssistant;

    @Resource(name = "getDefaultGlmAssistant")
    private Assistant glmAssistant;

    @Value("${ai.monitor-agent.default-model-name:ollama}")
    private String defaultModelName;

    // 消息数量阈值（超过此数量触发总结）
    private static final int MESSAGE_THRESHOLD = 20;

    /**
     * 是否使用Assistant Bean（支持工具调用）
     * 如果为false，则直接使用ChatLanguageModel（不支持工具调用）
     */
    @Value("${ai.monitor-agent.use-assistant:true}")
    private boolean useAssistant;

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
        SshAssistantMessage userMsg = SshAssistantMessage.builder()
                .role("user")
                .content(userMessage)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        aiSshRedisUtils.addMessage(aiSessionId, userMsg);

        // 3. 构建AI上下文
        List<dev.langchain4j.data.message.ChatMessage> context = buildContext(binding);

        // 4. 设置ThreadLocal上下文（供SshExecuteTool使用）
        SshSessionContext.setSshSessionId(binding.getSshSessionId());
        SshSessionContext.setAgentId(binding.getAgentId());
        SshSessionContext.setAiSessionId(binding.getAiSessionId());

        String aiReply;
        try {
            // 5. 调用AI模型（内部可能调用SshExecuteTool）
            aiReply = callAI(context, binding.getAgentId());
        } finally {
            // 6. 清理ThreadLocal上下文（防止内存泄漏）
            SshSessionContext.clear();
        }

        // 7. 保存AI回复
        SshAssistantMessage aiMsg = SshAssistantMessage.builder()
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
    public List<SshAssistantMessage> getMessages(String aiSessionId) {
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
    private List<dev.langchain4j.data.message.ChatMessage> buildContext(SshSessionBinding binding) {
        List<dev.langchain4j.data.message.ChatMessage> context = new ArrayList<>();

        // 1. 添加系统提示词（带主机上下文）
        String systemPromptWithContext = buildSystemPrompt(binding);
        context.add(new SystemMessage(systemPromptWithContext));

        // 2. 添加历史消息（最近10条）
        List<SshAssistantMessage> recentMessages = aiSshRedisUtils.getRecentMessages(
                binding.getAiSessionId(), 10);

        for (SshAssistantMessage msg : recentMessages) {
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
        // 使用统一的SSH助手提示词
        sb.append(SystemPrompt.getSshAssistantPrompt());
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
    private String callAI(List<dev.langchain4j.data.message.ChatMessage> context, String agentId) {
        try {
            if (useAssistant) {
                // 使用Assistant Bean（支持工具调用）
                return callAIWithAssistant(context);
            } else {
                // 直接使用ChatLanguageModel（不支持工具调用）
                return callAIWithModel(context);
            }
        } catch (Exception e) {
            log.error("AI调用失败: agentId={}", agentId, e);
            return "抱歉，AI服务暂时不可用，请稍后再试。错误: " + e.getMessage();
        }
    }

    /**
     * 使用Assistant Bean调用AI（支持工具调用）
     * 使用双参数chat方法，正确传递系统提示词
     */
    private String callAIWithAssistant(List<dev.langchain4j.data.message.ChatMessage> context) {
        // 提取系统提示词
        String systemPrompt = "";
        StringBuilder historyBuilder = new StringBuilder();

        for (dev.langchain4j.data.message.ChatMessage msg : context) {
            if (msg instanceof SystemMessage) {
                systemPrompt = ((SystemMessage) msg).text();
            } else if (msg instanceof UserMessage) {
                historyBuilder.append("用户: ").append(extractUserMessageText((UserMessage) msg)).append("\n");
            } else if (msg instanceof AiMessage) {
                historyBuilder.append("助手: ").append(((AiMessage) msg).text()).append("\n");
            }
        }

        // 构建用户消息（包含对话历史）
        String userMessage = buildUserMessage(historyBuilder);

        // 选择Assistant
        Assistant assistant = selectAssistant(defaultModelName);

        // 调用Assistant双参数方法：chat(systemPrompt, userMessage)
        // 这样系统提示词会被正确设置，而非被当作用户消息处理
        return assistant.chat(systemPrompt, userMessage);
    }

    /**
     * 构建用户消息（包含对话历史）
     */
    private String buildUserMessage(StringBuilder historyBuilder) {
        StringBuilder userMessage = new StringBuilder();

        if (!historyBuilder.isEmpty()) {
            userMessage.append("## 对话历史\n");
            userMessage.append(historyBuilder);
            userMessage.append("\n请根据以上对话历史回复用户的最后一条消息。");
        }

        return userMessage.toString();
    }

    /**
     * 使用ChatLanguageModel直接调用（不支持工具调用）
     */
    private String callAIWithModel(List<dev.langchain4j.data.message.ChatMessage> context) {
        ChatLanguageModel model = selectModel(defaultModelName);
        var response = model.chat(context);
        return response.aiMessage().text();
    }

    /**
     * 从UserMessage提取文本内容
     */
    private String extractUserMessageText(dev.langchain4j.data.message.UserMessage userMsg) {
        // UserMessage可能包含SingleText，需要特殊处理
        return userMsg.toString(); // 或者使用 userMsg.singleText() 如果可用
    }

    /**
     * 选择Assistant Bean
     *
     * @param modelName 模型名称（ollama或glm）
     * @return Assistant实例
     */
    private Assistant selectAssistant(String modelName) {
        if ("glm".equalsIgnoreCase(modelName)) {
            return glmAssistant;
        } else {
            return ollamaAssistant; // 默认使用Ollama
        }
    }

    /**
     * 选择ChatLanguageModel（用于非工具调用模式）
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
        List<SshAssistantMessage> allMessages = aiSshRedisUtils.getMessages(aiSessionId);

        // 2. 构建总结用的对话文本
        StringBuilder conversation = new StringBuilder();
        for (SshAssistantMessage msg : allMessages) {
            String role = "user".equals(msg.getRole()) ? "用户" : "助手";
            conversation.append(role).append(": ").append(msg.getContent()).append("\n");
        }

        // 3. 调用AI生成总结
        String summaryPrompt = "请将以下对话内容总结为一段简洁的摘要，保留关键信息（用户意图、重要操作、结论）：\n\n" + conversation;
        List<dev.langchain4j.data.message.ChatMessage> summaryContext = List.of(new UserMessage(summaryPrompt));

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

    // ==================== 命令输出分析 ====================

    /**
     * 分析命令输出
     * 此方法由CommandContextManager异步调用，不经过工具调用
     *
     * @param command 执行的命令
     * @param output  命令输出
     * @return AI分析结果
     */
    public String analyzeCommandOutput(String command, String output) {
        try {
            // 构建分析提示词
            String prompt = buildCommandAnalysisPrompt(command, output);

            // 直接调用模型，不使用Assistant（避免循环工具调用）
            ChatLanguageModel model = selectModel(defaultModelName);
            List<dev.langchain4j.data.message.ChatMessage> messages = List.of(
                    new SystemMessage(SystemPrompt.getSystemPrompt()),
                    new UserMessage(prompt)
            );

            return model.chat(messages).aiMessage().text();

        } catch (Exception e) {
            log.error("命令输出分析失败: command={}", command, e);
            return "命令已执行，但分析失败：" + e.getMessage();
        }
    }

    /**
     * 构建命令分析提示词
     *
     * @param command 执行的命令
     * @param output  命令输出
     * @return 分析提示词
     */
    private String buildCommandAnalysisPrompt(String command, String output) {
        return String.format("""
            请分析以下命令的执行结果：

            ## 命令
            ```
            %s
            ```

            ## 输出
            ```
            %s
            ```

            请从以下几个方面进行分析：

            1. **执行状态**：命令是否成功执行？有无错误？
            2. **关键信息**：输出中的关键数据是什么？（如CPU使用率、内存占用、进程状态等）
            3. **异常检查**：是否有警告、错误或异常情况？
            4. **建议**：基于执行结果，有什么运维建议？

            请用简洁清晰的语言总结分析结果。
            """,
            command,
            output.length() > 5000 ? output.substring(0, 5000) + "..." : output
        );
    }
}
