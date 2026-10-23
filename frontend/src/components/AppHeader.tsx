import { Link, NavLink } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { Brand } from './Brand'

const link = ({ isActive }: { isActive: boolean }) =>
  `transition-colors ${isActive ? 'text-white' : 'text-muted hover:text-white'}`

export function AppHeader() {
  const { user, logout } = useAuth()
  return (
    <header className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-5 py-5 md:px-8">
      <Link to="/" className="shrink-0"><Brand /></Link>
      <nav className="flex items-center gap-4 text-[15px] sm:gap-5">
        <NavLink to="/catalogo" className={link}>Catálogo</NavLink>
        <NavLink to="/perfil" className={link}>
          <span className="hidden sm:inline">@{user?.username}</span>
          <span className="sm:hidden">Perfil</span>
        </NavLink>
        <button onClick={logout} className="text-muted transition-colors hover:text-white">Sair</button>
      </nav>
    </header>
  )
} 