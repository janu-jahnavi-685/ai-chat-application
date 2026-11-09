import { useEffect, useRef } from 'react';
import type { Message } from '@/types';
import { MessageBubble, StreamingBubble } from '@/components/chat/MessageBubble';
import { ThinkingIndicator } from '@/components/chat/ThinkingIndicator';
import { Loader } from '@/components/common/Loader';
import styles from './ChatWindow.module.css';

// Fix: remove unused onNewChat prop
interface ChatWindowProps {
  messages: Message[];
  streamingContent: string;
  isThinking: boolean;
  isStreaming: boolean;
  isLoadingMessages: boolean;
  error: string | null;
  onClearError: () => void;
}

export function ChatWindow({
  messages,
  streamingContent,
  isThinking,
  isStreaming,
  isLoadingMessages,
  error,
  onClearError,
}: ChatWindowProps) {
  const bottomRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const isAtBottomRef = useRef(true);

  // Track if user is near the bottom
  const handleScroll = () => {
    const el = containerRef.current;
    if (!el) return;
    const threshold = 100;
    isAtBottomRef.current =
      el.scrollHeight - el.scrollTop - el.clientHeight < threshold;
  };

  // Auto-scroll to bottom when new content arrives, but only if already at bottom
  useEffect(() => {
    if (isAtBottomRef.current) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, streamingContent, isThinking]);

  if (isLoadingMessages) {
    return (
      <div className={styles.centered}>
        <Loader size="md" label="Loading messages..." />
      </div>
    );
  }

  const isEmpty = messages.length === 0 && !isStreaming && !isThinking;

  return (
    <div
      ref={containerRef}
      className={styles.window}
      onScroll={handleScroll}
      role="log"
      aria-label="Conversation messages"
      aria-live="polite"
    >
      {isEmpty ? (
        <div className={styles.welcome}>
          <div className={styles.welcomeIcon}>✦</div>
          <h2 className={styles.welcomeTitle}>How can I help you today?</h2>
          <p className={styles.welcomeSubtitle}>
            Ask me anything — code, writing, analysis, or just a chat.
          </p>
        </div>
      ) : (
        <div className={styles.messages}>
          {messages.map((msg) => (
            <MessageBubble key={msg.id} message={msg} />
          ))}

          {isThinking && !streamingContent && <ThinkingIndicator />}

          {streamingContent && <StreamingBubble content={streamingContent} />}
        </div>
      )}

      {error && (
        <div className={styles.errorBanner} role="alert">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
            <circle cx="12" cy="12" r="10" /><path d="M12 8v4M12 16h.01" />
          </svg>
          <span>{error}</span>
          <button onClick={onClearError} className={styles.dismissBtn} aria-label="Dismiss error">×</button>
        </div>
      )}

      <div ref={bottomRef} aria-hidden="true" />
    </div>
  );
}
