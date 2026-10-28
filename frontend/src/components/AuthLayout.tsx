import { Link } from 'react-router'
import type { ReactNode } from 'react'
import { Brand } from './Brand'
import { Wordmark } from './Wordmark'
import { useScrollEffects } from '../lib/motion'

type Props = { title: string; subtitle: string; footer: ReactNode; children: ReactNode }

export function AuthLayout({ title, subtitle, footer, children }: Props) {
  useScrollEffects()

  return (
    <div className="relative grid min-h-svh lg:grid-cols-2">
      <header className="absolute inset-x-0 top-0 z-10 px-5 py-5 md:px-8 md:py-6">
        <div className="flex h-11 items-center md:h-12">
          <Link to="/"><Brand /></Link>
        </div>
      </header>

      <aside className="hidden flex-col bg-surface px-8 pb-10 pt-24 lg:flex">
        <div className="flex flex-1 items-center py-12">
          <p data-reveal className="max-w-md text-3xl font-semibold leading-snug">
            Registre o que você assistiu. Guarde o que quer ver.
          </p>
        </div>
        <div data-reveal style={{ transitionDelay: '160ms' }}>
          <Wordmark className="text-accent" />
        </div>
      </aside>

      <main className="flex items-center justify-center px-6 pb-12 pt-28 lg:py-24">
        <div className="w-full max-w-sm">
          <h1 className="mask text-2xl font-semibold"><span key={title}>{title}</span></h1>
          <p data-reveal style={{ transitionDelay: '80ms' }} className="mt-1.5 text-muted">{subtitle}</p>
          <div className="mt-8">{children}</div>
          <p data-reveal style={{ transitionDelay: '160ms' }} className="mt-6 text-center text-sm text-muted">{footer}</p>
        </div>
      </main>
    </div>
  )
}
