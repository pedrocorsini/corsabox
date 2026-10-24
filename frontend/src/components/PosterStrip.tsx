import { useEffect, useRef, type RefObject } from 'react'
import { KIND_LABEL, itemKey, type CatalogItem } from '../lib/catalog'

const TONES = ['bg-paper-2 text-ink', 'bg-accent text-ink', 'bg-ash text-white', 'bg-paper-3 text-ink', 'bg-surface text-white']

/** Arrastar com o mouse acompanha o ponteiro 1:1 e solta com inércia. No toque vale a rolagem nativa. */
function useDragScroll(ref: RefObject<HTMLUListElement | null>) {
  useEffect(() => {
    const el = ref.current
    if (!el) return
    let down = false, startX = 0, startLeft = 0, lastX = 0, lastT = 0, v = 0, raf = 0
    const glide = () => {
      el.scrollLeft += v
      v *= 0.94
      if (Math.abs(v) > 0.4) raf = requestAnimationFrame(glide)
    }
    const onDown = (e: PointerEvent) => {
      if (e.pointerType === 'touch') return
      cancelAnimationFrame(raf)
      down = true; startX = lastX = e.clientX; startLeft = el.scrollLeft; lastT = e.timeStamp; v = 0
      el.setPointerCapture(e.pointerId)
    }
    const onMove = (e: PointerEvent) => {
      if (!down) return
      el.scrollLeft = startLeft - (e.clientX - startX)
      const dt = Math.max(e.timeStamp - lastT, 1)
      v = 0.7 * v + 0.3 * ((lastX - e.clientX) / dt) * 16
      lastX = e.clientX; lastT = e.timeStamp
    }
    const onUp = () => {
      if (!down) return
      down = false
      raf = requestAnimationFrame(glide)
    }
    el.addEventListener('pointerdown', onDown)
    el.addEventListener('pointermove', onMove)
    el.addEventListener('pointerup', onUp)
    el.addEventListener('pointercancel', onUp)
    return () => {
      cancelAnimationFrame(raf)
      el.removeEventListener('pointerdown', onDown)
      el.removeEventListener('pointermove', onMove)
      el.removeEventListener('pointerup', onUp)
      el.removeEventListener('pointercancel', onUp)
    }
  }, [ref])
}

type Props = { items: CatalogItem[]; activeKey: string; onActive: (key: string) => void; onLeave: () => void }

export function PosterStrip({ items, activeKey, onActive, onLeave }: Props) {
  const ref = useRef<HTMLUListElement>(null)
  useDragScroll(ref)
  return (
    <ul ref={ref} aria-label="Em alta agora" onPointerLeave={onLeave}
      className="flex h-full cursor-grab select-none gap-3 overflow-x-auto px-5 active:cursor-grabbing md:gap-4 md:px-8 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
      {items.map((it, i) => {
        const key = itemKey(it)
        return (
          <li key={key} style={{ animationDelay: `${Math.min(i, 6) * 70}ms` }}
              onPointerEnter={() => onActive(key)} className="fade-in aspect-[2/3] h-full shrink-0 p-1">
            <div className={`h-full w-full overflow-hidden outline-3 outline-offset-2 transition-[transform,outline-color] duration-150 active:scale-[0.97] ${
              activeKey === key ? 'outline-accent' : 'outline-transparent'} ${it.posterUrl ? 'bg-surface' : TONES[i % TONES.length]}`}>
              {it.posterUrl ? (
                <img src={it.posterUrl} alt={it.title} draggable={false} loading="lazy" className="h-full w-full object-cover" />
              ) : (
                <div className="flex h-full flex-col justify-between p-4">
                  <p className="text-[15px]">{KIND_LABEL[it.kind]}{it.year ? ` · ${it.year}` : ''}</p>
                  <p className="wide text-xl md:text-2xl">{it.title}</p>
                </div>
              )}
            </div>
          </li>
        )
      })}
    </ul>
  )
}

export function PosterSkeleton() {
  return (
    <ul aria-hidden="true" className="flex h-full gap-3 overflow-hidden px-5 md:gap-4 md:px-8">
      {Array.from({ length: 8 }, (_, i) => (
        <li key={i} className="aspect-[2/3] h-full shrink-0 p-1">
          <div className="h-full w-full bg-surface motion-safe:animate-pulse" />
        </li>
      ))}
    </ul>
  )
}
