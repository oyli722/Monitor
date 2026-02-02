package com.hundred.monitor.server.controller;

import com.hundred.monitor.server.model.response.BaseResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * 健康检查接口
     * 供客户端注册前检查服务端是否可用
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public BaseResponse<Void> health() {
        // TODO: 可扩展检查数据库连接、Redis连接等
        return BaseResponse.success();
    }
}
