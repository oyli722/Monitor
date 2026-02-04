package com.hundred.monitor.server.ai.entity;


import dev.langchain4j.service.SystemMessage;

/**
 * AI助手接口
 * LangChain4j通过此接口自动生成代理，支持工具调用
 */
public interface Assistant {

    /**
     * 发送消息并获取AI回复
     * 此方法会被LangChain4j自动实现，支持工具调用
     *
     * @param userMessage 用户消息
     * @return AI回复
     */
    @SystemMessage("You are a helpful assistant.")
    String chat(String userMessage);

    /**
     * 发送带系统提示词的消息
     *
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @return AI回复
     */
    String chat(String systemPrompt, String userMessage);
}
