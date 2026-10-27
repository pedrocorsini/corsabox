import type { CatalogItem, MediaDetails, MediaFormat, SeasonDetails, TrendingWindow } from './catalog'

export type BrowseCategory = 'movies' | 'tv' | 'anime'
export type BrowseSort = 'popularity' | 'title'
export type LibraryStatus = 'watchlist' | 'watching' | 'watched'
export type MediaGenre = { id: number; name: string }
export type WatchProvider = { name: string; logoUrl?: string }
export type WatchAvailability = {
  link?: string
  inTheaters: boolean
  streaming?: WatchProvider[]
  free?: WatchProvider[]
  ads?: WatchProvider[]
  rent?: WatchProvider[]
  buy?: WatchProvider[]
}

export type LibraryEntry = {
  id: string
  media: MediaDetails
  status: LibraryStatus
  rating?: number
  watchedOn?: string
  review?: string
  addedAt: string
  updatedAt: string
  progress?: { season: number; episode: number }
}

export type User = {
  id: string
  firstName: string
  lastName: string
  username: string
  email: string
  createdAt: string
}

export type RegisterData = {
  firstName: string
  lastName: string
  username: string
  email: string
  password: string
}

export type EpisodeLog = {
  season: number
  episode: number
  rating?: number
  watchedOn?: string
  review?: string
  updatedAt: string
}

export type WatchedData = { rating: number | null; watchedOn: string | null; review: string | null }
export type Page<T> = {
  content: T[]
  last?: boolean
  totalElements?: number
  page?: { number: number; totalPages: number; totalElements: number }
}

export function pageInfo<T>(p: Page<T>) {
  const more = p.last === false || (p.page ? p.page.number + 1 < p.page.totalPages : false)
  return {
    total: p.totalElements ?? p.page?.totalElements ?? p.content.length,
    hasMore: p.content.length > 0 && more,
  }
}

export type UpdateProfileData = Partial<Pick<User, 'firstName' | 'lastName' | 'username'>>

export type ResetPasswordData = { token: string; newPassword: string }
export type ChangePasswordData = { currentPassword: string; newPassword: string }

/** status 0 significa que a requisição nem chegou ao servidor (rede fora, deploy em andamento). */
export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

const BASE = import.meta.env.VITE_API_URL ?? ''
const TOKEN_KEY = 'corsabox.token'

/** O access token mais recente vive aqui, assim a renovação não dispara efeitos que dependem de `token`. */
export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t: string) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

let onExpired: (() => void) | null = null
export function bindSession(handler: (() => void) | null) {
  onExpired = handler
}

let refreshing: Promise<string | null> | null = null

/** Devolve o novo access token, ou null se a sessão acabou de vez. Erro de rede não derruba a sessão. */
function refreshAccessToken(usedToken: string): Promise<string | null> {
  refreshing ??= (async () => {
    const refresh = async (): Promise<string | null> => {
      // Outra aba pode ter renovado enquanto esta aguardava o bloqueio.
      const latest = tokenStore.get()
      if (latest && latest !== usedToken) return latest

      let res: Response
      try {
        res = await fetch(`${BASE}/api/auth/refresh`, { method: 'POST', credentials: 'include' })
      } catch {
        throw new ApiError(0, 'Sem conexão com o servidor. Tente novamente.')
      }
      if (res.status === 401) return null
      if (!res.ok) throw new ApiError(res.status, 'Algo deu errado. Tente novamente.')
      const { accessToken } = (await res.json()) as { accessToken: string }
      tokenStore.set(accessToken)
      return accessToken
    }

    // O bloqueio é compartilhado entre abas da mesma origem, evitando consumir
    // simultaneamente o mesmo refresh token de cookie.
    if (navigator.locks) {
      return navigator.locks.request('corsabox.refresh-token', refresh)
    }
    return refresh()
  })().finally(() => { refreshing = null })
  return refreshing
}

async function send(path: string, init: RequestInit, bearer: string | null): Promise<Response> {
  try {
    const headers = new Headers(init.headers)
    if (!headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
    if (bearer) headers.set('Authorization', `Bearer ${bearer}`)

    return await fetch(`${BASE}${path}`, {
      ...init,
      credentials: 'include',
      headers,
    })
  } catch {
    throw new ApiError(0, 'Sem conexão com o servidor. Tente novamente.')
  }
}

async function request<T>(path: string, init: RequestInit = {}, token?: string | null): Promise<T> {
  const used = token ? (tokenStore.get() ?? token) : null
  let res = await send(path, init, used)

  if (res.status === 401 && token) {
    const fresh = await refreshAccessToken(used ?? token)
    if (fresh) res = await send(path, init, fresh)
    else {
      // Não encerra uma sessão que outra aba acabou de renovar.
      const latest = tokenStore.get()
      if (latest && latest !== used) res = await send(path, init, latest)
      else onExpired?.()
    }
  }

  if (!res.ok) {
    const body = await res.json().catch(() => null)
    throw new ApiError(res.status, body?.detail ?? 'Algo deu errado. Tente novamente.')
  }
  // 202 e 204 chegam sem corpo
  const text = await res.text()
  return (text ? JSON.parse(text) : undefined) as T
}

export const api = {
  login: (data: { email: string; password: string }) =>
    request<{ accessToken: string }>('/api/auth/login', { method: 'POST', body: JSON.stringify(data) }),
  logout: () => request<void>('/api/auth/logout', { method: 'POST' }),
  startRegister: (data: RegisterData) =>
    request<void>('/api/auth/register', { method: 'POST', body: JSON.stringify(data) }),
  confirmRegister: (data: { email: string; code: string }) =>
    request<User>('/api/auth/register/confirm', { method: 'POST', body: JSON.stringify(data) }),
  me: (token: string) => request<User>('/api/users/me', {}, token),
  updateMe: (token: string, data: UpdateProfileData) =>
    request<User>('/api/users/me', { method: 'PATCH', body: JSON.stringify(data) }, token),

  // Recuperação de senha. Os caminhos abaixo são o contrato que o backend precisa expor.
  forgotPassword: (email: string) =>
    request<void>('/api/auth/forgot-password', { method: 'POST', body: JSON.stringify({ email }) }),
  resetPassword: (data: ResetPasswordData) =>
    request<void>('/api/auth/reset-password', { method: 'POST', body: JSON.stringify(data) }),
  changePassword: (token: string, data: ChangePasswordData) =>
    request<void>('/api/users/me/password', { method: 'PUT', body: JSON.stringify(data) }, token),

  catalog: {
    trending: (period: TrendingWindow = 'week') =>
      request<CatalogItem[]>(`/api/catalog/trending?window=${period}`),
    nowPlaying: (page = 1, genreIds: number[] = []) => {
      const genres = genreIds.length ? `&genreIds=${genreIds.join(',')}` : ''
      return request<CatalogItem[]>(`/api/catalog/now-playing?page=${page}${genres}`)
    },
    search: (token: string, q: string, page = 1) =>
      request<CatalogItem[]>(`/api/catalog/search?q=${encodeURIComponent(q)}&page=${page}`, {}, token),
    details: (token: string, format: MediaFormat, id: number) =>
      request<MediaDetails>(`/api/catalog/${format}/${id}`, {}, token),
    watchProviders: (token: string, format: MediaFormat, id: number) =>
      request<WatchAvailability>(`/api/catalog/${format}/${id}/watch-providers`, {}, token),
    genres: (token: string, format: MediaFormat) =>
      request<MediaGenre[]>(`/api/catalog/genres?format=${format}`, {}, token),
    browse: (token: string, category: BrowseCategory, sort: BrowseSort, page = 1, genreIds: number[] = []) => {
      const genres = genreIds.length ? `&genreIds=${genreIds.join(',')}` : ''
      return request<CatalogItem[]>(
        `/api/catalog/browse?category=${category}&sort=${sort}&page=${page}${genres}`, {}, token)
    },
    season: (token: string, id: number, season: number) =>
      request<SeasonDetails>(`/api/catalog/tv/${id}/seasons/${season}`, {}, token),
  },

  library: {
    entry: (token: string, format: MediaFormat, id: number) =>
      request<LibraryEntry>(`/api/library/entries/${format}/${id}`, {}, token),
    list: (token: string, status: LibraryStatus, page = 0, size = 24) =>
      request<Page<LibraryEntry>>(`/api/library/${status}?page=${page}&size=${size}`, {}, token),
    addToWatchlist: (token: string, format: MediaFormat, id: number) =>
      request<LibraryEntry>(`/api/library/watchlist/${format}/${id}`, { method: 'PUT' }, token),
    removeFromWatchlist: (token: string, format: MediaFormat, id: number) =>
      request<void>(`/api/library/watchlist/${format}/${id}`, { method: 'DELETE' }, token),
    markWatched: (token: string, format: MediaFormat, id: number, data: WatchedData) =>
      request<LibraryEntry>(`/api/library/watched/${format}/${id}`, { method: 'PUT', body: JSON.stringify(data) }, token),
    removeWatched: (token: string, format: MediaFormat, id: number) =>
      request<void>(`/api/library/watched/${format}/${id}`, { method: 'DELETE' }, token),
    progress: (token: string, id: number) =>
      request<EpisodeLog[]>(`/api/library/progress/tv/${id}`, {}, token),
    logEpisode: (token: string, id: number, season: number, episode: number, data: WatchedData) =>
      request<EpisodeLog>(`/api/library/progress/tv/${id}/seasons/${season}/episodes/${episode}`,
        { method: 'PUT', body: JSON.stringify(data) }, token),
    removeEpisode: (token: string, id: number, season: number, episode: number) =>
      request<void>(`/api/library/progress/tv/${id}/seasons/${season}/episodes/${episode}`,
        { method: 'DELETE' }, token),
    markSeason: (token: string, id: number, season: number, until?: number) =>
      request<EpisodeLog[]>(
        `/api/library/progress/tv/${id}/seasons/${season}${until ? `?until=${until}` : ''}`,
        { method: 'PUT' }, token),
    removeWatching: (token: string, id: number) =>
      request<void>(`/api/library/watching/tv/${id}`, { method: 'DELETE' }, token),
  },
}
