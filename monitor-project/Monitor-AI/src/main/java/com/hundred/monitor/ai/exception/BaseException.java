package com.hundred.monitor.ai.exception;

import com.hundred.monitor.commonlibrary.common.BaseResponse;
import lombok.Getter;

/**
 * 自定义异常基类
 */
@Getter
public class BaseException extends RuntimeException {

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    public BaseException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BaseException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    /**
     * 转换为BaseResponse
     */
    public <T> BaseResponse<T> toResponse() {
        return BaseResponse.error(code, message);
    }
}
