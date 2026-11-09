import { useEffect, useState } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { useChat } from '@/hooks/useChat';
import { ChatSidebar } from '@/components/chat/ChatSidebar';
import { ChatHeader } from '@/components/chat/ChatHeader';
import { ChatWindow } from '@/components/chat/ChatWindow';
import { ChatInput } from '@/components/chat/ChatInput';
import styles from './ChatPage.module.css';

export function ChatPage() {
  const { handleLogout } = useAuth();
  const {
    conversations,
    activeConversation,
    messages,
    isLoadingConversations,
    isLoadingMessages,
    isStreaming,
    isThinking,
    streamingContent,
    error,
    loadConversations,
    createConversation,
    selectConversation,
    deleteConversation,
    sendMessage,
    clearError,
  } = useChat();

  const [sidebarOpen, setSidebarOpen] = useState(false);

  // Load conversations on mount
  useEffect(() => {
    void loadConversations();
  }, [loadConversations]);

  const handleNewChat = async () => {
    setSidebarOpen(false);
    await createConversation();
  };

  const handleSend = async (message: string) => {
    if (!activeConversation) {
      // No active conversation — create one first, then send with its ID directly
      // (avoids React state staleness: sendMessage can't read the new conversation
      // from state until the next render, so we pass the ID explicitly)
      const conversation = await createConversation();
      if (conversation) {
        await sendMessage(message, conversation.id);
      }
    } else {
      await sendMessage(message);
    }
  };

  return (
    <div className={styles.layout}>
      <ChatSidebar
        conversations={conversations}
        activeConversation={activeConversation}
        isLoading={isLoadingConversations}
        onNewChat={handleNewChat}
        onSelectConversation={(c) => { setSidebarOpen(false); void selectConversation(c); }}
        onDeleteConversation={(id) => void deleteConversation(id)}
        onLogout={handleLogout}
        isOpen={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
      />

      <div className={styles.main}>
        <ChatHeader
          conversation={activeConversation}
          onMenuToggle={() => setSidebarOpen((o) => !o)}
        />

        <ChatWindow
          messages={messages}
          streamingContent={streamingContent}
          isThinking={isThinking}
          isStreaming={isStreaming}
          isLoadingMessages={isLoadingMessages}
          error={error}
          onClearError={clearError}
        />

        <ChatInput
          onSend={(msg) => void handleSend(msg)}
          isDisabled={isStreaming}
          isStreaming={isStreaming}
        />
      </div>
    </div>
  );
}
