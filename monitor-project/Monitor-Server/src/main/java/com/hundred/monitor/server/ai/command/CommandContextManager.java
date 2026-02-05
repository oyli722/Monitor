package com.hundred.monitor.server.ai.command;

import com.hundred.monitor.server.ai.entity.SshAssistantMessage;
import com.hundred.monitor.server.ai.service.AiSshAssistantService;
import com.hundred.monitor.server.ai.utils.TerminalChatRedisUtils;
import com.hundred.monitor.server.ai.websocket.dto.WsChatMessage;
import com.hundred.monitor.server.ai.websocket.manager.AiSshAssistantManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 命令上下文管理器
 * 管理AI发起的SSH命令执行生命周期
 * 功能：注册命令、追加输出、完成命令、超时检查、清理、推送输出
 */
@Slf4j
@Component
public class CommandContextManager {

    @Resource
    private AiSshAssistantManager aiSshAssistantManager;

    @Resource
    @Lazy
    private AiSshAssistantService aiSshAssistantService;

    @Resource
    private TerminalChatRedisUtils terminalChatRedisUtils;

    /**
     * 命令存储：commandId -> CommandContext
     */
    private final ConcurrentHashMap<String, CommandContext> commandMap = new ConcurrentHashMap<>();

    /**
     * 活动命令映射：sshSessionId -> commandId
     * 一个SSH会话同时只执行一个命令（串行执行）
     */
    private final ConcurrentHashMap<String, String> activeCommands = new ConcurrentHashMap<>();

    // ==================== 命令注册 ====================

    /**
     * 注册新命令
     *
     * @param aiSessionId  AI会话ID
     * @param sshSessionId SSH会话ID
     * @param command      执行的命令
     * @return 命令ID
     */
    public String registerCommand(String aiSessionId, String sshSessionId, String command) {
        String commandId = UUID.randomUUID().toString();

        CommandContext context = CommandContext.builder()
                .commandId(commandId)
                .aiSessionId(aiSessionId)
                .sshSessionId(sshSessionId)
                .command(command)
                .output(new StringBuilder())
                .startTime(System.currentTimeMillis())
                .status(CommandStatus.EXECUTING)
                .timeoutMillis(5000)  // 默认5秒超时
                .build();

        commandMap.put(commandId, context);
        activeCommands.put(sshSessionId, commandId);

        log.info("注册命令: commandId={}, sshSessionId={}, command={}", commandId, sshSessionId, command);
        return commandId;
    }

    /**
     * 注册新命令（自定义超时时间）
     *
     * @param aiSessionId  AI会话ID
     * @param sshSessionId SSH会话ID
     * @param command      执行的命令
     * @param timeoutMillis 超时时间（毫秒）
     * @return 命令ID
     */
    public String registerCommand(String aiSessionId, String sshSessionId, String command, long timeoutMillis) {
        String commandId = UUID.randomUUID().toString();

        CommandContext context = CommandContext.builder()
                .commandId(commandId)
                .aiSessionId(aiSessionId)
                .sshSessionId(sshSessionId)
                .command(command)
                .output(new StringBuilder())
                .startTime(System.currentTimeMillis())
                .status(CommandStatus.EXECUTING)
                .timeoutMillis(timeoutMillis)
                .build();

        commandMap.put(commandId, context);
        activeCommands.put(sshSessionId, commandId);

        log.info("注册命令(自定义超时): commandId={}, sshSessionId={}, timeout={}ms, command={}",
                commandId, sshSessionId, timeoutMillis, command);
        return commandId;
    }

    // ==================== 输出处理 ====================

    /**
     * 追加命令输出
     *
     * @param sshSessionId SSH会话ID
     * @param output       输出内容
     */
    public void appendOutput(String sshSessionId, String output) {
        String commandId = activeCommands.get(sshSessionId);
        if (commandId == null) {
            // 没有关联的AI命令，直接返回
            return;
        }

        CommandContext context = commandMap.get(commandId);
        if (context == null || context.getStatus() != CommandStatus.EXECUTING) {
            return;
        }

        // 追加输出
        context.appendOutput(output);

        // 实时推送给前端
        pushOutputToFrontend(context.getAiSessionId(), output);

        log.debug("追加命令输出: commandId={}, outputLength={}", commandId, output.length());
    }

    /**
     * 实时推送命令输出给前端
     *
     * @param aiSessionId AI会话ID
     * @param output      输出内容
     */
    private void pushOutputToFrontend(String aiSessionId, String output) {
        WsChatMessage message = WsChatMessage.builder()
                .type("command_output")
                .content(output)
                .timestamp(System.currentTimeMillis())
                .build();

        aiSshAssistantManager.sendToSession(aiSessionId, message);
    }

    // ==================== 命令完成 ====================

    /**
     * 标记命令完成
     *
     * @param sshSessionId SSH会话ID
     */
    public void completeCommand(String sshSessionId) {
        String commandId = activeCommands.get(sshSessionId);
        if (commandId == null) {
            return;
        }

        CommandContext context = commandMap.get(commandId);
        if (context != null && context.getStatus() == CommandStatus.EXECUTING) {
            context.setStatus(CommandStatus.COMPLETED);

            log.info("命令完成: commandId={}, elapsed={}ms, outputLength={}",
                    commandId, context.getElapsedTime(), context.getOutput().length());

            // 推送完成消息给前端
            pushCompleteToFrontend(context.getAiSessionId());

            // 从活动命令中移除（允许下一个命令执行）
            activeCommands.remove(sshSessionId);

            // 异步触发AI分析
            triggerAiAnalysis(context);
        }
    }

    /**
     * 推送命令完成消息给前端
     *
     * @param aiSessionId AI会话ID
     */
    private void pushCompleteToFrontend(String aiSessionId) {
        WsChatMessage message = WsChatMessage.builder()
                .type("command_complete")
                .content("命令执行完成")
                .timestamp(System.currentTimeMillis())
                .build();

        aiSshAssistantManager.sendToSession(aiSessionId, message);
    }

    /**
     * 异步触发AI分析命令输出
     *
     * @param context 命令上下文
     */
    @Async("commandAnalysisExecutor")
    public void triggerAiAnalysis(CommandContext context) {
        String commandId = context.getCommandId();

        log.info("开始AI分析: commandId={}, command={}", commandId, context.getCommand());

        try {
            // 调用AI分析
            String analysis = aiSshAssistantService.analyzeCommandOutput(
                    context.getCommand(),
                    context.getOutput()
            );

            // 保存AI分析结果到消息历史
            SshAssistantMessage aiMsg = SshAssistantMessage.builder()
                    .role("assistant")
                    .content(analysis)
                    .timestamp(Instant.now().toEpochMilli())
                    .build();
            terminalChatRedisUtils.addMessage(context.getAiSessionId(), aiMsg);

            // 推送分析结果给前端（WebSocket）
            aiSshAssistantManager.sendReply(context.getAiSessionId(), analysis);

            log.info("AI分析完成: commandId={}", commandId);

        } catch (Exception e) {
            log.error("AI分析失败: commandId={}", commandId, e);

            // 保存错误消息到历史
            String errorText = "命令执行完成，但分析失败：" + e.getMessage();
            SshAssistantMessage errorMessage = SshAssistantMessage.builder()
                    .role("assistant")
                    .content(errorText)
                    .timestamp(Instant.now().toEpochMilli())
                    .build();
            terminalChatRedisUtils.addMessage(context.getAiSessionId(), errorMessage);

            // 推送错误消息（WebSocket）
            aiSshAssistantManager.sendReply(context.getAiSessionId(), errorText);
        } finally {
            // 延迟清理命令上下文（5秒后）
            scheduleCleanup(commandId);
        }
    }

    /**
     * 延迟清理命令上下文
     *
     * @param commandId 命令ID
     */
    private void scheduleCleanup(String commandId) {
        CompletableFuture.delayedExecutor(5, java.util.concurrent.TimeUnit.SECONDS)
                .execute(() -> cleanup(commandId));
    }

    // ==================== 超时处理 ====================

    /**
     * 处理命令超时
     *
     * @param commandId 命令ID
     */
    public void handleTimeout(String commandId) {
        CommandContext context = commandMap.get(commandId);
        if (context == null || context.getStatus() != CommandStatus.EXECUTING) {
            return;
        }

        context.setStatus(CommandStatus.TIMEOUT);

        log.warn("命令超时: commandId={}, command={}, elapsed={}ms",
                commandId, context.getCommand(), context.getElapsedTime());

        // 从活动命令中移除
        activeCommands.remove(context.getSshSessionId());

        // 推送超时消息给前端
        WsChatMessage message = WsChatMessage.builder()
                .type("command_timeout")
                .content("命令执行超时")
                .timestamp(System.currentTimeMillis())
                .build();

        aiSshAssistantManager.sendToSession(context.getAiSessionId(), message);

        // 仍然触发AI分析（基于已收集的输出）
        triggerAiAnalysis(context);
    }

    // ==================== 获取方法 ====================

    /**
     * 获取命令上下文
     *
     * @param commandId 命令ID
     * @return 命令上下文，不存在返回null
     */
    public CommandContext getCommand(String commandId) {
        return commandMap.get(commandId);
    }

    /**
     * 获取SSH会话的当前活动命令ID
     *
     * @param sshSessionId SSH会话ID
     * @return 命令ID，无活动命令返回null
     */
    public String getActiveCommandId(String sshSessionId) {
        return activeCommands.get(sshSessionId);
    }

    /**
     * 获取所有命令上下文
     *
     * @return 命令上下文集合
     */
    public java.util.Collection<CommandContext> getAllCommands() {
        return commandMap.values();
    }

    /**
     * 获取活动命令数量
     *
     * @return 活动命令数量
     */
    public int getActiveCommandCount() {
        return activeCommands.size();
    }

    // ==================== 清理方法 ====================

    /**
     * 清理命令上下文
     *
     * @param commandId 命令ID
     */
    public void cleanup(String commandId) {
        CommandContext context = commandMap.remove(commandId);
        if (context != null) {
            activeCommands.remove(context.getSshSessionId());
            log.info("清理命令上下文: commandId={}", commandId);
        }
    }

    /**
     * 清理指定SSH会话的所有命令
     *
     * @param sshSessionId SSH会话ID
     */
    public void cleanupBySshSession(String sshSessionId) {
        String commandId = activeCommands.remove(sshSessionId);
        if (commandId != null) {
            commandMap.remove(commandId);
            log.info("清理SSH会话命令: sshSessionId={}, commandId={}", sshSessionId, commandId);
        }
    }

    /**
     * 清理指定AI会话的所有命令
     *
     * @param aiSessionId AI会话ID
     */
    public void cleanupByAiSession(String aiSessionId) {
        commandMap.entrySet().removeIf(entry -> {
            if (aiSessionId.equals(entry.getValue().getAiSessionId())) {
                activeCommands.remove(entry.getValue().getSshSessionId());
                log.info("清理AI会话命令: aiSessionId={}, commandId={}",
                        aiSessionId, entry.getKey());
                return true;
            }
            return false;
        });
    }
}
