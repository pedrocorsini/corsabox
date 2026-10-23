import type { CatalogItem } from './catalog'

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

async function request<T>(path: string, init: RequestInit = {}, token?: string | null): Promise<T> {
  let res: Response
  try {
    res = await fetch(`${import.meta.env.VITE_API_URL ?? ''}${path}`, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    })
  } catch {
    throw new ApiError(0, 'Sem conexão com o servidor. Tente novamente.')
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
  register: (data: RegisterData) =>
    request<User>('/api/users', { method: 'POST', body: JSON.stringify(data) }),
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
    trending: () => request<CatalogItem[]>('/api/catalog/trending'),
    search: (token: string, q: string, page = 1) =>
      request<CatalogItem[]>(`/api/catalog/search?q=${encodeURIComponent(q)}&page=${page}`, {}, token),
  },
}
