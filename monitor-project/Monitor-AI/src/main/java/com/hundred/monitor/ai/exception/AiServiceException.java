package com.hundred.monitor.ai.exception;

import com.hundred.monitor.commonlibrary.common.BaseResponse;

/**
 * AI服务异常
 */
public class AiServiceException extends BaseException {

    public AiServiceException(String message) {
        super(BaseResponse.INTERNAL_SERVER_ERROR_CODE, "AI服务异常: " + message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(BaseResponse.INTERNAL_SERVER_ERROR_CODE, message, cause);
    }
}
