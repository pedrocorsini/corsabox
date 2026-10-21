import { Loader2 } from 'lucide-react'
import type { ComponentProps, ReactNode } from 'react'

export function Field({ label, error, ...props }: ComponentProps<'input'> & { label: string; error?: string }) {
  return (
    <div>
      <label htmlFor={props.id} className="mb-1.5 block text-sm font-medium text-muted">{label}</label>
      <input
        {...props}
        aria-invalid={!!error}
        className="w-full bg-surface px-3.5 py-3 text-white outline-2 outline-transparent transition placeholder:text-muted focus:outline-accent aria-[invalid=true]:outline-red-400"
      />
      {error && <p className="mt-1.5 text-sm text-red-300">{error}</p>}
    </div>
  )
}

export function SubmitButton({ loading, children }: { loading: boolean; children: ReactNode }) {
  return (
    <button type="submit" disabled={loading}
      className="flex h-14 w-full items-center justify-center gap-2 bg-accent font-semibold text-ink transition hover:brightness-105 active:translate-y-px disabled:cursor-not-allowed disabled:opacity-60">
      {loading && <Loader2 className="animate-spin" size={18} />}
      {children}
    </button>
  )
}

export function ErrorBanner({ children }: { children: ReactNode }) {
  return <div role="alert" className="bg-red-500/15 px-3.5 py-3 text-sm text-red-200">{children}</div>
}
