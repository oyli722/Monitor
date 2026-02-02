package com.hundred.monitor.agent.controller;

import com.hundred.monitor.agent.model.request.UpdateConfigRequest;
import com.hundred.monitor.commonlibrary.response.BaseResponse;
import com.hundred.monitor.agent.service.ConfigUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent控制器
 * 接收服务端的远程调用
 *
 * 注意：SSH连接由服务端直接处理（使用预设用户名/密码），
 * 客户端只负责确保SSH服务在22端口监听
 */
@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    @Autowired
    private ConfigUpdateService configUpdateService;

    /**
     * 更新配置
     */
    @PostMapping("/config")
    public BaseResponse updateConfig(@RequestBody UpdateConfigRequest request) {
        // TODO: 验证agentId
        return configUpdateService.updateConfig(request.getConfigUpdates());
    }

}
