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

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function request<T>(path: string, init: RequestInit = {}, token?: string | null): Promise<T> {
  const res = await fetch(`${import.meta.env.VITE_API_URL ?? ''}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
  if (!res.ok) {
    const body = await res.json().catch(() => null)
    throw new ApiError(res.status, body?.detail ?? 'Algo deu errado. Tente novamente.')
  }
  return res.json()
}

export const api = {
  login: (data: { email: string; password: string }) =>
    request<{ accessToken: string }>('/api/auth/login', { method: 'POST', body: JSON.stringify(data) }),
  register: (data: RegisterData) =>
    request<User>('/api/users', { method: 'POST', body: JSON.stringify(data) }),
  me: (token: string) => request<User>('/api/users/me', {}, token),
  updateMe: (token: string, data: UpdateProfileData) =>
    request<User>('/api/users/me', { method: 'PATCH', body: JSON.stringify(data) }, token),
}
