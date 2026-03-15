import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ShoppingBag, Search, User, Menu, X, ChevronDown } from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import { useCartStore } from '@/store/cartStore'
import { useAuth } from '@/hooks/useAuth'
import { cn } from '@/utils'

export function Navbar() {
  const [scrolled, setScrolled] = useState(false)
  const [searchVal, setSearchVal] = useState('')
  const [searchOpen, setSearchOpen] = useState(false)
  const [userMenuOpen, setUserMenuOpen] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const { user, isAuthenticated } = useAuthStore()
  const { itemCount, openCart } = useCartStore()
  const { logout } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20)
    window.addEventListener('scroll', onScroll)
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    if (searchVal.trim()) {
      navigate(`/products?q=${encodeURIComponent(searchVal.trim())}`)
      setSearchOpen(false)
      setSearchVal('')
    }
  }

  const navLinks = [
    { label: 'New Arrivals', href: '/products?sortBy=newest' },
    { label: 'Sale', href: '/products?sale=true' },
    { label: 'Categories', href: '/categories' },
  ]

  return (
    <>
      <nav className={cn(
        'fixed top-0 left-0 right-0 z-40 transition-all duration-300',
        scrolled ? 'bg-ash/95 backdrop-blur-sm shadow-sm' : 'bg-ash',
        'border-b-2 border-ink/8'
      )}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
          <div className="flex items-center justify-between h-16">

            {/* Logo */}
            <Link to="/" className="flex items-center gap-2 group">
              <div className="w-8 h-8 bg-ember flex items-center justify-center">
                <span className="text-white font-display font-black text-sm">K</span>
              </div>
              <span className="font-display font-black text-xl text-ink tracking-tight">KURA</span>
            </Link>

            {/* Desktop Nav */}
            <div className="hidden md:flex items-center gap-8">
              {navLinks.map(link => (
                <Link
                  key={link.href}
                  to={link.href}
                  className="font-body text-sm font-medium text-ink/60 hover:text-ink transition-colors duration-150 relative group"
                >
                  {link.label}
                  <span className="absolute -bottom-0.5 left-0 w-0 h-0.5 bg-ember group-hover:w-full transition-all duration-200" />
                </Link>
              ))}
            </div>

            {/* Actions */}
            <div className="flex items-center gap-1">
              {/* Search */}
              <button
                onClick={() => setSearchOpen(!searchOpen)}
                className="p-2 hover:bg-ink/5 transition-colors"
                aria-label="Search"
              >
                <Search size={18} strokeWidth={2} />
              </button>

              {/* Cart */}
              <button
                onClick={openCart}
                className="relative p-2 hover:bg-ink/5 transition-colors"
                aria-label="Cart"
              >
                <ShoppingBag size={18} strokeWidth={2} />
                {itemCount > 0 && (
                  <span className="absolute -top-0.5 -right-0.5 min-w-4 h-4 bg-ember text-white text-[10px] font-mono font-bold flex items-center justify-center rounded-full px-1">
                    {itemCount > 99 ? '99+' : itemCount}
                  </span>
                )}
              </button>

              {/* User */}
              {isAuthenticated ? (
                <div className="relative ml-1">
                  <button
                    onClick={() => setUserMenuOpen(!userMenuOpen)}
                    className="flex items-center gap-1.5 px-3 py-1.5 hover:bg-ink/5 transition-colors"
                  >
                    <div className="w-6 h-6 bg-ink flex items-center justify-center">
                      <span className="text-ash text-xs font-display font-bold">
                        {user?.fullName?.[0]?.toUpperCase()}
                      </span>
                    </div>
                    <ChevronDown size={14} className={cn('transition-transform', userMenuOpen && 'rotate-180')} />
                  </button>

                  {userMenuOpen && (
                    <div className="absolute right-0 top-full mt-1 w-48 bg-white border-2 border-ink shadow-hard z-50">
                      <div className="px-4 py-3 border-b border-ink/8">
                        <p className="font-display font-semibold text-sm text-ink truncate">{user?.fullName}</p>
                        <p className="text-xs text-ink/40 font-mono truncate">{user?.email}</p>
                      </div>
                      {[
                        { label: 'Profile', href: '/profile' },
                        { label: 'My Orders', href: '/orders' },
                        ...(user?.role === 'ADMIN' ? [{ label: 'Admin Dashboard', href: '/admin' }] : []),
                      ].map(item => (
                        <Link
                          key={item.href}
                          to={item.href}
                          onClick={() => setUserMenuOpen(false)}
                          className="block px-4 py-2.5 text-sm font-body text-ink hover:bg-ash transition-colors"
                        >
                          {item.label}
                        </Link>
                      ))}
                      <div className="border-t border-ink/8">
                        <button
                          onClick={() => { setUserMenuOpen(false); logout.mutate() }}
                          className="block w-full text-left px-4 py-2.5 text-sm font-body text-ink/50 hover:text-ink hover:bg-ash transition-colors"
                        >
                          Sign out
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <Link
                  to="/login"
                  className="hidden md:flex items-center gap-1.5 px-4 py-2 border-2 border-ink text-ink text-sm font-display font-semibold hover:bg-ink hover:text-ash transition-all duration-150 ml-1"
                >
                  Sign in
                </Link>
              )}

              {/* Mobile menu toggle */}
              <button
                onClick={() => setMobileOpen(!mobileOpen)}
                className="md:hidden p-2 hover:bg-ink/5 transition-colors ml-1"
              >
                {mobileOpen ? <X size={18} /> : <Menu size={18} />}
              </button>
            </div>
          </div>

          {/* Search bar (slide-down) */}
          <div className={cn(
            'overflow-hidden transition-all duration-200',
            searchOpen ? 'max-h-16 pb-3' : 'max-h-0'
          )}>
            <form onSubmit={handleSearch}>
              <div className="relative">
                <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink/40" />
                <input
                  autoFocus={searchOpen}
                  value={searchVal}
                  onChange={e => setSearchVal(e.target.value)}
                  placeholder="Search products…"
                  className="w-full pl-9 pr-4 py-2.5 bg-white border-2 border-ink/20 focus:border-ember focus:outline-none text-sm font-body"
                />
              </div>
            </form>
          </div>
        </div>

        {/* Mobile menu */}
        {mobileOpen && (
          <div className="md:hidden bg-ash border-t-2 border-ink/8 px-4 py-4 space-y-1">
            {navLinks.map(link => (
              <Link
                key={link.href}
                to={link.href}
                onClick={() => setMobileOpen(false)}
                className="block py-2.5 font-body font-medium text-ink hover:text-ember transition-colors"
              >
                {link.label}
              </Link>
            ))}
            {!isAuthenticated && (
              <Link
                to="/login"
                onClick={() => setMobileOpen(false)}
                className="block pt-3 mt-2 border-t border-ink/10 font-display font-semibold text-ember"
              >
                Sign in
              </Link>
            )}
          </div>
        )}
      </nav>
      {/* Spacer */}
      <div className="h-16" />

      {/* Overlay for user menu */}
      {userMenuOpen && (
        <div className="fixed inset-0 z-30" onClick={() => setUserMenuOpen(false)} />
      )}
    </>
  )
}
