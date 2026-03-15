import apiClient from './client'
import type { ApiResponse, PaymentInitResponse } from '@/types'

export const paymentsApi = {
  initiate: (orderId: string) =>
    apiClient.post<ApiResponse<PaymentInitResponse>>('/payments/initiate', { orderId }),

  getStatus: (orderId: string) =>
    apiClient.get<ApiResponse<{ status: string }>>(`/payments/status/${orderId}`),

  adminRefund: (orderId: string, reason: string) =>
    apiClient.post(`/admin/payments/${orderId}/refund`, null, { params: { reason } }),
}
