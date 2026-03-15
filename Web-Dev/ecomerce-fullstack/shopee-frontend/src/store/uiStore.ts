import { create } from 'zustand'

interface UIState {
  searchOpen: boolean
  mobileMenuOpen: boolean
  openSearch: () => void
  closeSearch: () => void
  toggleSearch: () => void
  openMobileMenu: () => void
  closeMobileMenu: () => void
}

export const useUIStore = create<UIState>((set) => ({
  searchOpen: false,
  mobileMenuOpen: false,
  openSearch: () => set({ searchOpen: true }),
  closeSearch: () => set({ searchOpen: false }),
  toggleSearch: () => set((s) => ({ searchOpen: !s.searchOpen })),
  openMobileMenu: () => set({ mobileMenuOpen: true }),
  closeMobileMenu: () => set({ mobileMenuOpen: false }),
}))
