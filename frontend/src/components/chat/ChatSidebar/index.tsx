import { useEffect, useRef, useState } from 'react';
import type { Conversation } from '@/types';
import { useAuthContext } from '@/context/AuthContext';
import { Button } from '@/components/common/Button';
import styles from './ChatSidebar.module.css';

interface ChatSidebarProps {
  conversations: Conversation[];
  activeConversation: Conversation | null;
  isLoading: boolean;
  onNewChat: () => void;
  onSelectConversation: (c: Conversation) => void;
  onDeleteConversation: (id: number) => void;
  onLogout: () => void;
  isOpen: boolean;
  onClose: () => void;
}

export function ChatSidebar({
  conversations,
  activeConversation,
  isLoading,
  onNewChat,
  onSelectConversation,
  onDeleteConversation,
  onLogout,
  isOpen,
  onClose,
}: ChatSidebarProps) {
  const { user } = useAuthContext();
  const [hoveredId, setHoveredId] = useState<number | null>(null);
  const sidebarRef = useRef<HTMLDivElement>(null);

  // Close sidebar on click outside (mobile)
  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (isOpen && sidebarRef.current && !sidebarRef.current.contains(e.target as Node)) {
        onClose();
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, [isOpen, onClose]);

  const formatDate = (dateStr: string) => {
    const d = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - d.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    if (diffDays === 0) return 'Today';
    if (diffDays === 1) return 'Yesterday';
    if (diffDays < 7) return `${diffDays} days ago`;
    return d.toLocaleDateString();
  };

  return (
    <>
      {/* Mobile overlay */}
      {isOpen && <div className={styles.overlay} onClick={onClose} aria-hidden="true" />}

      <aside
        ref={sidebarRef}
        className={`${styles.sidebar} ${isOpen ? styles.open : ''}`}
        aria-label="Chat history"
      >
        {/* Header */}
        <div className={styles.header}>
          <div className={styles.logo}>✦ TDK AI</div>
          <button className={styles.newChatBtn} onClick={onNewChat} aria-label="New chat">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M12 5v14M5 12h14" />
            </svg>
          </button>
        </div>

        {/* Conversations list */}
        <div className={styles.listWrapper}>
          <div className={styles.listHeader}>Recent</div>
          <nav className={styles.list} aria-label="Conversations">
            {isLoading ? (
              <div className={styles.loadingList}>
                {[...Array(4)].map((_, i) => (
                  <div key={i} className={styles.skeleton} />
                ))}
              </div>
            ) : conversations.length === 0 ? (
              <div className={styles.empty}>No conversations yet</div>
            ) : (
              conversations.map((c) => (
                <div
                  key={c.id}
                  className={`${styles.item} ${activeConversation?.id === c.id ? styles.active : ''}`}
                  onMouseEnter={() => setHoveredId(c.id)}
                  onMouseLeave={() => setHoveredId(null)}
                >
                  <button
                    className={styles.itemBtn}
                    onClick={() => onSelectConversation(c)}
                    title={c.title}
                  >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className={styles.chatIcon}>
                      <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" />
                    </svg>
                    <span className={styles.itemTitle}>{c.title}</span>
                    <span className={styles.itemDate}>{formatDate(c.updatedAt)}</span>
                  </button>
                  {hoveredId === c.id && (
                    <button
                      className={styles.deleteBtn}
                      onClick={(e) => { e.stopPropagation(); onDeleteConversation(c.id); }}
                      aria-label={`Delete "${c.title}"`}
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6" />
                      </svg>
                    </button>
                  )}
                </div>
              ))
            )}
          </nav>
        </div>

        {/* User / logout */}
        <div className={styles.footer}>
          <div className={styles.userInfo}>
            <div className={styles.avatar}>{user?.name?.[0]?.toUpperCase() ?? '?'}</div>
            <div className={styles.userDetails}>
              <span className={styles.userName}>{user?.name}</span>
              <span className={styles.userEmail}>{user?.email}</span>
            </div>
          </div>
          <Button variant="ghost" size="sm" onClick={onLogout} className={styles.logoutBtn} aria-label="Log out">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9" />
            </svg>
          </Button>
        </div>
      </aside>
    </>
  );
}
