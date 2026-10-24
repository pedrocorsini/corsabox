const day = new Intl.DateTimeFormat('pt-BR', { day: 'numeric', month: 'short', year: 'numeric' })

/** 'YYYY-MM-DD' lido como data local, sem deslocar um dia por causa do fuso. */
export function formatDay(iso: string) {
  const [y, m, d] = iso.split('-').map(Number)
  return day.format(new Date(y, m - 1, d))
}

export function formatInstant(iso: string) {
  return day.format(new Date(iso))
}

export function todayISO() {
  const n = new Date()
  return `${n.getFullYear()}-${String(n.getMonth() + 1).padStart(2, '0')}-${String(n.getDate()).padStart(2, '0')}`
}

export const ratingText = (v: number) => v.toFixed(1).replace('.', ',')

export function formatRuntime(min: number, perEpisode = false) {
  const h = Math.floor(min / 60)
  const m = min % 60
  const text = h > 0 ? (m > 0 ? `${h}h ${m}min` : `${h}h`) : `${m}min`
  return perEpisode ? `${text} por episódio` : text
}