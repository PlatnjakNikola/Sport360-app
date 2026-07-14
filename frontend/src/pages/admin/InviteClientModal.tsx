import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { Modal } from '../../components/ui/Modal'
import { usersApi } from '../../api/users'
import { getErrorMessage } from '../../api/client'
import {
  errorTextClass,
  inputClass,
  labelClass,
  primaryButtonClass,
  secondaryButtonClass,
} from '../../components/ui/formStyles'

const schema = z.object({
  email: z.string().email('Enter a valid email'),
  contactName: z.string().min(1, 'Contact name is required'),
  companyName: z.string().min(1, 'Company name is required'),
  contactPhone: z.string().optional(),
  address: z.string().optional(),
})
type InviteForm = z.infer<typeof schema>

export function InviteClientModal({ onClose }: { onClose: () => void }) {
  const queryClient = useQueryClient()
  const { register, handleSubmit, formState } = useForm<InviteForm>({ resolver: zodResolver(schema) })

  const mutation = useMutation({
    mutationFn: (body: InviteForm) => usersApi.inviteClient(body),
    onSuccess: () => {
      toast.success('Invite sent')
      queryClient.invalidateQueries({ queryKey: ['client-invites'] })
      onClose()
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  const onSubmit = handleSubmit((values) => mutation.mutate(values))

  return (
    <Modal title="Invite client" onClose={onClose}>
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <div>
          <label className={labelClass} htmlFor="companyName">
            Company name
          </label>
          <input id="companyName" className={inputClass} {...register('companyName')} />
          {formState.errors.companyName && <p className={errorTextClass}>{formState.errors.companyName.message}</p>}
        </div>
        <div>
          <label className={labelClass} htmlFor="contactName">
            Contact name
          </label>
          <input id="contactName" className={inputClass} {...register('contactName')} />
          {formState.errors.contactName && <p className={errorTextClass}>{formState.errors.contactName.message}</p>}
        </div>
        <div>
          <label className={labelClass} htmlFor="email">
            Email
          </label>
          <input id="email" type="email" className={inputClass} {...register('email')} />
          {formState.errors.email && <p className={errorTextClass}>{formState.errors.email.message}</p>}
        </div>
        <div>
          <label className={labelClass} htmlFor="contactPhone">
            Contact phone (optional)
          </label>
          <input id="contactPhone" className={inputClass} {...register('contactPhone')} />
        </div>
        <div>
          <label className={labelClass} htmlFor="address">
            Address (optional)
          </label>
          <input id="address" className={inputClass} {...register('address')} />
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" onClick={onClose} className={secondaryButtonClass}>
            Cancel
          </button>
          <button type="submit" disabled={mutation.isPending} className={`${primaryButtonClass} w-auto`}>
            Send invite
          </button>
        </div>
      </form>
    </Modal>
  )
}
