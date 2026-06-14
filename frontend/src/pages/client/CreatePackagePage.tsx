import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { clientPackagesApi, type CreatePackageBody } from '../../api/packages'
import { getErrorMessage } from '../../api/client'
import { errorTextClass, inputClass, labelClass, primaryButtonClass, secondaryButtonClass } from '../../components/ui/formStyles'

const schema = z.object({
  packageNumber: z.string().min(1, 'Package number is required').max(100),
  description: z.string().min(1, 'Description is required'),
  note: z.string().optional(),
  outboundTrackingLink: z.string().max(2048).optional(),
  approxQuantity: z.preprocess(
    (value) => (value === '' || value === undefined || value === null ? undefined : Number(value)),
    z.number().int().min(0).optional(),
  ),
})
type CreateForm = z.infer<typeof schema>

export function CreatePackagePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { register, handleSubmit, formState } = useForm<CreateForm>({ resolver: zodResolver(schema) })

  const mutation = useMutation({
    mutationFn: (body: CreatePackageBody) => clientPackagesApi.create(body),
    onSuccess: (pkg) => {
      toast.success('Package created')
      queryClient.invalidateQueries({ queryKey: ['client-packages'] })
      queryClient.invalidateQueries({ queryKey: ['client-dashboard'] })
      navigate(`/client/packages/${pkg.id}`)
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  const onSubmit = handleSubmit((values) =>
    mutation.mutate({
      packageNumber: values.packageNumber,
      description: values.description,
      note: values.note || undefined,
      outboundTrackingLink: values.outboundTrackingLink || undefined,
      approxQuantity: values.approxQuantity,
    }),
  )

  return (
    <div className="mx-auto max-w-lg space-y-4">
      <h1 className="text-2xl font-semibold">Create package</h1>
      <form onSubmit={onSubmit} className="space-y-4 rounded-lg border border-slate-200 bg-white p-5" noValidate>
        <div>
          <label className={labelClass} htmlFor="packageNumber">
            Package / pallet number
          </label>
          <input id="packageNumber" className={inputClass} {...register('packageNumber')} />
          {formState.errors.packageNumber && <p className={errorTextClass}>{formState.errors.packageNumber.message}</p>}
        </div>
        <div>
          <label className={labelClass} htmlFor="description">
            Description (contents + quantity info)
          </label>
          <textarea id="description" rows={3} className={inputClass} {...register('description')} />
          {formState.errors.description && <p className={errorTextClass}>{formState.errors.description.message}</p>}
        </div>
        <div>
          <label className={labelClass} htmlFor="outboundTrackingLink">
            Outbound tracking link (optional)
          </label>
          <input id="outboundTrackingLink" className={inputClass} {...register('outboundTrackingLink')} />
        </div>
        <div>
          <label className={labelClass} htmlFor="note">
            Note (optional)
          </label>
          <textarea id="note" rows={2} className={inputClass} {...register('note')} />
        </div>
        <div>
          <label className={labelClass} htmlFor="approxQuantity">
            Approx quantity (optional)
          </label>
          <input id="approxQuantity" type="number" min={0} className={inputClass} {...register('approxQuantity')} />
          {formState.errors.approxQuantity && <p className={errorTextClass}>{formState.errors.approxQuantity.message}</p>}
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <Link to="/client/packages" className={secondaryButtonClass}>
            Cancel
          </Link>
          <button type="submit" disabled={mutation.isPending} className={`${primaryButtonClass} w-auto`}>
            Create
          </button>
        </div>
      </form>
    </div>
  )
}
