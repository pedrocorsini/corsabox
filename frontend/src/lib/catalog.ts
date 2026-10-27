import { useEffect, useState } from 'react'
import { api } from './api'
import { formatDay, formatRuntime } from './format'

export type MediaFormat = 'movie' | 'tv'
export type TrendingWindow = 'day' | 'week'

export type CatalogItem = {
  id: number
  title: string
  kind: 'movie' | 'tv' | 'anime'
  format: MediaFormat
  year?: number
  overview?: string
  posterUrl?: string
  backdropUrl?: string
}

export type MediaDetails = CatalogItem & {
  releaseDate?: string      // 'YYYY-MM-DD'
  worldwideReleaseDate?: string
  brazilTheatricalReleaseDate?: string
  runtimeMinutes?: number
  genres?: string[]
  directors?: string[]
  writers?: string[]
  creators?: string[]
  cast?: string[]
  trailerKey?: string
  seasons?: SeasonSummary[]
}

export const PERIODS = [
  { id: 'day', param: 'hoje', label: 'Hoje' },
  { id: 'week', param: 'semana', label: 'Nesta semana' },
] as const

export const titlePath = (i: Pick<CatalogItem, 'format' | 'id'>) => `/titulo/${i.format}/${i.id}`

export const itemKey = (i: Pick<CatalogItem, 'format' | 'id'>) => `${i.format}-${i.id}`

export const KIND_LABEL = { movie: 'Filme', tv: 'Série', anime: 'Anime' } as const

export function metaLine(item: MediaDetails) {
  return [
    KIND_LABEL[item.kind],
    item.releaseDate ? formatDay(item.releaseDate) : item.year ?? null,
    item.runtimeMinutes ? formatRuntime(item.runtimeMinutes, item.format === 'tv') : null,
    item.genres?.join(', '),
  ].filter(Boolean).join(' · ')
}

// Exemplos para quando a API não responde. Sem posterUrl, o cartão vira um cartão de título desenhado.
export const PLACEHOLDER_ITEMS: CatalogItem[] = [
  { id: 1, title: 'Parasita', kind: 'movie', format: 'movie', year: 2019 },
  { id: 2, title: 'Cowboy Bebop', kind: 'anime', format: 'tv', year: 1998 },
  { id: 3, title: 'Chernobyl', kind: 'tv', format: 'tv', year: 2019 },
  { id: 4, title: 'Your Name', kind: 'anime', format: 'movie', year: 2016 },
  { id: 5, title: 'Oppenheimer', kind: 'movie', format: 'movie', year: 2023 },
  { id: 6, title: 'Dark', kind: 'tv', format: 'tv', year: 2017 },
  { id: 7, title: 'A Viagem de Chihiro', kind: 'anime', format: 'movie', year: 2001 },
  { id: 8, title: 'Interestelar', kind: 'movie', format: 'movie', year: 2014 },
  { id: 9, title: 'Breaking Bad', kind: 'tv', format: 'tv', year: 2008 },
  { id: 10, title: 'Attack on Titan', kind: 'anime', format: 'tv', year: 2013 },
]

// `loading` vale só na primeira carga. Ao trocar o período, a lista antiga fica até a nova chegar (`pending`).
export function useTrending(period: TrendingWindow) {
  const [result, setResult] = useState<{ period: TrendingWindow; items: CatalogItem[] } | null>(null)
  useEffect(() => {
    let cancelled = false
    api.catalog.trending(period)
      .then((data) => (data.length > 0 ? data : PLACEHOLDER_ITEMS))
      .catch(() => PLACEHOLDER_ITEMS)
      .then((items) => { if (!cancelled) setResult({ period, items }) })
    return () => { cancelled = true }
  }, [period])
  return {
    items: result?.items ?? [],
    loading: result === null,
    pending: result !== null && result.period !== period,
  }
}

export type SeasonSummary = { number: number; name?: string | null; episodeCount: number; airDate?: string | null }

export type Episode = {
  number: number
  name?: string | null
  overview?: string | null
  airDate?: string | null
  runtimeMinutes?: number | null
  stillUrl?: string | null
}

export type SeasonDetails = { number: number; name?: string | null; episodes: Episode[] }

