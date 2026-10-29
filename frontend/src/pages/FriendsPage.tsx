import { useEffect, useRef, useState } from 'react'
import { Loader2, Search } from 'lucide-react'
import { AppHeader } from '../components/AppHeader'
import { FriendshipActions } from '../components/FriendshipActions'
import { UserLink } from '../components/UserLink'
import { Button } from '../components/ui'
import { useAuth } from '../auth/useAuth'
import { api, pageInfo, type FriendList, type Person } from '../lib/api'
import { useScrollEffects } from '../lib/motion'

const tabs: { id: FriendList; label: string }[] = [
  { id: 'friends', label: 'Meus amigos' }, { id: 'received', label: 'Recebidas' }, { id: 'sent', label: 'Enviadas' },
]
const empty: Record<FriendList, string> = {
  friends: 'Sua lista de amigos está vazia. Busque alguém para enviar uma solicitação.',
  received: 'Nenhuma solicitação recebida.', sent: 'Nenhuma solicitação enviada.',
}

function PeopleResults({ tab, query, onRelationshipChange }: { tab: FriendList; query: string; onRelationshipChange: () => void }) {
  const { token } = useAuth()
  const [items, setItems] = useState<Person[]>([])
  const [page, setPage] = useState(0)
  const [more, setMore] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<'initial' | 'more' | null>(null)
  const [attempt, setAttempt] = useState(0)
  const busy = useRef(false)
  const generation = useRef(0)

  useEffect(() => {
    if (!token) return
    const current = ++generation.current
    const request = query ? api.people.search(token, query) : api.people.list(token, tab)
    request.then((data) => {
      if (current !== generation.current) return
      setItems(data.content)
      setPage(0)
      setMore(pageInfo(data).hasMore)
      setError(null)
    }).catch(() => {
      if (current === generation.current) setError('initial')
    }).finally(() => {
      if (current === generation.current) setLoading(false)
    })
    return () => { generation.current = current + 1 }
  }, [token, tab, query, attempt])

  async function loadMore() {
    if (!token || busy.current || !more) return
    const current = generation.current
    busy.current = true
    setLoading(true)
    setError(null)
    try {
      const data = query ? await api.people.search(token, query, page + 1) : await api.people.list(token, tab, page + 1)
      if (current !== generation.current) return
      setItems((previous) => {
        const ids = new Set(previous.map((person) => person.user.id))
        return [...previous, ...data.content.filter((person) => !ids.has(person.user.id))]
      })
      setPage(page + 1)
      setMore(pageInfo(data).hasMore)
    } catch {
      if (current === generation.current) setError('more')
    } finally {
      busy.current = false
      if (current === generation.current) setLoading(false)
    }
  }

  const reload = () => { generation.current++; setLoading(true); setError(null); setAttempt((value) => value + 1) }

  return (
    <div aria-busy={loading}>
      {loading && items.length === 0 && <p role="status" className="flex items-center gap-2 text-muted"><Loader2 className="animate-spin" size={18} aria-hidden="true" />Carregando pessoas…</p>}
      {!loading && !error && items.length === 0 && <p className="bg-surface p-6 text-muted">{query ? 'Nenhuma pessoa encontrada. Tente outro nome ou @username.' : empty[tab]}</p>}
      <ul className="space-y-3">
        {items.map((person) => <li key={person.user.id} className="fade-in flex flex-wrap items-center justify-between gap-5 bg-surface p-5">
          <div className="min-w-0">
            <UserLink user={person.user} />
            <p className="mt-2 break-words text-sm text-muted">{person.user.firstName} {person.user.lastName}</p>
          </div>
          <FriendshipActions userId={person.user.id} relationship={person.relationship} onChange={(relationship) => {
            setItems((previous) => previous.map((item) => item.user.id === person.user.id ? { ...item, relationship } : item))
            reload()
            onRelationshipChange()
          }} />
        </li>)}
      </ul>
      {error && <div role="alert" className="mt-5 space-y-3">
        <p className="text-muted">Não foi possível carregar {items.length ? 'mais pessoas' : 'as pessoas'}.</p>
        <Button onClick={error === 'more' ? () => void loadMore() : reload}>Tentar de novo</Button>
      </div>}
      {more && !error && <Button className="mt-5" loading={loading} onClick={() => void loadMore()}>Carregar mais pessoas</Button>}
    </div>
  )
}

export default function FriendsPage() {
  useScrollEffects()
  const { token } = useAuth()
  const [tab, setTab] = useState<FriendList>('friends')
  const [input, setInput] = useState('')
  const [query, setQuery] = useState('')
  const [searchError, setSearchError] = useState(false)
  const [counts, setCounts] = useState<Record<FriendList, number | null>>({ friends: null, received: null, sent: null })
  const [countsAttempt, setCountsAttempt] = useState(0)

  useEffect(() => {
    if (!token) return
    let cancelled = false
    // Existing page metadata provides exact totals; only one person per list is needed.
    Promise.allSettled(tabs.map((item) => api.people.list(token, item.id, 0, 1))).then((results) => {
      if (cancelled) return
      setCounts(Object.fromEntries(results.map((result, index) => [tabs[index].id,
        result.status === 'fulfilled' ? pageInfo(result.value).total : null])) as Record<FriendList, number | null>)
    })
    return () => { cancelled = true }
  }, [token, countsAttempt])

  return (
    <div className="min-h-svh bg-bg text-white">
      <AppHeader />
      <main className="mx-auto w-full max-w-5xl px-5 pb-24 pt-8 sm:px-8 sm:pt-12">
        <h1 data-reveal className="text-4xl font-semibold md:text-5xl">Amigos</h1>
        <p data-reveal style={{ transitionDelay: '80ms' }} className="mt-4 text-muted">Encontre pessoas e acompanhe suas próximas descobertas.</p>
        <div className="mt-8 w-full">
          <form onSubmit={(event) => {
            event.preventDefault()
            const normalized = input.trim().replace(/^@/, '')
            if (normalized.length < 2) { setSearchError(true); return }
            setSearchError(false); setQuery(normalized)
          }} className="space-y-2">
            <label htmlFor="people-search" className="block text-sm font-medium">Buscar por nome ou @username</label>
            <div className="flex gap-2">
              <input id="people-search" type="search" value={input} maxLength={100} onChange={(event) => { setInput(event.target.value); setSearchError(false) }}
                aria-invalid={searchError} aria-describedby={searchError ? 'people-search-error' : undefined}
                className="min-w-0 flex-1 bg-surface px-4 py-3 outline-2 outline-transparent focus:outline-accent" placeholder="Nome ou @username" />
              <Button type="submit" aria-label="Buscar pessoas"><Search size={18} aria-hidden="true" /><span className="hidden sm:inline">Buscar</span></Button>
            </div>
            {searchError && <p id="people-search-error" role="alert" className="text-sm text-red-300">Digite pelo menos 2 caracteres.</p>}
          </form>
          <div className="mt-8 flex flex-wrap gap-x-6 gap-y-3" role="tablist" aria-label="Amizades">
            {tabs.map((item) => <button key={item.id} type="button" role="tab" aria-selected={!query && tab === item.id}
              onClick={() => { setTab(item.id); setQuery(''); setInput(''); setSearchError(false) }}
              className={`border-b-2 pb-2 transition-colors ${!query && tab === item.id ? 'border-accent text-white' : 'border-transparent text-muted hover:text-white'}`}>
              {item.label}
              {(counts[item.id] ?? 0) > 0 && <span className="ml-1.5 text-sm tabular-nums text-muted">{counts[item.id]}</span>}
            </button>)}
          </div>
          {query && <div className="mt-6 flex flex-wrap items-center justify-between gap-3">
            <h2 className="min-w-0 break-words font-semibold">Resultados para “{query}”</h2>
            <button type="button" className="min-h-11 text-sm text-accent hover:underline" onClick={() => { setQuery(''); setInput('') }}>Limpar busca</button>
          </div>}
          <div className="mt-6"><PeopleResults key={`${tab}:${query}`} tab={tab} query={query}
            onRelationshipChange={() => setCountsAttempt((value) => value + 1)} /></div>
        </div>
      </main>
    </div>
  )
}
