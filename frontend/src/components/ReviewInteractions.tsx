import { useEffect, useId, useRef, useState } from 'react'
import { Loader2, MessageCircle, ThumbsDown, ThumbsUp, Trash2 } from 'lucide-react'
import { useAuth } from '../auth/useAuth'
import { api, ApiError, pageInfo, type FeedComment, type Reaction, type ReactionSummary } from '../lib/api'
import { ConfirmDialog } from './ConfirmDialog'
import { Button } from './ui'
import { UserLink } from './UserLink'

const message = (error: unknown) => error instanceof ApiError ? error.message : 'Não foi possível concluir. Tente de novo.'
const action = 'inline-flex min-h-11 items-center justify-center gap-2 px-3 text-sm transition-colors hover:text-white disabled:opacity-50'

function CommentText({ text }: { text: string }) {
  const id = useId()
  const ref = useRef<HTMLParagraphElement>(null)
  const [expanded, setExpanded] = useState(false)
  const [overflowing, setOverflowing] = useState(false)

  useEffect(() => {
    const element = ref.current
    if (!element) return
    const observer = new ResizeObserver(() => {
      const fourLines = parseFloat(getComputedStyle(element).lineHeight) * 4
      setOverflowing(element.scrollHeight > fourLines + 1)
    })
    observer.observe(element)
    return () => observer.disconnect()
  }, [text])

  return (
    <div className="mt-3">
      <p ref={ref} id={id} className={`whitespace-pre-wrap break-words text-base leading-relaxed [overflow-wrap:anywhere] ${expanded ? '' : 'line-clamp-4'}`}>{text}</p>
      {overflowing && <button type="button" aria-expanded={expanded} aria-controls={id}
        onClick={() => setExpanded((value) => !value)}
        className="inline-flex min-h-11 items-center text-sm font-medium text-accent transition-opacity hover:underline active:opacity-70">
        {expanded ? 'Ler menos' : 'Ler mais'}
      </button>}
    </div>
  )
}

function ReactionButtons({ initial, reviewId, commentId }: {
  initial: ReactionSummary; reviewId: string; commentId?: string
}) {
  const { token } = useAuth()
  const [summary, setSummary] = useState(initial)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const busy = useRef(false)

  async function react(value: Reaction) {
    if (!token || busy.current) return
    busy.current = true
    setPending(true)
    setError(null)
    try {
      setSummary(await api.feedInteractions.react(token, reviewId, summary.myReaction === value ? null : value, commentId))
    } catch (error) {
      setError(message(error))
    } finally {
      busy.current = false
      setPending(false)
    }
  }

  return (
    <div>
      <div className="flex items-center" role="group" aria-label={commentId ? 'Reações ao comentário' : 'Reações à review'} aria-busy={pending}>
        {([{ value: 'LIKE', label: 'Curtir', Icon: ThumbsUp, count: summary.likes },
          { value: 'DISLIKE', label: 'Não curtir', Icon: ThumbsDown, count: summary.dislikes }] as const).map(({ value, label, Icon, count }) => (
          <button key={value} type="button" aria-label={`${label} ${commentId ? 'comentário' : 'review'} (${count})`}
            aria-pressed={summary.myReaction === value} disabled={pending} onClick={() => void react(value)}
            className={`${action} ${summary.myReaction === value ? 'text-accent' : 'text-muted'}`}>
            <Icon size={17} aria-hidden="true" fill={summary.myReaction === value ? 'currentColor' : 'none'} />
            <span className="tabular-nums">{count}</span>
          </button>
        ))}
      </div>
      {error && <p role="alert" className="mt-1 max-w-sm text-sm text-red-300">{error}</p>}
    </div>
  )
}

function Comments({ reviewId, enabled, onCountChange }: {
  reviewId: string; enabled: boolean; onCountChange: (change: number) => void
}) {
  const { token, user } = useAuth()
  const inputId = useId()
  const [items, setItems] = useState<FeedComment[]>([])
  const [page, setPage] = useState(0)
  const [more, setMore] = useState(false)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<number | null>(null)
  const [attempt, setAttempt] = useState(0)
  const [text, setText] = useState('')
  const [sending, setSending] = useState(false)
  const [closed, setClosed] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [deleting, setDeleting] = useState(false)
  const [remove, setRemove] = useState<string | null>(null)
  const busy = useRef(false)
  const listBusy = useRef(false)

  useEffect(() => {
    if (!token) return
    let cancelled = false
    api.feedInteractions.comments(token, reviewId).then((data) => {
      if (cancelled) return
      setItems(data.content)
      setPage(0)
      setMore(pageInfo(data).hasMore)
      setLoadError(null)
    }).catch(() => {
      if (!cancelled) setLoadError(0)
    }).finally(() => {
      if (!cancelled) setLoading(false)
    })
    return () => { cancelled = true }
  }, [token, reviewId, attempt])

  async function load(next: number) {
    if (!token || listBusy.current) return
    listBusy.current = true
    setLoading(true)
    setLoadError(null)
    try {
      const data = await api.feedInteractions.comments(token, reviewId, next)
      setItems((previous) => {
        const ids = new Set(previous.map((comment) => comment.id))
        return next === 0 ? data.content : [...previous, ...data.content.filter((comment) => !ids.has(comment.id))]
      })
      setPage(next)
      setMore(pageInfo(data).hasMore)
    } catch {
      setLoadError(next)
    } finally {
      listBusy.current = false
      setLoading(false)
    }
  }

  async function submit() {
    if (!token || busy.current || !text.trim()) return
    busy.current = true
    setSending(true)
    setError(null)
    try {
      const comment = await api.feedInteractions.addComment(token, reviewId, text.trim())
      setItems((previous) => [comment, ...previous])
      setText('')
      onCountChange(1)
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) setClosed(true)
      setError(message(error))
    } finally {
      busy.current = false
      setSending(false)
    }
  }

  async function deleteComment() {
    if (!token || !remove || busy.current) return
    busy.current = true
    setDeleting(true)
    setError(null)
    try {
      await api.feedInteractions.deleteComment(token, reviewId, remove)
      setItems((previous) => previous.filter((comment) => comment.id !== remove))
      onCountChange(-1)
      setRemove(null)
      // Reload the first page so deletion cannot shift an unseen comment past the pagination offset.
      await load(0)
    } catch (error) {
      setError(message(error))
      setRemove(null)
    } finally {
      busy.current = false
      setDeleting(false)
    }
  }

  return (
    <section className="notice-in mt-4 space-y-5" aria-label="Comentários">
      {enabled && !closed ? (
        <form onSubmit={(event) => { event.preventDefault(); void submit() }} className="space-y-3">
          <label htmlFor={inputId} className="block text-sm font-medium">Escreva um comentário</label>
          <textarea id={inputId} value={text} onChange={(event) => setText(event.target.value)} maxLength={2000}
            rows={3} disabled={sending} aria-describedby={`${inputId}-limit`}
            className="w-full resize-none bg-bg p-3 text-base outline-2 outline-transparent placeholder:text-muted focus:outline-accent"
            placeholder="O que você achou dessa review?" />
          <div className="flex flex-wrap items-center justify-between gap-3">
            <p id={`${inputId}-limit`} className="text-sm tabular-nums text-muted">{text.length}/2000</p>
            <Button type="submit" variant="primary" loading={sending} disabled={!text.trim() || loading || deleting}>Comentar</Button>
          </div>
        </form>
      ) : <p className="text-sm text-muted">O autor desativou novos comentários nesta review.</p>}
      {error && <p role="alert" className="text-sm text-red-300">{error}</p>}
      <div aria-busy={loading}>
        {!loading && loadError === null && items.length === 0 && <p className="text-sm text-muted">Nenhum comentário ainda.</p>}
        <ul className="space-y-4">
          {items.map((comment) => (
            <li key={comment.id} className="fade-in bg-bg p-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <UserLink user={comment.author} compact />
                <time dateTime={comment.createdAt} className="text-xs text-muted">
                  {new Date(comment.createdAt).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })}
                </time>
              </div>
              <CommentText text={comment.text} />
              <div className="-ml-3 mt-2 flex flex-wrap items-start justify-between gap-2">
                <ReactionButtons initial={comment.reactions} reviewId={reviewId} commentId={comment.id} />
                {comment.author.id === user?.id && <button type="button" onClick={() => setRemove(comment.id)} disabled={deleting || loading || sending}
                  className={`${action} text-muted`}><Trash2 size={15} aria-hidden="true" />Excluir</button>}
              </div>
            </li>
          ))}
        </ul>
        {loading && <p role="status" className="mt-4 flex items-center gap-2 text-sm text-muted"><Loader2 size={16} className="animate-spin" aria-hidden="true" />Carregando comentários…</p>}
        {loadError !== null && <div role="alert" className="mt-4 space-y-2">
          <p className="text-sm text-muted">Não foi possível carregar os comentários.</p>
          <Button disabled={deleting || sending} onClick={() => { if (loadError === 0 && items.length === 0) { setLoading(true); setAttempt((value) => value + 1) } else void load(loadError) }}>Tentar de novo</Button>
        </div>}
        {more && !loading && loadError === null && <Button className="mt-4" disabled={deleting || sending} onClick={() => void load(page + 1)}>Mais comentários</Button>}
      </div>
      <ConfirmDialog open={remove !== null} title="Excluir comentário?" description="O comentário e suas reações serão apagados."
        confirmLabel="Excluir" busy={deleting} onCancel={() => setRemove(null)} onConfirm={() => void deleteComment()} />
    </section>
  )
}

export function ReviewInteractions({ reviewId, reactions, commentCount, commentsEnabled }: {
  reviewId: string; reactions: ReactionSummary; commentCount: number; commentsEnabled: boolean
}) {
  const [open, setOpen] = useState(false)
  const [count, setCount] = useState(commentCount)
  const panelId = useId()
  return (
    <div className="mt-5">
      <div className="-ml-3 flex flex-wrap items-start gap-x-2">
        <ReactionButtons initial={reactions} reviewId={reviewId} />
        <button type="button" aria-expanded={open} aria-controls={panelId} onClick={() => setOpen((value) => !value)}
          className={`${action} ${open ? 'text-white' : 'text-muted'}`}>
          <MessageCircle size={17} aria-hidden="true" /><span>{count} {count === 1 ? 'comentário' : 'comentários'}</span>
        </button>
      </div>
      <div id={panelId}>
        {open && <Comments reviewId={reviewId} enabled={commentsEnabled} onCountChange={(change) => setCount((value) => Math.max(0, value + change))} />}
      </div>
    </div>
  )
}
