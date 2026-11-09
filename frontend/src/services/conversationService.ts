import { apiClient } from './api';
import type {
  Conversation,
  CreateConversationRequest,
  Message,
} from '@/types';

export const conversationService = {
  async create(data: CreateConversationRequest): Promise<Conversation> {
    const res = await apiClient.post<Conversation>('/conversations', data);
    return res.data;
  },

  async list(): Promise<Conversation[]> {
    const res = await apiClient.get<Conversation[]>('/conversations');
    return res.data;
  },

  async getById(id: number): Promise<Conversation> {
    const res = await apiClient.get<Conversation>(`/conversations/${id}`);
    return res.data;
  },

  async getMessages(id: number): Promise<Message[]> {
    const res = await apiClient.get<Message[]>(`/conversations/${id}/messages`);
    return res.data;
  },

  async delete(id: number): Promise<void> {
    await apiClient.delete(`/conversations/${id}`);
  },
};
