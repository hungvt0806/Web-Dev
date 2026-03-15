import apiClient from './client'
import type { ApiResponse, Product, ProductSummary, Category } from '@/types'

export interface ProductSearchParams {
  q?: string
  categoryId?: number
  minPrice?: number
  maxPrice?: number
  sortBy?: 'price_asc' | 'price_desc' | 'newest' | 'popular' | 'rating'
  page?: number
  size?: number
}

export const productsApi = {
  search: (params: ProductSearchParams) =>
    apiClient.get<ApiResponse<ProductSummary[]>>('/products', { params }),

  getById: (id: number) =>
    apiClient.get<ApiResponse<Product>>(`/products/${id}`),

  getBySlug: (slug: string) =>
    apiClient.get<ApiResponse<Product>>(`/products/slug/${slug}`),

  getFeatured: () =>
    apiClient.get<ApiResponse<ProductSummary[]>>('/products/featured'),

  getByCategory: (categorySlug: string, params?: { page?: number; size?: number }) =>
    apiClient.get<ApiResponse<ProductSummary[]>>(`/products/category/${categorySlug}`, { params }),

  // Admin
  adminList: (params?: { page?: number; size?: number; status?: string }) =>
    apiClient.get<ApiResponse<Product[]>>('/admin/products', { params }),

  adminCreate: (data: FormData) =>
    apiClient.post<ApiResponse<Product>>('/admin/products', data, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  adminUpdate: (id: number, data: Partial<Product>) =>
    apiClient.put<ApiResponse<Product>>(`/admin/products/${id}`, data),

  adminDelete: (id: number) =>
    apiClient.delete(`/admin/products/${id}`),
}

export const categoriesApi = {
  list: () =>
    apiClient.get<ApiResponse<Category[]>>('/categories'),

  getTree: () =>
    apiClient.get<ApiResponse<Category[]>>('/categories/tree'),

  adminCreate: (data: Partial<Category>) =>
    apiClient.post<ApiResponse<Category>>('/admin/categories', data),

  adminUpdate: (id: number, data: Partial<Category>) =>
    apiClient.put<ApiResponse<Category>>(`/admin/categories/${id}`, data),
}
