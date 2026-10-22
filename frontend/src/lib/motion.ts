import { useEffect } from 'react'

const THEME = { paper: '#e8e8e3', ink: '#181715' } as const

/** Revela os [data-reveal] uma vez e informa ao <html> qual campo de cor está no meio da tela. */
export function useScrollEffects() {
  useEffect(() => {
    const root = document.documentElement
    const reveal = new IntersectionObserver(
      (entries) =>
        entries.forEach((e) => {
          if (!e.isIntersecting) return
          e.target.classList.add('in')
          reveal.unobserve(e.target)
        }),
      { threshold: 0.15, rootMargin: '0px 0px -8% 0px' },
    )
    document.querySelectorAll('[data-reveal]').forEach((el) => reveal.observe(el))

    const meta = document.querySelector('meta[name="theme-color"]')
    const field = new IntersectionObserver(
      (entries) =>
        entries.forEach((e) => {
          if (!e.isIntersecting) return
          const f = (e.target as HTMLElement).dataset.field as keyof typeof THEME
          root.dataset.field = f
          meta?.setAttribute('content', THEME[f])
        }),
      { rootMargin: '-50% 0px -50% 0px' },
    )
    document.querySelectorAll('[data-field]').forEach((el) => field.observe(el))

    return () => {
      reveal.disconnect()
      field.disconnect()
      delete root.dataset.field
      meta?.setAttribute('content', THEME.paper)
    }
  }, [])
}
