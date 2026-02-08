package com.hundred.monitor.ai.controller;

import com.hundred.monitor.ai.constant.ErrorConstants;
import com.hundred.monitor.ai.service.ChatService;
import com.hundred.monitor.ai.util.JwtUtil;
import com.hundred.monitor.commonlibrary.ai.model.ChatMessage;
import com.hundred.monitor.commonlibrary.ai.model.ChatSessionInfo;
import com.hundred.monitor.commonlibrary.ai.request.CreateSessionRequest;
import com.hundred.monitor.commonlibrary.ai.request.SendMessageRequest;
import com.hundred.monitor.commonlibrary.ai.response.ChatMessageResponse;
import com.hundred.monitor.commonlibrary.ai.response.SessionInfoResponse;
import com.hundred.monitor.commonlibrary.common.BaseResponse;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * AI聊天控制器 - 处理AI对话相关请求（侧边栏AI助手 - HTTP REST API）
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Resource
    private ChatService chatService;

    @Resource
    private JwtUtil jwtUtil;

    /**
     * 从请求头中获取用户ID
     *
     * @param authHeader Authorization头
     * @return 用户ID
     */
    private String getUserIdFromAuth(String authHeader) {
        return jwtUtil.getUserIdFromAuth(authHeader);
    }

    /**
     * 创建新会话
     *
     * @param request 创建会话请求
     * @return 会话ID和标题
     */
    @PostMapping("/sessions")
    public Mono<BaseResponse<SessionInfoResponse>> createSession(
            @RequestBody @Valid CreateSessionRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = getUserIdFromAuth(authHeader);

        return chatService.createSession(userId, request.getFirstMessage())
                .flatMap(sessionId -> {
                    // 如果有agentId，关联主机
                    if (request.getAgentId() != null && !request.getAgentId().isEmpty()) {
                        return chatService.linkAgent(sessionId, request.getAgentId())
                                .thenReturn(sessionId);
                    }
                    return Mono.just(sessionId);
                })
                .flatMap(sessionId -> chatService.getSession(sessionId)
                        .map(this::toSessionInfoResponse))
                .map(BaseResponse::success);
    }

    /**
     * 获取用户的所有会话列表
     *
     * @return 会话列表
     */
    @GetMapping("/sessions")
    public Mono<BaseResponse<List<SessionInfoResponse>>> getSessions(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = getUserIdFromAuth(authHeader);

        return chatService.getUserSessions(userId)
                .map(sessions -> sessions.stream()
                        .map(this::toSessionInfoResponse)
                        .toList())
                .map(BaseResponse::success);
    }

    /**
     * 获取会话详情
     *
     * @param sessionId 会话ID
     * @return 会话信息
     */
    @GetMapping("/sessions/{sessionId}")
    public Mono<BaseResponse<SessionInfoResponse>> getSession(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return chatService.getSession(sessionId)
                .map(this::toSessionInfoResponse)
                .map(BaseResponse::success);
    }

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Mono<BaseResponse<Void>> deleteSession(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = getUserIdFromAuth(authHeader);

        return chatService.deleteSession(userId, sessionId)
                .thenReturn(BaseResponse.success());
    }

    /**
     * 获取会话的消息历史
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public Mono<BaseResponse<List<ChatMessageResponse>>> getMessages(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return chatService.getMessagesAsList(sessionId)
                .map(messages -> messages.stream()
                        .map(this::toChatMessageResponse)
                        .toList())
                .map(BaseResponse::success);
    }

    /**
     * 发送消息并获取AI回复（SSE流式输出）
     *
     * @param request 发送消息请求
     * @return AI回复流
     */
    @PostMapping(value = "/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sendMessage(
            @RequestBody @Valid SendMessageRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = getUserIdFromAuth(authHeader);
        log.info("发送消息: sessionId={}, userId={}", request.getSessionId(), userId);

        return chatService.sendMessageStreamWithSave(
                        request.getSessionId(),
                        userId,
                        request.getMessage(),
                        request.getModelName())
                .doOnError(e -> log.error("发送消息失败: sessionId={}", request.getSessionId(), e))
                .onErrorResume(e -> Flux.just(ErrorConstants.AI_SERVICE_UNAVAILABLE));
    }

    /**
     * 清空会话消息
     *
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/sessions/{sessionId}/messages")
    public Mono<BaseResponse<Void>> clearMessages(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return chatService.clearMessages(sessionId)
                .thenReturn(BaseResponse.success());
    }

    /**
     * 关联主机到会话
     *
     * @param sessionId 会话ID
     * @param agentId   主机ID
     * @return 操作结果
     */
    @PostMapping("/sessions/{sessionId}/link")
    public Mono<BaseResponse<Void>> linkAgent(
            @PathVariable String sessionId,
            @RequestParam String agentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return chatService.linkAgent(sessionId, agentId)
                .thenReturn(BaseResponse.success());
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
