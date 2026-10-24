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