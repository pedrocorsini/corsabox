import { Clapperboard, LogOut } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'

export default function HomePage() {
  const { user, logout } = useAuth()
  return (
    <div className="min-h-screen">
      <header className="border-b border-line">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-2 text-lg font-bold">
            <Clapperboard className="text-accent" size={22} />
            CorsaBox
          </div>
          <button onClick={logout}
            className="flex items-center gap-2 text-sm text-muted transition hover:text-white">
            <LogOut size={16} />
            Sair
          </button>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-6 py-16">
        <h1 className="text-3xl font-semibold">Olá, {user?.firstName}</h1>
        <p className="mt-2 text-muted">@{user?.username} · Seu catálogo vai aparecer aqui.</p>
      </main>
    </div>
  )
}