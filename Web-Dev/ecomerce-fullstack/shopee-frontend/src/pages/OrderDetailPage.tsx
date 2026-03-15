import { useParams, Link } from 'react-router-dom'
import { useQuery, useMutation } from '@tanstack/react-query'
import { Package, MapPin, CheckCircle2, Circle, Truck, ChevronLeft } from 'lucide-react'
import { ordersApi } from '@/api/orders'
import { OrderStatusBadge } from '@/components/order/OrderStatusBadge'
import { Button } from '@/components/ui/Button'
import { Skeleton } from '@/components/ui/Skeleton'
import { formatPrice, formatDate, formatRelativeDate } from '@/utils'
import toast from 'react-hot-toast'

export function OrderDetailPage() {
  const { id } = useParams<{ id: string }>()

  const { data: order, isLoading, refetch } = useQuery({
    queryKey: ['order', id],
    queryFn: () => ordersApi.getById(id!).then(r => r.data.data),
    enabled: !!id,
  })

  const cancelOrder = useMutation({
    mutationFn: (reason: string) => ordersApi.cancel(id!, reason),
    onSuccess: () => { toast.success('Order cancelled'); refetch() },
    onError: () => toast.error('Could not cancel order'),
  })

  if (isLoading) return (
    <div className="max-w-4xl mx-auto px-4 py-8 space-y-6">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-32 w-full" />
      <Skeleton className="h-48 w-full" />
    </div>
  )

  if (!order) return (
    <div className="max-w-4xl mx-auto px-4 py-20 text-center">
      <p className="font-display text-2xl">Order not found</p>
      <Link to="/orders" className="text-ember hover:underline mt-4 block">Back to Orders</Link>
    </div>
  )

  const canCancel = order.status === 'PENDING' || order.status === 'AWAITING_PAYMENT'

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-8">
      {/* Header */}
      <div className="flex items-start justify-between mb-8 gap-4 flex-wrap">
        <div>
          <Link to="/orders" className="inline-flex items-center gap-1 text-sm font-body text-ink/50 hover:text-ink mb-3 transition-colors">
            <ChevronLeft size={14} /> All Orders
          </Link>
          <h1 className="font-display font-bold text-2xl">{order.orderNumber}</h1>
          <p className="text-sm font-mono text-ink/40 mt-0.5">{formatDate(order.createdAt)}</p>
        </div>
        <div className="flex items-center gap-3">
          <OrderStatusBadge status={order.status} />
          {canCancel && (
            <Button
              variant="danger"
              size="sm"
              onClick={() => cancelOrder.mutate('Buyer requested cancellation')}
              loading={cancelOrder.isPending}
            >
              Cancel Order
            </Button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-5">

          {/* Order Timeline */}
          {order.timeline && (
            <div className="bg-white border border-ash-dark p-6">
              <h2 className="font-display font-semibold text-base mb-5">Order Status</h2>
              <div className="space-y-0">
                {order.timeline.map((step, i) => (
                  <div key={step.status} className="flex gap-4">
                    <div className="flex flex-col items-center">
                      <div className={`w-8 h-8 rounded-full border-2 flex items-center justify-center flex-shrink-0 ${
                        step.completed ? 'bg-ink border-ink text-ash' :
                        step.current ? 'bg-ember border-ember text-white' :
                        'border-ink/15 text-ink/20'
                      }`}>
                        {step.completed ? <CheckCircle2 size={14} /> : <Circle size={14} />}
                      </div>
                      {i < order.timeline.length - 1 && (
                        <div className={`w-0.5 h-8 mt-1 ${step.completed ? 'bg-ink' : 'bg-ink/10'}`} />
                      )}
                    </div>
                    <div className="pb-6">
                      <p className={`font-display text-sm font-semibold ${step.current ? 'text-ember' : step.completed ? 'text-ink' : 'text-ink/30'}`}>
                        {step.label}
                      </p>
                      {step.completedAt && (
                        <p className="text-xs font-mono text-ink/40 mt-0.5">{formatRelativeDate(step.completedAt)}</p>
                      )}
                    </div>
                  </div>
                ))}
              </div>

              {order.trackingNumber && (
                <div className="mt-4 p-3 bg-ash-dark flex items-center gap-2">
                  <Truck size={14} className="text-sage" />
                  <span className="text-xs font-mono text-ink/60">Tracking: {order.trackingNumber}</span>
                  {order.shippingCarrier && <span className="text-xs font-mono text-ink/40">· {order.shippingCarrier}</span>}
                </div>
              )}
            </div>
          )}

          {/* Items */}
          <div className="bg-white border border-ash-dark p-6">
            <h2 className="font-display font-semibold text-base mb-5">Items ({order.items.length})</h2>
            <div className="space-y-4">
              {order.items.map(item => (
                <div key={item.id} className="flex gap-4 pb-4 border-b border-ink/5 last:border-0 last:pb-0">
                  <div className="w-16 h-16 bg-ash-dark flex-shrink-0 overflow-hidden">
                    {item.productImage && <img src={item.productImage} alt="" className="w-full h-full object-cover" />}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-body text-sm font-medium text-ink">{item.displayName}</p>
                    {item.variantAttributes && Object.keys(item.variantAttributes).length > 0 && (
                      <p className="text-xs text-ink/40 font-mono mt-0.5">
                        {Object.entries(item.variantAttributes).map(([k,v]) => `${k}: ${v}`).join(' · ')}
                      </p>
                    )}
                    <div className="flex items-center justify-between mt-2">
                      <p className="text-xs font-mono text-ink/40">Qty: {item.quantity} × {formatPrice(item.unitPrice)}</p>
                      <p className="font-display font-semibold text-sm">{formatPrice(item.lineTotal)}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right column */}
        <div className="space-y-5">
          {/* Payment summary */}
          <div className="bg-white border border-ash-dark p-5">
            <h2 className="font-display font-semibold text-base mb-4">Payment</h2>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-ink/60 font-body">Subtotal</span>
                <span className="font-body">{formatPrice(order.subtotal)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-ink/60 font-body">Shipping</span>
                <span className="font-body">{order.shippingFee === 0 ? 'FREE' : formatPrice(order.shippingFee)}</span>
              </div>
              {order.discountAmount > 0 && (
                <div className="flex justify-between">
                  <span className="text-ink/60 font-body">Discount</span>
                  <span className="text-sage font-body">-{formatPrice(order.discountAmount)}</span>
                </div>
              )}
              <div className="flex justify-between font-display font-bold pt-2 border-t border-ink/8">
                <span>Total</span>
                <span className="text-lg">{formatPrice(order.totalAmount)}</span>
              </div>
            </div>
          </div>

          {/* Shipping address */}
          <div className="bg-white border border-ash-dark p-5">
            <h2 className="font-display font-semibold text-base mb-3 flex items-center gap-2">
              <MapPin size={14} /> Shipping Address
            </h2>
            <address className="not-italic text-sm font-body text-ink/70 space-y-0.5">
              <p className="font-medium text-ink">{order.shippingAddress.recipientName}</p>
              <p>{order.shippingAddress.addressLine1}</p>
              {order.shippingAddress.addressLine2 && <p>{order.shippingAddress.addressLine2}</p>}
              <p>{order.shippingAddress.city}, {order.shippingAddress.prefecture} {order.shippingAddress.postalCode}</p>
              <p>{order.shippingAddress.phone}</p>
            </address>
          </div>
        </div>
      </div>
    </div>
  )
}
