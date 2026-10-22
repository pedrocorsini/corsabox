import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useAuth } from '../auth/AuthContext'
import { api, ApiError } from '../lib/api'
import { ErrorBanner, Field, SubmitButton, SuccessBanner } from './ui'

const schema = z
  .object({
    currentPassword: z.string().min(1, 'Informe a senha atual'),
    newPassword: z.string().min(8, 'Mínimo de 8 caracteres').max(72, 'Máximo de 72 caracteres'),
    confirmPassword: z.string(),
  })
  .refine((d) => d.newPassword === d.confirmPassword, {
    message: 'As senhas não conferem',
    path: ['confirmPassword'],
  })
  .refine((d) => d.newPassword !== d.currentPassword, {
    message: 'A nova senha precisa ser diferente da atual',
    path: ['newPassword'],
  })
type Form = z.infer<typeof schema>

type Notice = { id: number; kind: 'success' | 'error'; text: string }

const empty: Form = { currentPassword: '', newPassword: '', confirmPassword: '' }

export function ChangePasswordForm() {
  const { token } = useAuth()
  const [notice, setNotice] = useState<Notice | null>(null)
  const {
    register,
    handleSubmit,
    reset,
    setError,
    control,
    formState: { errors, isSubmitting },
  } = useForm<Form>({ resolver: zodResolver(schema), defaultValues: empty })

  const values = useWatch({ control })
  const filled = !!values.currentPassword && !!values.newPassword && !!values.confirmPassword

  const notify = (kind: Notice['kind'], text: string) => setNotice({ id: Date.now(), kind, text })

  const onSubmit = async (data: Form) => {
    if (!token) return
    setNotice(null)
    try {
      await api.changePassword(token, { currentPassword: data.currentPassword, newPassword: data.newPassword })
      reset(empty)
      notify('success', 'Senha alterada.')
    } catch (e) {
      if (e instanceof ApiError && e.status === 400) {
        setError('currentPassword', { message: 'Senha atual incorreta.' }, { shouldFocus: true })
      } else if (e instanceof ApiError && e.status === 401) {
        notify('error', 'Sua sessão expirou. Entre novamente para alterar a senha.')
      } else {
        notify('error', 'Não foi possível alterar a senha. Tente novamente.')
      }
    }
  }

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      // Voltar a editar apaga o aviso de sucesso. Um erro fica até a próxima tentativa.
      onChange={() => setNotice((n) => (n?.kind === 'success' ? null : n))}
      className="mt-8 space-y-4"
      noValidate
    >
      <Field id="currentPassword" label="Senha atual" type="password" autoComplete="current-password"
        error={errors.currentPassword?.message} {...register('currentPassword')} />
      <div className="grid gap-4 sm:grid-cols-2">
        <Field id="newPassword" label="Nova senha" type="password" autoComplete="new-password"
          error={errors.newPassword?.message} {...register('newPassword')} />
        <Field id="confirmNewPassword" label="Confirmar nova senha" type="password" autoComplete="new-password"
          error={errors.confirmPassword?.message} {...register('confirmPassword')} />
      </div>

      {notice && (
        <div key={notice.id} className="notice-in">
          {notice.kind === 'success'
            ? <SuccessBanner>{notice.text}</SuccessBanner>
            : <ErrorBanner>{notice.text}</ErrorBanner>}
        </div>
      )}

      <div className="pt-2">
        <SubmitButton loading={isSubmitting} disabled={!filled} className="w-full sm:w-auto">
          Alterar senha
        </SubmitButton>
      </div>
    </form>
  )
}
