import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { adminApi, type UpdateModuleBody } from '../../api/admin'
import { usersApi } from '../../api/users'
import { getErrorMessage } from '../../api/client'
import type { AdminModuleDetail } from '../../types/admin'
import { ModuleStatusBadge } from '../../components/ui/ModuleStatusBadge'
import { ImageGallery } from '../../components/ui/ImageGallery'
import { Modal } from '../../components/ui/Modal'
import { ConfirmModal } from '../../components/ui/ConfirmModal'
import { formatDateTime } from '../../utils/formatDate'
import { inputClass, labelClass, primaryButtonClass, secondaryButtonClass } from '../../components/ui/formStyles'

type DialogKind = 'edit' | 'delete' | 'restore' | null

export function AdminModuleDetailPage() {
  const { id } = useParams<{ id: string }>()
  const moduleId = Number(id)
  const queryClient = useQueryClient()
  const [dialog, setDialog] = useState<DialogKind>(null)

  const { data: module, isLoading, isError } = useQuery({
    queryKey: ['admin-module', moduleId],
    queryFn: () => adminApi.moduleDetail(moduleId),
  })

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['admin-module', moduleId] })
  const close = () => setDialog(null)
  const onError = (e: unknown) => toast.error(getErrorMessage(e))

  const update = useMutation({
    mutationFn: (body: UpdateModuleBody) => adminApi.updateModule(moduleId, body),
    onSuccess: () => { toast.success('Module updated'); refresh(); close() },
    onError,
  })
  const remove = useMutation({
    mutationFn: () => adminApi.deleteModule(moduleId),
    onSuccess: () => { toast.success('Module deleted'); refresh(); close() },
    onError,
  })
  const restore = useMutation({
    mutationFn: () => adminApi.restoreModule(moduleId),
    onSuccess: () => { toast.success('Module restored'); refresh(); close() },
    onError,
  })

  if (isLoading) return <p className="text-slate-400">Loading…</p>
  if (isError || !module) return <p className="text-slate-500">Module not found.</p>

  return (
    <div className="space-y-5">
      <Link to={`/admin/packages/${module.packageId}`} className="text-sm text-slate-500 hover:underline">
        ← Back to {module.packageNumber}
      </Link>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-semibold">{module.moduleNumber}</h1>
          <ModuleStatusBadge code={module.statusCode} label={module.statusName} />
          {module.deletedAt && <span className="rounded bg-red-100 px-2 py-0.5 text-xs text-red-700">deleted</span>}
        </div>
        <div className="flex gap-2">
          <button type="button" className={secondaryButtonClass} onClick={() => setDialog('edit')}>Edit</button>
          {module.deletedAt
            ? <button type="button" className={secondaryButtonClass} onClick={() => setDialog('restore')}>Restore</button>
            : <button type="button" className={secondaryButtonClass} onClick={() => setDialog('delete')}>Delete</button>}
        </div>
      </div>

      <section className="rounded-lg border border-slate-200 bg-white p-5">
        <dl className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
          <Field label="Problem type" value={module.problemTypeName} />
          <Field label="Technician" value={module.technicianName} />
          <Field label="Pixels repaired" value={module.pixelsRepaired?.toString() ?? '—'} />
          <Field label="Chips replaced" value={module.chipsReplaced?.toString() ?? '—'} />
          <Field label="Price" value={module.price != null ? `€${module.price}` : '—'} />
          <Field label="Completed" value={module.completedAt ? formatDateTime(module.completedAt) : '—'} />
          <Field label="Created" value={formatDateTime(module.createdAt)} />
          <Field label="Repair note" value={module.repairNote ?? '—'} />
        </dl>
      </section>

      <section className="space-y-3 rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="text-sm font-medium text-slate-500">Images</h2>
        <ImageGallery images={module.images} />
      </section>

      {dialog === 'edit' && <EditModuleModal module={module} loading={update.isPending} onSave={(b) => update.mutate(b)} onClose={close} />}
      {dialog === 'delete' && (
        <ConfirmModal title="Delete module" message="Soft delete this module? It can be restored from Trash." confirmLabel="Delete"
          loading={remove.isPending} onConfirm={() => remove.mutate()} onClose={close} />
      )}
      {dialog === 'restore' && (
        <ConfirmModal title="Restore module" message="Restore this module from Trash?" confirmLabel="Restore"
          loading={restore.isPending} onConfirm={() => restore.mutate()} onClose={close} />
      )}
    </div>
  )
}

function EditModuleModal({
  module,
  loading,
  onSave,
  onClose,
}: {
  module: AdminModuleDetail
  loading: boolean
  onSave: (body: UpdateModuleBody) => void
  onClose: () => void
}) {
  const { data: statuses } = useQuery({ queryKey: ['module-statuses'], queryFn: adminApi.moduleStatuses })
  const { data: problemTypes } = useQuery({ queryKey: ['admin-problem-types'], queryFn: adminApi.problemTypes })
  const { data: technicians } = useQuery({ queryKey: ['admin-technicians-all'], queryFn: () => usersApi.listTechnicians(1, 100) })

  const [problemTypeId, setProblemTypeId] = useState(String(module.problemTypeId))
  const [technicianId, setTechnicianId] = useState(String(module.technicianId))
  const [statusCode, setStatusCode] = useState(module.statusCode)
  const [pixels, setPixels] = useState(String(module.pixelsRepaired ?? 0))
  const [chips, setChips] = useState(String(module.chipsReplaced ?? 0))
  const [price, setPrice] = useState(String(module.price ?? 0))
  const [note, setNote] = useState(module.repairNote ?? '')
  const isRepaired = statusCode === 'repaired'

  const submit = () =>
    onSave({
      problemTypeId: Number(problemTypeId),
      assignedTechnicianId: Number(technicianId),
      statusCode,
      pixelsRepaired: isRepaired ? Number(pixels) : 0,
      chipsReplaced: isRepaired ? Number(chips) : 0,
      price: isRepaired ? Number(price) : 0,
      repairNote: note || undefined,
    })

  return (
    <Modal title="Edit module" onClose={onClose}>
      <div className="space-y-3">
        <Labeled label="Problem type">
          <select className={inputClass} value={problemTypeId} onChange={(e) => setProblemTypeId(e.target.value)}>
            {(problemTypes ?? []).map((p) => <option key={p.id} value={p.id}>{p.name}{p.active ? '' : ' (inactive)'}</option>)}
          </select>
        </Labeled>
        <Labeled label="Technician">
          <select className={inputClass} value={technicianId} onChange={(e) => setTechnicianId(e.target.value)}>
            {(technicians?.items ?? []).map((t) => <option key={t.userId} value={t.userId}>{t.name} ({t.serviceCenterName})</option>)}
          </select>
        </Labeled>
        <Labeled label="Status">
          <select className={inputClass} value={statusCode} onChange={(e) => setStatusCode(e.target.value)}>
            {(statuses ?? []).map((s) => <option key={s.id} value={s.code}>{s.name}</option>)}
          </select>
        </Labeled>
        <div className="grid grid-cols-3 gap-3">
          <Labeled label="Pixels"><input type="number" min={0} disabled={!isRepaired} className={inputClass} value={pixels} onChange={(e) => setPixels(e.target.value)} /></Labeled>
          <Labeled label="Chips"><input type="number" min={0} disabled={!isRepaired} className={inputClass} value={chips} onChange={(e) => setChips(e.target.value)} /></Labeled>
          <Labeled label="Price"><input type="number" min={0} step="0.01" disabled={!isRepaired} className={inputClass} value={price} onChange={(e) => setPrice(e.target.value)} /></Labeled>
        </div>
        <Labeled label="Repair note"><textarea rows={2} className={inputClass} value={note} onChange={(e) => setNote(e.target.value)} /></Labeled>
        <div className="flex justify-end gap-2 pt-1">
          <button type="button" className={secondaryButtonClass} onClick={onClose}>Cancel</button>
          <button type="button" disabled={loading} className={`${primaryButtonClass} w-auto`} onClick={submit}>Save</button>
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

function Labeled({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label className={labelClass}>{label}</label>
      {children}
    </div>
  )
}
