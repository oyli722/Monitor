package com.hundred.monitor.ai.model;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * 该模块下的聊天助手，实现非阻塞式传输，支持工具调用
 *
 */
public interface ChatAssistant {
    /**
     * 发送消息并获取AI回复
     * 此方法会被LangChain4j自动实现，支持工具调用
     *
     * @param userMessage 用户消息
     * @return AI回复
     */
    @SystemMessage("{{systemPrompt}}")
    Flux<String> chat(@UserMessage String userMessage);

    /**
     * 发送带系统提示词的消息
     *
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @return AI回复
     */
    @SystemMessage("{{systemPrompt}}")
    Flux<String> chat(@dev.langchain4j.service.V("systemPrompt") String systemPrompt, @UserMessage String userMessage);

}
