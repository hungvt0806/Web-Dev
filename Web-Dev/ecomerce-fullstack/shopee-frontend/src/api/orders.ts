import apiClient from './client'
import type { ApiResponse, Order, OrderSummary, ShippingAddress } from '@/types'

export const ordersApi = {
  place: (data: { shippingAddress: ShippingAddress; buyerNote?: string; couponCode?: string }) =>
    apiClient.post<ApiResponse<Order>>('/orders', data),

  list: (params?: { page?: number; size?: number; status?: string }) =>
    apiClient.get<ApiResponse<OrderSummary[]>>('/orders', { params }),

  getById: (id: string) =>
    apiClient.get<ApiResponse<Order>>(`/orders/${id}`),

  getByNumber: (orderNumber: string) =>
    apiClient.get<ApiResponse<Order>>(`/orders/number/${orderNumber}`),

  cancel: (id: string, reason: string) =>
    apiClient.post<ApiResponse<Order>>(`/orders/${id}/cancel`, { reason }),

  // Admin
  adminList: (params?: { page?: number; size?: number; status?: string }) =>
    apiClient.get<ApiResponse<OrderSummary[]>>('/admin/orders', { params }),

  adminGetById: (id: string) =>
    apiClient.get<ApiResponse<Order>>(`/admin/orders/${id}`),

  adminUpdateStatus: (id: string, data: { status: string; note?: string; trackingNumber?: string }) =>
    apiClient.patch<ApiResponse<Order>>(`/admin/orders/${id}/status`, data),
}
