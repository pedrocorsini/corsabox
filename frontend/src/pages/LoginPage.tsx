import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { AuthLayout } from '../components/AuthLayout'
import { ErrorBanner, Field, SubmitButton } from '../components/ui'
import { useAuth } from '../auth/AuthContext'
import { ApiError } from '../lib/api'

const schema = z.object({
  email: z.string().email('Informe um email válido'),
  password: z.string().min(1, 'Informe a senha'),
})
type Form = z.infer<typeof schema>

export default function LoginPage() {
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
      await login(data.email, data.password)
      navigate('/')
    } catch (e) {
      setServerError(
        e instanceof ApiError && e.status === 401
          ? 'Email ou senha inválidos.'
          : 'Não foi possível entrar. Tente novamente.',
      )
    }
  }

  return (
    <AuthLayout
      title="Bem-vindo de volta"
      subtitle="Entre na sua conta para continuar."
      footer={
        <>
          Ainda não tem conta?{' '}
          <Link to="/register" className="font-medium text-accent hover:underline">
            Cadastre-se
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <Field id="email" label="Email" type="email" autoComplete="email"
          error={errors.email?.message} {...register('email')} />
        <Field id="password" label="Senha" type="password" autoComplete="current-password"
          error={errors.password?.message} {...register('password')} />
        {serverError && <ErrorBanner>{serverError}</ErrorBanner>}
        <SubmitButton loading={isSubmitting}>Entrar</SubmitButton>
      </form>
    </AuthLayout>
  )
}