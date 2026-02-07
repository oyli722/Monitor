package com.hundred.monitor.ai.model;

import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * 该模块下的聊天助手，实现非阻塞式传输，支持工具调用
 * 消息格式：JSON字符串，包含完整消息列表（包括system消息）
 * 例如：[{"role":"system","message":"..."},{"role":"user","message":"你好"}]
 */
public interface ChatAssistant {

    /**
     * 聊天对话
     * JSON字符串必须包含system消息作为第一条消息
     *
     * @param messagesJson 消息列表的JSON字符串格式
     * @return AI回复流
     */
    Flux<String> chat(String messagesJson);

    /**
     * 聊天对话（使用自定义系统提示词）
     * JSON字符串必须包含system消息作为第一条消息
     *
     * @param systemPrompt 自定义系统提示词（可选）
     * @param messagesJson 消息列表的JSON字符串格式
     * @return AI回复流
     */
    Flux<String> chat(String systemPrompt, String messagesJson);
}
