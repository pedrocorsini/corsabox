import { Link, useSearchParams } from 'react-router'
import { AppHeader } from '../components/AppHeader'
import { Poster } from '../components/Poster'
import { RatingMeter } from '../components/Rating'
import { Button } from '../components/ui'
import type { LibraryEntry, LibraryStatus } from '../lib/api'
import { KIND_LABEL, titlePath } from '../lib/catalog'
import { formatDay } from '../lib/format'
import { useLibraryList } from '../lib/library'
import { useScrollEffects } from '../lib/motion'
import { useInfiniteScroll } from '../lib/useInfiniteScroll'
import { Loader2 } from 'lucide-react'

const TABS: { status: LibraryStatus; param: string; label: string }[] = [
  { status: 'watched', param: 'assistidos', label: 'Assistidos' },
  { status: 'watchlist', param: 'quero-ver', label: 'Quero ver' },
]

const stagger = (i: number) => ({ animationDelay: `${Math.min(i, 8) * 40}ms` })

function WatchedRow({ entry, index }: { entry: LibraryEntry; index: number }) {
  const m = entry.media
  return (
    <li className="fade-in" style={stagger(index)}>
      <Link to={titlePath(m)}
        className="group grid grid-cols-[4.5rem_1fr] gap-4 bg-surface p-3 transition active:scale-[0.99] md:grid-cols-[5.5rem_1fr] md:gap-5 md:p-4">
        <Poster item={m} />
        <div className="min-w-0 self-center">
          <p className="truncate text-lg font-semibold transition-colors group-hover:text-accent">{m.title}</p>
          <p className="text-[15px] text-muted">{KIND_LABEL[m.kind]}{m.year ? ` · ${m.year}` : ''}</p>
          <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1">
            {entry.rating !== undefined
              ? <RatingMeter value={entry.rating} size="sm" />
              : <span className="text-[15px] text-muted">Sem nota</span>}
            {entry.watchedOn && <span className="text-[15px] text-muted">{formatDay(entry.watchedOn)}</span>}
          </div>
          {entry.review && <p className="mt-3 line-clamp-2 max-w-[70ch] text-[15px] text-muted">{entry.review}</p>}
        </div>
      </Link>
    </li>
  )
}

function WatchlistCard({ entry, index }: { entry: LibraryEntry; index: number }) {
  const m = entry.media
  return (
    <li className="fade-in" style={stagger(index)}>
      <Link to={titlePath(m)} className="block transition active:scale-[0.97]">
        <Poster item={m} />
        <p className="mt-2 truncate text-[15px]">{m.title}</p>
        <p className="text-sm text-muted">{KIND_LABEL[m.kind]}{m.year ? ` · ${m.year}` : ''}</p>
      </Link>
      <Link to={`${titlePath(m)}?assistir=1`} className="mt-1 inline-block text-sm font-medium text-accent hover:underline">
        Marcar como Assistido
      </Link>
    </li>
  )
}

export default function LibraryPage() {
  useScrollEffects()
  const [params, setParams] = useSearchParams()
  const active = TABS.find((t) => t.param === params.get('aba')) ?? TABS[0]
  const watched = useLibraryList('watched')
  const watchlist = useLibraryList('watchlist')
  const lists = { watched, watchlist }
  const list = lists[active.status]
  const sentinel = useInfiniteScroll(!list.loading && list.hasMore && !list.loadError, list.loadMore)

  return (
    <div className="min-h-svh bg-bg text-white">
      <AppHeader />
      <main className="mx-auto max-w-6xl px-5 pb-24 pt-12 md:px-8">
        <h1 data-reveal className="text-4xl font-semibold md:text-5xl">Biblioteca</h1>

        <div role="tablist" aria-label="Listas" className="mt-8 flex gap-8">
          {TABS.map((t) => {
            const total = lists[t.status].total
            const on = t.status === active.status
            return (
              <button key={t.status} role="tab" aria-selected={on}
                onClick={() => setParams({ aba: t.param }, { replace: true })}
                className={`border-b-2 pb-2 text-lg transition-colors ${on ? 'border-accent text-white' : 'border-transparent text-muted hover:text-white'}`}>
                {t.label}
                {total !== null && <span className="ml-2 text-[15px] tabular-nums text-muted">{total}</span>}
              </button>
            )
          })}
        </div>

        <div role="tabpanel" className="mt-8">
          {list.loading && (
            <ul aria-busy="true" className="space-y-2">
              {Array.from({ length: 5 }, (_, i) => <li key={i} className="h-28 bg-surface" />)}
            </ul>
          )}

          {!list.loading && list.error && list.items.length === 0 && (
            <div className="max-w-md space-y-4">
              <p className="text-xl font-semibold">Não foi possível carregar a lista.</p>
              <Button variant="primary" onClick={list.retry}>Tentar de novo</Button>
            </div>
          )}

          {!list.loading && !list.error && list.items.length === 0 && (
            <div className="max-w-md space-y-3">
              <p className="text-xl font-semibold">
                {active.status === 'watched' ? 'Nada registrado ainda.' : 'Sua fila está vazia.'}
              </p>
              <p className="text-muted">
                {active.status === 'watched'
                  ? 'Marque um título como assistido e ele aparece aqui, com nota, data e review.'
                  : 'Guarde o que você quer ver e passe para os assistidos quando terminar.'}
              </p>
              <Link to="/catalogo" className="inline-block font-medium text-accent hover:underline">Buscar no catálogo</Link>
            </div>
          )}

          {list.items.length > 0 && (
            active.status === 'watched' ? (
              <ul className="space-y-2">
                {list.items.map((e, i) => <WatchedRow key={e.id} entry={e} index={i} />)}
              </ul>
            ) : (
              <ul className="grid grid-cols-2 gap-x-3 gap-y-6 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6">
                {list.items.map((e, i) => <WatchlistCard key={e.id} entry={e} index={i} />)}
              </ul>
            )
          )}

          {list.hasMore && <div ref={sentinel} aria-hidden="true" className="h-px" />}

          <div className="mt-10 flex min-h-10 justify-center">
            {list.loadingMore && <Loader2 className="animate-spin text-muted" aria-label="Carregando mais títulos" />}
            {list.loadError && (
              <div className="space-y-3 text-center">
                <p className="text-muted">Não foi possível carregar mais.</p>
                <Button onClick={list.loadMore}>Tentar de novo</Button>
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  )
}