import { useEffect, useRef, useState } from 'react'
import { Check, Pencil, Plus } from 'lucide-react'
import { useAuth } from '../auth/useAuth'
import { api, ApiError, type Favorite, type ProfileCustomization } from '../lib/api'
import { itemKey, KIND_LABEL } from '../lib/catalog'
import { setProfile } from '../lib/profile'
import { Poster } from './Poster'
import { ProfileFavorites } from './ProfileFavorites'
import { Button, ErrorBanner, SubmitButton, SuccessBanner } from './ui'

export function ProfileFavoritesSection({ initial, editable }: { initial: ProfileCustomization; editable: boolean }) {
  const [editing, setEditing] = useState(false)
  const [saved, setSaved] = useState(false)
  return <section aria-labelledby="favorites-heading">
    <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
      <h2 id="favorites-heading" className="text-xl font-semibold">Favoritos</h2>
      {editable && !editing && <button type="button" onClick={() => { setSaved(false); setEditing(true) }}
        className="inline-flex min-h-11 items-center gap-2 px-2 text-sm text-muted transition-colors hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent motion-safe:active:scale-[0.97]">
        <Pencil size={14} aria-hidden="true" />Editar favoritos
      </button>}
    </div>
    {editing ? <FavoritesEditor initial={initial} onCancel={() => setEditing(false)} onSaved={() => { setEditing(false); setSaved(true) }} />
      : <ProfileFavorites entries={initial.favorites} />}
    {saved && <div className="mt-4"><SuccessBanner>Favoritos atualizados.</SuccessBanner></div>}
  </section>
}

function FavoritesEditor({ initial, onCancel, onSaved }: { initial: ProfileCustomization; onCancel: () => void; onSaved: () => void }) {
  const { token, user } = useAuth()
  const [selected, setSelected] = useState(initial.favorites)
  const [kind, setKind] = useState<'movie' | 'tv' | 'anime'>('movie')
  const [query, setQuery] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [searchOpen, setSearchOpen] = useState(false)
  const searchInput = useRef<HTMLInputElement>(null)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => {
    const timer = window.setTimeout(() => setSearchQuery(query), 250)
    return () => window.clearTimeout(timer)
  }, [query])
  const dirty = selected.length !== initial.favorites.length || selected.some(entry => !initial.favorites.some(saved => itemKey(saved.media) === itemKey(entry.media)))
  const count = selected.filter(e => e.media.kind === kind).length
  const searchPlaceholder = { movie: 'Buscar filme', tv: 'Buscar série', anime: 'Buscar anime' }[kind]
  const closeSearch = () => { searchInput.current?.focus(); setSearchOpen(false) }

  function toggle(entry: Favorite) {
    setSelected(current => {
      if (current.some(value => itemKey(value.media) === itemKey(entry.media))) return current.filter(value => itemKey(value.media) !== itemKey(entry.media))
      if (current.filter(value => value.media.kind === entry.media.kind).length >= 5) return current
      const next = [...current, entry]
      const original = initial.favorites.filter(saved => next.some(value => itemKey(value.media) === itemKey(saved.media)))
      const added = next.filter(value => !initial.favorites.some(saved => itemKey(saved.media) === itemKey(value.media)))
      return [...original, ...added]
    })
  }

  return <form className="min-w-0 space-y-6" onSubmit={async event => {
    event.preventDefault()
    if (!token || !user || saving || !dirty) return
    setSaving(true); setError('')
    try {
      const result = await api.profiles.saveFavorites(token, selected.map(e => ({ format: e.media.format, id: e.media.id })))
      setProfile(user.id, result); onSaved()
    } catch (e) { setError(e instanceof ApiError ? e.message : 'Não foi possível salvar. Tente novamente.') }
    finally { setSaving(false) }
  }}>
    <fieldset disabled={saving} className="min-w-0">
      <div>
        <p className="mt-1 text-sm text-muted">Até 5 filmes, 5 séries e 5 animes do catálogo.</p>
        <div className="mt-4"><ProfileFavorites entries={selected} onRemove={toggle} kind={kind} onKindChange={setKind} /></div>
        <div className="mt-5" onKeyDown={event => { if (event.key === 'Escape' && searchOpen) { event.preventDefault(); closeSearch() } }}>
          <label htmlFor="favorite-search" className="mb-2 block text-sm font-medium">Buscar no catálogo</label>
          <div className="flex gap-2">
            <input ref={searchInput} id="favorite-search" type="search" maxLength={100} placeholder={searchPlaceholder} value={query}
              onFocus={() => setSearchOpen(true)} onChange={e => { setQuery(e.target.value); setSearchOpen(true) }}
              aria-expanded={searchOpen} aria-controls="favorite-results"
              className="min-h-12 min-w-0 flex-1 bg-bg px-3 text-base outline-2 outline-transparent focus:outline-accent" />
            {searchOpen && <button type="button" onClick={closeSearch} className="min-h-11 px-2 text-sm text-muted hover:text-white">Fechar busca</button>}
          </div>
          <p role="status" className="my-3 text-sm text-muted">{count}/5 selecionados nesta categoria</p>
          {searchOpen && <div id="favorite-results">
            {query !== searchQuery ? <p role="status" className="text-sm text-muted">Buscando títulos…</p>
              : <FavoriteCandidates key={`${kind}:${searchQuery}`} kind={kind} query={searchQuery} selected={selected} onToggle={toggle} />}
          </div>}
        </div>
      </div>
    </fieldset>
    {error && <ErrorBanner>{error}</ErrorBanner>}
    <div className="flex flex-wrap gap-3">
      <SubmitButton loading={saving} disabled={!dirty} className="w-full sm:w-auto">Salvar favoritos</SubmitButton>
      <Button disabled={saving} onClick={onCancel}>Descartar edição</Button>
    </div>
  </form>
}

function FavoriteCandidates({ kind, query, selected, onToggle }: {
  kind: 'movie' | 'tv' | 'anime'; query: string; selected: Favorite[]; onToggle: (entry: Favorite) => void
}) {
  const { token } = useAuth()
  const [entries, setEntries] = useState<Favorite[]>([])
  const [page, setPage] = useState(1)
  const [more, setMore] = useState(false)
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)
  const [attempt, setAttempt] = useState(0)
  useEffect(() => {
    if (!token) return
    let cancelled = false
    const request = query.trim() ? api.catalog.search(token, query, page)
      : api.catalog.browse(token, kind === 'movie' ? 'movies' : kind, 'popularity', page)
    request.then(result => {
      if (cancelled) return
      setEntries(current => [...new Map([...current, ...result.map(media => ({ id: itemKey(media), media }))].map(entry => [entry.id, entry])).values()])
      setMore(result.length > 0 && page < 500); setFailed(false)
    }).catch(() => { if (!cancelled) setFailed(true) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [token, kind, query, page, attempt])
  return <div role="region" aria-label="Resultados da busca de favoritos" aria-busy={loading} tabIndex={0}
    className="max-h-80 overflow-y-auto overscroll-y-contain p-1 [scrollbar-gutter:stable] focus-visible:outline-2 focus-visible:outline-accent sm:max-h-96">
    <ul className="space-y-2">
      {entries.map(entry => {
        const active = selected.some(value => itemKey(value.media) === itemKey(entry.media))
        const limitReached = selected.filter(value => value.media.kind === entry.media.kind).length >= 5
        return <li key={entry.id}>
          <button type="button" aria-pressed={active} disabled={!active && limitReached} onClick={() => onToggle(entry)}
            aria-label={`${active ? 'Remover' : 'Selecionar'} ${entry.media.title}`}
            className={`group grid w-full grid-cols-[48px_minmax(0,1fr)_44px] items-center gap-3 px-2 py-2 text-left transition-colors duration-100 enabled:hover:bg-bg enabled:active:bg-line disabled:opacity-40 focus-visible:outline-2 focus-visible:outline-accent sm:grid-cols-[56px_minmax(0,1fr)_auto] sm:gap-4 sm:px-3 ${active ? 'bg-bg' : ''}`}>
            <Poster item={entry.media} compact className="w-full shrink-0" />
            <span className="min-w-0">
              <span className="line-clamp-2 break-words text-base font-medium leading-snug">{entry.media.title}</span>
              <span className="mt-1 block text-sm text-muted">{KIND_LABEL[entry.media.kind]} · {entry.media.year ?? 'Ano não informado'}</span>
            </span>
            <span className={`flex min-h-11 min-w-11 items-center justify-center gap-2 text-sm font-medium ${active ? 'text-accent' : 'text-muted group-enabled:group-hover:text-white'}`}>
              {active ? <Check size={20} aria-hidden="true" /> : <Plus size={20} aria-hidden="true" />}
              <span className="hidden sm:inline">{active ? 'Selecionado' : 'Selecionar'}</span>
            </span>
          </button>
        </li>
      })}
    </ul>
    {!entries.length && !loading && !failed && <p className="py-3 text-sm text-muted">Nenhum título encontrado. Tente outro nome.</p>}
    {loading && <p role="status" className="py-3 text-sm text-muted">Buscando no catálogo…</p>}
    {failed && <div className="mt-3"><ErrorBanner>Não foi possível carregar o catálogo.</ErrorBanner>
      <Button onClick={() => { setLoading(true); setAttempt(value => value + 1) }}>Tentar de novo</Button></div>}
    {more && !failed && <Button className="mt-3" disabled={loading} onClick={() => { setLoading(true); setPage(value => value + 1) }}>Carregar mais</Button>}
  </div>
}
