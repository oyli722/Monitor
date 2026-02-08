package com.hundred.monitor.ai.exception;

import com.hundred.monitor.commonlibrary.common.BaseResponse;

/**
 * 业务异常
 */
public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super(BaseResponse.INTERNAL_SERVER_ERROR_CODE, message);
    }

    public BusinessException(String message, Throwable cause) {
        super(BaseResponse.INTERNAL_SERVER_ERROR_CODE, message, cause);
    }

    public BusinessException(Integer code, String message) {
        super(code, message);
    }
}
