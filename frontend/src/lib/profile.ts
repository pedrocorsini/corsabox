import { useEffect, useSyncExternalStore } from 'react'
import { useAuth } from '../auth/useAuth'
import { api, type ProfileCustomization } from './api'

type Snapshot = { data?: ProfileCustomization; error?: boolean }
const empty: Snapshot = {}
const cache = new Map<string, Snapshot>()
const pending = new Map<string, Promise<void>>()
const listeners = new Set<() => void>()
const subscribe = (fn: () => void) => { listeners.add(fn); return () => { listeners.delete(fn) } }
let account = ''
// A library edit can remove a favorite from the watched set. Reload on the next visit.
window.addEventListener('corsabox:library-changed', () => { cache.clear() })
function publish(id: string, value: Snapshot) {
  cache.set(id, value)
  listeners.forEach(fn => fn())
}
export function setProfile(id: string, data: ProfileCustomization) { publish(id, { data }) }
export function loadProfile(token: string, id: string, retry = false) {
  if (pending.has(id) || (!retry && cache.has(id))) return
  const owner = account
  const task = api.profiles.get(token, id).then(data => {
    if (account === owner) publish(id, { data })
  }).catch(() => { if (account === owner) publish(id, { error: true }) })
    .finally(() => { if (pending.get(id) === task) pending.delete(id) })
  pending.set(id, task)
}
export function useProfile(id: string) {
  const { token, user } = useAuth()
  const snapshot = useSyncExternalStore(subscribe, () => cache.get(id) ?? empty)
  useEffect(() => {
    if (account !== (user?.id ?? '')) {
      account = user?.id ?? ''
      cache.clear(); pending.clear()
      listeners.forEach(fn => fn())
    }
    if (token && id) loadProfile(token, id)
  }, [token, id, user?.id])
  return { ...snapshot, retry: () => { if (token) loadProfile(token, id, true) } }
}

export const favoriteKinds = [
  { kind: 'movie', label: 'Filmes favoritos' },
  { kind: 'tv', label: 'Séries favoritas' },
  { kind: 'anime', label: 'Animes favoritos' },
] as const
