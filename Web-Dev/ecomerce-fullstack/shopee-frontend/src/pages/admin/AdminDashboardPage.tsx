import { useQuery } from '@tanstack/react-query'
import { TrendingUp, ShoppingBag, Package, Users, ArrowUpRight, ArrowDownRight } from 'lucide-react'
import { ordersApi } from '@/api/orders'
import { formatPrice, formatRelativeDate } from '@/utils'
import { OrderStatusBadge } from '@/components/order/OrderStatusBadge'
import { Skeleton } from '@/components/ui/Skeleton'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, LineChart, Line, CartesianGrid } from 'recharts'
import { Link } from 'react-router-dom'

// Fake chart data (replace with real API)
const revenueData = [
  { month: 'Oct', revenue: 420000 }, { month: 'Nov', revenue: 680000 },
  { month: 'Dec', revenue: 920000 }, { month: 'Jan', revenue: 510000 },
  { month: 'Feb', revenue: 730000 }, { month: 'Mar', revenue: 890000 },
]
const ordersData = [
  { day: 'Mon', orders: 12 }, { day: 'Tue', orders: 19 }, { day: 'Wed', orders: 15 },
  { day: 'Thu', orders: 24 }, { day: 'Fri', orders: 31 }, { day: 'Sat', orders: 42 },
  { day: 'Sun', orders: 28 },
]

export function AdminDashboardPage() {
  const { data: recentOrders, isLoading } = useQuery({
    queryKey: ['admin', 'orders', 'recent'],
    queryFn: () => ordersApi.adminList({ page: 0, size: 8 }).then(r => r.data.data),
  })

  const stats = [
    { label: 'Total Revenue',    value: formatPrice(4250000), change: '+12.5%', up: true,  icon: TrendingUp },
    { label: 'Orders (30d)',      value: '284',               change: '+8.2%',  up: true,  icon: ShoppingBag },
    { label: 'Active Products',  value: '1,247',              change: '-2.1%',  up: false, icon: Package },
    { label: 'Customers',        value: '3,891',              change: '+15.3%', up: true,  icon: Users },
  ]

  return (
    <div className="space-y-8">
      <div>
        <p className="section-label">Overview</p>
        <h1 className="font-display font-bold text-3xl text-ink">Dashboard</h1>
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        {stats.map(({ label, value, change, up, icon: Icon }, i) => (
          <div
            key={label}
            className="bg-white border border-ash-dark p-5 opacity-0 animate-fade-up"
            style={{ animationDelay: `${i * 0.07}s`, animationFillMode: 'forwards' }}
          >
            <div className="flex items-start justify-between mb-4">
              <div className="w-10 h-10 bg-ash-dark flex items-center justify-center">
                <Icon size={18} className="text-ink/50" />
              </div>
              <span className={`flex items-center gap-0.5 text-xs font-mono font-medium ${up ? 'text-sage' : 'text-red-500'}`}>
                {up ? <ArrowUpRight size={12} /> : <ArrowDownRight size={12} />}
                {change}
              </span>
            </div>
            <p className="font-display font-black text-2xl text-ink">{value}</p>
            <p className="text-xs font-mono text-ink/40 uppercase tracking-wider mt-1">{label}</p>
          </div>
        ))}
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-5">
        {/* Revenue */}
        <div className="lg:col-span-3 bg-white border border-ash-dark p-6">
          <div className="flex items-center justify-between mb-5">
            <div>
              <p className="section-label">6-Month Trend</p>
              <h3 className="font-display font-bold text-lg">Revenue</h3>
            </div>
          </div>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={revenueData} barSize={28}>
              <XAxis dataKey="month" axisLine={false} tickLine={false} tick={{ fontSize: 11, fontFamily: 'DM Mono', fill: '#0A0A0A60' }} />
              <YAxis hide />
              <Tooltip
                contentStyle={{ background: '#0A0A0A', border: 'none', color: '#F5F3EE', fontFamily: 'DM Mono', fontSize: 12, borderRadius: 0 }}
                formatter={(v: number) => [formatPrice(v), 'Revenue']}
              />
              <Bar dataKey="revenue" fill="#E8441A" radius={[2, 2, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Orders this week */}
        <div className="lg:col-span-2 bg-white border border-ash-dark p-6">
          <div className="mb-5">
            <p className="section-label">This Week</p>
            <h3 className="font-display font-bold text-lg">Orders</h3>
          </div>
          <ResponsiveContainer width="100%" height={200}>
            <LineChart data={ordersData}>
              <XAxis dataKey="day" axisLine={false} tickLine={false} tick={{ fontSize: 11, fontFamily: 'DM Mono', fill: '#0A0A0A60' }} />
              <YAxis hide />
              <Tooltip
                contentStyle={{ background: '#0A0A0A', border: 'none', color: '#F5F3EE', fontFamily: 'DM Mono', fontSize: 12, borderRadius: 0 }}
              />
              <Line dataKey="orders" stroke="#4A7C6A" strokeWidth={2.5} dot={{ fill: '#4A7C6A', r: 4 }} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Recent Orders */}
      <div className="bg-white border border-ash-dark">
        <div className="flex items-center justify-between px-6 py-4 border-b border-ash-dark">
          <h3 className="font-display font-bold text-base">Recent Orders</h3>
          <Link to="/admin/orders" className="text-xs font-mono text-ember hover:underline">View All</Link>
        </div>

        {isLoading ? (
          <div className="p-6 space-y-3">
            {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-ash-dark">
                  {['Order', 'Customer', 'Items', 'Total', 'Status', 'Date'].map(h => (
                    <th key={h} className="px-6 py-3 text-left text-xs font-mono text-ink/40 uppercase tracking-wider font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-ash-dark">
                {(recentOrders ?? []).map(order => (
                  <tr key={order.id} className="hover:bg-ash/50 transition-colors">
                    <td className="px-6 py-3">
                      <Link to={`/admin/orders/${order.id}`} className="font-mono text-xs text-ember hover:underline">
                        {order.orderNumber}
                      </Link>
                    </td>
                    <td className="px-6 py-3 font-body text-xs text-ink/70">—</td>
                    <td className="px-6 py-3 font-mono text-xs text-ink/50">{order.itemCount}</td>
                    <td className="px-6 py-3 font-display font-semibold text-sm">{formatPrice(order.totalAmount)}</td>
                    <td className="px-6 py-3"><OrderStatusBadge status={order.status} /></td>
                    <td className="px-6 py-3 font-mono text-xs text-ink/40">{formatRelativeDate(order.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
