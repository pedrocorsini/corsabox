import { Link, useLocation, useSearchParams } from 'react-router'
import { Loader2, Search } from 'lucide-react'
import { useMemo, useState } from 'react'
import { AppHeader } from '../components/AppHeader'
import { Poster } from '../components/Poster'
import { DropdownSelect } from '../components/DropdownSelect'
import { RatingMeter } from '../components/Rating'
import { Button } from '../components/ui'
import type { LibraryEntry, LibraryStatus } from '../lib/api'
import { KIND_LABEL, metaLine, titlePath } from '../lib/catalog'
import { formatDay } from '../lib/format'
import { useLibraryList } from '../lib/library'
import { useScrollEffects } from '../lib/motion'
import { useInfiniteScroll } from '../lib/useInfiniteScroll'

const TABS: { status: LibraryStatus; param: string; label: string }[] = [
  { status: 'watched', param: 'assistidos', label: 'Assistidos' },
  { status: 'watching', param: 'assistindo', label: 'Assistindo' },
  { status: 'watchlist', param: 'quero-ver', label: 'Quero ver' },
]

const EMPTY: Record<LibraryStatus, readonly [string, string]> = {
  watched: ['Nada registrado ainda.', 'Marque um título como assistido e ele aparece aqui, com nota, data e review.'],
  watching: ['Nada em andamento.', 'Marque um episódio na página de uma série ou anime e ele aparece aqui.'],
  watchlist: ['Sua fila está vazia.', 'Guarde o que você quer ver e passe para os assistidos quando terminar.'],
}

const stagger = (i: number) => ({ animationDelay: `${Math.min(i, 8) * 40}ms` })

function WatchedRow({ entry, index }: { entry: LibraryEntry; index: number }) {
  const m = entry.media
  const location = useLocation()
  const from = `${location.pathname}${location.search}`
  return (
    <li className="fade-in" style={stagger(index)}>
      <Link to={titlePath(m)} state={{ from }}
        className="group grid grid-cols-[4.5rem_1fr] gap-4 bg-surface p-3 transition active:scale-[0.99] md:grid-cols-[5.5rem_1fr] md:gap-5 md:p-4">
        <Poster item={m} />
        <div className="min-w-0 self-center">
          <p className="truncate text-lg font-semibold transition-colors group-hover:text-accent">{m.title}</p>
          <p className="text-[15px] text-muted">{metaLine(m)}</p>
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

function WatchingCard({ entry, index }: { entry: LibraryEntry; index: number }) {
  const m = entry.media
  const location = useLocation()
  const from = `${location.pathname}${location.search}`
  return (
    <li className="fade-in" style={stagger(index)}>
      <Link to={titlePath(m)} state={{ from }} className="block transition active:scale-[0.97]">
        <Poster item={m} />
        <p className="mt-2 truncate text-[15px]">{m.title}</p>
        <p className="text-sm text-muted">
          {entry.progress
            ? `T${entry.progress.season} E${entry.progress.episode}`
            : `${KIND_LABEL[m.kind]}${m.year ? ` · ${m.year}` : ''}`}
        </p>
      </Link>
      <Link to={titlePath(m)} state={{ from }} className="mt-1 inline-block text-sm font-medium text-accent hover:underline">
        Continuar
      </Link>
    </li>
  )
}

function WatchlistCard({ entry, index }: { entry: LibraryEntry; index: number }) {
  const m = entry.media
  const location = useLocation()
  const from = `${location.pathname}${location.search}`
  return (
    <li className="fade-in" style={stagger(index)}>
      <Link to={titlePath(m)} state={{ from }} className="block transition active:scale-[0.97]">
        <Poster item={m} />
        <p className="mt-2 truncate text-[15px]">{m.title}</p>
        <p className="text-sm text-muted">{KIND_LABEL[m.kind]}{m.year ? ` · ${m.year}` : ''}</p>
      </Link>
      <Link to={`${titlePath(m)}?assistir=1`} state={{ from }} className="mt-1 inline-block text-sm font-medium text-accent hover:underline">
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
  const watching = useLibraryList('watching')
  const watchlist = useLibraryList('watchlist')
  const lists = { watched, watching, watchlist }
  const list = lists[active.status]
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState<'recent' | 'oldest' | 'title'>('recent')
  const visibleItems = useMemo(() => {
    const normalized = query.trim().toLocaleLowerCase('pt-BR')
    return list.items
      .filter((entry) => !normalized || entry.media.title.toLocaleLowerCase('pt-BR').includes(normalized))
      .slice()
      .sort((a, b) => {
        if (sort === 'title') return a.media.title.localeCompare(b.media.title, 'pt-BR', { sensitivity: 'base' })
        const direction = sort === 'oldest' ? 1 : -1
        if (active.status === 'watched') {
          // Títulos sem data assistida ficam sempre no fim; não usar addedAt como substituto.
          if (!a.watchedOn && !b.watchedOn) return b.addedAt.localeCompare(a.addedAt)
          if (!a.watchedOn) return 1
          if (!b.watchedOn) return -1
          return direction * a.watchedOn.localeCompare(b.watchedOn)
            || b.addedAt.localeCompare(a.addedAt)
        }
        return direction * a.addedAt.localeCompare(b.addedAt)
      })
  }, [active.status, list.items, query, sort])
  const [emptyTitle, emptyText] = EMPTY[active.status]
  const sentinel = useInfiniteScroll(!list.loading && list.hasMore && !list.loadError, list.loadMore)

  return (
    <div className="min-h-svh bg-bg text-white">
      <AppHeader />
      <main className="mx-auto max-w-6xl px-5 pb-24 pt-12 md:px-8">
        <h1 data-reveal className="text-4xl font-semibold md:text-5xl">Biblioteca</h1>

        <div role="tablist" aria-label="Listas" className="mt-8 flex gap-4 overflow-x-auto sm:gap-8">
          {TABS.map((t) => {
            const total = lists[t.status].total
            const on = t.status === active.status
            return (
              <button key={t.status} role="tab" aria-selected={on}
                onClick={() => setParams({ aba: t.param }, { replace: true })}
                className={`shrink-0 border-b-2 pb-2 text-lg transition-colors ${on ? 'border-accent text-white' : 'border-transparent text-muted hover:text-white'}`}>
                {t.label}
                {total !== null && <span className="ml-1.5 text-[15px] tabular-nums text-muted">{total}</span>}
              </button>
            )
          })}
        </div>

        <div role="tabpanel" className="mt-8">
          <div className="relative z-30 mb-6 flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2 text-sm text-muted">
              <span>Ordenar</span>
              <div className="w-44">
                <DropdownSelect label="Ordenar títulos" value={sort} onChange={setSort}
                  options={[
                    { id: 'recent', label: 'Mais recentes' },
                    { id: 'oldest', label: 'Mais antigos' },
                    { id: 'title', label: 'A a Z' },
                  ]} />
              </div>
            </div>
            <label className="relative ml-auto block w-full sm:w-56">
              <Search size={16} aria-hidden="true" className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" />
              <input type="search" value={query} onChange={(e) => setQuery(e.target.value)}
                placeholder="Buscar título" aria-label="Buscar na lista"
                className="h-9 w-full bg-surface pl-9 pr-3 text-sm text-white outline-none placeholder:text-muted focus:outline-accent" />
            </label>
          </div>
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
              <p className="text-xl font-semibold">{emptyTitle}</p>
              <p className="text-muted">{emptyText}</p>
              <Link to="/catalogo" className="inline-block font-medium text-accent hover:underline">Buscar no catálogo</Link>
            </div>
          )}

          {list.items.length > 0 && (
            active.status === 'watched' ? (
              <ul className="space-y-2">
                {visibleItems.map((e, i) => <WatchedRow key={e.id} entry={e} index={i} />)}
              </ul>
            ) : (
              <ul className="grid grid-cols-2 gap-x-3 gap-y-6 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6">
                {visibleItems.map((e, i) =>
                  active.status === 'watching'
                    ? <WatchingCard key={e.id} entry={e} index={i} />
                    : <WatchlistCard key={e.id} entry={e} index={i} />)}
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
