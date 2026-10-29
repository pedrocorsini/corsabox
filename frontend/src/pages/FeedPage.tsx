import { AppHeader } from '../components/AppHeader'
import { FeedReviews } from '../components/FeedReviews'
import { useScrollEffects } from '../lib/motion'

export default function FeedPage() {
  useScrollEffects()
  return (
    <div className="min-h-svh bg-bg text-white">
      <AppHeader />
      <main className="mx-auto w-full max-w-5xl px-5 pb-24 pt-8 sm:px-8 sm:pt-12">
        <h1 data-reveal className="text-4xl font-semibold md:text-5xl">Feed</h1>
        <p data-reveal style={{ transitionDelay: '80ms' }} className="mt-4 max-w-[64ch] text-muted">As últimas reviews da comunidade. Descubra o que outras pessoas estão assistindo e comentando.</p>
        <div className="mt-8"><FeedReviews /></div>
      </main>
    </div>
  )
}
