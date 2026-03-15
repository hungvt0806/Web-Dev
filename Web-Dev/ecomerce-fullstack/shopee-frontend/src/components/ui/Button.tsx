import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { cn } from '@/utils'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  fullWidth?: boolean
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant = 'primary', size = 'md', loading, fullWidth, className, children, disabled, ...props }, ref) => {
    const base = 'inline-flex items-center justify-center gap-2 font-display font-semibold transition-all duration-150 active:scale-95 focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:pointer-events-none select-none'

    const variants = {
      primary:   'bg-ember text-white hover:bg-ember-dark focus:ring-ember',
      secondary: 'bg-transparent border-2 border-ink text-ink hover:bg-ink hover:text-ash focus:ring-ink',
      ghost:     'bg-transparent text-ink hover:bg-black/5 focus:ring-ink/20',
      danger:    'bg-red-600 text-white hover:bg-red-700 focus:ring-red-500',
    }
    const sizes = {
      sm: 'text-xs px-4 py-2',
      md: 'text-sm px-6 py-3',
      lg: 'text-base px-8 py-4',
    }

    return (
      <button
        ref={ref}
        disabled={disabled || loading}
        className={cn(base, variants[variant], sizes[size], fullWidth && 'w-full', className)}
        {...props}
      >
        {loading && (
          <span className="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" />
        )}
        {children}
      </button>
    )
  }
)
Button.displayName = 'Button'
