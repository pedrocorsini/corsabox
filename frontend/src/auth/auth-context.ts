import { createContext } from 'react'
import type { UpdateProfileData, User } from '../lib/api'

export type AuthState = {
  token: string | null
  user: User | null
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  updateProfile: (data: UpdateProfileData) => Promise<User>
}

export const AuthContext = createContext<AuthState | null>(null)
