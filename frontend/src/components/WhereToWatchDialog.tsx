import { useEffect, useId, useRef, useState } from 'react'
import { ExternalLink, X } from 'lucide-react'
import { api, type WatchAvailability, type WatchProvider } from '../lib/api'
import type { MediaFormat } from '../lib/catalog'
import { formatDay, todayISO } from '../lib/format'

type Props = {
  open: boolean
  title: string
  token: string
  format: MediaFormat
  tmdbId: number
  worldwideReleaseDate?: string
  brazilTheatricalReleaseDate?: string
  onClose: () => void
}

const sections: { key: keyof Pick<WatchAvailability, 'streaming' | 'free' | 'ads' | 'rent' | 'buy'>; label: string }[] = [
  { key: 'streaming', label: 'Assinatura' },
  { key: 'free', label: 'Grátis' },
  { key: 'ads', label: 'Com anúncios' },
  { key: 'rent', label: 'Aluguel' },
  { key: 'buy', label: 'Compra' },
]

function ProviderList({ providers }: { providers: WatchProvider[] }) {
  return (
    <ul className="mt-3 flex flex-wrap gap-3">
      {providers.map((provider) => (
        <li key={provider.name} className="flex items-center gap-2 bg-bg px-3 py-2 text-sm">
          {provider.logoUrl && <img src={provider.logoUrl} alt="" aria-hidden="true" className="size-7 rounded-md" />}
          <span>{provider.name}</span>
        </li>
      ))}
    </ul>
  )
}

export function WhereToWatchDialog(props: Props) {
  return <WhereToWatchDialogContent key={`${props.open}:${props.token}:${props.format}:${props.tmdbId}`} {...props} />
}

function WhereToWatchDialogContent({ open, title, token, format, tmdbId, worldwideReleaseDate,
  brazilTheatricalReleaseDate, onClose }: Props) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  const [data, setData] = useState<WatchAvailability | null>(null)
  const [error, setError] = useState(false)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  useEffect(() => {
    if (!open) return
    let cancelled = false
    api.catalog.watchProviders(token, format, tmdbId)
      .then((availability) => { if (!cancelled) setData(availability) })
      .catch(() => { if (!cancelled) setError(true) })
    return () => { cancelled = true }
  }, [open, token, format, tmdbId, attempt])

  const hasProviders = !!data && sections.some(({ key }) => (data[key]?.length ?? 0) > 0)
  const today = todayISO()
  const upcomingWorldwide = worldwideReleaseDate && worldwideReleaseDate >= today ? worldwideReleaseDate : null
  const upcomingBrazil = brazilTheatricalReleaseDate && brazilTheatricalReleaseDate >= today
    ? brazilTheatricalReleaseDate
    : null
  const sharedUpcomingDate = upcomingWorldwide && upcomingBrazil && upcomingWorldwide === upcomingBrazil
    ? upcomingWorldwide
    : null

  return (
    <dialog ref={ref} className="sheet" aria-labelledby={titleId}
      onCancel={(event) => { event.preventDefault(); onClose() }}
      onClick={(event) => { if (event.target === event.currentTarget) onClose() }}>
      <div className="p-5 md:p-7">
        <div className="flex items-start justify-between gap-4">
          <h2 id={titleId} className="text-xl font-semibold">Onde assistir {title}?</h2>
          <button type="button" onClick={onClose} aria-label="Fechar onde assistir"
            className="flex size-10 shrink-0 items-center justify-center text-muted transition enabled:hover:text-white active:scale-[0.97]">
            <X size={20} />
          </button>
        </div>

        <div aria-live="polite" aria-busy={!data && !error} className="mt-6 min-h-24">
          {(upcomingWorldwide || upcomingBrazil) && (
            <div className="mb-5 space-y-2 bg-bg p-4">
              {sharedUpcomingDate ? (
                <p>Estreia mundial e nos cinemas brasileiros: {formatDay(sharedUpcomingDate)}.</p>
              ) : (
                <>
                  {upcomingWorldwide && <p>Estreia mundial prevista: {formatDay(upcomingWorldwide)}.</p>}
                  {upcomingBrazil && <p>Estreia nos cinemas brasileiros: {formatDay(upcomingBrazil)}.</p>}
                </>
              )}
            </div>
          )}
          {!data && !error && <p className="text-muted">Buscando opções disponíveis no Brasil…</p>}
          {error && (
            <div>
              <p>Não foi possível carregar as opções agora.</p>
              <button type="button" onClick={() => { setData(null); setError(false); setAttempt((value) => value + 1) }} className="mt-3 text-accent hover:underline">
                Tentar novamente
              </button>
            </div>
          )}
          {data?.inTheaters && (
            <p className="mb-5 bg-bg p-4 font-medium">Em cartaz nos cinemas brasileiros.</p>
          )}
          {data && !hasProviders && !data.inTheaters && !upcomingWorldwide && !upcomingBrazil && (
            <p>Não encontramos opções de exibição para este título no Brasil.</p>
          )}
          {data && hasProviders && (
            <div className="space-y-5">
              {sections.map(({ key, label }) => {
                const providers = data[key] ?? []
                return providers.length > 0 ? (
                  <section key={key}>
                    <h3 className="font-medium">{label}</h3>
                    <ProviderList providers={providers} />
                  </section>
                ) : null
              })}
              {data.link && (
                <a href={data.link} target="_blank" rel="noopener noreferrer"
                  className="mt-2 inline-flex min-h-10 items-center gap-2 bg-accent px-4 py-2 font-medium text-ink transition hover:brightness-110">
                  Ver opções na TMDB <ExternalLink size={16} aria-hidden="true" />
                </a>
              )}
            </div>
          )}
        </div>

        <p className="mt-6 text-sm text-muted">
          Disponibilidade fornecida pelo <a href="https://www.justwatch.com/br" target="_blank" rel="noopener noreferrer" className="underline hover:text-white">JustWatch</a>.
        </p>
      </div>
    </dialog>
  )
}
