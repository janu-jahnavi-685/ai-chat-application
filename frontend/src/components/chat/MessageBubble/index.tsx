import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import type { Message } from '@/types';
import styles from './MessageBubble.module.css';

interface MessageBubbleProps {
  message: Message;
}

interface CodeProps {
  inline?: boolean;
  className?: string;
  children?: React.ReactNode;
}

export function MessageBubble({ message }: MessageBubbleProps) {
  const isUser = message.role === 'USER';
  const isFailed = message.status === 'FAILED';

  return (
    <div className={`${styles.wrapper} ${isUser ? styles.user : styles.assistant}`}>
      {!isUser && (
        <div className={styles.avatar} aria-hidden="true">✦</div>
      )}

      <div
        className={`${styles.bubble} ${isUser ? styles.userBubble : styles.assistantBubble} ${isFailed ? styles.failedBubble : ''}`}
      >
        {isUser ? (
          <p className={styles.userText}>{message.content}</p>
        ) : (
          <div className={styles.markdown}>
            <ReactMarkdown
              remarkPlugins={[remarkGfm]}
              components={{
                code({ inline, className, children, ...props }: CodeProps) {
                  const match = /language-(\w+)/.exec(className ?? '');
                  return !inline && match ? (
                    <SyntaxHighlighter
                      style={oneDark}
                      language={match[1]}
                      PreTag="div"
                      customStyle={{ margin: '8px 0', borderRadius: '8px', fontSize: '13px' }}
                    >
                      {String(children).replace(/\n$/, '')}
                    </SyntaxHighlighter>
                  ) : (
                    <code className={styles.inlineCode} {...props}>
                      {children}
                    </code>
                  );
                },
              }}
            >
              {isFailed ? '_Failed to generate response._' : message.content}
            </ReactMarkdown>
          </div>
        )}
      </div>
    </div>
  );
}

interface StreamingBubbleProps {
  content: string;
}

export function StreamingBubble({ content }: StreamingBubbleProps) {
  return (
    <div className={`${styles.wrapper} ${styles.assistant}`}>
      <div className={styles.avatar} aria-hidden="true">✦</div>
      <div className={`${styles.bubble} ${styles.assistantBubble}`}>
        <div className={styles.markdown}>
          <ReactMarkdown remarkPlugins={[remarkGfm]}>
            {content + '▍'}
          </ReactMarkdown>
        </div>
      </div>
    </div>
  );
}
