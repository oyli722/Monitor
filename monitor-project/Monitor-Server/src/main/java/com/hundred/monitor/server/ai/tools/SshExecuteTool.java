package com.hundred.monitor.server.ai.tools;

import com.hundred.monitor.server.ai.context.SshSessionContext;
import com.hundred.monitor.server.websocket.SshSession;
import com.hundred.monitor.server.websocket.SshSessionManager;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * AI工具类，用于执行SSH命令
 * 从ThreadLocal上下文获取SSH会话信息
 */
@Slf4j
@Component
public class SshExecuteTool {

    /**
     * 在SSH终端执行命令
     *
     * @param command 要执行的命令
     * @return 执行结果
     */
    @Tool("在SSH终端执行命令，用于执行Linux命令并查看结果")
    public String executeCommand(String command) {
        // 1. 从ThreadLocal获取SSH会话ID
        String sshSessionId = SshSessionContext.getSshSessionId();

        if (sshSessionId == null || sshSessionId.isEmpty()) {
            log.warn("SSH会话ID未设置，无法执行命令");
            return "错误：无法获取SSH会话信息，请确保已连接到终端。";
        }

        // 2. 获取SSH会话
        SshSession sshSession = SshSessionManager.getInstance().getSession(sshSessionId);
        if (sshSession == null) {
            log.warn("SSH会话不存在: sshSessionId={}", sshSessionId);
            return "错误：SSH会话不存在或已断开，请重新连接终端。";
        }

        // 3. 检查输出流是否可用
        OutputStream outputStream = sshSession.getOutputStream();
        if (outputStream == null) {
            log.error("SSH输出流不可用: sshSessionId={}", sshSessionId);
            return "错误：SSH连接不可用，请重新连接终端。";
        }

        try {
            // 4. 向SSH发送命令（添加换行符执行）
            String commandWithNewline = command + "\n";
            outputStream.write(commandWithNewline.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            log.info("SSH命令已发送: sshSessionId={}, command={}", sshSessionId, command);

            // TODO: 同步等待并返回命令输出结果
            // 当前实现：仅发送命令，结果会通过WebSocket异步推送到前端
            return "命令已发送到终端，请查看终端输出结果。";

        } catch (Exception e) {
            log.error("执行SSH命令失败: sshSessionId={}, command={}", sshSessionId, command, e);
            return "错误：执行命令失败 - " + e.getMessage();
        }
    }

    /**
     * 获取当前SSH会话的主机ID
     *
     * @return 主机ID
     */
    @Tool("获取当前SSH会话关联的主机ID")
    public String getCurrentAgentId() {
        String agentId = SshSessionContext.getAgentId();
        if (agentId == null || agentId.isEmpty()) {
            return "错误：无法获取主机信息，请确保已连接到终端。";
        }
        return agentId;
    }

    /**
     * 测试工具是否可用
     *
     * @return 测试消息
     */
    @Tool("测试SSH执行工具是否可用")
    public String testToolIfAvailable() {
        String sshSessionId = SshSessionContext.getSshSessionId();
        if (sshSessionId == null) {
            return "SSH执行工具已加载，但当前没有关联的SSH会话。";
        }
        return "SSH执行工具可用，当前会话: " + sshSessionId;
    }
}
