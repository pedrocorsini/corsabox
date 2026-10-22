/** A logo é o próprio favicon, para ficar idêntica em toda parte. */
export function Brand() {
  return (
    <span className="inline-flex items-center gap-2 text-lg font-bold">
      <img src="/favicon.svg" alt="" width={28} height={28} className="block" />
      CorsaBox
    </span>
  )
}
