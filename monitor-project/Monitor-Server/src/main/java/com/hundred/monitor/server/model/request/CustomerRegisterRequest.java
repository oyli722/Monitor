package com.hundred.monitor.server.model.request;

import com.hundred.monitor.commonlibrary.model.BasicInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent注册请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRegisterRequest {

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
