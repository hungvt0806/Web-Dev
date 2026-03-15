import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Layout } from '@/components/layout/Layout'
import { AdminLayout } from '@/components/layout/AdminLayout'
import { RequireAuth } from '@/components/auth/RequireAuth'
import { HomePage } from '@/pages/HomePage'
import { ProductListPage } from '@/pages/ProductListPage'
import { ProductDetailPage } from '@/pages/ProductDetailPage'
import { CartPage } from '@/pages/CartPage'
import { CheckoutPage } from '@/pages/CheckoutPage'
import { LoginPage } from '@/pages/LoginPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { ProfilePage } from '@/pages/ProfilePage'
import { OrderHistoryPage } from '@/pages/OrderHistoryPage'
import { OrderDetailPage } from '@/pages/OrderDetailPage'
import { AdminDashboardPage } from '@/pages/admin/AdminDashboardPage'
import { AdminProductsPage } from '@/pages/admin/AdminProductsPage'
import { AdminOrdersPage } from '@/pages/admin/AdminOrdersPage'
import { OAuth2RedirectPage } from '@/pages/OAuth2RedirectPage'


const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 60_000,
    },
  },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          {/* Public auth pages (no navbar) */}
          <Route path="/login"    element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/oauth2/redirect" element={<OAuth2RedirectPage />} />

          {/* Main layout */}
          <Route element={<Layout />}>
            <Route path="/"                  element={<HomePage />} />
            <Route path="/products"          element={<ProductListPage />} />
            <Route path="/products/:slug"    element={<ProductDetailPage />} />
            <Route path="/cart"              element={<CartPage />} />

            {/* Protected routes */}
            <Route path="/checkout"          element={<RequireAuth><CheckoutPage /></RequireAuth>} />
            <Route path="/profile"           element={<RequireAuth><ProfilePage /></RequireAuth>} />
            <Route path="/orders"            element={<RequireAuth><OrderHistoryPage /></RequireAuth>} />
            <Route path="/orders/:id"        element={<RequireAuth><OrderDetailPage /></RequireAuth>} />
          </Route>

          {/* Admin layout */}
          <Route path="/admin" element={<AdminLayout />}>
  <Route index             element={<AdminDashboardPage />} />
  <Route path="products"   element={<AdminProductsPage />} />
  <Route path="orders"     element={<AdminOrdersPage />} />
  <Route path="orders/:id" element={<AdminOrdersPage />} />
  <Route path="customers"  element={<AdminDashboardPage />} />
  <Route path="settings"   element={<AdminDashboardPage />} />
</Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
