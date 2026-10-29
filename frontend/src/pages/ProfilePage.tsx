import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { ArrowUpRight, LogOut } from 'lucide-react'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useAuth } from '../auth/useAuth'
import { ProfileCustomizationEditor } from '../components/ProfileCustomizationEditor'
import { profilePath } from '../lib/people'
import { AppHeader } from '../components/AppHeader'
import { ChangePasswordForm } from '../components/ChangePasswordForm'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { ErrorBanner, Field, SubmitButton, SuccessBanner } from '../components/ui'
import { ApiError, type UpdateProfileData, type User } from '../lib/api'
import { useScrollEffects } from '../lib/motion'

const schema = z.object({
  firstName: z.string().trim().min(1, 'Informe o nome'),
  lastName: z.string().trim().min(1, 'Informe o sobrenome'),
  username: z.string().regex(/^[a-zA-Z0-9_]{3,20}$/, 'Use de 3 a 20 letras, números ou _'),
})
type Form = z.infer<typeof schema>

type Notice = { id: number; kind: 'success' | 'error'; text: string }

const toForm = (u: User): Form => ({ firstName: u.firstName, lastName: u.lastName, username: u.username })

/** Só o que mudou de verdade vai no PATCH, já normalizado como o backend normaliza. */
function diff(values: Form, user: User): UpdateProfileData {
  const next = {
    firstName: values.firstName.trim(),
    lastName: values.lastName.trim(),
    username: values.username.trim().toLowerCase(),
  }
  const changes: UpdateProfileData = {}
  if (next.firstName !== user.firstName) changes.firstName = next.firstName
  if (next.lastName !== user.lastName) changes.lastName = next.lastName
  if (next.username !== user.username) changes.username = next.username
  return changes
}

export default function ProfilePage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [confirmingLogout, setConfirmingLogout] = useState(false)
  useScrollEffects()
  if (!user) return null

  return (
    <div className="min-h-svh bg-bg text-white">
      <AppHeader />

      <main className="mx-auto max-w-6xl px-5 pb-24 pt-12 md:px-8">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 data-reveal className="text-3xl font-semibold tracking-tight sm:text-4xl">Minha conta</h1>
            <p className="mt-3 text-muted">Atualize sua foto, bio e dados pessoais.</p>
          </div>
          <Link to={profilePath(user.username)} className="group inline-flex min-h-12 items-center justify-center gap-2 self-start bg-surface px-5 text-sm font-medium text-white transition-colors hover:bg-line focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent motion-safe:active:scale-[0.97]">
            Ver perfil público <ArrowUpRight size={16} aria-hidden="true" className="transition-transform motion-safe:group-hover:-translate-y-0.5 motion-safe:group-hover:translate-x-0.5" />
          </Link>
        </div>

        <div className="mt-8 grid items-start gap-10 lg:grid-cols-2 lg:gap-12">
          <div className="min-w-0 bg-surface p-5 sm:p-6">
            <ProfileCustomizationEditor />
          </div>
          <section aria-labelledby="personal-details" className="min-w-0">
            <h2 id="personal-details" className="text-xl font-semibold">Dados pessoais</h2>
            <p className="mt-2 text-muted">Seu nome e sua identificação na CorsaBox.</p>
            <ProfileForm user={user} />
          </section>
        </div>

        <section className="mt-12" aria-labelledby="account-access">
          <h2 id="account-access" className="text-xl font-semibold">Acesso e sessão</h2>
          <div className="mt-6 grid items-start gap-8 lg:grid-cols-2 lg:gap-12">
            <div className="min-w-0">
              <h3 className="font-semibold">{user.hasPassword ? 'Alterar senha' : 'Entrada com Google'}</h3>
              {user.hasPassword ? <>
                <p className="mt-2 text-sm text-muted">Confirme sua senha atual para escolher uma nova.</p>
                <ChangePasswordForm />
              </> : <p className="mt-2 max-w-prose text-sm leading-relaxed text-muted">Você usa sua conta Google para entrar. A senha é gerenciada pelo Google.</p>}
            </div>
            <div className="min-w-0 bg-surface p-5 sm:p-6">
              <h3 className="font-semibold">Sessão atual</h3>
              <p className="mt-2 break-all text-sm text-muted">{user.email}</p>
              <p className="mt-3 text-sm leading-relaxed text-muted">Encerre o acesso à sua conta neste navegador.</p>
              <button type="button" onClick={() => setConfirmingLogout(true)}
                className="mt-5 inline-flex min-h-12 w-full items-center justify-center gap-2 bg-bg px-5 text-sm font-semibold text-white transition-colors hover:bg-red-500/15 hover:text-red-200 active:bg-red-500/15 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent motion-safe:active:scale-[0.97] sm:w-auto">
                <LogOut size={16} aria-hidden="true" /> Sair da conta
              </button>
            </div>
          </div>
        </section>
      </main>
      <ConfirmDialog
        open={confirmingLogout}
        title="Sair da conta?"
        description="Você precisará entrar de novo para ver seu diário."
        confirmLabel="Sair"
        onConfirm={() => { setConfirmingLogout(false); navigate('/'); logout() }}
        onCancel={() => setConfirmingLogout(false)}
      />
    </div>
  )
}

function ProfileForm({ user }: { user: User }) {
  const { updateProfile } = useAuth()
  const [notice, setNotice] = useState<Notice | null>(null)
  const {
    register,
    handleSubmit,
    reset,
    setError,
    control,
    formState: { errors, isSubmitting },
  } = useForm<Form>({ resolver: zodResolver(schema), defaultValues: toForm(user) })

  const values = useWatch({ control })
  const hasChanges = Object.keys(diff(values as Form, user)).length > 0

  const notify = (kind: Notice['kind'], text: string) =>
    setNotice((current) => ({ id: (current?.id ?? 0) + 1, kind, text }))

  const onSubmit = async (data: Form) => {
    setNotice(null)
    const changes = diff(data, user)
    if (Object.keys(changes).length === 0) return
    try {
      const updated = await updateProfile(changes)
      reset(toForm(updated))
      notify('success', 'Perfil atualizado.')
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) {
        setError('username', { message: 'Este username já está em uso.' }, { shouldFocus: true })
        notify('error', 'Não foi possível salvar. Escolha outro username.')
      } else if (e instanceof ApiError && e.status === 401) {
        notify('error', 'Sua sessão expirou. Entre novamente para salvar.')
      } else {
        notify('error', 'Não foi possível salvar as alterações. Tente novamente.')
      }
    }
  }

  const discard = () => {
    reset(toForm(user))
    setNotice(null)
  }

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      // Voltar a editar apaga o aviso de sucesso. Um erro fica até a próxima tentativa.
      onChange={() => setNotice((n) => (n?.kind === 'success' ? null : n))}
      className="mt-8 space-y-4"
      noValidate
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <Field id="firstName" label="Nome" autoComplete="given-name"
          error={errors.firstName?.message} {...register('firstName')} />
        <Field id="lastName" label="Sobrenome" autoComplete="family-name"
          error={errors.lastName?.message} {...register('lastName')} />
      </div>
      <Field id="username" label="Username" autoComplete="username"
        hint="De 3 a 20 letras, números ou _. Sempre em minúsculas."
        error={errors.username?.message} {...register('username')} />
      <Field id="email" label="Email" type="email" readOnly value={user.email}
        hint="O email é usado para entrar e não pode ser alterado por enquanto." />

      {notice && (
        <div key={notice.id} className="notice-in">
          {notice.kind === 'success'
            ? <SuccessBanner>{notice.text}</SuccessBanner>
            : <ErrorBanner>{notice.text}</ErrorBanner>}
        </div>
      )}

      <div className="flex flex-wrap items-center gap-x-5 gap-y-3 pt-2">
        <SubmitButton loading={isSubmitting} disabled={!hasChanges} className="w-full sm:w-auto">
          Salvar alterações
        </SubmitButton>
        {hasChanges ? (
          <button type="button" onClick={discard} disabled={isSubmitting}
            className="h-14 px-2 text-muted transition enabled:hover:text-white enabled:active:scale-[0.97] disabled:cursor-not-allowed">
            Descartar
          </button>
        ) : (
          <p className="text-sm text-muted">Nenhuma alteração para salvar.</p>
        )}
      </div>
    </form>
  )
}
