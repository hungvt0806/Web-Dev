import { NavLink, Outlet, Navigate } from 'react-router-dom'
import { LayoutDashboard, Package, ShoppingBag, Users, Settings, ChevronRight, LogOut } from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import { useAuth } from '@/hooks/useAuth'
import { cn } from '@/utils'

const NAV = [
  { label: 'Dashboard',  href: '/admin',          icon: LayoutDashboard, exact: true },
  { label: 'Products',   href: '/admin/products',  icon: Package },
  { label: 'Orders',     href: '/admin/orders',    icon: ShoppingBag },
  { label: 'Customers',  href: '/admin/customers', icon: Users },
  { label: 'Settings',   href: '/admin/settings',  icon: Settings },
]

export function AdminLayout() {
  const { user } = useAuthStore()
  const { logout } = useAuth()

  if (!user || user.role !== 'ADMIN') return <Navigate to="/" replace />

  return (
    <div className="min-h-screen flex bg-ash">
      {/* Sidebar */}
      <aside className="hidden md:flex flex-col w-56 bg-ink text-ash flex-shrink-0 sticky top-0 h-screen overflow-y-auto">
        {/* Logo */}
        <div className="px-5 py-5 border-b border-ash/8">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 bg-ember flex items-center justify-center">
              <span className="text-white font-display font-black text-xs">K</span>
            </div>
            <div>
              <p className="font-display font-black text-sm leading-none">KURA</p>
              <p className="font-mono text-[9px] text-ash/35 uppercase tracking-widest mt-0.5">Admin</p>
            </div>
          </div>
        </div>

        {/* Nav */}
        <nav className="flex-1 px-2 py-4">
          {NAV.map(item => (
            <NavLink
              key={item.href}
              to={item.href}
              end={item.exact}
              className={({ isActive }) => cn(
                'flex items-center gap-3 px-3 py-2.5 text-sm font-body transition-all group mb-0.5',
                isActive
                  ? 'bg-ember text-white'
                  : 'text-ash/50 hover:text-ash hover:bg-ash/5'
              )}
            >
              {({ isActive }) => (
                <>
                  <item.icon size={15} />
                  <span className="flex-1">{item.label}</span>
                  {isActive && <ChevronRight size={12} />}
                </>
              )}
            </NavLink>
          ))}
        </nav>

        {/* User */}
        <div className="border-t border-ash/8 px-3 py-4">
          <div className="flex items-center gap-2.5 px-2 mb-2">
            <div className="w-7 h-7 bg-ash/15 flex items-center justify-center text-xs font-display font-bold flex-shrink-0">
              {user.fullName[0].toUpperCase()}
            </div>
            <div className="min-w-0">
              <p className="text-xs font-body text-ash/80 truncate">{user.fullName}</p>
              <p className="text-[10px] font-mono text-ash/35 truncate">{user.email}</p>
            </div>
          </div>
          <button
            onClick={() => logout.mutate()}
            className="w-full flex items-center gap-2 px-3 py-2 text-xs font-body text-ash/35 hover:text-ash hover:bg-ash/5 transition-colors"
          >
            <LogOut size={12} /> Sign out
          </button>
        </div>
      </aside>

      {/* Content */}
      <main className="flex-1 min-w-0">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 py-8">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
