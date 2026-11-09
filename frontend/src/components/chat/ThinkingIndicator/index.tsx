import styles from './ThinkingIndicator.module.css';

export function ThinkingIndicator() {
  return (
    <div className={styles.wrapper} role="status" aria-label="AI is thinking">
      <div className={styles.avatar}>✦</div>
      <div className={styles.bubble}>
        <span className={styles.dot} style={{ animationDelay: '0ms' }} />
        <span className={styles.dot} style={{ animationDelay: '160ms' }} />
        <span className={styles.dot} style={{ animationDelay: '320ms' }} />
      </div>
    </div>
  );
}
