import { useEffect, useState } from 'react'
import { api, googleLoginUrl } from '../lib/api'

export function GoogleLoginButton() {
  const [enabled, setEnabled] = useState(false)
  useEffect(() => {
    let active = true
    api.googleConfig().then(config => { if (active) setEnabled(config.enabled) }).catch(() => {})
    return () => { active = false }
  }, [])
  if (!enabled) return null
  return (
    <div className="mb-6">
      <a href={googleLoginUrl}
        className="flex min-h-12 w-full items-center justify-center gap-3 border border-line bg-white px-4 font-medium text-gray-900 transition hover:bg-gray-100 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-accent">
        <img src="/google.svg" alt="" aria-hidden="true" width={20} height={20} className="shrink-0" />
        Continuar com Google
      </a>
      <p className="mt-4 text-center text-sm text-muted">ou continue com email</p>
    </div>
  )
}
