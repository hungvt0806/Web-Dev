import { Link } from 'react-router-dom'
import { Trash2, Plus, Minus, ShoppingBag, ArrowRight, RefreshCw } from 'lucide-react'
import { useCart } from '@/hooks/useCart'
import { formatPrice } from '@/utils'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'

export function CartPage() {
  const { cart, isLoading, updateItem, removeItem, clearCart } = useCart()

  if (isLoading) return <div className="max-w-7xl mx-auto px-4 py-20 text-center font-mono text-sm text-ink/40">Loading cart…</div>

  if (!cart || cart.items.length === 0) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-20">
        <EmptyState
          icon={<ShoppingBag size={32} />}
          title="Your cart is empty"
          description="Browse our collection and add some items"
          action={{ label: 'Start Shopping', href: '/products' }}
        />
      </div>
    )
  }

  const FREE_SHIPPING_THRESHOLD = 3000
  const shippingFee = cart.subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : 500
  const remaining = FREE_SHIPPING_THRESHOLD - cart.subtotal

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 py-8">
      <div className="mb-8">
        <p className="section-label">Review</p>
        <h1 className="page-title">Your Cart</h1>
        <p className="font-mono text-sm text-ink/40 mt-1">{cart.totalQuantity} item{cart.totalQuantity !== 1 ? 's' : ''}</p>
      </div>

      {/* Free shipping progress */}
      {remaining > 0 && (
        <div className="bg-sage/10 border border-sage/25 px-5 py-3.5 mb-6">
          <div className="flex justify-between text-sm mb-2">
            <span className="font-body text-sage-dark">Add {formatPrice(remaining)} more for free shipping</span>
            <span className="font-mono text-sage">{Math.round((cart.subtotal / FREE_SHIPPING_THRESHOLD) * 100)}%</span>
          </div>
          <div className="h-1.5 bg-sage/20 rounded-full">
            <div
              className="h-full bg-sage rounded-full transition-all duration-500"
              style={{ width: `${Math.min(100, (cart.subtotal / FREE_SHIPPING_THRESHOLD) * 100)}%` }}
            />
          </div>
        </div>
      )}

      {cart.hasStalePrices && (
        <div className="bg-ember/8 border border-ember/20 px-5 py-3 mb-6 flex items-center gap-3">
          <RefreshCw size={15} className="text-ember flex-shrink-0" />
          <p className="text-sm font-body text-ember">Some prices have changed since you last updated your cart.</p>
          <button className="ml-auto text-xs font-display font-semibold text-ember underline">Sync Prices</button>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">

        {/* Items */}
        <div className="lg:col-span-2 space-y-3">
          {cart.items.map(item => (
            <div key={item.id} className="bg-white border border-ash-dark p-4 flex gap-4">
              {/* Image */}
              <Link to={`/products/${item.productId}`} className="w-24 h-24 bg-ash-dark flex-shrink-0 overflow-hidden">
                {item.productImage ? (
                  <img src={item.productImage} alt={item.productName} className="w-full h-full object-cover" />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-ink/15">
                    <ShoppingBag size={24} />
                  </div>
                )}
              </Link>

              {/* Details */}
              <div className="flex-1 min-w-0">
                <div className="flex justify-between gap-2">
                  <Link to={`/products/${item.productId}`}>
                    <h3 className="font-body font-medium text-sm text-ink hover:text-ember transition-colors line-clamp-2">
                      {item.productName}
                    </h3>
                  </Link>
                  <button
                    onClick={() => removeItem.mutate(item.id)}
                    className="text-ink/25 hover:text-ember transition-colors flex-shrink-0"
                  >
                    <Trash2 size={15} />
                  </button>
                </div>

                {item.variantAttributes && Object.keys(item.variantAttributes).length > 0 && (
                  <p className="text-xs text-ink/40 font-mono mt-1">
                    {Object.entries(item.variantAttributes).map(([k,v]) => `${k}: ${v}`).join(' · ')}
                  </p>
                )}

                <div className="flex items-center justify-between mt-3">
                  {/* Qty */}
                  <div className="flex items-center border-2 border-ink/15">
                    <button
                      onClick={() => updateItem.mutate({ cartItemId: item.id, quantity: item.quantity - 1 })}
                      className="w-8 h-8 flex items-center justify-center hover:bg-ink/5 transition-colors"
                    >
                      <Minus size={12} />
                    </button>
                    <span className="w-9 text-center font-mono text-sm">{item.quantity}</span>
                    <button
                      onClick={() => updateItem.mutate({ cartItemId: item.id, quantity: item.quantity + 1 })}
                      disabled={item.quantity >= item.availableStock}
                      className="w-8 h-8 flex items-center justify-center hover:bg-ink/5 transition-colors disabled:opacity-30"
                    >
                      <Plus size={12} />
                    </button>
                  </div>
                  <span className="font-display font-bold text-base">{formatPrice(item.lineTotal)}</span>
                </div>
              </div>
            </div>
          ))}

          <button
            onClick={() => clearCart.mutate()}
            className="text-xs font-body text-ink/40 hover:text-ember transition-colors flex items-center gap-1 mt-2"
          >
            <Trash2 size={12} /> Clear cart
          </button>
        </div>

        {/* Order Summary */}
        <div>
          <div className="bg-white border border-ash-dark p-6 sticky top-24">
            <h2 className="font-display font-bold text-lg mb-5">Order Summary</h2>

            <div className="space-y-3 mb-5">
              <div className="flex justify-between text-sm font-body">
                <span className="text-ink/60">Subtotal ({cart.totalQuantity} items)</span>
                <span className="font-medium">{formatPrice(cart.subtotal)}</span>
              </div>
              <div className="flex justify-between text-sm font-body">
                <span className="text-ink/60">Shipping</span>
                <span className={`font-medium ${shippingFee === 0 ? 'text-sage' : ''}`}>
                  {shippingFee === 0 ? 'FREE' : formatPrice(shippingFee)}
                </span>
              </div>
              <div className="border-t border-ink/8 pt-3 flex justify-between">
                <span className="font-display font-semibold">Total</span>
                <span className="font-display font-black text-xl">{formatPrice(cart.subtotal + shippingFee)}</span>
              </div>
            </div>

            {/* Coupon */}
            <div className="flex gap-2 mb-5">
              <input
                type="text"
                placeholder="Coupon code"
                className="flex-1 border-2 border-ink/15 px-3 py-2 text-sm font-mono focus:border-ember focus:outline-none"
              />
              <button className="px-4 py-2 border-2 border-ink text-sm font-display font-semibold hover:bg-ink hover:text-ash transition-all">
                Apply
              </button>
            </div>

            <Link to="/checkout">
              <Button fullWidth size="lg">
                Checkout <ArrowRight size={16} />
              </Button>
            </Link>

            <div className="mt-4 text-center">
              <Link to="/products" className="text-xs font-body text-ink/40 hover:text-ink transition-colors">
                ← Continue Shopping
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
