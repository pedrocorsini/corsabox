import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { AuthLayout } from '../components/AuthLayout'
import { ErrorBanner, Field, SubmitButton } from '../components/ui'
import { useAuth } from '../auth/useAuth'
import { api, ApiError, type GooglePending } from '../lib/api'

export default function GoogleLoginPage() {
  const [params] = useSearchParams()
  const failed = params.has('error')
  const [pending, setPending] = useState<GooglePending | null>(null)
  const [error, setError] = useState(failed ? 'O login com Google foi cancelado ou não pôde ser concluído.' : '')
  const [loading, setLoading] = useState(false)
  const { acceptSession } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (failed) return
    let active = true
    api.googlePending().then(data => { if (active) setPending(data) })
      .catch(() => { if (active) setError('O login expirou. Volte e tente novamente.') })
    return () => { active = false }
  }, [failed])

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (loading) return
    const form = new FormData(event.currentTarget)
    const text = (key: string) => String(form.get(key) ?? '')
    setError('')
    setLoading(true)
    try {
      const { accessToken } = await api.completeGoogle({
        username: text('username'), firstName: text('firstName'),
        lastName: text('lastName'), password: text('password'),
      })
      acceptSession(accessToken)
      navigate('/catalogo', { replace: true })
    } catch (e) {
      setError(e instanceof ApiError && e.status === 401 ? 'Senha incorreta. Tente novamente.'
        : e instanceof ApiError && e.status === 429 ? 'Muitas tentativas. Aguarde antes de tentar novamente.'
        : e instanceof ApiError && e.status === 409 ? 'Email, username ou vínculo já está em uso. Confira os dados ou reinicie o login.'
        : e instanceof ApiError && e.status === 400 ? 'Confira os dados. Se o login expirou, volte e tente novamente.'
        : 'Não foi possível concluir. Tente novamente.')
    } finally { setLoading(false) }
  }

  return (
    <AuthLayout title={pending?.step === 'register' ? 'Complete seu cadastro' : pending?.step === 'link' ? 'Vincule sua conta' : 'Entrar com Google'}
      subtitle={pending?.step === 'link' ? 'Já existe uma conta com este email. Confirme sua senha para vincular o Google.'
        : pending?.step === 'register' ? 'Escolha seu username e confira seu nome.' : 'Confirme para continuar no CorsaBox.'}
      footer={<Link to="/login" className="font-medium text-accent hover:underline">Voltar para o login</Link>}>
      {error && <div className="mb-4"><ErrorBanner>{error}</ErrorBanner></div>}
      {!pending && !error && <p role="status" className="text-muted">Verificando seu acesso…</p>}
      {pending && <form onSubmit={submit} className="space-y-4">
        <Field id="google-email" label="Email do Google" value={pending.email} readOnly />
        {pending.step === 'register' && <>
          <Field id="firstName" name="firstName" label="Nome" defaultValue={pending.firstName} required maxLength={255} autoComplete="given-name" />
          <Field id="lastName" name="lastName" label="Sobrenome" defaultValue={pending.lastName} required maxLength={255} autoComplete="family-name" />
          <Field id="username" name="username" label="Username" required pattern="[a-zA-Z0-9_]{3,20}" maxLength={20}
            hint="Use de 3 a 20 letras, números ou _." autoComplete="username" />
        </>}
        {pending.step === 'link' && <>
          <Field id="password" name="password" label="Senha atual do CorsaBox" type="password" required maxLength={72} autoComplete="current-password" />
          <Link to="/esqueci-senha" className="block text-sm text-muted hover:text-white">Esqueci minha senha</Link>
        </>}
        <SubmitButton loading={loading}>{pending.step === 'register' ? 'Criar conta' : pending.step === 'link' ? 'Vincular e entrar' : 'Entrar'}</SubmitButton>
      </form>}
    </AuthLayout>
  )
}
