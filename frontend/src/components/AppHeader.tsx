import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { Brand } from './Brand'
import { ConfirmDialog } from './ConfirmDialog'

const link = ({ isActive }: { isActive: boolean }) =>
  `transition-colors ${isActive ? 'text-white' : 'text-muted hover:text-white'}`

export function AppHeader() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [confirming, setConfirming] = useState(false)

  const leave = () => {
    setConfirming(false)
    navigate('/')
    logout()
  }

  return (
    <>
      <header className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-x-6 gap-y-3 px-5 py-5 md:px-8">
        <Link to="/"><Brand /></Link>
        <nav className="flex items-center gap-4 text-[15px] sm:gap-5">
          <NavLink to="/catalogo" className={link}>Catálogo</NavLink>
          <NavLink to="/biblioteca" className={link}>Biblioteca</NavLink>
          <NavLink to="/perfil" className={link}>
            <span className="hidden sm:inline">@{user?.username}</span>
            <span className="sm:hidden">Perfil</span>
          </NavLink>
          <button onClick={() => setConfirming(true)}
            className="text-muted transition hover:text-white active:scale-[0.97]">
            Sair
          </button>
        </nav>
      </header>

      <ConfirmDialog
        open={confirming}
        title="Sair da conta?"
        description="Você precisará entrar de novo para ver seu diário."
        confirmLabel="Sair"
        onConfirm={leave}
        onCancel={() => setConfirming(false)}
      />
    </>
  )
}
