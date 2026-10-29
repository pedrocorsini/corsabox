import { useRef, useState, type ChangeEvent } from 'react'
import { Upload } from 'lucide-react'
import { useAuth } from '../auth/useAuth'
import { api, ApiError, type ProfileCustomization } from '../lib/api'
import { setProfile, useProfile } from '../lib/profile'
import { Button, ErrorBanner, SubmitButton, SuccessBanner } from './ui'

export function ProfileCustomizationEditor() {
  const { user } = useAuth()
  const profile = useProfile(user?.id ?? '')
  return <section aria-labelledby="customize-profile">
    <h2 id="customize-profile" className="text-xl font-semibold">Foto e bio</h2>
    <p className="mt-2 text-muted">Como você aparece para a comunidade.</p>
    {!profile.data && !profile.error && <p role="status" className="mt-6 text-muted">Carregando personalização…</p>}
    {profile.error && <div className="mt-4"><ErrorBanner>Não foi possível carregar o perfil.</ErrorBanner><Button onClick={profile.retry}>Tentar de novo</Button></div>}
    {profile.data && <Editor initial={profile.data} />}
  </section>
}

function Editor({ initial }: { initial: ProfileCustomization }) {
  const { token, user } = useAuth()
  const photoInput = useRef<HTMLInputElement>(null)
  const [bio, setBio] = useState(initial.bio)
  const [photo, setPhoto] = useState(initial.photo)
  const [saving, setSaving] = useState(false)
  const [readingPhoto, setReadingPhoto] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)
  const dirty = bio !== initial.bio || photo !== initial.photo

  async function choosePhoto(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setSuccess(false); setError('')
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 5 * 1024 * 1024) {
      setError('Escolha uma imagem JPG, PNG ou WebP de até 5 MB.'); return
    }
    setReadingPhoto(true)
    try {
      const bitmap = await createImageBitmap(file)
      try {
        const canvas = document.createElement('canvas')
        canvas.width = canvas.height = 512
        const context = canvas.getContext('2d')
        if (!context) throw new Error()
        context.fillStyle = '#fff'; context.fillRect(0, 0, 512, 512)
        const edge = Math.min(bitmap.width, bitmap.height)
        context.drawImage(bitmap, (bitmap.width - edge) / 2, (bitmap.height - edge) / 2, edge, edge, 0, 0, 512, 512)
        const result = canvas.toDataURL('image/jpeg', 0.8)
        if (result.length > 400000) throw new Error()
        setPhoto(result)
      } finally { bitmap.close() }
    } catch { setError('Não foi possível abrir esta imagem. Escolha outra foto.') }
    finally { setReadingPhoto(false) }
  }

  return <form className="mt-6 space-y-6" onChange={() => setSuccess(false)} onSubmit={async event => {
    event.preventDefault()
    if (!token || !user || saving || readingPhoto) return
    setSaving(true); setError(''); setSuccess(false)
    try {
      const result = await api.profiles.savePresentation(token, { bio, photo })
      setProfile(user.id, result); setBio(result.bio); setPhoto(result.photo); setSuccess(true)
    } catch (e) { setError(e instanceof ApiError ? e.message : 'Não foi possível salvar. Tente novamente.') }
    finally { setSaving(false) }
  }}>
    <fieldset disabled={saving} className="space-y-6 disabled:opacity-70">
      <div className="flex flex-col items-start gap-4 sm:flex-row sm:items-center sm:gap-5">
        <span className="flex size-24 shrink-0 items-center justify-center overflow-hidden rounded-full bg-surface text-3xl" aria-label="Prévia da foto">
          {photo ? <img src={photo} alt="" className="size-full object-cover" /> : user?.firstName.charAt(0)}
        </span>
        <div className="w-full min-w-0 sm:flex-1">
          <p className="mb-2 text-sm font-medium">Foto de perfil</p>
          <input ref={photoInput} id="profile-photo" type="file" accept="image/jpeg,image/png,image/webp" onChange={choosePhoto} disabled={readingPhoto}
            aria-label="Foto de perfil" className="hidden" />
          <button type="button" disabled={readingPhoto} onClick={() => photoInput.current?.click()} aria-describedby="photo-hint"
            className="inline-flex min-h-12 w-full items-center justify-center gap-2 bg-bg px-5 text-sm font-semibold transition-colors enabled:hover:bg-line enabled:active:bg-line focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent disabled:cursor-not-allowed disabled:opacity-60 motion-safe:enabled:active:scale-[0.97] sm:w-auto">
            <Upload size={16} aria-hidden="true" /> Selecionar foto
          </button>
          <p id="photo-hint" className="mt-2 text-xs text-muted">JPG, PNG ou WebP, até 5 MB. Recorte quadrado central.</p>
          {readingPhoto && <p role="status" className="mt-2 text-sm text-muted">Preparando foto…</p>}
          {photo && <button type="button" disabled={readingPhoto} onClick={() => { setPhoto(''); setSuccess(false) }} className="min-h-11 text-sm text-muted hover:text-white">Remover foto</button>}
        </div>
      </div>
      <div>
        <label htmlFor="profile-bio" className="mb-2 block text-sm font-medium">Bio</label>
        <textarea id="profile-bio" maxLength={300} rows={3} value={bio} onChange={e => setBio(e.target.value)}
          aria-describedby="bio-count" placeholder="Conte um pouco sobre você e o que gosta de assistir."
          className="w-full resize-none bg-bg px-3.5 py-3 text-base text-white outline-2 outline-transparent transition placeholder:text-muted focus:outline-accent" />
        <p id="bio-count" className="mt-1 text-right text-xs tabular-nums text-muted">{bio.length}/300</p>
      </div>
    </fieldset>
    {error && <ErrorBanner>{error}</ErrorBanner>}
    {success && <SuccessBanner>Perfil público atualizado.</SuccessBanner>}
    <div className="flex flex-wrap gap-3">
      <SubmitButton loading={saving} disabled={!dirty || readingPhoto} className="w-full sm:w-auto">Salvar foto e bio</SubmitButton>
      {dirty && <Button disabled={saving || readingPhoto} onClick={() => { setBio(initial.bio); setPhoto(initial.photo); setError(''); setSuccess(false) }}>Descartar</Button>}
    </div>
  </form>
}
