import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { AuthLayout } from '../components/AuthLayout'
import { ErrorBanner, Field, SubmitButton, SuccessBanner } from '../components/ui'
import { useAuth } from '../auth/AuthContext'
import { api, ApiError, type RegisterData } from '../lib/api'

const schema = z
  .object({
    firstName: z.string().trim().min(1, 'Informe o nome'),
    lastName: z.string().trim().min(1, 'Informe o sobrenome'),
    username: z.string().regex(/^[a-zA-Z0-9_]{3,20}$/, 'Use de 3 a 20 letras, números ou _'),
    email: z.string().email('Informe um email válido'),
    password: z.string().min(8, 'Mínimo de 8 caracteres').max(72, 'Máximo de 72 caracteres'),
    confirmPassword: z.string(),
  })
  .refine((d) => d.password === d.confirmPassword, {
    message: 'As senhas não conferem',
    path: ['confirmPassword'],
  })
type Form = z.infer<typeof schema>

const codeSchema = z.object({ code: z.string().regex(/^\d{6}$/, 'Informe os 6 dígitos') })
type CodeForm = z.infer<typeof codeSchema>

function VerifyStep({ data, onBack }: { data: RegisterData; onBack: () => void }) {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CodeForm>({ resolver: zodResolver(codeSchema) })

  const onSubmit = async ({ code }: CodeForm) => {
    setError(null)
    setInfo(null)
    try {
      await api.confirmRegister({ email: data.email, code })
      await login(data.email, data.password)
      navigate('/catalogo')
    } catch (e) {
      setError(
        e instanceof ApiError && e.status === 400
          ? 'Código inválido ou expirado.'
          : e instanceof ApiError && e.status === 409
            ? 'Este email ou username já está em uso.'
            : 'Não foi possível confirmar. Tente novamente.',
      )
    }
  }

  const resend = async () => {
    setError(null)
    setInfo(null)
    try {
      await api.startRegister(data)
      setInfo('Enviamos um novo código.')
    } catch (e) {
      setError(
        e instanceof ApiError && e.status === 429
          ? 'Muitas tentativas. Aguarde um pouco antes de pedir outro código.'
          : 'Não foi possível reenviar. Tente novamente.',
      )
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
      <Field id="code" label="Código de verificação" inputMode="numeric" maxLength={6}
        autoComplete="one-time-code" error={errors.code?.message} {...register('code')} />
      {info && <SuccessBanner>{info}</SuccessBanner>}
      {error && <ErrorBanner>{error}</ErrorBanner>}
      <SubmitButton loading={isSubmitting}>Confirmar e criar conta</SubmitButton>
      <div className="flex justify-between text-sm">
        <button type="button" onClick={onBack} className="text-muted transition-colors hover:text-white">
          Corrigir dados
        </button>
        <button type="button" onClick={resend} className="font-medium text-accent hover:underline">
          Reenviar código
        </button>
      </div>
    </form>
  )
}

export default function RegisterPage() {
  const [pending, setPending] = useState<RegisterData | null>(null)
  const [serverError, setServerError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({ resolver: zodResolver(schema) })

  const onSubmit = async (data: Form) => {
    setServerError(null)
    const payload: RegisterData = {
      firstName: data.firstName,
      lastName: data.lastName,
      username: data.username.toLowerCase(),
      email: data.email.trim().toLowerCase(),
      password: data.password,
    }
    try {
      await api.startRegister(payload)
      setPending(payload)
    } catch (e) {
      setServerError(
        e instanceof ApiError && e.status === 409
          ? 'Este email ou username já está em uso.'
          : e instanceof ApiError && e.status === 429
            ? 'Muitas tentativas. Aguarde um pouco e tente de novo.'
            : 'Não foi possível criar a conta. Tente novamente.',
      )
    }
  }

  const footer = (
    <>
      Já tem conta?{' '}
      <Link to="/login" className="font-medium text-accent hover:underline">Entrar</Link>
    </>
  )

  if (pending)
    return (
      <AuthLayout title="Confirme seu email" subtitle={`Enviamos um código de 6 dígitos para ${pending.email}.`} footer={footer}>
        <VerifyStep data={pending} onBack={() => setPending(null)} />
      </AuthLayout>
    )

  return (
    <AuthLayout title="Crie sua conta" subtitle="Comece a registrar o que você assiste." footer={footer}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <div className="grid grid-cols-2 gap-3">
          <Field id="firstName" label="Nome" autoComplete="given-name"
            error={errors.firstName?.message} {...register('firstName')} />
          <Field id="lastName" label="Sobrenome" autoComplete="family-name"
            error={errors.lastName?.message} {...register('lastName')} />
        </div>
        <Field id="username" label="Username" autoComplete="username"
          error={errors.username?.message} {...register('username')} />
        <Field id="email" label="Email" type="email" autoComplete="email"
          error={errors.email?.message} {...register('email')} />
        <Field id="password" label="Senha" type="password" autoComplete="new-password"
          error={errors.password?.message} {...register('password')} />
        <Field id="confirmPassword" label="Confirmar senha" type="password" autoComplete="new-password"
          error={errors.confirmPassword?.message} {...register('confirmPassword')} />
        {serverError && <ErrorBanner>{serverError}</ErrorBanner>}
        <SubmitButton loading={isSubmitting}>Enviar código</SubmitButton>
      </form>
    </AuthLayout>
  )
}