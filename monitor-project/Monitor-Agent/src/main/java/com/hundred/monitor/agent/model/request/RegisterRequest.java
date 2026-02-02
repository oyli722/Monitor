package com.hundred.monitor.agent.model.request;

import com.hundred.monitor.agent.model.entity.BasicInfo;
import lombok.Data;

/**
 * 客户端注册请求
 */
@Data
public class RegisterRequest {

    /**
     * 主机名
     */
    private String hostname;

    /**
     * 客户端IP地址
     */
    private String ip;

    /**
     * 基本运行数据
     */
    private BasicInfo basicInfo;
}
