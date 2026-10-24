import { useEffect, useId, useRef } from 'react'
import { Button } from './ui'

type Props = {
  open: boolean
  title: string
  description: string
  confirmLabel: string
  busy?: boolean
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDialog({ open, title, description, confirmLabel, busy = false, onConfirm, onCancel }: Props) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  const descId = useId()

  useEffect(() => {
    const d = ref.current
    if (!d) return
    if (open && !d.open) d.showModal()
    if (!open && d.open) d.close()
  }, [open])

  return (
    <dialog ref={ref} className="sheet" aria-labelledby={titleId} aria-describedby={descId}
      // Esc: o estado manda, o efeito fecha.
      onCancel={(e) => { e.preventDefault(); if (!busy) onCancel() }}
      onClick={(e) => { if (e.target === e.currentTarget && !busy) onCancel() }}>
      <div className="p-6 md:p-8">
        <h2 id={titleId} className="text-xl font-semibold">{title}</h2>
        <p id={descId} className="mt-2 text-muted">{description}</p>
        <div className="mt-8 flex flex-wrap justify-end gap-3">
          <Button autoFocus onClick={onCancel} disabled={busy} className="bg-bg enabled:hover:bg-line">Cancelar</Button>
          <Button variant="primary" loading={busy} onClick={onConfirm}>{confirmLabel}</Button>
        </div>
      </div>
    </dialog>
  )
}