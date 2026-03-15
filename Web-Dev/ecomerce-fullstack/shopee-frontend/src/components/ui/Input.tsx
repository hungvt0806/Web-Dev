import { forwardRef, type InputHTMLAttributes } from 'react'
import { cn } from '@/utils'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  hint?: string
  leftIcon?: React.ReactNode
  rightIcon?: React.ReactNode
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, hint, leftIcon, rightIcon, className, id, ...props }, ref) => {
    const inputId = id || label?.toLowerCase().replace(/\s+/g, '-')
    return (
      <div className="flex flex-col gap-1.5">
        {label && (
          <label htmlFor={inputId} className="text-sm font-medium font-body text-ink/70">
            {label}
          </label>
        )}
        <div className="relative">
          {leftIcon && (
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-ink/40">
              {leftIcon}
            </span>
          )}
          <input
            ref={ref}
            id={inputId}
            className={cn(
              'w-full bg-white border-2 border-ink/20 px-4 py-3 font-body text-ink text-sm',
              'placeholder:text-ink/35 transition-colors duration-150 rounded-none',
              'focus:outline-none focus:border-ember',
              error && 'border-red-500 focus:border-red-500',
              leftIcon && 'pl-10',
              rightIcon && 'pr-10',
              className
            )}
            {...props}
          />
          {rightIcon && (
            <span className="absolute right-3 top-1/2 -translate-y-1/2 text-ink/40">
              {rightIcon}
            </span>
          )}
        </div>
        {error && <p className="text-xs text-red-600 font-body">{error}</p>}
        {hint && !error && <p className="text-xs text-ink/40 font-body">{hint}</p>}
      </div>
    )
  }
)
Input.displayName = 'Input'
