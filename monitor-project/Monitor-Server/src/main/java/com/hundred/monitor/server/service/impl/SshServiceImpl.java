package com.hundred.monitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hundred.monitor.server.mapper.AgentMapper;
import com.hundred.monitor.server.mapper.SshCredentialMapper;
import com.hundred.monitor.server.model.entity.Agent;
import com.hundred.monitor.server.model.entity.SshCredential;
import com.hundred.monitor.server.model.request.SshCommandRequest;
import com.hundred.monitor.server.model.request.SshConnectRequest;
import com.hundred.monitor.server.service.SshService;
import com.hundred.monitor.server.websocket.SshSession;
import com.hundred.monitor.server.websocket.SshSessionManager;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Properties;
import java.util.UUID;

/**
 * SSH服务实现类
 */
@Slf4j
@Service
public class SshServiceImpl implements SshService {

    @Autowired
    private SshCredentialMapper sshCredentialMapper;

    @Autowired
    private AgentMapper agentMapper;

    /**
     * SSH会话管理器（用于WebSocket通信）
     */
    private final SshSessionManager sessionManager = SshSessionManager.getInstance();

    /**
     * SSH连接默认端口
     */
    private static final int SSH_PORT = 22;

    /**
     * 连接超时时间（毫秒）
     */
    private static final int CONNECT_TIMEOUT = 5000;

    @Override
    public String connect(SshConnectRequest request) {
        // TODO: 验证agentId是否存在
        Agent agent = agentMapper.selectOne(new LambdaQueryWrapper<Agent>()
                .eq(Agent::getAgentId, request.getAgentId()));

        if (agent == null) {
            throw new RuntimeException("Agent不存在: " + request.getAgentId());
        }

        // 生成会话ID
        String sessionId = generateSessionId();

        // 确定SSH密码
        String sshPassword = request.getPassword();

        if (sshPassword == null || sshPassword.isEmpty()) {
            // 如果请求中没有密码，从数据库获取保存的凭证
            SshCredential credential = sshCredentialMapper.selectOne(new LambdaQueryWrapper<SshCredential>()
                    .eq(SshCredential::getAgentId, request.getAgentId()));

            if (credential != null) {
                sshPassword = credential.getPassword();
            } else {
                log.warn("未找到保存的SSH凭证: agentId={}", request.getAgentId());
                throw new RuntimeException("未提供SSH密码且未找到保存的凭证");
            }
        }

        // 建立SSH连接
        try {

            JSch jsch = new JSch();
            Session session = jsch.getSession(request.getUsername(), agent.getIp(), SSH_PORT);
            session.setPassword(sshPassword);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(CONNECT_TIMEOUT);
            session.connect();

            // TODO: 创建Shell通道
            ChannelShell channel = (ChannelShell) session.openChannel("shell");
            channel.setPtyType("xterm");
            channel.connect(1000);
            log.info("SSH通道已连接: sessionId={}", sessionId);

            // 立即获取输入输出流（必须在connect后立即获取）
            java.io.InputStream inputStream = channel.getInputStream();
            java.io.OutputStream outputStream = channel.getOutputStream();
            log.info("SSH流已获取: sessionId={}", sessionId);

            // 等待一小段时间，确保通道完全就绪
            Thread.sleep(2000);
            log.info("SSH通道状态: sessionId={}, connected={}, isOpen={}",
                    sessionId, channel.isConnected(), channel.getExitStatus());


            // TODO: 将会话保存到管理器
            SshSession sshSession = SshSession.builder()
                    .sessionId(sessionId)
                    .agentId(request.getAgentId())
                    .session(session)
                    .channel(channel)
                    .inputStream(inputStream)
                    .outputStream(outputStream)
                    .build();
            sessionManager.addSession(sessionId, sshSession);


            return sessionId;

        } catch (Exception e) {
            log.error("SSH连接失败: agentId={}", request.getAgentId(), e);
            throw new RuntimeException("SSH连接失败: " + e.getMessage());
        }
    }

    @Override
    public void disconnect(String sessionId) {
        // TODO: 获取会话并关闭
        SshSession sshSession = sessionManager.getSession(sessionId);
        if (sshSession != null) {
            try {
                if (sshSession.getChannel() != null && sshSession.getChannel().isConnected()) {
                    sshSession.getChannel().disconnect();
                }
                if (sshSession.getSession() != null && sshSession.getSession().isConnected()) {
                    sshSession.getSession().disconnect();
                }
                sessionManager.removeSession(sessionId);
                log.info("SSH连接已断开: sessionId={}", sessionId);
            } catch (Exception e) {
                log.error("SSH断开失败: sessionId={}", sessionId, e);
            }
        }
    }

    @Override
    public void sendCommand(SshCommandRequest request) {
        // TODO: 获取会话并发送命令
        SshSession sshSession = sessionManager.getSession(request.getSessionId());
        if (sshSession == null) {
            log.warn("SSH会话不存在: sessionId={}", request.getSessionId());
            return;
        }

        try {
            if (sshSession.getOutputStream() != null && sshSession.getChannel() != null && sshSession.getChannel().isConnected()) {
                // TODO: 将命令转换为字节数组发送
                String command = request.getCommand();
                if (command != null && !command.isEmpty()) {
                    byte[] bytes = (command + "\n").getBytes();
                    sshSession.getOutputStream().write(bytes);
                    sshSession.getOutputStream().flush();
                }
            }
        } catch (Exception e) {
            log.error("SSH命令发送失败: sessionId={}", request.getSessionId(), e);
        }
    }

    @Override
    public void saveCredential(String agentId, String username, String password) {
        // TODO: 加密密码（使用对称加密或BCrypt）
        String encryptedPassword = password;

        // TODO: 检查是否已存在
        SshCredential existing = sshCredentialMapper.selectOne(new LambdaQueryWrapper<SshCredential>()
                .eq(SshCredential::getAgentId, agentId));

        SshCredential credential;
        if (existing != null) {
            // 更新
            existing.setUsername( username);
            existing.setPassword(encryptedPassword);
            sshCredentialMapper.updateById(existing);
            log.info("SSH凭证已更新: agentId={}", agentId);
        } else {
            // 新增
            credential = SshCredential.builder()
                    .agentId(agentId)
                    .username(username)
                    .password(encryptedPassword)
                    .build();
            sshCredentialMapper.insert(credential);
            log.info("SSH凭证已保存: agentId={}", agentId);
        }
    }

    @Override
    public SshCredential getCredential(String agentId) {
        return sshCredentialMapper.selectOne(new LambdaQueryWrapper<SshCredential>()
                .eq(SshCredential::getAgentId, agentId));
    }

    /**
     * 生成会话ID
     */
    private String generateSessionId() {
        return "SSH_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
