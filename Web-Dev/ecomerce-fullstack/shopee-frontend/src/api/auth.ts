import apiClient from './client'
import type { ApiResponse, AuthResponse, User } from '@/types'

export const authApi = {
  register: (data: { email: string; password: string; fullName: string; phoneNumber?: string }) =>
    apiClient.post<ApiResponse<AuthResponse>>('/auth/register', data),

  login: (data: { email: string; password: string }) =>
    apiClient.post<ApiResponse<AuthResponse>>('/auth/login', data),

  logout: (refreshToken: string) =>
    apiClient.post('/auth/logout', { refreshToken }),

  refreshToken: (refreshToken: string) =>
    apiClient.post<ApiResponse<{ accessToken: string; refreshToken: string }>>('/auth/refresh', { refreshToken }),

  getMe: () =>
    apiClient.get<ApiResponse<User>>('/auth/me'),

  changePassword: (data: { currentPassword: string; newPassword: string }) =>
    apiClient.put('/auth/change-password', data),

  forgotPassword: (email: string) =>
    apiClient.post('/auth/forgot-password', { email }),
}
