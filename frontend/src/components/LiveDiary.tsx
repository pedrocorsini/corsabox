import { useId, useState } from 'react'
import { metaLine, type MediaDetails } from '../lib/catalog'
import { formatDay, todayISO } from '../lib/format'
import { Poster } from './Poster'
import { RatingInput } from './Rating'

type Status = 'watched' | 'watchlist'
type Entry = { status: Status; rating: number | null; watchedOn?: string; review?: string }

const TABS: { status: Status; label: string }[] = [
  { status: 'watched', label: 'Assistidos' },
  { status: 'watchlist', label: 'Quero ver' },
]

const TITLES: MediaDetails[] = [
  { id: 496243, title: 'Parasita', kind: 'movie', format: 'movie', year: 2019,
    releaseDate: '2019-11-07', runtimeMinutes: 132, genres: ['Comédia', 'Thriller', 'Drama'],
    posterUrl: '/posters/parasita.webp' },
  { id: 1, title: 'Cowboy Bebop', kind: 'anime', format: 'tv', year: 1998,
    releaseDate: '1998-04-03', runtimeMinutes: 24, genres: ['Animação', 'Ação', 'Ficção científica'],
    posterUrl: '/posters/cowboy-bebop.webp' },
  { id: 87108, title: 'Chernobyl', kind: 'tv', format: 'tv', year: 2019,
    releaseDate: '2019-05-06', runtimeMinutes: 60, genres: ['Drama', 'História'],
    posterUrl: '/posters/chernobyl.webp' },
]

const INITIAL: Record<number, Entry> = {
  496243: { status: 'watched', rating: 4.5, watchedOn: '2026-09-14', review: 'Não esperava aquele final.' },
  1: { status: 'watchlist', rating: null },
  87108: { status: 'watchlist', rating: null },
}

const action = 'h-10 px-4 text-[15px] font-semibold transition active:scale-[0.97]'

// A homepage em miniatura: a biblioteca de verdade, com exemplos. Nada aqui é salvo. 
export function LiveDiary() {
  const [tab, setTab] = useState<Status>('watched')
  const [state, setState] = useState(INITIAL)
  const uid = useId()

  const patch = (id: number, p: Partial<Entry>) =>
    setState((prev) => ({ ...prev, [id]: { ...prev[id], ...p } }))
  const count = (s: Status) => TITLES.filter((t) => state[t.id].status === s).length
  const rows = TITLES.filter((t) => state[t.id].status === tab)

  return (
    <div data-ui className="drift text-white">
      <div role="tablist" aria-label="Listas de exemplo" className="flex gap-8">
        {TABS.map((t) => {
          const on = t.status === tab
          return (
            <button key={t.status} role="tab" aria-selected={on} onClick={() => setTab(t.status)}
              className={`border-b-2 pb-2 text-lg transition-colors ${on ? 'border-accent text-white' : 'border-transparent text-muted hover:text-white'}`}>
              {t.label}
              <span className="ml-2 text-[15px] tabular-nums text-muted">{count(t.status)}</span>
            </button>
          )
        })}
      </div>

      <ul role="tabpanel" className="mt-6 space-y-2">
        {rows.length === 0 && (
          <li className="bg-surface p-5 text-muted">
            {tab === 'watched' ? 'Nada registrado ainda. Marque um título da fila.' : 'Sua fila está vazia.'}
          </li>
        )}
        {rows.map((t) => {
          const e = state[t.id]
          return (
            <li key={`${tab}-${t.id}`} className="fade-in grid grid-cols-[4rem_1fr] gap-4 bg-surface p-3 md:grid-cols-[5rem_1fr] md:gap-5 md:p-4">
              <Poster item={t} />
              <div className="min-w-0">
                <p className="truncate text-lg font-semibold">{t.title}</p>
                <p className="text-[15px] text-muted">{metaLine(t)}</p>

                {tab === 'watched' ? (
                  <div className="mt-4 space-y-3">
                    <span id={`${uid}-${t.id}`} className="sr-only">Nota de {t.title}</span>
                    <RatingInput value={e.rating} onChange={(rating) => patch(t.id, { rating })} labelledBy={`${uid}-${t.id}`} />
                    <p className="text-[15px] text-muted">
                      {e.watchedOn ? `Assistido em ${formatDay(e.watchedOn)}` : 'Sem data'}
                    </p>
                    {e.review && <p className="text-[15px] text-muted">{e.review}</p>}
                    <button type="button" onClick={() => patch(t.id, { status: 'watchlist', rating: null, watchedOn: undefined, review: undefined })}
                      className="text-sm text-muted transition-colors hover:text-white">
                      Voltar para a fila
                    </button>
                  </div>
                ) : (
                  <div className="mt-4">
                    <button type="button" onClick={() => patch(t.id, { status: 'watched', watchedOn: todayISO() })}
                      className={`${action} bg-accent text-ink hover:brightness-105`}>
                      Assisti
                    </button>
                  </div>
                )}
              </div>
            </li>
          )
        })}
      </ul>

      <p className="mt-5 text-[15px] text-muted">Exemplo para testar. Seus registros reais aparecem na Biblioteca depois do cadastro.</p>
    </div>
  )
}