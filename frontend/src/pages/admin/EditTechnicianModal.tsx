import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { Modal } from '../../components/ui/Modal'
import { catalogsApi } from '../../api/catalogs'
import { usersApi } from '../../api/users'
import { getErrorMessage } from '../../api/client'
import type { Technician } from '../../types/user'
import {
  errorTextClass,
  inputClass,
  labelClass,
  primaryButtonClass,
  secondaryButtonClass,
} from '../../components/ui/formStyles'

const schema = z.object({
  name: z.string().min(1, 'Name is required'),
  phone: z.string().optional(),
  serviceCenterId: z.coerce.number().int().positive('Select a service center'),
})
type EditForm = z.infer<typeof schema>

export function EditTechnicianModal({ technician, onClose }: { technician: Technician; onClose: () => void }) {
  const queryClient = useQueryClient()
  const { data: serviceCenters } = useQuery({ queryKey: ['service-centers'], queryFn: catalogsApi.serviceCenters })
  const { register, handleSubmit, formState } = useForm<EditForm>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: technician.name,
      phone: technician.phone ?? '',
      serviceCenterId: technician.serviceCenterId,
    },
  })

  const mutation = useMutation({
    mutationFn: (body: EditForm) => usersApi.updateTechnician(technician.userId, body),
    onSuccess: () => {
      toast.success('Technician updated')
      queryClient.invalidateQueries({ queryKey: ['technicians'] })
      onClose()
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  const onSubmit = handleSubmit((values) => mutation.mutate(values))

  return (
    <Modal title={`Edit ${technician.name}`} onClose={onClose}>
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <div>
          <label className={labelClass} htmlFor="name">
            Name
          </label>
          <input id="name" className={inputClass} {...register('name')} />
          {formState.errors.name && <p className={errorTextClass}>{formState.errors.name.message}</p>}
        </div>
        <div>
          <label className={labelClass} htmlFor="phone">
            Phone
          </label>
          <input id="phone" className={inputClass} {...register('phone')} />
        </div>
        <div>
          <label className={labelClass} htmlFor="serviceCenterId">
            Service center
          </label>
          <select id="serviceCenterId" className={inputClass} {...register('serviceCenterId')}>
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
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" onClick={onClose} className={secondaryButtonClass}>
            Cancel
          </button>
          <button type="submit" disabled={mutation.isPending} className={`${primaryButtonClass} w-auto`}>
            Save
          </button>
        </div>
      </form>
    </Modal>
  )
}
