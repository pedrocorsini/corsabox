import { Link } from 'react-router'
import { useAuth } from '../auth/useAuth'
import type { FeedReview } from '../lib/api'
import { metaLine, titlePath } from '../lib/catalog'
import { Poster } from './Poster'
import { RatingMeter } from './Rating'
import { ReviewInteractions } from './ReviewInteractions'
import { UserLink } from './UserLink'

export function FeedReviewCard({ review: { author, entry, reactions, commentCount }, from }: { review: FeedReview; from: string }) {
  const { user } = useAuth()
  return (
    <article className="bg-surface p-5 md:p-6">
      <header className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <UserLink user={author} />
        {entry.publishedAt && <time dateTime={entry.publishedAt} className="text-sm text-muted">
          {new Date(entry.publishedAt).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })}
        </time>}
      </header>
      <div className="grid grid-cols-[6rem_minmax(0,1fr)] gap-x-4 gap-y-5 sm:grid-cols-[9rem_minmax(0,1fr)] sm:gap-x-6">
        <Link to={titlePath(entry.media)} state={{ from }}
          className="self-start transition-transform active:scale-[0.97] motion-reduce:transform-none sm:row-span-3">
          <Poster item={entry.media} compact />
        </Link>
        <div className="min-w-0 self-center sm:self-start">
          <Link to={titlePath(entry.media)} state={{ from }}
            className="text-xl font-semibold leading-tight tracking-tight transition-colors hover:text-accent sm:text-2xl">
            <h2>{entry.media.title}</h2>
          </Link>
          <p className="mt-2 text-sm leading-relaxed text-muted">{metaLine(entry.media)}</p>
          {entry.rating !== undefined && <div className="mt-3"><RatingMeter value={entry.rating} size="sm" /></div>}
        </div>
        <p className="col-span-2 max-w-[70ch] whitespace-pre-wrap break-words text-base leading-relaxed [overflow-wrap:anywhere] sm:col-span-1 sm:col-start-2">{entry.review}</p>
        {author.id === user?.id && <Link to={`${titlePath(entry.media)}?assistir=1`} state={{ from }}
          className="col-span-2 justify-self-start text-sm font-medium text-accent hover:underline sm:col-span-1 sm:col-start-2">Editar publicação</Link>}
      </div>
      <ReviewInteractions reviewId={entry.id} reactions={reactions} commentCount={commentCount} commentsEnabled={entry.commentsEnabled} />
    </article>
  )
}
