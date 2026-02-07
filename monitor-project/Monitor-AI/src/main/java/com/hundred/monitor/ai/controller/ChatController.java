package com.hundred.monitor.ai.controller;

import com.hundred.monitor.ai.config.JwtConfig;
import com.hundred.monitor.ai.service.ChatService;
import com.hundred.monitor.commonlibrary.ai.model.ChatMessage;
import com.hundred.monitor.commonlibrary.ai.model.ChatSessionInfo;
import com.hundred.monitor.commonlibrary.ai.request.CreateSessionRequest;
import com.hundred.monitor.commonlibrary.ai.request.SendMessageRequest;
import com.hundred.monitor.commonlibrary.ai.response.ChatMessageResponse;
import com.hundred.monitor.commonlibrary.ai.response.ChatResponse;
import com.hundred.monitor.commonlibrary.ai.response.CreateSessionResponse;
import com.hundred.monitor.commonlibrary.ai.response.SessionInfoResponse;
import com.hundred.monitor.commonlibrary.common.BaseResponse;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

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
    private JwtConfig jwtConfig;

    /**
     * 从请求头中获取用户ID
     *
     * @param authHeader Authorization头
     * @return 用户ID
     */
    private String getUserIdFromAuth(String authHeader) {
        // TODO: 完善JWT解析逻辑
        // 当前简单实现：如果提供了Token则解析，否则使用默认用户
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtConfig.validateToken(token)) {
                return jwtConfig.getUserIdFromToken(token);
            }
        }
        return"default-user";
    }

    private Mono<String> getUserIdReactive(String authHeader) {
        return Mono.fromCallable(() -> getUserIdFromAuth(authHeader));
    }

    /**
     * 创建新会话
     *
     * @param request 创建会话请求
     * @return 会话ID和标题
     */
    @PostMapping("/sessions")
    public Mono<BaseResponse<CreateSessionResponse>> createSession(
            @RequestBody @Valid CreateSessionRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return Mono.fromCallable(() -> getUserIdFromAuth(authHeader))
                .flatMap(userId -> chatService.getUserSessionsReactive(userId))  // 需要响应式版本
                .map(sessions -> sessions.stream()
                        .map(this::toSessionInfoResponse)
                        .collect(Collectors.toList()))
                .map(BaseResponse::success)
                .onErrorResume(e -> {
                    log.error("获取会话列表失败", e);
                    return Mono.just(BaseResponse.error("获取会话列表失败: " + e.getMessage()));
                });
    }

    /**
     * 获取用户的所有会话列表
     *
     * @return 会话列表
     */
    @GetMapping("/sessions")
    public Mono<BaseResponse<List<SessionInfoResponse>>> getSessions(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return Mono.fromCallable(() -> getUserIdFromAuth(authHeader))
                .flatMap(userId -> chatService.getUserSessionsReactive(userId))  // 需要响应式版本
                .map(sessions -> sessions.stream()
                        .map(this::toSessionInfoResponse)
                        .collect(Collectors.toList()))
                .map(BaseResponse::success)
                .onErrorResume(e -> {
                    log.error("获取会话列表失败", e);
                    return Mono.just(BaseResponse.error("获取会话列表失败: " + e.getMessage()));
                });
    }
    /**
     * 获取会话详情
     *
     * @param sessionId 会话ID
     * @return 会话信息
     */
    @GetMapping("/sessions/{sessionId}")
    public BaseResponse<SessionInfoResponse> getSession(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
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
    public BaseResponse<Void> deleteSession(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String userId = getUserIdFromAuth(authHeader);

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
    public BaseResponse<List<ChatMessageResponse>> getMessages(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
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
    public Flux<BaseResponse<ChatResponse>> sendMessage(
            @RequestBody @Valid SendMessageRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String userId = getUserIdFromAuth(authHeader);

        return chatService.sendMessage(
                        request.getSessionId(),
                        userId,
                        request.getMessage(),
                        request.getModelName()
                ).map(
                        reply -> {
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
                        }
                )
                .onErrorResume(throwable -> {
                    // 1. 记录日志
                    log.error("发送消息失败: sessionId={}, userId={}",
                            request.getSessionId(), userId, throwable);

                    // 2. 返回错误响应流
                    ChatResponse errorResponse = ChatResponse.builder()
                            .sessionId(request.getSessionId())
                            .reply("服务暂时不可用，请稍后重试")
                            .build();

                    return Flux.just(BaseResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "消息发送失败",
                            errorResponse
                    ));
                });
    }

    ;


    /**
     * 清空会话消息
     *
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/sessions/{sessionId}/messages")
    public BaseResponse<Void> clearMessages(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
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
            @RequestParam String agentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
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