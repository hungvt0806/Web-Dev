import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { User, Lock, Package } from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import { authApi } from '@/api/auth'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Link } from 'react-router-dom'
import toast from 'react-hot-toast'

const passwordSchema = z.object({
  currentPassword: z.string().min(1, 'Current password required'),
  newPassword:     z.string().min(8, 'At least 8 characters'),
  confirmPassword: z.string(),
}).refine(d => d.newPassword === d.confirmPassword, {
  message: 'Passwords do not match', path: ['confirmPassword'],
})

export function ProfilePage() {
  const [activeTab, setActiveTab] = useState<'info' | 'security'>('info')
  const { user } = useAuthStore()

  const changePassword = useMutation({
    mutationFn: authApi.changePassword,
    onSuccess: () => toast.success('Password updated'),
    onError: () => toast.error('Incorrect current password'),
  })

  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    resolver: zodResolver(passwordSchema),
  })

  const TABS = [
    { id: 'info' as const,     label: 'Profile Info',  icon: User },
    { id: 'security' as const, label: 'Security',       icon: Lock },
  ]

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 py-8">
      <div className="mb-8">
        <p className="section-label">Account</p>
        <h1 className="page-title">My Profile</h1>
      </div>

      {/* Avatar block */}
      <div className="bg-white border border-ash-dark p-6 mb-6 flex items-center gap-5">
        <div className="w-16 h-16 bg-ink flex items-center justify-center flex-shrink-0">
          {user?.avatarUrl ? (
            <img src={user.avatarUrl} alt="" className="w-full h-full object-cover" />
          ) : (
            <span className="text-ash font-display font-black text-2xl">
              {user?.fullName?.[0]?.toUpperCase()}
            </span>
          )}
        </div>
        <div>
          <h2 className="font-display font-bold text-xl">{user?.fullName}</h2>
          <p className="font-mono text-sm text-ink/40">{user?.email}</p>
          <span className="inline-block mt-1 px-2 py-0.5 text-xs font-mono uppercase tracking-wide bg-ash-dark text-ink/50">
            {user?.role}
          </span>
        </div>
        <Link to="/orders" className="ml-auto hidden sm:flex items-center gap-2 text-sm font-body text-ink/50 hover:text-ember transition-colors">
          <Package size={15} /> View Orders
        </Link>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 border-b border-ink/10 mb-6">
        {TABS.map(t => (
          <button
            key={t.id}
            onClick={() => setActiveTab(t.id)}
            className={`flex items-center gap-2 px-5 py-3 text-sm font-body border-b-2 -mb-px transition-all ${
              activeTab === t.id ? 'border-ember text-ember font-medium' : 'border-transparent text-ink/50 hover:text-ink'
            }`}
          >
            <t.icon size={14} />
            {t.label}
          </button>
        ))}
      </div>

      {/* Profile Info */}
      {activeTab === 'info' && (
        <div className="bg-white border border-ash-dark p-6 space-y-4">
          <h3 className="font-display font-semibold text-base mb-2">Account Information</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <p className="section-label">Full Name</p>
              <p className="font-body text-sm text-ink bg-ash-dark px-4 py-3">{user?.fullName}</p>
            </div>
            <div>
              <p className="section-label">Email</p>
              <p className="font-body text-sm text-ink bg-ash-dark px-4 py-3">{user?.email}</p>
            </div>
          </div>
          <p className="text-xs font-mono text-ink/30">To update your name or email, please contact support.</p>
        </div>
      )}

      {/* Security */}
      {activeTab === 'security' && (
        <div className="bg-white border border-ash-dark p-6">
          <h3 className="font-display font-semibold text-base mb-5">Change Password</h3>
          <form onSubmit={handleSubmit((d) => changePassword.mutate(d))} className="space-y-4 max-w-sm">
            <Input
              label="Current Password"
              type="password"
              error={errors.currentPassword?.message}
              {...register('currentPassword')}
            />
            <Input
              label="New Password"
              type="password"
              error={errors.newPassword?.message}
              hint="Minimum 8 characters"
              {...register('newPassword')}
            />
            <Input
              label="Confirm New Password"
              type="password"
              error={errors.confirmPassword?.message}
              {...register('confirmPassword')}
            />
            <Button type="submit" loading={changePassword.isPending}>Update Password</Button>
          </form>
        </div>
      )}
    </div>
  )
}
