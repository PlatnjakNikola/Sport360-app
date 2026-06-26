import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import toast from 'react-hot-toast'
import { technicianApi, type RepairBody } from '../../api/technician'
import { getErrorMessage } from '../../api/client'
import { ImageGallery } from '../../components/ui/ImageGallery'
import { ImageUploader } from '../../components/ui/ImageUploader'
import { errorTextClass, inputClass, labelClass, primaryButtonClass, secondaryButtonClass } from '../../components/ui/formStyles'

const schema = z.object({
  decision: z.enum(['repaired', 'not_repairable']),
  pixelsRepaired: z.coerce.number().int().min(0).default(0),
  chipsReplaced: z.coerce.number().int().min(0).default(0),
  price: z.coerce.number().min(0).default(0),
  repairNote: z.string().optional(),
})
type RepairForm = z.infer<typeof schema>

export function RepairPage() {
  const { id } = useParams<{ id: string }>()
  const moduleId = Number(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data: module, isLoading, isError } = useQuery({
    queryKey: ['tech-module', moduleId],
    queryFn: () => technicianApi.moduleDetail(moduleId),
  })

  const { register, handleSubmit, watch, formState } = useForm<RepairForm>({
    resolver: zodResolver(schema),
    defaultValues: { decision: 'repaired', pixelsRepaired: 0, chipsReplaced: 0, price: 0 },
  })
  const notRepairable = watch('decision') === 'not_repairable'

  const mutation = useMutation({
    mutationFn: (body: RepairBody) => technicianApi.repair(moduleId, body),
    onSuccess: (saved) => {
      toast.success('Repair saved')
      queryClient.invalidateQueries({ queryKey: ['tech-package', saved.packageId] })
      queryClient.invalidateQueries({ queryKey: ['tech-modules', saved.packageId] })
      navigate(`/technician/packages/${saved.packageId}`)
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  const onSubmit = handleSubmit((values) => {
    const body: RepairBody =
      values.decision === 'repaired'
        ? {
            decision: 'repaired',
            pixelsRepaired: values.pixelsRepaired,
            chipsReplaced: values.chipsReplaced,
            price: values.price,
            repairNote: values.repairNote || undefined,
          }
        : { decision: 'not_repairable', repairNote: values.repairNote || undefined }
    mutation.mutate(body)
  })

  if (isLoading) return <p className="text-slate-400">Loading…</p>
  if (isError || !module) return <p className="text-slate-500">Module not found.</p>

  if (module.statusCode !== 'waiting_for_repair') {
    return (
      <div className="space-y-3">
        <p className="text-slate-600">This module has already been completed.</p>
        <Link to={`/technician/modules/${module.id}`} className="text-sm text-slate-700 underline">
          View module
        </Link>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-lg space-y-4">
      <Link to={`/technician/packages/${module.packageId}`} className="text-sm text-slate-500 hover:underline">
        ← Back to package
      </Link>
      <h1 className="text-2xl font-semibold">Repair {module.moduleNumber}</h1>
      <p className="text-sm text-slate-500">Problem: {module.problemTypeName}</p>

      <section className="space-y-4 rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="text-sm font-medium text-slate-500">Images</h2>
        <ImageGallery images={module.images} />
        <ImageUploader
          moduleId={module.id}
          currentCount={module.images.length}
          onUploaded={() => queryClient.invalidateQueries({ queryKey: ['tech-module', moduleId] })}
        />
      </section>

      <form onSubmit={onSubmit} className="space-y-4 rounded-lg border border-slate-200 bg-white p-5" noValidate>
        <fieldset>
          <legend className={labelClass}>Decision</legend>
          <div className="mt-2 flex gap-4 text-sm">
            <label className="flex items-center gap-2">
              <input type="radio" value="repaired" {...register('decision')} /> Repaired
            </label>
            <label className="flex items-center gap-2">
              <input type="radio" value="not_repairable" {...register('decision')} /> Not repairable
            </label>
          </div>
        </fieldset>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className={labelClass} htmlFor="pixelsRepaired">
              Pixels repaired
            </label>
            <input id="pixelsRepaired" type="number" min={0} disabled={notRepairable} className={inputClass} {...register('pixelsRepaired')} />
          </div>
          <div>
            <label className={labelClass} htmlFor="chipsReplaced">
              Chips replaced
            </label>
            <input id="chipsReplaced" type="number" min={0} disabled={notRepairable} className={inputClass} {...register('chipsReplaced')} />
          </div>
        </div>

        <div>
          <label className={labelClass} htmlFor="price">
            Price (EUR)
          </label>
          <input id="price" type="number" min={0} step="0.01" disabled={notRepairable} className={inputClass} {...register('price')} />
          {formState.errors.price && <p className={errorTextClass}>{formState.errors.price.message}</p>}
        </div>

        <div>
          <label className={labelClass} htmlFor="repairNote">
            Note
          </label>
          <textarea id="repairNote" rows={3} className={inputClass} {...register('repairNote')} />
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <Link to={`/technician/packages/${module.packageId}`} className={secondaryButtonClass}>
            Cancel
          </Link>
          <button type="submit" disabled={mutation.isPending} className={`${primaryButtonClass} w-auto`}>
            Save repair
          </button>
        </div>
      </form>
    </div>
  )
}
