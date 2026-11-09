import { Navigate, Outlet } from 'react-router-dom';
import { useAuthContext } from '@/context/AuthContext';
import styles from './ProtectedRoute.module.css';

export function ProtectedRoute() {
  const { isAuthenticated, isLoading } = useAuthContext();

  if (isLoading) {
    return (
      <div className={styles.loadingContainer}>
        <div className={styles.spinner} />
      </div>
    );
  }

  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
}

export function PublicRoute() {
  const { isAuthenticated, isLoading } = useAuthContext();

  if (isLoading) {
    return (
      <div className={styles.loadingContainer}>
        <div className={styles.spinner} />
      </div>
    );
  }

  return isAuthenticated ? <Navigate to="/chat" replace /> : <Outlet />;
}
