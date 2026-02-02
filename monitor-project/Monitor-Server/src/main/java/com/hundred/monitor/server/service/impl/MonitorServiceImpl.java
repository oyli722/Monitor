package com.hundred.monitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundred.monitor.server.mapper.AgentMapper;
import com.hundred.monitor.server.mapper.AgentMetricsMapper;
import com.hundred.monitor.server.model.entity.AgentMetrics;
import com.hundred.monitor.server.model.response.AgentBasicInfoResponse;
import com.hundred.monitor.server.model.response.MetricsHistoryResponse;
import com.hundred.monitor.server.model.response.MetricsResponse;
import com.hundred.monitor.server.model.request.MetricsHistoryRequest;
import com.hundred.monitor.server.service.MonitorService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitorServiceImpl implements MonitorService {
    @Resource
    private AgentMapper agentMapper;
    @Resource
    private AgentMetricsMapper agentMetricsMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AgentBasicInfoResponse getMonitorList() {
        try {
            return AgentBasicInfoResponse.success(agentMapper.selectList(null));
        } catch (Exception e) {
            log.error("获取Agent列表失败", e);
            return AgentBasicInfoResponse.error("获取Agent列表失败");
        }
    }

    @Override
    public MetricsResponse getLatestMetrics(String agentId) {
        try {
            // 查询最新的一条监控数据
            LambdaQueryWrapper<AgentMetrics> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AgentMetrics::getAgentId, agentId)
                    .orderByDesc(AgentMetrics::getTimestamp)
                    .last("LIMIT 1");
            AgentMetrics metrics = agentMetricsMapper.selectOne(wrapper);

            if (metrics == null) {
                return MetricsResponse.builder()
                        .agentId(agentId)
                        .build();
            }

            // 构建响应
            MetricsResponse.MetricsResponseBuilder builder = MetricsResponse.builder()
                    .agentId(metrics.getAgentId())
                    .cpuPercent(metrics.getCpuPercent() != null ? metrics.getCpuPercent().doubleValue() : null)
                    .memoryPercent(metrics.getMemoryPercent() != null ? metrics.getMemoryPercent().doubleValue() : null)
                    .networkUpMbps(metrics.getNetworkUpMbps() != null ? metrics.getNetworkUpMbps().doubleValue() : null)
                    .networkDownMbps(metrics.getNetworkDownMbps() != null ? metrics.getNetworkDownMbps().doubleValue() : null)
                    .sshRunning(metrics.getSshRunning())
                    .sshPortListening(metrics.getSshPortListening())
                    .sshPort(metrics.getSshPort())
                    .timestamp(metrics.getTimestamp() != null ? metrics.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);

            // 解析磁盘使用信息
            if (metrics.getDiskUsages() != null && !metrics.getDiskUsages().isEmpty()) {
                builder.diskUsages(parseDiskUsages(metrics.getDiskUsages()));
            }

            return builder.build();
        } catch (Exception e) {
            log.error("获取主机监控指标失败: agentId={}", agentId, e);
            throw new RuntimeException("获取监控指标失败");
        }
    }

    @Override
    public MetricsHistoryResponse getMetricsHistory(String agentId, MetricsHistoryRequest request) {
        try {
            // 解析请求参数
            MetricsHistoryRequest.MetricType metricType = MetricsHistoryRequest.MetricType.fromValue(request.getMetricType());
            MetricsHistoryRequest.TimeRange timeRange = MetricsHistoryRequest.TimeRange.fromValue(request.getTimeRange());

            // 计算查询时间范围
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusMinutes(timeRange.getMinutes());

            // 查询原始数据
            LambdaQueryWrapper<AgentMetrics> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AgentMetrics::getAgentId, agentId)
                    .ge(AgentMetrics::getTimestamp, startTime)
                    .le(AgentMetrics::getTimestamp, endTime)
                    .orderByAsc(AgentMetrics::getTimestamp);
            List<AgentMetrics> metricsList = agentMetricsMapper.selectList(wrapper);
            log.info("查询原始数据：{}", metricsList);
            // 聚合数据
            return aggregateMetrics(metricsList, metricType, timeRange);
        } catch (IllegalArgumentException e) {
            log.error("参数错误: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取历史监控数据失败: agentId={}, request={}", agentId, request, e);
            throw new RuntimeException("获取历史数据失败");
        }
    }

    /**
     * 解析磁盘使用信息
     */
    private List<MetricsResponse.DiskUsage> parseDiskUsages(String diskUsagesJson) {
        try {
            List<MetricsResponse.DiskUsage> result = new ArrayList<>();

            if (diskUsagesJson == null || diskUsagesJson.trim().isEmpty()) {
                return result;
            }

            // 解析 JSON
            JsonNode rootNode = objectMapper.readTree(diskUsagesJson);

            // 如果是数组格式：[{"mount":"..."}, ...]
            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    MetricsResponse.DiskUsage usage = parseSingleDiskUsage(node);
                    if (usage != null) {
                        result.add(usage);
                    }
                }
            }
            // 如果是单个对象格式：{"mount":"..."}
            else if (rootNode.isObject()) {
                MetricsResponse.DiskUsage usage = parseSingleDiskUsage(rootNode);
                if (usage != null) {
                    result.add(usage);
                }
            }

            return result;
        } catch (Exception e) {
            log.warn("解析磁盘使用信息失败: json={}", diskUsagesJson, e);
            return new ArrayList<>();
        }
    }

    /**
     * 解析单个磁盘使用信息
     */
    private MetricsResponse.DiskUsage parseSingleDiskUsage(JsonNode node) {
        try {
            String mount = node.has("mount") ? node.get("mount").asText() : "";
            String name = node.has("name") ? node.get("name").asText() : mount;
            Long totalGb = node.has("totalGb") ? node.get("totalGb").asLong() : null;
            Long usedGb = node.has("usedGb") ? node.get("usedGb").asLong() : null;
            Double usagePercent = node.has("usagePercent") ? node.get("usagePercent").asDouble() : null;
            // 兼容 usedPercent 字段名
            if (usagePercent == null && node.has("usedPercent")) {
                usagePercent = node.get("usedPercent").asDouble();
            }

            return MetricsResponse.DiskUsage.builder()
                    .mount(mount)
                    .name(name)
                    .totalGb(totalGb)
                    .usedGb(usedGb)
                    .usagePercent(usagePercent)
                    .build();
        } catch (Exception e) {
            log.warn("解析单个磁盘使用信息失败", e);
            return null;
        }
    }

    /**
     * 聚合监控数据
     */
    private MetricsHistoryResponse aggregateMetrics(
            List<AgentMetrics> metricsList,
            MetricsHistoryRequest.MetricType metricType,
            MetricsHistoryRequest.TimeRange timeRange
    ) {
        if (metricsList.isEmpty()) {
            return MetricsHistoryResponse.builder()
                    .timestamps(new ArrayList<>())
                    .values(new ArrayList<>())
                    .interval(timeRange.getInterval())
                    .build();
        }

        // 确定聚合间隔（秒）
        int intervalSeconds = parseIntervalToSeconds(timeRange.getInterval());
        long intervalMillis = intervalSeconds * 1000L;

        // 按时间间隔聚合
        Map<Long, List<BigDecimal>> groupedData = new HashMap<>();
        long startTime = metricsList.get(0).getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

        for (AgentMetrics metrics : metricsList) {
            long timestamp = metrics.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long bucket = ((timestamp - startTime) / intervalMillis) * intervalMillis + startTime;

            BigDecimal value = getMetricValue(metrics, metricType);
            if (value != null) {
                groupedData.computeIfAbsent(bucket, k -> new ArrayList<>()).add(value);
            }
        }

        // 计算每个时间桶的平均值
        List<String> timestamps = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        groupedData.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    timestamps.add(formatTimestamp(entry.getKey()));
                    double avgValue = entry.getValue().stream()
                            .mapToDouble(BigDecimal::doubleValue)
                            .average()
                            .orElse(0.0);
                    values.add(Math.round(avgValue * 10.0) / 10.0); // 保留一位小数
                });

        return MetricsHistoryResponse.builder()
                .timestamps(timestamps)
                .values(values)
                .interval(timeRange.getInterval())
                .build();
    }

    /**
     * 根据指标类型获取值
     */
    private BigDecimal getMetricValue(AgentMetrics metrics, MetricsHistoryRequest.MetricType metricType) {
        return switch (metricType) {
            case CPU -> metrics.getCpuPercent();
            case MEMORY -> metrics.getMemoryPercent();
            case DISK -> getMaxDiskUsage(metrics.getDiskUsages());
        };
    }

    /**
     * 获取磁盘最大使用率
     */
    private BigDecimal getMaxDiskUsage(String diskUsagesJson) {
        if (diskUsagesJson == null || diskUsagesJson.trim().isEmpty()) {
            return null;
        }

        try {
            // 解析 JSON
            JsonNode rootNode = objectMapper.readTree(diskUsagesJson);
            BigDecimal maxUsage = null;

            // 如果是数组格式
            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    BigDecimal usage = extractDiskUsagePercent(node);
                    if (usage != null && (maxUsage == null || usage.compareTo(maxUsage) > 0)) {
                        maxUsage = usage;
                    }
                }
            }
            // 如果是单个对象格式
            else if (rootNode.isObject()) {
                maxUsage = extractDiskUsagePercent(rootNode);
            }

            return maxUsage;
        } catch (Exception e) {
            log.warn("解析磁盘最大使用率失败: json={}", diskUsagesJson, e);
            return null;
        }
    }

    /**
     * 从 JSON 节点提取磁盘使用率
     */
    private BigDecimal extractDiskUsagePercent(JsonNode node) {
        // 优先使用 usagePercent
        if (node.has("usagePercent")) {
            return node.get("usagePercent").decimalValue();
        }
        // 兼容 usedPercent
        if (node.has("usedPercent")) {
            return node.get("usedPercent").decimalValue();
        }
        return null;
    }

    /**
     * 解析间隔字符串为秒数
     */
    private int parseIntervalToSeconds(String interval) {
        if (interval == null) return 10;
        if (interval.endsWith("s")) {
            return Integer.parseInt(interval.substring(0, interval.length() - 1));
        } else if (interval.endsWith("m")) {
            return Integer.parseInt(interval.substring(0, interval.length() - 1)) * 60;
        } else if (interval.endsWith("h")) {
            return Integer.parseInt(interval.substring(0, interval.length() - 1)) * 3600;
        } else if (interval.endsWith("d")) {
            return Integer.parseInt(interval.substring(0, interval.length() - 1)) * 86400;
        }
        return 10; // 默认10秒
    }

    /**
     * 格式化时间戳
     */
    private String formatTimestamp(long timestamp) {
        return java.time.Instant.ofEpochMilli(timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
