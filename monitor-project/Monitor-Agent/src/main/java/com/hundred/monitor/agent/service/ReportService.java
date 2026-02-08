package com.hundred.monitor.agent.service;

import com.hundred.monitor.agent.config.ConfigLoader;
import com.hundred.monitor.agent.model.ServerEndpoint;
import com.hundred.monitor.agent.model.entity.AgentConfig;
import com.hundred.monitor.agent.model.entity.BasicInfo;
import com.hundred.monitor.agent.model.entity.Metrics;
import com.hundred.monitor.agent.model.request.BasicReportRequest;
import com.hundred.monitor.agent.model.request.MetricsReportRequest;
import com.hundred.monitor.agent.model.response.CommonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Random;

/**
 * 上报服务
 * 负责双频率数据上报
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final RestTemplate restTemplate;
    private final Random random = new Random();

    @Autowired
    private CollectService collectService;

    @Autowired
    private ConfigLoader configLoader;

    @Autowired(required = false)
    private ServiceDiscoveryService serviceDiscoveryService;

    @Autowired
    public ReportService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 基本数据上报
     * 频率：每10分钟
     */
    @Scheduled(fixedRateString = "${reporting.basic_interval_sec:600}000")
    public void reportBasic() {
        AgentConfig config = configLoader.getConfig();

        // 检查是否已注册
        if (!isRegistered(config)) {
            log.warn("客户端未注册，跳过基本数据上报");
            return;
        }

        try {
            // 采集基本数据
            BasicInfo basicInfo = collectService.collectBasicInfo();

            // 构造上报请求
            BasicReportRequest request = new BasicReportRequest();
            request.setAgentId(config.getAgent().getId());
            request.setTimestamp(Instant.now().toString());
            request.setBasicInfo(basicInfo);

            // 调用服务端接口
            doReport(request, "基本数据");

        } catch (Exception e) {
            log.error("基本数据上报异常", e);
        }
    }

    /**
     * 运行时数据上报
     * 频率：每15秒
     */
    @Scheduled(fixedRateString = "${reporting.metrics_interval_sec:15}000")
    public void reportMetrics() {
        AgentConfig config = configLoader.getConfig();

        // 检查是否已注册
        if (!isRegistered(config)) {
            log.warn("客户端未注册，跳过运行时数据上报");
            return;
        }

        try {
            // 采集运行时数据
            Metrics metrics = collectService.collectMetrics();

            // 构造上报请求
            MetricsReportRequest request = new MetricsReportRequest();
            request.setAgentId(config.getAgent().getId());
            request.setTimestamp(Instant.now().toString());
            request.setMetrics(metrics);

            // 调用服务端接口
            doReport(request, "运行时数据");

        } catch (Exception e) {
            log.error("运行时数据上报异常", e);
        }
    }

    /**
     * 执行上报请求
     */
    private void doReport(Object request, String reportType) {
        AgentConfig config = configLoader.getConfig();

        try {
            // 构造URL
            String serverUrl = getRegisteredServerUrl(config);
            if (serverUrl == null) {
                log.warn("{}上报失败：未找到可用服务端", reportType);
                return;
            }

            // 根据类型选择端点
            String endpoint = request instanceof BasicReportRequest
                    ? "/api/v1/agent/basic"
                    : "/api/v1/agent/metrics";

            String url = buildUrl(serverUrl, endpoint);

            // 发送请求
            ResponseEntity<CommonResponse> response = restTemplate.postForEntity(url, request, CommonResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null
                    && response.getBody().getSuccess()) {
                log.info("{}上报成功", reportType);
            } else {
                log.warn("{}上报失败，响应状态: {}", reportType, response.getStatusCode());
            }

        } catch (Exception e) {
            // 异常处理：记录日志，丢弃数据，等待下次定时触发
            log.error("{}上报失败: {}", reportType, e.getMessage());
        }
    }

    /**
     * 检查是否已注册
     */
    private boolean isRegistered(AgentConfig config) {
        return config.getAgent() != null
                && config.getAgent().getId() != null
                && !config.getAgent().getId().isEmpty();
    }

    /**
     * 获取已注册的服务端URL
     * 优先使用服务发现获取的服务器列表，然后使用简单的负载均衡策略选择一个服务器
     * 如果服务发现不可用，则回退到静态配置
     */
    private String getRegisteredServerUrl(AgentConfig config) {
        // Try service discovery first
        if (serviceDiscoveryService != null) {
            List<ServerEndpoint> servers = serviceDiscoveryService.getAvailableServers();
            if (!servers.isEmpty()) {
                // Simple random load balancing
                ServerEndpoint selected = servers.get(random.nextInt(servers.size()));
                log.debug("Using server from service discovery: {}", selected.getUrl());
                return selected.getUrl();
            } else {
                log.warn("Service discovery returned empty server list");
            }
        }

        // Fallback to static configuration
        log.debug("Using static configuration for server URL");
        if (config.getServer() != null && config.getServer().getEndpoints() != null
                && config.getServer().getEndpoints().length > 0) {
            // Use random selection from static config as well
            String[] endpoints = config.getServer().getEndpoints();
            return endpoints[random.nextInt(endpoints.length)];
        }
        return null;
    }

    /**
     * 构造URL
     */
    private String buildUrl(String endpoint, String path) {
        // 判断是否已包含协议
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return endpoint + path;
        }
        // 默认使用http协议
        return "http://" + endpoint + path;
    }
}
