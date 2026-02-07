package com.hundred.monitor.ai.config;

import com.hundred.monitor.ai.model.ChatAssistant;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * AI模型配置类
 * 配置多个AI模型供ChatService使用
 */
@Slf4j
@Configuration
public class AIModelConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String glmApiKey;

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String glmBaseUrl;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String glmModelName;

    @Value("${langchain4j.ollama.chat-model.base-url}")
    private String ollamaBaseUrl;

    @Value("${langchain4j.ollama.chat-model.model-name}")
    private String ollamaModelName;

    /**
     * 默认OpenAI模型（用于通用对话）
     * 使用@Primary注解，作为默认注入的ChatLanguageModel
     */
    @Bean(name = "getDefaultAiChatModel")
    public ChatAssistant getDefaultAiChatAssistant() {
        log.info("初始化默认ChatAssistant: {}", ollamaModelName);
        OpenAiChatModel defaultOpenAiChatModel = OpenAiChatModel.builder()
                .modelName(ollamaModelName)
                .baseUrl(ollamaBaseUrl)
                .build();
        return AiServices.builder(ChatAssistant.class)
                .chatLanguageModel(defaultOpenAiChatModel)
                // TODO 调用工具
                .build();
    }

    /**
     * GLM-4.7模型（智谱AI）
     */
    @Bean(name = "getGlmAiChatModel")
    public ChatAssistant getGlmAiChatAssistant() {
        log.info("初始化GLM-4.7模型");
        OpenAiChatModel build = OpenAiChatModel.builder()
                .modelName(glmModelName)
                .baseUrl(glmBaseUrl)
                .apiKey(glmApiKey)
                .build();
        return AiServices.builder(ChatAssistant.class)
                .chatLanguageModel(build)
                // TODO 调用工具
                .build();
    }

    /**
     * Ollama模型（本地）
     */
    @Bean(name = "getOllamaAiChatModel")
    public ChatAssistant getOllamaAiChatAssistant() {
        log.info("初始化Ollama模型: {}", ollamaModelName);
        OpenAiChatModel build = OpenAiChatModel.builder()
                .modelName(ollamaModelName)
                .baseUrl(ollamaBaseUrl)
                .build();
        return AiServices.builder(ChatAssistant.class)
                .chatLanguageModel(build)
                // TODO 调用工具
                .build();
    }
}
