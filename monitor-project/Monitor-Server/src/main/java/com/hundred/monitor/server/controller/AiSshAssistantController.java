package com.hundred.monitor.server.controller;

import com.hundred.monitor.server.ai.entity.SshAssistantSessionInfo;
import com.hundred.monitor.server.ai.entity.SshSessionBinding;
import com.hundred.monitor.server.ai.utils.TerminalChatRedisUtils;
import com.hundred.monitor.server.model.request.ConnectRequest;
import com.hundred.monitor.commonlibrary.common.BaseResponse;
import com.hundred.monitor.server.model.response.ConnectResponse;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 主机详情页AI助手控制器
 * 处理AI助手连接的HTTP握手接口
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/ssh-assistant")
public class AiSshAssistantController {

    @Resource
    private TerminalChatRedisUtils aiSshRedisUtils;

    /**
     * 创建AI助手会话（连接）
     *
     * @param request 连接请求
     * @return AI会话ID
     */
    @PostMapping("/connect")
    public BaseResponse<ConnectResponse> connect(@RequestBody @Valid ConnectRequest request) {
        try {
            // 1. 生成唯一的AI会话ID
            String aiSessionId = UUID.randomUUID().toString();

            // 2. TODO: 从JWT中获取用户ID
            String userId = "default-user";

            // 3. 创建SSH与AI会话的绑定关系
            SshSessionBinding binding = SshSessionBinding.create(
                    aiSessionId,
                    request.getSshSessionId(),
                    request.getAgentId(),
                    userId
            );
            aiSshRedisUtils.saveBinding(binding);

            // 4. 创建会话信息
            SshAssistantSessionInfo sessionInfo = SshAssistantSessionInfo.builder()
                    .sessionId(aiSessionId)
                    .title("主机助手 - " + request.getAgentId())
                    .linkedAgentId(request.getAgentId())
                    .createdAt(System.currentTimeMillis())
                    .updatedAt(System.currentTimeMillis())
                    .messageCount(0)
                    .build();
            aiSshRedisUtils.saveSessionInfo(sessionInfo);

            log.info("创建AI助手会话: aiSessionId={}, agentId={}, sshSessionId={}",
                    aiSessionId, request.getAgentId(), request.getSshSessionId());

            // 5. 返回响应
            ConnectResponse response = ConnectResponse.builder()
                    .aiSessionId(aiSessionId)
                    .message("连接成功，请使用aiSessionId建立WebSocket连接")
                    .build();

            return BaseResponse.success(response);

        } catch (Exception e) {
            log.error("创建AI助手会话失败", e);
            return BaseResponse.error("创建AI助手会话失败: " + e.getMessage());
        }
    }

    /**
     * 断开AI助手会话
     *
     * @param aiSessionId AI会话ID
     * @return 操作结果
     */
    @DeleteMapping("/disconnect/{aiSessionId}")
    public BaseResponse<String> disconnect(@PathVariable String aiSessionId) {
        try {
            // 检查会话是否存在
            if (!aiSshRedisUtils.bindingExists(aiSessionId)) {
                return BaseResponse.notFound("AI会话不存在");
            }

            // 清理会话所有数据
            aiSshRedisUtils.cleanupSession(aiSessionId);

            log.info("断开AI助手会话: aiSessionId={}", aiSessionId);

            return BaseResponse.success("会话已断开");

        } catch (Exception e) {
            log.error("断开AI助手会话失败: aiSessionId={}", aiSessionId, e);
            return BaseResponse.error("断开AI助手会话失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话绑定信息
     *
     * @param aiSessionId AI会话ID
     * @return 绑定信息
     */
    @GetMapping("/binding/{aiSessionId}")
    public BaseResponse<SshSessionBinding> getBinding(@PathVariable String aiSessionId) {
        try {
            SshSessionBinding binding = aiSshRedisUtils.getBinding(aiSessionId);

            if (binding == null) {
                return BaseResponse.notFound("绑定关系不存在");
            }

            return BaseResponse.success(binding);

        } catch (Exception e) {
            log.error("获取绑定信息失败: aiSessionId={}", aiSessionId, e);
            return BaseResponse.error("获取绑定信息失败: " + e.getMessage());
        }
    }

    /**
     * 检查会话是否活跃
     *
     * @param aiSessionId AI会话ID
     * @return true表示活跃
     */
    @GetMapping("/active/{aiSessionId}")
    public BaseResponse<Boolean> isActive(@PathVariable String aiSessionId) {
        try {
            boolean active = aiSshRedisUtils.isSessionActive(aiSessionId);
            return BaseResponse.success(active);

        } catch (Exception e) {
            log.error("检查会话活跃状态失败: aiSessionId={}", aiSessionId, e);
            return BaseResponse.error("检查会话活跃状态失败: " + e.getMessage());
        }
    }
}
