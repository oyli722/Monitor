package com.hundred.monitor.agent;

import com.hundred.monitor.agent.service.CollectService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 客户端应用主类
 * 启用定时任务功能
 */
@SpringBootApplication
@EnableScheduling
@EnableFeignClients
public class MonitorAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(MonitorAgentApplication.class, args);
	}



}
