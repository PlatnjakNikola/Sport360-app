import { useForm } from 'react-hook-form'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { Modal } from '../../components/ui/Modal'
import { clientPackagesApi, type UpdatePackageBody } from '../../api/packages'
import { getErrorMessage } from '../../api/client'
import type { PackageDetail } from '../../types/package'
import { inputClass, labelClass, primaryButtonClass, secondaryButtonClass } from '../../components/ui/formStyles'

export function EditPackageModal({ pkg, onClose }: { pkg: PackageDetail; onClose: () => void }) {
  const queryClient = useQueryClient()
  const { register, handleSubmit } = useForm<UpdatePackageBody>({
    defaultValues: {
      outboundTrackingLink: pkg.outboundTrackingLink ?? '',
      note: pkg.note ?? '',
      description: pkg.description ?? '',
    },
  })

  const mutation = useMutation({
    mutationFn: (body: UpdatePackageBody) => clientPackagesApi.update(pkg.id, body),
    onSuccess: () => {
      toast.success('Package updated')
      queryClient.invalidateQueries({ queryKey: ['client-package', pkg.id] })
      queryClient.invalidateQueries({ queryKey: ['client-packages'] })
      onClose()
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  const onSubmit = handleSubmit((values) => mutation.mutate(values))

  return (
    <Modal title="Edit package" onClose={onClose}>
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <div>
          <label className={labelClass} htmlFor="description">
            Description
          </label>
          <textarea id="description" rows={3} className={inputClass} {...register('description')} />
        </div>
        <div>
          <label className={labelClass} htmlFor="outboundTrackingLink">
            Outbound tracking link
          </label>
          <input id="outboundTrackingLink" className={inputClass} {...register('outboundTrackingLink')} />
        </div>
        <div>
          <label className={labelClass} htmlFor="note">
            Note
          </label>
          <textarea id="note" rows={2} className={inputClass} {...register('note')} />
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
