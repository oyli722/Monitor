package com.hundred.monitor.commonlibrary.ai.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送消息请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /**
     * 用户消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String message;

    /**
     * 模型名称（可选，默认使用配置的默认模型）
     * 支持: glm, ollama
     */
    private String modelName;
}
