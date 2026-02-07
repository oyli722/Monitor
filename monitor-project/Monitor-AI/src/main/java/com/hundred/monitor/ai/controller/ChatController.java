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
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
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
//        if (authHeader != null && authHeader.startsWith("Bearer ")) {
//            String token = authHeader.substring(7);
//            if (jwtConfig.validateToken(token)) {
//                return jwtConfig.getUserIdFromToken(token);
//            }
//        }
        return"default-user";
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
                        .map(sessionInfo -> CreateSessionResponse.builder()
                                .sessionId(sessionInfo.getSessionId())
                                .title(sessionInfo.getTitle())
                                .build()))
                .map(BaseResponse::success)
                .doOnError(e -> log.error("创建会话失败: userId={}, firstMessage={}", userId, request.getFirstMessage(), e))
                .onErrorReturn(BaseResponse.error("创建会话失败"));
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
                .map(BaseResponse::success)
                .doOnError(e -> log.error("获取用户会话列表失败: userId={}", userId, e))
                .onErrorReturn(BaseResponse.error("获取会话列表失败"));
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
                .map(BaseResponse::success)
                .switchIfEmpty(Mono.just(BaseResponse.notFound("会话不存在")))
                .doOnError(e -> log.error("获取会话详情失败: sessionId={}", sessionId, e))
                .onErrorReturn(BaseResponse.error("获取会话详情失败"));
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
        BaseResponse<Void> successResponse = BaseResponse.success();
        BaseResponse<Void> errorResponse = errorResponse("删除会话失败");

        return chatService.deleteSession(userId, sessionId)
                .thenReturn(successResponse)
                .doOnError(e -> log.error("删除会话失败: userId={}, sessionId={}", userId, sessionId, e))
                .onErrorResume(e -> Mono.just(errorResponse));
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
                .map(BaseResponse::success)
                .doOnError(e -> log.error("获取消息历史失败: sessionId={}", sessionId, e))
                .onErrorReturn(BaseResponse.error("获取消息历史失败"));
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
        log.info("接收到用户发送的消息...");

        // 用于收集完整回复
        StringBuilder fullResponse = new StringBuilder();

        return chatService.sendMessage(request.getSessionId(), userId, request.getMessage(), request.getModelName())
                .doOnNext(chunk -> {
                    // 收集完整回复
                    fullResponse.append(chunk);
                    log.debug("[CTRL-STREAM] 收到chunk: '{}', 长度={}", chunk, chunk.length());
                })
                .doOnComplete(() -> {
                    // 保存完整的AI回复到Redis（异步执行）
                    String sessionId = request.getSessionId();
                    String content = fullResponse.toString();
                    log.info("[DEBUG-CTRL-SAVE] 保存AI回复: sessionId={}, length={}", sessionId, content.length());

                    com.hundred.monitor.commonlibrary.ai.model.ChatMessage aiMsg =
                            com.hundred.monitor.commonlibrary.ai.model.ChatMessage.builder()
                                    .role("assistant")
                                    .content(content)
                                    .timestamp(java.time.Instant.now().toEpochMilli())
                                    .build();

                    chatService.getRedisUtils().addMessage(sessionId, aiMsg)
                            .doOnSuccess(count -> log.info("[DEBUG-CTRL-SAVE] AI消息保存成功: count={}", count))
                            .doOnError(e -> log.error("[DEBUG-CTRL-SAVE] AI消息保存失败", e))
                            .subscribe();
                })
                // 不添加 SSE 格式，直接发送原始 chunk
                .doOnSubscribe(subscription -> log.info("[DEBUG-CTRL-1] Flux被订阅: sessionId={}", request.getSessionId()))
                .doOnError(e -> log.error("发送消息失败: sessionId={}, userId={}, message={}",
                        request.getSessionId(), userId, request.getMessage(), e))
                .onErrorResume(e -> Flux.just("[ERROR] 发送消息失败"));
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
        BaseResponse<Void> successResponse = BaseResponse.success();
        BaseResponse<Void> errorResponse = errorResponse("清空消息失败");

        return chatService.clearMessages(sessionId)
                .thenReturn(successResponse)
                .doOnError(e -> log.error("清空消息失败: sessionId={}", sessionId, e))
                .onErrorResume(e -> Mono.just(errorResponse));
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
        BaseResponse<Void> successResponse = BaseResponse.success();
        BaseResponse<Void> errorResponse = errorResponse("关联主机失败");

        return chatService.linkAgent(sessionId, agentId)
                .thenReturn(successResponse)
                .doOnError(e -> log.error("关联主机失败: sessionId={}, agentId={}", sessionId, agentId, e))
                .onErrorResume(e -> Mono.just(errorResponse));
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

    /**
     * 创建错误响应（Void类型）
     */
    private BaseResponse<Void> errorResponse(String message) {
        return BaseResponse.error(BaseResponse.INTERNAL_SERVER_ERROR_CODE, message);
    }
}