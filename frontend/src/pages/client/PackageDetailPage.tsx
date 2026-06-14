import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import toast from 'react-hot-toast'
import { clientPackagesApi } from '../../api/packages'
import { clientModulesApi } from '../../api/technician'
import { getErrorMessage } from '../../api/client'
import { PackageStatusBadge } from '../../components/ui/PackageStatusBadge'
import { ModuleStatusBadge } from '../../components/ui/ModuleStatusBadge'
import { Timeline } from '../../components/ui/Timeline'
import { ConfirmModal } from '../../components/ui/ConfirmModal'
import { EditPackageModal } from './EditPackageModal'
import { isRepairUnlocked } from '../../utils/packageStatus'
import { formatDateTime } from '../../utils/formatDate'
import { buttonClass, secondaryButtonClass } from '../../components/ui/formStyles'

type Tab = 'overview' | 'repair' | 'summary'

export function PackageDetailPage() {
  const { id } = useParams<{ id: string }>()
  const packageId = Number(id)
  const queryClient = useQueryClient()
  const [tab, setTab] = useState<Tab>('overview')
  const [editing, setEditing] = useState(false)
  const [confirmSend, setConfirmSend] = useState(false)
  const [confirmArrival, setConfirmArrival] = useState(false)

  const { data: pkg, isLoading, isError } = useQuery({
    queryKey: ['client-package', packageId],
    queryFn: () => clientPackagesApi.get(packageId),
  })

  const unlocked = pkg ? isRepairUnlocked(pkg.statusCode) : false

  const { data: modules } = useQuery({
    queryKey: ['client-package-modules', packageId],
    queryFn: () => clientModulesApi.listModules(packageId),
    enabled: unlocked,
  })

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['client-package', packageId] })
    queryClient.invalidateQueries({ queryKey: ['client-packages'] })
  }

  const markSent = useMutation({
    mutationFn: () => clientPackagesApi.markSent(packageId),
    onSuccess: () => {
      toast.success('Package marked as sent')
      refresh()
      setConfirmSend(false)
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  const confirmArrivalMutation = useMutation({
    mutationFn: () => clientModulesApi.confirmArrival(packageId),
    onSuccess: () => {
      toast.success('Arrival confirmed')
      refresh()
      setConfirmArrival(false)
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  if (isLoading) return <p className="text-slate-400">Loading…</p>
  if (isError || !pkg) return <p className="text-slate-500">Package not found.</p>

  const items = modules?.items ?? []
  const repairedCount = items.filter((m) => m.statusCode === 'repaired').length
  const notRepairableCount = items.filter((m) => m.statusCode === 'not_repairable').length
  const totalCost = items.reduce((sum, m) => sum + (m.price ?? 0), 0)

  return (
    <div className="space-y-5">
      <Link to="/client/packages" className="text-sm text-slate-500 hover:underline">
        ← Back to packages
      </Link>

      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-semibold">{pkg.packageNumber}</h1>
          <PackageStatusBadge code={pkg.statusCode} label={pkg.statusName} />
        </div>
        {pkg.statusCode === 'created' && (
          <button type="button" className={buttonClass} onClick={() => setConfirmSend(true)}>
            Mark as sent
          </button>
        )}
        {pkg.statusCode === 'shipped_to_client' && (
          <button type="button" className={buttonClass} onClick={() => setConfirmArrival(true)}>
            Confirm arrival
          </button>
        )}
      </div>

      <div className="flex gap-1 border-b border-slate-200">
        <TabButton label="Overview" active={tab === 'overview'} onClick={() => setTab('overview')} />
        <TabButton label="Repair history" active={tab === 'repair'} onClick={() => setTab('repair')} />
        {unlocked && <TabButton label="Summary" active={tab === 'summary'} onClick={() => setTab('summary')} />}
      </div>

      {tab === 'overview' && (
        <div className="grid grid-cols-1 gap-5 lg:grid-cols-3">
          <section className="rounded-lg border border-slate-200 bg-white p-5 lg:col-span-2">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-sm font-medium text-slate-500">Details</h2>
              <button type="button" className={secondaryButtonClass} onClick={() => setEditing(true)}>
                Edit
              </button>
            </div>
            <dl className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
              <Field label="Description" value={pkg.description ?? '—'} />
              <Field label="Note" value={pkg.note ?? '—'} />
              <Field label="Approx quantity" value={pkg.approxQuantity?.toString() ?? '—'} />
              <Field label="Outbound tracking" value={pkg.outboundTrackingLink ?? '—'} />
              <Field label="Return tracking" value={pkg.returnTrackingLink ?? '—'} />
              <Field label="Created" value={formatDateTime(pkg.createdAt)} />
            </dl>
          </section>
          <section className="rounded-lg border border-slate-200 bg-white p-5">
            <h2 className="mb-3 text-sm font-medium text-slate-500">Timeline</h2>
            <Timeline entries={pkg.timeline} />
          </section>
        </div>
      )}

      {tab === 'repair' &&
        (unlocked ? (
          <section className="overflow-hidden rounded-lg border border-slate-200 bg-white">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-slate-500">
                <tr>
                  <th className="px-4 py-2 font-medium">Module</th>
                  <th className="px-4 py-2 font-medium">Status</th>
                  <th className="px-4 py-2 font-medium">Problem type</th>
                  <th className="px-4 py-2 font-medium">Pixels</th>
                  <th className="px-4 py-2 font-medium">Chips</th>
                  <th className="px-4 py-2 font-medium">Price</th>
                  <th className="px-4 py-2 text-right font-medium">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {items.length === 0 && (
                  <tr>
                    <td colSpan={7} className="px-4 py-6 text-center text-slate-400">
                      No modules.
                    </td>
                  </tr>
                )}
                {items.map((module) => (
                  <tr key={module.id}>
                    <td className="px-4 py-2 font-medium">{module.moduleNumber}</td>
                    <td className="px-4 py-2">
                      <ModuleStatusBadge code={module.statusCode} label={module.statusName} />
                    </td>
                    <td className="px-4 py-2">{module.problemTypeName}</td>
                    <td className="px-4 py-2">{module.pixelsRepaired ?? '—'}</td>
                    <td className="px-4 py-2">{module.chipsReplaced ?? '—'}</td>
                    <td className="px-4 py-2">{module.price != null ? `€${module.price}` : '—'}</td>
                    <td className="px-4 py-2 text-right">
                      <Link to={`/client/packages/${packageId}/modules/${module.id}`} className="text-slate-600 underline">
                        View
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
        ) : (
          <section className="rounded-lg border border-slate-200 bg-white p-5 text-sm text-slate-600">
            Service is still in progress. Module details will be available after completion.
          </section>
        ))}

      {tab === 'summary' && unlocked && (
        <section className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <SummaryCard label="Total modules" value={items.length} />
          <SummaryCard label="Repaired" value={repairedCount} />
          <SummaryCard label="Not repairable" value={notRepairableCount} />
          <SummaryCard label="Total cost" value={`€${totalCost}`} />
        </section>
      )}

      {editing && <EditPackageModal pkg={pkg} onClose={() => setEditing(false)} />}
      {confirmSend && (
        <ConfirmModal
          title="Mark as sent"
          message="Are you sure you want to mark this package as sent to service?"
          confirmLabel="Mark as sent"
          loading={markSent.isPending}
          onConfirm={() => markSent.mutate()}
          onClose={() => setConfirmSend(false)}
        />
      )}
      {confirmArrival && (
        <ConfirmModal
          title="Confirm arrival"
          message="Are you sure you want to confirm that this package has arrived?"
          confirmLabel="Confirm arrival"
          loading={confirmArrivalMutation.isPending}
          onConfirm={() => confirmArrivalMutation.mutate()}
          onClose={() => setConfirmArrival(false)}
        />
      )}
    </div>
  )
}

function TabButton({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  const classes = active ? 'border-slate-800 text-slate-900' : 'border-transparent text-slate-500 hover:text-slate-700'
  return (
    <button type="button" onClick={onClick} className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium ${classes}`}>
      {label}
    </button>
  )
}

function SummaryCard({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <div className="text-2xl font-semibold">{value}</div>
      <div className="mt-1 text-xs text-slate-500">{label}</div>
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
