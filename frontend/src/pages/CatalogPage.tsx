import { Link } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { Brand } from '../components/Brand'
import type { CatalogItem } from '../lib/catalog'
import { useScrollEffects } from '../lib/motion'

export default function CatalogPage() {
  const { user, logout } = useAuth()
  useScrollEffects()
  const items: CatalogItem[] = []

  return (
    <div className="min-h-svh bg-bg text-white">
      <header className="mx-auto flex max-w-6xl items-center justify-between px-5 py-5 md:px-8">
        <Link to="/catalogo"><Brand /></Link>
        <div className="flex items-center gap-5 text-[15px]">
          <span className="hidden text-muted sm:inline">@{user?.username}</span>
          <button onClick={logout} className="text-muted transition hover:text-white">Sair</button>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-5 pb-24 pt-12 md:px-8">
        <h1 data-reveal className="text-4xl font-semibold md:text-5xl">Catálogo</h1>
        <p data-reveal style={{ transitionDelay: '80ms' }} className="mt-3 max-w-[52ch] text-lg text-muted">
          Olá, {user?.firstName}. A busca de filmes, séries e animes chega em breve.
        </p>
        <input disabled aria-label="Buscar no catálogo" placeholder="Buscar filmes, séries e animes"
          className="mt-8 w-full max-w-xl bg-surface px-4 py-3 text-white placeholder:text-muted disabled:cursor-not-allowed disabled:opacity-70" />

        <ul className="mt-10 grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6">
          {items.length > 0
            ? items.map((it) => (
                <li key={it.id}>
                  <div className="aspect-[2/3] bg-surface">
                    {it.posterUrl && <img src={it.posterUrl} alt={it.title} className="h-full w-full object-cover" />}
                  </div>
                  <p className="mt-2 text-[15px]">{it.title}</p>
                </li>
              ))
            : Array.from({ length: 12 }, (_, i) => (
                <li key={i} data-reveal style={{ transitionDelay: `${(i % 6) * 60}ms` }}>
                  <div className="aspect-[2/3] bg-surface" />
                </li>
              ))}
        </ul>
      </main>
    </div>
  )
}
