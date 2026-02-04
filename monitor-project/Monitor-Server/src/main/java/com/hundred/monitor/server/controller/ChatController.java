package com.hundred.monitor.server.controller;

import com.hundred.monitor.server.ai.entity.ChatMessage;
import com.hundred.monitor.server.ai.entity.ChatSessionInfo;
import com.hundred.monitor.server.ai.service.ChatService;
import com.hundred.monitor.server.model.request.CreateSessionRequest;
import com.hundred.monitor.server.model.request.SendMessageRequest;
import com.hundred.monitor.server.model.response.*;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI聊天控制器 - 处理AI对话相关请求
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Resource
    private ChatService chatService;

    /**
     * 创建新会话
     *
     * @param request 创建会话请求
     * @return 会话ID和标题
     */
    @PostMapping("/sessions")
    public BaseResponse<CreateSessionResponse> createSession(@RequestBody @Valid CreateSessionRequest request) {
        try {
            // TODO: 从JWT中获取用户ID
            String userId = "default-user";
            String sessionId = chatService.createSession(userId, request.getFirstMessage());

            // 如果指定了主机ID，关联主机
            if (request.getAgentId() != null && !request.getAgentId().isEmpty()) {
                chatService.linkAgent(sessionId, request.getAgentId());
            }

            ChatSessionInfo sessionInfo = chatService.getSession(sessionId);
            CreateSessionResponse response = CreateSessionResponse.builder()
                    .sessionId(sessionId)
                    .title(sessionInfo != null ? sessionInfo.getTitle() : "")
                    .build();

            return BaseResponse.success(response);
        } catch (Exception e) {
            log.error("创建会话失败", e);
            return BaseResponse.error("创建会话失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的所有会话列表
     *
     * @return 会话列表
     */
    @GetMapping("/sessions")
    public BaseResponse<List<SessionInfoResponse>> getSessions() {
        try {
            // TODO: 从JWT中获取用户ID
            String userId = "default-user";

            List<ChatSessionInfo> sessions = chatService.getUserSessions(userId);

            List<SessionInfoResponse> responseList = sessions.stream()
                    .map(this::toSessionInfoResponse)
                    .collect(Collectors.toList());

            return BaseResponse.success(responseList);
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return BaseResponse.error("获取会话列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话详情
     *
     * @param sessionId 会话ID
     * @return 会话信息
     */
    @GetMapping("/sessions/{sessionId}")
    public BaseResponse<SessionInfoResponse> getSession(@PathVariable String sessionId) {
        try {
            ChatSessionInfo sessionInfo = chatService.getSession(sessionId);
            if (sessionInfo == null) {
                return BaseResponse.notFound("会话不存在");
            }

            return BaseResponse.success(toSessionInfoResponse(sessionInfo));
        } catch (Exception e) {
            log.error("获取会话详情失败: sessionId={}", sessionId, e);
            return BaseResponse.error("获取会话详情失败: " + e.getMessage());
        }
    }

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/sessions/{sessionId}")
    public BaseResponse<Void> deleteSession(@PathVariable String sessionId) {
        try {
            // TODO: 从JWT中获取用户ID
            String userId = "default-user";

            chatService.deleteSession(userId, sessionId);
            return BaseResponse.success();
        } catch (Exception e) {
            log.error("删除会话失败: sessionId={}", sessionId, e);
            return BaseResponse.error("删除会话失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话的消息历史
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public BaseResponse<List<ChatMessageResponse>> getMessages(@PathVariable String sessionId) {
        try {
            List<ChatMessage> messages = chatService.getMessages(sessionId);

            List<ChatMessageResponse> responseList = messages.stream()
                    .map(this::toChatMessageResponse)
                    .collect(Collectors.toList());

            return BaseResponse.success(responseList);
        } catch (Exception e) {
            log.error("获取消息历史失败: sessionId={}", sessionId, e);
            return BaseResponse.error("获取消息历史失败: " + e.getMessage());
        }
    }

    /**
     * 发送消息并获取AI回复
     *
     * @param request 发送消息请求
     * @return AI回复
     */
    @PostMapping("/messages")
    public BaseResponse<ChatResponse> sendMessage(@RequestBody @Valid SendMessageRequest request) {
        try {
            // TODO: 从JWT中获取用户ID
            String userId = "default-user";

            String reply = chatService.sendMessage(
                    request.getSessionId(),
                    userId,
                    request.getMessage(),
                    request.getModelName()
            );

            ChatMessageResponse message = ChatMessageResponse.builder()
                    .role("assistant")
                    .content(reply)
                    .timestamp(System.currentTimeMillis())
                    .build();

            ChatResponse response = ChatResponse.builder()
                    .sessionId(request.getSessionId())
                    .reply(reply)
                    .message(message)
                    .build();

            return BaseResponse.success(response);
        } catch (IllegalArgumentException e) {
            log.warn("发送消息参数错误: {}", e.getMessage());
            return BaseResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("发送消息失败: sessionId={}", request.getSessionId(), e);
            return BaseResponse.error("发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 清空会话消息
     *
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/sessions/{sessionId}/messages")
    public BaseResponse<Void> clearMessages(@PathVariable String sessionId) {
        try {
            chatService.clearMessages(sessionId);
            return BaseResponse.success();
        } catch (Exception e) {
            log.error("清空消息失败: sessionId={}", sessionId, e);
            return BaseResponse.error("清空消息失败: " + e.getMessage());
        }
    }

    /**
     * 关联主机到会话
     *
     * @param sessionId 会话ID
     * @param agentId   主机ID
     * @return 操作结果
     */
    @PostMapping("/sessions/{sessionId}/link")
    public BaseResponse<Void> linkAgent(
            @PathVariable String sessionId,
            @RequestParam String agentId) {
        try {
            chatService.linkAgent(sessionId, agentId);
            return BaseResponse.success();
        } catch (Exception e) {
            log.error("关联主机失败: sessionId={}, agentId={}", sessionId, agentId, e);
            return BaseResponse.error("关联主机失败: " + e.getMessage());
        }
    }

    // ==================== 转换方法 ====================

    /**
     * 转换为会话信息响应
     */
    private SessionInfoResponse toSessionInfoResponse(ChatSessionInfo sessionInfo) {
        return SessionInfoResponse.builder()
                .sessionId(sessionInfo.getSessionId())
                .title(sessionInfo.getTitle())
                .createdAt(sessionInfo.getCreatedAt())
                .updatedAt(sessionInfo.getUpdatedAt())
                .messageCount(sessionInfo.getMessageCount())
                .linkedAgentId(sessionInfo.getLinkedAgentId())
                .build();
    }

    /**
     * 转换为聊天消息响应
     */
    private ChatMessageResponse toChatMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .role(message.getRole())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .build();
    }
}
