import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import {
  adminApi,
  type CreateProblemTypeBody,
  type CreateServiceCenterBody,
  type UpdateProblemTypeBody,
  type UpdateServiceCenterBody,
} from '../../api/admin'
import { getErrorMessage } from '../../api/client'
import type { ProblemTypeAdmin, ServiceCenterAdmin } from '../../types/admin'
import { Modal } from '../../components/ui/Modal'
import { inputClass, labelClass, primaryButtonClass, secondaryButtonClass } from '../../components/ui/formStyles'

export function CatalogsPage() {
  const [tab, setTab] = useState<'problem-types' | 'service-centers'>('problem-types')
  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Catalogs</h1>
      <div className="flex gap-2 text-sm">
        {(['problem-types', 'service-centers'] as const).map((t) => (
          <button
            key={t}
            type="button"
            onClick={() => setTab(t)}
            className={`rounded-md px-3 py-1.5 ${tab === t ? 'bg-slate-800 text-white' : 'border border-slate-300 text-slate-600'}`}
          >
            {t === 'problem-types' ? 'Problem types' : 'Service centers'}
          </button>
        ))}
      </div>
      {tab === 'problem-types' ? <ProblemTypesTab /> : <ServiceCentersTab />}
    </div>
  )
}

function ProblemTypesTab() {
  const queryClient = useQueryClient()
  const { data: types, isLoading } = useQuery({ queryKey: ['admin-problem-types'], queryFn: adminApi.problemTypes })
  const [editing, setEditing] = useState<ProblemTypeAdmin | null>(null)
  const [creating, setCreating] = useState(false)
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['admin-problem-types'] })
  const onError = (e: unknown) => toast.error(getErrorMessage(e))

  const create = useMutation({
    mutationFn: (b: CreateProblemTypeBody) => adminApi.createProblemType(b),
    onSuccess: () => { toast.success('Problem type created'); refresh(); setCreating(false) },
    onError,
  })
  const update = useMutation({
    mutationFn: ({ id, body }: { id: number; body: UpdateProblemTypeBody }) => adminApi.updateProblemType(id, body),
    onSuccess: () => { toast.success('Problem type updated'); refresh(); setEditing(null) },
    onError,
  })

  return (
    <div className="space-y-3">
      <div className="flex justify-end">
        <button type="button" className={`${primaryButtonClass} w-auto`} onClick={() => setCreating(true)}>Add problem type</button>
      </div>
      <section className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <div className="overflow-x-auto"><table className="w-full min-w-[640px] text-left text-sm">
          <thead className="bg-slate-50 text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">Code</th>
              <th className="px-4 py-2 font-medium">Name</th>
              <th className="px-4 py-2 font-medium">Sort</th>
              <th className="px-4 py-2 font-medium">Usage</th>
              <th className="px-4 py-2 font-medium">Active</th>
              <th className="px-4 py-2 text-right font-medium">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {isLoading && <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>}
            {types?.map((t) => (
              <tr key={t.id}>
                <td className="px-4 py-2 font-mono text-xs">{t.code}</td>
                <td className="px-4 py-2 font-medium">{t.name}</td>
                <td className="px-4 py-2">{t.sortOrder}</td>
                <td className="px-4 py-2">{t.usageCount}</td>
                <td className="px-4 py-2">{t.active ? <span className="text-emerald-700">Active</span> : <span className="text-slate-400">Inactive</span>}</td>
                <td className="px-4 py-2 text-right">
                  <button type="button" className="mr-3 text-slate-700 underline" onClick={() => setEditing(t)}>Edit</button>
                  <button
                    type="button"
                    className="text-slate-600 underline"
                    onClick={() => update.mutate({ id: t.id, body: { active: !t.active } })}
                  >
                    {t.active ? 'Deactivate' : 'Activate'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table></div>
      </section>

      {creating && (
        <ProblemTypeModal
          title="Add problem type"
          loading={create.isPending}
          onSave={(b) => create.mutate(b as CreateProblemTypeBody)}
          onClose={() => setCreating(false)}
        />
      )}
      {editing && (
        <ProblemTypeModal
          title="Edit problem type"
          existing={editing}
          loading={update.isPending}
          onSave={(b) => update.mutate({ id: editing.id, body: b })}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  )
}

function ProblemTypeModal({
  title,
  existing,
  loading,
  onSave,
  onClose,
}: {
  title: string
  existing?: ProblemTypeAdmin
  loading: boolean
  onSave: (body: CreateProblemTypeBody | UpdateProblemTypeBody) => void
  onClose: () => void
}) {
  const [code, setCode] = useState(existing?.code ?? '')
  const [name, setName] = useState(existing?.name ?? '')
  const [sortOrder, setSortOrder] = useState(String(existing?.sortOrder ?? ''))
  const isCreate = !existing
  return (
    <Modal title={title} onClose={onClose}>
      <div className="space-y-3">
        <Labeled label="Code (immutable)">
          <input className={inputClass} value={code} disabled={!isCreate} onChange={(e) => setCode(e.target.value)} />
        </Labeled>
        <Labeled label="Name"><input className={inputClass} value={name} onChange={(e) => setName(e.target.value)} /></Labeled>
        <Labeled label="Sort order"><input type="number" className={inputClass} value={sortOrder} onChange={(e) => setSortOrder(e.target.value)} /></Labeled>
        <div className="flex justify-end gap-2 pt-1">
          <button type="button" className={secondaryButtonClass} onClick={onClose}>Cancel</button>
          <button
            type="button"
            disabled={loading || !name.trim() || !sortOrder || (isCreate && !code.trim())}
            className={`${primaryButtonClass} w-auto`}
            onClick={() =>
              onSave(isCreate
                ? { code: code.trim(), name: name.trim(), sortOrder: Number(sortOrder) }
                : { name: name.trim(), sortOrder: Number(sortOrder) })}
          >
            Save
          </button>
        </div>
      </div>
    </Modal>
  )
}

function ServiceCentersTab() {
  const queryClient = useQueryClient()
  const { data: centers, isLoading } = useQuery({ queryKey: ['admin-service-centers'], queryFn: adminApi.serviceCenters })
  const [editing, setEditing] = useState<ServiceCenterAdmin | null>(null)
  const [creating, setCreating] = useState(false)
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['admin-service-centers'] })
  const onError = (e: unknown) => toast.error(getErrorMessage(e))

  const create = useMutation({
    mutationFn: (b: CreateServiceCenterBody) => adminApi.createServiceCenter(b),
    onSuccess: () => { toast.success('Service center created'); refresh(); setCreating(false) },
    onError,
  })
  const update = useMutation({
    mutationFn: ({ id, body }: { id: number; body: UpdateServiceCenterBody }) => adminApi.updateServiceCenter(id, body),
    onSuccess: () => { toast.success('Service center updated'); refresh(); setEditing(null) },
    onError,
  })

  return (
    <div className="space-y-3">
      <div className="flex justify-end">
        <button type="button" className={`${primaryButtonClass} w-auto`} onClick={() => setCreating(true)}>Add service center</button>
      </div>
      <section className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <div className="overflow-x-auto"><table className="w-full min-w-[640px] text-left text-sm">
          <thead className="bg-slate-50 text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">Code</th>
              <th className="px-4 py-2 font-medium">Name</th>
              <th className="px-4 py-2 font-medium">Location</th>
              <th className="px-4 py-2 font-medium">Techs</th>
              <th className="px-4 py-2 font-medium">Active</th>
              <th className="px-4 py-2 text-right font-medium">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {isLoading && <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>}
            {centers?.map((c) => (
              <tr key={c.id}>
                <td className="px-4 py-2 font-mono text-xs">{c.code}</td>
                <td className="px-4 py-2 font-medium">{c.name}</td>
                <td className="px-4 py-2">{[c.city, c.country].filter(Boolean).join(', ') || '—'}</td>
                <td className="px-4 py-2">{c.technicianCount}</td>
                <td className="px-4 py-2">{c.active ? <span className="text-emerald-700">Active</span> : <span className="text-slate-400">Inactive</span>}</td>
                <td className="px-4 py-2 text-right">
                  <button type="button" className="mr-3 text-slate-700 underline" onClick={() => setEditing(c)}>Edit</button>
                  <button
                    type="button"
                    className="text-slate-600 underline"
                    onClick={() => update.mutate({ id: c.id, body: { active: !c.active } })}
                  >
                    {c.active ? 'Deactivate' : 'Activate'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table></div>
      </section>

      {creating && (
        <ServiceCenterModal
          title="Add service center"
          loading={create.isPending}
          onSave={(b) => create.mutate(b as CreateServiceCenterBody)}
          onClose={() => setCreating(false)}
        />
      )}
      {editing && (
        <ServiceCenterModal
          title="Edit service center"
          existing={editing}
          loading={update.isPending}
          onSave={(b) => update.mutate({ id: editing.id, body: b })}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  )
}

function ServiceCenterModal({
  title,
  existing,
  loading,
  onSave,
  onClose,
}: {
  title: string
  existing?: ServiceCenterAdmin
  loading: boolean
  onSave: (body: CreateServiceCenterBody | UpdateServiceCenterBody) => void
  onClose: () => void
}) {
  const [code, setCode] = useState(existing?.code ?? '')
  const [name, setName] = useState(existing?.name ?? '')
  const [country, setCountry] = useState(existing?.country ?? '')
  const [city, setCity] = useState(existing?.city ?? '')
  const [address, setAddress] = useState(existing?.address ?? '')
  const isCreate = !existing
  return (
    <Modal title={title} onClose={onClose}>
      <div className="space-y-3">
        <Labeled label="Code (immutable)">
          <input className={inputClass} value={code} disabled={!isCreate} onChange={(e) => setCode(e.target.value)} />
        </Labeled>
        <Labeled label="Name"><input className={inputClass} value={name} onChange={(e) => setName(e.target.value)} /></Labeled>
        <div className="grid grid-cols-2 gap-3">
          <Labeled label="Country"><input className={inputClass} value={country} onChange={(e) => setCountry(e.target.value)} /></Labeled>
          <Labeled label="City"><input className={inputClass} value={city} onChange={(e) => setCity(e.target.value)} /></Labeled>
        </div>
        <Labeled label="Address"><input className={inputClass} value={address} onChange={(e) => setAddress(e.target.value)} /></Labeled>
        <div className="flex justify-end gap-2 pt-1">
          <button type="button" className={secondaryButtonClass} onClick={onClose}>Cancel</button>
          <button
            type="button"
            disabled={loading || !name.trim() || (isCreate && !code.trim())}
            className={`${primaryButtonClass} w-auto`}
            onClick={() =>
              onSave(isCreate
                ? { code: code.trim(), name: name.trim(), country: country || undefined, city: city || undefined, address: address || undefined }
                : { name: name.trim(), country, city, address })}
          >
            Save
          </button>
        </div>
      </div>
    </Modal>
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
