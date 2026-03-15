import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'
import { format, formatDistanceToNow } from 'date-fns'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatPrice(amount: number, currency = 'JPY'): string {
  return new Intl.NumberFormat('ja-JP', {
    style: 'currency',
    currency,
    minimumFractionDigits: 0,
  }).format(amount)
}

export function formatDate(dateStr: string): string {
  return format(new Date(dateStr), 'MMM d, yyyy')
}

export function formatRelativeDate(dateStr: string): string {
  return formatDistanceToNow(new Date(dateStr), { addSuffix: true })
}

export function truncate(str: string, length: number): string {
  return str.length > length ? str.slice(0, length) + '…' : str
}

export function getDiscountPercent(original: number, current: number): number {
  return Math.round(((original - current) / original) * 100)
}

export function slugify(str: string): string {
  return str.toLowerCase().replace(/\s+/g, '-').replace(/[^\w-]/g, '')
}

export const ORDER_STATUS_COLORS: Record<string, string> = {
  PENDING:          'bg-gold/15 text-gold-dark border-gold/30',
  AWAITING_PAYMENT: 'bg-blue-50 text-blue-700 border-blue-200',
  PAID:             'bg-sage/15 text-sage-dark border-sage/30',
  PROCESSING:       'bg-sage/15 text-sage border-sage/30',
  SHIPPED:          'bg-ember/10 text-ember border-ember/20',
  DELIVERED:        'bg-sage/20 text-sage-dark border-sage/40',
  CANCELLED:        'bg-red-50 text-red-600 border-red-200',
  REFUNDED:         'bg-purple-50 text-purple-600 border-purple-200',
}

export const ORDER_STATUS_LABELS: Record<string, string> = {
  PENDING:          'Pending',
  AWAITING_PAYMENT: 'Awaiting Payment',
  PAID:             'Paid',
  PROCESSING:       'Processing',
  SHIPPED:          'Shipped',
  DELIVERED:        'Delivered',
  CANCELLED:        'Cancelled',
  REFUNDED:         'Refunded',
}
