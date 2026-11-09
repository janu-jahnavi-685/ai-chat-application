# TDK AI Chat

A full-stack AI chat application built with **React + Spring Boot**, featuring real-time LLM response streaming, JWT authentication, and persistent conversation history.

---

## Features

- 🔐 JWT-based authentication (signup, login, logout)
- 💬 Real-time AI responses via Server-Sent Events (SSE)
- 📝 Persistent conversation history across sessions
- 🤖 OpenAI-compatible LLM integration (Groq or OpenAI) with conversation context
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
| Database | MySQL 8.0 |
| LLM | OpenAI-compatible Chat Completions API (Groq / OpenAI) |
| Streaming | Server-Sent Events (SseEmitter) |
| API Docs | Springdoc OpenAPI (Swagger UI) |
| Backend Tests | JUnit 5 + Mockito + WireMock |
| Frontend Tests | Vitest + React Testing Library + MSW |
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
     ┌──────────┐              ┌─────────┐
     │ MySQL 8  │              │ LLM API │
     └──────────┘              └─────────┘
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
| Integration | OpenAI-compatible WebClient wrapper |
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

Swagger UI available at: `http://localhost:9600/swagger-ui.html`

---

## SSE Streaming Flow

```
Frontend                           Backend                        LLM API
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

**Infrastructure deliberately kept simple** (modular monolith) — no Kubernetes, Kafka, Redis, or microservices.

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
| `OPENAI_API_KEY` | Your LLM provider API key |
| `OPENAI_BASE_URL` | OpenAI-compatible API base URL (Groq or OpenAI) |
| `OPENAI_MODEL` | Model name (e.g. `llama-3.1-8b-instant`, `gpt-4o-mini`) |

The `.env.example` ships with Groq defaults (`https://api.groq.com/openai/v1` + `llama-3.1-8b-instant`); OpenAI values are shown commented out.

---

## Docker Startup (Recommended)

```bash
# Clone and navigate
git clone <repo-url>
cd ai-chat-application

# Configure environment
cp .env.example .env
# Edit .env — set OPENAI_API_KEY and strong passwords

# Start everything
docker compose up --build

# Application is ready at:
# Frontend: http://localhost:5000
# Backend API: http://localhost:9600
# Swagger UI: http://localhost:9600/swagger-ui.html
```

MySQL data persists in the `mysql_data` Docker volume across restarts.

> A production override (`docker-compose.prod.yml`) pulls pre-built images from GitHub Container Registry instead of building locally:
> `docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d`

---

## Local Development

### Backend

```bash
cd backend

# Start MySQL locally (or use Docker just for MySQL)
docker run -d --name tdk-mysql -e MYSQL_DATABASE=ai_chat -e MYSQL_USER=app_user \
  -e MYSQL_PASSWORD=change_me -e MYSQL_ROOT_PASSWORD=change_me_root -p 3306:3306 mysql:8.0

# Set environment variables
export JWT_SECRET=your-secret-key-at-least-32-chars
export OPENAI_API_KEY=your-llm-api-key
export OPENAI_BASE_URL=https://api.groq.com/openai/v1   # or https://api.openai.com/v1
export OPENAI_MODEL=llama-3.1-8b-instant                # or gpt-4o-mini

# Run
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev   # http://localhost:5173
```

In local dev the frontend reads `VITE_API_BASE_URL` from `.env` (set to `http://localhost:9600/api` in `.env.example`), so both Axios REST calls and `fetch()` SSE streaming reach the backend on port 9600 directly.

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

Backend tests use an H2 in-memory database (MySQL mode) and WireMock to stub the LLM endpoint — no real API key needed. Frontend tests use MSW to mock API responses.

---

## Why Spring Boot + MySQL Instead of Node.js + MongoDB?

The assessment explicitly requires:
- **Java 21 + Spring Boot** — mature enterprise framework, excellent JPA/security/SSE support
- **MySQL** — ACID-compliant relational database ideal for structured chat data with referential integrity (users → conversations → messages)
- **Spring Security** — battle-tested auth with JWT support
- **Spring MVC SseEmitter** — built-in SSE support without reactive complexity

Node.js + MongoDB would be a valid alternative but was explicitly excluded from scope.

The LLM layer is intentionally provider-agnostic: `OpenAiClient` talks to any OpenAI-compatible Chat Completions endpoint, so switching between Groq and OpenAI is a matter of changing `OPENAI_BASE_URL` and `OPENAI_MODEL` — no code changes required.

---

## Architecture Documentation

See [docs/architecture.md](docs/architecture.md) for detailed component diagrams.
