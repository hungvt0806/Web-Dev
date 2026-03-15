import React, { useState, useEffect } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { ShoppingBag, Search, User, Menu, X, ChevronDown, Package, LogOut, LayoutDashboard } from 'lucide-react'
import { useAuthStore, useIsAdmin } from '@/store/authStore'
import { useCartCount } from '@/store/cartStore'
import { cn } from '@/utils'

export const Header = () => {
  const [menuOpen,   setMenuOpen]   = useState(false)
  const [userOpen,   setUserOpen]   = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)
  const [scrolled,   setScrolled]   = useState(false)
  const [query,      setQuery]      = useState('')

  const { user, isAuth, logout } = useAuthStore()
  const cartCount = useCartCount()
  const isAdmin   = useIsAdmin()
  const navigate  = useNavigate()
  const location  = useLocation()

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 12)
    window.addEventListener('scroll', onScroll)
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  useEffect(() => { setMenuOpen(false) }, [location.pathname])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    if (query.trim()) { navigate(`/products?q=${encodeURIComponent(query.trim())}`); setSearchOpen(false); setQuery('') }
  }

  const handleLogout = async () => { await logout(); navigate('/'); setUserOpen(false) }

  const navLinks = [
    { to: '/products',       label: 'Shop' },
    { to: '/products?featured=true', label: 'New Arrivals' },
  ]

  return (
    <>
      <header className={cn(
        'fixed top-0 left-0 right-0 z-40 transition-all duration-300',
        scrolled ? 'bg-cream/95 backdrop-blur-md shadow-sm border-b border-border/60' : 'bg-cream/80 backdrop-blur-sm'
      )}>
        <div className="container-page">
          <div className="flex items-center justify-between h-16 gap-4">

            {/* Logo */}
            <Link to="/" className="flex items-center gap-2 flex-shrink-0">
              <span className="font-display text-xl font-semibold tracking-tight text-ink">
                shōp<span className="text-accent">·</span>
              </span>
            </Link>

            {/* Desktop nav */}
            <nav className="hidden md:flex items-center gap-1">
              {navLinks.map(l => (
                <Link key={l.to} to={l.to}
                  className={cn(
                    'px-3 py-1.5 rounded-lg text-sm font-medium transition-colors',
                    location.pathname === l.to.split('?')[0]
                      ? 'bg-cream-warm text-ink'
                      : 'text-ink-muted hover:text-ink hover:bg-cream-warm'
                  )}
                >{l.label}</Link>
              ))}
            </nav>

            {/* Actions */}
            <div className="flex items-center gap-1">

              {/* Search */}
              <button onClick={() => setSearchOpen(true)}
                className="btn-ghost btn-sm rounded-xl p-2.5" aria-label="Search">
                <Search className="h-4 w-4" />
              </button>

              {/* Cart */}
              <Link to="/cart" className="btn-ghost btn-sm rounded-xl p-2.5 relative" aria-label="Cart">
                <ShoppingBag className="h-4 w-4" />
                {cartCount > 0 && (
                  <span className="absolute -top-0.5 -right-0.5 h-4 min-w-4 px-0.5 bg-accent text-white text-2xs font-mono font-medium rounded-full flex items-center justify-center leading-none">
                    {cartCount > 99 ? '99+' : cartCount}
                  </span>
                )}
              </Link>

              {/* User menu */}
              {isAuth ? (
                <div className="relative">
                  <button onClick={() => setUserOpen(o => !o)}
                    className="flex items-center gap-2 btn-ghost btn-sm rounded-xl px-2.5">
                    <div className="w-7 h-7 rounded-lg bg-cream-warm border border-border flex items-center justify-center text-xs font-medium">
                      {user?.fullName?.[0]?.toUpperCase() ?? 'U'}
                    </div>
                    <ChevronDown className={cn('h-3 w-3 transition-transform', userOpen && 'rotate-180')} />
                  </button>
                  {userOpen && (
                    <>
                      <div className="fixed inset-0 z-10" onClick={() => setUserOpen(false)} />
                      <div className="absolute right-0 top-full mt-2 w-52 card py-1.5 z-20 animate-scale-in">
                        <div className="px-3 py-2 border-b border-border mb-1">
                          <p className="text-sm font-medium">{user?.fullName}</p>
                          <p className="text-xs text-ink-muted truncate">{user?.email}</p>
                        </div>
                        <MenuItem to="/profile" icon={<User className="h-4 w-4" />} label="My Profile" onClick={() => setUserOpen(false)} />
                        <MenuItem to="/orders"  icon={<Package className="h-4 w-4" />} label="Orders" onClick={() => setUserOpen(false)} />
                        {isAdmin && <MenuItem to="/admin" icon={<LayoutDashboard className="h-4 w-4" />} label="Admin Panel" onClick={() => setUserOpen(false)} />}
                        <div className="border-t border-border mt-1 pt-1">
                          <button onClick={handleLogout}
                            className="w-full flex items-center gap-2.5 px-3 py-2 text-sm text-error hover:bg-red-50 rounded-lg transition-colors">
                            <LogOut className="h-4 w-4" /><span>Sign out</span>
                          </button>
                        </div>
                      </div>
                    </>
                  )}
                </div>
              ) : (
                <Link to="/login" className="btn-primary btn-sm ml-1">Sign in</Link>
              )}

              {/* Mobile menu toggle */}
              <button onClick={() => setMenuOpen(o => !o)} className="md:hidden btn-ghost btn-sm rounded-xl p-2.5 ml-1">
                {menuOpen ? <X className="h-4 w-4" /> : <Menu className="h-4 w-4" />}
              </button>
            </div>
          </div>
        </div>

        {/* Mobile nav */}
        {menuOpen && (
          <div className="md:hidden border-t border-border bg-cream animate-fade-up">
            <nav className="container-page py-4 flex flex-col gap-1">
              {navLinks.map(l => (
                <Link key={l.to} to={l.to}
                  className="px-4 py-2.5 rounded-xl text-sm font-medium text-ink hover:bg-cream-warm">
                  {l.label}
                </Link>
              ))}
            </nav>
          </div>
        )}
      </header>

      {/* Search overlay */}
      {searchOpen && (
        <div className="fixed inset-0 z-50 flex items-start justify-center pt-[20vh] px-4">
          <div className="absolute inset-0 bg-ink/30 backdrop-blur-sm" onClick={() => setSearchOpen(false)} />
          <div className="relative w-full max-w-xl animate-scale-in">
            <form onSubmit={handleSearch} className="card-lg shadow-lift">
              <div className="flex items-center gap-3 p-4">
                <Search className="h-5 w-5 text-ink-muted flex-shrink-0" />
                <input
                  autoFocus
                  type="text"
                  value={query}
                  onChange={e => setQuery(e.target.value)}
                  placeholder="Search products…"
                  className="flex-1 bg-transparent text-base outline-none placeholder:text-ink-muted"
                />
                <button type="button" onClick={() => setSearchOpen(false)} className="text-ink-muted hover:text-ink">
                  <X className="h-5 w-5" />
                </button>
              </div>
            </form>
            <p className="text-center text-xs text-ink-muted mt-3">Press Enter to search · Esc to close</p>
          </div>
        </div>
      )}
    </>
  )
}

const MenuItem = ({ to, icon, label, onClick }: { to: string; icon: React.ReactNode; label: string; onClick?: () => void }) => (
  <Link to={to} onClick={onClick}
    className="flex items-center gap-2.5 px-3 py-2 text-sm text-ink hover:bg-cream-warm rounded-lg transition-colors">
    <span className="text-ink-muted">{icon}</span>
    {label}
  </Link>
)
