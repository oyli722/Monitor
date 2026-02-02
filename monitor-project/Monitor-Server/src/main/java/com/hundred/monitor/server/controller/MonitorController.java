package com.hundred.monitor.server.controller;

import com.hundred.monitor.server.model.response.AgentBasicInfoResponse;
import com.hundred.monitor.commonlibrary.response.BaseResponse;
import com.hundred.monitor.server.model.response.MetricsHistoryResponse;
import com.hundred.monitor.server.model.response.MetricsResponse;
import com.hundred.monitor.server.model.request.MetricsHistoryRequest;
import com.hundred.monitor.server.service.MonitorService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 前端进行监控 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {
    @Resource
    private MonitorService monitorService;

    /**
     * 获取所有监控信息
     *
     * @return 响应
     */
    @GetMapping("/getMonitorList")
    public BaseResponse<AgentBasicInfoResponse> getMonitorList() {
        AgentBasicInfoResponse response = monitorService.getMonitorList();
        return response.getSuccess() ? BaseResponse.success(response) : BaseResponse.error(response.getMessage());
    }

    /**
     * 获取主机最新监控指标
     *
     * @param agentId Agent ID
     * @return 监控指标响应
     */
    @GetMapping("/{agentId}/metrics/latest")
    public BaseResponse<MetricsResponse> getLatestMetrics(@PathVariable String agentId) {
        try {
            MetricsResponse metrics = monitorService.getLatestMetrics(agentId);
            return BaseResponse.success(metrics);
        } catch (Exception e) {
            log.error("获取主机监控指标失败: agentId={}", agentId, e);
            return BaseResponse.error("获取监控指标失败");
        }
    }

    /**
     * 获取主机历史监控指标
     *
     * @param agentId Agent ID
     * @param request 查询请求
     * @return 历史数据响应
     */
    @GetMapping("/{agentId}/metrics/history")
    public BaseResponse<MetricsHistoryResponse> getMetricsHistory(
            @PathVariable String agentId,
            MetricsHistoryRequest request
    ) {
        try {
            MetricsHistoryResponse history = monitorService.getMetricsHistory(agentId, request);
            return BaseResponse.success(history);
        } catch (IllegalArgumentException e) {
            log.warn("参数错误: {}", e.getMessage());
            return BaseResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("获取历史监控数据失败: agentId={}, request={}", agentId, request, e);
            return BaseResponse.error("获取历史数据失败");
        }
    }
}
