package com.hundred.monitor.commonlibrary.request;

import com.hundred.monitor.commonlibrary.model.BasicInfo;
import lombok.Data;

/**
 * Agent注册请求
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
     * 基本硬件信息
     */
    private BasicInfo basicInfo;
}
