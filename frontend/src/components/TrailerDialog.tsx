import { useEffect, useId, useRef } from 'react'
import { X } from 'lucide-react'

type Props = { open: boolean; title: string; videoKey: string; onClose: () => void }

export function TrailerDialog({ open, title, videoKey, onClose }: Props) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()

  useEffect(() => {
    const d = ref.current
    if (!d) return
    if (open && !d.open) d.showModal()
    if (!open && d.open) d.close()
  }, [open])

  return (
    <dialog ref={ref} className="sheet sheet-video" aria-labelledby={titleId}
      onCancel={(e) => { e.preventDefault(); onClose() }}
      onClick={(e) => { if (e.target === e.currentTarget) onClose() }}>
      <div className="p-4 md:p-6">
        <div className="mb-4 flex items-center justify-between gap-4">
          <h2 id={titleId} className="truncate text-xl font-semibold">Trailer de {title}</h2>
          <button type="button" onClick={onClose} aria-label="Fechar trailer"
            className="flex size-10 shrink-0 items-center justify-center text-muted transition enabled:hover:text-white active:scale-[0.97]">
            <X size={20} />
          </button>
        </div>
        <div className="aspect-video w-full bg-bg">
          {open && (
            <iframe
              src={`https://www.youtube-nocookie.com/embed/${videoKey}?autoplay=1&rel=0`}
              title={`Trailer de ${title}`}
              allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
              allowFullScreen
              className="h-full w-full border-0"
            />
          )}
        </div>
      </div>
    </dialog>
  )
}