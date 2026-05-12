import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { useAuth } from '../../hooks/useAuth'
import { authApi } from '../../api/auth'
import { getErrorMessage } from '../../api/client'
import { roleHome } from '../../utils/roleHome'
import { AuthShell } from '../../components/layout/AuthShell'
import { errorTextClass, inputClass, labelClass, primaryButtonClass } from '../../components/ui/formStyles'

const credentialsSchema = z.object({
  email: z.string().email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
})
type CredentialsForm = z.infer<typeof credentialsSchema>

const codeSchema = z.object({
  code: z.string().regex(/^\d{6}$/, 'Enter the 6-digit code'),
})
type CodeForm = z.infer<typeof codeSchema>

export function LoginPage() {
  const { login, verifyMfa } = useAuth()
  const navigate = useNavigate()
  const [mfaToken, setMfaToken] = useState<string | null>(null)
  const [resending, setResending] = useState(false)

  const credentialsForm = useForm<CredentialsForm>({ resolver: zodResolver(credentialsSchema) })
  const codeForm = useForm<CodeForm>({ resolver: zodResolver(codeSchema) })

  const submitCredentials = credentialsForm.handleSubmit(async ({ email, password }) => {
    try {
      const result = await login(email, password)
      if (result.mfaRequired && result.mfaToken) {
        setMfaToken(result.mfaToken)
        toast.success('We emailed you a 6-digit verification code')
      } else if (result.user) {
        navigate(roleHome(result.user.role), { replace: true })
      }
    } catch (error) {
      toast.error(getErrorMessage(error))
    }
  })

  const submitCode = codeForm.handleSubmit(async ({ code }) => {
    if (!mfaToken) return
    try {
      const user = await verifyMfa(mfaToken, code)
      navigate(roleHome(user.role), { replace: true })
    } catch (error) {
      toast.error(getErrorMessage(error))
    }
  })

  const handleResend = async () => {
    if (!mfaToken) return
    setResending(true)
    try {
      await authApi.resendMfa(mfaToken)
      toast.success('A new code has been sent')
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setResending(false)
    }
  }

  if (mfaToken) {
    return (
      <AuthShell
        title="Enter verification code"
        subtitle="We sent a 6-digit code to your email."
        footer={
          <button type="button" onClick={handleResend} disabled={resending} className="underline disabled:opacity-50">
            Resend code
          </button>
        }
      >
        <form onSubmit={submitCode} className="space-y-4" noValidate>
          <div>
            <label className={labelClass} htmlFor="code">
              Code
            </label>
            <input
              id="code"
              inputMode="numeric"
              autoComplete="one-time-code"
              className={inputClass}
              placeholder="123456"
              {...codeForm.register('code')}
            />
            {codeForm.formState.errors.code && (
              <p className={errorTextClass}>{codeForm.formState.errors.code.message}</p>
            )}
          </div>
          <button type="submit" disabled={codeForm.formState.isSubmitting} className={primaryButtonClass}>
            Verify
          </button>
        </form>
      </AuthShell>
    )
  }

  return (
    <AuthShell
      title="Sign in"
      subtitle="Module Service"
      footer={
        <Link to="/forgot-password" className="underline">
          Forgot password?
        </Link>
      }
    >
      <form onSubmit={submitCredentials} className="space-y-4" noValidate>
        <div>
          <label className={labelClass} htmlFor="email">
            Email
          </label>
          <input id="email" type="email" autoComplete="email" className={inputClass} {...credentialsForm.register('email')} />
          {credentialsForm.formState.errors.email && (
            <p className={errorTextClass}>{credentialsForm.formState.errors.email.message}</p>
          )}
        </div>
        <div>
          <label className={labelClass} htmlFor="password">
            Password
          </label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            className={inputClass}
            {...credentialsForm.register('password')}
          />
          {credentialsForm.formState.errors.password && (
            <p className={errorTextClass}>{credentialsForm.formState.errors.password.message}</p>
          )}
        </div>
        <button type="submit" disabled={credentialsForm.formState.isSubmitting} className={primaryButtonClass}>
          Sign in
        </button>
      </form>
    </AuthShell>
  )
}
