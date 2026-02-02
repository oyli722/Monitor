package com.hundred.monitor.server.controller;

import com.hundred.monitor.server.model.request.SshCommandRequest;
import com.hundred.monitor.server.model.request.SshConnectRequest;
import com.hundred.monitor.commonlibrary.response.BaseResponse;
import com.hundred.monitor.server.model.response.SshConnectResponse;
import com.hundred.monitor.server.model.response.SshCredentialResponse;
import com.hundred.monitor.server.service.SshService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * SSH控制器
 * 处理SSH连接、断开、命令发送等请求
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ssh")
public class SshController {

    @Autowired
    private SshService sshService;

    /**
     * 获取SSH凭证
     * 查询指定Agent的SSH凭证（不返回密码）
     *
     * @param agentId Agent ID
     * @return 凭证信息
     */
    @GetMapping("/credential/{agentId}")
    public BaseResponse<SshCredentialResponse> getCredential(@PathVariable String agentId) {
        log.info("获取SSH凭证: agentId={}", agentId);

        try {
            var credential = sshService.getCredential(agentId);

            if (credential == null) {
                return BaseResponse.success(SshCredentialResponse.builder()
                        .hasCredential(false)
                        .username(null)
                        .build());
            }

            return BaseResponse.success(SshCredentialResponse.builder()
                    .hasCredential(true)
                    .username(credential.getUsername())
                    .build());
        } catch (Exception e) {
            log.error("获取SSH凭证失败: agentId={}", agentId, e);
            return BaseResponse.error("获取凭证失败");
        }
    }

    /**
     * 建立SSH连接
     * 前端提供agentId、username、password
     * 后端建立SSH连接并返回会话ID
     *
     * @param request 连接请求
     * @return 会话ID
     */
    @PostMapping("/connect")
    public BaseResponse<SshConnectResponse> connect(@RequestBody SshConnectRequest request) {
        log.info("收到SSH连接请求: agentId={}, username={}", request.getAgentId(), request.getUsername());

        try {
            String sessionId = sshService.connect(request);

            // 只有当密码不为空时才保存凭证，避免覆盖已保存的正确密码
            String password = request.getPassword();
            if (password != null && !password.isEmpty()) {
                sshService.saveCredential(request.getAgentId(), request.getUsername(), password);
            }

            return BaseResponse.success(SshConnectResponse.builder()
                    .success(true)
                    .sessionId(sessionId)
                    .build());
        } catch (Exception e) {
            log.error("SSH连接失败", e);
            return BaseResponse.error(e.getMessage());
        }
    }

    /**
     * 断开SSH连接
     *
     * @param request 断开请求（携带sessionId）
     * @return 操作结果
     */
    @PostMapping("/disconnect")
    public BaseResponse<Void> disconnect(@RequestBody SshCommandRequest request) {
        log.info("收到SSH断开请求: sessionId={}", request.getSessionId());

        try {
            sshService.disconnect(request.getSessionId());
            return BaseResponse.success();
        } catch (Exception e) {
            log.error("SSH断开失败", e);
            return BaseResponse.error(e.getMessage());
        }
    }

    /**
     * 发送SSH命令
     * 通过WebSocket已连接时，此接口可能不需要
     * 保留作为备用方案
     *
     * @param request 命令请求
     * @return 操作结果
     */
    @PostMapping("/command")
    public BaseResponse<Void> sendCommand(@RequestBody SshCommandRequest request) {
        log.debug("收到SSH命令: sessionId={}, command={}", request.getSessionId(), request.getCommand());

        try {
            sshService.sendCommand(request);
            return BaseResponse.success();
        } catch (Exception e) {
            log.error("SSH命令发送失败", e);
            return BaseResponse.error(e.getMessage());
        }
    }
}
