import React from 'react'
import { cn } from '@/utils'
import { Loader2, X } from 'lucide-react'

// ── Button ────────────────────────────────────────────────────────────────

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'accent' | 'outline' | 'ghost' | 'danger'
  size?:    'sm' | 'md' | 'lg'
  loading?: boolean
  icon?:    React.ReactNode
  iconEnd?: React.ReactNode
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = 'primary', size = 'md', loading, icon, iconEnd, children, disabled, ...props }, ref) => {
    const variantClass = {
      primary: 'btn-primary',
      accent:  'btn-accent',
      outline: 'btn-outline',
      ghost:   'btn-ghost',
      danger:  'btn-danger',
    }[variant]
    const sizeClass = { sm: 'btn-sm', md: 'btn-md', lg: 'btn-lg' }[size]

    return (
      <button
        ref={ref}
        className={cn('btn', variantClass, sizeClass, className)}
        disabled={disabled || loading}
        {...props}
      >
        {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : icon}
        {children}
        {!loading && iconEnd}
      </button>
    )
  }
)
Button.displayName = 'Button'

// ── Input ─────────────────────────────────────────────────────────────────

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?:  string
  hint?:   string
  error?:  string
  iconLeft?: React.ReactNode
  iconRight?: React.ReactNode
  size?: 'sm' | 'md' | 'lg'
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, hint, error, iconLeft, iconRight, size = 'md', id, ...props }, ref) => {
    const inputId = id || label?.toLowerCase().replace(/\s+/g, '-')
    return (
      <div className="w-full">
        {label && <label htmlFor={inputId} className="label">{label}</label>}
        <div className="relative">
          {iconLeft && (
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-muted pointer-events-none">
              {iconLeft}
            </span>
          )}
          <input
            ref={ref}
            id={inputId}
            className={cn(
              error ? 'input-error' : 'input',
              size === 'lg' && 'input-lg',
              iconLeft && 'pl-9',
              iconRight && 'pr-9',
              className
            )}
            {...props}
          />
          {iconRight && (
            <span className="absolute right-3 top-1/2 -translate-y-1/2 text-ink-muted">
              {iconRight}
            </span>
          )}
        </div>
        {hint && !error && <p className="hint">{hint}</p>}
        {error && <p className="error-msg">{error}</p>}
      </div>
    )
  }
)
Input.displayName = 'Input'

// ── Textarea ──────────────────────────────────────────────────────────────

interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string
  error?: string
  hint?:  string
}

export const Textarea = React.forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, label, error, hint, id, ...props }, ref) => {
    const inputId = id || label?.toLowerCase().replace(/\s+/g, '-')
    return (
      <div className="w-full">
        {label && <label htmlFor={inputId} className="label">{label}</label>}
        <textarea
          ref={ref}
          id={inputId}
          className={cn(
            'w-full font-body text-sm text-ink bg-white border border-border rounded-xl px-4 py-3',
            'outline-none transition-all duration-200 placeholder:text-ink-muted resize-none',
            'hover:border-border-strong focus:border-ink focus:ring-2 focus:ring-ink/8',
            error && 'border-error focus:border-error',
            className
          )}
          {...props}
        />
        {hint && !error && <p className="hint">{hint}</p>}
        {error && <p className="error-msg">{error}</p>}
      </div>
    )
  }
)
Textarea.displayName = 'Textarea'

// ── Badge ─────────────────────────────────────────────────────────────────

interface BadgeProps { className?: string; children: React.ReactNode; dot?: boolean }
export const Badge = ({ className, children, dot }: BadgeProps) => (
  <span className={cn('badge-default', className)}>
    {dot && <span className="w-1.5 h-1.5 rounded-full bg-current opacity-70" />}
    {children}
  </span>
)

// ── Skeleton ──────────────────────────────────────────────────────────────

export const Skeleton = ({ className }: { className?: string }) => (
  <div className={cn('skeleton', className)} />
)

export const ProductCardSkeleton = () => (
  <div className="card overflow-hidden">
    <Skeleton className="aspect-square w-full rounded-none" />
    <div className="p-4 space-y-2">
      <Skeleton className="h-4 w-3/4" />
      <Skeleton className="h-3 w-1/2" />
      <Skeleton className="h-5 w-1/3 mt-2" />
    </div>
  </div>
)

// ── Spinner ───────────────────────────────────────────────────────────────

export const Spinner = ({ size = 'md', className }: { size?: 'sm' | 'md' | 'lg'; className?: string }) => {
  const s = { sm: 'h-4 w-4', md: 'h-6 w-6', lg: 'h-8 w-8' }[size]
  return <Loader2 className={cn(s, 'animate-spin text-ink-muted', className)} />
}

// ── Divider ───────────────────────────────────────────────────────────────

export const Divider = ({ className }: { className?: string }) => (
  <hr className={cn('divider', className)} />
)

// ── Empty state ───────────────────────────────────────────────────────────

interface EmptyProps { icon?: React.ReactNode; title: string; description?: string; action?: React.ReactNode }
export const Empty = ({ icon, title, description, action }: EmptyProps) => (
  <div className="flex flex-col items-center justify-center py-20 text-center">
    {icon && <div className="text-ink-muted mb-4 opacity-40">{icon}</div>}
    <h3 className="font-display text-xl font-medium text-ink mb-2">{title}</h3>
    {description && <p className="text-sm text-ink-muted max-w-sm">{description}</p>}
    {action && <div className="mt-6">{action}</div>}
  </div>
)

// ── Modal ─────────────────────────────────────────────────────────────────

interface ModalProps {
  open:       boolean
  onClose:    () => void
  title?:     string
  children:   React.ReactNode
  size?:      'sm' | 'md' | 'lg' | 'xl'
  className?: string
}

export const Modal = ({ open, onClose, title, children, size = 'md', className }: ModalProps) => {
  React.useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    if (open) document.addEventListener('keydown', handler)
    return () => document.removeEventListener('keydown', handler)
  }, [open, onClose])

  if (!open) return null
  const sizeClass = { sm: 'max-w-sm', md: 'max-w-md', lg: 'max-w-lg', xl: 'max-w-2xl' }[size]

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-ink/40 backdrop-blur-sm animate-fade-in" onClick={onClose} />
      <div className={cn('relative w-full card-lg p-6 animate-scale-in', sizeClass, className)}>
        {title && (
          <div className="flex items-center justify-between mb-5">
            <h2 className="font-display text-xl font-semibold">{title}</h2>
            <button onClick={onClose} className="btn-ghost btn-sm rounded-lg p-1.5"><X className="h-4 w-4" /></button>
          </div>
        )}
        {children}
      </div>
    </div>
  )
}

// ── Quantity Stepper ──────────────────────────────────────────────────────

interface QtyStepperProps { value: number; onChange: (v: number) => void; min?: number; max?: number; disabled?: boolean }
export const QuantityStepper = ({ value, onChange, min = 1, max = 99, disabled }: QtyStepperProps) => (
  <div className="flex items-center gap-0 border border-border rounded-xl overflow-hidden">
    <button
      onClick={() => onChange(Math.max(min, value - 1))}
      disabled={disabled || value <= min}
      className="w-9 h-9 flex items-center justify-center text-ink-muted hover:bg-cream-warm transition-colors disabled:opacity-30"
    >–</button>
    <span className="w-10 text-center text-sm font-medium">{value}</span>
    <button
      onClick={() => onChange(Math.min(max, value + 1))}
      disabled={disabled || value >= max}
      className="w-9 h-9 flex items-center justify-center text-ink-muted hover:bg-cream-warm transition-colors disabled:opacity-30"
    >+</button>
  </div>
)

// ── Rating stars ──────────────────────────────────────────────────────────

export const StarRating = ({ rating, count, size = 'sm' }: { rating: number; count?: number; size?: 'sm' | 'md' }) => {
  const sz = size === 'md' ? 'h-4 w-4' : 'h-3 w-3'
  return (
    <div className="flex items-center gap-1">
      <div className="flex gap-0.5">
        {[1,2,3,4,5].map(i => (
          <svg key={i} className={cn(sz, i <= Math.round(rating) ? 'text-amber-400' : 'text-border')} fill="currentColor" viewBox="0 0 20 20">
            <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
          </svg>
        ))}
      </div>
      {count !== undefined && <span className="text-xs text-ink-muted">({count.toLocaleString()})</span>}
    </div>
  )
}

// ── Price display ─────────────────────────────────────────────────────────

interface PriceDisplayProps {
  price: number
  originalPrice?: number
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

export const PriceDisplay = ({ price, originalPrice, size = 'md', className }: PriceDisplayProps) => {
  const sizeClass = { sm: 'text-base', md: 'text-xl', lg: 'text-3xl' }[size]
  const hasDiscount = originalPrice && originalPrice > price
  return (
    <div className={cn('flex items-baseline gap-2', className)}>
      <span className={cn('price', sizeClass)}>¥{price.toLocaleString()}</span>
      {hasDiscount && (
        <>
          <span className="price-strike">¥{originalPrice!.toLocaleString()}</span>
          <span className="badge-accent text-xs">
            -{Math.round((1 - price / originalPrice!) * 100)}%
          </span>
        </>
      )}
    </div>
  )
}
