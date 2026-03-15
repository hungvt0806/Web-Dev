import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ShoppingBag, Heart, ChevronRight, Plus, Minus, Truck, Shield } from 'lucide-react'
import { productsApi } from '@/api/products'
import { useCart } from '@/hooks/useCart'
import { useCartStore } from '@/store/cartStore'
import { formatPrice, getDiscountPercent } from '@/utils'
import { Rating } from '@/components/ui/Rating'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Skeleton } from '@/components/ui/Skeleton'
import type { ProductVariant } from '@/types'

export function ProductDetailPage() {
  const { slug } = useParams<{ slug: string }>()
  const [selectedVariant, setSelectedVariant] = useState<ProductVariant | null>(null)
  const [quantity, setQuantity] = useState(1)
  const [activeImage, setActiveImage] = useState(0)
  const { addItem } = useCart()
  const { openCart } = useCartStore()

  const { data: product, isLoading } = useQuery({
    queryKey: ['product', slug],
    queryFn: () => productsApi.getBySlug(slug!).then(r => r.data.data),
    enabled: !!slug,
  })

  if (isLoading) return <ProductDetailSkeleton />
  if (!product) return (
    <div className="max-w-7xl mx-auto px-4 py-20 text-center">
      <p className="font-display text-2xl">Product not found</p>
      <Link to="/products" className="text-ember hover:underline mt-4 block">Browse Products</Link>
    </div>
  )

  const currentPrice = selectedVariant?.price ?? product.basePrice
  const images = product.imageUrls.length > 0 ? product.imageUrls : (product.thumbnailUrl ? [product.thumbnailUrl] : [])
  const maxQty = selectedVariant?.stock ?? product.totalStock
  const discount = product.originalPrice ? getDiscountPercent(product.originalPrice, currentPrice) : 0

  // Group variants by attribute key
  const variantGroups = product.variants.reduce<Record<string, string[]>>((acc, v) => {
    Object.entries(v.attributes).forEach(([key, val]) => {
      if (!acc[key]) acc[key] = []
      if (!acc[key].includes(val)) acc[key].push(val)
    })
    return acc
  }, {})

  const handleAddToCart = async () => {
    await addItem.mutateAsync({
      productId: product.id,
      variantId: selectedVariant?.id,
      quantity,
    })
    openCart()
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 py-8">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-1.5 text-xs font-mono text-ink/40 mb-8">
        <Link to="/" className="hover:text-ink">Home</Link>
        <ChevronRight size={11} />
        <Link to="/products" className="hover:text-ink">Products</Link>
        <ChevronRight size={11} />
        {product.category && (
      <Link to={`/products?categoryId=${product.category.id}`} className="hover:text-ink">
        {product.category.name}
      </Link>
    )}
        <ChevronRight size={11} />
        <span className="text-ink truncate max-w-40">{product.name}</span>
      </nav>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 xl:gap-20">
        {/* ── Images ── */}
        <div className="flex gap-3">
          {/* Thumbnails */}
          {images.length > 1 && (
            <div className="flex flex-col gap-2 w-16">
              {images.map((img, i) => (
                <button
                  key={i}
                  onClick={() => setActiveImage(i)}
                  className={`w-16 h-16 overflow-hidden border-2 transition-all ${
                    activeImage === i ? 'border-ink' : 'border-transparent opacity-50 hover:opacity-80'
                  }`}
                >
                  <img src={img} alt="" className="w-full h-full object-cover" />
                </button>
              ))}
            </div>
          )}

          {/* Main image */}
          <div className="flex-1 aspect-square bg-ash-dark overflow-hidden relative">
            {images.length > 0 ? (
              <img
                src={images[activeImage]}
                alt={product.name}
                className="w-full h-full object-cover"
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center text-ink/15">
                <ShoppingBag size={60} />
              </div>
            )}
            {discount > 0 && (
              <div className="absolute top-4 left-4">
                <Badge variant="ember">-{discount}%</Badge>
              </div>
            )}
          </div>
        </div>

        {/* ── Product Info ── */}
        <div className="flex flex-col gap-5">
          <div>
            <p className="text-xs font-mono text-ink/40 uppercase tracking-widest mb-2">{product.category?.name ?? ''}</p>
            <h1 className="font-display font-bold text-3xl md:text-4xl text-ink leading-tight">{product.name}</h1>
          </div>

          <div className="flex items-center gap-4">
            <Rating value={product.ratingAvg} count={product.ratingCount} size="md" />
            <span className="font-mono text-xs text-ink/35">{product.soldCount.toLocaleString()} sold</span>
          </div>

          {/* Price */}
          <div className="flex items-baseline gap-3">
            <span className="font-display font-black text-4xl text-ink">{formatPrice(currentPrice)}</span>
            {product.originalPrice && product.originalPrice > currentPrice && (
              <>
                <span className="font-body text-base text-ink/35 line-through">{formatPrice(product.originalPrice)}</span>
                <Badge variant="ember">Save {formatPrice(product.originalPrice - currentPrice)}</Badge>
              </>
            )}
          </div>

          {/* Variants */}
          {Object.entries(variantGroups).map(([key, values]) => (
            <div key={key}>
              <p className="section-label capitalize">{key}</p>
              <div className="flex flex-wrap gap-2">
                {values.map(val => {
                  const variant = product.variants.find(v => v.attributes[key] === val)
                  const isSelected = selectedVariant?.attributes[key] === val
                  const isOutOfStock = variant ? variant.stock === 0 : false
                  return (
                    <button
                      key={val}
                      onClick={() => variant && setSelectedVariant(variant)}
                      disabled={isOutOfStock}
                      className={`px-4 py-2 text-sm font-body border-2 transition-all
                        ${isSelected ? 'bg-ink text-ash border-ink' : 'border-ink/20 hover:border-ink text-ink'}
                        ${isOutOfStock ? 'opacity-30 line-through cursor-not-allowed' : ''}`}
                    >
                      {val}
                    </button>
                  )
                })}
              </div>
            </div>
          ))}

          {/* Quantity */}
          <div>
            <p className="section-label">Quantity</p>
            <div className="flex items-center gap-4">
              <div className="flex items-center border-2 border-ink/20">
                <button
                  onClick={() => setQuantity(q => Math.max(1, q - 1))}
                  className="w-11 h-11 flex items-center justify-center hover:bg-ink/5 transition-colors"
                >
                  <Minus size={14} />
                </button>
                <span className="w-12 text-center font-mono font-bold">{quantity}</span>
                <button
                  onClick={() => setQuantity(q => Math.min(maxQty, q + 1))}
                  disabled={quantity >= maxQty}
                  className="w-11 h-11 flex items-center justify-center hover:bg-ink/5 transition-colors disabled:opacity-30"
                >
                  <Plus size={14} />
                </button>
              </div>
              <span className="text-xs font-mono text-ink/40">
                {maxQty > 0 ? `${maxQty} available` : 'Out of stock'}
              </span>
            </div>
          </div>

          {/* CTA Buttons */}
          <div className="flex gap-3 pt-2">
            <Button
              size="lg"
              fullWidth
              onClick={handleAddToCart}
              loading={addItem.isPending}
              disabled={maxQty === 0}
            >
              <ShoppingBag size={18} />
              {maxQty === 0 ? 'Out of Stock' : 'Add to Cart'}
            </Button>
            <button className="w-14 h-14 border-2 border-ink/20 flex items-center justify-center hover:border-ember hover:text-ember transition-all flex-shrink-0">
              <Heart size={18} />
            </button>
          </div>

          {/* Trust badges */}
          <div className="flex gap-5 pt-2 border-t border-ink/8">
            <div className="flex items-center gap-2 text-xs font-body text-ink/50">
              <Truck size={14} className="text-sage" />
              Free shipping over ¥3,000
            </div>
            <div className="flex items-center gap-2 text-xs font-body text-ink/50">
              <Shield size={14} className="text-sage" />
              30-day returns
            </div>
          </div>

          {/* Description */}
          {product.shortDescription && (
            <div className="pt-2 border-t border-ink/8">
              <p className="section-label">About</p>
              <p className="font-body text-sm text-ink/70 leading-relaxed">{product.shortDescription}</p>
            </div>
          )}

          {/* Tags */}
          {product.tags.length > 0 && (
            <div className="flex flex-wrap gap-2 pt-2">
              {product.tags.map(tag => (
                <Link
                  key={tag}
                  to={`/products?q=${tag}`}
                  className="text-xs font-mono text-ink/40 border border-ink/15 px-2 py-1 hover:border-ink hover:text-ink transition-colors"
                >
                  #{tag}
                </Link>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Description */}
      {product.description && (
        <div className="mt-16 pt-8 border-t border-ink/8">
          <h2 className="font-display font-bold text-2xl mb-5">Product Details</h2>
          <div className="font-body text-sm text-ink/70 leading-relaxed max-w-2xl whitespace-pre-line">
            {product.description}
          </div>
        </div>
      )}
    </div>
  )
}

function ProductDetailSkeleton() {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 py-8">
      <div className="flex gap-2 mb-8">
        <Skeleton className="h-4 w-48" />
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
        <Skeleton className="aspect-square w-full" />
        <div className="space-y-4">
          <Skeleton className="h-10 w-3/4" />
          <Skeleton className="h-5 w-1/3" />
          <Skeleton className="h-12 w-1/2" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-14 w-full" />
        </div>
      </div>
    </div>
  )
}
