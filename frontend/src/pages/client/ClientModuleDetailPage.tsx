import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { clientModulesApi } from '../../api/technician'
import { ModuleStatusBadge } from '../../components/ui/ModuleStatusBadge'
import { ImageGallery } from '../../components/ui/ImageGallery'

export function ClientModuleDetailPage() {
  const { id, moduleId } = useParams<{ id: string; moduleId: string }>()
  const { data: module, isLoading, isError } = useQuery({
    queryKey: ['client-module', Number(moduleId)],
    queryFn: () => clientModulesApi.moduleDetail(Number(moduleId)),
  })

  if (isLoading) return <p className="text-slate-400">Loading…</p>
  if (isError || !module) return <p className="text-slate-500">Module not found.</p>

  return (
    <div className="space-y-4">
      <Link to={`/client/packages/${id}`} className="text-sm text-slate-500 hover:underline">
        ← Back to package
      </Link>
      <div className="flex items-center gap-3">
        <h1 className="text-2xl font-semibold">{module.moduleNumber}</h1>
        <ModuleStatusBadge code={module.statusCode} label={module.statusName} />
      </div>
      <section className="rounded-lg border border-slate-200 bg-white p-5">
        <dl className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
          <Field label="Problem type" value={module.problemTypeName} />
          <Field label="Pixels repaired" value={module.pixelsRepaired?.toString() ?? '—'} />
          <Field label="Chips replaced" value={module.chipsReplaced?.toString() ?? '—'} />
          <Field label="Price" value={module.price != null ? `€${module.price}` : '—'} />
          <Field label="Repair note" value={module.repairNote ?? '—'} />
        </dl>
      </section>
      <section className="space-y-3 rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="text-sm font-medium text-slate-500">Images</h2>
        <ImageGallery images={module.images} />
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
