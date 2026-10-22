import { Loader2 } from 'lucide-react'
import type { ComponentProps, ReactNode } from 'react'

type FieldProps = ComponentProps<'input'> & { label: string; error?: string; hint?: string }

export function Field({ label, error, hint, ...props }: FieldProps) {
  const noteId = error ? `${props.id}-error` : hint ? `${props.id}-hint` : undefined
  return (
    <div>
      <label htmlFor={props.id} className="mb-1.5 block text-sm font-medium text-muted">{label}</label>
      <input
        {...props}
        aria-invalid={!!error}
        aria-describedby={noteId}
        className="w-full bg-surface px-3.5 py-3 text-white outline-2 outline-transparent transition placeholder:text-muted read-only:text-muted focus:outline-accent aria-[invalid=true]:outline-red-400"
      />
      {error ? (
        <p id={noteId} className="mt-1.5 text-sm text-red-300">{error}</p>
      ) : (
        hint && <p id={noteId} className="mt-1.5 text-sm text-muted">{hint}</p>
      )}
    </div>
  )
}

type SubmitButtonProps = {
  loading: boolean
  disabled?: boolean
  className?: string
  children: ReactNode
}

export function SubmitButton({ loading, disabled = false, className = 'w-full', children }: SubmitButtonProps) {
  return (
    <button type="submit" disabled={loading || disabled}
      className={`flex h-14 items-center justify-center gap-2 bg-accent px-8 font-semibold text-ink transition enabled:hover:brightness-105 enabled:active:scale-[0.97] disabled:cursor-not-allowed disabled:opacity-60 ${className}`}>
      {loading && <Loader2 className="animate-spin" size={18} />}
      {children}
    </button>
  )
}

export function ErrorBanner({ children }: { children: ReactNode }) {
  return <div role="alert" className="bg-red-500/15 px-3.5 py-3 text-sm text-red-200">{children}</div>
}

/** Sucesso na cor de destaque da casa, em tom translúcido, como o ErrorBanner. */
export function SuccessBanner({ children }: { children: ReactNode }) {
  return <div role="status" className="bg-accent/15 px-3.5 py-3 text-sm text-accent">{children}</div>
}
