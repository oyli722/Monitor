package com.hundred.monitor.ai.model;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * 该模块下的聊天助手，实现非阻塞式传输，支持工具调用
 * 消息格式：JSON字符串，例如：[{"role":"system","message":"你是一个..."},{"role":"user","message":"你好"}]
 */
public interface ChatAssistant {

    /**
     * 聊天对话（使用默认系统提示词）
     *
     * @param messagesJson 消息列表的JSON字符串格式
     *                    例如：[{"role":"system","message":"..."},{"role":"user","message":"..."}]
     * @return AI回复流
     */
    @SystemMessage("{{systemPrompt}}")
    Flux<String> chat(@UserMessage String messagesJson);

    /**
     * 聊天对话（指定系统提示词）
     *
     * @param systemPrompt 系统提示词
     * @param messagesJson  消息列表的JSON字符串格式
     * @return AI回复流
     */
    @SystemMessage("{{systemPrompt}}")
    Flux<String> chat(@dev.langchain4j.service.V("systemPrompt") String systemPrompt, @UserMessage String messagesJson);
}
