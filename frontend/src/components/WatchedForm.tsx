import { useId, useState } from 'react'
import { todayISO } from '../lib/format'
import { RatingInput } from './Rating'
import { Button, SubmitButton } from './ui'

export type WatchedValues = { rating: number | null; watchedOn: string; review: string; publishToFeed?: boolean; commentsEnabled?: boolean }
export type WatchedPayload = { rating: number | null; watchedOn: string | null; review: string | null; publishToFeed?: boolean; commentsEnabled?: boolean }

type Props = {
  initial: WatchedValues
  submitLabel: string
  busy: boolean
  onSubmit: (data: WatchedPayload) => void
  onCancel: () => void
  allowPublication?: boolean
}

const MAX_REVIEW = 5000
const input = 'w-full bg-bg px-3.5 py-3 text-white outline-2 outline-transparent transition placeholder:text-muted focus:outline-accent [color-scheme:dark]'

export function WatchedForm({ initial, submitLabel, busy, onSubmit, onCancel, allowPublication = false }: Props) {
  const [rating, setRating] = useState(initial.rating)
  const [watchedOn, setWatchedOn] = useState(initial.watchedOn)
  const [review, setReview] = useState(initial.review)
  const [publishToFeed, setPublishToFeed] = useState(initial.publishToFeed ?? false)
  const [commentsEnabled, setCommentsEnabled] = useState(initial.commentsEnabled ?? true)
  const ratingId = useId()
  const dateId = useId()
  const reviewId = useId()

  return (
    <form
      noValidate
      onSubmit={(e) => {
        e.preventDefault()
        onSubmit({ rating, watchedOn: watchedOn || null, review: review.trim() || null,
          ...(allowPublication ? { publishToFeed: publishToFeed && !!review.trim(), commentsEnabled } : {}) })
      }}
      className="space-y-6"
    >
      <div>
        <p id={ratingId} className="mb-2 text-sm font-medium text-muted">Nota</p>
        <RatingInput value={rating} onChange={setRating} labelledBy={ratingId} />
      </div>

      <div>
        <label htmlFor={dateId} className="mb-1.5 block text-sm font-medium text-muted">Assistido em</label>
        <div className="flex flex-wrap items-center gap-3">
          <input id={dateId} type="date" value={watchedOn} max={todayISO()}
            onChange={(e) => setWatchedOn(e.target.value)} className={`${input} sm:w-auto`} />
          <button type="button" onClick={() => setWatchedOn(todayISO())}
            className="text-sm font-medium text-accent transition active:scale-[0.97] hover:underline">
            Hoje
          </button>
          {watchedOn && (
            <button type="button" onClick={() => setWatchedOn('')}
              className="text-sm text-muted transition-colors hover:text-white">
              Sem data
            </button>
          )}
        </div>
      </div>

      <div>
        <label htmlFor={reviewId} className="mb-1.5 block text-sm font-medium text-muted">Review</label>
        <textarea id={reviewId} rows={5} maxLength={MAX_REVIEW} value={review}
          onChange={(e) => setReview(e.target.value)}
          placeholder="O que você achou? Opcional."
          className={`${input} resize-y`} />
        <p className="mt-1.5 text-right text-sm tabular-nums text-muted">{review.length}/{MAX_REVIEW}</p>
      </div>

      {allowPublication && (
        <fieldset disabled={busy} className="space-y-3">
          <legend className="mb-3 text-sm font-medium text-muted">Publicação</legend>
          <label className="flex items-center gap-3">
            <input type="checkbox" checked={publishToFeed} disabled={!review.trim()}
              onChange={(e) => setPublishToFeed(e.target.checked)} className="size-4 accent-accent" />
            Publicar review no feed
          </label>
          <p className="text-sm text-muted">Sua review será visível para outros usuários. Sem texto, o registro fica privado.</p>
          <label className="flex items-center gap-3">
            <input type="checkbox" checked={commentsEnabled}
              onChange={(e) => setCommentsEnabled(e.target.checked)} className="size-4 accent-accent" />
            Permitir comentários
          </label>
        </fieldset>
      )}

      <div className="flex flex-wrap items-center gap-3">
        <SubmitButton loading={busy} className="w-full sm:w-auto">{submitLabel}</SubmitButton>
        <Button onClick={onCancel} disabled={busy} className="bg-transparent text-muted enabled:hover:bg-transparent enabled:hover:text-white">
          Cancelar
        </Button>
      </div>
    </form>
  )
}
