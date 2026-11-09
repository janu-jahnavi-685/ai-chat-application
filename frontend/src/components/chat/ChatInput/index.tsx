import { useEffect, useRef, useState } from 'react';
import styles from './ChatInput.module.css';

interface ChatInputProps {
  onSend: (message: string) => void;
  isDisabled: boolean;
  isStreaming: boolean;
}

export function ChatInput({ onSend, isDisabled, isStreaming }: ChatInputProps) {
  const [value, setValue] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Auto-resize textarea
  useEffect(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 200)}px`;
  }, [value]);

  // Focus on mount
  useEffect(() => {
    textareaRef.current?.focus();
  }, []);

  const handleSend = () => {
    const trimmed = value.trim();
    if (!trimmed || isDisabled) return;
    onSend(trimmed);
    setValue('');
    // Reset height
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const canSend = value.trim().length > 0 && !isDisabled;

  return (
    <div className={styles.container}>
      <div className={styles.inputArea}>
        <textarea
          ref={textareaRef}
          className={styles.textarea}
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={isStreaming ? 'Waiting for response...' : 'Message TDK AI… (Enter to send, Shift+Enter for newline)'}
          disabled={isDisabled}
          rows={1}
          aria-label="Chat input"
          aria-multiline="true"
        />
        <button
          className={`${styles.sendBtn} ${canSend ? styles.active : ''}`}
          onClick={handleSend}
          disabled={!canSend}
          aria-label="Send message"
        >
          {isStreaming ? (
            <span className={styles.streamingDot} aria-hidden="true" />
          ) : (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <path d="M22 2L11 13M22 2L15 22 11 13 2 9l20-7z" />
            </svg>
          )}
        </button>
      </div>
      <p className={styles.hint}>
        Press <kbd>Enter</kbd> to send · <kbd>Shift+Enter</kbd> for newline
      </p>
    </div>
  );
}
