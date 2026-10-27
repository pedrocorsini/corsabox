import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useLocation, useParams, useSearchParams } from 'react-router'
import { ArrowLeft, Play } from 'lucide-react'
import { useAuth } from '../auth/useAuth'
import { AppHeader } from '../components/AppHeader'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { Poster } from '../components/Poster'
import { RatingMeter } from '../components/Rating'
import { SeasonsSection } from '../components/SeasonsSection'
import { TrailerDialog } from '../components/TrailerDialog'
import { WatchedForm, type WatchedPayload } from '../components/WatchedForm'
import { Button, ErrorBanner } from '../components/ui'
import { api, ApiError, type LibraryEntry } from '../lib/api'
import { metaLine, type MediaDetails, type MediaFormat } from '../lib/catalog'
import { formatDay, formatInstant } from '../lib/format'

// A TMDB serve a mesma imagem em vários tamanhos. Em tela cheia, o w1280 estica e perde nitidez.
const hiRes = (url: string) => url.replace('/w1280/', '/original/')

function describe(e: unknown) {
  if (e instanceof ApiError) {
    if (e.status === 0) return e.message
    if (e.status === 401) return 'Sua sessão expirou. Entre novamente.'
    if (e.status === 409) return 'Este título acabou de mudar na sua biblioteca. Tente de novo.'
    if (e.status === 400) return 'Não foi possível salvar. Confira a nota e a data.'
  }
  return 'Não foi possível concluir. Tente novamente.'
}

function Credits({ item, className = '' }: { item: MediaDetails; className?: string }) {
  const rows = [
    { one: 'Diretor', many: 'Diretores', names: item.directors },
    { one: 'Roteirista', many: 'Roteiristas', names: item.writers },
    { one: 'Criador', many: 'Criadores', names: item.creators },
    { one: 'Elenco principal', many: 'Elenco principal', names: item.cast },
  ].filter((r) => r.names && r.names.length > 0)
  if (rows.length === 0) return null
  return (
    <dl className={`space-y-5 text-[15px] ${className}`}>
      {rows.map((r) => (
        <div key={r.one}>
          <dt className="text-muted">{r.names!.length > 1 ? r.many : r.one}</dt>
          <dd>
            <ul className="mt-1 space-y-0.5">
              {r.names!.map((n) => <li key={n}>{n}</li>)}
            </ul>
          </dd>
        </div>
      ))}
    </dl>
  )
}

export default function TitlePage() {
  const { format, id } = useParams()
  const tmdbId = Number(id)
  if ((format !== 'movie' && format !== 'tv') || !Number.isInteger(tmdbId) || tmdbId <= 0)
    return <Navigate to="/catalogo" replace />
  return <TitleView key={`${format}-${tmdbId}`} format={format} tmdbId={tmdbId} />
}

function TitleView({ format, tmdbId }: { format: MediaFormat; tmdbId: number }) {
  const { token } = useAuth()
  const location = useLocation()
  const from = (location.state as { from?: unknown } | null)?.from
  const returnTo = typeof from === 'string' && from.startsWith('/') && !from.startsWith('//')
    ? from
    : '/catalogo'
  const [params] = useSearchParams()
  const wantsForm = params.get('assistir') === '1'
  const [item, setItem] = useState<MediaDetails | null>(null)
  const [entry, setEntry] = useState<LibraryEntry | null>(null)
  const [failure, setFailure] = useState<'missing' | 'network' | null>(null)
  const [attempt, setAttempt] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [confirmRemove, setConfirmRemove] = useState(false)
  const [confirmStop, setConfirmStop] = useState(false)
  const [trailerOpen, setTrailerOpen] = useState(false)
  const [epoch, setEpoch] = useState(0)
  const [busy, setBusy] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!token) return
    let cancelled = false
    Promise.all([
      api.catalog.details(token, format, tmdbId),
      api.library.entry(token, format, tmdbId).catch((e) => {
        if (e instanceof ApiError && e.status === 404) return null
        throw e
      }),
    ])
      .then(([details, current]) => {
        if (cancelled) return
        setItem(details)
        setEntry(current)
        setFormOpen(wantsForm && current?.status !== 'watched')
      })
      .catch((e) => {
        if (!cancelled) setFailure(e instanceof ApiError && e.status === 404 ? 'missing' : 'network')
      })
    return () => { cancelled = true }
  }, [token, format, tmdbId, attempt, wantsForm])

  const retry = () => {
    setFailure(null)
    setAttempt((a) => a + 1)
  }

  async function act(key: string, fn: (t: string) => Promise<void>) {
    if (!token) return
    setBusy(key)
    setError(null)
    try {
      await fn(token)
    } catch (e) {
      setError(describe(e))
    } finally {
      setBusy(null)
    }
  }

  // O primeiro episódio marcado promove o título para "assistindo" no servidor.
  // Aqui só recarregamos o registro quando ele ainda não estava nesse estado.
  const syncEntry = useCallback(() => {
    if (!token || entry?.status === 'watching' || entry?.status === 'watched') return
    api.library.entry(token, format, tmdbId).then(setEntry).catch(() => {})
  }, [token, format, tmdbId, entry?.status])

  const addToList = () => act('list', async (t) => setEntry(await api.library.addToWatchlist(t, format, tmdbId)))

  const removeFromList = () => act('unlist', async (t) => {
    await api.library.removeFromWatchlist(t, format, tmdbId)
    setEntry(null)
  })

  const stopWatching = () => act('stop', async (t) => {
    await api.library.removeWatching(t, tmdbId)
    setEntry(null)
    setEpoch((n) => n + 1) // remonta as temporadas, que já não têm episódios marcados
    setConfirmStop(false)
  })

  const save = (data: WatchedPayload) => act('save', async (t) => {
    let saved: LibraryEntry
    try {
      saved = await api.library.markWatched(t, format, tmdbId, data)
    } catch (e) {
      // Dois PUTs simultâneos: na segunda tentativa o título já existe e o fluxo normal assume.
      if (!(e instanceof ApiError && e.status === 409)) throw e
      saved = await api.library.markWatched(t, format, tmdbId, data)
    }
    setEntry(saved)
    setFormOpen(false)
    setEpoch((n) => n + 1)
  })

  const removeWatched = () => act('remove', async (t) => {
    await api.library.removeWatched(t, format, tmdbId)
    setEntry(null)
    setEpoch((n) => n + 1)
    setConfirmRemove(false)
  })

  const watched = entry?.status === 'watched'
  const watching = entry?.status === 'watching'

  return (
    <div className="relative isolate min-h-svh bg-bg text-white">
      {item?.backdropUrl && (
        <>
          <img
            key={item.backdropUrl}
            src={hiRes(item.backdropUrl)}
            alt=""
            decoding="async"
            onError={(e) => {
              // Se a versão original falhar, volta para a de 1280.
              if (e.currentTarget.src !== item.backdropUrl) e.currentTarget.src = item.backdropUrl!
            }}
            className="fade-in fixed inset-0 -z-10 h-full w-full object-cover"
          />
          <div className="fixed inset-0 -z-10 bg-bg/75" />
        </>
      )}

      <AppHeader />
      <main className="mx-auto max-w-6xl px-5 pb-24 pt-2 md:px-8">
        <Link to={returnTo} className="inline-flex items-center gap-1.5 text-[15px] text-muted transition-colors hover:text-white">
          <ArrowLeft size={16} /> Voltar
        </Link>

        {failure && (
          <div className="mt-10 max-w-md space-y-4">
            <p className="text-2xl font-semibold">
              {failure === 'missing' ? 'Título não encontrado.' : 'Não foi possível carregar o título.'}
            </p>
            {failure === 'network' && <Button variant="primary" onClick={retry}>Tentar de novo</Button>}
          </div>
        )}

        {!failure && !item && (
          <div aria-busy="true" className="mt-6 space-y-8">
            <div className="grid gap-8 md:grid-cols-12 md:gap-x-6">
              <div className="aspect-[2/3] w-36 bg-surface md:col-span-3 md:w-full" />
              <div className="space-y-4 md:col-span-8 md:col-start-5">
                <div className="h-4 w-32 bg-surface" />
                <div className="h-12 w-2/3 bg-surface" />
              </div>
            </div>
          </div>
        )}

        {item && (
          <div className="fade-in">
            <div className="mt-8 grid gap-8 md:grid-cols-12 md:gap-x-6">
              <div className="w-36 md:col-span-3 md:w-full">
                <Poster item={item} />
                <Credits item={item} className="mt-6 hidden md:block" />
              </div>

              <div className="md:col-span-8 md:col-start-5">
                <p className="text-[15px] text-muted">{metaLine(item)}</p>
                <h1 className="mt-1 text-4xl font-semibold md:text-5xl">
                  {item.title}
                  {item.year && <span className="ml-3 font-normal text-muted">({item.year})</span>}
                </h1>

                <p className="mt-6 max-w-[60ch] text-[1.0625rem] leading-[1.6] md:text-lg">
                  {item.overview ?? <span className="text-muted">Sem sinopse disponível.</span>}
                </p>

                {item.trailerKey && (
                  <Button className="mt-5" onClick={() => setTrailerOpen(true)}>
                    <Play size={16} aria-hidden="true" /> Assistir trailer
                  </Button>
                )}

                {entry?.status === 'watchlist' && (
                  <p className="mt-3 text-muted">Na sua lista de quero ver desde {formatInstant(entry.addedAt)}.</p>
                )}

                {error && <div key={error} className="notice-in mt-6"><ErrorBanner>{error}</ErrorBanner></div>}

                {!formOpen && (
                  <div className="mt-8 flex flex-wrap gap-3">
                    <Button variant="primary" onClick={() => setFormOpen(true)}>
                      {watched ? 'Editar registro' : 'Marcar como Assistido'}
                    </Button>
                    {watched ? (
                      <Button onClick={() => setConfirmRemove(true)}>Remover dos assistidos</Button>
                    ) : watching ? (
                      <Button onClick={() => setConfirmStop(true)}>Parar de acompanhar</Button>
                    ) : entry ? (
                      <Button loading={busy === 'unlist'} onClick={removeFromList}>Tirar da lista</Button>
                    ) : (
                      <Button loading={busy === 'list'} onClick={addToList}>Quero ver</Button>
                    )}
                  </div>
                )}

                {formOpen && (
                  <section key="form" className="notice-in mt-8 bg-surface p-5 md:p-6">
                    <h2 className="mb-6 text-xl font-semibold">{watched ? 'Editar registro' : 'Registrar como assistido'}</h2>
                    <WatchedForm
                      initial={{ rating: entry?.rating ?? null, watchedOn: entry?.watchedOn ?? '', review: entry?.review ?? '' }}
                      submitLabel="Salvar"
                      busy={busy === 'save'}
                      onSubmit={save}
                      onCancel={() => setFormOpen(false)}
                    />
                  </section>
                )}

                {watched && !formOpen && entry && (
                  <section className="mt-10 bg-surface p-5 md:p-6">
                    <h2 className="text-xl font-semibold">Seu registro</h2>
                    <dl className="mt-5 space-y-5 text-[15px]">
                      <div>
                        <dt className="text-muted">Nota</dt>
                        <dd className="mt-1">
                          {entry.rating !== undefined ? <RatingMeter value={entry.rating} /> : <span className="text-muted">Sem nota</span>}
                        </dd>
                      </div>
                      <div>
                        <dt className="text-muted">Assistido em</dt>
                        <dd className="mt-1">{entry.watchedOn ? formatDay(entry.watchedOn) : <span className="text-muted">Sem data</span>}</dd>
                      </div>
                      {entry.review && (
                        <div>
                          <dt className="text-muted">Review</dt>
                          <dd className="mt-1 max-w-[60ch] whitespace-pre-wrap text-base leading-relaxed">{entry.review}</dd>
                        </div>
                      )}
                    </dl>
                  </section>
                )}

                {format === 'tv' && item.seasons && item.seasons.length > 0 && (
                  <SeasonsSection key={epoch} tmdbId={tmdbId} seasons={item.seasons} onChange={syncEntry} />
                )}

                <Credits item={item} className="mt-10 md:hidden" />
              </div>
            </div>
          </div>
        )}
      </main>

      <ConfirmDialog
        open={confirmRemove}
        title="Remover dos assistidos?"
        description="A nota, a data e a review deste registro serão apagadas."
        confirmLabel="Remover"
        busy={busy === 'remove'}
        onConfirm={removeWatched}
        onCancel={() => setConfirmRemove(false)}
      />

      <ConfirmDialog
        open={confirmStop}
        title="Parar de acompanhar?"
        description="Os episódios marcados, com nota, data e review, serão apagados."
        confirmLabel="Parar"
        busy={busy === 'stop'}
        onConfirm={stopWatching}
        onCancel={() => setConfirmStop(false)}
      />

      {item?.trailerKey && (
        <TrailerDialog
          open={trailerOpen}
          title={item.title}
          videoKey={item.trailerKey}
          onClose={() => setTrailerOpen(false)}
        />
      )}
    </div>
  )
}
