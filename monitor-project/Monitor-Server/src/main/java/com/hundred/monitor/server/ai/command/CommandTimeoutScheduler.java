package com.hundred.monitor.server.ai.command;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 命令超时定时任务
 * 定期检查命令执行超时并触发处理
 */
@Slf4j
@Component
public class CommandTimeoutScheduler {

    @Resource
    private CommandContextManager commandContextManager;

    private volatile boolean running = false;

    /**
     * 启动定时任务
     */
    @PostConstruct
    public void start() {
        running = true;
        log.info("命令超时定时任务已启动");
    }

    /**
     * 停止定时任务
     */
    @PreDestroy
    public void stop() {
        running = false;
        log.info("命令超时定时任务已停止");
    }

    /**
     * 每1秒检查一次命令超时
     */
    @Scheduled(fixedDelay = 1000)
    public void checkTimeout() {
        if (!running) {
            return;
        }

        try {
            // 遍历所有活动命令
            commandContextManager.getAllCommands().forEach(context -> {
                // 只检查执行中的命令
                if (context.getStatus() == CommandStatus.EXECUTING) {
                    // 检查是否超时
                    if (context.isTimeout()) {
                        log.warn("检测到命令超时: commandId={}, command={}, elapsed={}ms",
                                context.getCommandId(),
                                context.getCommand(),
                                context.getElapsedTime());

                        // 触发超时处理
                        commandContextManager.handleTimeout(context.getCommandId());
                    }
                }
            });

        } catch (Exception e) {
            log.error("命令超时检查失败", e);
        }
    }

    /**
     * 每5分钟清理一次已完成的命令上下文
     */
    @Scheduled(fixedDelay = 300000)
    public void cleanupCompletedCommands() {
        if (!running) {
            return;
        }

        try {
            int count = 0;
            for (var context : commandContextManager.getAllCommands()) {
                // 清理已完成或超时的命令（超过5分钟）
                if ((context.getStatus() == CommandStatus.COMPLETED ||
                     context.getStatus() == CommandStatus.TIMEOUT) &&
                    context.getElapsedTime() > 300000) {

                    commandContextManager.cleanup(context.getCommandId());
                    count++;
                }
            }

            if (count > 0) {
                log.info("定时清理已完成命令: count={}", count);
            }

        } catch (Exception e) {
            log.error("清理已完成命令失败", e);
        }
    }
}
