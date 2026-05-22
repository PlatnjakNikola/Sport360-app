import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import toast from 'react-hot-toast'
import { useAuth } from '../../hooks/useAuth'
import { authApi } from '../../api/auth'
import { getErrorMessage } from '../../api/client'
import { errorTextClass, inputClass, labelClass, primaryButtonClass } from '../../components/ui/formStyles'

const schema = z
  .object({
    currentPassword: z.string().min(1, 'Current password is required'),
    newPassword: z.string().min(8, 'Password must be at least 8 characters'),
    confirmPassword: z.string(),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  })
type ChangePasswordForm = z.infer<typeof schema>

export function ProfilePage() {
  const { user } = useAuth()
  const { register, handleSubmit, reset, formState } = useForm<ChangePasswordForm>({ resolver: zodResolver(schema) })

  const onSubmit = handleSubmit(async ({ currentPassword, newPassword }) => {
    try {
      await authApi.changePassword(currentPassword, newPassword)
      toast.success('Password changed')
      reset()
    } catch (error) {
      toast.error(getErrorMessage(error))
    }
  })

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Profile</h1>

      <section className="rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="text-sm font-medium text-slate-500">Account</h2>
        <dl className="mt-3 grid grid-cols-1 gap-3 text-sm sm:grid-cols-3">
          <div>
            <dt className="text-slate-500">Name</dt>
            <dd className="font-medium">{user?.name}</dd>
          </div>
          <div>
            <dt className="text-slate-500">Email</dt>
            <dd className="font-medium">{user?.email}</dd>
          </div>
          <div>
            <dt className="text-slate-500">Role</dt>
            <dd className="font-medium capitalize">{user?.role}</dd>
          </div>
        </dl>
      </section>

      <section className="max-w-sm rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="text-sm font-medium text-slate-500">Change password</h2>
        <form onSubmit={onSubmit} className="mt-3 space-y-4" noValidate>
          <div>
            <label className={labelClass} htmlFor="currentPassword">
              Current password
            </label>
            <input
              id="currentPassword"
              type="password"
              autoComplete="current-password"
              className={inputClass}
              {...register('currentPassword')}
            />
            {formState.errors.currentPassword && (
              <p className={errorTextClass}>{formState.errors.currentPassword.message}</p>
            )}
          </div>
          <div>
            <label className={labelClass} htmlFor="newPassword">
              New password
            </label>
            <input
              id="newPassword"
              type="password"
              autoComplete="new-password"
              className={inputClass}
              {...register('newPassword')}
            />
            {formState.errors.newPassword && <p className={errorTextClass}>{formState.errors.newPassword.message}</p>}
          </div>
          <div>
            <label className={labelClass} htmlFor="confirmPassword">
              Confirm new password
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
            Change password
          </button>
        </form>
      </section>
    </div>
  )
}
