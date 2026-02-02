package com.hundred.monitor.server.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundred.monitor.server.mapper.AgentMetricsMapper;
import com.hundred.monitor.server.model.entity.AgentMetrics;
import com.hundred.monitor.commonlibrary.request.MetricsReportRequest;
import com.hundred.monitor.commonlibrary.model.Metrics;
import com.hundred.monitor.server.service.AgentMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Agent监控数据服务实现类
 */
@Slf4j
@Service
public class AgentMetricsServiceImpl implements AgentMetricsService {

    @Autowired
    private AgentMetricsMapper agentMetricsMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void saveMetrics(MetricsReportRequest request) {
        // TODO: 构建监控数据实体
        Metrics metrics = request.getMetrics();

        AgentMetrics agentMetrics = AgentMetrics.builder()
                .agentId(request.getAgentId())
                .cpuPercent(convertToBigDecimal(metrics.getCpuPercent()))
                .memoryPercent(convertToBigDecimal(metrics.getMemoryPercent()))
                .networkUpMbps(convertToBigDecimal(metrics.getNetworkUpMbps()))
                .networkDownMbps(convertToBigDecimal(metrics.getNetworkDownMbps()))
                .timestamp(parseTimestamp(request.getTimestamp()))
                .build();

        // 处理SSH状态
        if (metrics.getSshStatus() != null) {
            agentMetrics.setSshRunning(metrics.getSshStatus().getRunning());
            agentMetrics.setSshPortListening(metrics.getSshStatus().getPortListening());
            agentMetrics.setSshPort(metrics.getSshStatus().getPort());
        }

        // 处理磁盘使用信息，转换为JSON
        if (metrics.getDiskUsages() != null) {
            try {
                agentMetrics.setDiskUsages(objectMapper.writeValueAsString(metrics.getDiskUsages()));
            } catch (JsonProcessingException e) {
                log.error("磁盘使用信息JSON转换失败", e);
            }
        }

        // TODO: 保存到数据库
        agentMetricsMapper.insert(agentMetrics);
        log.info("监控数据已保存: agentId={}, timestamp={}", request.getAgentId(), request.getTimestamp());
    }

    /**
     * 转换为BigDecimal
     */
    private java.math.BigDecimal convertToBigDecimal(Double value) {
        if (value == null) {
            return null;
        }
        return java.math.BigDecimal.valueOf(value);
    }

    /**
     * 解析时间戳
     */
    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            Instant instant = Instant.parse(timestamp);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (Exception e) {
            log.warn("时间戳解析失败: {}, 使用当前时间", timestamp, e);
            return LocalDateTime.now();
        }
    }
}
