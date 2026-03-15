import { Link } from 'react-router-dom'
import { ShoppingBag } from 'lucide-react'
import type { ProductSummary } from '@/types'
import { formatPrice, getDiscountPercent } from '@/utils'
import { Rating } from '@/components/ui/Rating'
import { Badge } from '@/components/ui/Badge'
import { useCart } from '@/hooks/useCart'

interface ProductCardProps {
  product: ProductSummary
  index?: number
}

export function ProductCard({ product, index = 0 }: ProductCardProps) {
  const { addItem } = useCart()
  const discount = product.originalPrice
    ? getDiscountPercent(product.originalPrice, product.basePrice)
    : 0

  return (
    <div
      className="group bg-white border border-ash-dark overflow-hidden flex flex-col opacity-0 animate-fade-up"
      style={{ animationDelay: `${index * 0.05}s`, animationFillMode: 'forwards' }}
    >
      {/* Image */}
      <Link to={`/products/${product.slug}`} className="relative overflow-hidden aspect-square bg-ash-dark">
        {product.thumbnailUrl ? (
          <img
            src={product.thumbnailUrl}
            alt={product.name}
            className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-ink/15">
            <ShoppingBag size={40} />
          </div>
        )}

        {/* Badges */}
        <div className="absolute top-2 left-2 flex flex-col gap-1">
          {discount > 0 && (
            <Badge variant="ember">-{discount}%</Badge>
          )}
          {product.totalStock === 0 && (
            <Badge className="bg-ink/80 text-ash">Sold out</Badge>
          )}
          {product.soldCount > 1000 && (
            <Badge variant="gold">Popular</Badge>
          )}
        </div>

        {/* Quick add */}
        <button
          onClick={(e) => {
            e.preventDefault()
            addItem.mutate({ productId: product.id, quantity: 1 })
          }}
          disabled={product.totalStock === 0 || addItem.isPending}
          className="absolute bottom-3 left-3 right-3 bg-ink text-ash py-2.5 text-xs font-display font-semibold uppercase tracking-wide opacity-0 translate-y-2 group-hover:opacity-100 group-hover:translate-y-0 transition-all duration-200 disabled:opacity-40"
        >
          Quick Add
        </button>
      </Link>

      {/* Info */}
      <div className="p-4 flex flex-col gap-2 flex-1">
        <p className="text-xs font-mono text-ink/35 uppercase tracking-wider">{product.category?.name ?? ''}</p>
        <Link to={`/products/${product.slug}`}>
          <h3 className="font-body font-medium text-sm text-ink line-clamp-2 leading-snug hover:text-ember transition-colors">
            {product.name}
          </h3>
        </Link>
        <Rating value={product.ratingAvg} count={product.ratingCount} />
        <div className="flex items-baseline gap-2 mt-auto pt-1">
          <span className="font-display font-bold text-base text-ink">
            {formatPrice(product.basePrice)}
          </span>
          {product.originalPrice && product.originalPrice > product.basePrice && (
            <span className="text-xs font-body text-ink/35 line-through">
              {formatPrice(product.originalPrice)}
            </span>
          )}
        </div>
      </div>
    </div>
  )
}
