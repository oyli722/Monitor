package com.hundred.monitor.commonlibrary.request;

import com.hundred.monitor.commonlibrary.model.*;
import lombok.Data;

/**
 * 基本数据上报请求
 */
@Data
public class BasicReportRequest {

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * 时间戳
     */
    private String timestamp;

    /**
     * 基本信息数据
     */
    private BasicInfo basicInfo;
}
