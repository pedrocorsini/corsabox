import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { Loader2, Search } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'
import { AppHeader } from '../components/AppHeader'
import { Poster } from '../components/Poster'
import { Button, ErrorBanner } from '../components/ui'
import { api, type BrowseCategory, type BrowseSort } from '../lib/api'
import { KIND_LABEL, itemKey, titlePath, type CatalogItem } from '../lib/catalog'
import { useScrollEffects } from '../lib/motion'

type Tab = 'trending' | BrowseCategory
type Result = { key: string; items: CatalogItem[]; page: number; more: boolean; error: boolean }

const TABS: { id: Tab; param: string; label: string }[] = [
  { id: 'trending', param: 'em-alta', label: 'Em alta' },
  { id: 'movies', param: 'filmes', label: 'Filmes' },
  { id: 'tv', param: 'series', label: 'Séries' },
  { id: 'anime', param: 'animes', label: 'Animes' },
]

const SORTS: { id: BrowseSort; param: string; label: string }[] = [
  { id: 'popularity', param: 'popularidade', label: 'Popularidade' },
  { id: 'title', param: 'alfabetica', label: 'A a Z' },
]

const merge = (a: CatalogItem[], b: CatalogItem[]) => {
  const seen = new Set(a.map(itemKey))
  return [...a, ...b.filter((i) => !seen.has(itemKey(i)))]
}

function fetchPage(token: string, tab: Tab, sort: BrowseSort, query: string, page: number) {
  if (query) return api.catalog.search(token, query, page)
  if (tab === 'trending') return api.catalog.trending()
  return api.catalog.browse(token, tab, sort, page)
}

export default function CatalogPage() {
  const { token } = useAuth()
  useScrollEffects()
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''
  const query = q.length >= 2 ? q : ''
  const tab = TABS.find((t) => t.param === params.get('aba'))?.id ?? 'trending'
  const sort = SORTS.find((s) => s.param === params.get('ordem'))?.id ?? 'popularity'
  const key = query ? `q:${query}` : tab === 'trending' ? 'trending' : `${tab}:${sort}`
  const paginated = query !== '' || tab !== 'trending'

  const [text, setText] = useState(q)
  const [result, setResult] = useState<Result | null>(null)
  const [attempt, setAttempt] = useState(0)
  const [loadingMore, setLoadingMore] = useState(false)
  const [loadError, setLoadError] = useState(false)
  const input = useRef<HTMLInputElement>(null)
  const sentinel = useRef<HTMLDivElement>(null)
  const busy = useRef(false)

  const setParam = (name: string, value: string | null) =>
    setParams((p) => {
      const next = new URLSearchParams(p)
      if (value) next.set(name, value)
      else next.delete(name)
      return next
    }, { replace: true })

  // A busca vai para a URL com debounce, sem apagar a aba e a ordem.
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
    fetchPage(token, tab, sort, query, 1)
      .then((items) => {
        if (cancelled) return
        setLoadError(false)
        setResult({ key, items: merge([], items), page: 1, more: paginated && items.length > 0, error: false })
      })
      .catch(() => {
        if (!cancelled) setResult({ key, items: [], page: 1, more: false, error: true })
      })
    return () => { cancelled = true }
  }, [token, tab, sort, query, key, paginated, attempt])

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
  const items = result?.items ?? []

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
      const next = await fetchPage(token, tab, sort, query, page)
      setResult((prev) =>
        prev && prev.key === key ? { ...prev, items: merge(prev.items, next), page, more: next.length > 0 } : prev)
    } catch {
      setLoadError(true)
    } finally {
      busy.current = false
      setLoadingMore(false)
    }
  }, [token, tab, sort, query, key, result])

  // Scroll infinito: quando o fim da lista chega perto da tela, busca a próxima página.
  const canLoadMore = !loading && !!result?.more && !loadError
  useEffect(() => {
    const el = sentinel.current
    if (!el || !canLoadMore) return
    const io = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting) loadMore()
    }, { rootMargin: '600px 0px' })
    io.observe(el)
    return () => io.disconnect()
  }, [canLoadMore, loadMore])

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
          <div className="mt-12 flex flex-wrap items-end justify-between gap-x-8 gap-y-4">
            <div role="tablist" aria-label="Seções do catálogo" className="flex gap-6 overflow-x-auto sm:gap-8">
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

            {tab !== 'trending' && (
              <div role="group" aria-label="Ordenar por" className="flex items-center gap-2">
                <span className="mr-1 text-[15px] text-muted">Ordenar</span>
                {SORTS.map((s) => (
                  <button key={s.id} aria-pressed={s.id === sort}
                    onClick={() => setParam('ordem', s.id === 'popularity' ? null : s.param)}
                    className={`h-9 px-3 text-[15px] font-medium transition active:scale-[0.97] ${
                      s.id === sort ? 'bg-paper text-ink' : 'bg-surface text-muted hover:text-white'}`}>
                    {s.label}
                  </button>
                ))}
              </div>
            )}
          </div>
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