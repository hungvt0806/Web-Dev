import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Search, ChevronDown } from 'lucide-react'
import { ordersApi } from '@/api/orders'
import { OrderStatusBadge } from '@/components/order/OrderStatusBadge'
import { Button } from '@/components/ui/Button'
import { Modal } from '@/components/ui/Modal'
import { Skeleton } from '@/components/ui/Skeleton'
import { formatPrice, formatDate } from '@/utils'
import type { OrderStatus } from '@/types'
import toast from 'react-hot-toast'

const STATUS_TRANSITIONS: Record<string, string[]> = {
  PAID:       ['PROCESSING'],
  PROCESSING: ['SHIPPED', 'CANCELLED'],
  SHIPPED:    ['DELIVERED'],
  DELIVERED:  ['REFUNDED'],
}

export function AdminOrdersPage() {
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [selectedOrder, setSelectedOrder] = useState<{ id: string; status: OrderStatus } | null>(null)
  const [newStatus, setNewStatus] = useState('')
  const [trackingNumber, setTrackingNumber] = useState('')
  const [note, setNote] = useState('')
  const qc = useQueryClient()

  const { data: orders, isLoading } = useQuery({
    queryKey: ['admin', 'orders', statusFilter],
    queryFn: () => ordersApi.adminList({ status: statusFilter || undefined, size: 50 }).then(r => r.data.data),
  })

  const updateStatus = useMutation({
    mutationFn: () => ordersApi.adminUpdateStatus(selectedOrder!.id, {
      status: newStatus,
      note: note || undefined,
      trackingNumber: trackingNumber || undefined,
    }),
    onSuccess: () => {
      toast.success('Order status updated')
      qc.invalidateQueries({ queryKey: ['admin', 'orders'] })
      setSelectedOrder(null)
      setNewStatus(''); setNote(''); setTrackingNumber('')
    },
    onError: () => toast.error('Status update failed'),
  })

  const filtered = orders?.filter(o =>
    o.orderNumber.includes(search.toUpperCase()) ||
    o.id.includes(search)
  ) ?? []

  return (
    <div className="space-y-6">
      <div>
        <p className="section-label">Management</p>
        <h1 className="font-display font-bold text-3xl text-ink">Orders</h1>
      </div>

      {/* Toolbar */}
      <div className="flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-48">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink/40" />
          <input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search by order number…"
            className="w-full pl-9 pr-4 py-2.5 border-2 border-ink/15 bg-white text-sm font-body focus:outline-none focus:border-ember"
          />
        </div>
        <div className="relative">
          <select
            value={statusFilter}
            onChange={e => setStatusFilter(e.target.value)}
            className="appearance-none border-2 border-ink/15 bg-white px-4 pr-8 py-2.5 text-sm font-body focus:outline-none focus:border-ember"
          >
            <option value="">All Status</option>
            {['PENDING','AWAITING_PAYMENT','PAID','PROCESSING','SHIPPED','DELIVERED','CANCELLED','REFUNDED'].map(s => (
              <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>
            ))}
          </select>
          <ChevronDown size={12} className="absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none text-ink/40" />
        </div>
      </div>

      {/* Table */}
      <div className="bg-white border border-ash-dark overflow-hidden">
        {isLoading ? (
          <div className="p-6 space-y-3">
            {Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-14 w-full" />)}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-ash-dark border-b border-ink/8">
                  {['Order #', 'Items', 'Total', 'Status', 'Date', 'Actions'].map(h => (
                    <th key={h} className="px-5 py-3.5 text-left text-xs font-mono text-ink/50 uppercase tracking-wider">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-ash-dark">
                {filtered.map(order => (
                  <tr key={order.id} className="hover:bg-ash/30 transition-colors group">
                    <td className="px-5 py-3">
                      <Link to={`/admin/orders/${order.id}`} className="font-mono text-xs text-ember hover:underline font-medium">
                        {order.orderNumber}
                      </Link>
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-2">
                        {order.firstItemImage && (
                          <div className="w-8 h-8 bg-ash-dark overflow-hidden flex-shrink-0">
                            <img src={order.firstItemImage} alt="" className="w-full h-full object-cover" />
                          </div>
                        )}
                        <div className="min-w-0">
                          <p className="text-xs font-body text-ink truncate max-w-32">{order.firstItemName}</p>
                          {order.additionalItemCount > 0 && (
                            <p className="text-xs font-mono text-ink/35">+{order.additionalItemCount}</p>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-3 font-display font-semibold text-sm">{formatPrice(order.totalAmount)}</td>
                    <td className="px-5 py-3"><OrderStatusBadge status={order.status} /></td>
                    <td className="px-5 py-3 font-mono text-xs text-ink/40">{formatDate(order.createdAt)}</td>
                    <td className="px-5 py-3">
                      {STATUS_TRANSITIONS[order.status] && (
                        <button
                          onClick={() => { setSelectedOrder({ id: order.id, status: order.status }); setNewStatus(STATUS_TRANSITIONS[order.status][0]) }}
                          className="text-xs font-display font-semibold text-ember hover:underline opacity-0 group-hover:opacity-100 transition-opacity"
                        >
                          Update Status
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Status Update Modal */}
      <Modal
        open={!!selectedOrder}
        onClose={() => setSelectedOrder(null)}
        title="Update Order Status"
        size="sm"
      >
        {selectedOrder && (
          <div className="space-y-4">
            <div>
              <p className="section-label mb-2">New Status</p>
              <div className="flex flex-wrap gap-2">
                {(STATUS_TRANSITIONS[selectedOrder.status] ?? []).map(s => (
                  <button
                    key={s}
                    onClick={() => setNewStatus(s)}
                    className={`px-4 py-2 text-sm font-body border-2 transition-all ${
                      newStatus === s ? 'bg-ink text-ash border-ink' : 'border-ink/20 hover:border-ink'
                    }`}
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>

            {newStatus === 'SHIPPED' && (
              <div>
                <label className="section-label mb-1">Tracking Number *</label>
                <input
                  value={trackingNumber}
                  onChange={e => setTrackingNumber(e.target.value)}
                  placeholder="e.g. JP1234567890"
                  className="w-full border-2 border-ink/15 px-4 py-2.5 text-sm font-mono focus:border-ember focus:outline-none"
                />
              </div>
            )}

            <div>
              <label className="section-label mb-1">Internal Note (optional)</label>
              <textarea
                value={note}
                onChange={e => setNote(e.target.value)}
                rows={2}
                className="w-full border-2 border-ink/15 px-4 py-2.5 text-sm font-body resize-none focus:border-ember focus:outline-none"
              />
            </div>

            <div className="flex gap-3 pt-2">
              <Button
                fullWidth
                onClick={() => updateStatus.mutate()}
                loading={updateStatus.isPending}
                disabled={!newStatus || (newStatus === 'SHIPPED' && !trackingNumber)}
              >
                Confirm Update
              </Button>
              <Button variant="secondary" onClick={() => setSelectedOrder(null)}>Cancel</Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
