import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { authApi } from '@/api/auth'
import toast from 'react-hot-toast'

export function OAuth2RedirectPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const { setAuth } = useAuthStore()

  useEffect(() => {
    const token = params.get('token')
    const error = params.get('error')

    if (error) {
      toast.error('Social login failed: ' + error)
      navigate('/login')
      return
    }

    if (token) {
      // Store token temporarily, fetch user info
      useAuthStore.getState().setTokens(token, '')
      authApi.getMe()
        .then(res => {
          const user = (res.data as any).data ?? res.data
          setAuth(user, token, '')
          toast.success(`Welcome, ${user.fullName.split(' ')[0]}!`)
          navigate('/')
        })
        .catch(() => {
          toast.error('Failed to load user info')
          navigate('/login')
        })
    } else {
      navigate('/login')
    }
  }, [])

  return (
    <div className="min-h-screen flex items-center justify-center">
      <p className="font-body text-ink/50">Signing you in...</p>
    </div>
  )
}