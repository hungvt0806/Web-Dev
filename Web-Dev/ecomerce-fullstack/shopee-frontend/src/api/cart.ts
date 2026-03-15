import apiClient from './client'
import type { ApiResponse, Cart } from '@/types'

export const cartApi = {
  getCart: () =>
    apiClient.get<ApiResponse<Cart>>('/cart'),

  addItem: (data: { productId: number; variantId?: number; quantity: number }) =>
    apiClient.post<ApiResponse<Cart>>('/cart/items', data),

  updateItem: (cartItemId: number, quantity: number) =>
    apiClient.patch<ApiResponse<Cart>>(`/cart/items/${cartItemId}`, { quantity }),

  removeItem: (cartItemId: number) =>
    apiClient.delete<ApiResponse<Cart>>(`/cart/items/${cartItemId}`),

  clearCart: () =>
    apiClient.delete<ApiResponse<void>>('/cart'),

  mergeCart: (sessionId: string) =>
    apiClient.post<ApiResponse<Cart>>('/cart/merge', { sessionId }),

  syncPrices: () =>
    apiClient.post<ApiResponse<Cart>>('/cart/sync-prices'),
}
