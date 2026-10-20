import { Loader2 } from 'lucide-react'
import type { ComponentProps, ReactNode } from 'react'

export function Field({ label, error, ...props }: ComponentProps<'input'> & { label: string; error?: string }) {
  return (
    <div>
      <label htmlFor={props.id} className="mb-1.5 block text-sm font-medium text-muted">
        {label}
      </label>
      <input
        {...props}
        aria-invalid={!!error}
        className="w-full rounded-md border border-line bg-bg px-3.5 py-2.5 text-white outline-none transition placeholder:text-slate-500 focus:border-accent focus:ring-2 focus:ring-accent/30 aria-[invalid=true]:border-red-500"
      />
      {error && <p className="mt-1.5 text-sm text-red-400">{error}</p>}
    </div>
  )
}

export function SubmitButton({ loading, children }: { loading: boolean; children: ReactNode }) {
  return (
    <button
      type="submit"
      disabled={loading}
      className="flex w-full items-center justify-center gap-2 rounded-md bg-accent py-2.5 font-semibold text-black transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-60"
    >
      {loading && <Loader2 className="animate-spin" size={18} />}
      {children}
    </button>
  )
}

export function ErrorBanner({ children }: { children: ReactNode }) {
  return (
    <div role="alert" className="rounded-md border border-red-500/40 bg-red-500/10 px-3.5 py-2.5 text-sm text-red-300">
      {children}
    </div>
  )
}