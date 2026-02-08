# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a service operations monitoring system (服务运维监控系统) with a client-server architecture. The system provides host monitoring, data visualization, web SSH terminal, and AI assistant capabilities.

**Technology Stack:**
- **Backend**: Spring Boot 3.5.10 (Java 17), Maven multi-module project
- **Frontend**: Vue 3 + TypeScript + Vite, Element Plus UI
- **Databases**: MySQL (primary), Redis (cache/sessions), RabbitMQ (messaging)
- **AI**: LangChain4j with GLM-4.7 and Ollama models
- **Monitoring**: OSHI for system information collection

## Module Architecture

```
Monitor/
├── CommonLibrary/     # Shared entities and DTOs
├── Monitor-Agent/     # Agent for monitored hosts (port 8081)
├── Monitor-Server/    # Core server (port 8080)
├── Monitor-AI/        # AI assistant service (port 8081) - Spring WebFlux (reactive)
└── Monitor-Web/       # Vue 3 frontend
```

**Important:** Monitor-AI uses Spring WebFlux (reactive, non-blocking) while Monitor-Server uses Spring Web MVC (blocking).

## Build Commands

### Backend (Maven)

```bash
# Build all modules from parent directory
cd monitor-project
mvn clean install

# Build specific module
mvn clean install -pl Monitor-Server
mvn clean install -pl Monitor-Agent
mvn clean install -pl Monitor-AI
mvn clean install -pl CommonLibrary

# Run applications
java -jar Monitor-Server/target/Monitor-Server-0.0.1-SNAPSHOT.jar
java -jar Monitor-Agent/target/Monitor-Agent-0.0.1-SNAPSHOT.jar
java -jar Monitor-AI/target/Monitor-AI-0.0.1-SNAPSHOT.jar
```

### Frontend (npm)

```bash
cd Monitor-Web

# Install dependencies
npm install

# Development server
npm run dev

# Build for production
npm run build

# Type checking
npm run type-check

# Linting
npm run lint

# Run tests
npm run test:unit
npm run test:e2e
```

## Architecture Patterns

### Dual AI Assistant Pattern

1. **Sidebar AI Assistant** (Monitor-AI, /api/chat/*): General-purpose AI chat with session management
2. **SSH-Bound AI Assistant** (Monitor-Server, /api/ai/ssh-assistant): Integrated with SSH terminal for command execution

### Communication Patterns

- **Agent → Server**: HTTP REST for registration and reporting
- **Frontend → Server**: HTTP REST + WebSocket (for SSH)
- **Frontend → AI**: HTTP REST + SSE (Server-Sent Events for streaming)
- **AI → Server**: Feign client (Spring Cloud OpenFeign)

### Data Flow

```
Agent Registration: Bootstrap → RegisterService → Server
Data Reporting: Scheduled tasks (15s metrics, 10min basic) → Server
SSH Session: Frontend → WebSocket → Server → JSch → Host
```

## Key Components

### Monitor-Agent (Port 8081)

- **BootstrapService**: Handles agent registration on startup
- **RegisterService**: Registers agent with server (health check for fastest server)
- **ReportService**: Scheduled data reporting (basic: 10min, metrics: 15sec)
- **CollectService**: Uses OSHI to collect system information

### Monitor-Server (Port 8080)

**Controllers:**
- `AgentController`: Agent registration and data reporting
- `AgentApiController`: Agent API for AI service calls
- `AuthController`: User authentication and token management
- `MonitorController`: Monitoring data queries
- `SshController`: SSH connection management
- `AiSshAssistantController`: SSH-bound AI assistant

**Services:**
- `AgentService`: Agent management
- `AgentMetricsService`: Metrics storage and historical queries
- `AuthService`: JWT authentication
- `SshService`: SSH connection and credential management
- `AiSshAssistantService`: SSH-bound AI assistant service

**WebSocket Handlers:**
- `SshWebSocketHandler`: SSH WebSocket handling
- `SshSessionManager`: SSH session management

### Monitor-AI (Port 8081) - Reactive

**Controller:**
- `ChatController`: 8 endpoints for chat sessions and messages

**Service:**
- `ChatService`: Chat service logic with reactive streams (returns `Mono<T>` and `Flux<T>`)

**Feign Client:**
- `AgentClient`: Calls Monitor-Server APIs

### Frontend (Monitor-Web)

**Key Directories:**
- `api/`: API interfaces (ai.ts, auth.ts, monitor.ts)
- `components/`: Vue components (ai/, auth/, monitor/, etc.)
- `stores/`: Pinia state management
- `utils/`: HTTP clients (request.ts for Server, ai-request.ts for AI)
- `views/`: Page views

**Environment:**
- Development API endpoints in `.env.development`:
  - `VITE_API_BASE_URL=http://localhost:8080/api`
  - `VITE_AI_API_BASE_URL=http://localhost:8081/api`

## Authentication

- JWT-based authentication shared between Server and AI modules
- Token expiration: 24 hours
- Both services use Spring Security (Reactive for Monitor-AI)
- Frontend stores token in localStorage and sends via `Authorization: Bearer {token}` header

## Important Development Notes

### Reactive Programming (Monitor-AI)

Monitor-AI uses Spring WebFlux. Key differences from Monitor-Server:
- Return types: `Mono<T>` (single value) and `Flux<T>` (stream)
- Non-blocking I/O
- Reactive Redis operations
- Never block in reactive streams (avoid `block()`, `blockFirst()`, `blockLast()`)

### Scheduled Tasks

- Agent metrics reporting: Every 15 seconds
- Agent basic info reporting: Every 10 minutes

### CORS Configuration

Allowed origins: `localhost:5173`, `127.0.0.1:5173`, `localhost:8080`

### Known Issues

1. SSH passwords stored in plaintext (needs encryption)
2. InfluxDB dependency added but not used (planned for time-series data)
3. User management API missing

## Package Structure

Base package: `com.hundred.monitor.{module}`

Standard layers:
- controller
- service
- model/entity
- config
- utils

## API Design

- RESTful endpoints with `/api` prefix
- Consistent response format using `BaseResponse` from CommonLibrary
- JWT authentication via `Authorization: Bearer {token}` header

## Development Setup Prerequisites

- Java 17+
- Maven 3.x
- Node.js 20.19.0+ or 22.12.0+
- MySQL 8.x
- Redis 7.x
- RabbitMQ 3.x

## Database Initialization

Run the init script: `Monitor-Server/src/main/resources/sql/init.sql`

## Important Documentation

See `项目文档/` (in Chinese) for:
- `开发文档.md`: Comprehensive development guide
- `AI开发约束.md`: AI development constraints
- `AI模块优化方案.md`: AI module optimization plan
- `功能文档.md`: Feature documentation

## Development Constraints

Per `项目文档/AI开发约束.md`:

1. Verify code logic after generation and report to user
2. For compilation errors: report problem type/location, ask for authorization to fix (max 2 attempts)
3. Architectural design: clear structure, modular design, report module-level changes
4. When user reports bugs: verify user's description, show thought process, maintain code traceability
5. Introduce third-party libraries only after reporting purpose/version and getting approval
6. Only commit when explicitly requested by user
7. Keep configuration separate from code logic
8. Maintain backward compatibility for API changes
