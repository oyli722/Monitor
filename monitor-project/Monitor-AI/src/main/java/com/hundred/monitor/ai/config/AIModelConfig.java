package com.hundred.monitor.ai.config;

import com.hundred.monitor.ai.model.ChatAssistant;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
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

    // ==================== 流式模型Bean ====================

    /**
     * 默认流式模型（Ollama）
     */
    @Bean(name = "defaultOpenAiStreamingChatModel")
    public StreamingChatLanguageModel defaultOpenAiStreamingChatModel() {
        log.info("初始化默认流式模型: {}", ollamaModelName);
        return OpenAiStreamingChatModel.builder()
                .modelName(ollamaModelName)
                .baseUrl(ollamaBaseUrl)
                .build();
    }

    /**
     * GLM流式模型
     */
    @Bean(name = "glmStreamingChatModel")
    public StreamingChatLanguageModel glmStreamingChatModel() {
        log.info("初始化GLM流式模型: {}", glmModelName);
        return OpenAiStreamingChatModel.builder()
                .modelName(glmModelName)
                .baseUrl(glmBaseUrl)
                .apiKey(glmApiKey)
                .build();
    }

    /**
     * Ollama流式模型
     */
    @Bean(name = "ollamaStreamingChatModel")
    public StreamingChatLanguageModel ollamaStreamingChatModel() {
        log.info("初始化Ollama流式模型: {}", ollamaModelName);
        return OpenAiStreamingChatModel.builder()
                .modelName(ollamaModelName)
                .baseUrl(ollamaBaseUrl)
                .build();
    }

    // ==================== ChatAssistant Bean ====================

    /**
     * 默认OpenAI模型（用于通用对话）
     * 使用@Primary注解，作为默认注入的ChatLanguageModel
     */
    @Bean(name = "defaultOpenAiChatAssistant")
    @Primary
    public ChatAssistant defaultOpenAiChatAssistant() {
        log.info("初始化默认ChatAssistant: {}", ollamaModelName);
        OpenAiStreamingChatModel defaultOpenAiChatModel = OpenAiStreamingChatModel.builder()
                .modelName(ollamaModelName)
                .baseUrl(ollamaBaseUrl)
                .build();
        return AiServices.builder(ChatAssistant.class)
                .streamingChatLanguageModel(defaultOpenAiChatModel)
                // TODO 调用工具
                .build();
    }

    /**
     * GLM-4.7模型（智谱AI）
     */
    @Bean(name = "glmAiChatAssistant")
    public ChatAssistant glmAiChatAssistant() {
        log.info("初始化GLM-4.7模型");
        OpenAiStreamingChatModel build = OpenAiStreamingChatModel.builder()
                .modelName(glmModelName)
                .baseUrl(glmBaseUrl)
                .apiKey(glmApiKey)
                .build();
        return AiServices.builder(ChatAssistant.class)
                .streamingChatLanguageModel(build)
                // TODO 调用工具
                .build();
    }

    /**
     * Ollama模型（本地）
     */
    @Bean(name = "ollamaAiChatAssistant")
    public ChatAssistant ollamaAiChatAssistant() {
        log.info("初始化Ollama模型: {}", ollamaModelName);
        OpenAiStreamingChatModel build = OpenAiStreamingChatModel.builder()
                .modelName(ollamaModelName)
                .baseUrl(ollamaBaseUrl)
                .build();
        return AiServices.builder(ChatAssistant.class)
                .streamingChatLanguageModel(build)
                // TODO 调用工具
                .build();
    }
}
