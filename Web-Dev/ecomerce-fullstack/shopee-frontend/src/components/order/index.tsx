import React from 'react'
import { Link } from 'react-router-dom'
import { Package, CheckCircle2, Circle, Truck, MapPin, Clock } from 'lucide-react'
import type { Order, OrderSummary, OrderTimelineEntry, OrderStatus } from '@/types'
import { getOrderStatusMeta, formatDateTime, formatDate, formatPrice } from '@/utils'
import { cn } from '@/utils'
import { Badge } from '@/components/ui'

// ── OrderStatusBadge ──────────────────────────────────────────────────────

export const OrderStatusBadge = ({ status }: { status: OrderStatus }) => {
  const meta = getOrderStatusMeta(status)
  return <span className={cn('badge', meta.badge)}>{meta.label}</span>
}

// ── OrderCard (summary) ───────────────────────────────────────────────────

export const OrderCard = ({ order }: { order: OrderSummary }) => (
  <Link to={`/orders/${order.id}`}
    className="card p-5 block hover:shadow-card-lg transition-shadow group">
    <div className="flex items-start justify-between gap-4 mb-4">
      <div>
        <p className="font-mono text-xs text-ink-muted mb-1">{order.orderNumber}</p>
        <OrderStatusBadge status={order.status} />
      </div>
      <div className="text-right">
        <p className="font-display text-lg font-semibold">{formatPrice(order.totalAmount)}</p>
        <p className="text-xs text-ink-muted">{order.totalQuantity} item{order.totalQuantity !== 1 ? 's' : ''}</p>
      </div>
    </div>

    {/* First item preview */}
    <div className="flex items-center gap-3 py-3 border-t border-border">
      {order.firstItemImage
        ? <img src={order.firstItemImage} alt={order.firstItemName} className="w-12 h-12 rounded-xl object-cover flex-shrink-0 bg-cream-warm" />
        : <div className="w-12 h-12 rounded-xl bg-cream-warm flex items-center justify-center flex-shrink-0">
            <Package className="h-5 w-5 text-ink-muted/40" />
          </div>
      }
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-ink line-clamp-1">{order.firstItemName}</p>
        {order.additionalItemCount > 0 && (
          <p className="text-xs text-ink-muted">+{order.additionalItemCount} more item{order.additionalItemCount !== 1 ? 's' : ''}</p>
        )}
      </div>
      <span className="text-ink-muted group-hover:text-ink transition-colors text-sm">→</span>
    </div>

    <p className="text-xs text-ink-muted mt-2">{formatDate(order.createdAt)}</p>
  </Link>
)

// ── OrderTimeline ─────────────────────────────────────────────────────────

export const OrderTimeline = ({ timeline }: { timeline: OrderTimelineEntry[] }) => (
  <div className="space-y-0">
    {timeline.map((entry, i) => (
      <div key={entry.status} className="flex gap-4">
        {/* Icon column */}
        <div className="flex flex-col items-center">
          <div className={cn(
            'w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 z-10',
            entry.completed ? 'bg-success text-white' :
            entry.current   ? 'bg-accent text-white ring-4 ring-accent-light' :
                              'bg-cream-warm border-2 border-border text-ink-muted'
          )}>
            {entry.completed
              ? <CheckCircle2 className="h-4 w-4" />
              : entry.current
              ? <Circle className="h-3 w-3 fill-current animate-pulse-dot" />
              : <Circle className="h-3 w-3" />
            }
          </div>
          {i < timeline.length - 1 && (
            <div className={cn('w-0.5 h-10 mt-1', entry.completed ? 'bg-success' : 'bg-border')} />
          )}
        </div>

        {/* Content */}
        <div className="pb-8 flex-1 min-w-0">
          <p className={cn(
            'text-sm font-medium',
            entry.current ? 'text-ink' : entry.completed ? 'text-ink' : 'text-ink-muted'
          )}>{entry.label}</p>
          <p className="text-xs text-ink-muted mt-0.5">{entry.description}</p>
          {entry.completedAt && (
            <p className="text-xs text-ink-muted/60 mt-1 font-mono">{formatDateTime(entry.completedAt)}</p>
          )}
        </div>
      </div>
    ))}
  </div>
)

// ── Order detail shipping info ────────────────────────────────────────────

export const ShippingInfo = ({ order }: { order: Order }) => (
  <div className="space-y-4">
    <div className="flex items-start gap-3">
      <MapPin className="h-4 w-4 text-ink-muted mt-0.5 flex-shrink-0" />
      <div>
        <p className="text-sm font-medium">{order.shippingAddress.fullName}</p>
        <p className="text-sm text-ink-muted">{order.shippingAddress.phone}</p>
        <p className="text-sm text-ink-muted mt-1">
          {order.shippingAddress.addressLine1}
          {order.shippingAddress.addressLine2 && `, ${order.shippingAddress.addressLine2}`}<br />
          {order.shippingAddress.city}, {order.shippingAddress.postalCode}<br />
          {order.shippingAddress.country}
        </p>
      </div>
    </div>
    {order.trackingNumber && (
      <div className="flex items-start gap-3">
        <Truck className="h-4 w-4 text-ink-muted mt-0.5 flex-shrink-0" />
        <div>
          <p className="text-xs text-ink-muted">Tracking</p>
          <p className="text-sm font-mono font-medium">{order.trackingNumber}</p>
          {order.shippingCarrier && <p className="text-xs text-ink-muted">{order.shippingCarrier}</p>}
        </div>
      </div>
    )}
    {order.estimatedDeliveryDate && (
      <div className="flex items-start gap-3">
        <Clock className="h-4 w-4 text-ink-muted mt-0.5 flex-shrink-0" />
        <div>
          <p className="text-xs text-ink-muted">Estimated delivery</p>
          <p className="text-sm font-medium">{formatDate(order.estimatedDeliveryDate)}</p>
        </div>
      </div>
    )}
  </div>
)
