package com.hundred.monitor.server.ai.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 命令执行上下文
 * 存储AI发起的SSH命令执行过程中的所有相关信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandContext {

    /**
     * 命令唯一标识
     */
    private String commandId;

    /**
     * AI会话ID
     */
    private String aiSessionId;

    /**
     * SSH会话ID
     */
    private String sshSessionId;

    /**
     * 执行的命令
     */
    private String command;

    /**
     * 命令输出缓存
     */
    private StringBuilder output;

    /**
     * 开始时间（毫秒时间戳）
     */
    private long startTime;

    /**
     * 命令状态
     */
    private CommandStatus status;

    /**
     * 超时时间（毫秒）
     */
    private long timeoutMillis;

    /**
     * 检查是否超时
     *
     * @return true表示已超时
     */
    public boolean isTimeout() {
        return System.currentTimeMillis() - startTime > timeoutMillis;
    }

    /**
     * 获取完整输出
     *
     * @return 命令输出字符串
     */
    public String getOutput() {
        return output != null ? output.toString() : "";
    }

    /**
     * 追加输出
     *
     * @param text 输出文本
     */
    public void appendOutput(String text) {
        if (output == null) {
            output = new StringBuilder();
        }
        output.append(text);
    }

    /**
     * 获取已执行时长（毫秒）
     *
     * @return 已执行时长
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
}
