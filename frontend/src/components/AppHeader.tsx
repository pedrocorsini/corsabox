import { Link, NavLink, useMatch } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { profilePath } from '../lib/people'
import { Brand } from './Brand'

const link = ({ isActive }: { isActive: boolean }) =>
  `transition-colors ${isActive ? 'text-white' : 'text-muted hover:text-white'}`

export function AppHeader() {
  const { user } = useAuth()
  const ownProfile = useMatch('/perfil')
  const publicProfile = useMatch('/perfil/:username')
  const profileActive = Boolean(ownProfile || (user && publicProfile?.params.username?.toLowerCase() === user.username.toLowerCase()))

  return (
    <header className="mx-auto grid max-w-6xl grid-cols-2 items-center gap-x-6 gap-y-3 px-5 py-5 md:grid-cols-[1fr_auto_auto] md:px-8">
      <Link to="/"><Brand /></Link>
      <nav className="col-span-2 flex items-center gap-4 text-[15px] md:col-span-1 md:col-start-2 md:row-start-1 sm:gap-5">
        <NavLink to="/catalogo" className={link}>Catálogo</NavLink>
        <NavLink to="/biblioteca" className={link}>Biblioteca</NavLink>
        <NavLink to="/feed" className={link}>Feed</NavLink>
        <NavLink to="/amigos" className={link}>Amigos</NavLink>
      </nav>
      <Link to={user ? profilePath(user.username) : "/perfil"} aria-current={profileActive ? 'page' : undefined}
        className={`${link({ isActive: profileActive })} col-start-2 row-start-1 justify-self-end text-[15px] md:col-start-3`}>
        @{user?.username}
      </Link>
    </header>
  )
}
