package com.hundred.monitor.server.ai.command;

/**
 * 命令执行状态枚举
 */
public enum CommandStatus {
    /**
     * 执行中
     */
    EXECUTING,

    /**
     * 已完成
     */
    COMPLETED,

    /**
     * 超时
     */
    TIMEOUT,

    /**
     * 错误
     */
    ERROR
}
