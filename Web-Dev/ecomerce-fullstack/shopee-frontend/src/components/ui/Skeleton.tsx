import { cn } from '@/utils'

export function Skeleton({ className }: { className?: string }) {
  return (
    <div className={cn(
      'bg-gradient-to-r from-ash-dark via-ash to-ash-dark bg-[length:200%_100%] animate-shimmer',
      className
    )} />
  )
}

export function ProductCardSkeleton() {
  return (
    <div className="bg-white border border-ash-dark overflow-hidden">
      <Skeleton className="aspect-square w-full" />
      <div className="p-4 space-y-2">
        <Skeleton className="h-4 w-3/4" />
        <Skeleton className="h-3 w-1/2" />
        <Skeleton className="h-5 w-1/3 mt-2" />
      </div>
    </div>
  )
}
