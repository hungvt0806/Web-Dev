import { Star } from 'lucide-react'
import { cn } from '@/utils'

interface RatingProps {
  value: number
  count?: number
  size?: 'sm' | 'md'
  showCount?: boolean
}

export function Rating({ value, count, size = 'sm', showCount = true }: RatingProps) {
  const stars = Array.from({ length: 5 }, (_, i) => i + 1)
  const sz = size === 'sm' ? 12 : 16

  return (
    <div className="flex items-center gap-1.5">
      <div className="flex items-center gap-0.5">
        {stars.map((star) => (
          <Star
            key={star}
            size={sz}
            className={cn(
              star <= Math.round(value) ? 'text-gold fill-gold' : 'text-ink/20 fill-ink/10'
            )}
          />
        ))}
      </div>
      {showCount && count !== undefined && (
        <span className={cn('font-body text-ink/40', size === 'sm' ? 'text-xs' : 'text-sm')}>
          ({count.toLocaleString()})
        </span>
      )}
    </div>
  )
}
