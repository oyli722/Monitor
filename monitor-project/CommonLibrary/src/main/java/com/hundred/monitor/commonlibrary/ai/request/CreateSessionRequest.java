package com.hundred.monitor.commonlibrary.ai.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建会话请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionRequest {

    /**
     * 首条用户消息（用于生成标题）
     */
    @NotBlank(message = "首条消息不能为空")
    private String firstMessage;

    /**
     * 关联的主机ID（可选）
     */
    private String agentId;
}
