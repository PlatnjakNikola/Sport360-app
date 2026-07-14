import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { Modal } from '../../components/ui/Modal'
import { catalogsApi } from '../../api/catalogs'
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
  name: z.string().min(1, 'Name is required'),
  serviceCenterId: z.coerce.number().int().positive('Select a service center'),
  phone: z.string().optional(),
})
type InviteForm = z.infer<typeof schema>

export function InviteTechnicianModal({ onClose }: { onClose: () => void }) {
  const queryClient = useQueryClient()
  const { data: serviceCenters } = useQuery({ queryKey: ['service-centers'], queryFn: catalogsApi.serviceCenters })
  const { register, handleSubmit, formState } = useForm<InviteForm>({ resolver: zodResolver(schema) })

  const mutation = useMutation({
    mutationFn: (body: InviteForm) => usersApi.inviteTechnician(body),
    onSuccess: () => {
      toast.success('Invite sent')
      queryClient.invalidateQueries({ queryKey: ['technician-invites'] })
      onClose()
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  const onSubmit = handleSubmit((values) => mutation.mutate(values))

  return (
    <Modal title="Invite technician" onClose={onClose}>
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <div>
          <label className={labelClass} htmlFor="email">
            Email
          </label>
          <input id="email" type="email" className={inputClass} {...register('email')} />
          {formState.errors.email && <p className={errorTextClass}>{formState.errors.email.message}</p>}
        </div>
        <div>
          <label className={labelClass} htmlFor="name">
            Name
          </label>
          <input id="name" className={inputClass} {...register('name')} />
          {formState.errors.name && <p className={errorTextClass}>{formState.errors.name.message}</p>}
        </div>
        <div>
          <label className={labelClass} htmlFor="serviceCenterId">
            Service center
          </label>
          <select id="serviceCenterId" className={inputClass} defaultValue="" {...register('serviceCenterId')}>
            <option value="" disabled>
              Select…
            </option>
            {serviceCenters?.map((center) => (
              <option key={center.id} value={center.id}>
                {center.name}
              </option>
            ))}
          </select>
          {formState.errors.serviceCenterId && (
            <p className={errorTextClass}>{formState.errors.serviceCenterId.message}</p>
          )}
        </div>
        <div>
          <label className={labelClass} htmlFor="phone">
            Phone (optional)
          </label>
          <input id="phone" className={inputClass} {...register('phone')} />
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
