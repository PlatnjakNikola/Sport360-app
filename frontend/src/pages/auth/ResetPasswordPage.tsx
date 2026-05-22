import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useNavigate, useParams } from 'react-router-dom'
import toast from 'react-hot-toast'
import { authApi } from '../../api/auth'
import { getErrorMessage } from '../../api/client'
import { AuthShell } from '../../components/layout/AuthShell'
import { errorTextClass, inputClass, labelClass, primaryButtonClass } from '../../components/ui/formStyles'

const schema = z
  .object({
    newPassword: z.string().min(8, 'Password must be at least 8 characters'),
    confirmPassword: z.string(),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  })
type ResetForm = z.infer<typeof schema>

export function ResetPasswordPage() {
  const { token } = useParams<{ token: string }>()
  const navigate = useNavigate()
  const { register, handleSubmit, formState } = useForm<ResetForm>({ resolver: zodResolver(schema) })

  const onSubmit = handleSubmit(async ({ newPassword }) => {
    if (!token) return
    try {
      await authApi.resetPassword(token, newPassword)
      toast.success('Password has been reset. Please sign in.')
      navigate('/login', { replace: true })
    } catch (error) {
      toast.error(getErrorMessage(error))
    }
  })

  return (
    <AuthShell title="Set a new password" footer={<Link to="/login" className="underline">Back to sign in</Link>}>
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <div>
          <label className={labelClass} htmlFor="newPassword">
            New password
          </label>
          <input id="newPassword" type="password" autoComplete="new-password" className={inputClass} {...register('newPassword')} />
          {formState.errors.newPassword && <p className={errorTextClass}>{formState.errors.newPassword.message}</p>}
        </div>
        <div>
          <label className={labelClass} htmlFor="confirmPassword">
            Confirm password
          </label>
          <input
            id="confirmPassword"
            type="password"
            autoComplete="new-password"
            className={inputClass}
            {...register('confirmPassword')}
          />
          {formState.errors.confirmPassword && (
            <p className={errorTextClass}>{formState.errors.confirmPassword.message}</p>
          )}
        </div>
        <button type="submit" disabled={formState.isSubmitting} className={primaryButtonClass}>
          Reset password
        </button>
      </form>
    </AuthShell>
  )
}
