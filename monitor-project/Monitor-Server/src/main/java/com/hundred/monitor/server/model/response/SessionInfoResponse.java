package com.hundred.monitor.server.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话信息响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfoResponse {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 创建时间
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Long createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Long updatedAt;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * 关联的主机ID
     */
    private String linkedAgentId;
}
