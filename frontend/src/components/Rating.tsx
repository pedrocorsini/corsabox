import { useRef, useState, type KeyboardEvent, type PointerEvent } from 'react'
import { ratingText } from '../lib/format'

/** Altura de cada barra: sobe de 30% a 100% ao longo das 10 meias unidades. */
const barHeight = (i: number) => `${30 + i * 7.8}%`

function Bars({ filled, size }: { filled: number; size: 'sm' | 'md' }) {
  const w = size === 'sm' ? 'w-1.5' : 'w-4'
  return (
    <>
      {Array.from({ length: 5 }, (_, g) => (
        <span key={g} className={`flex h-full items-end ${size === 'sm' ? 'gap-px' : 'gap-[3px]'}`}>
          {[0, 1].map((h) => {
            const i = g * 2 + h
            return (
              <span key={i} style={{ height: barHeight(i) }}
                className={`${w} transition-colors duration-100 ${i < filled ? 'bg-accent' : 'bg-line'}`} />
            )
          })}
        </span>
      ))}
    </>
  )
}

/** Leitura apenas. */
export function RatingMeter({ value, size = 'md' }: { value: number; size?: 'sm' | 'md' }) {
  return (
    <span role="img" aria-label={`Nota ${ratingText(value)} de 5`} className="inline-flex items-center gap-3">
      <span className={`flex items-end ${size === 'sm' ? 'h-4 gap-1' : 'h-6 gap-1.5'}`}>
        <Bars filled={value * 2} size={size} />
      </span>
      <span className={`font-semibold tabular-nums ${size === 'sm' ? 'text-[15px]' : 'text-xl'}`}>{ratingText(value)}</span>
    </span>
  )
}

type InputProps = { value: number | null; onChange: (v: number | null) => void; labelledBy?: string }

/** Nota de 0 a 5 em passos de 0,5. `null` significa sem nota. */
export function RatingInput({ value, onChange, labelledBy }: InputProps) {
  const ref = useRef<HTMLDivElement>(null)
  const [hover, setHover] = useState<number | null>(null)
  const press = useRef<{ start: number | null; first: number; last: number; moved: boolean } | null>(null)

  const fromX = (clientX: number) => {
    const r = ref.current!.getBoundingClientRect()
    const ratio = (clientX - r.left) / r.width
    return Math.min(5, Math.max(0.5, Math.ceil(ratio * 10) / 2))
  }

  const onPointerDown = (e: PointerEvent<HTMLDivElement>) => {
    e.currentTarget.setPointerCapture(e.pointerId)
    const v = fromX(e.clientX)
    press.current = { start: value, first: v, last: v, moved: false }
    onChange(v)
  }

  const onPointerMove = (e: PointerEvent<HTMLDivElement>) => {
    const p = press.current
    const v = fromX(e.clientX)
    if (p) {
      if (v !== p.last) {
        p.last = v
        p.moved = true
        onChange(v)
      }
    } else if (e.pointerType === 'mouse') {
      setHover(v)
    }
  }

  // Tocar de novo na nota que já estava marcada limpa a nota.
  const onPointerUp = () => {
    const p = press.current
    press.current = null
    if (p && !p.moved && p.start === p.first) onChange(null)
  }

  const onKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    let next: number | null | undefined
    switch (e.key) {
      case 'ArrowRight':
      case 'ArrowUp':
        next = Math.min(5, value === null ? 0.5 : value + 0.5)
        break
      case 'ArrowLeft':
      case 'ArrowDown':
        next = Math.max(0, (value ?? 0) - 0.5)
        break
      case 'Home': next = 0; break
      case 'End': next = 5; break
      case 'Backspace':
      case 'Delete': next = null; break
    }
    if (next === undefined) return
    e.preventDefault()
    onChange(next)
  }

  const shown = hover ?? value

  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-3">
      <button type="button" aria-pressed={value === 0} aria-label="Nota zero" onClick={() => onChange(value === 0 ? null : 0)}
        className={`size-10 shrink-0 font-semibold tabular-nums transition active:scale-[0.97] ${
          value === 0 ? 'bg-accent text-ink' : 'bg-bg text-muted hover:text-white'}`}>
        0
      </button>
      <div ref={ref} role="slider" tabIndex={0} aria-labelledby={labelledBy}
        aria-valuemin={0} aria-valuemax={5} aria-valuenow={value ?? 0}
        aria-valuetext={value === null ? 'Sem nota' : `${ratingText(value)} de 5`}
        onKeyDown={onKeyDown} onPointerDown={onPointerDown} onPointerMove={onPointerMove}
        onPointerUp={onPointerUp} onPointerCancel={onPointerUp} onPointerLeave={() => setHover(null)}
        className="flex h-10 cursor-pointer touch-pan-y select-none items-end gap-1.5">
        <Bars filled={(shown ?? 0) * 2} size="md" />
      </div>
      <p aria-hidden="true" className="min-w-[4ch] text-xl font-semibold tabular-nums">
        {shown === null ? '–' : ratingText(shown)}
      </p>
      {value !== null && (
        <button type="button" onClick={() => onChange(null)}
          className="text-sm text-muted transition-colors hover:text-white">
          Limpar
        </button>
      )}
    </div>
  )
}