import { UserRound } from 'lucide-react'
import { useProfile } from '../lib/profile'

export function UserAvatar({ userId, className = 'size-10' }: { userId: string; className?: string }) {
  const { data } = useProfile(userId)
  return <span aria-hidden="true" className={`inline-flex shrink-0 items-center justify-center overflow-hidden rounded-full bg-bg text-muted ${className}`}>
    {data?.photo ? <img src={data.photo} alt="" className="size-full object-cover" /> : <UserRound className="h-1/2 w-1/2" />}
  </span>
}
