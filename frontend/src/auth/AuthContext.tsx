import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { api, ApiError, bindSession, tokenStore, type UpdateProfileData, type User } from '../lib/api'
import { AuthContext } from './auth-context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => tokenStore.get())
  const [user, setUser] = useState<User | null>(null)

  const clearSession = useCallback(() => {
    tokenStore.clear()
    setToken(null)
    setUser(null)
  }, [])

  // Saída pedida pelo usuário: revoga o refresh token no servidor e limpa o estado local.
  const logout = useCallback(() => {
    void api.logout().catch(() => {})
    clearSession()
  }, [clearSession])

  // Quando a renovação falha de vez, a sessão acabou.
  useEffect(() => {
    bindSession(clearSession)
    return () => bindSession(null)
  }, [clearSession])

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
          // Aqui um 401 já significa que a renovação também falhou.
          // Deploy, cold start e rede fora não derrubam a sessão: espera e tenta de novo.
          if (e instanceof ApiError && e.status === 401) {
            clearSession()
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
  }, [token, clearSession])

  const login = async (email: string, password: string) => {
    const { accessToken } = await api.login({ email, password })
    acceptSession(accessToken)
  }

  const acceptSession = (accessToken: string) => {
    setUser(null)
    tokenStore.set(accessToken)
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
    <AuthContext.Provider value={{ token, user, login, acceptSession, logout, updateProfile }}>{children}</AuthContext.Provider>
  )
}
