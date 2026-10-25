import { useEffect, useRef, useState } from 'react'
import { Check } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'
import { api, type EpisodeLog } from '../lib/api'
import type { Episode, SeasonDetails, SeasonSummary } from '../lib/catalog'
import { EMPTY_LOG, episodeKey, useEpisodeProgress } from '../lib/episodes'
import { formatDay, formatRuntime, todayISO } from '../lib/format'
import { ConfirmDialog } from './ConfirmDialog'
import { RatingMeter } from './Rating'
import { WatchedForm, type WatchedPayload } from './WatchedForm'
import { Button, ErrorBanner } from './ui'

type Props = { tmdbId: number; seasons: SeasonSummary[]; onChange: () => void }

const hasAired = (date?: string | null) => !date || date <= todayISO()
const hasDetails = (l: EpisodeLog) => typeof l.rating === 'number' || !!l.watchedOn || !!l.review
const tabLabel = (n: number) => (n === 0 ? 'Especiais' : `T${n}`)

/** Onde a pessoa parou: a maior temporada regular, e nela o maior episódio. */
function lastWatched(logs: EpisodeLog[]) {
  const pool = logs.some((l) => l.season > 0) ? logs.filter((l) => l.season > 0) : logs
  return pool.reduce<EpisodeLog | null>((best, l) =>
    !best || l.season > best.season || (l.season === best.season && l.episode > best.episode) ? l : best, null)
}

export function SeasonsSection({ tmdbId, seasons, onChange }: Props) {
  const { token } = useAuth()
  const progress = useEpisodeProgress(tmdbId, onChange)
  const all = Object.values(progress.logs)

  // Especiais (temporada 0) vão para o fim e ficam fora da conta geral.
  const ordered = [...seasons.filter((s) => s.number > 0), ...seasons.filter((s) => s.number === 0)]
  const regularTotal = seasons.filter((s) => s.number > 0).reduce((sum, s) => sum + s.episodeCount, 0)
  const regularDone = all.filter((l) => l.season > 0).length
  const resume = lastWatched(all)

  const [active, setActive] = useState<number | null>(null)
  if (active === null && !progress.loading) setActive(resume?.season ?? ordered[0].number)
  const current = active ?? ordered[0].number

  const [data, setData] = useState<Record<number, SeasonDetails | 'error'>>({})
  const [attempt, setAttempt] = useState(0)
  const [open, setOpen] = useState<string | null>(null)
  const [busy, setBusy] = useState<string | null>(null)
  const [confirm, setConfirm] = useState<{ season: number; episode: number } | null>(null)
  const tabs = useRef<HTMLDivElement>(null)

  const loaded = data[current] !== undefined
  useEffect(() => {
    if (!token || loaded) return
    let cancelled = false
    api.catalog.season(token, tmdbId, current)
      .then((d) => { if (!cancelled) setData((p) => ({ ...p, [current]: d })) })
      .catch(() => { if (!cancelled) setData((p) => ({ ...p, [current]: 'error' })) })
    return () => { cancelled = true }
  }, [token, tmdbId, current, loaded, attempt])

  // Mantém a aba ativa centralizada, inclusive quando "Continuar" pula para outra temporada.
  useEffect(() => {
    const list = tabs.current
    const tab = list?.querySelector<HTMLElement>('[aria-selected="true"]')
    if (!list || !tab) return
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    list.scrollTo({
      left: tab.offsetLeft - (list.clientWidth - tab.offsetWidth) / 2,
      behavior: reduce ? 'auto' : 'smooth',
    })
  }, [current])

  const retrySeason = () => {
    setData((p) => {
      const next = { ...p }
      delete next[current]
      return next
    })
    setAttempt((a) => a + 1)
  }

  const pick = (n: number) => {
    setActive(n)
    setOpen(null)
  }

  const scrollToTab = (n: number, smooth = true) => {
    const list = tabs.current
    const tab = list?.querySelector<HTMLElement>(`[data-season="${n}"]`)
    if (!list || !tab) return
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    list.scrollTo({
      left: tab.offsetLeft - (list.clientWidth - tab.offsetWidth) / 2,
      behavior: smooth && !reduce ? 'smooth' : 'auto',
    })
  }

  const positioned = useRef(false)
  useEffect(() => {
    if (progress.loading || positioned.current) return
    positioned.current = true
    scrollToTab(current, false)
  })

  const summary = ordered.find((s) => s.number === current)
  const season = data[current]
  const total = summary?.episodeCount ?? 0
  const done = all.filter((l) => l.season === current).length
  const ratio = total > 0 ? Math.min(done / total, 1) : 0

  const toggle = (ep: Episode) => {
    const log = progress.logs[episodeKey(current, ep.number)]
    if (!log) void progress.save(current, ep.number, EMPTY_LOG)
    else if (hasDetails(log)) setConfirm({ season: current, episode: ep.number })
    else void progress.clear(current, ep.number)
  }

  const submit = async (ep: Episode, payload: WatchedPayload) => {
    const key = episodeKey(current, ep.number)
    setBusy(key)
    const ok = await progress.save(current, ep.number, payload)
    setBusy(null)
    if (ok) setOpen(null)
  }

  const fill = async (until?: number) => {
    setBusy(`season-${current}`)
    const ok = await progress.markSeason(current, until)
    setBusy(null)
    if (ok) setOpen(null)
  }

  return (
    <section className="mt-10">
      <div className="flex flex-wrap items-end justify-between gap-x-6 gap-y-1">
        <h2 className="text-xl font-semibold">Temporadas</h2>
        {!progress.loading && !progress.loadFailed && (
          <p className="text-[15px] tabular-nums text-muted">{regularDone} de {regularTotal} episódios</p>
        )}
      </div>

      {resume && resume.season !== current && (
        <button type="button"
          onClick={() => { pick(resume.season); scrollToTab(resume.season) }}
          className="mt-2 text-[15px] font-medium text-accent transition active:scale-[0.97] hover:underline">
          Você parou em T{resume.season} E{resume.episode}. Continuar
        </button>
      )}

      <div ref={tabs} role="tablist" aria-label="Temporadas"
        className="scroll-thin relative mt-6 flex gap-6 overflow-x-auto pb-3 sm:gap-8">
        {ordered.map((s) => {
          const on = s.number === current
          return (
            <button key={s.number} role="tab" data-season={s.number} aria-selected={on}
              aria-label={s.name ?? tabLabel(s.number)}
              onClick={() => pick(s.number)}
              className={`shrink-0 border-b-2 pb-2 text-lg transition-colors ${on ? 'border-accent text-white' : 'border-transparent text-muted hover:text-white'}`}>
              {tabLabel(s.number)}
            </button>
          )
        })}
      </div>

      <div role="tabpanel" className="mt-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="text-[15px] text-muted">
            {summary?.name ?? tabLabel(current)} · {total} {total === 1 ? 'episódio' : 'episódios'}
            {total > 0 && <span className="tabular-nums"> · {done} assistidos</span>}
          </p>
          <Button loading={busy === `season-${current}`} disabled={progress.loading || done >= total}
            onClick={() => fill()}>
            Marcar temporada
          </Button>
        </div>

        <div role="progressbar" aria-label="Progresso da temporada" aria-valuemin={0} aria-valuemax={total}
          aria-valuenow={done} className="mt-3 h-1 bg-line">
          <div className="h-full origin-left bg-accent transition-transform duration-200 ease-(--ease-out) motion-reduce:transition-none"
            style={{ transform: `scaleX(${ratio})` }} />
        </div>

        {progress.error && (
          <div key={progress.error} className="notice-in mt-4"><ErrorBanner>{progress.error}</ErrorBanner></div>
        )}

        {progress.loadFailed && (
          <div className="mt-6 max-w-md space-y-4">
            <p className="text-muted">Não foi possível carregar o seu progresso.</p>
            <Button variant="primary" onClick={progress.retry}>Tentar de novo</Button>
          </div>
        )}

        {!progress.loadFailed && (progress.loading || season === undefined) && (
          <ul aria-busy="true" className="mt-6 space-y-2">
            {Array.from({ length: 5 }, (_, i) => <li key={i} className="h-[4.5rem] bg-surface" />)}
          </ul>
        )}

        {!progress.loadFailed && season === 'error' && (
          <div className="mt-6 max-w-md space-y-4">
            <ErrorBanner>Não foi possível carregar os episódios.</ErrorBanner>
            <Button variant="primary" onClick={retrySeason}>Tentar de novo</Button>
          </div>
        )}

        {!progress.loading && !progress.loadFailed && season && season !== 'error' && (
          <ul className="mt-6 space-y-2">
            {season.episodes.map((ep) => {
              const key = episodeKey(current, ep.number)
              return (
                <EpisodeRow key={key} ep={ep} log={progress.logs[key]}
                  open={open === key} saving={busy === key}
                  canFill={season.episodes.some((e) =>
                    e.number < ep.number && hasAired(e.airDate) && !progress.logs[episodeKey(current, e.number)])}
                  onToggle={() => toggle(ep)}
                  onOpen={() => setOpen(open === key ? null : key)}
                  onSave={(payload) => submit(ep, payload)}
                  onFill={() => fill(ep.number)} />
              )
            })}
          </ul>
        )}
      </div>

      <ConfirmDialog
        open={confirm !== null}
        title="Desmarcar episódio?"
        description="A nota, a data e a review deste episódio serão apagadas."
        confirmLabel="Desmarcar"
        onConfirm={() => {
          if (confirm) void progress.clear(confirm.season, confirm.episode)
          setConfirm(null)
        }}
        onCancel={() => setConfirm(null)}
      />
    </section>
  )
}

type RowProps = {
  ep: Episode
  log?: EpisodeLog
  open: boolean
  saving: boolean
  canFill: boolean
  onToggle: () => void
  onOpen: () => void
  onSave: (payload: WatchedPayload) => void
  onFill: () => void
}

function EpisodeRow({ ep, log, open, saving, canFill, onToggle, onOpen, onSave, onFill }: RowProps) {
  const aired = hasAired(ep.airDate)
  const meta = [
    ep.airDate ? (aired ? formatDay(ep.airDate) : `Estreia em ${formatDay(ep.airDate)}`) : null,
    ep.runtimeMinutes ? formatRuntime(ep.runtimeMinutes) : null,
  ].filter(Boolean).join(' · ')

  return (
    <li className="bg-surface">
      <div className="grid grid-cols-[2.5rem_1fr_auto] items-center gap-3 p-3 md:gap-4 md:p-4">
        <button type="button" aria-pressed={!!log} disabled={!aired} onClick={onToggle}
          aria-label={`${log ? 'Desmarcar' : 'Marcar'} episódio ${ep.number}`}
          className={`flex size-10 items-center justify-center font-semibold tabular-nums transition active:scale-[0.97] disabled:cursor-not-allowed disabled:opacity-40 ${log ? 'bg-accent text-ink' : 'bg-bg text-muted enabled:hover:text-white'}`}>
          {log ? <Check size={18} aria-hidden="true" /> : ep.number}
        </button>

        <div className="min-w-0">
          <p className="truncate font-semibold">{ep.name ?? `Episódio ${ep.number}`}</p>
          {meta && <p className="text-[15px] text-muted">{meta}</p>}
          {log && (typeof log.rating === 'number' || log.watchedOn) && (
            <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1">
              {typeof log.rating === 'number' && <RatingMeter value={log.rating} size="sm" />}
              {log.watchedOn && <span className="text-[15px] text-muted">{formatDay(log.watchedOn)}</span>}
            </div>
          )}
        </div>

        <button type="button" aria-expanded={open} disabled={!aired} onClick={onOpen}
          className="h-10 px-2 text-[15px] text-muted transition active:scale-[0.97] enabled:hover:text-white disabled:cursor-not-allowed disabled:opacity-40">
          {open ? 'Fechar' : 'Detalhes'}
        </button>
      </div>

      {open && (
        <div className="notice-in space-y-6 pb-5 pl-16 pr-3 md:pl-[4.5rem] md:pr-4">
          {ep.overview && <p className="max-w-[60ch] text-[15px] leading-relaxed text-muted">{ep.overview}</p>}
          {canFill && (
            <button type="button" onClick={onFill}
              className="text-sm font-medium text-accent transition active:scale-[0.97] hover:underline">
              Marcar do início até este episódio
            </button>
          )}
          <WatchedForm
            initial={{ rating: log?.rating ?? null, watchedOn: log?.watchedOn ?? '', review: log?.review ?? '' }}
            submitLabel="Salvar"
            busy={saving}
            onSubmit={onSave}
            onCancel={onOpen}
          />
        </div>
      )}
    </li>
  )
}