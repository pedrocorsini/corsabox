import { ArrowLeft } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { UserAvatar } from '../components/UserAvatar'
import { ProfileFavoritesSection } from '../components/ProfileFavoritesEditor'
import { useProfile } from '../lib/profile'
import { useAuth } from '../auth/useAuth'
import { AppHeader } from '../components/AppHeader'
import { FeedReviews } from '../components/FeedReviews'
import { FriendshipActions } from '../components/FriendshipActions'
import { Button } from '../components/ui'
import { api, ApiError, type Person } from '../lib/api'
import { useScrollEffects } from '../lib/motion'

function Profile({ username }: { username: string }) {
  useScrollEffects()
  const { token, user } = useAuth()
  const ownProfile = user?.username.toLowerCase() === username.toLowerCase()
  const [person, setPerson] = useState<Person | null>(null)
  const [failure, setFailure] = useState<'missing' | 'network' | null>(null)
  const [attempt, setAttempt] = useState(0)
  const userId = person?.user.id ?? ''
  const customization = useProfile(userId)

  useEffect(() => {
    if (!token) return
    let cancelled = false
    api.people.byUsername(token, username).then((data) => {
      if (!cancelled) setPerson(data)
    }).catch((error) => {
      if (!cancelled) setFailure(error instanceof ApiError && (error.status === 404 || error.status === 400) ? 'missing' : 'network')
    })
    return () => { cancelled = true }
  }, [token, username, attempt])

  return (
    <div className="min-h-svh bg-bg text-white">
      <AppHeader />
      <main className="mx-auto w-full max-w-5xl px-5 pb-24 pt-8 sm:px-8 sm:pt-12">
        {!ownProfile && <Link to="/amigos" className="inline-flex min-h-11 items-center gap-2 px-2 text-sm text-muted transition-colors hover:bg-surface hover:text-white focus-visible:outline-2 focus-visible:outline-accent motion-safe:active:scale-[0.97]">
          <ArrowLeft size={16} aria-hidden="true" />Pessoas e amizades
        </Link>}
        <h1 data-reveal className="mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">Perfil público</h1>
        {!person && !failure && <p role="status" className="mt-8 text-muted">Carregando perfil…</p>}
        {failure && <div role="alert" className="mt-8 space-y-4">
          <p className="text-muted">{failure === 'missing' ? 'Este perfil não foi encontrado.' : 'Não foi possível carregar o perfil.'}</p>
          {failure === 'network' && <Button onClick={() => { setFailure(null); setAttempt((value) => value + 1) }}>Tentar de novo</Button>}
        </div>}
        {person && <>
          <section className="fade-in mt-6 bg-surface p-6 sm:p-8" aria-label="Informações públicas">
            <div className="flex flex-col items-center gap-6 md:flex-row md:justify-between">
              <div className="flex min-w-0 max-w-full flex-col items-center gap-4 text-center sm:flex-1 sm:flex-row sm:gap-5 sm:text-left">
                <UserAvatar userId={userId} className="size-20 sm:size-24" />
                <div className="min-w-0 max-w-full">
                  <h2 className="break-words text-xl font-semibold leading-tight tracking-tight sm:text-2xl">@{person.user.username}</h2>
                  <p className="mt-1 break-words text-muted">{person.user.firstName} {person.user.lastName}</p>
                  {customization.data?.bio && <p className="mt-3 max-w-prose whitespace-pre-wrap break-words text-sm">{customization.data.bio}</p>}
                  <p className="mt-2 text-sm text-muted">Na CorsaBox desde {new Date(person.user.createdAt).toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })}</p>
                </div>
              </div>
              <FriendshipActions userId={person.user.id} relationship={person.relationship}
                onChange={(relationship) => setPerson((current) => current ? { ...current, relationship } : current)} />
            </div>
            <div className="mt-8 sm:mt-10">
              {customization.data && <ProfileFavoritesSection initial={customization.data} editable={ownProfile} />}
              {customization.error && <div role="alert">Não foi possível carregar a personalização. <Button onClick={customization.retry}>Tentar de novo</Button></div>}
            </div>
          </section>
          <section className="mt-10" aria-labelledby="profile-reviews">
            <h2 id="profile-reviews" className="mb-6 text-xl font-semibold">Reviews publicadas</h2>
            <FeedReviews authorId={person.user.id} authorUsername={person.user.username} />
          </section>
        </>}
      </main>
    </div>
  )
}

export default function PublicProfilePage() {
  const { username = '' } = useParams()
  return <Profile key={username} username={username} />
}
