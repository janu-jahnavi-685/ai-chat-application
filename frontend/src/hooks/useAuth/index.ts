import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthContext } from '@/context/AuthContext';
import { authService } from '@/services/authService';
import type { LoginRequest, SignupRequest } from '@/types';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types';

export function useAuth() {
  const { user, token, isAuthenticated, isLoading, login, logout } = useAuthContext();
  const navigate = useNavigate();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSignup = async (data: SignupRequest) => {
    setIsSubmitting(true);
    setError(null);
    try {
      const res = await authService.signup(data);
      login(res.token, res.user);
      navigate('/chat');
    } catch (err) {
      const axiosErr = err as AxiosError<ApiError>;
      setError(
        axiosErr.response?.data?.message ?? 'Registration failed. Please try again.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleLogin = async (data: LoginRequest) => {
    setIsSubmitting(true);
    setError(null);
    try {
      const res = await authService.login(data);
      login(res.token, res.user);
      navigate('/chat');
    } catch {
      setError('Invalid email or password.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return {
    user,
    token,
    isAuthenticated,
    isLoading,
    isSubmitting,
    error,
    setError,
    handleSignup,
    handleLogin,
    handleLogout,
  };
}
