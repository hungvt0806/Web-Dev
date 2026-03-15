import React, { useState } from 'react'
import { Link, Outlet, NavLink, useNavigate } from 'react-router-dom'
import { Header } from './Header'
import {
  LayoutDashboard, Package, ShoppingCart, Users, ChevronLeft, Menu,
  Instagram, Twitter, Globe, Mail
} from 'lucide-react'
import { cn } from '@/utils'
import { useAuthStore } from '@/store/authStore'

// ── Footer ────────────────────────────────────────────────────────────────

export const Footer = () => (
  <footer className="bg-ink text-cream/80 mt-auto">
    <div className="container-page py-12">
      <div className="grid grid-cols-2 md:grid-cols-4 gap-8 mb-10">
        <div className="col-span-2 md:col-span-1">
          <Link to="/" className="font-display text-xl font-semibold text-cream">
            shōp<span className="text-accent">·</span>
          </Link>
          <p className="mt-3 text-sm text-cream/50 leading-relaxed">
            Curated products at exceptional value. Delivered with care.
          </p>
          <div className="flex gap-3 mt-4">
            {[Instagram, Twitter, Globe].map((Icon, i) => (
              <a key={i} href="#" className="w-8 h-8 rounded-lg bg-white/5 hover:bg-white/10 flex items-center justify-center transition-colors">
                <Icon className="h-3.5 w-3.5" />
              </a>
            ))}
          </div>
        </div>
        <FooterCol title="Shop" links={[
          { to: '/products', label: 'All Products' },
          { to: '/products?featured=true', label: 'New Arrivals' },
          { to: '/products?sort=soldCount,desc', label: 'Best Sellers' },
        ]} />
        <FooterCol title="Support" links={[
          { to: '/orders', label: 'Track Order' },
          { to: '#', label: 'Returns & Refunds' },
          { to: '#', label: 'Help Center' },
        ]} />
        <FooterCol title="Company" links={[
          { to: '#', label: 'About Us' },
          { to: '#', label: 'Privacy Policy' },
          { to: '#', label: 'Terms of Service' },
        ]} />
      </div>
      <div className="border-t border-white/10 pt-6 flex flex-col sm:flex-row items-center justify-between gap-3">
        <p className="text-xs text-cream/30">© {new Date().getFullYear()} Shōp. All rights reserved.</p>
        <div className="flex items-center gap-1.5 text-xs text-cream/30">
          <Mail className="h-3 w-3" />
          <span>support@shop.example.com</span>
        </div>
      </div>
    </div>
  </footer>
)

const FooterCol = ({ title, links }: { title: string; links: { to: string; label: string }[] }) => (
  <div>
    <h4 className="text-xs font-mono font-medium tracking-widest text-cream/30 uppercase mb-3">{title}</h4>
    <ul className="space-y-2">
      {links.map(l => (
        <li key={l.label}><Link to={l.to} className="text-sm text-cream/60 hover:text-cream transition-colors">{l.label}</Link></li>
      ))}
    </ul>
  </div>
)

// ── Main Layout ───────────────────────────────────────────────────────────

export const MainLayout = () => (
  <div className="flex flex-col min-h-screen">
    <Header />
    <main className="flex-1 pt-16">
      <Outlet />
    </main>
    <Footer />
  </div>
)

// ── Admin Layout ──────────────────────────────────────────────────────────

const adminNav = [
  { to: '/admin',          icon: LayoutDashboard, label: 'Dashboard',  exact: true },
  { to: '/admin/products', icon: Package,         label: 'Products' },
  { to: '/admin/orders',   icon: ShoppingCart,    label: 'Orders' },
  { to: '/admin/users',    icon: Users,           label: 'Users' },
]

export const AdminLayout = () => {
  const [collapsed, setCollapsed] = useState(false)
  const { user, logout } = useAuthStore()
  const navigate = useNavigate()

  const handleLogout = async () => { await logout(); navigate('/') }

  return (
    <div className="flex min-h-screen bg-cream-warm">

      {/* Sidebar */}
      <aside className={cn(
        'flex-shrink-0 bg-ink text-cream flex flex-col transition-all duration-300 sticky top-0 h-screen',
        collapsed ? 'w-16' : 'w-56'
      )}>
        {/* Logo */}
        <div className="flex items-center justify-between px-4 h-14 border-b border-white/10">
          {!collapsed && (
            <Link to="/" className="font-display text-base font-semibold text-cream">
              shōp<span className="text-accent">·</span>
            </Link>
          )}
          <button onClick={() => setCollapsed(c => !c)}
            className="ml-auto p-1.5 rounded-lg hover:bg-white/10 transition-colors text-cream/60 hover:text-cream">
            {collapsed ? <Menu className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
          </button>
        </div>

        {/* Nav */}
        <nav className="flex-1 py-4 space-y-0.5 px-2">
          {adminNav.map(item => (
            <NavLink key={item.to} to={item.to} end={item.exact}
              className={({ isActive }) => cn(
                'flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm transition-all',
                isActive ? 'bg-white/10 text-cream' : 'text-cream/50 hover:bg-white/5 hover:text-cream'
              )}
            >
              <item.icon className="h-4 w-4 flex-shrink-0" />
              {!collapsed && <span className="font-medium">{item.label}</span>}
            </NavLink>
          ))}
        </nav>

        {/* User */}
        <div className="px-2 pb-4 border-t border-white/10 pt-4">
          {!collapsed && (
            <div className="px-3 mb-2">
              <p className="text-xs font-medium text-cream truncate">{user?.fullName}</p>
              <p className="text-2xs text-cream/40 truncate">{user?.email}</p>
            </div>
          )}
          <button onClick={handleLogout}
            className="w-full flex items-center gap-3 px-3 py-2 rounded-xl text-sm text-cream/50 hover:text-cream hover:bg-white/5 transition-colors">
            <ChevronLeft className="h-4 w-4 flex-shrink-0" />
            {!collapsed && 'Sign out'}
          </button>
        </div>
      </aside>

      {/* Content */}
      <div className="flex-1 flex flex-col min-w-0">
        <div className="flex-1 p-6 md:p-8 overflow-auto">
          <Outlet />
        </div>
      </div>
    </div>
  )
}
