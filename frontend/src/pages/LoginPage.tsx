import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { AuthLayout } from '../components/AuthLayout'
import { GoogleLoginButton } from '../components/GoogleLoginButton'
import { ErrorBanner, Field, SubmitButton } from '../components/ui'
import { useAuth } from '../auth/useAuth'
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
      navigate('/catalogo')
    } catch (e) {
      setServerError(
        e instanceof ApiError && e.status === 401
          ? 'Email ou senha inválidos.'
          : e instanceof ApiError && e.status === 429
            ? 'Muitas tentativas. Aguarde um pouco e tente de novo.'
            : 'Não foi possível entrar. Tente novamente.',
      )
    }
  }

  return (
    <AuthLayout
      title="Bem-vindo!"
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
      <GoogleLoginButton />
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <Field id="email" label="Email" type="email" autoComplete="email"
          error={errors.email?.message} {...register('email')} />
        <Field id="password" label="Senha" type="password" autoComplete="current-password"
          error={errors.password?.message} {...register('password')} />
        <div className="text-right">
          <Link to="/esqueci-senha" className="text-sm text-muted transition-colors hover:text-white">
            Esqueci minha senha
          </Link>
        </div>
        {serverError && (
          <div key={serverError} className="notice-in">
            <ErrorBanner>{serverError}</ErrorBanner>
          </div>
        )}
        <SubmitButton loading={isSubmitting}>Entrar</SubmitButton>
      </form>
    </AuthLayout>
  )
}
