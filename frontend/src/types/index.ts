// ── Auth ─────────────────────────────────────────────────────────────────────

export interface User {
  id: number;
  name: string;
  email: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: User;
}

export interface SignupRequest {
  name: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

// ── Conversations ─────────────────────────────────────────────────────────────

export interface Conversation {
  id: number;
  title: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateConversationRequest {
  title: string;
}

// ── Messages ──────────────────────────────────────────────────────────────────

export type MessageRole = 'USER' | 'ASSISTANT';
export type MessageStatus = 'COMPLETED' | 'FAILED';

export interface Message {
  id: number;
  role: MessageRole;
  content: string;
  status: MessageStatus;
  createdAt: string;
}

// ── Chat Streaming ────────────────────────────────────────────────────────────

export interface SseStatusEvent {
  status: string;
  message: string;
}

export interface SseTokenEvent {
  content: string;
}

export interface SseDoneEvent {
  messageId: number;
  status: string;
}

export interface SseErrorEvent {
  code: string;
  message: string;
}

export type SseEventType = 'status' | 'token' | 'done' | 'error';

export interface ParsedSseEvent {
  event: SseEventType;
  data: string;
}

// ── API Errors ────────────────────────────────────────────────────────────────

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}
