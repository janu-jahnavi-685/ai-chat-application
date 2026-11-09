# TDK AI Chat — Architecture Documentation

## Component Overview

```
tdk-ai-chat/
├── frontend/        React 18 + TypeScript + Vite
├── backend/         Java 21 + Spring Boot 3.4
├── docs/            Architecture docs
├── docker-compose.yml
└── .env.example
```

## Backend Package Structure

```
com.tdk.aichat
├── AiChatApplication.java          Entry point
├── config/
│   ├── SecurityConfig.java         Spring Security filter chain (stateless JWT)
│   ├── CorsConfig.java             CORS origin/header policy
│   ├── JwtProperties.java          JWT secret + expiry from env
│   ├── OpenAiConfig.java           WebClient bean + OpenAI model config
│   ├── AsyncConfig.java            sseTaskExecutor thread pool
│   └── JpaAuditingConfig.java      @CreatedDate / @LastModifiedDate
├── controller/
│   ├── AuthController.java         POST /signup, POST /login, GET /me
│   ├── ConversationController.java CRUD /api/conversations/**
│   └── ChatController.java         POST /api/chat/{id}/stream → SseEmitter
├── service/
│   ├── AuthService.java            Signup (BCrypt), login (AuthManager), me
│   ├── ConversationService.java    CRUD with ownership enforcement
│   ├── ChatService.java            Streaming orchestrator
│   └── LlmService.java             Context builder + OpenAI delegation
├── repository/                     Spring Data JPA interfaces
├── entity/                         JPA-mapped tables (User, Conversation, Message)
├── dto/request/                    Input validation records
├── dto/response/                   API response records
├── security/
│   ├── JwtService.java             Sign/validate/extract JWT (HS256)
│   ├── JwtAuthenticationFilter.java Bearer token extraction + security context
│   └── CustomUserDetailsService.java UserDetailsService by email
├── integration/openai/
│   ├── OpenAiClient.java           WebClient wrapper (streaming)
│   ├── OpenAiRequest.java          Request DTO (model, messages, stream:true)
│   └── OpenAiStreamHandler.java    Parse "data: {...}" lines, extract delta.content
├── exception/
│   ├── GlobalExceptionHandler.java @RestControllerAdvice with consistent error shape
│   └── [custom exceptions]         ResourceNotFound, Unauthorized, BadRequest, LlmError
└── enums/
    ├── MessageRole.java             USER, ASSISTANT, SYSTEM
    └── MessageStatus.java           COMPLETED, FAILED
```

## Frontend Structure

```
src/
├── types/index.ts               Shared TypeScript interfaces
├── utils/sseParser.ts           Stateful SSE line buffer + frame parser
├── services/
│   ├── api.ts                   Axios instance (JWT interceptor, 401 redirect)
│   ├── authService.ts           signup / login / me
│   ├── conversationService.ts   CRUD conversations + messages
│   └── chatService.ts           fetch()-based SSE stream + AbortController
├── context/AuthContext/         AuthProvider (user, token, login, logout)
├── hooks/
│   ├── useAuth/                 Auth actions + error state
│   └── useChat/                 Conversation + streaming state management
├── routes/ProtectedRoute.tsx    ProtectedRoute + PublicRoute
├── components/
│   ├── common/                  Button, Input, Loader
│   ├── auth/                    LoginForm, SignupForm
│   └── chat/                    ChatSidebar, ChatHeader, ChatWindow,
│                                MessageBubble, StreamingBubble,
│                                ChatInput, ThinkingIndicator
├── pages/
│   ├── LoginPage/               Renders LoginForm
│   ├── SignupPage/               Renders SignupForm
│   └── ChatPage/                Full chat layout (sidebar + main + input)
├── App.tsx                      Router + AuthProvider
└── main.tsx                     React root
```

## Data Flow — Streaming Chat

```
User types message → ChatInput.handleSend()
  → ChatPage.handleSend()
    → useChat.sendMessage()
      1. Optimistic user message added to UI
      2. chatService.streamMessage(conversationId, message, callbacks)
         → fetch(POST /api/chat/{id}/stream, {Authorization, body})
           → SseParser.parse(chunk) on each ReadableStream chunk
           → callbacks.onThinking  → isThinking = true
           → callbacks.onToken     → streamingContent accumulates
           → callbacks.onDone      → reload messages from API
           → callbacks.onError     → show error banner

Backend (on sseTaskExecutor thread):
  ChatController.stream() → new SseEmitter(5min)
    → chatService.handleStreamingChat()
      1. Validate JWT + ownership
      2. Persist user message (COMPLETED)
      3. Auto-title if first message
      4. Create assistant message (FAILED placeholder)
      5. Send event:status "Analyzing..."
      6. llmService.streamResponse()
         → Build context (last 20 COMPLETED messages + system prompt)
         → OpenAiClient.streamCompletion(request, onToken, onComplete, onError)
            → WebClient POST https://api.openai.com/v1/chat/completions (stream:true)
            → Per line: OpenAiStreamHandler.extractToken()
            → onToken callback → SseEmitter.send(event:token)
            → onComplete callback → update assistant to COMPLETED → send event:done
            → onError callback → send event:error
```

## Horizontal Scaling

```
                    Load Balancer
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
     Backend-1       Backend-2       Backend-3
          │              │              │
          └──────────────┼──────────────┘
                         │
                    MySQL (shared)
```

Each backend instance:
- Validates JWT independently (shared secret via env var)
- Reads/writes all state from/to MySQL — no in-memory state
- Handles its own SSE connections end-to-end
- Does not require sticky sessions

## Security Architecture

```
Request → JwtAuthenticationFilter
  → Extract "Bearer <token>" from Authorization header
  → JwtService.extractSubject() → email
  → CustomUserDetailsService.loadUserByUsername() → UserDetails
  → JwtService.isTokenValid() → set SecurityContext
  → Controller receives @AuthenticationPrincipal UserDetails

All services:
  → userRepository.findByEmail(userDetails.getUsername())
  → conversationRepository.findByIdAndUserId(id, user.getId())
  ← Never trust a user ID from the request body
```
