package com.hundred.monitor.server.ai.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 模型创建配置（控制模型创建的类）
 */
@Component
@Data
@ConfigurationProperties(prefix = "ai.monitor-agent")
public class MonitorAgentCreateProperty {
    String defaultModelName;
    Double defaultModelTemperature;
}
