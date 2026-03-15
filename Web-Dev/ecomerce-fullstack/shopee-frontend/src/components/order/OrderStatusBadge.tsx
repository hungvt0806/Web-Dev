import type { OrderStatus } from '@/types'
import { ORDER_STATUS_COLORS, ORDER_STATUS_LABELS } from '@/utils'
import { cn } from '@/utils'

export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  return (
    <span className={cn(
      'inline-flex items-center px-2.5 py-1 text-xs font-mono font-semibold uppercase tracking-wide border',
      ORDER_STATUS_COLORS[status]
    )}>
      {ORDER_STATUS_LABELS[status]}
    </span>
  )
}
