import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Mail, Lock, User } from 'lucide-react'
import { useAuth } from '@/hooks/useAuth'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'

const schema = z.object({
  fullName:        z.string().min(2, 'Full name required'),
  email:           z.string().email('Valid email required'),
  password:        z.string().min(8, 'At least 8 characters'),
  confirmPassword: z.string(),
}).refine(d => d.password === d.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
})

export function RegisterPage() {
  const { register: regAuth } = useAuth()
  const { register, handleSubmit, formState: { errors } } = useForm({ resolver: zodResolver(schema) })

  return (
    <div className="min-h-screen flex">
      <div className="hidden lg:flex lg:w-1/2 bg-ink relative overflow-hidden items-center justify-center">
        <div className="absolute inset-0 bg-noise" />
        <div className="absolute top-1/4 right-1/4 w-64 h-64 border border-ash/5 rotate-45" />
        <div className="absolute bottom-1/4 left-1/4 w-48 h-48 bg-sage/8 rounded-full" />
        <div className="relative text-center px-12">
          <div className="w-14 h-14 bg-ember flex items-center justify-center mx-auto mb-6">
            <span className="text-white font-display font-black text-2xl">K</span>
          </div>
          <h1 className="font-display font-black text-5xl text-ash mb-4">Join KURA</h1>
          <p className="font-body text-ash/50">Thousands of curated products await.</p>
        </div>
      </div>

      <div className="w-full lg:w-1/2 flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-md">
          <div className="mb-10">
            <p className="section-label">Get started</p>
            <h2 className="font-display font-bold text-3xl text-ink">Create your account</h2>
          </div>

          <form onSubmit={handleSubmit((d) => regAuth.mutate(d))} className="space-y-4">
            <Input
              label="Full Name"
              placeholder="Yuki Tanaka"
              leftIcon={<User size={16} />}
              error={errors.fullName?.message}
              {...register('fullName')}
            />
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
              placeholder="At least 8 characters"
              leftIcon={<Lock size={16} />}
              error={errors.password?.message}
              {...register('password')}
            />
            <Input
              label="Confirm Password"
              type="password"
              placeholder="Repeat password"
              leftIcon={<Lock size={16} />}
              error={errors.confirmPassword?.message}
              {...register('confirmPassword')}
            />

            <Button type="submit" fullWidth size="lg" loading={regAuth.isPending} className="mt-2">
              Create Account
            </Button>
          </form>

          <p className="mt-8 text-sm font-body text-ink/50 text-center">
            Already have an account?{' '}
            <Link to="/login" className="text-ember font-medium hover:underline">Sign in</Link>
          </p>

          <p className="mt-4 text-xs text-center font-body text-ink/30">
            By registering you agree to our{' '}
            <Link to="/terms" className="hover:underline">Terms of Service</Link> and{' '}
            <Link to="/privacy" className="hover:underline">Privacy Policy</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
