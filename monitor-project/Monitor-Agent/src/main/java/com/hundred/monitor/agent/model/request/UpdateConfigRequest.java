package com.hundred.monitor.agent.model.request;

import com.hundred.monitor.agent.model.entity.AgentConfig;
import lombok.Data;

/**
 * 配置更新请求
 */
@Data
public class UpdateConfigRequest {

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * 配置更新内容
     */
    private AgentConfig configUpdates;
}
