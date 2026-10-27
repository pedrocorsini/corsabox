import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useAuth } from '../auth/useAuth'
import { AppHeader } from '../components/AppHeader'
import { ChangePasswordForm } from '../components/ChangePasswordForm'
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
  const { user } = useAuth()
  useScrollEffects()
  if (!user) return null

  const since = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' }).format(new Date(user.createdAt))

  return (
    <div className="min-h-svh bg-bg text-white">
      <AppHeader />

      <main className="mx-auto max-w-6xl px-5 pb-24 pt-12 md:px-8">
        <h1 data-reveal className="text-4xl font-semibold md:text-5xl">Perfil</h1>
        <p data-reveal style={{ transitionDelay: '80ms' }} className="mt-3 max-w-[52ch] text-lg text-muted">
          Atualize seu nome, seu username e sua senha. As mudanças valem na hora, em todo o app.
        </p>

        <div className="mt-12 grid gap-12 md:grid-cols-12 md:gap-x-6">
          <aside className="md:col-span-4">
            <div className="bg-surface p-6">
              <div aria-hidden="true" className="wide flex size-20 items-center justify-center bg-accent text-4xl text-ink">
                {user.firstName.charAt(0).toUpperCase()}
              </div>
              <p className="mt-6 text-xl font-semibold">{user.firstName} {user.lastName}</p>
              <p className="text-muted">@{user.username}</p>
              <dl className="mt-6 space-y-4 text-[15px]">
                <div>
                  <dt className="text-muted">Email</dt>
                  <dd className="break-all">{user.email}</dd>
                </div>
                <div>
                  <dt className="text-muted">Na CorsaBox desde</dt>
                  <dd>{since}</dd>
                </div>
              </dl>
            </div>
          </aside>

          <div className="space-y-16 md:col-span-7 md:col-start-6">
            <section data-reveal>
              <h2 className="text-xl font-semibold">Dados pessoais</h2>
              <ProfileForm user={user} />
            </section>

            <section data-reveal>
              <h2 className="text-xl font-semibold">Senha</h2>
              <p className="mt-2 text-muted">Use uma senha que você não usa em outros sites.</p>
              <ChangePasswordForm />
            </section>
          </div>
        </div>
      </main>
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