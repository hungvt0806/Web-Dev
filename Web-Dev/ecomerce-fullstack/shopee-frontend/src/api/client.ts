import axios from 'axios'
import toast from 'react-hot-toast'
import { useAuthStore } from '@/store/authStore'

const apiClient = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

// Request: attach access token
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) config.headers.Authorization = `Bearer ${token}`
  const sessionId = localStorage.getItem('sessionId')
  if (sessionId) config.headers['X-Session-Id'] = sessionId
  return config
})

// Response: handle 401 with token refresh
let isRefreshing = false
let failedQueue: Array<{ resolve: Function; reject: Function }> = []

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    error ? reject(error) : resolve(token)
  })
  failedQueue = []
}

apiClient.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    if (error.response?.status === 401 && !original._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then((token) => {
          original.headers.Authorization = `Bearer ${token}`
          return apiClient(original)
        })
      }
      original._retry = true
      isRefreshing = true
      try {
        const refreshToken = useAuthStore.getState().refreshToken
        const { data } = await axios.post('/api/auth/refresh', { refreshToken })
        useAuthStore.getState().setTokens(data.data.accessToken, data.data.refreshToken)
        processQueue(null, data.data.accessToken)
        original.headers.Authorization = `Bearer ${data.data.accessToken}`
        return apiClient(original)
      } catch (err) {
        processQueue(err, null)
        useAuthStore.getState().logout()
        window.location.href = '/login'
        return Promise.reject(err)
      } finally {
        isRefreshing = false
      }
    }
    const msg = error.response?.data?.message || 'Something went wrong'
    if (error.response?.status !== 401) toast.error(msg)
    return Promise.reject(error)
  }
)

export default apiClient
