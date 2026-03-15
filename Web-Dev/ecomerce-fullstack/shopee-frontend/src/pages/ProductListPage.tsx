import { useState, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { SlidersHorizontal, X, ChevronDown } from 'lucide-react'
import { productsApi, categoriesApi } from '@/api/products'
import { ProductGrid } from '@/components/product/ProductGrid'
import { Button } from '@/components/ui/Button'
import { formatPrice } from '@/utils'

const SORT_OPTIONS = [
  { value: 'newest',     label: 'Newest' },
  { value: 'popular',    label: 'Most Popular' },
  { value: 'rating',     label: 'Top Rated' },
  { value: 'price_asc',  label: 'Price: Low to High' },
  { value: 'price_desc', label: 'Price: High to Low' },
]

export function ProductListPage() {
  const [params, setParams] = useSearchParams()
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [priceRange, setPriceRange] = useState({ min: '', max: '' })

  const q          = params.get('q') || ''
  const categoryId = params.get('categoryId') ? Number(params.get('categoryId')) : undefined
  const sortBy     = params.get('sortBy') || 'newest'
  const page       = Number(params.get('page') || 1) - 1

  const setParam = (key: string, value: string | null) => {
    const next = new URLSearchParams(params)
    if (value === null) next.delete(key)
    else next.set(key, value)
    next.delete('page')
    setParams(next)
  }

  const { data: productsData, isLoading } = useQuery({
    queryKey: ['products', 'search', q, categoryId, sortBy, priceRange, page],
    queryFn: () => productsApi.search({
      q: q || undefined,
      categoryId,
      sortBy: sortBy as any,
      minPrice: priceRange.min ? Number(priceRange.min) : undefined,
      maxPrice: priceRange.max ? Number(priceRange.max) : undefined,
      page,
      size: 20,
    }).then(r => r.data),
  })

  const { data: categoriesData } = useQuery({
    queryKey: ['categories'],
    queryFn: () => categoriesApi.list().then(r => r.data.data),
  })

  const products = productsData?.data ?? []
  const total    = productsData?.totalElements ?? 0
  const totalPages = productsData?.totalPages ?? 0

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 py-8">

      {/* Page Header */}
      <div className="mb-8">
        <p className="section-label">Discover</p>
        <h1 className="font-display font-bold text-4xl text-ink">
          {q ? `"${q}"` : categoryId ? categoriesData?.find(c => c.id === categoryId)?.name || 'Products' : 'All Products'}
        </h1>
        {total > 0 && (
          <p className="font-mono text-sm text-ink/40 mt-1">{total.toLocaleString()} results</p>
        )}
      </div>

      {/* Active filters */}
      {(q || categoryId) && (
        <div className="flex flex-wrap gap-2 mb-6">
          {q && (
            <button
              onClick={() => setParam('q', null)}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-ink text-ash text-xs font-mono border-2 border-ink hover:bg-ember hover:border-ember transition-colors"
            >
              Search: {q} <X size={11} />
            </button>
          )}
          {categoryId && (
            <button
              onClick={() => setParam('categoryId', null)}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-ink text-ash text-xs font-mono border-2 border-ink hover:bg-ember hover:border-ember transition-colors"
            >
              {categoriesData?.find(c => c.id === categoryId)?.name} <X size={11} />
            </button>
          )}
        </div>
      )}

      <div className="flex flex-col lg:flex-row gap-8">

        {/* ── Sidebar Filters ── */}
        <aside className={`lg:w-56 flex-shrink-0 ${filtersOpen ? 'block' : 'hidden lg:block'}`}>
          <div className="space-y-6 lg:sticky lg:top-24">

            {/* Category */}
            {categoriesData && (
              <FilterSection title="Category">
                <ul className="space-y-1">
                  <li>
                    <button
                      onClick={() => setParam('categoryId', null)}
                      className={`text-sm font-body w-full text-left py-1.5 px-2 transition-colors ${!categoryId ? 'text-ember font-medium bg-ember/5' : 'text-ink/60 hover:text-ink'}`}
                    >
                      All Categories
                    </button>
                  </li>
                  {categoriesData.map(cat => (
                    <li key={cat.id}>
                      <button
                        onClick={() => setParam('categoryId', String(cat.id))}
                        className={`text-sm font-body w-full text-left py-1.5 px-2 transition-colors ${categoryId === cat.id ? 'text-ember font-medium bg-ember/5' : 'text-ink/60 hover:text-ink'}`}
                      >
                        {cat.name}
                      </button>
                    </li>
                  ))}
                </ul>
              </FilterSection>
            )}

            {/* Price Range */}
            <FilterSection title="Price Range">
              <div className="flex gap-2">
                <input
                  type="number"
                  placeholder="Min"
                  value={priceRange.min}
                  onChange={e => setPriceRange(p => ({ ...p, min: e.target.value }))}
                  className="w-full border-2 border-ink/15 px-2 py-1.5 text-sm font-mono focus:border-ember focus:outline-none"
                />
                <input
                  type="number"
                  placeholder="Max"
                  value={priceRange.max}
                  onChange={e => setPriceRange(p => ({ ...p, max: e.target.value }))}
                  className="w-full border-2 border-ink/15 px-2 py-1.5 text-sm font-mono focus:border-ember focus:outline-none"
                />
              </div>
              <Button size="sm" variant="secondary" className="w-full mt-2 text-xs" onClick={() => {}}>
                Apply
              </Button>
            </FilterSection>

          </div>
        </aside>

        {/* ── Products ── */}
        <div className="flex-1 min-w-0">
          {/* Toolbar */}
          <div className="flex items-center justify-between mb-5 gap-4">
            <button
              onClick={() => setFiltersOpen(!filtersOpen)}
              className="lg:hidden flex items-center gap-2 text-sm font-body border-2 border-ink/20 px-4 py-2 hover:border-ink transition-colors"
            >
              <SlidersHorizontal size={14} /> Filters
            </button>

            {/* Sort */}
            <div className="ml-auto flex items-center gap-2">
              <span className="text-xs font-mono text-ink/40 hidden sm:block">Sort:</span>
              <div className="relative">
                <select
                  value={sortBy}
                  onChange={e => setParam('sortBy', e.target.value)}
                  className="appearance-none bg-white border-2 border-ink/20 px-4 pr-8 py-2 text-sm font-body focus:outline-none focus:border-ember cursor-pointer"
                >
                  {SORT_OPTIONS.map(o => (
                    <option key={o.value} value={o.value}>{o.label}</option>
                  ))}
                </select>
                <ChevronDown size={12} className="absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none text-ink/40" />
              </div>
            </div>
          </div>

          <ProductGrid products={products} loading={isLoading} />

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-center gap-2 mt-10">
              {Array.from({ length: Math.min(totalPages, 7) }, (_, i) => i + 1).map(p => (
                <button
                  key={p}
                  onClick={() => setParam('page', String(p))}
                  className={`w-10 h-10 flex items-center justify-center font-mono text-sm border-2 transition-all
                    ${page + 1 === p ? 'bg-ink text-ash border-ink' : 'border-ink/15 text-ink/50 hover:border-ink hover:text-ink'}`}
                >
                  {p}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function FilterSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <p className="section-label mb-3">{title}</p>
      {children}
    </div>
  )
}
