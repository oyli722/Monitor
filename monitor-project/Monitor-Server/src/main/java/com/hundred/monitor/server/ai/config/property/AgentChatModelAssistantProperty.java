package com.hundred.monitor.server.ai.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 从配置文件里读取配置,读取模型基本信息
 */
@Component
@Data
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
public class AgentChatModelAssistantProperty {
    private String modelName;
    private String baseUrl;
    private String apiKey;
}
