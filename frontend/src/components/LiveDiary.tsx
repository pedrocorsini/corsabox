import { useState } from 'react'

type Status = 'seen' | 'later' | null
const SAMPLE = [
  { id: 1, title: 'Parasita', meta: 'Filme · 2019' },
  { id: 2, title: 'Cowboy Bebop', meta: 'Anime · 1998' },
  { id: 3, title: 'Chernobyl', meta: 'Série · 2019' },
]
const ACTIONS = [['seen', 'Assisti'], ['later', 'Quero ver']] as const

/** A coisa viva da homepage: marque os exemplos e veja o diário mudar. */
export function LiveDiary() {
  const [state, setState] = useState<Record<number, Status>>({ 1: 'seen', 3: 'later' })
  const toggle = (id: number, s: 'seen' | 'later') =>
    setState((p) => ({ ...p, [id]: p[id] === s ? null : s }))
  const count = (s: Status) => Object.values(state).filter((v) => v === s).length

  return (
    <div data-ui className="drift bg-paper-2 p-4 text-ink md:p-6">
      <p className="text-xl font-semibold">Seu diário</p>
      <p className="mt-1 text-[15px]" aria-live="polite">
        {count('seen')} assistidos · {count('later')} para ver
      </p>
      <ul className="mt-5 space-y-2">
        {SAMPLE.map((f) => (
          <li key={f.id} className="flex flex-wrap items-center justify-between gap-3 bg-paper p-4">
            <div>
              <p className="font-semibold">{f.title}</p>
              <p className="text-[15px] text-ash">{f.meta}</p>
            </div>
            <div className="flex gap-2">
              {ACTIONS.map(([s, label]) => (
                <button key={s} type="button" aria-pressed={state[f.id] === s} onClick={() => toggle(f.id, s)}
                  className={`px-3.5 py-2 text-[15px] font-medium transition active:translate-y-px ${
                    state[f.id] === s ? 'bg-ink text-paper' : 'bg-paper-2 text-ink hover:bg-ink/10'}`}>
                  {label}
                </button>
              ))}
            </div>
          </li>
        ))}
      </ul>
      <p className="mt-5 text-[15px] text-ash">Exemplo. Seus registros reais aparecem aqui depois do cadastro.</p>
    </div>
  )
}
