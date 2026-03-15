import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Mail, Lock } from 'lucide-react'
import { useAuth } from '@/hooks/useAuth'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'

const schema = z.object({
  email:    z.string().email('Valid email required'),
  password: z.string().min(1, 'Password required'),
})

export function LoginPage() {
  const { login } = useAuth()
  const { register, handleSubmit, formState: { errors } } = useForm({ resolver: zodResolver(schema) })

  return (
    <div className="min-h-screen flex">
      <div className="hidden lg:flex lg:w-1/2 bg-ink relative overflow-hidden items-center justify-center">
        <div className="absolute inset-0 bg-noise" />
        <div className="absolute top-1/4 left-1/4 w-80 h-80 border border-ash/5 rotate-12" />
        <div className="absolute top-1/3 left-1/3 w-48 h-48 border border-ash/5 -rotate-6" />
        <div className="absolute bottom-0 right-0 w-96 h-96 bg-ember/5 rounded-tl-full" />
        <div className="relative text-center px-12">
          <div className="w-14 h-14 bg-ember flex items-center justify-center mx-auto mb-6">
            <span className="text-white font-display font-black text-2xl">K</span>
          </div>
          <h1 className="font-display font-black text-5xl text-ash mb-4 tracking-tight">KURA</h1>
          <p className="font-body text-ash/50 text-lg">Curated goods for a considered life.</p>
        </div>
      </div>

      <div className="w-full lg:w-1/2 flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-md">
          <div className="mb-10">
            <Link to="/" className="lg:hidden flex items-center gap-2 mb-8">
              <div className="w-8 h-8 bg-ember flex items-center justify-center">
                <span className="text-white font-display font-black text-sm">K</span>
              </div>
              <span className="font-display font-black text-xl">KURA</span>
            </Link>
            <p className="section-label">Welcome back</p>
            <h2 className="font-display font-bold text-3xl text-ink">Sign in to your account</h2>
          </div>

          <form onSubmit={handleSubmit((d) => login.mutate(d))} className="space-y-4">
            <Input
              label="Email"
              type="email"
              placeholder="you@example.com"
              leftIcon={<Mail size={16} />}
              error={errors.email?.message}
              {...register('email')}
            />
            <Input
              label="Password"
              type="password"
              placeholder="••••••••"
              leftIcon={<Lock size={16} />}
              error={errors.password?.message}
              {...register('password')}
            />
            <div className="flex justify-end">
              <Link to="/forgot-password" className="text-xs font-body text-ink/50 hover:text-ember transition-colors">
                Forgot password?
              </Link>
            </div>
            <Button type="submit" fullWidth size="lg" loading={login.isPending}>
              Sign In
            </Button>
          </form>

          <div className="mt-6">
            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-ink/10" />
              </div>
              <div className="relative flex justify-center text-xs">
                <span className="px-2 bg-white font-mono text-ink/30">or continue with</span>
              </div>
            </div>
            <div className="mt-4">
              
               <a href="http://localhost:8080/oauth2/authorize/google"
                className="w-full flex items-center justify-center gap-3 px-4 py-3 border-2 border-ink/15 hover:border-ink transition-colors font-body text-sm text-ink"
              >
                <svg width="18" height="18" viewBox="0 0 24 24">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                </svg>
                Continue with Google
              </a>
            </div>
          </div>

          <p className="mt-8 text-sm font-body text-ink/50 text-center">
            Don't have an account?{' '}
            <Link to="/register" className="text-ember font-medium hover:underline">Create one</Link>
          </p>

          <div className="mt-8 p-4 bg-ash-dark border border-ink/8">
            <p className="text-xs font-mono text-ink/40 mb-1">Demo Credentials</p>
            <p className="text-xs font-mono text-ink/60">admin@kura.jp / admin123</p>
          </div>
        </div>
      </div>
    </div>
  )
}