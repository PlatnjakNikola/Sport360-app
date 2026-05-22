import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link } from 'react-router-dom'
import toast from 'react-hot-toast'
import { authApi } from '../../api/auth'
import { getErrorMessage } from '../../api/client'
import { AuthShell } from '../../components/layout/AuthShell'
import { errorTextClass, inputClass, labelClass, primaryButtonClass } from '../../components/ui/formStyles'

const schema = z.object({ email: z.string().email('Enter a valid email') })
type ForgotForm = z.infer<typeof schema>

export function ForgotPasswordPage() {
  const [sent, setSent] = useState(false)
  const { register, handleSubmit, formState } = useForm<ForgotForm>({ resolver: zodResolver(schema) })

  const onSubmit = handleSubmit(async ({ email }) => {
    try {
      await authApi.forgotPassword(email)
      setSent(true)
    } catch (error) {
      toast.error(getErrorMessage(error))
    }
  })

  return (
    <AuthShell
      title="Reset password"
      subtitle={sent ? undefined : 'Enter your email and we will send a reset link.'}
      footer={<Link to="/login" className="underline">Back to sign in</Link>}
    >
      {sent ? (
        <p className="text-sm text-slate-600">
          If an account exists for that email, a reset link has been sent. Please check your inbox.
        </p>
      ) : (
        <form onSubmit={onSubmit} className="space-y-4" noValidate>
          <div>
            <label className={labelClass} htmlFor="email">
              Email
            </label>
            <input id="email" type="email" autoComplete="email" className={inputClass} {...register('email')} />
            {formState.errors.email && <p className={errorTextClass}>{formState.errors.email.message}</p>}
          </div>
          <button type="submit" disabled={formState.isSubmitting} className={primaryButtonClass}>
            Send reset link
          </button>
        </form>
      )}
    </AuthShell>
  )
}
