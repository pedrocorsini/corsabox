import { useEffect, useState } from 'react'

export type CatalogItem = {
  id: number
  title: string
  kind: 'movie' | 'tv' | 'anime'
  year?: number
  posterUrl?: string
  backdropUrl?: string
}

export const KIND_LABEL = { movie: 'Filme', tv: 'Série', anime: 'Anime' } as const

// Exemplos enquanto o backend do TMDB não existe. Sem posterUrl, o cartão vira um cartão de título desenhado.
export const PLACEHOLDER_ITEMS: CatalogItem[] = [
  { id: 1, title: 'Parasita', kind: 'movie', year: 2019 },
  { id: 2, title: 'Cowboy Bebop', kind: 'anime', year: 1998 },
  { id: 3, title: 'Chernobyl', kind: 'tv', year: 2019 },
  { id: 4, title: 'Your Name', kind: 'anime', year: 2016 },
  { id: 5, title: 'Oppenheimer', kind: 'movie', year: 2023 },
  { id: 6, title: 'Dark', kind: 'tv', year: 2017 },
  { id: 7, title: 'A Viagem de Chihiro', kind: 'anime', year: 2001 },
  { id: 8, title: 'Interestelar', kind: 'movie', year: 2014 },
  { id: 9, title: 'Breaking Bad', kind: 'tv', year: 2008 },
  { id: 10, title: 'Attack on Titan', kind: 'anime', year: 2013 },
]

/**
 * TODO TMDB: trocar pelo endpoint real do backend, por exemplo GET /api/catalog/trending.
 * Ele deve devolver CatalogItem[] com posterUrl e backdropUrl já montados.
 * Quando existir, use `const [items, setItems]` e descomente a chamada abaixo.
 */
export function useTrending(): CatalogItem[] {
  const [items] = useState<CatalogItem[]>(PLACEHOLDER_ITEMS)
  useEffect(() => {
    // api.catalog.trending().then(setItems).catch(() => {})
  }, [])
  return items
}
