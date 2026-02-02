package com.hundred.monitor.server.service;

import com.hundred.monitor.server.model.entity.Agent;
import com.hundred.monitor.server.model.request.CustomerRegisterRequest;
import com.hundred.monitor.server.model.response.AgentRegisterResponse;

/**
 * Agent服务接口
 */
public interface AgentService {

    /**
     * Agent注册
     *
     * @param request 注册请求
     * @return 注册响应
     */
    AgentRegisterResponse register(CustomerRegisterRequest request);

    /**
     * 更新Agent基本数据
     *
     * @param agentId Agent ID
     * @param basicInfo 基本数据
     */
    void updateBasicInfo(String agentId, CustomerRegisterRequest.AgentBasicInfo basicInfo);

    /**
     * 根据ID获取Agent
     *
     * @param agentId Agent ID
     * @return Agent实体
     */
    Agent getAgentById(String agentId);
}
