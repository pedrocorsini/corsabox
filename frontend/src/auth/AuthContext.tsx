import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { api, ApiError, type UpdateProfileData, type User } from '../lib/api'

const TOKEN_KEY = 'corsabox.token'

type AuthState = {
  token: string | null
  user: User | null
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  updateProfile: (data: UpdateProfileData) => Promise<User>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY))
  const [user, setUser] = useState<User | null>(null)

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    setToken(null)
    setUser(null)
  }, [])

  useEffect(() => {
    if (!token) return
    let cancelled = false
    let timer: ReturnType<typeof setTimeout> | undefined

    const load = (attempt: number) => {
      api
        .me(token)
        .then((u) => {
          if (!cancelled) setUser(u)
        })
        .catch((e) => {
          if (cancelled) return
          // Só um 401 prova que o token acabou. Deploy, cold start e rede fora
          // não são motivo para derrubar a sessão: espera e tenta de novo.
          if (e instanceof ApiError && e.status === 401) {
            logout()
            return
          }
          timer = setTimeout(() => load(attempt + 1), Math.min(1000 * 2 ** attempt, 10_000))
        })
    }
    load(0)

    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [token, logout])

  const login = async (email: string, password: string) => {
    const { accessToken } = await api.login({ email, password })
    localStorage.setItem(TOKEN_KEY, accessToken)
    setToken(accessToken)
  }

  // O usuário devolvido pelo backend vira o estado global, então o header e o perfil se atualizam juntos.
  const updateProfile = async (data: UpdateProfileData) => {
    if (!token) throw new Error('Sem sessão ativa')
    const updated = await api.updateMe(token, data)
    setUser(updated)
    return updated
  }

  return (
    <AuthContext.Provider value={{ token, user, login, logout, updateProfile }}>{children}</AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
