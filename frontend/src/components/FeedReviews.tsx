import { useCallback, useEffect, useRef, useState } from 'react'
import { Loader2 } from 'lucide-react'
import { Link } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { api, pageInfo, type FeedReview } from '../lib/api'
import { profilePath } from '../lib/people'
import { useInfiniteScroll } from '../lib/useInfiniteScroll'
import { FeedReviewCard } from './FeedReviewCard'
import { Button } from './ui'

export function FeedReviews({ authorId, authorUsername }: { authorId?: string; authorUsername?: string }) {
  const { token } = useAuth()
  const [reviews, setReviews] = useState<FeedReview[]>([])
  const [page, setPage] = useState(0)
  const [more, setMore] = useState(false)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState(false)
  const [attempt, setAttempt] = useState(0)
  const busy = useRef(false)
  const generation = useRef(0)

  useEffect(() => {
    if (!token) return
    const current = ++generation.current
    api.feed(token, 0, authorId).then((data) => {
      if (current !== generation.current) return
      setReviews(data.content)
      setPage(0)
      setMore(pageInfo(data).hasMore)
      setError(false)
    }).catch(() => {
      if (current === generation.current) setError(true)
    }).finally(() => {
      if (current === generation.current) setLoading(false)
    })
    return () => { generation.current = current + 1 }
  }, [token, authorId, attempt])

  const loadMore = useCallback(async () => {
    if (!token || busy.current || loading || !more) return
    const current = generation.current
    busy.current = true
    setLoadingMore(true)
    setError(false)
    try {
      const data = await api.feed(token, page + 1, authorId)
      if (current !== generation.current) return
      setReviews((previous) => {
        const ids = new Set(previous.map((review) => review.entry.id))
        return [...previous, ...data.content.filter((review) => !ids.has(review.entry.id))]
      })
      setPage(page + 1)
      setMore(pageInfo(data).hasMore)
    } catch {
      if (current === generation.current) setError(true)
    } finally {
      busy.current = false
      if (current === generation.current) setLoadingMore(false)
    }
  }, [token, page, loading, more, authorId])

  const sentinel = useInfiniteScroll(!loading && !loadingMore && more && !error, loadMore)
  const from = authorUsername ? profilePath(authorUsername) : '/feed'

  return (
    <div aria-busy={loading || loadingMore} className="w-full">
      {loading && reviews.length === 0 && <p role="status" className="text-muted">Carregando reviews…</p>}
      {!loading && !error && reviews.length === 0 && <div className="fade-in space-y-3 bg-surface p-6">
        <h2 className="text-xl font-semibold">{authorId ? 'Nenhuma review publicada ainda.' : 'A primeira review pode ser sua.'}</h2>
        {!authorId && <>
          <p className="text-muted">Ao registrar um título como assistido, escreva sua review e escolha publicar no feed.</p>
          <Link to="/catalogo" className="inline-block font-medium text-accent hover:underline">Escolher um título</Link>
        </>}
      </div>}
      <ul className="space-y-4">
        {reviews.map((review, index) => <li key={review.entry.id} className="fade-in" style={{ animationDelay: `${Math.min(index % 20, 8) * 40}ms` }}>
          <FeedReviewCard review={review} from={from} />
        </li>)}
      </ul>
      {more && <div ref={sentinel} aria-hidden="true" className="h-px" />}
      {loadingMore && <div role="status" className="mt-10 flex min-h-10 justify-center">
        <Loader2 className="animate-spin text-muted" aria-hidden="true" /><span className="sr-only">Carregando mais reviews</span>
      </div>}
      {error && <div role="alert" className="fade-in mt-6 space-y-3">
        <p className="text-muted">Não foi possível carregar {reviews.length ? 'mais reviews' : 'as reviews'}.</p>
        <Button onClick={reviews.length ? loadMore : () => { setLoading(true); setError(false); setAttempt((value) => value + 1) }}>Tentar de novo</Button>
      </div>}
    </div>
  )
}
