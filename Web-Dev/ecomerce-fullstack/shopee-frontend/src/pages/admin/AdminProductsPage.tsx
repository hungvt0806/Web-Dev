import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Search, Edit2, Trash2, Eye, Package } from 'lucide-react'
import { productsApi } from '@/api/products'
import { Button } from '@/components/ui/Button'
import { Badge } from '@/components/ui/Badge'
import { Skeleton } from '@/components/ui/Skeleton'
import { formatPrice, formatDate } from '@/utils'
import toast from 'react-hot-toast'

const STATUS_COLORS = {
  ACTIVE:  'sage',
  DRAFT:   'outline',
  PAUSED:  'gold',
  DELETED: 'default',
} as const

export function AdminProductsPage() {
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const qc = useQueryClient()

  const { data: products, isLoading } = useQuery({
    queryKey: ['admin', 'products', statusFilter],
    queryFn: () => productsApi.adminList({ status: statusFilter || undefined, size: 50 }).then(r => r.data.data),
  })

  const deleteProduct = useMutation({
    mutationFn: productsApi.adminDelete,
    onSuccess: () => { toast.success('Product deleted'); qc.invalidateQueries({ queryKey: ['admin', 'products'] }) },
  })

  const filtered = products?.filter(p =>
    p.name.toLowerCase().includes(search.toLowerCase()) ||
    p.id.toString().includes(search)
  ) ?? []

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-4">
        <div>
          <p className="section-label">Management</p>
          <h1 className="font-display font-bold text-3xl text-ink">Products</h1>
        </div>
        <Button size="sm">
          <Plus size={16} /> Add Product
        </Button>
      </div>

      {/* Toolbar */}
      <div className="flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-48">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink/40" />
          <input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search products…"
            className="w-full pl-9 pr-4 py-2.5 border-2 border-ink/15 bg-white text-sm font-body focus:outline-none focus:border-ember"
          />
        </div>
        <select
          value={statusFilter}
          onChange={e => setStatusFilter(e.target.value)}
          className="border-2 border-ink/15 bg-white px-4 py-2.5 text-sm font-body focus:outline-none focus:border-ember"
        >
          <option value="">All Status</option>
          <option value="ACTIVE">Active</option>
          <option value="DRAFT">Draft</option>
          <option value="PAUSED">Paused</option>
        </select>
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
                  {['Product', 'Category', 'Price', 'Stock', 'Status', 'Created', 'Actions'].map(h => (
                    <th key={h} className="px-5 py-3.5 text-left text-xs font-mono text-ink/50 uppercase tracking-wider">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-ash-dark">
                {filtered.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-5 py-16 text-center">
                      <div className="flex flex-col items-center gap-2 text-ink/30">
                        <Package size={28} />
                        <p className="font-body text-sm">No products found</p>
                      </div>
                    </td>
                  </tr>
                ) : filtered.map(product => (
                  <tr key={product.id} className="hover:bg-ash/30 transition-colors group">
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 bg-ash-dark flex-shrink-0 overflow-hidden">
                          {product.thumbnailUrl ? (
                            <img src={product.thumbnailUrl} alt="" className="w-full h-full object-cover" />
                          ) : (
                            <div className="w-full h-full flex items-center justify-center text-ink/15">
                              <Package size={14} />
                            </div>
                          )}
                        </div>
                        <div className="min-w-0">
                          <p className="font-body font-medium text-ink text-sm truncate max-w-48">{product.name}</p>
                          <p className="text-xs font-mono text-ink/35">#{product.id}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-3 font-body text-xs text-ink/60">{product.category?.name}</td>
                    <td className="px-5 py-3 font-display font-semibold text-sm">{formatPrice(product.basePrice)}</td>
                    <td className="px-5 py-3">
                      <span className={`font-mono text-sm font-medium ${product.totalStock === 0 ? 'text-red-500' : product.totalStock < 10 ? 'text-gold' : 'text-ink/60'}`}>
                        {product.totalStock}
                      </span>
                    </td>
                    <td className="px-5 py-3">
                      <Badge variant={STATUS_COLORS[product.status as keyof typeof STATUS_COLORS] || 'default'}>
                        {product.status}
                      </Badge>
                    </td>
                    <td className="px-5 py-3 font-mono text-xs text-ink/40">{formatDate(product.createdAt)}</td>
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button className="p-1.5 hover:bg-ash-dark rounded transition-colors" title="View">
                          <Eye size={13} />
                        </button>
                        <button className="p-1.5 hover:bg-ash-dark rounded transition-colors" title="Edit">
                          <Edit2 size={13} />
                        </button>
                        <button
                          onClick={() => deleteProduct.mutate(product.id)}
                          className="p-1.5 hover:bg-red-50 hover:text-red-600 rounded transition-colors"
                          title="Delete"
                        >
                          <Trash2 size={13} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <p className="text-xs font-mono text-ink/30">
        {filtered.length} product{filtered.length !== 1 ? 's' : ''} shown
      </p>
    </div>
  )
}
