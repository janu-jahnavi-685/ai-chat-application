# TDK AI Chat

A full-stack AI chat application built with **React + Spring Boot**, featuring real-time LLM response streaming, JWT authentication, and persistent conversation history.

---

## Features

- 🔐 JWT-based authentication (signup, login, logout)
- 💬 Real-time AI responses via Server-Sent Events (SSE)
- 📝 Persistent conversation history across sessions
- 🤖 OpenAI GPT-4o-mini integration with conversation context
- 🎨 Polished dark-mode UI with Markdown + code block rendering
- 📱 Responsive design with mobile sidebar drawer
- 🐳 One-command Docker Compose startup
- 🧪 Unit + integration tests (JUnit 5, Vitest, React Testing Library)

---

## Technology Stack

| Layer | Technology |
|---|---|
| Frontend | React 18 + Vite + TypeScript |
| Routing | React Router v6 |
| Styling | CSS Modules |
| HTTP Client | Axios + Fetch (SSE) |
| Backend | Java 21 + Spring Boot 3.4.1 |
| Security | Spring Security + JWT (jjwt) |
| Persistence | Spring Data JPA + Flyway |
| Database | MySQL 8.4 |
| LLM | OpenAI Chat Completions API |
| Streaming | Server-Sent Events (SseEmitter) |
| API Docs | Springdoc OpenAPI (Swagger UI) |
| Backend Tests | JUnit 5 + Mockito |
| Frontend Tests | Vitest + React Testing Library |
| Containerization | Docker + Docker Compose |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Browser                                  │
│  React SPA                                                       │
│  ├── AuthContext (JWT in sessionStorage)                         │
│  ├── REST calls via Axios (/api/auth/*, /api/conversations/*)   │
│  └── SSE stream via fetch() (/api/chat/{id}/stream)             │
└────────────────────────┬────────────────────────────────────────┘
                         │ HTTP / SSE
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Backend                           │
│  JwtAuthenticationFilter → Controller → Service → Repository   │
│  ├── AuthController    /api/auth/**                             │
│  ├── ConversationController /api/conversations/**              │
│  └── ChatController    /api/chat/{id}/stream (SSE)             │
│                                                                  │
│  ChatService (SseEmitter + sseTaskExecutor)                     │
│  └── LlmService → OpenAiClient (WebClient, streaming)          │
└──────────┬──────────────────────────┬───────────────────────────┘
           │ JDBC (Flyway migrations)  │ HTTPS
           ▼                          ▼
     ┌──────────┐              ┌────────────┐
     │ MySQL 8  │              │ OpenAI API │
     └──────────┘              └────────────┘
```

### Backend Layers

| Layer | Responsibility |
|---|---|
| Controller | HTTP request handling, no business logic |
| Service | Business logic, ownership validation |
| Repository | JPA database operations |
| Entity | JPA-mapped database tables |
| DTO | API request/response shapes (records) |
| Security | JWT filter, UserDetailsService |
| Integration | OpenAI WebClient wrapper |
| Exception | @RestControllerAdvice global handler |

---

## Database Schema

```sql
users (id, name, email UNIQUE, password, created_at, updated_at)
  |
  | 1:N
  |
conversations (id, user_id FK, title, created_at, updated_at)
  |
  | 1:N
  |
messages (id, conversation_id FK, role, content, status, created_at)
```

**Indexes:** `users(email)`, `conversations(user_id, updated_at)`, `messages(conversation_id, created_at)`

---

## API Endpoints

### Authentication
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | /api/auth/signup | Public | Register → 201 + JWT |
| POST | /api/auth/login | Public | Login → 200 + JWT |
| GET | /api/auth/me | Required | Current user |

### Conversations
| Method | Endpoint | Description |
|---|---|---|
| POST | /api/conversations | Create |
| GET | /api/conversations | List (user-scoped, newest first) |
| GET | /api/conversations/{id} | Get by ID |
| GET | /api/conversations/{id}/messages | Get messages |
| DELETE | /api/conversations/{id} | Delete |

### Chat
| Method | Endpoint | Description |
|---|---|---|
| POST | /api/chat/{id}/stream | Stream AI response (SSE) |

Swagger UI available at: `http://localhost:8080/swagger-ui.html`

---

## SSE Streaming Flow

```
Frontend                           Backend                        OpenAI
  │                                   │                              │
  │── POST /api/chat/{id}/stream ────▶│                              │
  │   Authorization: Bearer <jwt>     │                              │
  │   Accept: text/event-stream       │                              │
  │                                   │── Persist user message ─────▶│
  │                                   │── Create assistant (FAILED) →│
  │◀─ event: status ─────────────────│   "Analyzing your question.."│
  │                                   │── WebClient POST ────────────▶│
  │                                   │◀─ data: {"delta":"Spring "} ─│
  │◀─ event: token {"content":"Spring"}│                              │
  │◀─ event: token {"content":"Boot"} │                              │
  │                                   │◀─ data: [DONE] ──────────────│
  │                                   │── Update msg → COMPLETED ────│
  │◀─ event: done {"messageId":N} ───│                              │
```

**Key SSE implementation details:**
- Frontend uses `fetch()` (not native EventSource) to support POST + Authorization header
- Custom `SseParser` handles split network chunks — one network delivery ≠ one SSE frame
- Backend runs streaming on `sseTaskExecutor` (off Tomcat threads) via `SseEmitter`
- `proxy_buffering off` in nginx ensures real-time token delivery in Docker

---

## Authentication Flow

1. User submits credentials
2. Backend validates, BCrypt-checks password
3. JWT signed with HS256, configurable expiry (default 1 hour)
4. Token stored in `sessionStorage` (survives page refresh, cleared on tab close)
5. Axios interceptor attaches `Authorization: Bearer <token>` to all requests
6. `JwtAuthenticationFilter` validates token on every protected request
7. User identity always extracted from security context — never trusted from client

---

## Scalability Approach

The backend is fully **stateless**:
- No HTTP session state — JWT is self-contained
- No in-memory conversation cache — all state in MySQL
- Multiple Spring Boot instances can run behind a load balancer
- Each instance independently validates JWT and queries MySQL
- SSE connections are per-instance (each request is handled end-to-end by one instance)

**Infrastructure deliberately kept simple** (modular monolith):
- No Kubernetes, Kafka, Redis, or microservices
- In-memory rate limiting via Bucket4j (suitable for single-instance or low-scale multi-instance)

---

## Security

1. BCrypt password hashing (cost factor 12)
2. JWT with configurable expiry and secret via environment variable
3. Stateless Spring Security — no server sessions
4. Conversation ownership validated on every operation
5. User identity from security context (never client-supplied)
6. Input validation (`@Valid` + Bean Validation)
7. JPA parameterized queries prevent SQL injection
8. CORS restricted to allowed origins via environment variable
9. No secrets in code — all via environment variables
10. Error responses never expose internal details or stack traces
11. Passwords never logged or returned in API responses

---

## Prerequisites

- Docker Desktop 4.x+
- (For local dev) **Java 21** (`C:\Program Files\Java\jdk-21.0.11` on this machine), Maven 3.9+, Node.js 22+, MySQL 8.x

> **Java 21 local setup (Windows):** Java 21 is installed at `C:\Program Files\Java\jdk-21.0.11`. Run `. .\backend\set-java21.ps1` in your terminal to point Maven at it before running `mvn` commands.

---

## Environment Setup

Copy the example file and fill in your values:

```bash
cp .env.example .env
```

Required values to change:

| Variable | Description |
|---|---|
| `MYSQL_PASSWORD` | Database password |
| `MYSQL_ROOT_PASSWORD` | MySQL root password |
| `JWT_SECRET` | ≥32 character random string |
| `OPENAI_API_KEY` | Your OpenAI API key |

---

## Docker Startup (Recommended)

```bash
# Clone and navigate
git clone <repo-url>
cd tdk-ai-chat

# Configure environment
cp .env.example .env
# Edit .env — set OPENAI_API_KEY and strong passwords

# Start everything
docker compose up --build

# Application is ready at:
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

MySQL data persists in the `mysql_data` Docker volume across restarts.

---

## Local Development

### Backend

```bash
cd backend

# Start MySQL locally (or use Docker just for MySQL)
docker run -d --name tdk-mysql -e MYSQL_DATABASE=ai_chat -e MYSQL_USER=app_user \
  -e MYSQL_PASSWORD=change_me -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 mysql:8.4

# Set environment variables
export JWT_SECRET=your-secret-key-at-least-32-chars
export OPENAI_API_KEY=your-openai-key

# Run
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev   # http://localhost:5173
```

The Vite dev server proxies `/api` to `http://localhost:8080`.

---

## Running Tests

### Backend

```bash
cd backend
mvn test
```

### Frontend

```bash
cd frontend
npm install
npm test          # single run
npm run test:watch  # watch mode
```

Tests use H2 in-memory database and mocked OpenAI responses — no real API key needed.

---

## Why Spring Boot + MySQL Instead of Node.js + MongoDB?

The assessment explicitly requires:
- **Java 21 + Spring Boot** — mature enterprise framework, excellent JPA/security/SSE support
- **MySQL** — ACID-compliant relational database ideal for structured chat data with referential integrity (users → conversations → messages)
- **Spring Security** — battle-tested auth with JWT support
- **Spring MVC SseEmitter** — built-in SSE support without reactive complexity

Node.js + MongoDB would be a valid alternative but was explicitly excluded from scope.

---

## Architecture Documentation

See [docs/architecture.md](docs/architecture.md) for detailed component diagrams.
