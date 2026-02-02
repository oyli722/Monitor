package com.hundred.monitor.agent.service;

import com.hundred.monitor.agent.config.ConfigLoader;
import com.hundred.monitor.agent.model.entity.AgentConfig;
import com.hundred.monitor.agent.model.entity.BasicInfo;
import com.hundred.monitor.agent.model.request.RegisterRequest;
import com.hundred.monitor.agent.model.response.CommonResponse;
import com.hundred.monitor.agent.model.response.RegisterResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;

/**
 * 注册服务
 * 负责客户端向服务端注册
 */
@Service
public class RegisterService {

    private static final Logger log = LoggerFactory.getLogger(RegisterService.class);

    private static final String LOCAL_IP = "127.0.0.1";

    private static final int BASE_RETRY_DELAY_SEC = 5;  // 基础重试延迟：5秒
    private static final int MAX_RETRY_DELAY_SEC = 60;    // 最大重试延迟：60秒
    private static final int MAX_RETRIES = 5;             // 最大重试次数

    private final RestTemplate restTemplate;

    @Autowired
    private ConfigLoader configLoader;

    @Autowired
    private CollectService collectService;

    @Autowired
    public RegisterService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 检查是否已注册
     */
    public boolean isRegistered() {
        AgentConfig config = configLoader.getConfig();
        return config.getAgent() != null
                && config.getAgent().getId() != null
                && !config.getAgent().getId().isEmpty();
    }

    /**
     * 获取当前配置
     */
    public AgentConfig getAgentConfig() {
        return configLoader.getConfig();
    }

    /**
     * 执行注册流程
     */
    public RegisterResponse register() {
        log.info("开始注册流程...");

        AgentConfig config = configLoader.getConfig();
        if (config.getServer() == null || config.getServer().getEndpoints() == null
                || config.getServer().getEndpoints().length == 0) {
            log.error("服务端地址未配置");
            return null;
        }

        String[] endpoints = config.getServer().getEndpoints();
        log.info("服务端地址列表: {}", String.join(", ", endpoints));

        // 遍历服务端地址，选择最快的可用地址
        String fastestServer = selectFastestServer(endpoints);
        if (fastestServer == null) {
            log.error("无法连接到任何服务端");
            return null;
        }

        log.info("选择服务端: {}", fastestServer);

        // 构造注册请求
        RegisterRequest request = buildRegisterRequest();

        // 调用注册接口
        RegisterResponse response = callRegister(fastestServer, request);

        // 保存注册信息
        if (response != null && response.getSuccess()) {
            saveRegistrationInfo(config, response);
            log.info("注册成功，Agent ID: {}, Agent Name: {}",
                    response.getAgentId(), response.getAgentName());
        } else {
            log.error("注册失败");
        }

        return response;
    }

    /**
     * 尝试连接服务端并选择最快的地址
     */
    private String selectFastestServer(String[] endpoints) {
        String fastestServer = null;
        long minResponseTime = Long.MAX_VALUE;

        for (String endpoint : endpoints) {
            log.info("尝试连接服务端: {}", endpoint);

            try {
                long responseTime = checkServerHealth(endpoint);
                if (responseTime > 0) {
                    log.info("服务端 {} 响应时间: {}ms", endpoint, responseTime);
                    if (responseTime < minResponseTime) {
                        minResponseTime = responseTime;
                        fastestServer = endpoint;
                    }
                }
            } catch (Exception e) {
                log.warn("连接服务端 {} 失败: {}", endpoint, e.getMessage());
            }
        }

        if (fastestServer == null) {
            log.warn("所有服务端均不可用，进入指数退避重试...");
            return retryWithBackoff(endpoints);
        }

        return fastestServer;
    }

    /**
     * 检查服务端健康状态
     */
    private long checkServerHealth(String endpoint) {
        long startTime = System.currentTimeMillis();

        try {
            String url = buildUrl(endpoint, "/api/health");
            ResponseEntity<CommonResponse> response = restTemplate.getForEntity(url, CommonResponse.class);

            long responseTime = System.currentTimeMillis() - startTime;

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null
                    && response.getBody().getSuccess()) {
                return responseTime;
            }

            log.warn("服务端 {} 健康检查失败", endpoint);
            return -1;

        } catch (Exception e) {
            log.warn("服务端 {} 连接异常: {}", endpoint, e.getMessage());
            return -1;
        }
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

    /**
     * 指数退避重试
     */
    private String retryWithBackoff(String[] endpoints) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            long delay = Math.min(BASE_RETRY_DELAY_SEC * (1L << attempt), MAX_RETRY_DELAY_SEC);
            log.info("等待 {} 秒后进行第 {} 次重试...", delay, attempt);

            try {
                Thread.sleep(delay * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }

            // 重试检查所有服务端
            for (String endpoint : endpoints) {
                long responseTime = checkServerHealth(endpoint);
                if (responseTime > 0) {
                    log.info("服务端 {} 恢复可用", endpoint);
                    return endpoint;
                }
            }
        }

        log.error("超过最大重试次数，放弃注册");
        return null;
    }

    /**
     * 构造注册请求
     */
    private RegisterRequest buildRegisterRequest() {
        RegisterRequest request = new RegisterRequest();

        // 采集基本信息
        BasicInfo basicInfo = collectService.collectBasicInfo();

        // 获取主机名
        String hostname = basicInfo.getHostname();

        // 暂时使用本地地址（后续由工具类替换）
        String ip = getLocalIp();

        request.setHostname(hostname);
        request.setIp(ip);
        request.setBasicInfo(basicInfo);

        log.debug("注册请求: hostname={}, ip={}", hostname, ip);
        return request;
    }

    /**
     * 调用注册接口
     */
    private RegisterResponse callRegister(String serverUrl, RegisterRequest request) {
        try {
            String url = buildUrl(serverUrl, "/api/v1/customer/register");
            ResponseEntity<RegisterResponse> response = restTemplate.postForEntity(url, request, RegisterResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("注册请求失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 保存注册信息
     */
    private void saveRegistrationInfo(AgentConfig config, RegisterResponse response) {
        AgentConfig.AgentInfo agentInfo = new AgentConfig.AgentInfo();
        agentInfo.setId(response.getAgentId());
        agentInfo.setName(response.getAgentName());
        agentInfo.setRegisteredAt(java.time.Instant.now().toString());

        AgentConfig.AuthConfig authConfig = new AgentConfig.AuthConfig();
        authConfig.setToken(response.getAuthToken());
        if (response.getTokenExpires() != null) {
            authConfig.setTokenExpires(response.getTokenExpires());
        } else {
            // 默认设置30天后过期
            authConfig.setTokenExpires(java.time.Instant.now()
                    .plusSeconds(30 * 24 * 3600).toString());
        }

        config.setAgent(agentInfo);
        config.setAuth(authConfig);

        configLoader.save(config);
    }

    /**
     * 获取本地IP（临时实现，后续由工具类替换）
     */
    private String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.warn("获取本地IP失败，使用默认地址: {}", e.getMessage());
            return LOCAL_IP;
        }
    }
}
