import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useNavigate, useParams } from 'react-router-dom'
import toast from 'react-hot-toast'
import { inviteApi } from '../../api/invites'
import { getErrorMessage } from '../../api/client'
import type { InviteValidation } from '../../types/user'
import { AuthShell } from '../../components/layout/AuthShell'
import { LoadingSpinner } from '../../components/ui/LoadingSpinner'
import { errorTextClass, inputClass, labelClass, primaryButtonClass } from '../../components/ui/formStyles'

const schema = z
  .object({
    password: z.string().min(8, 'Password must be at least 8 characters'),
    confirmPassword: z.string(),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  })
type AcceptForm = z.infer<typeof schema>

export function AcceptInvitePage() {
  const { token } = useParams<{ token: string }>()
  const navigate = useNavigate()
  const [invite, setInvite] = useState<InviteValidation | null>(null)
  const [loading, setLoading] = useState(true)
  const [invalid, setInvalid] = useState(false)
  const { register, handleSubmit, formState } = useForm<AcceptForm>({ resolver: zodResolver(schema) })

  useEffect(() => {
    if (!token) {
      setInvalid(true)
      setLoading(false)
      return
    }
    inviteApi
      .validate(token)
      .then(setInvite)
      .catch(() => setInvalid(true))
      .finally(() => setLoading(false))
  }, [token])

  const onSubmit = handleSubmit(async ({ password }) => {
    if (!token) return
    try {
      await inviteApi.accept(token, password)
      toast.success('Account created. Please sign in.')
      navigate('/login', { replace: true })
    } catch (error) {
      toast.error(getErrorMessage(error))
    }
  })

  if (loading) return <LoadingSpinner />

  if (invalid || !invite) {
    return (
      <AuthShell title="Invite not valid" footer={<Link to="/login" className="underline">Back to sign in</Link>}>
        <p className="text-sm text-slate-600">
          This invite link is invalid or has expired. Please ask the administrator to resend it.
        </p>
      </AuthShell>
    )
  }

  return (
    <AuthShell
      title="Set up your account"
      subtitle={`Invited as ${invite.type}`}
      footer={<Link to="/login" className="underline">Back to sign in</Link>}
    >
      <dl className="mb-4 space-y-1 rounded-md bg-slate-50 p-3 text-sm text-slate-600">
        <div>
          <span className="text-slate-400">Email:</span> {invite.email}
        </div>
        {invite.type === 'technician' && (
          <>
            <div>
              <span className="text-slate-400">Name:</span> {invite.name}
            </div>
            {invite.serviceCenterName && (
              <div>
                <span className="text-slate-400">Service center:</span> {invite.serviceCenterName}
              </div>
            )}
          </>
        )}
        {invite.type === 'client' && (
          <>
            <div>
              <span className="text-slate-400">Contact:</span> {invite.name}
            </div>
            {invite.companyName && (
              <div>
                <span className="text-slate-400">Company:</span> {invite.companyName}
              </div>
            )}
          </>
        )}
      </dl>

      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <div>
          <label className={labelClass} htmlFor="password">
            Password
          </label>
          <input id="password" type="password" autoComplete="new-password" className={inputClass} {...register('password')} />
          {formState.errors.password && <p className={errorTextClass}>{formState.errors.password.message}</p>}
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
          Create account
        </button>
      </form>
    </AuthShell>
  )
}
