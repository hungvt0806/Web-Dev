import { cn } from '@/utils'

interface BadgeProps {
  children: React.ReactNode
  variant?: 'default' | 'ember' | 'sage' | 'gold' | 'outline'
  className?: string
}

export function Badge({ children, variant = 'default', className }: BadgeProps) {
  const variants = {
    default: 'bg-ink/8 text-ink',
    ember:   'bg-ember text-white',
    sage:    'bg-sage/15 text-sage-dark',
    gold:    'bg-gold/15 text-amber-800',
    outline: 'border border-ink/25 text-ink/70',
  }
  return (
    <span className={cn(
      'inline-flex items-center px-2 py-0.5 text-xs font-mono font-medium uppercase tracking-wider',
      variants[variant], className
    )}>
      {children}
    </span>
  )
}
