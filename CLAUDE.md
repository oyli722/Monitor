# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **multi-module Maven microservices monitoring system** for service operations. The system collects system metrics from monitored hosts, provides SSH terminal access, and includes AI assistant capabilities.

**Technology Stack:**
- **Backend**: Java 17, Spring Boot 3.5.10, Spring WebFlux (AI module)
- **Frontend**: Vue 3.5.26 + TypeScript, Vite 7.3.1, Element Plus
- **Database**: MySQL 8.0+ (with MyBatis-Plus 3.5.15)
- **Cache**: Redis 6.0+
- **Message Queue**: RabbitMQ 3.8+
- **AI**: LangChain4j 1.0.0-beta3 (GLM-4.7, Ollama)

## Module Architecture

```
monitor-project/
├── CommonLibrary/          # Shared entities and DTOs
├── Monitor-Agent/          # Client agent (deployed on monitored hosts, port 8081)
├── Monitor-Server/         # Core server (port 8080)
├── Monitor-AI/             # AI assistant service (port 8081)
└── Monitor-Web/            # Vue 3 frontend
```

### Key Architecture Patterns

1. **Dual-Frequency Reporting**: Agent reports basic info every 10 minutes, metrics every 15 seconds
2. **WebSocket SSH Proxy**: Browser cannot connect directly to SSH, requires server relay
3. **Reactive AI Service**: Monitor-AI uses Spring WebFlux for non-blocking streaming responses
4. **JWT Authentication**: Stateless auth across microservices
5. **Dual AI Assistants**: Sidebar AI (Monitor-AI) and SSH-bound AI (Monitor-Server)

## Build and Run Commands

### Backend

```bash
cd monitor-project

# Build all modules
mvn clean install

# Build specific module
mvn clean install -pl Monitor-Server

# Run Agent (on monitored host)
cd Monitor-Agent && mvn spring-boot:run

# Run Server
cd Monitor-Server && mvn spring-boot:run

# Run AI Service
cd Monitor-AI && mvn spring-boot:run
```

### Frontend

```bash
cd Monitor-Web
npm install
npm run dev        # Development (port 5173)
npm run build      # Production build
```

### Database Initialization

```bash
# Run init script
mysql -u root -p < Monitor-Server/src/main/resources/sql/init.sql
```

## Service Communication

| From | To | Protocol | Purpose |
|------|-----|----------|---------|
| Agent | Server | HTTP REST | Registration, reporting |
| Frontend | Server | HTTP REST + JWT | API calls |
| Frontend | AI Service | HTTP REST | AI chat |
| AI Service | Server | OpenFeign | Agent data queries |
| Frontend | Server | WebSocket | SSH terminal |

## Common Development Tasks

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl Monitor-AI

# Run single test class
mvn test -Dtest=ChatServiceTest -pl Monitor-AI
```

### Working with Reactive Code (Monitor-AI)

Monitor-AI uses Spring WebFlux. Key patterns:
- Return types: `Mono<T>` (single value) or `Flux<T>` (stream)
- Use `.flatMap()` for async composition, not `.map()`
- Never block: avoid `.block()`, `.blockFirst()`, `.blockLast()`
- Use `.subscribe()` for fire-and-forget operations in side effects

Example:
```java
public Flux<String> sendMessage(...) {
    return redisUtils.sessionExists(sessionId)
        .flatMapMany(exists -> {
            if (Boolean.FALSE.equals(exists)) {
                return Mono.error(new ChatSessionNotFoundException(sessionId));
            }
            return callAI(...);
        });
}
```

### Adding New API Endpoints

1. **Monitor-Server**: Add controller in `controller/` package, register in `SecurityConfig` if needed
2. **Monitor-AI**: Add reactive endpoint in `ChatController.java` returning `Mono<>` or `Flux<>`
3. **Frontend**: Add API function in `src/api/ai.ts` (AI) or `src/utils/request.ts` (Server)

### JWT Authentication

JWT tokens are validated in `JwtAuthenticationFilter` (Server) and `JwtUtil` (AI).

Default secret and expiration are in `application.yaml`. Use `Authorization: Bearer <token>` header.

## Important File Locations

| Purpose | Location |
|---------|----------|
| Database init script | `Monitor-Server/src/main/resources/sql/init.sql` |
| Agent config | `Monitor-Agent/src/main/resources/agent-config.yaml` |
| AI model config | `Monitor-AI/src/main/java/com/hundred/monitor/ai/config/AIModelConfig.java` |
| Security config | `Monitor-Server/src/main/java/com/hundred/monitor/server/conf/SecurityConfig.java` |
| SSH WebSocket | `Monitor-Server/src/main/java/com/hundred/monitor/server/websocket/SshWebSocketHandler.java` |

## Known Issues and TODOs

1. **SSH credentials stored in plaintext** - `SshServiceImpl.java:175` needs encryption
2. **InfluxDB dependency unused** - Included but not implemented; currently using MySQL for time-series data
3. **WebSocket heartbeat incomplete** - Terminal resize and heartbeat support need completion
4. **User management APIs missing** - Frontend exists but backend APIs are not implemented

## Reactive Stack Notes (Monitor-AI)

The Monitor-AI module has been refactored to use reactive programming:

- **Exception Handling**: Use `GlobalExceptionHandler` with custom exceptions from `exception/` package
- **Constants**: Use `ChatConstants` and `ErrorConstants` instead of magic numbers/strings
- **Redis Operations**: Use `ChatSessionRedisReactiveUtils` for all reactive Redis operations
- **JWT Parsing**: Use `JwtUtil` for token validation and user ID extraction

## Port Assignments

- **Monitor-Server**: 8080
- **Monitor-Agent**: 8081 (on each monitored host)
- **Monitor-AI**: 8081
- **Frontend Dev Server**: 5173

Note: Monitor-Agent and Monitor-AI both use port 8081 but run on different machines (Agent on monitored hosts, AI service with Server).
