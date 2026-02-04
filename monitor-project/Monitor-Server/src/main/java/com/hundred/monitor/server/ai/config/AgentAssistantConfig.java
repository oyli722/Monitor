package com.hundred.monitor.server.ai.config;

import com.hundred.monitor.server.ai.config.property.AgentChatModelAssistantProperty;
import com.hundred.monitor.server.ai.config.property.AgentOllamaChatModelAssistantProperty;
import com.hundred.monitor.server.ai.config.property.MonitorAgentCreateProperty;
import com.hundred.monitor.server.ai.entity.Assistant;
import com.hundred.monitor.server.ai.tools.SshExecuteTool;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * langchain4j配置下的配置项
 */

@Configuration
public class AgentAssistantConfig {
    @Resource
    private AgentChatModelAssistantProperty agentChatModelAssistantProperty;
    @Resource
    private AgentOllamaChatModelAssistantProperty agentOllamaChatModelAssistantProperty;
    @Resource
    private MonitorAgentCreateProperty monitorAgentCreateProperty;
    @Resource
    SshExecuteTool sshExecuteTool;

    @Bean
    public OpenAiChatModel defaultOpenAiChatModel() {
        return monitorAgentCreateProperty.getDefaultModelName().equals("ollama") ? getOllamaAiChatModel() : getGlmAiChatModel();
    }

    @Bean
    public OpenAiChatModel getGlmAiChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(agentChatModelAssistantProperty.getApiKey())
                .modelName(agentChatModelAssistantProperty.getModelName())
                .baseUrl(agentChatModelAssistantProperty.getBaseUrl())
                .temperature(monitorAgentCreateProperty.getDefaultModelTemperature())
                .build();
    }

    @Bean
    public OpenAiChatModel getOllamaAiChatModel() {
        return OpenAiChatModel.builder()
                .modelName(agentOllamaChatModelAssistantProperty.getModelName())
                .baseUrl(agentOllamaChatModelAssistantProperty.getBaseUrl())
                .build();
    }

    @Bean
    public Assistant getDefaultAssistant() {
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(defaultOpenAiChatModel())
                .tools(sshExecuteTool)
                .build();
    }

    @Bean
    public Assistant getOllamaAssistant() {
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(getOllamaAiChatModel())
                .tools(sshExecuteTool)
                .build();
    }

    @Bean
    public Assistant getGlmAssistant() {
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(getGlmAiChatModel())
                .tools(sshExecuteTool)
                .build();
    }
}
