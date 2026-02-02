# AI 阅读文档

## 文档说明

本文档专门供 AI 助手阅读，用于快速理解 Monitor 项目结构、协助开发工作。

**文档版本**：v1.0.0
**更新日期**：2026-02-02
**项目路径**：`C:\Users\hundred\Desktop\Java学习\Monitor\Monitor\monitor-project`

---

## 1. 项目快照

### 1.1 项目定位

**服务器运维监控系统** - 分布式客户端-服务端架构

- **Monitor-Agent**: 部署在被监控主机，采集系统信息并上报
- **Monitor-Server**: 中央服务器，接收数据、提供API、SSH代理
- **Monitor-Web**: Vue 3 前端，数据展示和Web终端
- **CommonLibrary**: 共享DTO和数据模型

### 1.2 当前实现状态

| 模块 | 状态 | 说明 |
|------|------|------|
| Agent注册上报 | ✅ 完整 | 双频上报，自动重试 |
| 监控数据存储 | ✅ MySQL | InfluxDB依赖引入但未使用 |
| SSH WebSocket代理 | ✅ 完整 | JSch实现 |
| JWT认证 | ✅ 完整 | Spring Security + JWT |
| 邮件验证 | ✅ 完整 | RabbitMQ异步 |
| AI助手 | ⚠️ 仅前端 | 后端Controller缺失 |
| 用户管理 | ⚠️ 仅前端 | 后端API缺失 |

### 1.3 已删除/未实现功能

- **AI助手后端**: 前端UI完整，后端 `/src/main/java/../aitools/` 目录为空
- **InfluxDB时序存储**: 依赖存在但代码中完全未使用
- **用户管理API**: 前端有页面，后端无对应Controller
- **系统设置API**: 前端有页面，后端无对应Controller

---

## 2. 关键设计决策（理解意图）

### 2.1 双频上报设计

**为什么？**
- 硬件信息（CPU型号、内存大小）变化频率低 → 10分钟上报一次
- 运行指标（CPU使用率、网络流量）需要实时监控 → 15秒上报一次

**代码位置**：`Monitor-Agent/src/main/java/com/hundred/monitor/agent/service/ReportService.java`

### 2.2 Agent多服务器配置

**为什么？**
- 支持配置多个Server地址
- Agent启动时健康检查，选择响应最快的服务器
- 实现高可用

**配置位置**：`Monitor-Agent/src/main/resources/agent-config.yaml`

### 2.3 SSH WebSocket代理

**为什么？**
- 浏览器无法直接建立SSH连接
- Server作为中继：WebSocket ←→ JSch ←→ SSH
- 统一管理SSH会话和凭证

**核心类**：
- `SshWebSocketHandler`: WebSocket处理
- `SshSessionManager`: 会话管理
- `SshService`: SSH连接（JSch）

---

## 3. 代码模块依赖关系

```
┌─────────────────────────────────────────────────────────────┐
│                    CommonLibrary                             │
│  (Agent 和 Server 共享的 DTO: RegisterRequest, Metrics...)   │
└────────────┬──────────────────────────────┬─────────────────┘
             │                              │
    ┌────────▼────────┐          ┌─────────▼────────┐
    │  Monitor-Agent  │          │  Monitor-Server  │
    │                 │          │                  │
    │ ┌─────────────┐ │          │ ┌──────────────┐ │
    │ │ 依赖 Common │ │          │ │ 依赖 Common   │ │
    │ └─────────────┘ │          │ └──────────────┘ │
    │                 │          │                  │
    │ ┌─────────────┐ │          │ ┌──────────────┐ │
    │ │ OSHI采集    │ │          │ │ MyBatis-Plus │ │
    │ │ OpenFeign   │ │          │ │ Spring Sec   │ │
    │ │ 上报数据    │ │          │ │ JSch         │ │
    │ └─────────────┘ │          │ │ WebSocket    │ │
    └─────────────────┘          │ └──────────────┘ │
                                 └──────────────────┘
                                         ▲
                                         │ HTTP/WebSocket
                        ┌────────────────┴────────────────┐
                        │      Monitor-Web (Vue 3)        │
                        │  - 数据展示                       │
                        │  - SSH终端(xterm.js)             │
                        │  - 调用Server API                │
                        └──────────────────────────────────┘
```

---

## 4. API 路径速查

### 4.1 实际存在的API（基于代码）

| 路径 | 方法 | 功能 | Controller |
|------|------|------|------------|
| `/api/v1/customer/register` | POST | Agent注册 | AgentController |
| `/api/v1/agent/basic` | POST | 基本数据上报 | AgentController |
| `/api/v1/agent/metrics` | POST | 指标数据上报 | AgentController |
| `/api/v1/agent/config` | POST | 配置更新（Agent端） | AgentController |
| `/api/auth/login` | POST | 用户登录 | AuthController |
| `/api/auth/logout` | POST | 用户登出 | AuthController |
| `/api/auth/register` | POST | 用户注册 | AuthController |
| `/api/auth/refresh` | POST | 刷新Token | AuthController |
| `/api/auth/validate` | GET | 验证Token | AuthController |
| `/api/auth/reset-password` | POST | 重置密码 | AuthController |
| `/api/auth/ask-code` | POST | 请求验证码 | AuthController |
| `/api/monitor/getMonitorList` | GET | 获取Agent列表 | MonitorController |
| `/api/monitor/{agentId}/metrics/latest` | GET | 获取最新指标 | MonitorController |
| `/api/monitor/{agentId}/metrics/history` | GET | 获取历史指标 | MonitorController |
| `/api/v1/ssh/credential/{agentId}` | GET | 获取SSH凭证 | SshController |
| `/api/v1/ssh/connect` | POST | 建立SSH连接 | SshController |
| `/api/v1/ssh/disconnect` | POST | 断开SSH连接 | SshController |
| `/api/v1/ssh/command` | POST | 发送SSH命令 | SshController |
| `/api/health` | GET | 健康检查 | HealthController |
| `/api/v1/ssh/terminal/{sessionId}` | WS | SSH终端WebSocket | SshWebSocketHandler |

### 4.2 前端期望但后端不存在的API

| 路径 | 方法 | 功能 | 状态 |
|------|------|------|------|
| `/ai/chat` | POST | 发送聊天消息 | ❌ 缺失 |
| `/ai/chat/{sessionId}/history` | GET | 获取聊天历史 | ❌ 缺失 |
| `/ai/sessions` | GET/POST/DELETE | 会话管理 | ❌ 缺失 |
| `/ai/quick-actions` | GET | 快捷指令 | ❌ 缺失 |
| `/api/admin/users` | GET/POST/PUT/DELETE | 用户管理 | ❌ 缺失 |
| `/api/admin/settings` | GET/PUT | 系统设置 | ❌ 缺失 |

---

## 5. 数据库表速查

| 表名 | 用途 | 关键字段 |
|------|------|----------|
| `user` | 用户账号 | user_id, username, email, password, user_role |
| `agent` | Agent信息 | agent_id(PK), agent_name, hostname, ip, cpu_model, memory_gb |
| `agent_metrics` | 监控指标 | id(PK), agent_id, cpu_percent, memory_percent, timestamp |
| `ssh_credential` | SSH凭证 | id(PK), agent_id, username, password(明文) |

**注意**：`password` 字段当前明文存储，代码中标记 `TODO: 加密密码`

---

## 6. 端口使用清单

| 端口 | 组件 | 协议 |
|------|------|------|
| 8080 | Monitor-Server | HTTP |
| 8081 | Monitor-Agent | HTTP |
| 3306 | MySQL | TCP |
| 6379 | Redis | TCP |
| 5672 | RabbitMQ | TCP |
| 15672 | RabbitMQ管理界面 | HTTP |

---

## 7. 敏感配置位置

| 配置项 | 位置 | 说明 |
|--------|------|------|
| JWT密钥 | `Monitor-Server/src/main/resources/application.yaml` | `jwt.secret` |
| 数据库密码 | 同上 | `spring.datasource.password` |
| Redis密码 | 同上 | `spring.redis.password` |
| 邮件密码 | 同上 | `spring.mail.password` |
| Agent Token | `Monitor-Agent/src/main/resources/agent-config.yaml` | `auth.token` |
| SSH密码 | 数据库 `ssh_credential` 表 | 明文存储 |

---

## 8. 核心类速查

### Agent端

| 类 | 职责 |
|------|------|
| `BootstrapService` | 启动时注册逻辑 |
| `RegisterService` | 向Server注册，选择最快服务器 |
| `ReportService` | 定时上报（10分钟基础数据，15秒指标） |
| `CollectService` | OSHI采集系统信息 |
| `ConfigLoader` | 加载YAML配置 |

### Server端

| 类 | 职责 |
|------|------|
| `AgentController` | 处理Agent注册、数据上报 |
| `AuthController` | 处理用户认证、注册 |
| `MonitorController` | 处理监控数据查询 |
| `SshController` | 处理SSH连接请求 |
| `SshWebSocketHandler` | WebSocket处理SSH终端 |
| `SshSessionManager` | 管理SSH会话 |
| `JwtAuthenticationFilter` | JWT认证过滤器 |
| `JwtTokenProvider` | 生成和验证JWT |

---

## 9. 日志调试位置

| 组件 | 日志位置/方式 |
|------|---------------|
| Server | 控制台输出 |
| Agent | 控制台输出 |
| Web | 浏览器DevTools Console |
| MyBatis SQL | `application.yaml` → `mybatis-plus.configuration.log-impl` |

---

## 10. 常见修改场景

### 添加新的监控指标

1. **Agent端**：`CollectService.java` 添加采集逻辑
2. **Common**：`Metrics.java` 添加字段
3. **Server端**：`AgentMetricsService.java` 添加存储逻辑
4. **前端**：`HostDetailDialog.vue` 添加展示

### 添加新的API接口

1. **Controller**：在对应的Controller添加方法
2. **Service**：实现业务逻辑（如有）
3. **SecurityConfig**：配置匿名访问（如需要）
4. **前端**：`api/*.ts` 添加调用方法

### 修改上报频率

**位置**：`ReportService.java`
```java
@Scheduled(fixedRate = 600000)  // 基本数据：10分钟
@Scheduled(fixedRate = 15000)   // 运行时数据：15秒
```

---

## 11. 已知TODO

| 位置 | TODO | 优先级 |
|------|------|--------|
| `SshServiceImpl.java:175` | 加密SSH密码 | 高 |
| `SshWebSocketHandler.java:37` | 处理二进制消息（resize） | 低 |
| `SshWebSocketHandler.java:43` | WebSocket心跳（pong） | 低 |

---

## 12. 与CLAUDE.md的差异

以下内容在CLAUDE.md中描述但与实际代码不符：

| 描述 | CLAUDE.md | 实际代码 |
|------|-----------|----------|
| 认证API路径 | `/api/v1/auth/login` | `/api/auth/login` |
| 监控API路径 | `/api/v1/monitor/agents` | `/api/monitor/getMonitorList` |
| InfluxDB使用 | "Server uses InfluxDB" | 代码中完全未使用 |
| AI助手功能 | "AI-assisted operations" | 后端完全缺失 |

**注意**：开发时以实际代码为准，不要依赖CLAUDE.md的API路径描述。
