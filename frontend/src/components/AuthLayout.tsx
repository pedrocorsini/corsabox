import { Link } from 'react-router'
import type { ReactNode } from 'react'
import { Brand } from './Brand'
import { Wordmark } from './Wordmark'

type Props = { title: string; subtitle: string; footer: ReactNode; children: ReactNode }

export function AuthLayout({ title, subtitle, footer, children }: Props) {
  return (
    <div className="grid min-h-svh lg:grid-cols-2">
      <aside className="hidden flex-col justify-between bg-surface p-10 lg:flex">
        <Link to="/"><Brand /></Link>
        <p className="max-w-md text-3xl font-semibold leading-snug">
          Registre o que você assistiu. Guarde o que quer ver.
        </p>
        <Wordmark className="text-accent" />
      </aside>

      <main className="flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-sm">
          <Link to="/" className="mb-8 inline-block lg:hidden"><Brand /></Link>
          <h1 className="text-2xl font-semibold">{title}</h1>
          <p className="mt-1.5 text-muted">{subtitle}</p>
          <div className="mt-8">{children}</div>
          <p className="mt-6 text-center text-sm text-muted">{footer}</p>
        </div>
      </main>
    </div>
  )
}
