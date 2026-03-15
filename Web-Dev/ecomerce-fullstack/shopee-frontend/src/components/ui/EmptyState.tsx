import { Button } from './Button'
import { Link } from 'react-router-dom'
import { cn } from '@/utils'

interface EmptyStateProps {
  icon?: React.ReactNode
  title: string
  description?: string
  action?: { label: string; href?: string; onClick?: () => void }
  className?: string
}

export function EmptyState({ icon, title, description, action, className }: EmptyStateProps) {
  return (
    <div className={cn('flex flex-col items-center justify-center py-20 text-center', className)}>
      {icon && (
        <div className="w-16 h-16 flex items-center justify-center bg-ash-dark mb-5 text-ink/30">
          {icon}
        </div>
      )}
      <h3 className="font-display font-bold text-xl text-ink mb-2">{title}</h3>
      {description && <p className="text-ink/50 font-body text-sm max-w-xs mb-6">{description}</p>}
      {action && (
        action.href ? (
          <Link to={action.href}><Button size="sm">{action.label}</Button></Link>
        ) : (
          <Button size="sm" onClick={action.onClick}>{action.label}</Button>
        )
      )}
    </div>
  )
}
