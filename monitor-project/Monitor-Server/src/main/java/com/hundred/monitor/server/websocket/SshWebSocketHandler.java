package com.hundred.monitor.server.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * SSH WebSocket处理器
 * 处理前端SSH终端的WebSocket连接
 */
@Slf4j
@Component
public class SshWebSocketHandler extends TextWebSocketHandler {

    private final SshSessionManager sessionManager = SshSessionManager.getInstance();

    @Override
    protected void handleTextMessage(WebSocketSession webSocketSession, TextMessage message) {
        String payload = message.getPayload();
        forwardToSsh(webSocketSession, payload);
    }


    @Override
    protected void handleBinaryMessage(WebSocketSession webSocketSession, BinaryMessage message) {
        // TODO: 处理二进制消息（如终端大小调整等）
        log.debug("收到WebSocket二进制消息: sessionId={}", webSocketSession.getId());
    }

    @Override
    protected void handlePongMessage(WebSocketSession webSocketSession, PongMessage message) {
        // TODO: 处理Pong消息（心跳）
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) {
        String sessionId = extractSessionId(webSocketSession.getUri().toString());

        // TODO: 将WebSocket会话与SSH会话关联
        SshSession sshSession = sessionManager.getSession(sessionId);
        if (sshSession == null) {
            log.error("SSH会话不存在: sessionId={}", sessionId);
            return;
        }

        sshSession.getWebSocketSessions().put(sessionId, webSocketSession);
        log.info("WebSocket会话已添加: 当前WebSocket数量={}", sshSession.getWebSocketSessions().size());

        // TODO: 启动SSH输出读取线程，将输出通过WebSocket推送
        startOutputReader(sshSession);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus closeStatus) {
        String sessionId = extractSessionId(webSocketSession.getUri().toString());
        log.info("WebSocket连接已关闭: sessionId={}, status={}", sessionId, closeStatus);

        // TODO: 移除WebSocket会话，可选关闭SSH连接
        SshSession sshSession = sessionManager.getSession(sessionId);
        if (sshSession != null) {
            sshSession.getWebSocketSessions().remove(sessionId);
            // 如果没有其他WebSocket连接，可以考虑关闭SSH会话
        }
    }

    @Override
    public void handleTransportError(WebSocketSession webSocketSession, Throwable exception) {
        log.error("WebSocket传输错误: sessionId={}", webSocketSession.getId(), exception);
    }

    /**
     * 将消息转发到SSH
     */
    private void forwardToSsh(WebSocketSession webSocketSession, String message) {
        String sessionId = extractSessionId(Objects.requireNonNull(webSocketSession.getUri()).toString());
        log.info("转发消息到SSH: sessionId={}, message='{}'", sessionId, message);
        log.info("message is : {}",message.getBytes());
        SshSession sshSession = sessionManager.getSession(sessionId);
        if (sshSession == null) {
            log.warn("SSH会话不存在: sessionId={}", sessionId);
            return;
        }
        if (sshSession.getOutputStream() == null) {
            log.warn("SSH输出流不存在: sessionId={}", sessionId);
            return;
        }
        try {
            byte[] bytes = message.getBytes();
            sshSession.getOutputStream().write(bytes);
            sshSession.getOutputStream().flush();
            log.info("消息已发送到SSH: sessionId={}, bytes={}", sessionId, bytes.length);
        } catch (Exception e) {
            log.error("SSH消息发送失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 从URI中提取会话ID
     */
    private String extractSessionId(String uri) {
        // TODO: 从WebSocket URI中提取sessionId，格式如 /api/v1/ssh/terminal/SSH_ABC123
        if (uri != null && uri.contains("/terminal/")) {
            return uri.substring(uri.lastIndexOf("/terminal/") + 10);
        }
        return uri;
    }

    /**
     * 启动SSH输出读取线程
     */
    private void startOutputReader(SshSession sshSession) {
        new Thread(() -> {
            String sessionId = sshSession.getSessionId();
            log.info("SSH输出读取线程已启动: sessionId={}", sessionId);

            try {
                // 使用连接时已获取的InputStream
                InputStream inputStream = sshSession.getInputStream();
                if (inputStream == null) {
                    log.error("SSH InputStream为空: sessionId={}", sessionId);
                    return;
                }

                byte[] buffer = new byte[1024 * 1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    String output = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    log.info("SSH输出已接收: sessionId={}, output='{}'", sessionId, output);
                    sendToWebSocket(sshSession, output);
                }
            } catch (Exception e) {
                log.error("SSH输出读取失败: sessionId={}", sessionId, e);
            }
        }).start();
    }

    /**
     * 通过WebSocket发送SSH输出
     */
    private void sendToWebSocket(SshSession sshSession, String output) {
        for (WebSocketSession wsSession : sshSession.getWebSocketSessions().values()) {
            if (wsSession.isOpen()) {
                try {
                    wsSession.sendMessage(new TextMessage(output));
                } catch (Exception e) {
                    log.error("WebSocket消息发送失败", e);
                }
            }
        }
    }
}
