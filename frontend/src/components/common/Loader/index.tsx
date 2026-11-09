import styles from './Loader.module.css';

interface LoaderProps {
  size?: 'sm' | 'md' | 'lg';
  label?: string;
}

export function Loader({ size = 'md', label = 'Loading...' }: LoaderProps) {
  return (
    <div className={styles.container} role="status" aria-label={label}>
      <div className={`${styles.spinner} ${styles[size]}`} />
      <span className="sr-only">{label}</span>
    </div>
  );
}
