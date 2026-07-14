import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { adminApi, type CorrectionModuleBody, type UpdatePackageBody } from '../../api/admin'
import { usersApi } from '../../api/users'
import { getErrorMessage } from '../../api/client'
import { PackageStatusBadge } from '../../components/ui/PackageStatusBadge'
import { ModuleStatusBadge } from '../../components/ui/ModuleStatusBadge'
import { Timeline } from '../../components/ui/Timeline'
import { Modal } from '../../components/ui/Modal'
import { ConfirmModal } from '../../components/ui/ConfirmModal'
import { formatDateTime } from '../../utils/formatDate'
import { inputClass, labelClass, primaryButtonClass, secondaryButtonClass } from '../../components/ui/formStyles'

type DialogKind = 'edit' | 'status' | 'correction' | 'delete' | 'restore' | null

export function AdminPackageDetailPage() {
  const { id } = useParams<{ id: string }>()
  const packageId = Number(id)
  const queryClient = useQueryClient()
  const [dialog, setDialog] = useState<DialogKind>(null)

  const { data: pkg, isLoading, isError } = useQuery({
    queryKey: ['admin-package', packageId],
    queryFn: () => adminApi.packageDetail(packageId),
  })
  const { data: stats } = useQuery({
    queryKey: ['admin-package-stats', packageId],
    queryFn: () => adminApi.packageStatistics(packageId),
    enabled: !Number.isNaN(packageId),
  })

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['admin-package', packageId] })
    queryClient.invalidateQueries({ queryKey: ['admin-package-stats', packageId] })
    queryClient.invalidateQueries({ queryKey: ['admin-packages'] })
  }
  const close = () => setDialog(null)

  const onMutationError = (e: unknown) => toast.error(getErrorMessage(e))

  const update = useMutation({
    mutationFn: (body: UpdatePackageBody) => adminApi.updatePackage(packageId, body),
    onSuccess: () => { toast.success('Package updated'); refresh(); close() },
    onError: onMutationError,
  })
  const override = useMutation({
    mutationFn: (statusId: number) => adminApi.overrideStatus(packageId, statusId),
    onSuccess: () => { toast.success('Status overridden'); refresh(); close() },
    onError: onMutationError,
  })
  const correction = useMutation({
    mutationFn: (body: CorrectionModuleBody) => adminApi.addCorrectionModule(packageId, body),
    onSuccess: () => { toast.success('Correction module added'); refresh(); close() },
    onError: onMutationError,
  })
  const remove = useMutation({
    mutationFn: () => adminApi.deletePackage(packageId),
    onSuccess: () => { toast.success('Package deleted'); refresh(); close() },
    onError: onMutationError,
  })
  const restore = useMutation({
    mutationFn: () => adminApi.restorePackage(packageId),
    onSuccess: () => { toast.success('Package restored'); refresh(); close() },
    onError: onMutationError,
  })

  if (isLoading) return <p className="text-slate-400">Loading…</p>
  if (isError || !pkg) return <p className="text-slate-500">Package not found.</p>

  return (
    <div className="space-y-5">
      <Link to="/admin/packages" className="text-sm text-slate-500 hover:underline">← Back to all packages</Link>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-semibold">{pkg.packageNumber}</h1>
          <PackageStatusBadge code={pkg.statusCode} label={pkg.statusName} />
          {pkg.isInternal && <span className="rounded bg-slate-100 px-2 py-0.5 text-xs text-slate-500">internal</span>}
          {pkg.deletedAt && <span className="rounded bg-red-100 px-2 py-0.5 text-xs text-red-700">deleted</span>}
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" className={secondaryButtonClass} onClick={() => setDialog('edit')}>Edit</button>
          <button type="button" className={secondaryButtonClass} onClick={() => setDialog('status')}>Override status</button>
          <button type="button" className={secondaryButtonClass} onClick={() => setDialog('correction')}>Add module</button>
          {pkg.deletedAt
            ? <button type="button" className={secondaryButtonClass} onClick={() => setDialog('restore')}>Restore</button>
            : <button type="button" className={secondaryButtonClass} onClick={() => setDialog('delete')}>Delete</button>}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-5 lg:grid-cols-3">
        <section className="rounded-lg border border-slate-200 bg-white p-5 lg:col-span-2">
          <h2 className="mb-3 text-sm font-medium text-slate-500">Info</h2>
          <dl className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
            <Field label="Client" value={pkg.companyName ?? (pkg.isInternal ? 'Internal' : '—')} />
            <Field label="Description" value={pkg.description ?? '—'} />
            <Field label="Note" value={pkg.note ?? '—'} />
            <Field label="Approx quantity" value={pkg.approxQuantity?.toString() ?? '—'} />
            <Field label="Outbound tracking" value={pkg.outboundTrackingLink ?? '—'} />
            <Field label="Return tracking" value={pkg.returnTrackingLink ?? '—'} />
            <Field label="Received" value={pkg.receivedAt ? formatDateTime(pkg.receivedAt) : '—'} />
            <Field label="Shipped" value={pkg.shippedAt ? formatDateTime(pkg.shippedAt) : '—'} />
            <Field label="Arrived" value={pkg.arrivedAt ? formatDateTime(pkg.arrivedAt) : '—'} />
          </dl>
          <div className="mt-4 grid grid-cols-2 gap-3 border-t border-slate-100 pt-4 text-sm sm:grid-cols-4">
            <Stat label="Modules" value={pkg.totalModules} />
            <Stat label="Repaired" value={pkg.repairedCount} />
            <Stat label="Not repairable" value={pkg.notRepairableCount} />
            <Stat label="Total / avg" value={`€${pkg.totalValue} / €${pkg.averagePrice}`} />
          </div>
        </section>
        <section className="rounded-lg border border-slate-200 bg-white p-5">
          <h2 className="mb-3 text-sm font-medium text-slate-500">Timeline</h2>
          <Timeline entries={pkg.timeline} />
        </section>
      </div>

      <section className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <div className="border-b border-slate-100 px-4 py-2 text-sm font-medium text-slate-500">Modules</div>
        <div className="overflow-x-auto"><table className="w-full min-w-[640px] text-left text-sm">
          <thead className="bg-slate-50 text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">Number</th>
              <th className="px-4 py-2 font-medium">Problem type</th>
              <th className="px-4 py-2 font-medium">Status</th>
              <th className="px-4 py-2 font-medium">Technician</th>
              <th className="px-4 py-2 font-medium">Price</th>
              <th className="px-4 py-2 text-right font-medium">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {pkg.modules.length === 0 && (
              <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">No modules.</td></tr>
            )}
            {pkg.modules.map((m) => (
              <tr key={m.id}>
                <td className="px-4 py-2 font-medium">{m.moduleNumber}</td>
                <td className="px-4 py-2">{m.problemTypeName}</td>
                <td className="px-4 py-2"><ModuleStatusBadge code={m.statusCode} label={m.statusName} /></td>
                <td className="px-4 py-2">{m.technicianName}</td>
                <td className="px-4 py-2">{m.price != null ? `€${m.price}` : '—'}</td>
                <td className="px-4 py-2 text-right">
                  <Link to={`/admin/modules/${m.id}`} className="text-slate-700 underline">Open</Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table></div>
      </section>

      {stats && stats.technicianBreakdown.length > 0 && (
        <section className="overflow-hidden rounded-lg border border-slate-200 bg-white">
          <div className="border-b border-slate-100 px-4 py-2 text-sm font-medium text-slate-500">
            Technician breakdown · {stats.totalPixels} pixels / {stats.totalChips} chips total
          </div>
          <div className="overflow-x-auto"><table className="w-full min-w-[640px] text-left text-sm">
            <thead className="bg-slate-50 text-slate-500">
              <tr>
                <th className="px-4 py-2 font-medium">Technician</th>
                <th className="px-4 py-2 font-medium">Modules</th>
                <th className="px-4 py-2 font-medium">Repaired</th>
                <th className="px-4 py-2 font-medium">Not repairable</th>
                <th className="px-4 py-2 font-medium">Pixels</th>
                <th className="px-4 py-2 font-medium">Chips</th>
                <th className="px-4 py-2 font-medium">Value</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {stats.technicianBreakdown.map((t) => (
                <tr key={t.technicianId}>
                  <td className="px-4 py-2 font-medium">{t.technicianName}</td>
                  <td className="px-4 py-2">{t.moduleCount}</td>
                  <td className="px-4 py-2">{t.repairedCount}</td>
                  <td className="px-4 py-2">{t.notRepairableCount}</td>
                  <td className="px-4 py-2">{t.totalPixels}</td>
                  <td className="px-4 py-2">{t.totalChips}</td>
                  <td className="px-4 py-2">€{t.totalValue}</td>
                </tr>
              ))}
            </tbody>
          </table></div>
        </section>
      )}

      {dialog === 'edit' && <EditPackageModal pkg={pkg} loading={update.isPending} onSave={(b) => update.mutate(b)} onClose={close} />}
      {dialog === 'status' && <OverrideStatusModal loading={override.isPending} onConfirm={(s) => override.mutate(s)} onClose={close} />}
      {dialog === 'correction' && <CorrectionModuleModal loading={correction.isPending} onConfirm={(b) => correction.mutate(b)} onClose={close} />}
      {dialog === 'delete' && (
        <ConfirmModal
          title="Delete package"
          message="Soft delete this package? It can be restored from Trash."
          confirmLabel="Delete"
          loading={remove.isPending}
          onConfirm={() => remove.mutate()}
          onClose={close}
        />
      )}
      {dialog === 'restore' && (
        <ConfirmModal
          title="Restore package"
          message="Restore this package from Trash?"
          confirmLabel="Restore"
          loading={restore.isPending}
          onConfirm={() => restore.mutate()}
          onClose={close}
        />
      )}
      {pkg.deletedAt && (
        <p className="text-xs text-slate-400">Deleted at {formatDateTime(pkg.deletedAt)}.</p>
      )}
    </div>
  )
}

function EditPackageModal({
  pkg,
  loading,
  onSave,
  onClose,
}: {
  pkg: { packageNumber: string; description?: string | null; note?: string | null; outboundTrackingLink?: string | null; returnTrackingLink?: string | null }
  loading: boolean
  onSave: (body: UpdatePackageBody) => void
  onClose: () => void
}) {
  const [packageNumber, setPackageNumber] = useState(pkg.packageNumber)
  const [description, setDescription] = useState(pkg.description ?? '')
  const [note, setNote] = useState(pkg.note ?? '')
  const [outbound, setOutbound] = useState(pkg.outboundTrackingLink ?? '')
  const [ret, setRet] = useState(pkg.returnTrackingLink ?? '')
  return (
    <Modal title="Edit package" onClose={onClose}>
      <div className="space-y-3">
        <Labeled label="Package number"><input className={inputClass} value={packageNumber} onChange={(e) => setPackageNumber(e.target.value)} /></Labeled>
        <Labeled label="Description"><textarea rows={2} className={inputClass} value={description} onChange={(e) => setDescription(e.target.value)} /></Labeled>
        <Labeled label="Note"><textarea rows={2} className={inputClass} value={note} onChange={(e) => setNote(e.target.value)} /></Labeled>
        <Labeled label="Outbound tracking"><input className={inputClass} value={outbound} onChange={(e) => setOutbound(e.target.value)} /></Labeled>
        <Labeled label="Return tracking"><input className={inputClass} value={ret} onChange={(e) => setRet(e.target.value)} /></Labeled>
        <div className="flex justify-end gap-2 pt-1">
          <button type="button" className={secondaryButtonClass} onClick={onClose}>Cancel</button>
          <button
            type="button"
            disabled={loading}
            className={`${primaryButtonClass} w-auto`}
            onClick={() => onSave({ packageNumber, description, note, outboundTrackingLink: outbound, returnTrackingLink: ret })}
          >
            Save
          </button>
        </div>
      </div>
    </Modal>
  )
}

function OverrideStatusModal({ loading, onConfirm, onClose }: { loading: boolean; onConfirm: (statusId: number) => void; onClose: () => void }) {
  const { data: statuses } = useQuery({ queryKey: ['package-statuses'], queryFn: adminApi.packageStatuses })
  const [statusId, setStatusId] = useState('')
  return (
    <Modal title="Override status" onClose={onClose}>
      <div className="space-y-4">
        <p className="text-sm text-slate-600">Set any status. This is logged as an admin override.</p>
        <select className={inputClass} value={statusId} onChange={(e) => setStatusId(e.target.value)}>
          <option value="">Select status…</option>
          {statuses?.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
        </select>
        <div className="flex justify-end gap-2">
          <button type="button" className={secondaryButtonClass} onClick={onClose}>Cancel</button>
          <button type="button" disabled={loading || !statusId} className={`${primaryButtonClass} w-auto`} onClick={() => onConfirm(Number(statusId))}>Apply</button>
        </div>
      </div>
    </Modal>
  )
}

function CorrectionModuleModal({ loading, onConfirm, onClose }: { loading: boolean; onConfirm: (body: CorrectionModuleBody) => void; onClose: () => void }) {
  const { data: problemTypes } = useQuery({ queryKey: ['admin-problem-types'], queryFn: adminApi.problemTypes })
  const { data: technicians } = useQuery({ queryKey: ['admin-technicians-all'], queryFn: () => usersApi.listTechnicians(1, 100) })
  const [moduleNumber, setModuleNumber] = useState('')
  const [problemTypeId, setProblemTypeId] = useState('')
  const [technicianId, setTechnicianId] = useState('')
  const activeTypes = (problemTypes ?? []).filter((p) => p.active)
  const activeTechs = (technicians?.items ?? []).filter((t) => t.active)
  return (
    <Modal title="Add correction module" onClose={onClose}>
      <div className="space-y-3">
        <Labeled label="Module number"><input className={inputClass} value={moduleNumber} onChange={(e) => setModuleNumber(e.target.value)} /></Labeled>
        <Labeled label="Problem type">
          <select className={inputClass} value={problemTypeId} onChange={(e) => setProblemTypeId(e.target.value)}>
            <option value="">Select…</option>
            {activeTypes.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
        </Labeled>
        <Labeled label="Technician">
          <select className={inputClass} value={technicianId} onChange={(e) => setTechnicianId(e.target.value)}>
            <option value="">Select…</option>
            {activeTechs.map((t) => <option key={t.userId} value={t.userId}>{t.name} ({t.serviceCenterName})</option>)}
          </select>
        </Labeled>
        <div className="flex justify-end gap-2 pt-1">
          <button type="button" className={secondaryButtonClass} onClick={onClose}>Cancel</button>
          <button
            type="button"
            disabled={loading || !moduleNumber.trim() || !problemTypeId || !technicianId}
            className={`${primaryButtonClass} w-auto`}
            onClick={() => onConfirm({ moduleNumber: moduleNumber.trim(), problemTypeId: Number(problemTypeId), assignedTechnicianId: Number(technicianId) })}
          >
            Add module
          </button>
        </div>
      </div>
    </Modal>
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

function Stat({ label, value }: { label: string; value: string | number }) {
  return (
    <div>
      <dt className="text-slate-500">{label}</dt>
      <dd className="text-lg font-semibold">{value}</dd>
    </div>
  )
}

function Labeled({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label className={labelClass}>{label}</label>
      {children}
    </div>
  )
}
