import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { ChevronDown, Loader2, Search } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'
import { AppHeader } from '../components/AppHeader'
import { Poster } from '../components/Poster'
import { Button, ErrorBanner } from '../components/ui'
import { api, type BrowseCategory, type BrowseSort, type MediaGenre } from '../lib/api'
import { KIND_LABEL, PERIODS, itemKey, titlePath, type CatalogItem, type TrendingWindow } from '../lib/catalog'
import { useScrollEffects } from '../lib/motion'
import { useInfiniteScroll } from '../lib/useInfiniteScroll'

type Tab = 'trending' | BrowseCategory
type Result = { key: string; items: CatalogItem[]; page: number; more: boolean; error: boolean }

const TABS: { id: Tab; param: string; label: string }[] = [
  { id: 'trending', param: 'em-alta', label: 'Em alta' },
  { id: 'movies', param: 'filmes', label: 'Filmes' },
  { id: 'tv', param: 'series', label: 'Séries' },
  { id: 'anime', param: 'animes', label: 'Animes' },
]

const SORTS = [
  { id: 'popularity', label: 'Popularidade' },
  { id: 'title', label: 'A a Z' },
] as const

const TRENDING_SORTS = [
  { id: 'popularity', label: 'Em alta' },
  { id: 'title', label: 'A a Z' },
] as const

const merge = (a: CatalogItem[], b: CatalogItem[]) => {
  const seen = new Set(a.map(itemKey))
  return [...a, ...b.filter((i) => !seen.has(itemKey(i)))]
}

function fetchPage(token: string, tab: Tab, sort: BrowseSort, period: TrendingWindow, query: string, page: number, genreIds: number[]) {
  if (query) return api.catalog.search(token, query, page)
  if (tab === 'trending') return api.catalog.trending(period)
  return api.catalog.browse(token, tab, sort, page, genreIds)
}

export default function CatalogPage() {
  const { token } = useAuth()
  useScrollEffects()
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''
  const query = q.length >= 2 ? q : ''
  const tab = TABS.find((t) => t.param === params.get('aba'))?.id ?? 'trending'
  const sort: BrowseSort = params.get('ordem') === 'alfabetica' ? 'title' : 'popularity'
  const period: TrendingWindow = params.get('periodo') === 'semana' ? 'week' : 'day'
  const [genreIds, setGenreIds] = useState<number[]>([])
  const [genres, setGenres] = useState<MediaGenre[]>([])
  const apiSort: BrowseSort = tab === 'trending' ? 'popularity' : sort
  const key = query ? `q:${query}` : tab === 'trending' ? `trending:${period}` : `${tab}:${apiSort}:genres:${genreIds.join(',') || 'all'}`
  const paginated = query !== '' || tab !== 'trending'

  const [text, setText] = useState(q)
  const [result, setResult] = useState<Result | null>(null)
  const [attempt, setAttempt] = useState(0)
  const [loadingMore, setLoadingMore] = useState(false)
  const [loadError, setLoadError] = useState(false)
  const input = useRef<HTMLInputElement>(null)
  const busy = useRef(false)

  const genreFormat = tab === 'movies' ? 'movie' : 'tv'

  const sortedGenres = [...genres].sort((a, b) => a.name.localeCompare(b.name, 'pt-BR', { sensitivity: 'base' }))
  const genreFilterLabel = genreIds.length === 0
    ? 'Todas as categorias'
    : genreIds.length === 1
      ? genres.find((genre) => genre.id === genreIds[0])?.name ?? '1 categoria'
      : `${genreIds.length} categorias selecionadas`

  const toggleGenre = (id: number) => {
    setGenreIds((current) => current.includes(id)
      ? current.filter((genreId) => genreId !== id)
      : [...current, id].sort((a, b) => a - b))
  }

  useEffect(() => {
    if (!token || tab === 'trending' || query) {
      setGenres([])
      return
    }
    let cancelled = false
    setGenreIds([])
    api.catalog.genres(token, genreFormat)
      .then((items) => { if (!cancelled) setGenres(items) })
      .catch(() => { if (!cancelled) setGenres([]) })
    return () => { cancelled = true }
  }, [token, tab, query, genreFormat])

  const setParam = (name: string, value: string | null) =>
    setParams((p) => {
      const next = new URLSearchParams(p)
      if (value) next.set(name, value)
      else next.delete(name)
      return next
    }, { replace: true })

  // A busca vai para a URL com debounce, sem apagar a aba, o período e a ordem.
  useEffect(() => {
    const t = setTimeout(() => {
      const v = text.trim()
      if (v !== q) setParam('q', v || null)
    }, 350)
    return () => clearTimeout(t)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [text, q])

  useEffect(() => {
    if (!token) return
    let cancelled = false
    fetchPage(token, tab, apiSort, period, query, 1, genreIds)
      .then((items) => {
        if (cancelled) return
        setLoadError(false)
        setResult({ key, items: merge([], items), page: 1, more: paginated && items.length > 0, error: false })
      })
      .catch(() => {
        if (!cancelled) setResult({ key, items: [], page: 1, more: false, error: true })
      })
    return () => { cancelled = true }
  }, [token, tab, apiSort, period, query, genreIds, key, paginated, attempt])

  // A tecla "/" foca a busca.
  useEffect(() => {
    const on = (e: KeyboardEvent) => {
      const tag = document.activeElement?.tagName
      if (e.key !== '/' || tag === 'INPUT' || tag === 'TEXTAREA') return
      e.preventDefault()
      input.current?.focus()
    }
    window.addEventListener('keydown', on)
    return () => window.removeEventListener('keydown', on)
  }, [])

  const loading = !result || result.key !== key
  const base = result?.items ?? []
  const items = !query && tab === 'trending' && sort === 'title'
    ? [...base].sort((a, b) => a.title.localeCompare(b.title, 'pt-BR'))
    : base

  const retry = () => {
    setResult(null)
    setAttempt((a) => a + 1)
  }

  const loadMore = useCallback(async () => {
    if (!token || busy.current || !result?.more || result.key !== key) return
    busy.current = true
    setLoadingMore(true)
    setLoadError(false)
    const page = result.page + 1
    try {
      const next = await fetchPage(token, tab, apiSort, period, query, page, genreIds)
      setResult((prev) =>
        prev && prev.key === key ? { ...prev, items: merge(prev.items, next), page, more: next.length > 0 } : prev)
    } catch {
      setLoadError(true)
    } finally {
      busy.current = false
      setLoadingMore(false)
    }
  }, [token, tab, apiSort, period, query, genreIds, key, result])

  const sentinel = useInfiniteScroll(!loading && !!result?.more && !loadError, loadMore)

  return (
    <div className="min-h-svh bg-bg text-white">
      <AppHeader />

      <main className="mx-auto max-w-6xl px-5 pb-24 pt-12 md:px-8">
        <h1 data-reveal className="text-4xl font-semibold md:text-5xl">Catálogo</h1>
        <p data-reveal style={{ transitionDelay: '80ms' }} className="mt-3 max-w-[52ch] text-lg text-muted">
          Procure um filme, uma série ou um anime para registrar ou guardar.
        </p>

        <div className="relative mt-8 max-w-xl">
          <Search size={18} aria-hidden="true" className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-muted" />
          <input ref={input} type="search" value={text} onChange={(e) => setText(e.target.value)}
            aria-label="Buscar no catálogo" placeholder="Buscar filmes, séries e animes"
            className="w-full bg-surface py-3 pl-11 pr-4 text-white outline-2 outline-transparent transition placeholder:text-muted focus:outline-accent" />
        </div>

        {query ? (
          <h2 className="mt-12 text-xl font-semibold">Resultados para “{query}”</h2>
        ) : (
          <>
            <div role="tablist" aria-label="Seções do catálogo" className="mt-12 flex gap-6 overflow-x-auto sm:gap-8">
              {TABS.map((t) => {
                const on = t.id === tab
                return (
                  <button key={t.id} role="tab" aria-selected={on}
                    onClick={() => setParam('aba', t.id === 'trending' ? null : t.param)}
                    className={`shrink-0 border-b-2 pb-2 text-lg transition-colors ${on ? 'border-accent text-white' : 'border-transparent text-muted hover:text-white'}`}>
                    {t.label}
                  </button>
                )
              })}
            </div>

            <div className="relative z-30 mt-6 grid grid-cols-2 items-start gap-x-4 gap-y-3 sm:flex sm:flex-wrap sm:items-center sm:gap-x-8">
              {tab === 'trending' && (
                <div className="flex min-w-0 flex-col items-stretch gap-2 sm:flex-row sm:items-center">
                  <span className="mr-1 text-[15px] text-muted">Período</span>
                  <label className="sr-only" htmlFor="catalog-period">Período do catálogo em alta</label>
                  <select
                    id="catalog-period"
                    value={period}
                    onChange={(e) => setParam('periodo', e.target.value === 'day' ? null : 'semana')}
                    className="h-9 w-full max-w-full cursor-pointer bg-paper px-3 text-[15px] font-medium text-ink transition active:scale-[0.97] focus-visible:outline-2 focus-visible:outline-accent motion-reduce:transition-none sm:w-auto"
                  >
                    {PERIODS.map((option) => (
                      <option key={option.id} value={option.id}>{option.label}</option>
                    ))}
                  </select>
                </div>
              )}
              <div className="flex min-w-0 flex-col items-stretch gap-2 sm:flex-row sm:items-center">
                <span className="mr-1 text-[15px] text-muted">Ordenar</span>
                <label className="sr-only" htmlFor="catalog-sort">Ordenar catálogo</label>
                <select
                  id="catalog-sort"
                  value={sort}
                  onChange={(e) => setParam('ordem', e.target.value === 'popularity' ? null : 'alfabetica')}
                  className="h-9 w-full max-w-full cursor-pointer bg-paper px-3 text-[15px] font-medium text-ink transition active:scale-[0.97] focus-visible:outline-2 focus-visible:outline-accent motion-reduce:transition-none sm:w-auto"
                >
                  {(tab === 'trending' ? TRENDING_SORTS : SORTS).map((option) => (
                    <option key={option.id} value={option.id}>{option.label}</option>
                  ))}
                </select>
              </div>
              {tab !== 'trending' && (
                <div className="fade-in flex min-w-0 flex-col gap-2 sm:ml-auto sm:w-52">
                  <div className="flex flex-wrap items-center justify-between gap-x-2 gap-y-1">
                    <span className="text-[15px] text-muted">Filtro</span>
                    <button
                      type="button"
                      onClick={() => setGenreIds([])}
                      disabled={genreIds.length === 0}
                      className="text-sm text-accent transition-colors hover:underline disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:no-underline"
                    >
                      Limpar categorias
                    </button>
                  </div>
                  <details className="group relative w-full">
                    <summary className={`flex h-9 w-full cursor-pointer list-none items-center justify-between gap-3 px-3 text-[15px] font-medium transition active:scale-[0.97] focus-visible:outline-2 focus-visible:outline-accent motion-reduce:transition-none ${
                      genreIds.length > 0 ? 'bg-paper text-ink' : 'bg-surface text-muted hover:text-white'
                    }`}>
                      <span className="truncate">{genreFilterLabel}</span>
                      <ChevronDown size={16} aria-hidden="true" className="shrink-0 transition-transform duration-200 group-open:rotate-180 motion-reduce:transition-none" />
                    </summary>
                    <div role="group" aria-label="Categorias disponíveis" className="absolute right-0 top-full z-50 mt-2 max-h-72 w-full min-w-56 max-w-[min(90vw,20rem)] overflow-y-auto bg-surface p-2 shadow-xl">
                      <button type="button" onClick={() => setGenreIds([])} className="mb-1 w-full px-3 py-2 text-left text-sm text-muted transition-colors hover:bg-line hover:text-white">
                        Todas as categorias
                      </button>
                      {sortedGenres.map((genre) => (
                        <label key={genre.id} className="flex cursor-pointer items-center gap-3 px-3 py-2 text-sm text-white transition-colors hover:bg-line">
                          <input
                            type="checkbox"
                            checked={genreIds.includes(genre.id)}
                            onChange={() => toggleGenre(genre.id)}
                            className="accent-accent"
                          />
                          <span>{genre.name}</span>
                        </label>
                      ))}
                    </div>
                  </details>
                </div>
              )}
            </div>
          </>
        )}

        {result?.error && !loading && (
          <div className="mt-6 max-w-md space-y-4">
            <ErrorBanner>O catálogo está indisponível agora.</ErrorBanner>
            <Button variant="primary" onClick={retry}>Tentar de novo</Button>
          </div>
        )}

        {!loading && !result?.error && items.length === 0 && (
          <p className="mt-6 text-muted">Nada encontrado. Tente outro nome ou outra grafia.</p>
        )}

        <ul aria-busy={loading}
          className={`mt-6 grid grid-cols-2 gap-x-3 gap-y-6 transition-opacity duration-150 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 ${loading && items.length > 0 ? 'opacity-50' : ''}`}>
          {loading && items.length === 0 &&
            Array.from({ length: 12 }, (_, i) => <li key={i} className="aspect-[2/3] bg-surface" />)}
          {items.map((it, i) => (
            <li key={itemKey(it)} className="fade-in" style={{ animationDelay: `${Math.min(i % 20, 8) * 40}ms` }}>
              <Link to={titlePath(it)} className="block transition active:scale-[0.97]">
                <Poster item={it} />
                <p className="mt-2 truncate text-[15px]">{it.title}</p>
                <p className="text-sm text-muted">{KIND_LABEL[it.kind]}{it.year ? ` · ${it.year}` : ''}</p>
              </Link>
            </li>
          ))}
        </ul>

        {result?.more && <div ref={sentinel} aria-hidden="true" className="h-px" />}

        <div className="mt-10 flex min-h-10 justify-center">
          {loadingMore && <Loader2 className="animate-spin text-muted" aria-label="Carregando mais títulos" />}
          {loadError && (
            <div className="space-y-3 text-center">
              <p className="text-muted">Não foi possível carregar mais.</p>
              <Button onClick={loadMore}>Tentar de novo</Button>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
