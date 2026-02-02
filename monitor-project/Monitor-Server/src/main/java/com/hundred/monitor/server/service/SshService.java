package com.hundred.monitor.server.service;

import com.hundred.monitor.server.model.entity.SshCredential;
import com.hundred.monitor.server.model.request.SshCommandRequest;
import com.hundred.monitor.server.model.request.SshConnectRequest;

/**
 * SSH服务接口
 */
public interface SshService {

    /**
     * 建立SSH连接
     *
     * @param request 连接请求
     * @return 会话ID
     */
    String connect(SshConnectRequest request);

    /**
     * 断开SSH连接
     *
     * @param sessionId 会话ID
     */
    void disconnect(String sessionId);

    /**
     * 发送SSH命令
     *
     * @param request 命令请求
     */
    void sendCommand(SshCommandRequest request);

    /**
     * 保存SSH凭证
     *
     * @param agentId Agent ID
     * @param username SSH用户名
     * @param password SSH密码（加密后）
     */
    void saveCredential(String agentId, String username, String password);

    /**
     * 获取SSH凭证
     *
     * @param agentId Agent ID
     * @return SSH凭证
     */
    SshCredential getCredential(String agentId);
}
