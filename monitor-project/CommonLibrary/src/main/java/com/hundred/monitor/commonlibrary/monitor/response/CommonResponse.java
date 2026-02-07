package com.hundred.monitor.commonlibrary.monitor.response;

import lombok.Data;

/**
 * 通用响应模型
 */
@Data
public class CommonResponse {

    /**
     * 请求是否成功
     */
    private Boolean success;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private Object data;

    /**
     * 成功响应（无数据）
     */
    public static CommonResponse success() {
        CommonResponse response = new CommonResponse();
        response.setSuccess(true);
        return response;
    }

    /**
     * 成功响应（带数据）
     */
    public static CommonResponse success(Object data) {
        CommonResponse response = new CommonResponse();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }

    /**
     * 错误响应
     */
    public static CommonResponse error(String message) {
        CommonResponse response = new CommonResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
