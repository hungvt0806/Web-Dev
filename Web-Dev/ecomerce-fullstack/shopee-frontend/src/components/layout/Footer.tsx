import { Link } from 'react-router-dom'
import { Instagram, Twitter, Mail } from 'lucide-react'

export function Footer() {
  return (
    <footer className="bg-ink text-ash mt-24">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-16">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-10">
          {/* Brand */}
          <div className="col-span-2 md:col-span-1">
            <div className="flex items-center gap-2 mb-4">
              <div className="w-8 h-8 bg-ember flex items-center justify-center">
                <span className="text-white font-display font-black text-sm">K</span>
              </div>
              <span className="font-display font-black text-xl tracking-tight">KURA</span>
            </div>
            <p className="text-ash/50 font-body text-sm leading-relaxed">
              Curated goods, delivered with care. Modern commerce for a discerning world.
            </p>
            <div className="flex items-center gap-3 mt-5">
              {[Instagram, Twitter, Mail].map((Icon, i) => (
                <button key={i} className="w-9 h-9 border border-ash/20 flex items-center justify-center hover:border-ember hover:text-ember transition-colors">
                  <Icon size={15} />
                </button>
              ))}
            </div>
          </div>

          {/* Links */}
          {[
            {
              title: 'Shop',
              links: [
                { label: 'New Arrivals', href: '/products?sortBy=newest' },
                { label: 'Best Sellers', href: '/products?sortBy=popular' },
                { label: 'Sale', href: '/products?sale=true' },
                { label: 'All Products', href: '/products' },
              ]
            },
            {
              title: 'Account',
              links: [
                { label: 'Sign In', href: '/login' },
                { label: 'Register', href: '/register' },
                { label: 'My Orders', href: '/orders' },
                { label: 'Profile', href: '/profile' },
              ]
            },
            {
              title: 'Support',
              links: [
                { label: 'Help Center', href: '#' },
                { label: 'Shipping Info', href: '#' },
                { label: 'Returns', href: '#' },
                { label: 'Contact Us', href: '#' },
              ]
            },
          ].map(section => (
            <div key={section.title}>
              <p className="font-mono text-xs uppercase tracking-widest text-ash/30 mb-4">{section.title}</p>
              <ul className="space-y-2.5">
                {section.links.map(link => (
                  <li key={link.href}>
                    <Link
                      to={link.href}
                      className="text-sm font-body text-ash/60 hover:text-ash transition-colors"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="mt-14 pt-6 border-t border-ash/10 flex flex-col sm:flex-row justify-between items-center gap-3">
          <p className="text-xs font-mono text-ash/30">© 2024 KURA. All rights reserved.</p>
          <div className="flex items-center gap-5">
            {['Privacy', 'Terms', 'Cookies'].map(item => (
              <Link key={item} to="#" className="text-xs font-mono text-ash/30 hover:text-ash/60 transition-colors">
                {item}
              </Link>
            ))}
          </div>
        </div>
      </div>
    </footer>
  )
}
