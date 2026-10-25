import { useCallback, useEffect, useRef, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { api, ApiError, type EpisodeLog, type WatchedData } from './api'

export const EMPTY_LOG: WatchedData = { rating: null, watchedOn: null, review: null }
export const episodeKey = (season: number, episode: number) => `${season}-${episode}`

const toMap = (list: EpisodeLog[]) =>
  Object.fromEntries(list.map((l) => [episodeKey(l.season, l.episode), l])) as Record<string, EpisodeLog>

function restore(prev: Record<string, EpisodeLog>, key: string, previous: EpisodeLog | undefined) {
  const next = { ...prev }
  if (previous) next[key] = previous
  else delete next[key]
  return next
}

function describeFailure(e: unknown) {
  if (e instanceof ApiError) {
    if (e.status === 0) return e.message
    if (e.status === 401) return 'Sua sessão expirou. Entre novamente.'
    if (e.status === 400) return 'Não foi possível salvar. Confira a nota e a data, e veja se o episódio já estreou.'
  }
  return 'Não foi possível salvar. Tente novamente.'
}

/** Progresso por episódio. As mudanças aparecem na hora e voltam atrás se o servidor recusar. */
export function useEpisodeProgress(tmdbId: number, onStarted: () => void) {
  const { token } = useAuth()
  const [logs, setLogs] = useState<Record<string, EpisodeLog>>({})
  const [loading, setLoading] = useState(true)
  const [loadFailed, setLoadFailed] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [attempt, setAttempt] = useState(0)
  const latest = useRef(logs)
  useEffect(() => { latest.current = logs }, [logs])

  useEffect(() => {
    if (!token) return
    let cancelled = false
    api.library.progress(token, tmdbId)
      .then((list) => {
        if (cancelled) return
        setLogs(toMap(list))
        setLoading(false)
      })
      .catch(() => {
        if (cancelled) return
        setLoadFailed(true)
        setLoading(false)
      })
    return () => { cancelled = true }
  }, [token, tmdbId, attempt])

  const retry = () => {
    setLoadFailed(false)
    setLoading(true)
    setAttempt((a) => a + 1)
  }

  const save = useCallback(async (season: number, episode: number, data: WatchedData) => {
    if (!token) return false
    const key = episodeKey(season, episode)
    const previous = latest.current[key]
    setError(null)
    setLogs((p) => ({
      ...p,
      [key]: {
        season, episode,
        rating: data.rating ?? undefined,
        watchedOn: data.watchedOn ?? undefined,
        review: data.review ?? undefined,
        updatedAt: previous?.updatedAt ?? new Date().toISOString(),
      },
    }))
    try {
      const saved = await api.library.logEpisode(token, tmdbId, season, episode, data)
      setLogs((p) => ({ ...p, [key]: saved }))
      if (!previous) onStarted()
      return true
    } catch (e) {
      setLogs((p) => restore(p, key, previous))
      setError(describeFailure(e))
      return false
    }
  }, [token, tmdbId, onStarted])

  const clear = useCallback(async (season: number, episode: number) => {
    if (!token) return false
    const key = episodeKey(season, episode)
    const previous = latest.current[key]
    setError(null)
    setLogs((p) => restore(p, key, undefined))
    try {
      await api.library.removeEpisode(token, tmdbId, season, episode)
      return true
    } catch (e) {
      setLogs((p) => restore(p, key, previous))
      setError(describeFailure(e))
      return false
    }
  }, [token, tmdbId])

  // O servidor decide quais episódios entram (só os que já estrearam), então aqui não há otimismo.
  const markSeason = useCallback(async (season: number, until?: number) => {
    if (!token) return false
    setError(null)
    try {
      setLogs(toMap(await api.library.markSeason(token, tmdbId, season, until)))
      onStarted()
      return true
    } catch (e) {
      setError(describeFailure(e))
      return false
    }
  }, [token, tmdbId, onStarted])

  return { logs, loading, loadFailed, retry, error, save, clear, markSeason }
}