package com.hundred.monitor.server.ai.config;

import com.hundred.monitor.server.ai.config.property.AgentChatModelAssistantProperty;
import com.hundred.monitor.server.ai.config.property.AgentOllamaChatModelAssistantProperty;
import com.hundred.monitor.server.ai.config.property.MonitorAgentCreateProperty;
import com.hundred.monitor.server.ai.entity.Assistant;
import com.hundred.monitor.server.ai.tools.SshExecuteTool;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


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

    /**
     * Ollama模型（用于场景A直接注入，场景B通过Assistant使用）
     */
    @Bean
    public OpenAiChatModel getOllamaAiChatModel() {
        return OpenAiChatModel.builder()
                .modelName(agentOllamaChatModelAssistantProperty.getModelName())
                .baseUrl(agentOllamaChatModelAssistantProperty.getBaseUrl())
                .httpClientBuilder(new JdkHttpClientBuilder())
                .build();
    }

    /**
     * GLM模型（用于场景A直接注入，场景B通过Assistant使用）
     */
    @Bean
    public OpenAiChatModel getGlmAiChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(agentChatModelAssistantProperty.getApiKey())
                .modelName(agentChatModelAssistantProperty.getModelName())
                .baseUrl(agentChatModelAssistantProperty.getBaseUrl())
                .temperature(monitorAgentCreateProperty.getDefaultModelTemperature())
                .httpClientBuilder(new JdkHttpClientBuilder())
                .build();
    }

    /**
     * 默认模型（用于场景A：全局聊天）
     * 场景A直接使用此模型，不经过Assistant
     */
    @Bean
    @Primary
    public OpenAiChatModel defaultOpenAiChatModel() {
        // 直接根据配置创建，避免调用其他Bean方法
        String modelName = monitorAgentCreateProperty.getDefaultModelName();
        if ("glm".equalsIgnoreCase(modelName)) {
            return getGlmAiChatModel();
        } else {
            return getOllamaAiChatModel();
        }
    }

    /**
     * 默认Ollama Assistant（用于场景B：SSH终端助手）
     */
    @Bean
    public Assistant getDefaultOllamaAssistant() {
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(getOllamaAiChatModel())
                .tools(sshExecuteTool)
                .build();
    }

    /**
     * 默认GLM Assistant（用于场景B：SSH终端助手）
     */
    @Bean
    public Assistant getDefaultGlmAssistant() {
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(getGlmAiChatModel())
                .tools(sshExecuteTool)
                .build();
    }

    /**
     * 根据配置选择默认的Assistant（用于场景B）
     * 使用方法注入避免循环依赖
     */
    @Bean
    @Primary
    public Assistant defaultAssistant(
            @org.springframework.beans.factory.annotation.Qualifier("defaultOpenAiChatModel") OpenAiChatModel model) {
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .tools(sshExecuteTool)
                .build();
    }
}
