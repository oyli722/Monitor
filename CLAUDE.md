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

```bash
# Build a module (build CommonLibrary first as it's a dependency)
cd CommonLibrary && mvn clean install
cd Monitor-Agent && mvn clean install
cd Monitor-Server && mvn clean install

# Run tests
mvn test

# Run Spring Boot application
mvn spring-boot:run
```

### Web Module (npm/Vite)

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

# Lint
npm run lint

# Unit tests (Vitest)
npm run test:unit

# E2E tests (Playwright)
npm run test:e2e
npx playwright install  # First time only
```

## Startup Sequence

1. Start MySQL (port 3306) - Create database using `Monitor-Server/src/main/resources/sql/init.sql`
2. Start Redis (port 6379)
3. Start RabbitMQ (port 5672)
4. Build CommonLibrary: `cd CommonLibrary && mvn clean install`
5. Start Monitor-Server: `cd Monitor-Server && mvn spring-boot:run`
6. Start Monitor-Agent: `cd Monitor-Agent && mvn spring-boot:run`
7. Start Monitor-Web: `cd Monitor-Web && npm run dev`

## Key Architecture Patterns

### Agent-Server Communication
- Agents register with server on startup via `POST /api/v1/customer/register`
- Dual-frequency reporting:
  - Basic data (hardware info via `BasicInfo`): every 10 minutes
  - Metrics (CPU, memory, disk, network via `Metrics`): every 15 seconds
- Server can push config updates to agents via `POST /api/v1/agent/config`

### Authentication
- JWT-based stateless authentication (Spring Security + JwtTokenProvider)
- Role-based access control (admin/user)
- Email verification for registration/password reset

### SSH Terminal Proxy
- Browser connects to Server via WebSocket (`WS /ws/ssh/terminal/{sessionId}`)
- Server maintains SSH session to Agent host via JSch
- Terminal I/O relayed through WebSocket
- Credentials stored encrypted in `ssh_credential` table

### Data Flow
```
Agent (OSHI) → CollectService → ReportService → Server API → MySQL
                                                            ↓
   Browser ← REST API ← MonitorController ← Database Query
```

## Database Schema

Located in `Monitor-Server/src/main/resources/sql/init.sql`

- **user** - User accounts (user_id, username, email, password, role)
- **agent** - Registered agents (agent_id, hostname, ip, cpu_model, gpu_info, network_interfaces)
- **agent_metrics** - Time-series metrics (cpu_percent, memory_percent, disk_usages, network rates)
- **ssh_credential** - Saved SSH credentials (encrypted)

## Configuration Files

- **Monitor-Agent/src/main/resources/agent-config.yaml** - Agent registration & reporting intervals
- **Monitor-Server/src/main/resources/application.yaml** - Server config (MySQL, Redis, RabbitMQ, JWT, email)
- **Monitor-Web/.env.development** - Dev environment (API: http://localhost:8080/api)

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

## Technology Stack

**Backend:** Java 17, Spring Boot 3.5.10, Spring Security + JWT, MyBatis Plus, MySQL, Redis, RabbitMQ, OSHI (system monitoring), JSch (SSH), Resilience4j

**Frontend:** Vue 3 (Composition API), TypeScript, Vite, Element Plus, ECharts, xterm.js, Pinia

**Build:** Maven (Java), npm/Vite (Web)

**Testing:** JUnit + Spring Boot Test, Vitest, Playwright
