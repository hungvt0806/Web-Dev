import { useMutation } from '@tanstack/react-query'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/store/authStore'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'

export function useAuth() {
  const { setAuth, logout: storeLogout, user, isAuthenticated } = useAuthStore()
  const navigate = useNavigate()

  const login = useMutation({
    mutationFn: authApi.login,
    onSuccess: (res) => {
  const { user, accessToken, refreshToken } = res.data as any
      setAuth(user, accessToken, refreshToken)
      toast.success(`Welcome back, ${user.fullName.split(' ')[0]}!`)
      navigate('/')
    },
  })

  const register = useMutation({
    mutationFn: authApi.register,
    onSuccess: (res) => {
      const { user, accessToken, refreshToken } = res.data as any
      setAuth(user, accessToken, refreshToken)
      toast.success('Account created!')
      navigate('/')
    },
  })

  const logout = useMutation({
    mutationFn: () => {
      const rt = useAuthStore.getState().refreshToken
      return rt ? authApi.logout(rt) : Promise.resolve()
    },
    onSettled: () => {
      storeLogout()
      navigate('/login')
      toast.success('Logged out')
    },
  })

  return { login, register, logout, user, isAuthenticated }
}
