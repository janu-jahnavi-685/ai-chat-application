import type { Conversation } from '@/types';
import { useTheme } from '@/context/ThemeContext';
import styles from './ChatHeader.module.css';

interface ChatHeaderProps {
  conversation: Conversation | null;
  onMenuToggle: () => void;
}

export function ChatHeader({ conversation, onMenuToggle }: ChatHeaderProps) {
  const { theme, toggleTheme } = useTheme();
  const isLight = theme === 'light';

  return (
    <header className={styles.header}>
      {/* Mobile hamburger */}
      <button
        className={styles.menuBtn}
        onClick={onMenuToggle}
        aria-label="Toggle sidebar"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M3 12h18M3 6h18M3 18h18" />
        </svg>
      </button>

      <div className={styles.title}>
        {conversation ? conversation.title : 'TDK AI Chat'}
      </div>

      <div className={styles.actions}>
        {/* Theme toggle */}
        <button
          className={styles.themeBtn}
          onClick={toggleTheme}
          aria-label={isLight ? 'Switch to dark mode' : 'Switch to light mode'}
          title={isLight ? 'Dark mode' : 'Light mode'}
        >
          {isLight ? (
            /* Moon — switch to dark */
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
            </svg>
          ) : (
            /* Sun — switch to light */
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="5" />
              <line x1="12" y1="1" x2="12" y2="3" />
              <line x1="12" y1="21" x2="12" y2="23" />
              <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
              <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
              <line x1="1" y1="12" x2="3" y2="12" />
              <line x1="21" y1="12" x2="23" y2="12" />
              <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
              <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
            </svg>
          )}
        </button>

        {/* Model badge */}
        <div className={styles.badge}>
          <span className={styles.dot} aria-hidden="true" />
          <span className={styles.badgeLabel}>Groq</span>
        </div>
      </div>
    </header>
  );
}
