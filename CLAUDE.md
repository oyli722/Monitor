# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A distributed monitoring system (服务运维监控系统) with client-server architecture. The system monitors remote hosts by deploying agents that collect system metrics and report them to a central server, which provides a web-based dashboard for visualization and management with integrated SSH terminal and AI assistant.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Web Browser                              │
│                    (Monitor-Web: Vue 3)                         │
│                  Port: 5173 (dev), 8080 (prod)                  │
└──────────────────────────────┬──────────────────────────────────┘
                               │ HTTP/WebSocket
                               │
┌──────────────────────────────▼──────────────────────────────────┐
│                      Monitor-Server                              │
│                   (Spring Boot 3.5.10)                          │
│                      Port: 8080                                  │
│  Controllers: Auth, Agent, Monitor, Ssh, Health                 │
│  Services: AuthService, AgentService, MonitorService, SshService │
│  Data: MySQL (MyBatis Plus), Redis, RabbitMQ                    │
└──────────────────────────────┬──────────────────────────────────┘
                               │ HTTP (Agent registration/reporting)
                               │
         ┌─────────────────────┼─────────────────────┐
         │                     │                     │
┌────────▼─────────┐  ┌───────▼────────┐  ┌────────▼─────────┐
│  Monitor-Agent   │  │ Monitor-Agent  │  │  Monitor-Agent   │
│   (Host 1)       │  │   (Host 2)     │  │   (Host 3)       │
│  Port: 8081      │  │  Port: 8081    │  │  Port: 8081      │
└──────────────────┘  └────────────────┘  └──────────────────┘
```

## Module Structure

- **CommonLibrary/** - Shared models and DTOs (BasicInfo, Metrics, registration requests/responses)
- **Monitor-Agent/** - Agent for monitored hosts, collects metrics via OSHI library
- **Monitor-Server/** - Central server with REST API, SSH WebSocket proxy, authentication
- **Monitor-Web/** - Vue 3 + TypeScript web dashboard with Element Plus and ECharts

## Common Development Commands

### Java Modules (Maven)

**Parent POM:** `monitor-project/pom.xml` defines unified dependency management and uses Aliyun mirrors for faster downloads in China.

```bash
# IMPORTANT: Build CommonLibrary first (it's a dependency for Agent and Server)
cd monitor-project/CommonLibrary && mvn clean install

# Build Agent (after CommonLibrary)
cd monitor-project/Monitor-Agent && mvn clean install

# Build Server (after CommonLibrary)
cd monitor-project/Monitor-Server && mvn clean install

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=ClassName

# Run Spring Boot application
mvn spring-boot:run

# Package as JAR
mvn clean package
```

### Web Module (npm/Vite)

```bash
cd monitor-project/Monitor-Web

# Install dependencies
npm install

# Development server (runs on port 5173)
npm run dev

# Build for production
npm run build

# Type checking
npm run type-check

# Lint with auto-fix
npm run lint

# Format code
npm run format

# Unit tests (Vitest)
npm run test:unit

# E2E tests (Playwright)
npm run test:e2e
npx playwright install  # First time only - installs browsers
```

## Startup Sequence

1. Start MySQL (port 3306) - Create database using `monitor-project/Monitor-Server/src/main/resources/sql/init.sql`
2. Start Redis (port 6379)
3. Start RabbitMQ (port 5672)
4. Build CommonLibrary: `cd monitor-project/CommonLibrary && mvn clean install`
5. Start Monitor-Server: `cd monitor-project/Monitor-Server && mvn spring-boot:run`
6. Start Monitor-Agent: `cd monitor-project/Monitor-Agent && mvn spring-boot:run`
7. Start Monitor-Web: `cd monitor-project/Monitor-Web && npm run dev`

## Key Architecture Patterns

### Agent-Server Communication
- Agents register with server on startup via `POST /api/v1/customer/register`
- Dual-frequency reporting:
  - Basic data (hardware info via `BasicInfo`): every 10 minutes
  - Metrics (CPU, memory, disk, network via `Metrics`): every 15 seconds
- Server can push config updates to agents via `POST /api/v1/agent/config`
- **Agent startup sequence**: `BootstrapService` checks registration → registers if needed → starts scheduled reporting tasks

### Authentication
- JWT-based stateless authentication (Spring Security + JwtTokenProvider)
- Role-based access control (admin/user)
- Email verification for registration/password reset
- **SecurityConfig.java**: Public endpoints include `/api/health`, `/api/auth/**`, `/api/v1/agent/**`, `/ws/**`
- Frontend uses Pinia store (`stores/auth.ts`) with token persistence

### SSH Terminal Proxy
- Browser connects to Server via WebSocket (`WS /ws/ssh/terminal/{sessionId}`)
- Server maintains SSH session to Agent host via JSch
- Terminal I/O relayed through WebSocket
- Credentials stored encrypted in `ssh_credential` table
- **SshSessionManager**: Singleton managing active SSH sessions
- **xterm.js**: Frontend terminal emulation with `@xterm/addon-fit`

### AI Assistant Integration (LangChain4j)

**Two Distinct Scenarios:**

1. **Global Chat (Sidebar)** - Direct model injection
   - Uses `@Primary OpenAiChatModel defaultOpenAiChatModel()`
   - No tool calling
   - Context stored in Redis via `ChatSessionRedisUtils`
   - HTTP REST API communication
   - Frontend: `Monitor-Web/src/views/ai/` chat interface

2. **Host Detail SSH Assistant** - With tool calling
   - Uses `@Primary Assistant defaultAssistant()` with `SshExecuteTool`
   - Tool can execute SSH commands on bound host
   - **ThreadLocal context** (`SshSessionContext`) carries: aiSessionId, sshSessionId, agentId
   - WebSocket-based communication
   - Command output tracking via `CommandContextManager`
   - Location: `Monitor-Server/src/main/java/com/hundred/monitor/server/ai/`

**SSH Assistant Tool Calling Flow:**
```
User message → AiSshAssistantService.sendMessage()
    → Sets ThreadLocal context (aiSessionId, sshSessionId, agentId)
    → AI model with tools
    → SshExecuteTool.executeCommand() - fetches context from ThreadLocal
    → Sends command to SSH session with markers:
        ## COMMAND_START: {timestamp} ##
        {command}
        ## COMMAND_END: {timestamp} ##
    → SshWebSocketHandler captures output
    → CommandContextManager accumulates output
    → On END marker: triggers async AI analysis
    → AiSshAssistantService.analyzeCommandOutput()
    → Analysis result saved to Redis and sent via WebSocket
```

**Command Context Management:**
- `CommandContext` entity: commandId, aiSessionId, sshSessionId, command, output (StringBuilder), status
- `CommandContextManager`: Singleton managing active commands
- `CommandTimeoutScheduler`: Scheduled task checking for command timeouts (1s interval)
- Async analysis thread pool: `AsyncConfig.commandAnalysisExecutor` (corePoolSize=2, maxPoolSize=5)

**Model Configuration:**
- **GLM-4.7**: Via BigModel API (`https://open.bigmodel.cn/api/paas/v4/`)
- **Ollama**: Local models (default: `qwen2.5:7b` at `http://localhost:11434/v1`)
- **Important**: Ollama baseUrl MUST include `/v1` for OpenAI-compatible endpoint
- **Default model**: Set via `ai.monitor-agent.default-model-name` property (default: `ollama`)
- Models must support function calling for SSH assistant tool use (qwen2.5:7b recommended)

### Data Flow
```
Agent (OSHI) → CollectService → ReportService → Server API → MySQL
                                                            ↓
   Browser ← REST API ← MonitorController ← Database Query
```

### Frontend Architecture
- **Route guards** (`router/guards.ts`): Auth check + admin permission check
- **API layer** (`api/*.ts`): Centralized axios wrappers with base URL from `.env`
- **Composables** (`composables/*.ts`): Reusable logic (useAuth, useWebSocket, useTheme)
- **Component structure**: Layout components contain view components, use TypeScript types from `types/`

## Database Schema

Located in `Monitor-Server/src/main/resources/sql/init.sql`

- **user** - User accounts (user_id, username, email, password, role)
- **agent** - Registered agents (agent_id, hostname, ip, cpu_model, gpu_info, network_interfaces)
- **agent_metrics** - Time-series metrics (cpu_percent, memory_percent, disk_usages, network rates, ssh status)
- **ssh_credential** - Saved SSH credentials (encrypted)

## Configuration Files

- **monitor-project/Monitor-Agent/src/main/resources/agent-config.yaml** - Agent registration & reporting intervals
  - `server.endpoints`: List of server addresses
  - `reporting.basicIntervalSec`: Basic info interval (default: 600s)
  - `reporting.metricsIntervalSec`: Metrics interval (default: 15s)
- **monitor-project/Monitor-Server/src/main/resources/application.yaml** - Server config (MySQL, Redis, RabbitMQ, JWT, email, LangChain4j AI)
  - Contains sensitive data (database passwords, email credentials, AI API keys) - should be secured
  - `langchain4j.open-ai.chat-model.api-key`: AI model API key
  - `langchain4j.open-ai.chat-model.base-url`: Model API endpoint
  - `langchain4j.open-ai.chat-model.model-name`: Model name (e.g., glm-4.7)
  - `langchain4j.ollama.chat-model.base-url`: Must be `http://localhost:11434/v1` (include /v1)
  - `langchain4j.ollama.chat-model.model-name`: Model name (e.g., qwen2.5:7b for tool support)
  - `ai.monitor-agent.default-model-name`: Default AI model selection (`ollama` or `open-ai`)
- **monitor-project/Monitor-Web/.env.development** - Dev environment (API: http://localhost:8080/api)
- **monitor-project/Monitor-Server/src/main/resources/sql/init.sql** - Database schema initialization

## Key API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `POST /api/auth/reset-password` - Reset password

### Agent (Server-side)
- `POST /api/v1/customer/register` - Agent registration
- `POST /api/v1/agent/basic` - Basic data report
- `POST /api/v1/agent/metrics` - Metrics report

### Monitor
- `GET /api/monitor/getMonitorList` - Get all monitored hosts
- `GET /api/monitor/stats` - Get system statistics
- `GET /api/monitor/{agentId}/metrics/latest` - Get latest metrics

### SSH
- `GET /api/v1/ssh/credential/{agentId}` - Get SSH credential
- `POST /api/v1/ssh/connect` - Establish SSH connection
- `WS /ws/ssh/terminal/{sessionId}` - SSH terminal WebSocket

### AI Assistant
- **Global Chat**: `POST /api/ai/chat`, `GET /api/ai/sessions`
- **SSH Assistant**: `POST /api/ai/ssh-assistant/connect`, `WS /ws/ai/ssh-assistant/{aiSessionId}`

## Package Structure Reference

### Monitor-Agent
- `service/` - RegisterService, CollectService (OSHI), ReportService (scheduled)
- `config/` - ConfigLoader, RestTemplateConfig
- `BootstrapService.java` - ApplicationRunner for auto-registration on startup

### Monitor-Server
- `controller/` - AgentController, AuthController, MonitorController, SshController, HealthController
- `service/` - AgentService, AgentMetricsService, MonitorService, AuthService, SshService
- `security/` - JwtTokenProvider, JwtAuthenticationFilter, SecurityConfig
- `websocket/` - SshWebSocketHandler, SshSessionManager (SSH proxy)
- `ai/` - AI assistant integration (LangChain4j services)
  - `command/` - CommandContext, CommandStatus, CommandContextManager, CommandTimeoutScheduler
  - `config/` - AgentAssistantConfig, AsyncConfig, AI model properties
  - `context/` - SshSessionContext (ThreadLocal context holder)
  - `entity/` - Assistant interface, SshAssistantMessage, SshSessionBinding, SystemPrompt
  - `service/` - AiSshAssistantService (main service), ChatService (global chat)
  - `tools/` - SshExecuteTool (SSH command execution with @Tool annotation)
  - `utils/` - TerminalChatRedisUtils, ModelRedisUtils
  - `websocket/` - AiSshAssistantHandler, AiSshAssistantManager, WsChatMessage
- `model/entity/` - User, Agent, AgentMetrics, SshCredential (JPA/MyBatis Plus)

### Monitor-Web
- `views/` - auth/, dashboard/, host/, ai/, terminal/, admin/
- `components/` - layout/, monitor/, ai/, common/
- `router/` - index.ts (routes), guards.ts (auth/permission)
- `stores/` - Pinia stores (auth, notification, theme)
- `api/` - axios wrappers for backend endpoints
- `composables/` - useAuth, useWebSocket, useTheme
- `types/` - TypeScript definitions

### CommonLibrary
- `model/BasicInfo.java` - Static hardware info (reported every 10 min)
- `model/Metrics.java` - Runtime metrics (reported every 15 sec)
- `request/`, `response/` - DTOs for Agent-Server communication

## Technology Stack

**Backend:** Java 17, Spring Boot 3.5.10, Spring Security + JWT, MyBatis Plus, MySQL, Redis, RabbitMQ, InfluxDB, OSHI (system monitoring), JSch (SSH), LangChain4j (AI integration)

**Frontend:** Vue 3 (Composition API), TypeScript, Vite, Element Plus, ECharts, xterm.js, Pinia, Vue Router

**Build:** Maven (Java), npm/Vite (Web)

**Testing:** JUnit + Spring Boot Test, Vitest, Playwright

**Node.js Version:** ^20.19.0 || >=22.12.0 (specified in package.json engines)

## Development Constraints

This project follows specific development rules defined in `项目文档/AI开发约束.md`:

1. **Code Verification**: Generate code, verify logic, report to user
2. **Error Handling**: Report problem type/location, request authorization for fixes (max 2 attempts)
3. **Architecture**: Clear structure, modular design, extensible interfaces
4. **Third-party Libraries**: Report purpose and version before introducing
5. **Version Control**: Only commit when explicitly requested by user
6. **Configuration Separation**: Keep config separate from business logic
7. **Backward Compatibility**: Consider compatibility for API/interface changes

## Known Issues & Considerations

- **Circular Dependency**: CommandContextManager → AiSshAssistantService → CommandContextManager
  - Resolved with `@Lazy` annotation on aiSshAssistantService in CommandContextManager
- **HTTP Client Conflict**: LangChain4j detects multiple HTTP client implementations
  - Resolved by explicitly specifying `.httpClientBuilder(new JdkHttpClientBuilder())` in AgentAssistantConfig
- **Ollama baseUrl**: Must include `/v1` path for OpenAI-compatible endpoint
- **Model Selection**: Only models supporting function calling work with SSH assistant (e.g., qwen2.5:7b, not gemma3:4b)
- **AI Analysis Results**: Must be saved to Redis AND sent via WebSocket for complete functionality
