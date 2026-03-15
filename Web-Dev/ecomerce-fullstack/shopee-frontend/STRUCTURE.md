# Shopee Frontend — Project Structure

## Tech Stack
- **React 18** + **TypeScript** + **Vite** (build tool)
- **Tailwind CSS** — utility-first styling with custom design tokens
- **React Router v6** — client-side routing
- **Axios** — HTTP client with JWT interceptors and auto-refresh
- **Zustand** — global state (auth, cart)
- **TanStack Query** — server-state caching and data fetching
- **React Hook Form** + **Zod** — form management with schema validation
- **Recharts** — dashboard charts
- **React Hot Toast** — toast notifications

---

## Design System

### Colors (tailwind.config.js)
| Token         | Value     | Use                         |
|---------------|-----------|------------------------------|
| `ink`         | `#0E0E0E` | Primary text, buttons        |
| `ink.light`   | `#2A2A2A` | Hover state for dark elements|
| `ink.muted`   | `#6B6B6B` | Secondary text, icons        |
| `cream`       | `#FAF9F6` | Page background              |
| `cream.warm`  | `#F5F2EC` | Card backgrounds, hover      |
| `accent`      | `#E8431A` | CTA buttons, links, badges   |
| `border`      | `#E2DED8` | Dividers, input borders      |
| `success`     | `#1A7A4A` | Success states               |
| `warning`     | `#D4830A` | Warning states               |
| `error`       | `#C41C1C` | Error states                 |

### Typography
- **Display**: `Noto Serif JP` — h1/h2/h3 and prices
- **Body**: `DM Sans` — all UI text
- **Mono**: `DM Mono` — order numbers, SKUs, codes

### Component Classes (index.css)
```
btn, btn-sm, btn-md, btn-lg         — base button sizes
btn-primary, btn-accent, btn-outline, btn-ghost, btn-danger — button variants
input, input-lg, input-error        — form inputs
label, hint, error-msg              — form labels
card, card-lg                       — containers
badge, badge-default, badge-accent, badge-success, badge-warning, badge-error — status badges
price, price-lg, price-strike       — price display
section, container-page             — layout helpers
skeleton                            — loading placeholders
```

---

## Folder Structure

```
shopee-frontend/
├── public/
│   └── favicon.svg
├── src/
│   ├── api/
│   │   ├── client.ts          # Axios instance, token storage, interceptors
│   │   └── services.ts        # All API calls grouped by domain
│   │
│   ├── components/
│   │   ├── ui/
│   │   │   └── index.tsx      # Button, Input, Textarea, Badge, Skeleton,
│   │   │                      # Spinner, Modal, QuantityStepper, StarRating,
│   │   │                      # PriceDisplay, Empty, Divider
│   │   │
│   │   ├── layout/
│   │   │   ├── Header.tsx     # Navigation, search overlay, cart badge, user menu
│   │   │   └── index.tsx      # MainLayout, AdminLayout (sidebar), Footer
│   │   │
│   │   ├── product/
│   │   │   └── index.tsx      # ProductCard (grid/list), ProductGrid, ProductImageGallery
│   │   │
│   │   ├── order/
│   │   │   └── index.tsx      # OrderStatusBadge, OrderCard, OrderTimeline, ShippingInfo
│   │   │
│   │   ├── cart/              # (Reserved for future CartItem components)
│   │   ├── auth/              # (Reserved for future auth widgets)
│   │   └── admin/             # (Reserved for future admin widgets)
│   │
│   ├── pages/
│   │   ├── HomePage.tsx           # Hero, categories, featured products, CTA
│   │   ├── ProductListPage.tsx    # Search, filters, sort, grid/list toggle, pagination
│   │   ├── ProductDetailPage.tsx  # Gallery, variants, add to cart, tabs
│   │   ├── CartPage.tsx           # Cart items, price sync warning, order summary
│   │   ├── CheckoutPage.tsx       # Multi-step: address form → review → place order
│   │   ├── PaymentPage.tsx        # PayPay initiation, status polling, redirect
│   │   ├── LoginPage.tsx          # Sign-in form (+ RegisterPage export)
│   │   ├── RegisterPage.tsx       # Re-exports from LoginPage
│   │   ├── ProfilePage.tsx        # Edit profile, change password
│   │   ├── OrderHistoryPage.tsx   # Paginated order list with status filters
│   │   ├── OrderDetailPage.tsx    # Full order detail, timeline, cancel button
│   │   └── admin/
│   │       ├── AdminDashboardPage.tsx  # Stats cards, revenue chart, order distribution
│   │       ├── AdminProductsPage.tsx   # Product table, CRUD modal, delete confirm
│   │       └── AdminOrdersPage.tsx     # Order table, status update modal
│   │
│   ├── store/
│   │   ├── authStore.ts       # Zustand: user, isAuth, login/logout/register/fetchMe
│   │   └── cartStore.ts       # Zustand: cart state, add/update/remove/sync
│   │
│   ├── hooks/
│   │   └── index.ts           # useDebounce, useLocalStorage, useScrollTop,
│   │                          # useIntersectionObserver, usePagination, useMediaQuery
│   │
│   ├── types/
│   │   └── index.ts           # TypeScript types: User, Product, Cart, Order, Payment, etc.
│   │
│   ├── utils/
│   │   └── index.ts           # cn(), formatPrice(), formatDate(), getOrderStatusMeta(), etc.
│   │
│   ├── App.tsx                # Route tree with auth guards (RequireAuth, RequireAdmin)
│   ├── main.tsx               # React root, QueryClient, BrowserRouter, Toaster
│   └── index.css              # Tailwind + global design system CSS
│
├── index.html                 # Google Fonts: Noto Serif JP, DM Sans, DM Mono
├── package.json
├── tailwind.config.js         # Extended design tokens
├── tsconfig.json
├── vite.config.ts             # Proxy /api → localhost:8080
└── postcss.config.js
```

---

## API Integration

All API calls are in `src/api/services.ts`, grouped by domain:

```ts
authApi.login(email, password)
authApi.register(email, password, fullName)
authApi.logout(refreshToken)
authApi.me()

productApi.list(filters)        // with pagination
productApi.bySlug(slug)
productApi.featured()
productApi.adminList(params)    // admin only
productApi.create(data)
productApi.update(id, data)
productApi.delete(id)

cartApi.get()
cartApi.add(productId, variantId, quantity)
cartApi.update(cartItemId, quantity)
cartApi.remove(cartItemId)
cartApi.syncPrices()

orderApi.place(request)
orderApi.history(params)
orderApi.detail(id)
orderApi.cancel(id, reason)
orderApi.adminList(params)
orderApi.updateStatus(id, status, note, trackingNumber, carrier)

paymentApi.initiate(orderId)    // Returns PayPay redirect URL
paymentApi.status(orderId)      // Poll for payment confirmation
paymentApi.refund(orderId, reason)
```

The Axios client in `src/api/client.ts`:
- Attaches JWT to every request via `Authorization: Bearer {token}`
- Auto-refreshes on 401 (deduplicates concurrent refresh calls)
- Clears tokens and redirects to /login on refresh failure

---

## Authentication Flow

1. User logs in → tokens stored in localStorage
2. App boot: `fetchMe()` called if access token exists
3. Protected routes wrapped in `<RequireAuth>` / `<RequireAdmin>`
4. Every API request automatically includes the token
5. On 401: token auto-refreshed; on refresh failure → logout + redirect

---

## Getting Started

```bash
npm install
npm run dev         # Start dev server at http://localhost:3000
npm run build       # Production build
npm run preview     # Preview production build
```

The dev server proxies `/api/*` → `http://localhost:8080` (Spring Boot backend).

Set `PAYPAY_*` environment variables in the backend's `application.yml` — see the payment module README.
