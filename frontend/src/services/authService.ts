import { apiClient } from './api';
import type { AuthResponse, LoginRequest, SignupRequest, User } from '@/types';

export const authService = {
  async signup(data: SignupRequest): Promise<AuthResponse> {
    const res = await apiClient.post<AuthResponse>('/auth/signup', data);
    return res.data;
  },

  async login(data: LoginRequest): Promise<AuthResponse> {
    const res = await apiClient.post<AuthResponse>('/auth/login', data);
    return res.data;
  },

  async getCurrentUser(): Promise<User> {
    const res = await apiClient.get<User>('/auth/me');
    return res.data;
  },
};
