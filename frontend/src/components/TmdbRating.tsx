type Props = { value?: number | null; votes?: number | null }

export function TmdbRating({ value, votes }: Props) {
  const rated = typeof value === 'number' && Number.isFinite(value) && value >= 0 && value <= 10
    && typeof votes === 'number' && votes > 0

  return (
    <span className="inline-flex flex-wrap items-baseline gap-x-2 gap-y-1 text-[15px]">
      <span className="text-muted">TMDB</span>
      {rated ? (
        <>
          <span className="font-semibold tabular-nums text-accent">
            {value.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}
            <span className="font-normal text-muted"> / 10</span>
          </span>
          <span className="text-muted">({votes.toLocaleString('pt-BR')} {votes === 1 ? 'voto' : 'votos'})</span>
        </>
      ) : <span className="text-muted">Sem avaliações</span>}
    </span>
  )
}
