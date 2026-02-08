package com.hundred.monitor.ai.exception;

import com.hundred.monitor.commonlibrary.common.BaseResponse;

/**
 * 聊天会话不存在异常
 */
public class ChatSessionNotFoundException extends BaseException {

    public ChatSessionNotFoundException(String sessionId) {
        super(BaseResponse.NOT_FOUND_CODE, "会话不存在: " + sessionId);
    }
}
