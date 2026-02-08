package com.hundred.monitor.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 客户端应用主类
 * 启用定时任务功能
 */
@SpringBootApplication
@EnableScheduling
@EnableFeignClients
@EnableConfigurationProperties
public class MonitorAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(MonitorAgentApplication.class, args);
	}



}
