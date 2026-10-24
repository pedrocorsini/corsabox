import { useState } from 'react'
import { KIND_LABEL, type CatalogItem } from '../lib/catalog'

const TONES = ['bg-paper-2 text-ink', 'bg-accent text-ink', 'bg-ash text-white', 'bg-paper-3 text-ink', 'bg-surface text-white']

type Props = { item: Pick<CatalogItem, 'id' | 'title' | 'kind' | 'posterUrl'>; className?: string }

// Pôster 2:3. Sem imagem, ou com imagem que falha, vira um cartão de título desenhado.
export function Poster({ item, className = '' }: Props) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null)
  const showImage = !!item.posterUrl && item.posterUrl !== failedUrl

  return (
    <div className={`aspect-[2/3] overflow-hidden ${showImage ? 'bg-surface' : TONES[item.id % TONES.length]} ${className}`}>
      {showImage ? (
        <img src={item.posterUrl} alt={item.title} loading="lazy" draggable={false}
          onError={() => setFailedUrl(item.posterUrl ?? null)}
          className="h-full w-full object-cover" />
      ) : (
        <div className="flex h-full flex-col justify-between p-3">
          <p className="text-[13px]">{KIND_LABEL[item.kind]}</p>
          <p className="wide text-lg">{item.title}</p>
        </div>
      )}
    </div>
  )
}