import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { AuthLayout } from '../components/AuthLayout'
import { ErrorBanner, Field, SubmitButton, SuccessBanner } from '../components/ui'
import { api, ApiError } from '../lib/api'

const schema = z
  .object({
    password: z.string().min(8, 'Mínimo de 8 caracteres').max(72, 'Máximo de 72 caracteres'),
    confirmPassword: z.string(),
  })
  .refine((d) => d.password === d.confirmPassword, {
    message: 'As senhas não conferem',
    path: ['confirmPassword'],
  })
type Form = z.infer<typeof schema>

const buttonLink =
  'flex h-14 w-full items-center justify-center bg-accent px-8 font-semibold text-ink transition hover:brightness-105 active:scale-[0.97]'

export default function ResetPasswordPage() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const [linkInvalid, setLinkInvalid] = useState(!token)
  const [done, setDone] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({ resolver: zodResolver(schema) })

  const onSubmit = async (data: Form) => {
    if (!token) return
    setServerError(null)
    try {
      await api.resetPassword({ token, newPassword: data.password })
      setDone(true)
    } catch (e) {
      // Token vencido, já usado ou substituído por um link mais novo.
      if (e instanceof ApiError && (e.status === 400 || e.status === 404 || e.status === 410)) {
        setLinkInvalid(true)
      } else {
        setServerError('Não foi possível redefinir a senha. Tente novamente.')
      }
    }
  }

  const footer = (
    <>
      Lembrou a senha?{' '}
      <Link to="/login" className="font-medium text-accent hover:underline">
        Entrar
      </Link>
    </>
  )

  if (done)
    return (
      <AuthLayout title="Senha redefinida" subtitle="Já pode entrar com a nova senha." footer={footer}>
        <div key="done" className="notice-in space-y-5">
          <SuccessBanner>Tudo certo. Sua senha foi atualizada.</SuccessBanner>
          <Link to="/login" className={buttonLink}>Ir para o login</Link>
        </div>
      </AuthLayout>
    )

  if (linkInvalid)
    return (
      <AuthLayout title="Link inválido" subtitle="Este link venceu, já foi usado ou foi substituído por outro." footer={footer}>
        <div key="invalid" className="notice-in space-y-5">
          <ErrorBanner>Peça um novo link para continuar.</ErrorBanner>
          <Link to="/esqueci-senha" className={buttonLink}>Pedir novo link</Link>
        </div>
      </AuthLayout>
    )

  return (
    <AuthLayout title="Nova senha" subtitle="Escolha uma senha com pelo menos 8 caracteres." footer={footer}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <Field id="password" label="Nova senha" type="password" autoComplete="new-password"
          error={errors.password?.message} {...register('password')} />
        <Field id="confirmPassword" label="Confirmar nova senha" type="password" autoComplete="new-password"
          error={errors.confirmPassword?.message} {...register('confirmPassword')} />
        {serverError && (
          <div key={serverError} className="notice-in">
            <ErrorBanner>{serverError}</ErrorBanner>
          </div>
        )}
        <SubmitButton loading={isSubmitting}>Salvar nova senha</SubmitButton>
      </form>
    </AuthLayout>
  )
}
