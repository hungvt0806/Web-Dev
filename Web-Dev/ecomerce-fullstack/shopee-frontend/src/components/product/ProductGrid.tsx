import type { ProductSummary } from '@/types'
import { ProductCard } from './ProductCard'
import { ProductCardSkeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { Search } from 'lucide-react'

interface ProductGridProps {
  products: ProductSummary[]
  loading?: boolean
  emptyMessage?: string
}

export function ProductGrid({ products, loading, emptyMessage }: ProductGridProps) {
  if (loading) {
    return (
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
        {Array.from({ length: 8 }).map((_, i) => <ProductCardSkeleton key={i} />)}
      </div>
    )
  }
  if (products.length === 0) {
    return (
      <EmptyState
        icon={<Search size={28} />}
        title="No products found"
        description={emptyMessage || 'Try adjusting your search or filters'}
        action={{ label: 'Clear Filters', href: '/products' }}
      />
    )
  }
  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
      {products.map((p, i) => <ProductCard key={p.id} product={p} index={i} />)}
    </div>
  )
}
