package com.hundred.monitor.server.ai.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 读取Ollama中的配置
 */
@Component
@Data
@ConfigurationProperties(prefix = "langchain4j.ollama.chat-model")
public class AgentOllamaChatModelAssistantProperty {
    private String modelName;
    private String baseUrl;
}
