// ── Auth ──────────────────────────────────────────────────────────────────
export interface User {
  id: string;
  email: string;
  fullName: string;
  phoneNumber?: string;
  avatarUrl?: string;
  role: 'BUYER' | 'SELLER' | 'ADMIN';
  active: boolean;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

// ── Products ──────────────────────────────────────────────────────────────
export interface Category {
  id: number;
  name: string;
  slug: string;
  description?: string;
  imageUrl?: string;
  parentId?: number;
  children?: Category[];
  sortOrder: number;
}

export interface ProductVariant {
  id: number;
  sku: string;
  attributes: Record<string, string>;
  price: number;
  stock: number;
  imageUrl?: string;
  active: boolean;
}

export interface Product {
  id: number;
  name: string;
  slug: string;
  description: string;
  shortDescription?: string;
  basePrice: number;
  originalPrice?: number;
  currency: string;
  totalStock: number;
  soldCount: number;
  thumbnailUrl?: string;
  imageUrls: string[];
  tags: string[];
  ratingAvg: number;
  ratingCount: number;
  weightGrams?: number;
  category: Category;
  variants: ProductVariant[];
  status: 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'DELETED';
  featured: boolean;
  sellerId: string;
  sellerName?: string;
  createdAt: string;
}

export interface ProductSummary {
  id: number;
  name: string;
  slug: string;
  basePrice: number;
  originalPrice?: number;
  thumbnailUrl?: string;
  ratingAvg: number;
  ratingCount: number;
  soldCount: number;
  totalStock: number;
  category: Pick<Category, 'id' | 'name' | 'slug'>;
}

// ── Cart ──────────────────────────────────────────────────────────────────
export interface CartItem {
  id: number;
  productId: number;
  productName: string;
  productImage?: string;
  variantId?: number;
  variantAttributes?: Record<string, string>;
  sku?: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  priceStale: boolean;
  availableStock: number;
}

export interface Cart {
  id: number;
  items: CartItem[];
  itemCount: number;
  totalQuantity: number;
  subtotal: number;
  hasStalePrices: boolean;
}

// ── Orders ────────────────────────────────────────────────────────────────
export type OrderStatus =
  | 'PENDING' | 'AWAITING_PAYMENT' | 'PAID'
  | 'PROCESSING' | 'SHIPPED' | 'DELIVERED'
  | 'CANCELLED' | 'REFUNDED';

export interface ShippingAddress {
  recipientName: string;
  phone: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  prefecture: string;
  postalCode: string;
  country: string;
}

export interface OrderItem {
  id: number;
  productId?: number;
  productName: string;
  productImage?: string;
  sku?: string;
  variantAttributes?: Record<string, string>;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
  displayName: string;
  reviewed: boolean;
}

export interface OrderStatusHistory {
  id: number;
  fromStatus?: OrderStatus;
  toStatus: OrderStatus;
  note?: string;
  actorType: 'SYSTEM' | 'BUYER' | 'SELLER' | 'ADMIN';
  createdAt: string;
}

export interface OrderTimelineEntry {
  status: OrderStatus;
  label: string;
  description?: string;
  completedAt?: string;
  completed: boolean;
  current: boolean;
}

export interface Order {
  id: string;
  orderNumber: string;
  status: OrderStatus;
  items: OrderItem[];
  statusHistory: OrderStatusHistory[];
  timeline: OrderTimelineEntry[];
  shippingAddress: ShippingAddress;
  subtotal: number;
  shippingFee: number;
  discountAmount: number;
  totalAmount: number;
  currency: string;
  buyerNote?: string;
  cancellationReason?: string;
  trackingNumber?: string;
  shippingCarrier?: string;
  paidAt?: string;
  shippedAt?: string;
  deliveredAt?: string;
  cancelledAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface OrderSummary {
  id: string;
  orderNumber: string;
  status: OrderStatus;
  totalAmount: number;
  currency: string;
  itemCount: number;
  firstItemName?: string;
  firstItemImage?: string;
  additionalItemCount: number;
  createdAt: string;
}

// ── Payment ───────────────────────────────────────────────────────────────
export interface PaymentInitResponse {
  paymentId: string;
  merchantPaymentId: string;
  orderId: string;
  amount: number;
  currency: string;
  status: string;
  paymentUrl: string;
  deeplink?: string;
  expiresAt: string;
}

// ── API Response wrappers ─────────────────────────────────────────────────
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
  page?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
  last?: boolean;
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  last: boolean;
}
