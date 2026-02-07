package com.hundred.monitor.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Monitor-AI 主启动类
 * AI助手独立服务
 */
@SpringBootApplication(scanBasePackages = {
        "com.hundred.monitor.ai",
        "com.hundred.monitor.commonlibrary"
})
@EnableFeignClients(basePackages = "com.hundred.monitor.ai")
public class MonitorAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitorAiApplication.class, args);
    }
}
