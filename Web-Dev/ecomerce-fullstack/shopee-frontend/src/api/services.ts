import { get, post, patch, del, getPage } from './client'
import type {
  User, AuthTokens, LoginRequest, RegisterRequest,
  Product, ProductFilters, Category,
  Cart, CartSummary,
  Order, OrderSummary, PlaceOrderRequest,
  Payment, Page,
} from '@/types'

// ── Auth ──────────────────────────────────────────────────────────────────

export const authApi = {
  login:    (data: LoginRequest)    => post<{ user: User; tokens: AuthTokens }>('/auth/login', data),
  register: (data: RegisterRequest) => post<{ user: User; tokens: AuthTokens }>('/auth/register', data),
  logout:   (refreshToken: string)  => post<void>('/auth/logout', { refreshToken }),
  me:       ()                      => get<User>('/auth/me'),
  refresh:  (refreshToken: string)  => post<{ accessToken: string }>('/auth/refresh', { refreshToken }),
}

// ── Products ──────────────────────────────────────────────────────────────

export const productApi = {
  list: (filters: ProductFilters) =>
    getPage<Page<Product>>('/products', filters as Record<string, unknown>),
  byId:       (id: number)   => get<Product>(`/products/${id}`),
  bySlug:     (slug: string) => get<Product>(`/products/slug/${slug}`),
  featured:   ()             => get<Product[]>('/products/featured'),
  byCategory: (categoryId: number, params?: ProductFilters) =>
    getPage<Page<Product>>(`/products/category/${categoryId}`, params as Record<string, unknown>),

  // Admin
  adminList:   (params?: Record<string, unknown>) => getPage<Page<Product>>('/admin/products', params),
  create:      (data: FormData) => post<Product>('/admin/products', data),
  update:      (id: number, data: Partial<Product>) => patch<Product>(`/admin/products/${id}`, data),
  delete:      (id: number)     => del<void>(`/admin/products/${id}`),
  uploadImage: (id: number, file: FormData) => post<{ url: string }>(`/admin/products/${id}/images`, file),
}

// ── Categories ────────────────────────────────────────────────────────────

export const categoryApi = {
  list: ()      => get<Category[]>('/categories'),
  tree: ()      => get<Category[]>('/categories/tree'),
  byId: (id: number) => get<Category>(`/categories/${id}`),
}

// ── Cart ──────────────────────────────────────────────────────────────────

export const cartApi = {
  get:     ()                          => get<Cart>('/cart'),
  summary: ()                          => get<CartSummary>('/cart/summary'),
  add:     (productId: number, variantId: number | undefined, quantity: number) =>
    post<Cart>('/cart/items', { productId, variantId, quantity }),
  update:  (cartItemId: number, quantity: number) =>
    patch<Cart>(`/cart/items/${cartItemId}`, { quantity }),
  remove:  (cartItemId: number)        => del<Cart>(`/cart/items/${cartItemId}`),
  clear:   ()                          => del<Cart>('/cart'),
  merge:   (guestSessionId: string)    => post<Cart>('/cart/merge', { guestSessionId }),
  syncPrices: ()                       => post<Cart>('/cart/sync-prices'),
}

// ── Orders ────────────────────────────────────────────────────────────────

export const orderApi = {
  place:       (data: PlaceOrderRequest) => post<Order>('/orders', data),
  history:     (params?: { status?: string; page?: number; size?: number }) =>
    getPage<Page<OrderSummary>>('/orders', params as Record<string, unknown>),
  detail:      (id: string)              => get<Order>(`/orders/${id}`),
  byNumber:    (orderNumber: string)     => get<Order>(`/orders/number/${orderNumber}`),
  cancel:      (id: string, reason: string) => post<Order>(`/orders/${id}/cancel`, { reason }),

  // Admin
  adminList:   (params?: Record<string, unknown>) => getPage<Page<OrderSummary>>('/admin/orders', params),
  adminDetail: (id: string)              => get<Order>(`/admin/orders/${id}`),
  updateStatus:(id: string, status: string, note?: string, trackingNumber?: string, shippingCarrier?: string) =>
    patch<Order>(`/admin/orders/${id}/status`, { status, note, trackingNumber, shippingCarrier }),
}

// ── Payments ──────────────────────────────────────────────────────────────

export const paymentApi = {
  initiate: (orderId: string)  => post<Payment>('/payments/initiate', { orderId }),
  status:   (orderId: string)  => get<Payment>(`/payments/status/${orderId}`),
  refund:   (orderId: string, reason: string) =>
    post<Payment>(`/admin/payments/${orderId}/refund`, { reason }),
}

// ── Users ─────────────────────────────────────────────────────────────────

export const userApi = {
  updateProfile: (data: Partial<User>)  => patch<User>('/users/me', data),
  changePassword: (current: string, next: string) =>
    post<void>('/auth/change-password', { currentPassword: current, newPassword: next }),
}
