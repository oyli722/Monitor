package com.hundred.monitor.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hundred.monitor.server.mapper.AgentMapper;
import com.hundred.monitor.server.mapper.AgentMetricsMapper;
import com.hundred.monitor.server.model.dto.AgentDetailDTO;
import com.hundred.monitor.server.model.dto.AgentInfoDTO;
import com.hundred.monitor.server.model.entity.Agent;
import com.hundred.monitor.server.model.entity.AgentMetrics;
import com.hundred.monitor.commonlibrary.common.BaseResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent API控制器（供AI服务调用）
 * 提供Agent信息的查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
public class AgentApiController {

    @Resource
    private AgentMapper agentMapper;

    @Resource
    private AgentMetricsMapper agentMetricsMapper;

    /**
     * 获取所有Agent列表
     *
     * @param token JWT令牌（用于认证）
     * @return Agent列表
     */
    @GetMapping("/list")
    public BaseResponse<List<AgentInfoDTO>> getAgentList(
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // TODO: 验证JWT Token
            log.debug("AI服务请求Agent列表，token: {}", token);

            List<Agent> agents = agentMapper.selectList(null);

            List<AgentInfoDTO> dtoList = agents.stream()
                    .map(this::toAgentInfoDTO)
                    .collect(Collectors.toList());

            return BaseResponse.success(dtoList);
        } catch (Exception e) {
            log.error("获取Agent列表失败", e);
            return BaseResponse.error("获取Agent列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取单个Agent详情
     *
     * @param agentId Agent ID
     * @param token   JWT令牌（用于认证）
     * @return Agent详情
     */
    @GetMapping("/{agentId}")
    public BaseResponse<AgentDetailDTO> getAgent(
            @PathVariable String agentId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // TODO: 验证JWT Token
            log.debug("AI服务请求Agent详情，agentId: {}, token: {}", agentId, token);

            Agent agent = agentMapper.selectById(agentId);
            if (agent == null) {
                return BaseResponse.notFound("Agent不存在: " + agentId);
            }

            return BaseResponse.success(toAgentDetailDTO(agent));
        } catch (Exception e) {
            log.error("获取Agent详情失败: agentId={}", agentId, e);
            return BaseResponse.error("获取Agent详情失败: " + e.getMessage());
        }
    }

    // ==================== 转换方法 ====================

    /**
     * 转换为AgentInfoDTO
     */
    private AgentInfoDTO toAgentInfoDTO(Agent agent) {
        return AgentInfoDTO.builder()
                .agentId(agent.getAgentId())
                .hostname(agent.getHostname())
                .ip(agent.getIp())
                .cpuModel(agent.getCpuModel())
                .cpuCores(agent.getCpuCores())
                .memoryGb(agent.getMemoryGb() != null ? agent.getMemoryGb().doubleValue() : null)
                .online(true)  // 简化处理，假设注册的Agent都在线
                .registeredAt(toTimestamp(agent.getRegisteredAt()))
                .build();
    }

    /**
     * 转换为AgentDetailDTO
     */
    private AgentDetailDTO toAgentDetailDTO(Agent agent) {
        // 获取最新的AgentMetrics来判断SSH状态
        boolean sshRunning = false;
        Integer sshPort = null;

        LambdaQueryWrapper<AgentMetrics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentMetrics::getAgentId, agent.getAgentId())
                .orderByDesc(AgentMetrics::getTimestamp)
                .last("LIMIT 1");
        AgentMetrics metrics = agentMetricsMapper.selectOne(wrapper);

        if (metrics != null) {
            sshRunning = metrics.getSshRunning() != null ? metrics.getSshRunning() : false;
            sshPort = metrics.getSshPort();
        }

        return AgentDetailDTO.builder()
                .agentId(agent.getAgentId())
                .hostname(agent.getHostname())
                .ip(agent.getIp())
                .cpuModel(agent.getCpuModel())
                .cpuCores(agent.getCpuCores())
                .memoryGb(agent.getMemoryGb() != null ? agent.getMemoryGb().doubleValue() : null)
                .gpuInfo(agent.getGpuInfo())
                .networkInterfaces(agent.getNetworkInterfaces())
                .online(true)  // 简化处理
                .sshRunning(sshRunning)
                .sshPort(sshPort)
                .registeredAt(toTimestamp(agent.getRegisteredAt()))
                .updatedAt(toTimestamp(agent.getUpdatedAt()))
                .build();
    }

    /**
     * 转换LocalDateTime为时间戳（毫秒）
     */
    private Long toTimestamp(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
