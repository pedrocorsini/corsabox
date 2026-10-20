import { Clapperboard } from 'lucide-react'
import type { ReactNode } from 'react'

function Brand() {
  return (
    <div className="relative flex items-center gap-2 text-xl font-bold">
      <Clapperboard className="text-accent" size={26} />
      CorsaBox
    </div>
  )
}

type Props = { title: string; subtitle: string; footer: ReactNode; children: ReactNode }

export function AuthLayout({ title, subtitle, footer, children }: Props) {
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <aside className="relative hidden flex-col justify-between overflow-hidden bg-surface p-12 lg:flex">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_30%_20%,rgba(0,224,84,0.18),transparent_55%)]" />
        <Brand />
        <div className="relative max-w-md">
          <p className="text-3xl font-semibold leading-snug">
            Registre o que você assistiu. Guarde o que quer ver.
          </p>
          <p className="mt-4 text-muted">Seu diário de filmes, séries e animes em um só lugar.</p>
        </div>
        <p className="relative text-sm text-muted">© CorsaBox</p>
      </aside>

      <main className="flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-sm">
          <div className="mb-8 lg:hidden">
            <Brand />
          </div>
          <h1 className="text-2xl font-semibold">{title}</h1>
          <p className="mt-1.5 text-muted">{subtitle}</p>
          <div className="mt-8">{children}</div>
          <p className="mt-6 text-center text-sm text-muted">{footer}</p>
        </div>
      </main>
    </div>
  )
}