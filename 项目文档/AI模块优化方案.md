# Monitor-AI模块代码优化方案

## 一、优化概述

### 1.1 优化背景

Monitor-AI模块已完成响应式重构，但代码存在以下问题需要优化：
- 异常处理不统一
- 日志混乱（DEBUG日志残留）
- Controller直接暴露内部组件
- 错误响应硬编码
- JWT解析逻辑未完成
- 魔法数字硬编码

### 1.2 优化目标

```
Monitor-AI优化目标
├── 异常管理统一化
│   ├── 自定义异常体系
│   ├── 全局异常处理器
│   └── 错误码枚举
├── 代码规范化
│   ├── 清理残留日志
│   ├── 提取常量
│   └── 移除不当暴露
└── 功能完善
    ├── JWT解析逻辑
    └── 参数校验增强
```

---

## 二、文件结构变更

### 2.1 新增文件

```
Monitor-AI/src/main/java/com/hundred/monitor/ai/
├── exception/                                    ✅ 新增包
│   ├── BaseException.java                        自定义异常基类
│   ├── BusinessException.java                    业务异常
│   ├── ChatSessionNotFoundException.java      会话不存在异常
│   └── AiServiceException.java                   AI服务异常
├── constant/                                     ✅ 新增包
│   ├── ChatConstants.java                        聊天相关常量
│   └── ErrorConstants.java                       错误消息常量
├── handler/                                      ✅ 新增包
│   └── GlobalExceptionHandler.java               全局异常处理器
├── util/                                         ✅ 新增包
│   └── JwtUtil.java                              JWT工具类
└── aspect/                                       ✅ 新增包
    └── UserIdAspect.java                         用户ID切面
```

### 2.2 修改文件

| 文件 | 修改类型 | 说明 |
|------|----------|------|
| `ChatController.java` | 重构 | 移除getRedisUtils()，完善JWT，统一异常处理 |
| `ChatService.java` | 重构 | 清理DEBUG日志，提取常量，优化异常 |
| `ChatSessionRedisReactiveUtils.java` | 优化 | 统一异常处理 |
| `JwtConfig.java` | 完善 | 添加JWT工具方法 |

---

## 三、详细优化内容

### 3.1 统一异常管理

#### 3.1.1 自定义异常体系

**创建 `BaseException.java`**：

```java
package com.hundred.monitor.ai.exception;

import com.hundred.monitor.commonlibrary.common.BaseResponse;
import lombok.Getter;

/**
 * 自定义异常基类
 */
@Getter
public class BaseException extends RuntimeException {

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    public BaseException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BaseException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    /**
     * 转换为BaseResponse
     */
    public <T> BaseResponse<T> toResponse() {
        return BaseResponse.error(code, message);
    }
}
```

**创建 `BusinessException.java`**：

```java
package com.hundred.monitor.ai.exception;

/**
 * 业务异常
 */
public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super(BaseResponse.INTERNAL_SERVER_ERROR_CODE, message);
    }

    public BusinessException(String message, Throwable cause) {
        super(BaseResponse.INTERNAL_SERVER_ERROR_CODE, message, cause);
    }

    public BusinessException(Integer code, String message) {
        super(code, message);
    }
}
```

**创建 `ChatSessionNotFoundException.java`**：

```java
package com.hundred.monitor.ai.exception;

/**
 * 聊天会话不存在异常
 */
public class ChatSessionNotFoundException extends BaseException {

    public ChatSessionNotFoundException(String sessionId) {
        super(BaseResponse.NOT_FOUND_CODE, "会话不存在: " + sessionId);
    }
}
```

**创建 `AiServiceException.java`**：

```java
package com.hundred.monitor.ai.exception;

/**
 * AI服务异常
 */
public class AiServiceException extends BaseException {

    public AiServiceException(String message) {
        super(BaseResponse.INTERNAL_SERVER_ERROR_CODE, "AI服务异常: " + message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(BaseResponse.INTERNAL_SERVER_ERROR_CODE, message, cause);
    }
}
```

#### 3.1.2 全局异常处理器

**创建 `GlobalExceptionHandler.java`**：

```java
package com.hundred.monitor.ai.handler;

import com.hundred.monitor.ai.exception.*;
import com.hundred.monitor.ai.exception.BaseException;
import com.hundred.monitor.commonlibrary.common.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Mono<BaseResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Mono.just(e.toResponse());
    }

    /**
     * 会话不存在异常
     */
    @ExceptionHandler(ChatSessionNotFoundException.class)
    public Mono<BaseResponse<Void>> handleChatSessionNotFoundException(ChatSessionNotFoundException e) {
        log.warn("会话不存在: {}", e.getMessage());
        return Mono.just(e.toResponse());
    }

    /**
     * AI服务异常
     */
    @ExceptionHandler(AiServiceException.class)
    public Mono<BaseResponse<Void>> handleAiServiceException(AiServiceException e) {
        log.error("AI服务异常", e);
        return Mono.just(e.toResponse());
    }

    /**
     * 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<BaseResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return Mono.just(BaseResponse.badRequest(e.getMessage()));
    }

    /**
     * 通用异常
     */
    @ExceptionHandler(Exception.class)
    public Mono<BaseResponse<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return Mono.just(BaseResponse.error("系统异常，请稍后重试"));
    }
}
```

### 3.2 常量提取

**创建 `ChatConstants.java`**：

```java
package com.hundred.monitor.ai.constant;

/**
 * 聊天相关常量
 */
public class ChatConstants {

    /**
     * 消息数量阈值（超过此数量触发总结）
     */
    public static final int MESSAGE_THRESHOLD = 20;

    /**
     * 最近消息数量
     */
    public static final int RECENT_MESSAGE_COUNT = 10;

    /**
     * 会话标题最大长度
     */
    public static final int SESSION_TITLE_MAX_LENGTH = 20;

    /**
     * 会话标题后缀
     */
    public static final String SESSION_TITLE_SUFFIX = "...";

    /**
     * 默认用户ID
     */
    public static final String DEFAULT_USER_ID = "default-user";

    /**
     * Bearer Token前缀
     */
    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * 会话TTL天数
     */
    public static final long SESSION_TTL_DAYS = 30;

    /**
     * 总结提示词
     */
    public static final String SUMMARY_PROMPT = """
            请将以下对话内容总结为一段简洁的摘要，保留关键信息（用户意图、重要操作、结论）：
            """;
}
```

**创建 `ErrorConstants.java`**：

```java
package com.hundred.monitor.ai.constant;

/**
 * 错误消息常量
 */
public class ErrorConstants {

    /**
     * 会话相关
     */
    public static final String SESSION_NOT_FOUND = "会话不存在";
    public static final String SESSION_CREATE_FAILED = "创建会话失败";
    public static final String SESSION_DELETE_FAILED = "删除会话失败";
    public static final String SESSION_LIST_FAILED = "获取会话列表失败";
    public static final String SESSION_GET_FAILED = "获取会话详情失败";

    /**
     * 消息相关
     */
    public static final String MESSAGE_SEND_FAILED = "发送消息失败";
    public static final String MESSAGE_GET_FAILED = "获取消息历史失败";
    public static final String MESSAGE_CLEAR_FAILED = "清空消息失败";

    /**
     * 主机相关
     */
    public static final String AGENT_LINK_FAILED = "关联主机失败";
    public static final String AGENT_NOT_FOUND = "主机不存在";

    /**
     * AI服务相关
     */
    public static final String AI_SERVICE_UNAVAILABLE = "AI服务暂时不可用，请稍后再试";
    public static final String AI_MODEL_ERROR = "AI模型调用失败";

    /**
     * 认证相关
     */
    public static final String UNAUTHORIZED = "未授权访问";
    public static final String TOKEN_INVALID = "Token无效或已过期";
}
```

### 3.3 日志清理

#### 3.3.1 ChatService.java 清理

**清理前**：
```java
log.info("[DEBUG-1] sendMessage开始: sessionId={}, userId={}, message={}", sessionId, userId, userMessage);
log.info("[DEBUG-2] 会话存在检查: exists={}", exists);
log.info("[DEBUG-3] 开始构建用户消息");
log.info("[DEBUG-4] 开始构建上下文: sessionId={}", sessionId);
log.info("[DEBUG-5] 上下文构建完成, 消息数={}", contextMessages.size());
log.info("[DEBUG-6] 开始调用AI: modelName={}", modelName);
log.debug("[DEBUG-STREAM] 发送chunk: {}", chunk.substring(0, Math.min(20, chunk.length())));
// ... 大量DEBUG日志
```

**清理后**：
```java
// 只保留关键业务日志
log.info("发送消息: sessionId={}, userId={}, messageLength={}", sessionId, userId, userMessage.length());
log.debug("会话存在检查: exists={}", exists);
log.info("AI调用完成: sessionId={}, modelName={}", sessionId, modelName);
// 异常日志保留
log.error("发送消息失败: sessionId={}, userId={}", sessionId, userId, e);
```

#### 3.3.2 ChatController.java 清理

**清理前**：
```java
log.info("[DEBUG-CTRL-1] Flux被订阅: sessionId={}", request.getSessionId());
log.info("[DEBUG-CTRL-SAVE] 保存AI回复: sessionId={}, length={}", sessionId, content.length());
log.debug("[CTRL-STREAM] 收到chunk: '{}', 长度={}", chunk, chunk.length());
```

**清理后**：
```java
log.info("发送消息: sessionId={}, userId={}", sessionId, userId);
log.debug("保存AI回复: sessionId={}, length={}", sessionId, content.length());
```

### 3.4 移除组件暴露

#### 3.4.1 移除 ChatService.getRedisUtils()

**清理前**（ChatController.java:37-38, 200）：
```java
// ChatController.java
@Resource
private ChatService chatService;

// ChatService.java
public ChatSessionRedisReactiveUtils getRedisUtils() {
    return redisUtils;
}

// ChatController.java:200
chatService.getRedisUtils().addMessage(sessionId, aiMsg)
```

**优化后**：
```java
// ChatService.java - 移除getter
// 删除 getRedisUtils() 方法

// ChatController.java - 使用Service封装
@PostMapping(value = "/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> sendMessage(...) {
    return chatService.sendMessageStreamWithSave(
            request.getSessionId(),
            userId,
            request.getMessage(),
            request.getModelName())
        .doOnError(e -> log.error("发送消息失败: sessionId={}", request.getSessionId(), e))
        .onErrorResume(e -> Flux.just(ErrorConstants.AI_SERVICE_UNAVAILABLE));
}
```

**ChatService.java 新增方法**：
```java
/**
 * 发送消息并自动保存（封装版本）
 */
public Flux<String> sendMessageStreamWithSave(String sessionId, String userId, String userMessage, String modelName) {
    StringBuilder fullResponse = new StringBuilder();
    return sendMessage(sessionId, userId, userMessage, modelName)
        .doOnComplete(() -> {
            ChatMessage aiMsg = ChatMessage.builder()
                .role("assistant")
                .content(fullResponse.toString())
                .timestamp(Instant.now().toEpochMilli())
                .build();
            redisUtils.addMessage(sessionId, aiMsg)
                .doOnSuccess(count -> log.debug("AI消息保存成功: sessionId={}, count={}", sessionId, count))
                .doOnError(e -> log.error("AI消息保存失败: sessionId={}", sessionId, e))
                .subscribe();
        })
        .doOnNext(fullResponse::append);
}
```

### 3.5 完善JWT解析逻辑

#### 3.5.1 创建JWT工具类

**创建 `JwtUtil.java`**：

```java
package com.hundred.monitor.ai.util;

import com.hundred.monitor.ai.constant.ChatConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 生成Token
     */
    public String generateToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
            .subject(userId)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * 从Token中获取用户ID
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            log.warn("解析Token失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证Token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从Authorization头中提取Token
     */
    public String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(ChatConstants.BEARER_PREFIX)) {
            return authHeader.substring(ChatConstants.BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 从Authorization头中获取用户ID
     */
    public String getUserIdFromAuth(String authHeader) {
        String token = extractToken(authHeader);
        if (token != null && validateToken(token)) {
            return getUserIdFromToken(token);
        }
        return ChatConstants.DEFAULT_USER_ID;
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

#### 3.5.2 更新 ChatController

**清理前**（ChatController.java:46-56）：
```java
private String getUserIdFromAuth(String authHeader) {
    // TODO: 完善JWT解析逻辑
    // 当前简单实现：如果提供了Token则解析，否则使用默认用户
//    if (authHeader != null && authHeader.startsWith("Bearer ")) {
//        String token = authHeader.substring(7);
//        if (jwtConfig.validateToken(token)) {
//            return jwtConfig.getUserIdFromToken(token);
//        }
//    }
    return "default-user";
}
```

**优化后**：
```java
@Resource
private JwtUtil jwtUtil;

private String getUserIdFromAuth(String authHeader) {
    return jwtUtil.getUserIdFromAuth(authHeader);
}
```

### 3.6 统一异常处理

#### 3.6.1 ChatService 优化

**清理前**：
```java
public Flux<String> sendMessage(...) {
    return redisUtils.sessionExists(sessionId)
        .flatMapMany(exists -> {
            if (!exists) {
                return Mono.error(new IllegalArgumentException("会话不存在: " + sessionId));
            }
            // ...
        })
        .doOnError(e -> log.error("发送消息失败: ...", e));
}
```

**优化后**：
```java
public Flux<String> sendMessage(...) {
    return redisUtils.sessionExists(sessionId)
        .flatMap(exists -> {
            if (Boolean.FALSE.equals(exists)) {
                return Mono.error(new ChatSessionNotFoundException(sessionId));
            }
            return Mono.just(sessionId);
        })
        .flatMapMany(...);
}
```

#### 3.6.2 ChatController 优化

**清理前**：
```java
@PostMapping("/sessions")
public Mono<BaseResponse<CreateSessionResponse>> createSession(...) {
    return chatService.createSession(userId, request.getFirstMessage())
        .flatMap(...)
        .map(BaseResponse::success)
        .doOnError(e -> log.error("创建会话失败: ...", e))
        .onErrorReturn(BaseResponse.error("创建会话失败"));
}
```

**优化后**：
```java
@PostMapping("/sessions")
public Mono<BaseResponse<CreateSessionResponse>> createSession(
        @RequestBody @Valid CreateSessionRequest request,
        @RequestHeader(value = "Authorization", required = false) String authHeader) {
    String userId = getUserIdFromAuth(authHeader);

    return chatService.createSession(userId, request.getFirstMessage())
        .flatMap(sessionId -> {
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
        .map(BaseResponse::success);
}
```

### 3.7 配置优化

#### 3.7.1 移除未使用的Feign配置

**删除 `FeignConfig.java`**：
- 用户澄清：AI与终端模块无相互调用需求
- OpenFeign依赖可从pom.xml中移除

#### 3.7.2 清理AIModelConfig中的TODO注释

**清理前**：
```java
.streamingChatLanguageModel(defaultOpenAiChatModel)
// TODO 调用工具
.build();
```

**清理后**：
```java
.streamingChatLanguageModel(defaultOpenAiChatModel)
.build();
```

---

## 四、优化后的代码结构

### 4.1 文件清单

```
Monitor-AI/src/main/java/com/hundred/monitor/ai/
├── MonitorAiApplication.java
├── config/
│   ├── AIModelConfig.java                   ✅ 清理TODO
│   ├── JwtConfig.java                       ✅ 保留（可能废弃）
│   ├── RedisConfig.java                     ✅ 无变更
│   └── CorsConfig.java                      ✅ 无变更
├── constant/                                ✅ 新增
│   ├── ChatConstants.java                   聊天常量
│   └── ErrorConstants.java                  错误消息常量
├── controller/
│   └── ChatController.java                  ✅ 重构：移除getter，完善JWT
├── exception/                               ✅ 新增
│   ├── BaseException.java                   异常基类
│   ├── BusinessException.java               业务异常
│   ├── ChatSessionNotFoundException.java   会话异常
│   └── AiServiceException.java              AI服务异常
├── handler/                                 ✅ 新增
│   └── GlobalExceptionHandler.java          全局异常处理
├── model/
│   └── ChatAssistant.java                   ✅ 无变更
├── service/
│   └── ChatService.java                     ✅ 重构：清理日志，提取常量
├── util/                                    ✅ 新增
│   └── JwtUtil.java                         JWT工具类
└── utils/
    └── ChatSessionRedisReactiveUtils.java   ✅ 优化：统一异常处理
```

### 4.2 异常处理流程

```
请求 → ChatController
       ↓
   业务逻辑
       ↓
   抛出异常
       ↓
GlobalExceptionHandler
       ↓
   BaseResponse<T>
       ↓
   返回给前端
```

---

## 五、验证清单

### 5.1 编译验证

| 验证项 | 命令 | 预期结果 |
|--------|------|----------|
| 编译Monitor-AI | `mvn clean compile` | 成功 |
| 运行单元测试 | `mvn test` | 全部通过 |
| 打包 | `mvn package` | 成功 |

### 5.2 功能验证

| 场景 | 操作 | 预期结果 |
|------|------|----------|
| 创建会话 | POST /api/chat/sessions | 成功返回sessionId |
| 发送消息 | POST /api/chat/messages | SSE流式响应 |
| 会话不存在 | GET /api/chat/sessions/invalid | 返回404错误 |
| JWT解析 | 携带有效Token | 正确解析userId |
| JWT解析 | 携带无效Token | 使用default-user |

### 5.3 日志验证

| 验证项 | 说明 |
|--------|------|
| DEBUG日志清理 | 不再出现`[DEBUG-xxx]`日志 |
| 异常日志统一 | 所有异常通过GlobalExceptionHandler处理 |
| 业务日志保留 | 关键业务操作日志正常输出 |

---

## 六、实施步骤

### 阶段1：创建基础设施（无风险）

1. 创建 `constant/ChatConstants.java`
2. 创建 `constant/ErrorConstants.java`
3. 创建 `exception/` 包下的所有异常类
4. 创建 `util/JwtUtil.java`

### 阶段2：全局异常处理（低风险）

1. 创建 `handler/GlobalExceptionHandler.java`
2. 编写异常处理器单元测试

### 阶段3：代码重构（中风险）

1. 重构 `ChatService.java`（清理日志，提取常量）
2. 重构 `ChatController.java`（移除getter，完善JWT）
3. 更新 `ChatSessionRedisReactiveUtils.java`（统一异常）

### 阶段4：清理工作（低风险）

1. 清理 `AIModelConfig.java` 中的TODO注释
2. 移除未使用的 `FeignConfig.java`
3. 更新相关文档

### 阶段5：测试验证（必须）

1. 单元测试验证
2. 集成测试验证
3. 功能测试验证

---

## 七、风险评估

| 风险 | 等级 | 缓解措施 |
|------|------|----------|
| 异常处理变更导致兼容性问题 | 中 | 保留原有错误码格式 |
| JWT解析变更导致认证失败 | 低 | 保留default-user兜底逻辑 |
| 日志清理导致问题排查困难 | 低 | 保留关键业务日志和异常日志 |
| 常量提取引入拼写错误 | 低 | 编译期检查 |

---

## 八、回滚方案

如果优化后出现问题，可通过Git快速回滚：

```bash
# 查看提交历史
git log --oneline

# 回滚到优化前的提交
git revert <commit-hash>

# 或者直接回滚
git reset --hard <commit-hash>
```

---

## 九、执行记录

### 9.1 执行状态

| 阶段 | 状态 | 说明 |
|------|------|------|
| 阶段1：创建基础设施 | ✅ 完成 | 7个文件创建成功 |
| 阶段2：全局异常处理器 | ✅ 完成 | GlobalExceptionHandler创建 |
| 阶段3：代码重构 | ✅ 完成 | ChatService、ChatController重构 |
| 阶段4：清理工作 | ✅ 完成 | TODO注释、FeignConfig删除 |
| 额外修复：注入方式问题 | ✅ 完成 | @Resource改为@Autowired |
| 额外修复：blockLast阻塞 | ✅ 完成 | generateSummary完全异步化 |

### 9.2 额外修复记录

#### 修复1：注入方式问题（2026-02-08）

**问题**：ChatSessionRedisReactiveUtils使用@Resource注入ObjectMapper导致Redis序列化异常

**原因**：@Resource与@Autowired在注入ObjectMapper时行为不同，导致序列化配置不一致

**解决方案**：将@Resource改为@Autowired

**修改文件**：`ChatSessionRedisReactiveUtils.java`

#### 修复2：blockLast()阻塞问题（2026-02-08）

**问题**：generateSummary方法中使用blockLast()阻塞EventLoop线程

**影响**：占用EventLoop线程，影响并发性能

**解决方案**：改为完全异步实现
```java
// 修复前
return Mono.fromCallable(() -> {
    assistant.chat(messagesJson)
        .doOnNext(summaryBuilder::append)
        .blockLast(); // ❌ 阻塞
    return summaryBuilder.toString();
});

// 修复后
return assistant.chat(messagesJson)
    .collectList()                              // ✅ 异步收集
    .map(chunks -> String.join("", chunks))      // ✅ 异步拼接
    .map(summary -> summary.isEmpty() ? "对话总结生成失败" : summary);
```

**修改文件**：`ChatService.java`

### 9.3 待优化项（未执行）

| 优先级 | 问题 | 说明 |
|--------|------|------|
| 高 | AI模型重复创建 | AIModelConfig中重复创建OpenAiStreamingChatModel |
| 中 | 异常处理不一致 | try-catch与响应式异常处理混用 |
| 中 | 注入方式不一致 | 代码中@Resource和@Autowired混用 |
| 低 | 未使用导入 | GlobalExceptionHandler中未使用导入 |
| 低 | 代码风格 | 无意义的空操作、冗余注释 |

---

*文档版本：V1.1（已完成版）*
*创建日期：2026年2月8日*
*最后更新：2026年2月8日*
