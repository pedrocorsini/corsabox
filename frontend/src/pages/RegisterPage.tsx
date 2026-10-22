import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { AuthLayout } from '../components/AuthLayout'
import { ErrorBanner, Field, SubmitButton } from '../components/ui'
import { useAuth } from '../auth/AuthContext'
import { api, ApiError } from '../lib/api'

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

export default function RegisterPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [serverError, setServerError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({ resolver: zodResolver(schema) })

  const onSubmit = async (data: Form) => {
    setServerError(null)
    try {
      await api.register({
        firstName: data.firstName,
        lastName: data.lastName,
        username: data.username.toLowerCase(),
        email: data.email,
        password: data.password,
      })
      await login(data.email, data.password)
      navigate('/catalogo')
    } catch (e) {
      setServerError(
        e instanceof ApiError && e.status === 409
          ? 'Este email ou username já está em uso.'
          : 'Não foi possível criar a conta. Tente novamente.',
      )
    }
  }

  return (
    <AuthLayout
      title="Crie sua conta"
      subtitle="Comece a registrar o que você assiste."
      footer={
        <>
          Já tem conta?{' '}
          <Link to="/login" className="font-medium text-accent hover:underline">
            Entrar
          </Link>
        </>
      }
    >
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
        <SubmitButton loading={isSubmitting}>Criar conta</SubmitButton>
      </form>
    </AuthLayout>
  )
}