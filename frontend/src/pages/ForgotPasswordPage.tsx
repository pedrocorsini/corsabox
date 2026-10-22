import { useState } from 'react'
import { Link } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { AuthLayout } from '../components/AuthLayout'
import { ErrorBanner, Field, SubmitButton, SuccessBanner } from '../components/ui'
import { api } from '../lib/api'

const schema = z.object({ email: z.string().email('Informe um email válido') })
type Form = z.infer<typeof schema>

export default function ForgotPasswordPage() {
  const [sentTo, setSentTo] = useState<string | null>(null)
  const [serverError, setServerError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({ resolver: zodResolver(schema) })

  const onSubmit = async (data: Form) => {
    setServerError(null)
    try {
      await api.forgotPassword(data.email.trim().toLowerCase())
      setSentTo(data.email.trim().toLowerCase())
    } catch {
      setServerError('Não foi possível enviar agora. Tente novamente.')
    }
  }

  return (
    <AuthLayout
      title="Esqueceu a senha?"
      subtitle="Informe seu email e enviamos um link para criar uma nova."
      footer={
        <>
          Lembrou?{' '}
          <Link to="/login" className="font-medium text-accent hover:underline">
            Entrar
          </Link>
        </>
      }
    >
      {sentTo ? (
        <div key="sent" className="notice-in space-y-5">
          {/* Resposta neutra de propósito: a tela não revela se o email tem conta. */}
          <SuccessBanner>Se existir uma conta para {sentTo}, o link já está a caminho.</SuccessBanner>
          <p className="text-sm text-muted">
            O link vale por 30 minutos e só pode ser usado uma vez. Se não chegar, olhe a caixa de spam.
          </p>
          <button
            type="button"
            onClick={() => setSentTo(null)}
            className="text-sm font-medium text-accent transition active:scale-[0.97] hover:underline"
          >
            Usar outro email
          </button>
        </div>
      ) : (
        <form key="form" onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <Field id="email" label="Email" type="email" autoComplete="email"
            error={errors.email?.message} {...register('email')} />
          {serverError && (
            <div key={serverError} className="notice-in">
              <ErrorBanner>{serverError}</ErrorBanner>
            </div>
          )}
          <SubmitButton loading={isSubmitting}>Enviar link</SubmitButton>
        </form>
      )}
    </AuthLayout>
  )
}
