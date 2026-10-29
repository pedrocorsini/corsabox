import { Link } from 'react-router'
import { Brand } from '../components/Brand'
import { useScrollEffects } from '../lib/motion'

export default function NotFoundPage() {
  useScrollEffects()

  return (
    <div data-field="ink" className="flex min-h-svh flex-col bg-bg">
      <header className="px-5 py-5 md:px-8 md:py-6">
        <Link to="/" className="inline-flex"><Brand /></Link>
      </header>

      <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col justify-center px-6 py-16 md:px-8">
        <p className="display-n text-[clamp(7rem,24vw,16rem)] text-accent">404</p>
        <h1 className="mt-8 text-3xl font-semibold sm:text-4xl">Esta página saiu de cena.</h1>
        <p className="mt-4 max-w-md text-base leading-relaxed text-muted">
          Não encontramos a página que você procura. O endereço pode estar incorreto ou a página pode ter sido removida.
        </p>
        <Link
          to="/"
          className="mt-8 inline-flex min-h-11 w-fit items-center justify-center rounded-md bg-accent px-5 py-3 text-sm font-semibold text-ink transition-colors hover:bg-accent/90"
        >
          Voltar ao início
        </Link>
      </main>
    </div>
  )
}
