# AI助手终端集成开发记录

## 一、项目概述

### 1.1 功能定位

Monitor监控系统实现了**两种独立AI助手场景**，满足不同使用需求：

| 特性 | 侧边栏AI助手 | 主机详情页AI助手 |
|------|-------------|-----------------|
| **位置** | 全局侧边栏 (`/ai`) | 主机详情页SSH终端旁 |
| **服务模块** | Monitor-AI (8081) | Monitor-Server (8080) |
| **通信方式** | HTTP REST API + SSE | WebSocket长连接 |
| **会话类型** | 多会话管理 | 单会话（与SSH绑定） |
| **主机关联** | 可选 | 必需 |
| **SSH命令执行** | 不支持 | 支持（工具调用） |
| **编程模型** | **响应式（WebFlux）** | 传统阻塞式 |
| **开发状态** | ✅ 已完成（V3.0） | ✅ 已完成（V2.2） |

### 1.2 技术栈

**后端**：
- Spring Boot 3.5.10
- LangChain4j 1.0.0-beta3（GLM-4.7、Ollama）
- Spring WebFlux（Monitor-AI响应式）
- Redis 6.0+（会话存储）

**前端**：
- Vue 3.5.26 + TypeScript
- Element Plus 2.13.1
- SSE（Server-Sent Events）流式接收
- WebSocket（SSH绑定AI）

---

## 二、架构演进

### 2.1 V1.0 初始版本（已废弃）

单模块设计，所有AI功能集中在Monitor-Server中，存在职责不清晰、扩展性差等问题。

### 2.2 V2.0 场景分离

将AI助手分离为两个独立场景：
- **场景A**：全局侧边栏AI助手（HTTP REST API）
- **场景B**：主机详情页AI助手（WebSocket + SSH绑定）

### 2.3 V3.0 模块化重构（当前版本）

将侧边栏AI助手抽离为独立的Monitor-AI微服务，实现**响应式编程**重构。

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              Monitor-Web (Vue 3)                        │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  SidebarAssistant.vue (侧边栏)    AiAssistantDialog.vue (SSH)    │   │
│  │  HTTP REST + SSE                   WebSocket                     │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────────────────────┘
                         │                    │
                         ▼                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  Monitor-AI (8081)                Monitor-Server (8080)                │
│  ┌──────────────────┐            ┌──────────────────────┐             │
│  │ ChatController   │            │ AiSshAssistantController│           │
│  │ (响应式)         │            │ (传统)               │             │
│  └──────────────────┘            └──────────────────────┘             │
│  ┌──────────────────┐            ┌──────────────────────┐             │
│  │ ChatService      │            │ AiSshAssistantService│             │
│  │ (Mono/Flux)      │            │ (阻塞式)             │             │
│  └──────────────────┘            └──────────────────────┘             │
│  ┌──────────────────┐                                           │    │
│  │ ReactiveRedis    │                                           │    │
│  └──────────────────┘            ┌──────────────────────┐             │
│                                  │ SshExecuteTool       │             │
│                                  │ (工具调用)            │             │
│                                  └──────────────────────┘             │
└─────────────────────────────────────────────────────────────────────────┘
                         │                    │
                         ▼                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     CommonLibrary (共享实体)                             │
│  ai/model/ChatMessage, SystemPrompt, ChatSessionInfo                    │
│  ai/request/CreateSessionRequest, SendMessageRequest                    │
│  ai/response/ChatResponse, SessionInfoResponse                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.4 服务间通信

**Monitor-AI → Monitor-Server**（已取消）：
- 原计划通过Feign调用Agent API
- 用户澄清：AI与终端模块无相互调用需求
- OpenFeign依赖已移除

**前端 → Monitor-AI**（侧边栏AI）：
```
POST   /api/chat/sessions                     创建会话
GET    /api/chat/sessions                     获取会话列表
GET    /api/chat/sessions/{id}                获取会话详情
DELETE /api/chat/sessions/{id}                删除会话
GET    /api/chat/sessions/{id}/messages       获取消息历史
POST   /api/chat/messages                     发送消息（SSE流式）
DELETE /api/chat/sessions/{id}/messages       清空消息
POST   /api/chat/sessions/{id}/link           关联主机
```

**前端 → Monitor-Server**（SSH绑定AI）：
```
POST   /api/ai/ssh-assistant/connect          连接AI助手
DELETE /api/ai/ssh-assistant/disconnect/{id}  断开连接
GET    /api/ai/ssh-assistant/binding/{id}     获取绑定信息
WebSocket /ws/ai/ssh-assistant/{id}           WebSocket连接
```

---

## 三、响应式重构（V3.0核心变更）

### 3.1 重构概述

Monitor-AI模块已完成从Spring MVC到Spring WebFlux的响应式重构，实现非阻塞I/O和流式响应。

### 3.2 技术栈变更

| 维度 | 重构前 | 重构后 |
|------|--------|--------|
| Web框架 | Spring Web MVC | Spring WebFlux |
| Redis访问 | `RedisTemplate` | `ReactiveRedisTemplate` |
| 返回类型 | `T`, `List<T>` | `Mono<T>`, `Flux<T>` |
| 异步模型 | 阻塞I/O | 非阻塞I/O |
| 流式响应 | 同步返回 | SSE流式输出 |

### 3.3 响应式代码特征

**Controller层**：
```java
// 单值响应
@PostMapping("/sessions")
public Mono<BaseResponse<CreateSessionResponse>> createSession(...) {
    return chatService.createSession(userId, request.getFirstMessage())
        .flatMap(...)
        .map(BaseResponse::success);
}

// 流式响应（SSE）
@PostMapping(value = "/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> sendMessage(...) {
    return chatService.sendMessage(sessionId, userId, message, modelName)
        .doOnNext(chunk -> fullResponse.append(chunk))
        .doOnComplete(() -> saveToRedis(fullResponse.toString()));
}
```

**Service层**（链式调用）：
```java
public Flux<String> sendMessage(String sessionId, String userId, ...) {
    return redisUtils.sessionExists(sessionId)
        .flatMapMany(exists -> {
            if (!exists) return Mono.error(...);
            return buildUserMessage()
                .thenMany(buildContext(sessionId)
                    .flatMapMany(contextMessages -> callAI(contextMessages)));
        });
}
```

**Utils层**（响应式Redis）：
```java
public Mono<List<ChatSessionInfo>> getUserSessions(String userId) {
    return getUserSessionIds(userId)
        .flatMapMany(Flux::fromIterable)
        .flatMapSequential(this::getSessionInfo)
        .collectList();
}
```

### 3.4 核心组件变更

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `ChatController.java` | 重构 | 返回类型改为`Mono`/`Flux` |
| `ChatService.java` | 重构 | 全面响应式链式调用 |
| `ChatSessionRedisReactiveUtils.java` | 新建 | 替代原`ChatSessionRedisUtils` |
| `AIModelConfig.java` | 重构 | 使用`StreamingChatLanguageModel` |

### 3.5 SSE流式输出

```java
// AI模型配置 - 使用流式模型
@Bean(name = "ollamaAiChatAssistant")
public ChatAssistant ollamaAiChatAssistant() {
    OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
        .modelName("qwen2.5:7b")
        .baseUrl("http://localhost:11434/v1")
        .build();
    return AiServices.builder(ChatAssistant.class)
        .streamingChatLanguageModel(model)
        .build();
}

// Controller返回Flux
@PostMapping(value = "/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> sendMessage(...) {
    return chatService.sendMessage(...)
        .doOnNext(chunk -> log.debug("收到chunk: {}", chunk))
        .doOnComplete(() -> log.info("流式输出完成"));
}
```

---

## 四、核心功能实现

### 4.1 侧边栏AI助手（Monitor-AI）

#### 4.1.1 会话管理

| 功能 | 实现方式 |
|------|----------|
| 创建会话 | UUID生成sessionId，首条消息生成标题 |
| 会话列表 | Redis ZSet按更新时间排序 |
| 会话切换 | 加载对应sessionId的消息历史 |
| 会话删除 | 删除会话、消息、用户索引 |

#### 4.1.2 消息处理

```java
// 智能上下文管理：总结压缩机制
public Flux<String> sendMessage(...) {
    return buildContext(sessionId)  // 系统提示词 + 总结 + 近期消息
        .flatMapMany(messages -> callAI(messages, modelName))
        .doOnComplete(() -> {
            // 检查是否需要总结（消息数>=20）
            redisUtils.needsSummary(sessionId)
                .filter(need -> need)
                .flatMap(need -> performSummary(sessionId))
                .subscribe();
        });
}
```

#### 4.1.3 Redis数据结构

```
# 会话信息（String，JSON）
assistant:chat:info:{sessionId} → {sessionId, title, createdAt, updatedAt, messageCount, summary, linkedAgentId, lastSummaryAt}
TTL: 30天

# 消息列表（List）
assistant:chat:messages:{sessionId} → [Message1, Message2, ...]
TTL: 30天

# 用户会话索引（ZSet，按时间排序）
assistant:user:sessions:{userId} → {sessionId1: score1, sessionId2: score2, ...}
```

### 4.2 SSH绑定AI助手（Monitor-Server）

#### 4.2.1 绑定机制

```
┌─────────────────────────────────────────────────────────────────┐
│  步骤1：SSH连接建立                                             │
│  用户点击[连接终端] → WebSocket: /ws/ssh/terminal/{sshSessionId}│
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  步骤2：AI助手连接                                               │
│  POST /api/ai/ssh-assistant/connect                             │
│  {sshSessionId, agentId}                                        │
│  → 返回 {aiSessionId}                                           │
│  → Redis存储绑定: ai:ssh:binding:{aiSessionId}                  │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  步骤3：WebSocket连接                                            │
│  WebSocket: /ws/ai/ssh-assistant/{aiSessionId}                  │
│  → 从Redis获取绑定关系（sshSessionId, agentId）                 │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  步骤4：AI对话交互                                               │
│  用户消息 → 构建上下文 → AI决定调用工具                          │
│  └─ SshExecuteTool.executeCommand(cmd)                         │
│      ├─ 生成commandId（时间戳标记）                             │
│      ├─ 发送命令到SSH（带## COMMAND_START/END标记）            │
│      ├─ 捕获SSH输出 → 实时推送前端                               │
│      └─ 触发AI分析 → 推送分析结果                               │
└─────────────────────────────────────────────────────────────────┘
```

#### 4.2.2 ThreadLocal上下文传递

```java
// AiSshAssistantService.sendMessage() 中设置
SshSessionContext.setSshSessionId(sshSessionId);
SshSessionContext.setAgentId(agentId);
SshSessionContext.setAiSessionId(aiSessionId);

try {
    // 调用AI模型
    model.chat(context);
} finally {
    SshSessionContext.clear();
}

// SshExecuteTool.executeCommand() 中获取
String sshSessionId = SshSessionContext.getSshSessionId();
String agentId = SshSessionContext.getAgentId();
```

#### 4.2.3 命令执行结果分析

```
命令执行流程：
1. AI决定执行命令 → 生成commandId
2. 注册命令上下文: CommandContext{commandId, aiSessionId, sshSessionId}
3. 发送命令到SSH（带起止标记）：
   ## COMMAND_START: {timestamp} ##
   {command}
   ## COMMAND_END: {timestamp} ##
4. SSH输出返回时 → 通过commandId关联
5. 检测COMMAND_END标记 → 触发AI分析
6. 异步分析结果 → 推送前端 + 保存到Redis
```

#### 4.2.4 超时处理机制

```java
@Scheduled(fixedDelay = 1000)
public void checkTimeouts() {
    commandMap.values().stream()
        .filter(ctx -> ctx.getStatus() == CommandStatus.EXECUTING)
        .filter(ctx -> ctx.isTimeout())
        .forEach(ctx -> {
            ctx.setStatus(CommandStatus.TIMEOUT);
            activeCommands.remove(ctx.getSshSessionId());
            // 仍然触发AI分析（基于已收集的输出）
            triggerAiAnalysis(ctx);
        });
}
```

---

## 五、前端实现

### 5.1 侧边栏AI助手

**文件结构**：
```
src/
├── views/ai/
│   └── SidebarAssistant.vue          ✅ 主页面
├── types/ai.ts                        ✅ 类型定义
├── api/ai.ts                          ✅ API客户端
├── utils/ai-request.ts                ✅ AI服务专用HTTP客户端
└── components/ai/
    ├── ChatMessage.vue                ✅ 消息组件
    ├── ChatInput.vue                  ✅ 输入组件
    └── MarkdownRenderer.vue           ✅ Markdown渲染
```

**核心功能**：
- 会话列表（左侧280px面板）
- 消息发送与接收
- SSE流式消息处理
- 相对时间显示（刚刚、X分钟前）

### 5.2 SSH绑定AI助手

**文件结构**：
```
src/
├── components/ai/
│   ├── AiAssistantDialog.vue         ✅ AI助手对话框（非模态）
│   ├── ChatInterface.vue             ✅ 聊天界面容器
│   ├── ChatMessage.vue               ✅ 消息组件
│   ├── ChatInput.vue                 ✅ 输入组件
│   └── MarkdownRenderer.vue          ✅ Markdown渲染
├── composables/
│   └── useAiChat.ts                  ✅ WebSocket状态管理
└── components/monitor/
    └── SshTerminalDialog.vue         ✅ 添加AI助手按钮
```

**WebSocket消息协议**：

| 类型 | 方向 | 说明 |
|------|------|------|
| `chat` | 客户端→服务端 | 用户聊天消息 |
| `reply` | 服务端→客户端 | AI回复（含isComplete） |
| `command_output` | 服务端→客户端 | 命令实时输出 |
| `command_complete` | 服务端→客户端 | 命令执行完成 |
| `error` | 服务端→客户端 | 错误通知 |

---

## 六、测试验证

### 6.1 测试统计

| 测试类 | 测试数量 | 状态 |
|--------|---------|------|
| CommandContextTest | 13 | ✅ 通过 |
| SshSessionContextTest | 11 | ✅ 通过 |
| AiSshAssistantManagerTest | 20 | ✅ 通过 |
| CommandContextManagerTest | 22 | ✅ 通过 |
| AiSshAssistantServiceTest | 13 | ✅ 通过 |
| AiSshAssistantServiceIntegrationTest | 9 | ✅ 通过 |
| AiSshTaskExecuteTest | 1 | ✅ 通过 |
| **总计** | **89** | **✅ 全部通过** |

### 6.2 关键问题修复

| 问题 | 原因 | 修复方案 |
|------|------|----------|
| 404 page not found | baseUrl缺少`/v1` | 改为`http://localhost:11434/v1` |
| HTTP客户端冲突 | 多个HTTP客户端实现 | 添加`.httpClientBuilder(new JdkHttpClientBuilder())` |
| 循环依赖 | commandContextManager ↔ aiSshAssistantService | 添加`@Lazy`注解 |
| AI分析结果未保存 | triggerAiAnalysis()只发送WebSocket | 添加`terminalChatRedisUtils.addMessage()` |

---

## 七、已知问题

| 问题编号 | 问题描述 | 状态 |
|---------|---------|------|
| ISSUE-001 | AI回复内容显示不一致（偶发） | 未复现，观察中 |
| TODO-001 | JWT用户ID写死"default-user" | 待集成JWT |
| TODO-002 | 响应式代码存在优化空间 | 待优化 |

---

## 八、开发提交记录

| 提交 | 说明 | 日期 |
|------|------|------|
| bbeaf6c | 重构AI模块：场景分离与工具调用优化 | 2025-02-04 |
| bc065c6 | 实现AI命令执行结果分析功能 | 2025-02-04 |
| e0f5c1a | 更新AI助手终端开发记录 | 2025-02-05 |
| 1d62b0d | 实现侧边栏AI助手功能：多会话管理与HTTP REST API通信 | 2026-02-07 |
| 9145db4 | 修复AI模型配置：使用流式ChatModel确保SSE正常工作 | 2026-02-08 |
| 723c2f1 | 修复侧边栏AI助手流式输出问题：响应式流、SSE格式、前端显示 | 2026-02-08 |

---

## 八、开发提交记录

| 提交 | 说明 | 日期 |
|------|------|------|
| bbeaf6c | 重构AI模块：场景分离与工具调用优化 | 2025-02-04 |
| bc065c6 | 实现AI命令执行结果分析功能 | 2025-02-04 |
| e0f5c1a | 更新AI助手终端开发记录 | 2025-02-05 |
| 1d62b0d | 实现侧边栏AI助手功能：多会话管理与HTTP REST API通信 | 2026-02-07 |
| 9145db4 | 修复AI模型配置：使用流式ChatModel确保SSE正常工作 | 2026-02-08 |
| 723c2f1 | 修复侧边栏AI助手流式输出问题：响应式流、SSE格式、前端显示 | 2026-02-08 |
| **(待提交)** | **Monitor-AI模块代码优化：统一异常管理、响应式编程优化** | 2026-02-08 |

---

## 九、Monitor-AI模块代码优化（2026-02-08）

### 9.1 优化概述

在响应式重构完成后，对Monitor-AI模块进行了全面的代码质量优化。

### 9.2 新增文件

| 文件 | 说明 |
|------|------|
| `constant/ChatConstants.java` | 聊天相关常量（MESSAGE_THRESHOLD、SESSION_TTL_DAYS等） |
| `constant/ErrorConstants.java` | 错误消息常量（统一错误文案） |
| `exception/BaseException.java` | 自定义异常基类 |
| `exception/BusinessException.java` | 业务异常 |
| `exception/ChatSessionNotFoundException.java` | 会话不存在异常 |
| `exception/AiServiceException.java` | AI服务异常 |
| `handler/GlobalExceptionHandler.java` | 全局异常处理器（@ControllerAdvice） |
| `util/JwtUtil.java` | JWT工具类（完成TODO功能） |

### 9.3 主要变更

#### 变更1：统一异常管理

**修复前**：
```java
// ChatController.java
.onErrorReturn(BaseResponse.error("创建会话失败"))

// ChatService.java
.onErrorResume(e -> Mono.error(new RuntimeException("...")))
```

**修复后**：
```java
// 抛出自定义异常 → GlobalExceptionHandler统一处理
throw new BusinessException(ErrorConstants.SESSION_CREATE_FAILED);
throw new ChatSessionNotFoundException(sessionId);
throw new AiServiceException(ErrorConstants.AI_MODEL_ERROR);
```

#### 变更2：常量提取

**修复前**：
```java
if (firstMessage.length() > 20) { ... }  // 魔法数字
Duration.ofDays(30)                         // 硬编码
```

**修复后**：
```java
if (firstMessage.length() > ChatConstants.SESSION_TITLE_MAX_LENGTH) { ... }
Duration.ofDays(ChatConstants.SESSION_TTL_DAYS)
```

#### 变更3：JWT解析完善

**修复前**：
```java
private String getUserIdFromAuth(String authHeader) {
    // TODO: 完善JWT解析逻辑
    return "default-user";
}
```

**修复后**：
```java
@Resource
private JwtUtil jwtUtil;

private String getUserIdFromAuth(String authHeader) {
    return jwtUtil.getUserIdFromAuth(authHeader);
}
```

#### 变更4：移除组件暴露

**修复前**：
```java
// ChatService.java
public ChatSessionRedisReactiveUtils getRedisUtils() {
    return redisUtils;
}

// ChatController.java
chatService.getRedisUtils().addMessage(sessionId, aiMsg)
```

**修复后**：
```java
// ChatService.java - 移除getter

// ChatController.java - 使用封装方法
chatService.sendMessageStreamWithSave(...)
```

#### 变更5：响应式阻塞修复

**修复前**：
```java
private Mono<String> generateSummary(String conversation) {
    return Mono.fromCallable(() -> {
        assistant.chat(messagesJson)
            .doOnNext(summaryBuilder::append)
            .blockLast(); // ❌ 阻塞EventLoop
        return summaryBuilder.toString();
    });
}
```

**修复后**：
```java
private Mono<String> generateSummary(String conversation) {
    // 构建消息（同步，很快）
    List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
    messages.add(new SystemMessage("..."));
    String messagesJson = convertMessagesToJson(messages);

    // 异步调用AI（不阻塞）
    return assistant.chat(messagesJson)
        .collectList()                              // ✅ 异步收集
        .map(chunks -> String.join("", chunks))      // ✅ 异步拼接
        .map(summary -> summary.isEmpty() ? "对话总结生成失败" : summary);
}
```

### 9.4 清理工作

| 清理项 | 说明 |
|--------|------|
| DEBUG日志 | 移除所有`[DEBUG-xxx]`日志 |
| TODO注释 | 清理AIModelConfig中的TODO |
| FeignConfig | 删除（AI与终端模块无相互调用需求） |
| 未使用导入 | 移除ChatService中的Schedulers导入 |

### 9.5 注入方式统一

| 文件 | 注解 |
|------|------|
| ChatController.java | `@Resource` |
| ChatService.java | `@Resource` |
| ChatSessionRedisReactiveUtils.java | `@Autowired`（修复Redis序列化问题） |

**注意**：ChatSessionRedisReactiveUtils必须使用`@Autowired`，否则ObjectMapper序列化配置不一致。

### 9.6 优化成果

| 指标 | 优化前 | 优化后 |
|------|--------|--------|
| 异常处理 | 分散、硬编码 | 统一、常量化 |
| 日志清晰度 | DEBUG日志残留 | 只保留关键日志 |
| JWT功能 | TODO未完成 | 完整实现 |
| 组件封装 | getter暴露内部组件 | 严格封装 |
| 响应式性能 | blockLast()阻塞 | 完全异步 |
| 代码行数 | ~600行 | ~550行（清理后） |

---

*文档版本：V3.1（含代码优化）*
*最后更新：2026年2月8日*
