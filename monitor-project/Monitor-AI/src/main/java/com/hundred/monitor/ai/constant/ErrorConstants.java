package com.hundred.monitor.ai.constant;

/**
 * 错误消息常量
 */
public class ErrorConstants {

    /**
     * 会话相关
     */
    public static final String SESSION_NOT_FOUND = "会话不存在";
    public static final String SESSION_CREATE_FAILED = "创建会话失败";
    public static final String SESSION_DELETE_FAILED = "删除会话失败";
    public static final String SESSION_LIST_FAILED = "获取会话列表失败";
    public static final String SESSION_GET_FAILED = "获取会话详情失败";

    /**
     * 消息相关
     */
    public static final String MESSAGE_SEND_FAILED = "发送消息失败";
    public static final String MESSAGE_GET_FAILED = "获取消息历史失败";
    public static final String MESSAGE_CLEAR_FAILED = "清空消息失败";

    /**
     * 主机相关
     */
    public static final String AGENT_LINK_FAILED = "关联主机失败";
    public static final String AGENT_NOT_FOUND = "主机不存在";

    /**
     * AI服务相关
     */
    public static final String AI_SERVICE_UNAVAILABLE = "AI服务暂时不可用，请稍后再试";
    public static final String AI_MODEL_ERROR = "AI模型调用失败";

    /**
     * 认证相关
     */
    public static final String UNAUTHORIZED = "未授权访问";
    public static final String TOKEN_INVALID = "Token无效或已过期";

    private ErrorConstants() {
        // 防止实例化
    }
}
