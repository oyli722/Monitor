package com.hundred.monitor.server.controller;

import com.hundred.monitor.server.model.request.BasicReportRequest;
import com.hundred.monitor.server.model.request.CustomerRegisterRequest;
import com.hundred.monitor.server.model.request.MetricsReportRequest;
import com.hundred.monitor.server.model.response.AgentRegisterResponse;
import com.hundred.monitor.commonlibrary.common.BaseResponse;
import com.hundred.monitor.server.service.AgentMetricsService;
import com.hundred.monitor.server.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent控制器
 * 处理客户端注册、数据上报等请求
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class AgentController {

    @Autowired
    private AgentService agentService;

    @Autowired
    private AgentMetricsService agentMetricsService;

    /**
     * 客户端注册接口
     * 供客户端首次启动时注册使用
     *
     * @param request 注册请求
     * @return 注册响应
     */
    @PostMapping("/customer/register")
    public BaseResponse<AgentRegisterResponse> register(@RequestBody CustomerRegisterRequest request) {
        log.info("收到客户端注册请求: hostname={}, ip={}", request.getHostname(), request.getIp());

        try {
            AgentRegisterResponse response = agentService.register(request);
            log.info("客户端注册成功: agentId={}, agentName={}", response.getAgentId(), response.getAgentName());
            return BaseResponse.success(response);
        } catch (Exception e) {
            log.error("客户端注册失败", e);
            return BaseResponse.error("客户端注册失败: " + e.getMessage());
        }
    }

    /**
     * 基本数据上报接口
     * 供客户端每10分钟上报一次
     *
     * @param request 基本数据上报请求
     * @return 上报结果
     */
    @PostMapping("/agent/basic")
    public BaseResponse<Void> reportBasic(@RequestBody BasicReportRequest request) {
        log.info("收到基本数据上报: agentId={}", request.getAgentId());

        try {
            agentService.updateBasicInfo(request.getAgentId(), request.getBasicInfo());
            return BaseResponse.success();
        } catch (Exception e) {
            log.error("基本数据上报失败: agentId={}", request.getAgentId(), e);
            return BaseResponse.error("基本数据上报失败: " + e.getMessage());
        }
    }

    /**
     * 运行时数据上报接口
     * 供客户端每15秒上报一次
     *
     * @param request 运行时数据上报请求
     * @return 上报结果
     */
    @PostMapping("/agent/metrics")
    public BaseResponse<Void> reportMetrics(@RequestBody MetricsReportRequest request) {
        log.info("收到运行时数据上报: agentId={}", request.getAgentId());

        try {
            agentMetricsService.saveMetrics(request);
            return BaseResponse.success();
        } catch (Exception e) {
            log.error("运行时数据上报失败: agentId={}", request.getAgentId(), e);
            return BaseResponse.error("运行时数据上报失败: " + e.getMessage());
        }
    }
}
