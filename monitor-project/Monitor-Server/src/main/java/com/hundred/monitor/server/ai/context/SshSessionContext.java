package com.hundred.monitor.server.ai.context;

/**
 * SSH会话上下文持有者（ThreadLocal）
 * 用于在AI调用链中传递SSH Session ID
 */
public class SshSessionContext {

    private static final ThreadLocal<String> SSH_SESSION_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> AGENT_ID = new ThreadLocal<>();

    /**
     * 设置当前线程的SSH会话ID
     *
     * @param sshSessionId SSH会话ID
     */
    public static void setSshSessionId(String sshSessionId) {
        SSH_SESSION_ID.set(sshSessionId);
    }

    /**
     * 获取当前线程的SSH会话ID
     *
     * @return SSH会话ID，未设置返回null
     */
    public static String getSshSessionId() {
        return SSH_SESSION_ID.get();
    }

    /**
     * 设置当前线程的主机ID
     *
     * @param agentId 主机ID
     */
    public static void setAgentId(String agentId) {
        AGENT_ID.set(agentId);
    }

    /**
     * 获取当前线程的主机ID
     *
     * @return 主机ID，未设置返回null
     */
    public static String getAgentId() {
        return AGENT_ID.get();
    }

    /**
     * 清空当前线程的上下文
     * 应在请求处理完成后调用，防止ThreadLocal内存泄漏
     */
    public static void clear() {
        SSH_SESSION_ID.remove();
        AGENT_ID.remove();
    }

    /**
     * 检查上下文是否已设置
     *
     * @return true表示已设置
     */
    public static boolean isSet() {
        return SSH_SESSION_ID.get() != null;
    }
}
