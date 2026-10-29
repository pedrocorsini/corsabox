import { useRef, useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { api, ApiError, type Relationship } from '../lib/api'
import { Button } from './ui'
import { ConfirmDialog } from './ConfirmDialog'

export function FriendshipActions({ userId, relationship, onChange }: {
  userId: string; relationship: Relationship; onChange: (relationship: Relationship) => void
}) {
  const { token } = useAuth()
  const [pending, setPending] = useState<'request' | 'accept' | 'remove' | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [confirm, setConfirm] = useState(false)
  const busy = useRef(false)

  async function perform(action: 'request' | 'accept' | 'remove') {
    if (!token || busy.current) return
    busy.current = true
    setPending(action)
    setError(null)
    try {
      const result = await api.people[action](token, userId)
      setConfirm(false)
      onChange(result.relationship)
    } catch (error) {
      setError(error instanceof ApiError ? error.message : 'Não foi possível concluir. Tente de novo.')
      if (error instanceof ApiError && (error.status === 409 || error.status === 404)) {
        try { onChange((await api.people.profile(token, userId)).relationship) } catch { /* Keep the original error visible. */ }
      }
      setConfirm(false)
    } finally {
      busy.current = false
      setPending(null)
    }
  }

  if (relationship === 'SELF') return <Link to="/perfil" className="inline-flex min-h-12 shrink-0 items-center justify-center bg-bg px-5 text-sm font-semibold text-white transition-colors hover:bg-line focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent motion-safe:active:scale-[0.97]">Minha conta</Link>

  return (
    <div>
      <div className="flex flex-wrap items-center gap-2" aria-busy={pending !== null}>
        {relationship === 'NONE' && <Button variant="primary" loading={pending === 'request'} disabled={pending !== null}
          onClick={() => void perform('request')}>Adicionar amigo</Button>}
        {relationship === 'REQUEST_SENT' && <>
          <span className="text-sm text-muted">Solicitação enviada</span>
          <Button loading={pending === 'remove'} disabled={pending !== null} onClick={() => void perform('remove')}>Cancelar solicitação</Button>
        </>}
        {relationship === 'REQUEST_RECEIVED' && <>
          <Button variant="primary" loading={pending === 'accept'} disabled={pending !== null} onClick={() => void perform('accept')}>Aceitar</Button>
          <Button loading={pending === 'remove'} disabled={pending !== null} onClick={() => void perform('remove')}>Recusar</Button>
        </>}
        {relationship === 'FRIENDS' && <>
          <span className="text-sm text-muted">Amigos</span>
          <Button disabled={pending !== null} onClick={() => setConfirm(true)}>Remover amigo</Button>
        </>}
      </div>
      {error && <p role="alert" className="mt-2 max-w-sm text-sm text-red-300">{error}</p>}
      <ConfirmDialog open={confirm} title="Remover amizade?" description="Vocês poderão enviar uma nova solicitação depois."
        confirmLabel="Remover" busy={pending !== null} onCancel={() => setConfirm(false)} onConfirm={() => void perform('remove')} />
    </div>
  )
}
