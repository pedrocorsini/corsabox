import { UserAvatar } from './UserAvatar'
import { Link } from 'react-router'
import { profilePath } from '../lib/people'

export function UserLink({ user, compact = false }: { user: { id: string; username: string }; compact?: boolean }) {
  return (
    <Link to={profilePath(user.username)} className={`group inline-flex min-w-0 items-center ${compact ? 'gap-2 text-sm' : 'gap-3'}`}>
      <UserAvatar userId={user.id} className={compact ? 'size-7' : 'size-10'} />
      <span className="min-w-0 break-words font-semibold transition-colors group-hover:text-accent">@{user.username}</span>
    </Link>
  )
}
