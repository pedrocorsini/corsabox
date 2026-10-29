import { useId, useState } from 'react'
import { Link } from 'react-router'
import { favoriteKinds } from '../lib/profile'
import { Poster } from './Poster'
import { titlePath } from '../lib/catalog'
import type { Favorite } from '../lib/api'

type Kind = Favorite['media']['kind']
type Props = { entries: Favorite[]; onRemove?: (entry: Favorite) => void; kind?: Kind; onKindChange?: (kind: Kind) => void }

export function ProfileFavorites({ entries, onRemove, kind, onKindChange }: Props) {
  const [localKind, setLocalKind] = useState<Kind>('movie')
  const selectedKind = kind ?? localKind
  const id = useId()
  const favorites = entries.filter(entry => entry.media.kind === selectedKind)
  const select = (next: Kind) => { setLocalKind(next); onKindChange?.(next) }
  return <div>
    <div className="flex gap-4 sm:gap-6" role="tablist" aria-label="Favoritos" onKeyDown={event => {
      const index = favoriteKinds.findIndex(item => item.kind === selectedKind)
      const next = event.key === 'ArrowRight' ? (index + 1) % 3 : event.key === 'ArrowLeft' ? (index + 2) % 3 : event.key === 'Home' ? 0 : event.key === 'End' ? 2 : -1
      if (next < 0) return
      event.preventDefault()
      select(favoriteKinds[next].kind)
      document.getElementById(`${id}-${favoriteKinds[next].kind}`)?.focus()
    }}>
      {favoriteKinds.map(({ kind: tabKind, label }) => <button key={tabKind} id={`${id}-${tabKind}`} type="button" role="tab"
        aria-selected={selectedKind === tabKind} aria-controls={`${id}-panel`} tabIndex={selectedKind === tabKind ? 0 : -1}
        onClick={() => select(tabKind)}
        className={`flex min-h-11 items-center justify-center gap-1.5 border-b-2 py-2 text-base transition-colors focus-visible:outline-2 focus-visible:outline-accent ${selectedKind === tabKind ? 'border-accent font-medium text-white' : 'border-transparent text-muted hover:text-white'}`}>
        {label.split(' ')[0]}<span className="text-xs font-normal tabular-nums text-muted">{entries.filter(entry => entry.media.kind === tabKind).length}/5</span>
      </button>)}
    </div>
    <div id={`${id}-panel`} role="tabpanel" aria-labelledby={`${id}-${selectedKind}`} tabIndex={0} className="mt-5">
      {favorites.length ? <div className="grid auto-cols-[112px] grid-flow-col items-start justify-start gap-3 overflow-x-auto overscroll-x-contain px-1 pb-3 pt-1 snap-x snap-proximity sm:auto-cols-[144px] lg:auto-cols-[160px]">
        {favorites.map(entry => {
          const content = <>
            <Poster item={entry.media} compact className="w-full shrink-0 transition-transform motion-safe:group-hover:-translate-y-1 motion-safe:group-active:scale-95" />
            <span className="mt-2 line-clamp-2 min-h-8 break-words text-xs leading-4 sm:min-h-10 sm:text-sm sm:leading-5">{entry.media.title}</span>
            {onRemove && <span className="mt-1 block text-xs text-muted group-hover:text-white">Remover</span>}
          </>
          const className = 'group flex min-w-0 snap-start flex-col self-start text-left'
          return onRemove ? <button key={entry.id} type="button" onClick={() => onRemove(entry)} className={className} title={entry.media.title} aria-label={`Remover ${entry.media.title} dos favoritos`}>{content}</button>
            : <Link key={entry.id} to={titlePath(entry.media)} className={className} title={entry.media.title}>{content}</Link>
        })}
      </div> : <p className="bg-surface p-5 text-sm text-muted">Nenhum favorito escolhido nesta categoria.</p>}
    </div>
  </div>
}
