package com.hundred.monitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundred.monitor.server.mapper.AgentMapper;
import com.hundred.monitor.server.model.entity.Agent;
import com.hundred.monitor.server.model.request.CustomerRegisterRequest;
import com.hundred.monitor.commonlibrary.model.BasicInfo;
import com.hundred.monitor.commonlibrary.response.RegisterResponse;
import com.hundred.monitor.server.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Agent服务实现类
 */
@Slf4j
@Service
public class AgentServiceImpl implements AgentService {

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 生成唯一Agent ID
     */
    private String generateAgentId() {
        return "AGT_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * 生成Agent名称
     */
    private String generateAgentName(String hostname) {
        return hostname + "-" + System.currentTimeMillis() % 10000;
    }

    @Override
    public RegisterResponse register(CustomerRegisterRequest request) {
        // TODO: 生成Agent ID和名称
        String agentId = generateAgentId();
        String agentName = generateAgentName(request.getHostname());

        // TODO: 构建Agent实体并保存
        Agent agent = Agent.builder()
                .agentId(agentId)
                .agentName(agentName)
                .hostname(request.getHostname())
                .ip(request.getIp())
                .registeredAt(LocalDateTime.now())
                .build();

        BasicInfo basicInfo = request.getBasicInfo();
        if (basicInfo != null) {
            agent.setCpuModel(basicInfo.getCpuModel());
            agent.setCpuCores(basicInfo.getCpuCores());
            agent.setMemoryGb(basicInfo.getMemoryGb());

            // 转换GPU信息为JSON
            try {
                agent.setGpuInfo(objectMapper.writeValueAsString(basicInfo.getGpus()));
            } catch (JsonProcessingException e) {
                log.error("GPU信息JSON转换失败", e);
            }

            // 转换网络接口为JSON
            try {
                agent.setNetworkInterfaces(objectMapper.writeValueAsString(basicInfo.getNetworkInterfaces()));
            } catch (JsonProcessingException e) {
                log.error("网络接口JSON转换失败", e);
            }
        }

        // TODO: 保存到数据库
        agentMapper.insert(agent);

        // TODO: 生成认证Token
        LocalDateTime tokenExpires = LocalDateTime.now().plusDays(30);
        String authToken = "JWT_TOKEN_" + agentId + "_" + System.currentTimeMillis();

        return RegisterResponse.builder()
                .success(true)
                .agentId(agentId)
                .agentName(agentName)
                .authToken(authToken)
                .tokenExpires(tokenExpires.toString())
                .build();
    }

    @Override
    public void updateBasicInfo(String agentId, BasicInfo basicInfo) {
        // TODO: 更新Agent基本数据
        Agent existingAgent = getAgentById(agentId);
        if (existingAgent == null) {
            log.warn("Agent不存在: {}", agentId);
            return;
        }

        if (basicInfo != null) {
            if (basicInfo.getCpuModel() != null) {
                existingAgent.setCpuModel(basicInfo.getCpuModel());
            }
            if (basicInfo.getCpuCores() != null) {
                existingAgent.setCpuCores(basicInfo.getCpuCores());
            }
            if (basicInfo.getMemoryGb() != null) {
                existingAgent.setMemoryGb(basicInfo.getMemoryGb());
            }
            if (basicInfo.getGpus() != null) {
                try {
                    existingAgent.setGpuInfo(objectMapper.writeValueAsString(basicInfo.getGpus()));
                } catch (JsonProcessingException e) {
                    log.error("GPU信息JSON转换失败", e);
                }
            }
            if (basicInfo.getNetworkInterfaces() != null) {
                try {
                    existingAgent.setNetworkInterfaces(objectMapper.writeValueAsString(basicInfo.getNetworkInterfaces()));
                } catch (JsonProcessingException e) {
                    log.error("网络接口JSON转换失败", e);
                }
            }
        }

        agentMapper.updateById(existingAgent);
        log.info("Agent基本数据已更新: {}", agentId);
    }

    @Override
    public Agent getAgentById(String agentId) {
        return agentMapper.selectOne(new LambdaQueryWrapper<Agent>()
                .eq(Agent::getAgentId, agentId));
    }
}
