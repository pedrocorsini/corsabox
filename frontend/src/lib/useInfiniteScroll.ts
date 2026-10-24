import { useEffect, useRef } from 'react'

// Devolve a ref de um marcador no fim da lista. Quando ele chega perto da tela, chama onLoad.
export function useInfiniteScroll(enabled: boolean, onLoad: () => void) {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const el = ref.current
    if (!el || !enabled) return
    const io = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting) onLoad()
    }, { rootMargin: '600px 0px' })
    io.observe(el)
    return () => io.disconnect()
  }, [enabled, onLoad])
  return ref
}