/** O nome como monumento: ocupa a largura inteira do contêiner, em uma linha só. */
export function Wordmark({ className = '' }: { className?: string }) {
  return (
    <svg viewBox="0 0 1000 150" className={`block h-auto w-full ${className}`} aria-hidden="true" focusable="false">
      <text
        x="0" y="138" textLength="1000" lengthAdjust="spacingAndGlyphs" fill="currentColor"
        style={{ fontFamily: 'var(--font-sans)', fontWeight: 800, fontSize: 196, fontVariationSettings: '"wdth" 150' }}
      >
        CORSABOX
      </text>
    </svg>
  )
}
