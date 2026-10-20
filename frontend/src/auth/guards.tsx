import { Navigate, Outlet } from 'react-router'
import { Loader2 } from 'lucide-react'
import { useAuth } from './AuthContext'

export function RequireAuth() {
  const { token, user } = useAuth()
  if (!token) return <Navigate to="/login" replace />
  if (!user)
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="animate-spin text-accent" />
      </div>
    )
  return <Outlet />
}

export function GuestOnly() {
  const { token } = useAuth()
  return token ? <Navigate to="/" replace /> : <Outlet />
}