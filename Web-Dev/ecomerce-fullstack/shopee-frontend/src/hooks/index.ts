import { useEffect, useRef, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'

// ── useRequireAuth ────────────────────────────────────────────────────────

export function useRequireAuth(redirectTo = '/login') {
  const { isAuth } = useAuthStore()
  const navigate = useNavigate()
  useEffect(() => {
    if (!isAuth) navigate(redirectTo, { replace: true })
  }, [isAuth])
  return isAuth
}

// ── useDebounce ───────────────────────────────────────────────────────────

export function useDebounce<T>(value: T, delay = 300): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(timer)
  }, [value, delay])
  return debounced
}

// ── useIntersectionObserver ───────────────────────────────────────────────

export function useIntersectionObserver(options?: IntersectionObserverInit) {
  const ref = useRef<HTMLDivElement>(null)
  const [isVisible, setIsVisible] = useState(false)
  useEffect(() => {
    const el = ref.current
    if (!el) return
    const obs = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) { setIsVisible(true); obs.disconnect() }
    }, options)
    obs.observe(el)
    return () => obs.disconnect()
  }, [])
  return { ref, isVisible }
}

// ── useLocalStorage ───────────────────────────────────────────────────────

export function useLocalStorage<T>(key: string, initialValue: T) {
  const [storedValue, setStoredValue] = useState<T>(() => {
    try {
      const item = window.localStorage.getItem(key)
      return item ? JSON.parse(item) : initialValue
    } catch { return initialValue }
  })
  const setValue = useCallback((value: T | ((val: T) => T)) => {
    try {
      const valueToStore = value instanceof Function ? value(storedValue) : value
      setStoredValue(valueToStore)
      window.localStorage.setItem(key, JSON.stringify(valueToStore))
    } catch {}
  }, [key, storedValue])
  return [storedValue, setValue] as const
}

// ── useScrollTop ──────────────────────────────────────────────────────────

export function useScrollTop() {
  const [scrolled, setScrolled] = useState(false)
  useEffect(() => {
    const handler = () => setScrolled(window.scrollY > 0)
    window.addEventListener('scroll', handler, { passive: true })
    return () => window.removeEventListener('scroll', handler)
  }, [])
  return scrolled
}

// ── usePagination ─────────────────────────────────────────────────────────

export function usePagination(totalPages: number, initialPage = 0) {
  const [page, setPage] = useState(initialPage)
  const prev = useCallback(() => setPage(p => Math.max(0, p - 1)), [])
  const next = useCallback(() => setPage(p => Math.min(totalPages - 1, p + 1)), [totalPages])
  const goTo = useCallback((p: number) => setPage(Math.max(0, Math.min(totalPages - 1, p))), [totalPages])
  return { page, prev, next, goTo, isFirst: page === 0, isLast: page >= totalPages - 1 }
}

// ── useMediaQuery ─────────────────────────────────────────────────────────

export function useMediaQuery(query: string) {
  const [matches, setMatches] = useState(() => window.matchMedia(query).matches)
  useEffect(() => {
    const mq = window.matchMedia(query)
    const handler = (e: MediaQueryListEvent) => setMatches(e.matches)
    mq.addEventListener('change', handler)
    return () => mq.removeEventListener('change', handler)
  }, [query])
  return matches
}

export const useIsMobile = () => useMediaQuery('(max-width: 768px)')
