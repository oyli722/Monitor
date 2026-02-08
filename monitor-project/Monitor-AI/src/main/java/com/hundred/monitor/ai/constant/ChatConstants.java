package com.hundred.monitor.ai.constant;

/**
 * 聊天相关常量
 */
public class ChatConstants {

    /**
     * 消息数量阈值（超过此数量触发总结）
     */
    public static final int MESSAGE_THRESHOLD = 20;

    /**
     * 最近消息数量
     */
    public static final int RECENT_MESSAGE_COUNT = 10;

    /**
     * 会话标题最大长度
     */
    public static final int SESSION_TITLE_MAX_LENGTH = 20;

    /**
     * 会话标题后缀
     */
    public static final String SESSION_TITLE_SUFFIX = "...";

    /**
     * 默认用户ID
     */
    public static final String DEFAULT_USER_ID = "default-user";

    /**
     * Bearer Token前缀
     */
    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * 会话TTL天数
     */
    public static final long SESSION_TTL_DAYS = 30;

    /**
     * 总结提示词
     */
    public static final String SUMMARY_PROMPT = """
            请将以下对话内容总结为一段简洁的摘要，保留关键信息（用户意图、重要操作、结论）：
            """;

    private ChatConstants() {
        // 防止实例化
    }
}
