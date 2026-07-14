import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { adminApi } from '../../api/admin'
import { getErrorMessage } from '../../api/client'
import { useDebounce } from '../../hooks/useDebounce'
import { PackageStatusBadge } from '../../components/ui/PackageStatusBadge'
import { Pagination } from '../../components/ui/Pagination'
import { ConfirmModal } from '../../components/ui/ConfirmModal'
import { Modal } from '../../components/ui/Modal'
import { formatDate } from '../../utils/formatDate'
import { inputClass, labelClass, primaryButtonClass, secondaryButtonClass } from '../../components/ui/formStyles'

export function AllPackagesPage() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(1)
  const [searchInput, setSearchInput] = useState('')
  const search = useDebounce(searchInput, 300)
  const [status, setStatus] = useState('')
  const [type, setType] = useState('all')
  const [serviceCenterId, setServiceCenterId] = useState('')
  const [includeDeleted, setIncludeDeleted] = useState(false)
  const [selected, setSelected] = useState<Set<number>>(new Set())
  const [bulkDeleteOpen, setBulkDeleteOpen] = useState(false)
  const [bulkStatusOpen, setBulkStatusOpen] = useState(false)

  const filters = {
    page,
    search,
    status,
    type,
    includeDeleted,
    serviceCenterId: serviceCenterId ? Number(serviceCenterId) : undefined,
  }

  const { data, isLoading } = useQuery({
    queryKey: ['admin-packages', filters],
    queryFn: () => adminApi.listPackages(filters),
  })
  const { data: statuses } = useQuery({ queryKey: ['package-statuses'], queryFn: adminApi.packageStatuses })
  const { data: centers } = useQuery({ queryKey: ['admin-service-centers'], queryFn: adminApi.serviceCenters })
  const centerName = useMemo(() => new Map((centers ?? []).map((c) => [c.id, c.code])), [centers])

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['admin-packages'] })
  const clearSelection = () => setSelected(new Set())

  const bulkDelete = useMutation({
    mutationFn: () => adminApi.bulkDelete([...selected]),
    onSuccess: (n) => {
      toast.success(`${n} package(s) deleted`)
      setBulkDeleteOpen(false)
      clearSelection()
      refresh()
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  })

  const bulkStatus = useMutation({
    mutationFn: (statusId: number) => adminApi.bulkStatus([...selected], statusId),
    onSuccess: (n) => {
      toast.success(`${n} package(s) updated`)
      setBulkStatusOpen(false)
      clearSelection()
      refresh()
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  })

  const items = data?.items ?? []
  const allSelected = items.length > 0 && items.every((p) => selected.has(p.id))
  const toggleAll = () =>
    setSelected(allSelected ? new Set() : new Set(items.map((p) => p.id)))
  const toggleOne = (id: number) =>
    setSelected((current) => {
      const next = new Set(current)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })

  const resetPageThen = (fn: () => void) => {
    setPage(1)
    fn()
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">All packages</h1>

      <div className="flex flex-wrap items-end gap-3 rounded-lg border border-slate-200 bg-white p-4">
        <div className="flex-1 min-w-[180px]">
          <label className={labelClass} htmlFor="search">Search</label>
          <input
            id="search"
            className={inputClass}
            placeholder="Package number or client…"
            value={searchInput}
            onChange={(e) => resetPageThen(() => setSearchInput(e.target.value))}
          />
        </div>
        <div>
          <label className={labelClass} htmlFor="status">Status</label>
          <select id="status" className={inputClass} value={status} onChange={(e) => resetPageThen(() => setStatus(e.target.value))}>
            <option value="">All statuses</option>
            {statuses?.map((s) => (
              <option key={s.id} value={s.code}>{s.name}</option>
            ))}
          </select>
        </div>
        <div>
          <label className={labelClass} htmlFor="type">Type</label>
          <select id="type" className={inputClass} value={type} onChange={(e) => resetPageThen(() => setType(e.target.value))}>
            <option value="all">All</option>
            <option value="external">External</option>
            <option value="internal">Internal</option>
          </select>
        </div>
        <div>
          <label className={labelClass} htmlFor="center">Service center</label>
          <select id="center" className={inputClass} value={serviceCenterId} onChange={(e) => resetPageThen(() => setServiceCenterId(e.target.value))}>
            <option value="">All centers</option>
            {centers?.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
        </div>
        <label className="flex items-center gap-2 pb-2 text-sm text-slate-600">
          <input type="checkbox" checked={includeDeleted} onChange={(e) => resetPageThen(() => setIncludeDeleted(e.target.checked))} />
          Show deleted
        </label>
      </div>

      {selected.size > 0 && (
        <div className="flex items-center gap-3 rounded-lg border border-slate-300 bg-slate-50 px-4 py-2 text-sm">
          <span className="font-medium">{selected.size} selected</span>
          <button type="button" className={secondaryButtonClass} onClick={() => setBulkStatusOpen(true)}>Set status…</button>
          <button type="button" className={secondaryButtonClass} onClick={() => setBulkDeleteOpen(true)}>Delete selected</button>
          <button type="button" className="ml-auto text-slate-500 hover:underline" onClick={clearSelection}>Clear</button>
        </div>
      )}

      <section className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <div className="overflow-x-auto"><table className="w-full min-w-[640px] text-left text-sm">
          <thead className="bg-slate-50 text-slate-500">
            <tr>
              <th className="w-8 px-3 py-2"><input type="checkbox" checked={allSelected} onChange={toggleAll} aria-label="Select all" /></th>
              <th className="px-4 py-2 font-medium">Package</th>
              <th className="px-4 py-2 font-medium">Client</th>
              <th className="px-4 py-2 font-medium">Center</th>
              <th className="px-4 py-2 font-medium">Status</th>
              <th className="px-4 py-2 font-medium">Modules</th>
              <th className="px-4 py-2 font-medium">Created</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {isLoading && (
              <tr><td colSpan={7} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
            )}
            {!isLoading && items.length === 0 && (
              <tr><td colSpan={7} className="px-4 py-6 text-center text-slate-400">No packages found.</td></tr>
            )}
            {items.map((pkg) => (
              <tr key={pkg.id} className={pkg.deleted ? 'bg-red-50/40' : undefined}>
                <td className="px-3 py-2"><input type="checkbox" checked={selected.has(pkg.id)} onChange={() => toggleOne(pkg.id)} aria-label={`Select ${pkg.packageNumber}`} /></td>
                <td className="px-4 py-2 font-medium">
                  <Link to={`/admin/packages/${pkg.id}`} className="text-slate-800 hover:underline">{pkg.packageNumber}</Link>
                  {pkg.isInternal && <span className="ml-2 rounded bg-slate-100 px-1.5 py-0.5 text-xs text-slate-500">internal</span>}
                  {pkg.deleted && <span className="ml-2 rounded bg-red-100 px-1.5 py-0.5 text-xs text-red-700">deleted</span>}
                </td>
                <td className="px-4 py-2">{pkg.companyName ?? '—'}</td>
                <td className="px-4 py-2">{pkg.serviceCenterId ? centerName.get(pkg.serviceCenterId) ?? '—' : '—'}</td>
                <td className="px-4 py-2"><PackageStatusBadge code={pkg.statusCode} label={pkg.statusName} /></td>
                <td className="px-4 py-2">{pkg.repairedCount}✓ / {pkg.notRepairableCount}✗ / {pkg.totalModules}</td>
                <td className="px-4 py-2 text-slate-500">{formatDate(pkg.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table></div>
      </section>
      <Pagination page={page} totalPages={data?.pagination.totalPages ?? 1} onChange={setPage} />

      {bulkDeleteOpen && (
        <ConfirmModal
          title="Delete selected packages"
          message={`Soft delete ${selected.size} package(s)? They can be restored from Trash.`}
          confirmLabel="Delete"
          loading={bulkDelete.isPending}
          onConfirm={() => bulkDelete.mutate()}
          onClose={() => setBulkDeleteOpen(false)}
        />
      )}
      {bulkStatusOpen && (
        <BulkStatusModal
          statuses={statuses ?? []}
          loading={bulkStatus.isPending}
          onConfirm={(id) => bulkStatus.mutate(id)}
          onClose={() => setBulkStatusOpen(false)}
        />
      )}
    </div>
  )
}

function BulkStatusModal({
  statuses,
  loading,
  onConfirm,
  onClose,
}: {
  statuses: { id: number; name: string }[]
  loading: boolean
  onConfirm: (statusId: number) => void
  onClose: () => void
}) {
  const [statusId, setStatusId] = useState('')
  return (
    <Modal title="Set status for selected" onClose={onClose}>
      <div className="space-y-4">
        <p className="text-sm text-slate-600">This is an admin override and will be logged.</p>
        <select className={inputClass} value={statusId} onChange={(e) => setStatusId(e.target.value)}>
          <option value="">Select status…</option>
          {statuses.map((s) => (
            <option key={s.id} value={s.id}>{s.name}</option>
          ))}
        </select>
        <div className="flex justify-end gap-2">
          <button type="button" onClick={onClose} className={secondaryButtonClass}>Cancel</button>
          <button
            type="button"
            disabled={loading || !statusId}
            className={`${primaryButtonClass} w-auto`}
            onClick={() => onConfirm(Number(statusId))}
          >
            Apply
          </button>
        </div>
      </div>
    </Modal>
  )
}
