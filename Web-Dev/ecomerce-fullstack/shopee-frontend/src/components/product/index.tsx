import React from 'react'
import { Link } from 'react-router-dom'
import { ShoppingBag, Heart, Eye } from 'lucide-react'
import type { Product } from '@/types'
import { PriceDisplay, StarRating, ProductCardSkeleton } from '@/components/ui'
import { useCartStore } from '@/store/cartStore'
import { cn } from '@/utils'

// ── ProductCard ───────────────────────────────────────────────────────────

interface ProductCardProps {
  product: Product
  variant?: 'grid' | 'list'
  className?: string
}

export const ProductCard = ({ product, variant = 'grid', className }: ProductCardProps) => {
  const [hovered, setHovered] = React.useState(false)
  const [imgError, setImgError] = React.useState(false)
  const { addItem, isUpdating } = useCartStore()

  const handleAddToCart = (e: React.MouseEvent) => {
    e.preventDefault()
    e.stopPropagation()
    addItem(product.id, undefined, 1, product.name)
  }

  if (variant === 'list') {
    return (
      <Link to={`/products/${product.slug}`}
        className={cn('card flex gap-4 p-4 hover:shadow-card-lg transition-shadow group', className)}>
        <div className="w-24 h-24 flex-shrink-0 rounded-xl overflow-hidden bg-cream-warm">
          {!imgError && product.thumbnailUrl
            ? <img src={product.thumbnailUrl} alt={product.name} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" onError={() => setImgError(true)} />
            : <div className="w-full h-full flex items-center justify-center text-ink-muted/20"><ShoppingBag className="h-8 w-8" /></div>
          }
        </div>
        <div className="flex-1 min-w-0">
          <h3 className="text-sm font-medium text-ink leading-snug line-clamp-2">{product.name}</h3>
          {product.ratingCount > 0 && <div className="mt-1"><StarRating rating={product.ratingAvg} count={product.ratingCount} /></div>}
          <PriceDisplay price={product.basePrice} originalPrice={product.originalPrice} size="sm" className="mt-2" />
        </div>
      </Link>
    )
  }

  return (
    <Link to={`/products/${product.slug}`}
      className={cn('card overflow-hidden group cursor-pointer', className)}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      {/* Image */}
      <div className="relative aspect-square overflow-hidden bg-cream-warm">
        {!imgError && product.thumbnailUrl
          ? <img src={product.thumbnailUrl} alt={product.name}
              className={cn('w-full h-full object-cover transition-transform duration-500', hovered && 'scale-105')}
              onError={() => setImgError(true)} />
          : <div className="w-full h-full flex items-center justify-center text-ink-muted/20">
              <ShoppingBag className="h-12 w-12" />
            </div>
        }

        {/* Badges */}
        <div className="absolute top-3 left-3 flex flex-col gap-1.5">
          {product.featured && <span className="badge-accent">Featured</span>}
          {product.originalPrice && product.originalPrice > product.basePrice && (
            <span className="badge bg-ink text-cream">
              -{Math.round((1 - product.basePrice / product.originalPrice) * 100)}%
            </span>
          )}
        </div>

        {/* Hover actions */}
        <div className={cn(
          'absolute bottom-3 right-3 flex flex-col gap-1.5 transition-all duration-200',
          hovered ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-2'
        )}>
          <button
            onClick={handleAddToCart}
            disabled={isUpdating || product.totalStock === 0}
            title="Add to cart"
            className="w-9 h-9 bg-ink text-cream rounded-xl flex items-center justify-center hover:bg-ink-light transition-colors shadow-lift disabled:opacity-40"
          ><ShoppingBag className="h-4 w-4" /></button>
          <button
            onClick={e => { e.preventDefault(); e.stopPropagation() }}
            title="Wishlist"
            className="w-9 h-9 bg-white text-ink rounded-xl flex items-center justify-center hover:bg-cream-warm transition-colors shadow-card"
          ><Heart className="h-4 w-4" /></button>
        </div>

        {product.totalStock === 0 && (
          <div className="absolute inset-0 bg-cream/60 backdrop-blur-[1px] flex items-center justify-center">
            <span className="badge-default">Out of Stock</span>
          </div>
        )}
      </div>

      {/* Info */}
      <div className="p-4">
        <p className="text-xs text-ink-muted mb-1">{product.category?.name}</p>
        <h3 className="text-sm font-medium text-ink leading-snug line-clamp-2 mb-2">{product.name}</h3>
        {product.ratingCount > 0 && <StarRating rating={product.ratingAvg} count={product.ratingCount} />}
        <PriceDisplay price={product.basePrice} originalPrice={product.originalPrice} size="sm" className="mt-2" />
        {product.soldCount > 0 && (
          <p className="text-2xs text-ink-muted mt-1">{product.soldCount.toLocaleString()} sold</p>
        )}
      </div>
    </Link>
  )
}

// ── ProductGrid ───────────────────────────────────────────────────────────

interface ProductGridProps {
  products:   Product[]
  loading?:   boolean
  columns?:   2 | 3 | 4
  variant?:   'grid' | 'list'
}

export const ProductGrid = ({ products, loading, columns = 4, variant = 'grid' }: ProductGridProps) => {
  if (variant === 'list') {
    return (
      <div className="space-y-3">
        {loading
          ? Array.from({ length: 6 }).map((_, i) => <ProductCardSkeleton key={i} />)
          : products.map(p => <ProductCard key={p.id} product={p} variant="list" />)
        }
      </div>
    )
  }

  const gridClass = {
    2: 'grid-cols-2',
    3: 'grid-cols-2 sm:grid-cols-3',
    4: 'grid-cols-2 sm:grid-cols-3 lg:grid-cols-4',
  }[columns]

  return (
    <div className={cn('grid gap-4 md:gap-5', gridClass)}>
      {loading
        ? Array.from({ length: 8 }).map((_, i) => <ProductCardSkeleton key={i} />)
        : products.map(p => <ProductCard key={p.id} product={p} />)
      }
    </div>
  )
}

// ── ProductImageGallery ───────────────────────────────────────────────────

export const ProductImageGallery = ({ images, name }: { images: string[]; name: string }) => {
  const [active, setActive] = React.useState(0)
  const [imgError, setImgError] = React.useState(false)

  if (!images.length) {
    return (
      <div className="aspect-square rounded-3xl bg-cream-warm flex items-center justify-center">
        <ShoppingBag className="h-16 w-16 text-ink-muted/20" />
      </div>
    )
  }

  return (
    <div className="space-y-3">
      {/* Main image */}
      <div className="aspect-square rounded-3xl overflow-hidden bg-cream-warm">
        {!imgError
          ? <img src={images[active]} alt={`${name} - ${active + 1}`}
              className="w-full h-full object-cover"
              onError={() => setImgError(true)} />
          : <div className="w-full h-full flex items-center justify-center"><Eye className="h-12 w-12 text-ink-muted/20" /></div>
        }
      </div>
      {/* Thumbnails */}
      {images.length > 1 && (
        <div className="flex gap-2 overflow-x-auto no-scrollbar">
          {images.map((img, i) => (
            <button key={i} onClick={() => { setActive(i); setImgError(false) }}
              className={cn(
                'flex-shrink-0 w-16 h-16 rounded-xl overflow-hidden border-2 transition-all',
                i === active ? 'border-ink' : 'border-transparent opacity-60 hover:opacity-100'
              )}>
              <img src={img} alt={`${name} thumbnail ${i + 1}`} className="w-full h-full object-cover" />
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
