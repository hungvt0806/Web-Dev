import { create } from 'zustand'
import type { Cart, CartItem } from '@/types'

interface CartState {
  cart: Cart | null
  isOpen: boolean
  isLoading: boolean
  setCart: (cart: Cart) => void
  setLoading: (loading: boolean) => void
  openCart: () => void
  closeCart: () => void
  toggleCart: () => void
  itemCount: number
}

export const useCartStore = create<CartState>((set, get) => ({
  cart: null,
  isOpen: false,
  isLoading: false,
  itemCount: 0,

  setCart: (cart) => set({ cart, itemCount: cart?.totalQuantity ?? 0 }),
  setLoading: (isLoading) => set({ isLoading }),
  openCart: () => set({ isOpen: true }),
  closeCart: () => set({ isOpen: false }),
  toggleCart: () => set((s) => ({ isOpen: !s.isOpen })),
}))
