import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { ArrowRight, Zap, Shield, Truck } from 'lucide-react'
import { productsApi, categoriesApi } from '@/api/products'
import { ProductGrid } from '@/components/product/ProductGrid'
import { formatPrice } from '@/utils'

export function HomePage() {
  const { data: featuredData, isLoading: featuredLoading } = useQuery({
    queryKey: ['products', 'featured'],
    queryFn: () => productsApi.getFeatured().then(r => r.data.data),
  })

  const { data: categoriesData } = useQuery({
    queryKey: ['categories'],
    queryFn: () => categoriesApi.list().then(r => r.data.data),
  })

  const { data: newArrivalsData, isLoading: newLoading } = useQuery({
    queryKey: ['products', 'new'],
    queryFn: () => productsApi.search({ sortBy: 'newest', size: 8 }).then(r => r.data.data),
  })

  return (
    <div className="overflow-hidden">

      {/* ── Hero ────────────────────────────────────────────────────────── */}
      <section className="relative bg-ink text-ash overflow-hidden bg-noise">
        {/* Background geometric accent */}
        <div className="absolute right-0 top-0 w-1/2 h-full bg-ember/5 pointer-events-none" />
        <div className="absolute right-[20%] top-[15%] w-64 h-64 border border-ash/5 rotate-12 pointer-events-none" />
        <div className="absolute right-[25%] top-[25%] w-40 h-40 border border-ash/5 rotate-45 pointer-events-none" />

        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-24 md:py-36 relative">
          <div className="max-w-2xl">
            <p className="section-label text-ash/30 mb-4">New Season · 2024</p>
            <h1 className="font-display font-black text-5xl md:text-7xl leading-[0.92] tracking-tight mb-6">
              CURATED<br />
              <span className="text-ember">GOODS</span><br />
              FOR LIFE.
            </h1>
            <p className="font-body text-ash/60 text-lg max-w-md leading-relaxed mb-10">
              Discover handpicked products from independent makers. Quality, authenticity, and care in every detail.
            </p>
            <div className="flex flex-wrap gap-4">
              <Link
                to="/products"
                className="inline-flex items-center gap-2 bg-ember text-white px-8 py-4 font-display font-semibold text-sm hover:bg-ember-dark transition-colors group"
              >
                Shop Now
                <ArrowRight size={16} className="group-hover:translate-x-1 transition-transform" />
              </Link>
              <Link
                to="/products?sortBy=newest"
                className="inline-flex items-center gap-2 border-2 border-ash/30 text-ash px-8 py-4 font-display font-semibold text-sm hover:border-ash hover:bg-ash/5 transition-all"
              >
                New Arrivals
              </Link>
            </div>
          </div>
        </div>

        {/* Stats bar */}
        <div className="border-t border-ash/8 bg-ash/3">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 py-5 flex flex-wrap gap-8">
            {[
              ['10,000+', 'Products'],
              ['500+', 'Brands'],
              ['98%', 'Satisfaction'],
              ['Free', 'Returns'],
            ].map(([num, label]) => (
              <div key={label} className="flex items-baseline gap-2">
                <span className="font-display font-black text-2xl text-ember">{num}</span>
                <span className="font-mono text-xs text-ash/40 uppercase tracking-wide">{label}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Categories ──────────────────────────────────────────────────── */}
      {categoriesData && categoriesData.length > 0 && (
        <section className="max-w-7xl mx-auto px-4 sm:px-6 py-16">
          <div className="flex items-end justify-between mb-8">
            <div>
              <p className="section-label">Browse by</p>
              <h2 className="font-display font-bold text-3xl text-ink">Categories</h2>
            </div>
            <Link to="/categories" className="font-body text-sm text-ember hover:underline flex items-center gap-1">
              All <ArrowRight size={14} />
            </Link>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
            {categoriesData.slice(0, 6).map((cat, i) => (
              <Link
                key={cat.id}
                to={`/products?categoryId=${cat.id}`}
                className="group flex flex-col items-center gap-3 p-5 bg-white border border-ash-dark hover:border-ember hover:shadow-ember/10 hover:shadow-md transition-all duration-200"
                style={{ animationDelay: `${i * 0.06}s` }}
              >
                <div className="w-12 h-12 bg-ash-dark flex items-center justify-center">
                  {cat.imageUrl ? (
                    <img src={cat.imageUrl} alt={cat.name} className="w-8 h-8 object-contain" />
                  ) : (
                    <span className="font-display font-black text-xl text-ink/30">
                      {cat.name[0].toUpperCase()}
                    </span>
                  )}
                </div>
                <span className="font-body text-xs font-medium text-ink text-center leading-tight group-hover:text-ember transition-colors">
                  {cat.name}
                </span>
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* ── Featured Products ────────────────────────────────────────────── */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 py-4">
        <div className="flex items-end justify-between mb-8">
          <div>
            <p className="section-label">Editor's Pick</p>
            <h2 className="font-display font-bold text-3xl text-ink">Featured</h2>
          </div>
          <Link to="/products?featured=true" className="font-body text-sm text-ember hover:underline flex items-center gap-1">
            View All <ArrowRight size={14} />
          </Link>
        </div>
        <ProductGrid products={featuredData ?? []} loading={featuredLoading} />
      </section>

      {/* ── Promotional Banner ───────────────────────────────────────────── */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 py-12">
        <div className="bg-sage relative overflow-hidden p-12 md:p-16">
          <div className="absolute right-0 bottom-0 w-64 h-64 bg-sage-light/20 rounded-tl-full" />
          <div className="relative max-w-lg">
            <Badge className="bg-white/20 text-white mb-4">Limited Offer</Badge>
            <h2 className="font-display font-black text-4xl md:text-5xl text-white mb-4 leading-tight">
              UP TO 40% OFF SELECTED ITEMS
            </h2>
            <p className="font-body text-white/70 mb-8">
              Handpicked deals on premium goods. Limited quantities available.
            </p>
            <Link
              to="/products?sale=true"
              className="inline-flex items-center gap-2 bg-white text-sage font-display font-semibold px-7 py-3.5 hover:bg-ash transition-colors"
            >
              Shop the Sale <ArrowRight size={16} />
            </Link>
          </div>
        </div>
      </section>

      {/* ── New Arrivals ─────────────────────────────────────────────────── */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 py-4 pb-16">
        <div className="flex items-end justify-between mb-8">
          <div>
            <p className="section-label">Just Dropped</p>
            <h2 className="font-display font-bold text-3xl text-ink">New Arrivals</h2>
          </div>
          <Link to="/products?sortBy=newest" className="font-body text-sm text-ember hover:underline flex items-center gap-1">
            View All <ArrowRight size={14} />
          </Link>
        </div>
        <ProductGrid products={newArrivalsData ?? []} loading={newLoading} />
      </section>

      {/* ── Trust Signals ────────────────────────────────────────────────── */}
      <section className="border-t-2 border-ink/8 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-10">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-8">
            {[
              { icon: Truck,  title: 'Free Shipping', desc: 'On orders over ¥3,000' },
              { icon: Shield, title: 'Secure Payments', desc: 'PayPay & all major cards' },
              { icon: Zap,    title: 'Fast Delivery',  desc: 'Same-day in major cities' },
            ].map(({ icon: Icon, title, desc }) => (
              <div key={title} className="flex items-center gap-5">
                <div className="w-12 h-12 bg-ash-dark flex items-center justify-center flex-shrink-0">
                  <Icon size={20} className="text-ember" />
                </div>
                <div>
                  <p className="font-display font-semibold text-sm text-ink">{title}</p>
                  <p className="font-body text-xs text-ink/50 mt-0.5">{desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  )
}

function Badge({ children, className }: { children: React.ReactNode; className?: string }) {
  return (
    <span className={`inline-flex items-center px-2.5 py-1 text-xs font-mono font-medium uppercase tracking-wider ${className}`}>
      {children}
    </span>
  )
}
