package com.hundred.monitor.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 基本数据上报请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BasicReportRequest {

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * 时间戳
     */
    private String timestamp;

    /**
     * 基本数据
     */
    private CustomerRegisterRequest.AgentBasicInfo basicInfo;
}
