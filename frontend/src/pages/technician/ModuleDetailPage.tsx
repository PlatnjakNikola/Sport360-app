import { Link, useParams } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { technicianApi } from '../../api/technician'
import { ModuleStatusBadge } from '../../components/ui/ModuleStatusBadge'
import { ImageGallery } from '../../components/ui/ImageGallery'
import { ImageUploader } from '../../components/ui/ImageUploader'
import { formatDateTime } from '../../utils/formatDate'

export function TechnicianModuleDetailPage() {
  const { id } = useParams<{ id: string }>()
  const moduleId = Number(id)
  const queryClient = useQueryClient()
  const { data: module, isLoading, isError } = useQuery({
    queryKey: ['tech-module', moduleId],
    queryFn: () => technicianApi.moduleDetail(moduleId),
  })

  if (isLoading) return <p className="text-slate-400">Loading…</p>
  if (isError || !module) return <p className="text-slate-500">Module not found.</p>

  return (
    <div className="space-y-4">
      <Link to={`/technician/packages/${module.packageId}`} className="text-sm text-slate-500 hover:underline">
        ← Back to package
      </Link>
      <div className="flex items-center gap-3">
        <h1 className="text-2xl font-semibold">{module.moduleNumber}</h1>
        <ModuleStatusBadge code={module.statusCode} label={module.statusName} />
      </div>
      <section className="rounded-lg border border-slate-200 bg-white p-5">
        <dl className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
          <Field label="Problem type" value={module.problemTypeName} />
          <Field label="Technician" value={module.technicianName} />
          <Field label="Pixels repaired" value={module.pixelsRepaired?.toString() ?? '—'} />
          <Field label="Chips replaced" value={module.chipsReplaced?.toString() ?? '—'} />
          <Field label="Price" value={module.price != null ? `€${module.price}` : '—'} />
          <Field label="Completed" value={module.completedAt ? formatDateTime(module.completedAt) : '—'} />
          <Field label="Note" value={module.repairNote ?? '—'} />
        </dl>
      </section>

      <section className="space-y-4 rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="text-sm font-medium text-slate-500">Images</h2>
        <ImageGallery images={module.images} />
        <ImageUploader
          moduleId={module.id}
          currentCount={module.images.length}
          onUploaded={() => queryClient.invalidateQueries({ queryKey: ['tech-module', moduleId] })}
        />
      </section>
    </div>
  )
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-slate-500">{label}</dt>
      <dd className="font-medium break-words">{value}</dd>
    </div>
  )
}
