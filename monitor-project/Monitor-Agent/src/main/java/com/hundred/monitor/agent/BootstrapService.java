package com.hundred.monitor.agent;

import com.hundred.monitor.agent.model.response.RegisterResponse;
import com.hundred.monitor.agent.service.RegisterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动引导服务
 * 应用启动后自动执行注册流程
 */
@Component
public class BootstrapService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapService.class);

    @Autowired
    private RegisterService registerService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Monitor-Agent 启动中...");

        try {
            // 检查是否已注册
            if (registerService.isRegistered()) {
                log.info("客户端已注册，Agent ID: {}", registerService.getAgentConfig().getAgent().getId());
                log.info("定时任务已启动，开始数据上报...");
                return;
            }

            // 未注册，执行注册流程
            log.info("客户端未注册，开始注册流程...");
            RegisterResponse response = registerService.register();

            if (response != null && response.getSuccess()) {
                log.info("注册成功，Agent ID: {}, Agent Name: {}",
                        response.getAgentId(), response.getAgentName());
            } else {
                log.warn("注册失败，请检查网络连接或服务端状态");
            }

        } catch (Exception e) {
            log.error("启动流程执行失败", e);
        }
    }
}
