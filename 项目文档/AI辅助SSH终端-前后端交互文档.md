# AI辅助SSH终端 - 前后端交互文档

## 一、概述

本文档描述主机详情页AI助手功能的前后端交互规范，包括HTTP握手接口和WebSocket实时通信协议。

**注意**：系统中有两种AI助手场景：

1. **侧边栏AI助手**（全局）：通用AI对话，多会话管理，使用HTTP REST API
   - 路由：`/ai`
   - 详细文档：见《AI助手终端集成开发记录.md》第十三章

2. **主机详情页AI助手**（本文档描述）：与SSH终端绑定的AI助手，使用WebSocket
   - 位置：主机详情页对话框
   - 支持SSH命令执行和结果分析

**功能特点**：
- 与SSH终端绑定的AI助手
- 支持自然语言执行SSH命令
- AI自动分析命令执行结果
- WebSocket实时双向通信
- 消息历史持久化（Redis）

---

## 二、架构概览

```
┌─────────────────────────────────────────────────────────────────────┐
│                          前端 (Vue 3)                             │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  主机详情页 (HostDetailDialog.vue)                           │  │
│  │    ├─ SSH终端面板 (TerminalPanel.vue)                         │  │
│  │    └─ AI助手面板 (AiAssistantPanel.vue)                       │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
         │                                    │
         │ SSH WebSocket                      │ AI WebSocket
         │ WS /ws/ssh/terminal/{sshSessionId} │ WS /ws/ai/ssh-assistant/{aiSessionId}
         │                                    │
┌─────────────────────────────────────────────────────────────────────┐
│                        后端 (Spring Boot)                           │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐  │
│  │ SshWebSocketHandler  │  │ AiSshAssistantHandler            │  │
│  │ (SSH终端代理)         │  │ (AI助手WebSocket处理器)           │  │
│  └──────────────────────┘  └──────────────────────────────────┘  │
│         │                              │                            │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐  │
│  │ SshSessionManager     │  │ AiSshAssistantService            │  │
│  │ (SSH会话管理)         │  │ (AI对话服务)                     │  │
│  └──────────────────────┘  └──────────────────────────────────┘  │
│                                        │                            │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ AiSshAssistantController (HTTP握手接口)                    │  │
│  │ POST /api/ai/ssh-assistant/connect                         │  │
│  │ DELETE /api/ai/ssh-assistant/disconnect/{aiSessionId}       │  │
│  └────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          Redis                                   │
│  ai:ssh:binding:{aiSessionId}     → SSH-AI绑定关系                 │
│  ai:ssh:messages:{aiSessionId}   → 消息历史                       │
│  ai:ssh:info:{aiSessionId}        → 会话信息                       │
│  ai:ssh:ws:sessions              → 活跃WebSocket会话集合            │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 三、HTTP握手接口

### 3.1 创建AI助手会话（连接）

**请求**

```http
POST /api/ai/ssh-assistant/connect
Content-Type: application/json
Authorization: Bearer {jwt_token}
```

```json
{
  "sshSessionId": "SSH_3C91E85038D549C1",
  "agentId": "agent-001"
}
```

**字段说明**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sshSessionId | String | 是 | SSH终端WebSocket的会话ID |
| agentId | String | 是 | 主机ID，用于AI获取主机上下文 |

**响应**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "aiSessionId": "550e8400-e29b-41d4-a716-446655440000",
    "message": "连接成功，请使用aiSessionId建立WebSocket连接"
  },
  "timestamp": 1738657200000
}
```

**字段说明**

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 响应码，200表示成功 |
| message | String | 响应消息 |
| data.aiSessionId | String | 生成的AI会话ID，用于后续WebSocket连接 |
| data.message | String | 连接提示信息 |
| timestamp | Long | 响应时间戳 |

---

### 3.2 断开AI助手会话

**请求**

```http
DELETE /api/ai/ssh-assistant/disconnect/{aiSessionId}
Authorization: Bearer {jwt_token}
```

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| aiSessionId | String | 是 | AI会话ID |

**响应**

```json
{
  "code": 200,
  "message": "会话已断开",
  "data": "会话已断开",
  "timestamp": 1738657200000
}
```

---

### 3.3 获取会话绑定信息

**请求**

```http
GET /api/ai/ssh-assistant/binding/{aiSessionId}
Authorization: Bearer {jwt_token}
```

**响应**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "aiSessionId": "550e8400-e29b-41d4-a716-446655440000",
    "sshSessionId": "SSH_3C91E85038D549C1",
    "agentId": "agent-001",
    "userId": "user-001",
    "createdAt": 1738657200000
  },
  "timestamp": 1738657200000
}
```

---

### 3.4 检查会话是否活跃

**请求**

```http
GET /api/ai/ssh-assistant/active/{aiSessionId}
Authorization: Bearer {jwt_token}
```

**响应**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": true,
  "timestamp": 1738657200000
}
```

**data为true表示WebSocket连接活跃，false表示未连接或已断开**

---

## 四、WebSocket通信协议

### 4.1 连接建立

**WebSocket端点**

```
ws://server/ws/ai/ssh-assistant/{aiSessionId}
```

**连接流程**

1. 前端先调用HTTP接口创建会话，获取`aiSessionId`
2. 使用`aiSessionId`建立WebSocket连接
3. 后端验证会话是否存在
4. 连接成功后，后端发送欢迎消息

**连接成功消息**

```json
{
  "type": "reply",
  "content": "AI助手已连接，您可以开始对话了",
  "timestamp": 1738657200000
}
```

**连接失败示例**

```json
{
  "type": "error",
  "errorCode": "SESSION_NOT_FOUND",
  "content": "AI会话不存在，请重新连接",
  "timestamp": 1738657200000
}
```

---

### 4.2 消息格式

**通用消息结构**

```json
{
  "type": "消息类型",
  "content": "消息内容",
  "timestamp": 1738657200000,
  "errorCode": "错误码(仅type=error时)"
}
```

**字段说明**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | String | 是 | 消息类型（见下表） |
| content | String | 条件 | 消息内容，type=error时为错误描述 |
| timestamp | Long | 是 | 消息时间戳（毫秒） |
| errorCode | String | 条件 | 错误码，仅type=error时存在 |

---

### 4.3 消息类型

#### 4.3.1 客户端发送消息

| type | 用途 | content示例 |
|------|------|-------------|
| chat | 发送聊天消息 | "帮我查看CPU使用率" |
| ping | 心跳保活 | - |

**聊天消息示例**

```json
{
  "type": "chat",
  "content": "帮我查看CPU使用率",
  "timestamp": 1738657200000
}
```

---

#### 4.3.2 服务端发送消息

| type | 用途 | content示例 |
|------|------|-------------|
| reply | AI回复 | "好的，正在为您查询CPU使用率..." |
| error | 错误通知 | "消息处理失败" |
| ping | 心跳响应 | "pong" |
| command_output | 命令实时输出 | "total 123" |
| command_complete | 命令执行完成 | "命令执行完成" |
| command_timeout | 命令执行超时 | "命令执行超时" |

**AI回复消息示例**

```json
{
  "type": "reply",
  "content": "我已经为您执行了 `ls` 命令...",
  "timestamp": 1738657200000
}
```

**错误消息示例**

```json
{
  "type": "error",
  "errorCode": "EMPTY_MESSAGE",
  "content": "消息内容不能为空",
  "timestamp": 1738657200000
}
```

---

### 4.4 错误码清单

| errorCode | 说明 | HTTP状态码 |
|-----------|------|-------------|
| SESSION_NOT_FOUND | AI会话不存在 | 404 |
| EMPTY_MESSAGE | 消息内容为空 | 400 |
| INVALID_PARAM | 参数无效 | 400 |
| MESSAGE_ERROR | 消息处理失败 | 500 |
| CHAT_ERROR | 对话处理失败 | 500 |
| UNKNOWN_TYPE | 未知消息类型 | 400 |
| TRANSPORT_ERROR | WebSocket传输错误 | 500 |

---

## 五、完整交互流程

### 5.1 会话建立流程

```sequence
Frontend            Backend              Redis
   │                   │                    │
   │ 1. SSH连接成功，获取sshSessionId   │
   │ ──────────────────────────────────> │
   │                   │                    │
   │ 2. POST /api/ai/ssh-assistant/connect
   │    {sshSessionId, agentId}           │
   │ ──────────────────────────────────> │
   │                   │ 3. 创建绑定关系    │
   │                   │ ──────────────────> │
   │                   │ 4. 保存会话信息    │
   │                   │ ──────────────────> │
   │                   │                    │
   │ 5. 返回aiSessionId                   │
   │ <────────────────────────────────── │
   │                   │                    │
   │ 6. WS /ws/ai/ssh-assistant/{aiSessionId}
   │ ──────────────────────────────────> │
   │                   │ 7. 验证会话        │
   │                   │ ──────────────────> │
   │                   │                    │
   │ 8. 连接成功，发送欢迎消息            │
   │ <────────────────────────────────── │
   │                   │ 9. 标记会话为活跃  │
   │                   │ ──────────────────> │
```

---

### 5.2 对话交互流程

```sequence
Frontend            AiSshAssistantService   SshExecuteTool    SSH
   │                        │                      │           │
   │ 1. 发送chat消息         │                      │           │
   │ ──────────────────────>│                      │           │
   │                        │ 2. 保存用户消息       │           │
   │                        │ ──────────────────>  (Redis)   │
   │                        │                      │           │
   │                        │ 3. 构建上下文        │           │
   │                        │ 4. 设置ThreadLocal   │           │
   │                        │                      │           │
   │                        │ 5. 调用AI模型        │           │
   │                        │ ────> (LangChain4j) ──>│           │
   │                        │                      │           │
   │                        │ 6. AI决定调用工具    │           │
   │                        │ <────────────────── │           │
   │                        │                      │           │
   │                        │ 7. 调用SshExecuteTool│           │
   │                        │ ───────────────────>│           │
   │                        │                      │ 8. 发送命令│
   │                        │                      │ ─────────>│
   │                        │                      │ 9. 返回结果│
   │                        │ <──────────────────│           │
   │                        │                      │           │
   │ 10. AI回复            │                      │           │
   │ <─────────────────────│                      │           │
   │                        │ 11. 保存AI回复       │           │
   │                        │ ──────────────────>  (Redis)   │
   │                        │ 12. 清理ThreadLocal  │           │
```

---

### 5.3 命令执行与结果分析流程

```sequence
Frontend    AiSshAssistantService  SshExecuteTool  CommandContextManager  SSH
   │                 │                      │                   │        │
   │                 │ 1. AI调用工具         │                   │        │
   │                 │ ────────────────────>│                   │        │
   │                 │                      │ 2. 注册命令       │        │
   │                 │                      │ ─────────────────>│        │
   │                 │                      │                   │        │
   │                 │                      │ 3. 发送命令到SSH  │        │
   │                 │                      │ ───────────────────────>│
   │                 │                      │                   │        │
   │                 │                      │ 4. SSH输出返回    │        │
   │                 │                      │ <───────────────────────│
   │                 │                      │ 5. 追加输出       │        │
   │                 │                      │ ─────────────────>│        │
   │                 │                      │                   │ 6. 推送输出│
   │ 7. 命令输出     │                      │ <────────────────>│        │
   │ <────────────────│                      │                   │        │
   │                 │                      │ 7. 检测END标记   │        │
   │                 │                      │ ─────────────────>│        │
   │                 │                      │                   │ 8. 完成命令│
   │                 │                      │ <─────────────────│        │
   │                 │                      │                   │        │
   │                 │ 9. 触发异步分析      │                   │        │
   │                 │ <───────────────────│                   │        │
   │                 │ 10. 分析命令输出    │                   │        │
   │                 │ ───> (AI模型)        │                   │        │
   │                 │ 11. 保存分析结果    │                   │        │
   │                 │ ──────────────────>  (Redis)           │        │
   │ 12. 分析结果   │                      │                   │        │
   │ <────────────────│                      │                   │        │
```

---

## 六、WebSocket消息时序示例

### 6.1 基本对话

```json
// 客户端发送
{"type":"chat","content":"你好","timestamp":1738657200000}

// 服务端回复
{"type":"reply","content":"您好！我是主机运维AI助手，有什么可以帮助您的吗？","timestamp":1738657200000}
```

---

### 6.2 执行命令

```json
// 客户端发送
{"type":"chat","content":"帮我执行ls命令","timestamp":1738657200000}

// 服务端立即回复（AI执行中）
{"type":"reply","content":"正在执行命令: ls (命令ID: abc-123)...","timestamp":1738657200000}

// 命令输出（可能多条）
{"type":"command_output","content":"Desktop  Documents  Downloads","timestamp":1738657200000}
{"type":"command_output","content":"Music  Pictures  Public","timestamp":1738657200100}

// 命令完成
{"type":"command_complete","content":"命令执行完成","timestamp":1738657200200}

// AI分析结果（异步）
{"type":"reply","content":"### 命令分析\n\n1. **执行状态**：命令 `ls` 成功执行...\n2. **关键信息**：Desktop, Documents, Downloads...","timestamp":1738657205000}
```

---

### 6.3 错误处理

```json
// 客户端发送空消息
{"type":"chat","content":"","timestamp":1738657200000}

// 服务端错误响应
{"type":"error","errorCode":"EMPTY_MESSAGE","content":"消息内容不能为空","timestamp":1738657200000}
```

---

## 七、前端实现要点

### 7.1 连接建立

```typescript
// 1. 先创建AI会话
const connectResponse = await api.post('/api/ai/ssh-assistant/connect', {
  sshSessionId: currentSshSessionId,
  agentId: currentAgentId
});

const { aiSessionId } = connectResponse.data;

// 2. 建立WebSocket连接
const wsUrl = `ws://server/ws/ai/ssh-assistant/${aiSessionId}`;
const ws = new WebSocket(wsUrl);

ws.onopen = () => {
  console.log('AI助手WebSocket连接成功');
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  handleMessage(message);
};

ws.onerror = (error) => {
  console.error('WebSocket错误:', error);
};

ws.onclose = () => {
  console.log('AI助手WebSocket连接关闭');
};
```

---

### 7.2 发送消息

```typescript
function sendMessage(content: string) {
  const message = {
    type: 'chat',
    content: content,
    timestamp: Date.now()
  };

  ws.send(JSON.stringify(message));
}
```

---

### 7.3 处理接收消息

```typescript
function handleMessage(message: WsChatMessage) {
  switch (message.type) {
    case 'reply':
      // AI回复
      addAssistantMessage(message.content);
      break;

    case 'command_output':
      // 命令实时输出
      appendCommandOutput(message.content);
      break;

    case 'command_complete':
      // 命令执行完成
      markCommandComplete();
      break;

    case 'error':
      // 错误消息
      showError(message.content);
      break;

    case 'ping':
      // 心跳响应
      // 不需要处理
      break;

    default:
      console.warn('未知消息类型:', message.type);
  }
}
```

---

### 7.4 心跳保活

```typescript
// 每30秒发送一次心跳
setInterval(() => {
  if (ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'ping' }));
  }
}, 30000);
```

---

### 7.5 断开连接

```typescript
async function disconnect() {
  // 1. 关闭WebSocket
  ws.close();

  // 2. 调用断开接口
  await api.delete(`/api/ai/ssh-assistant/disconnect/${aiSessionId}`);
}
```

---

## 八、数据结构定义

### 8.1 ConnectRequest

```typescript
interface ConnectRequest {
  sshSessionId: string;  // SSH会话ID
  agentId: string;       // 主机ID
}
```

### 8.2 ConnectResponse

```typescript
interface ConnectResponse {
  aiSessionId: string;  // AI会话ID
  message: string;      // 连接提示
}
```

### 8.3 WsChatMessage

```typescript
type MessageType = 'chat' | 'reply' | 'error' | 'ping' | 'command_output' | 'command_complete' | 'command_timeout';

interface WsChatMessage {
  type: MessageType;     // 消息类型
  content?: string;       // 消息内容
  timestamp: number;      // 时间戳（毫秒）
  errorCode?: string;     // 错误码（type=error时）
}
```

### 8.4 BaseResponse

```typescript
interface BaseResponse<T = any> {
  code: number;          // 响应码
  message: string;       // 响应消息
  data?: T;             // 响应数据
  timestamp: number;     // 时间戳

  isSuccess(): boolean; // 是否成功
  isError(): boolean;   // 是否失败
}
```

---

## 九、错误处理

### 9.1 连接失败

**场景**：WebSocket连接时aiSessionId不存在

**处理**：
1. 接收error类型消息，errorCode=SESSION_NOT_FOUND
2. 关闭WebSocket连接
3. 提示用户"AI会话已过期，请重新连接"
4. 返回SSH终端界面，引导用户重新创建AI会话

---

### 9.2 消息发送失败

**场景**：发送的消息内容为空

**处理**：
1. 接收error类型消息，errorCode=EMPTY_MESSAGE
2. 提示用户"消息内容不能为空"
3. 不将消息添加到聊天界面

---

### 9.3 心跳超时

**场景**：WebSocket连接意外断开

**处理**：
1. 监听WebSocket.onclose事件
2. 清理本地状态
3. 提示用户"AI助手连接已断开"
4. 可选：自动重连（带重试次数限制）

---

## 十、安全注意事项

### 10.1 JWT认证

所有HTTP接口必须携带JWT Token：

```http
Authorization: Bearer {jwt_token}
```

---

### 10.2 会话验证

1. WebSocket连接时验证aiSessionId是否存在
2. 每次处理消息时验证会话仍然有效
3. 会话不存在时主动关闭WebSocket连接

---

### 10.3 输入验证

1. 用户消息不能为空
2. 用户消息长度限制（建议：最大4096字符）
3. 防止注入攻击（消息内容应进行转义）

---

## 十一、性能优化建议

### 11.1 消息历史管理

1. 前端分页加载历史消息（每次加载20条）
2. 定期清理过期消息（Redis自动设置24小时TTL）
3. 消息数量超过阈值时触发AI总结压缩

---

### 11.2 WebSocket连接管理

1. 页面关闭时主动断开WebSocket连接
2. 实现心跳机制（每30秒一次）
3. 检测连接超时自动重连（最多重试3次）

---

### 11.3 命令执行优化

1. 命令执行超时时间：5秒（可配置）
2. 命令输出长度限制：避免输出过大导致性能问题
3. 异步分析：命令完成后异步触发AI分析，避免阻塞

---

## 十二、测试接口地址

### 开发环境

```
HTTP: http://localhost:8080/api/ai/ssh-assistant
WebSocket: ws://localhost:8080/ws/ai/ssh-assistant/{aiSessionId}
```

### 生产环境

```
HTTP: https://your-domain.com/api/ai/ssh-assistant
WebSocket: wss://your-domain.com/ws/ai/ssh-assistant/{aiSessionId}
```

---

## 十三、附录：完整的WebSocket消息示例

### 示例1：简单对话

```json
// 发送
{"type":"chat","content":"你好","timestamp":1738657200000}

// 接收
{"type":"reply","content":"您好！我是主机运维AI助手...","timestamp":1738657200000}
```

### 示例2：执行命令并分析结果

```json
// 发送
{"type":"chat","content":"帮我查看CPU使用率","timestamp":1738657200000}

// 接收（立即回复）
{"type":"reply","content":"正在执行命令: top -n 1 (命令ID: cmd-123)...","timestamp":1738657200000}

// 接收（命令输出，可能多条）
{"type":"command_output","content":"top - 10:05:32 up 1 day, 2:15, 2 users, load average: 0.01, 0.03, 0.05","timestamp":1738657200100}
{"type":"command_output","content":"Tasks: 123 total, 2 running, 121 sleeping, 0 stopped, 0 zombie","timestamp":1738657200150}

// 接收（命令完成）
{"type":"command_complete","content":"命令执行完成","timestamp":1738657200200}

// 接收（AI分析，异步）
{"type":"reply","content":"### 命令分析\n\n根据命令输出分析：\n1. CPU使用率较低（约1-2%）\n2. 系统负载正常（0.01, 0.03, 0.05）\n3. 当前运行进程：123个...","timestamp":1738657205000}
```

### 示例3：错误处理

```json
// 发送
{"type":"chat","content":"","timestamp":1738657200000}

// 接收
{"type":"error","errorCode":"EMPTY_MESSAGE","content":"消息内容不能为空","timestamp":1738657200000}
```
