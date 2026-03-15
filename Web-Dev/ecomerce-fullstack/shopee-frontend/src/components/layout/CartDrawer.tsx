import { X, ShoppingBag, Trash2, Plus, Minus } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useCartStore } from '@/store/cartStore'
import { useCart } from '@/hooks/useCart'
import { formatPrice } from '@/utils'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'

export function CartDrawer() {
  const { isOpen, closeCart, cart } = useCartStore()
  const { updateItem, removeItem } = useCart()

  return (
    <>
      {/* Backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 bg-ink/40 backdrop-blur-sm z-40 transition-opacity"
          onClick={closeCart}
        />
      )}

      {/* Drawer */}
      <div className={`
        fixed right-0 top-0 bottom-0 w-full max-w-md bg-ash z-50 flex flex-col
        transform transition-transform duration-300 ease-in-out border-l-2 border-ink/10
        ${isOpen ? 'translate-x-0' : 'translate-x-full'}
      `}>
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-5 border-b-2 border-ink/8">
          <div className="flex items-center gap-2">
            <ShoppingBag size={18} />
            <h2 className="font-display font-bold text-lg">
              Cart
              {cart && cart.itemCount > 0 && (
                <span className="ml-2 text-sm font-mono text-ink/40 font-normal">({cart.itemCount})</span>
              )}
            </h2>
          </div>
          <button onClick={closeCart} className="p-1.5 hover:bg-ink/8 transition-colors">
            <X size={18} />
          </button>
        </div>

        {/* Items */}
        <div className="flex-1 overflow-y-auto px-6 py-4">
          {!cart || cart.items.length === 0 ? (
            <EmptyState
              icon={<ShoppingBag size={28} />}
              title="Your cart is empty"
              description="Add some products to get started"
              action={{ label: 'Browse Products', href: '/products' }}
            />
          ) : (
            <div className="space-y-4">
              {cart.items.map(item => (
                <div key={item.id} className="flex gap-4 p-3 bg-white border border-ash-dark">
                  {/* Image */}
                  <div className="w-16 h-16 bg-ash-dark flex-shrink-0">
                    {item.productImage ? (
                      <img src={item.productImage} alt={item.productName} className="w-full h-full object-cover" />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-ink/20">
                        <ShoppingBag size={20} />
                      </div>
                    )}
                  </div>

                  {/* Details */}
                  <div className="flex-1 min-w-0">
                    <p className="font-body text-sm font-medium text-ink line-clamp-1">{item.productName}</p>
                    {item.variantAttributes && Object.keys(item.variantAttributes).length > 0 && (
                      <p className="text-xs text-ink/40 font-mono mt-0.5">
                        {Object.entries(item.variantAttributes).map(([k,v]) => `${k}: ${v}`).join(' · ')}
                      </p>
                    )}
                    {item.priceStale && (
                      <p className="text-xs text-ember mt-0.5">Price updated</p>
                    )}

                    <div className="flex items-center justify-between mt-2">
                      {/* Qty controls */}
                      <div className="flex items-center border-2 border-ink/15">
                        <button
                          onClick={() => updateItem.mutate({ cartItemId: item.id, quantity: item.quantity - 1 })}
                          className="w-7 h-7 flex items-center justify-center hover:bg-ink/5 transition-colors"
                        >
                          <Minus size={12} />
                        </button>
                        <span className="w-8 text-center text-sm font-mono">{item.quantity}</span>
                        <button
                          onClick={() => updateItem.mutate({ cartItemId: item.id, quantity: item.quantity + 1 })}
                          disabled={item.quantity >= item.availableStock}
                          className="w-7 h-7 flex items-center justify-center hover:bg-ink/5 transition-colors disabled:opacity-30"
                        >
                          <Plus size={12} />
                        </button>
                      </div>

                      <div className="flex items-center gap-3">
                        <span className="font-display font-semibold text-sm">{formatPrice(item.lineTotal)}</span>
                        <button
                          onClick={() => removeItem.mutate(item.id)}
                          className="text-ink/25 hover:text-ember transition-colors"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Footer */}
        {cart && cart.items.length > 0 && (
          <div className="border-t-2 border-ink/8 px-6 py-5 space-y-4 bg-white">
            <div className="flex justify-between items-center">
              <span className="font-body text-sm text-ink/60">Subtotal</span>
              <span className="font-display font-bold text-lg">{formatPrice(cart.subtotal)}</span>
            </div>
            <p className="text-xs text-ink/40 font-body">Shipping calculated at checkout</p>
            <Link to="/checkout" onClick={closeCart}>
              <Button fullWidth size="lg">Proceed to Checkout</Button>
            </Link>
            <Link to="/cart" onClick={closeCart}>
              <Button fullWidth variant="secondary" size="md" className="mt-2">View Full Cart</Button>
            </Link>
          </div>
        )}
      </div>
    </>
  )
}
