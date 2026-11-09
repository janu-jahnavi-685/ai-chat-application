import { useCallback, useRef, useState } from 'react';
import { conversationService } from '@/services/conversationService';
import { streamMessage } from '@/services/chatService';
import type { Conversation, Message, SseErrorEvent } from '@/types';

export interface ChatState {
  conversations: Conversation[];
  activeConversation: Conversation | null;
  messages: Message[];
  isLoadingConversations: boolean;
  isLoadingMessages: boolean;
  isStreaming: boolean;
  isThinking: boolean;
  streamingContent: string;
  error: string | null;
}

export function useChat() {
  const [state, setState] = useState<ChatState>({
    conversations: [],
    activeConversation: null,
    messages: [],
    isLoadingConversations: false,
    isLoadingMessages: false,
    isStreaming: false,
    isThinking: false,
    streamingContent: '',
    error: null,
  });

  const abortControllerRef = useRef<AbortController | null>(null);

  const update = useCallback((partial: Partial<ChatState>) => {
    setState((prev) => ({ ...prev, ...partial }));
  }, []);

  // ── Conversations ─────────────────────────────────────────────────────────

  const loadConversations = useCallback(async () => {
    update({ isLoadingConversations: true, error: null });
    try {
      const conversations = await conversationService.list();
      update({ conversations, isLoadingConversations: false });
    } catch {
      update({ isLoadingConversations: false, error: 'Failed to load conversations.' });
    }
  }, [update]);

  const createConversation = useCallback(async () => {
    try {
      const conversation = await conversationService.create({ title: 'New Chat' });
      setState((prev) => ({
        ...prev,
        conversations: [conversation, ...prev.conversations],
        activeConversation: conversation,
        messages: [],
        streamingContent: '',
        error: null,
      }));
      return conversation;
    } catch {
      update({ error: 'Failed to create conversation.' });
      return null;
    }
  }, [update]);

  const selectConversation = useCallback(
    async (conversation: Conversation) => {
      update({
        activeConversation: conversation,
        isLoadingMessages: true,
        messages: [],
        streamingContent: '',
        error: null,
      });
      try {
        const messages = await conversationService.getMessages(conversation.id);
        update({ messages, isLoadingMessages: false });
      } catch {
        update({ isLoadingMessages: false, error: 'Failed to load messages.' });
      }
    },
    [update]
  );

  const deleteConversation = useCallback(
    async (id: number) => {
      try {
        await conversationService.delete(id);
        setState((prev) => {
          const conversations = prev.conversations.filter((c) => c.id !== id);
          const activeConversation =
            prev.activeConversation?.id === id ? null : prev.activeConversation;
          const messages = activeConversation ? prev.messages : [];
          return { ...prev, conversations, activeConversation, messages };
        });
      } catch {
        update({ error: 'Failed to delete conversation.' });
      }
    },
    [update]
  );

  // ── Chat / Streaming ──────────────────────────────────────────────────────

  const sendMessage = useCallback(
    async (content: string, overrideConversationId?: number) => {
      // Use override (from just-created conversation) or fall back to state
      const conversationId = overrideConversationId ?? state.activeConversation?.id;
      if (!conversationId || !content.trim()) return;

      // Optimistically add the user message
      const optimisticUserMsg: Message = {
        id: Date.now(),
        role: 'USER',
        content: content.trim(),
        status: 'COMPLETED',
        createdAt: new Date().toISOString(),
      };

      setState((prev) => ({
        ...prev,
        messages: [...prev.messages, optimisticUserMsg],
        isStreaming: true,
        isThinking: true,
        streamingContent: '',
        error: null,
      }));

      abortControllerRef.current = streamMessage(conversationId, content.trim(), {
        onThinking: () => {
          update({ isThinking: true });
        },

        onToken: ({ content: token }) => {
          setState((prev) => ({
            ...prev,
            isThinking: false,
            streamingContent: prev.streamingContent + token,
          }));
        },

        onDone: async ({ messageId }) => {
          // Replace streaming content with the persisted message
          try {
            const messages = await conversationService.getMessages(conversationId);
            // Also refresh conversation list to update title/timestamp
            const conversations = await conversationService.list();
            setState((prev) => ({
              ...prev,
              messages,
              conversations,
              isStreaming: false,
              isThinking: false,
              streamingContent: '',
              activeConversation:
                conversations.find((c) => c.id === conversationId) ??
                prev.activeConversation,
            }));
          } catch {
            // Fallback: convert streaming content to a message in-place
            const assistantMsg: Message = {
              id: messageId,
              role: 'ASSISTANT',
              content: state.streamingContent,
              status: 'COMPLETED',
              createdAt: new Date().toISOString(),
            };
            setState((prev) => ({
              ...prev,
              messages: [...prev.messages, assistantMsg],
              isStreaming: false,
              isThinking: false,
              streamingContent: '',
            }));
          }
        },

        onError: (err) => {
          const message =
            err instanceof Error
              ? err.message
              : (err as SseErrorEvent).message ?? 'Unable to generate response.';
          setState((prev) => ({
            ...prev,
            isStreaming: false,
            isThinking: false,
            streamingContent: '',
            error: message,
          }));
        },
      });
    },
    [state.activeConversation, state.streamingContent, update]
  );

  const cancelStreaming = useCallback(() => {
    abortControllerRef.current?.abort();
    update({ isStreaming: false, isThinking: false, streamingContent: '' });
  }, [update]);

  const clearError = useCallback(() => update({ error: null }), [update]);

  return {
    ...state,
    loadConversations,
    createConversation,
    selectConversation,
    deleteConversation,
    sendMessage,
    cancelStreaming,
    clearError,
  };
}
