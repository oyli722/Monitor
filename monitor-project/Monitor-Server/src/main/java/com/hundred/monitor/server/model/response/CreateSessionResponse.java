package com.hundred.monitor.server.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建会话响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionResponse {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 会话标题
     */
    private String title;
}
