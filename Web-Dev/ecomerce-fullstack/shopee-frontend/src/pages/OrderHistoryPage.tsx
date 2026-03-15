import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Package } from 'lucide-react'
import { ordersApi } from '@/api/orders'
import { OrderStatusBadge } from '@/components/order/OrderStatusBadge'
import { EmptyState } from '@/components/ui/EmptyState'
import { Skeleton } from '@/components/ui/Skeleton'
import { formatPrice, formatDate } from '@/utils'
import type { OrderStatus } from '@/types'

const STATUS_FILTERS: { value: OrderStatus | ''; label: string }[] = [
  { value: '', label: 'All Orders' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'AWAITING_PAYMENT', label: 'Awaiting Payment' },
  { value: 'PAID', label: 'Paid' },
  { value: 'SHIPPED', label: 'Shipped' },
  { value: 'DELIVERED', label: 'Delivered' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

export function OrderHistoryPage() {
  const [statusFilter, setStatusFilter] = useState<OrderStatus | ''>('')

  const { data, isLoading } = useQuery({
    queryKey: ['orders', statusFilter],
    queryFn: () => ordersApi.list({ status: statusFilter || undefined, size: 20 }).then(r => r.data.data),
  })

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-8">
      <div className="mb-8">
        <p className="section-label">Account</p>
        <h1 className="page-title">My Orders</h1>
      </div>

      {/* Status filter tabs */}
      <div className="flex gap-1 flex-wrap mb-6 border-b border-ink/10 pb-1">
        {STATUS_FILTERS.map(f => (
          <button
            key={f.value}
            onClick={() => setStatusFilter(f.value)}
            className={`px-4 py-2 text-sm font-body transition-all border-b-2 -mb-[3px] ${
              statusFilter === f.value
                ? 'text-ember border-ember font-medium'
                : 'text-ink/50 border-transparent hover:text-ink'
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-28 w-full" />)}
        </div>
      ) : !data || data.length === 0 ? (
        <EmptyState
          icon={<Package size={28} />}
          title="No orders yet"
          description={statusFilter ? `No ${statusFilter.toLowerCase()} orders` : 'Your order history will appear here'}
          action={{ label: 'Start Shopping', href: '/products' }}
        />
      ) : (
        <div className="space-y-3">
          {data.map(order => (
            <Link
              key={order.id}
              to={`/orders/${order.id}`}
              className="block bg-white border border-ash-dark p-5 hover:shadow-lift transition-all group"
            >
              <div className="flex items-start justify-between gap-4 mb-4">
                <div>
                  <p className="font-mono text-xs text-ink/40 mb-1">{formatDate(order.createdAt)}</p>
                  <p className="font-display font-semibold text-sm text-ink group-hover:text-ember transition-colors">
                    {order.orderNumber}
                  </p>
                </div>
                <OrderStatusBadge status={order.status} />
              </div>

              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3 min-w-0">
                  {order.firstItemImage && (
                    <div className="w-12 h-12 bg-ash-dark flex-shrink-0 overflow-hidden">
                      <img src={order.firstItemImage} alt="" className="w-full h-full object-cover" />
                    </div>
                  )}
                  <div className="min-w-0">
                    <p className="font-body text-sm text-ink truncate">{order.firstItemName}</p>
                    {order.additionalItemCount > 0 && (
                      <p className="text-xs font-mono text-ink/40">+{order.additionalItemCount} more item{order.additionalItemCount !== 1 ? 's' : ''}</p>
                    )}
                  </div>
                </div>
                <div className="text-right flex-shrink-0 ml-4">
                  <p className="font-display font-bold text-base">{formatPrice(order.totalAmount)}</p>
                  <p className="text-xs font-mono text-ink/40">{order.itemCount} item{order.itemCount !== 1 ? 's' : ''}</p>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
