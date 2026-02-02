package com.hundred.monitor.server.websocket;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.Session;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.web.socket.WebSocketSession;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSH会话实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SshSession {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * SSH会话
     */
    private Session session;

    /**
     * SSH Shell通道
     */
    private ChannelShell channel;

    /**
     * WebSocket会话（用于向前端推送数据）
     */
    @Builder.Default
    private ConcurrentHashMap<String, WebSocketSession> webSocketSessions = new ConcurrentHashMap<>();

    /**
     * SSH输入流（用于读取SSH输出）
     */
    private InputStream inputStream;

    /**
     * SSH输出流（用于向SSH发送命令）
     */
    private OutputStream outputStream;
}
