# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Server Operations Monitoring System** (服务器运维监控系统) with a client-server architecture for centralized server monitoring, web-based SSH terminal access, and AI-assisted operations automation.

### Architecture Components

1. **Monitor-Agent** (`monitor-project/Monitor-Agent/`) - Agent deployed on monitored hosts
2. **Monitor-Server** (`monitor-project/Monitor-Server/`) - Central server for data processing and API services
3. **Monitor-Web** (`monitor-project/Monitor-Web/`) - Vue 3 frontend console
4. **CommonLibrary** (`monitor-project/CommonLibrary/`) - Shared DTOs and models

## Build & Run Commands

### Server (Monitor-Server)
```bash
cd monitor-project/Monitor-Server
mvn spring-boot:run
```
- Runs on port 8080 by default
- Requires: MySQL (localhost:3306), Redis (localhost:6379), RabbitMQ (localhost:5672)
- Build: `mvn clean package`

### Agent (Monitor-Agent)
```bash
cd monitor-project/Monitor-Agent
mvn spring-boot:run
```
- Runs on port 8081 by default
- Configuration: `src/main/resources/agent-config.yaml`
- Build: `mvn clean package`

### Web Frontend (Monitor-Web)
```bash
cd monitor-project/Monitor-Web
npm install          # First time setup
npm run dev          # Development server with hot-reload
npm run build        # Production build
npm run lint         # ESLint with auto-fix
```
- Dev server: Vite (default port varies)
- Node engine: ^20.19.0 || >=22.12.0

## Technology Stack

### Backend
- **Java 17** + **Spring Boot 3.5.10**
- **OSHI 6.x** (Agent) - Cross-platform hardware monitoring
- **MyBatis-Plus 3.5.15** - Database ORM
- **InfluxDB Java Client 6.6.0** - Time-series data storage
- **JSch 0.1.55** - SSH connections
- **WebSocket** - Real-time SSH terminal streaming
- **Spring Security + JWT** - Authentication/authorization
- **RabbitMQ** - Message queue for async tasks (email sending)
- **OpenFeign** (Agent) - HTTP client for server communication

### Frontend
- **Vue 3** with Composition API
- **TypeScript**
- **Vite** - Build tool
- **Element Plus** - UI component library
- **ECharts** - Data visualization
- **Pinia** - State management
- **Vue Router** - Routing
- **xterm.js** - Web-based SSH terminal

## Architecture & Data Flow

### Agent Registration & Reporting Flow
1. Agent startup checks `agent-config.yaml` for existing `agent.id`
2. If not registered, Agent calls `POST /api/v1/customer/register` with hostname, IP, and basic hardware info
3. Server responds with `agent_id`, `agent_name`, and `auth_token`
4. Agent saves these to local config

### Dual-Frequency Data Reporting
- **Basic Data** (every 10 min): Static hardware info via `POST /api/v1/agent/basic`
- **Metrics Data** (every 15 sec): CPU/memory/disk/network via `POST /api/v1/agent/metrics`
- Use `@Scheduled` annotations in `ReportService.java`

### SSH WebSocket Architecture
- Frontend connects to `WS /api/v1/ssh/terminal/{sessionId}`
- `SshWebSocketHandler` bridges WebSocket and SSH session via JSch
- `SshSessionManager` manages active SSH connections
- Data flows: WebSocket -> SSH shell input; SSH output -> WebSocket

### Authentication Flow
1. User login via `POST /api/v1/auth/login` → JWT token
2. Frontend stores token and includes in `Authorization: Bearer` header
3. `JwtAuthenticationFilter` validates token on protected endpoints
4. Spring Security handles role-based access control

## Key Configuration Files

- **Server**: `monitor-project/Monitor-Server/src/main/resources/application.yaml`
  - Database, Redis, RabbitMQ, mail settings, JWT secret
- **Agent**: `monitor-project/Monitor-Agent/src/main/resources/agent-config.yaml`
  - Server endpoints, agent credentials, reporting intervals
- **Agent application**: `monitor-project/Monitor-Agent/src/main/resources/application.yaml`
  - Agent HTTP port, config file path

## Important API Endpoints

### Agent APIs
- `POST /api/v1/customer/register` - Agent registration
- `POST /api/v1/agent/basic` - Basic hardware data report
- `POST /api/v1/agent/metrics` - Runtime metrics report
- `POST /api/v1/agent/config` - Config updates from server

### Auth APIs
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/forgot-password` - Password reset request

### Monitor APIs
- `GET /api/v1/monitor/agents` - List all agents
- `GET /api/v1/monitor/agents/{agentId}/basic` - Agent basic info
- `GET /api/v1/monitor/agents/{agentId}/metrics` - Latest metrics
- `GET /api/v1/monitor/agents/{agentId}/metrics/history` - Historical metrics

### SSH APIs
- `POST /api/v1/ssh/connect` - Establish SSH connection (returns sessionId)
- `POST /api/v1/ssh/disconnect` - Close SSH connection
- `POST /api/v1/ssh/command` - Send command to SSH session
- `POST /api/v1/ssh/credential` - Save SSH credentials
- `GET /api/v1/ssh/credential/{agentId}` - Get saved SSH credentials
- `WS /api/v1/ssh/terminal/{sessionId}` - WebSocket for interactive terminal

## Database Schema (Key Tables)

- **agent** - Registered agents with basic hardware info
- **agent_metrics** - Time-series metrics (also stored in InfluxDB)
- **ssh_credential** - Encrypted SSH credentials for agents
- **user** - User accounts

## Frontend Structure

- `/src/views/auth/` - Login, register, forgot password
- `/src/views/dashboard/` - Main dashboard with host overview
- `/src/views/host/` - Host monitoring with metrics charts
- `/src/views/ai/` - AI assistant for natural language operations
- `/src/views/admin/` - User management, system settings
- `/src/components/` - Reusable components (layout, monitors, auth)

## Development Notes

- The **CommonLibrary** module contains shared request/response DTOs used by both Agent and Server
- Agent uses **RestTemplate** for HTTP calls (configured in `RestTemplateConfig.java`)
- Server uses **InfluxDB** for time-series metrics storage; MySQL for agent/user metadata
- SSH terminal uses **xterm.js** on frontend with **WebSocket** proxying to JSch SSH sessions
- Credentials are stored in `ssh_credential` table (encryption is TODO - see `SshServiceImpl.java:169`)
- Agent registration is one-time; credentials persist in `agent-config.yaml`
- Server includes flow limiting and email verification for auth (see `FlowUtils.java`, `MailQueueListener.java`)
