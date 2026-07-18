import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { adminApi } from '../../api/admin'
import type { AuditLogEntry } from '../../types/notification'
import { Pagination } from '../../components/ui/Pagination'
import { formatDateTime } from '../../utils/formatDate'
import { inputClass, labelClass } from '../../components/ui/formStyles'

const ENTITY_TYPES = ['package', 'module', 'repair', 'user', 'problem_type', 'service_center', 'image']
const ACTION_TYPES = ['create', 'update', 'delete', 'status_change', 'admin_override', 'cleanup']

export function AuditLogPage() {
  const [page, setPage] = useState(1)
  const [entityType, setEntityType] = useState('')
  const [actionType, setActionType] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [expanded, setExpanded] = useState<Set<number>>(new Set())

  const filters = {
    page,
    entityType,
    actionType,
    dateFrom: from ? `${from}T00:00:00Z` : undefined,
    dateTo: to ? `${to}T23:59:59Z` : undefined,
  }

  const { data, isLoading } = useQuery({
    queryKey: ['audit-logs', filters],
    queryFn: () => adminApi.auditLogs(filters),
  })

  const toggle = (id: number) =>
    setExpanded((cur) => {
      const next = new Set(cur)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })

  const reset = (fn: () => void) => { setPage(1); fn() }
  const items = data?.items ?? []

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Audit log</h1>

      <div className="flex flex-wrap items-end gap-3 rounded-lg border border-slate-200 bg-white p-4">
        <div>
          <label className={labelClass} htmlFor="entity">Entity</label>
          <select id="entity" className={inputClass} value={entityType} onChange={(e) => reset(() => setEntityType(e.target.value))}>
            <option value="">All</option>
            {ENTITY_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
        </div>
        <div>
          <label className={labelClass} htmlFor="action">Action</label>
          <select id="action" className={inputClass} value={actionType} onChange={(e) => reset(() => setActionType(e.target.value))}>
            <option value="">All</option>
            {ACTION_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
        </div>
        <div>
          <label className={labelClass} htmlFor="from">From</label>
          <input id="from" type="date" className={inputClass} value={from} onChange={(e) => reset(() => setFrom(e.target.value))} />
        </div>
        <div>
          <label className={labelClass} htmlFor="to">To</label>
          <input id="to" type="date" className={inputClass} value={to} onChange={(e) => reset(() => setTo(e.target.value))} />
        </div>
      </div>

      <section className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <div className="overflow-x-auto"><table className="w-full min-w-[640px] text-left text-sm">
          <thead className="bg-slate-50 text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">When</th>
              <th className="px-4 py-2 font-medium">User</th>
              <th className="px-4 py-2 font-medium">Entity</th>
              <th className="px-4 py-2 font-medium">Action</th>
              <th className="px-4 py-2 text-right font-medium">Details</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {isLoading && <tr><td colSpan={5} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>}
            {!isLoading && items.length === 0 && <tr><td colSpan={5} className="px-4 py-6 text-center text-slate-400">No audit entries.</td></tr>}
            {items.map((entry) => (
              <AuditRow key={entry.id} entry={entry} expanded={expanded.has(entry.id)} onToggle={() => toggle(entry.id)} />
            ))}
          </tbody>
        </table></div>
      </section>
      <Pagination page={page} totalPages={data?.pagination.totalPages ?? 1} onChange={setPage} />
    </div>
  )
}

function AuditRow({ entry, expanded, onToggle }: { entry: AuditLogEntry; expanded: boolean; onToggle: () => void }) {
  const hasJson = entry.oldValueJson || entry.newValueJson
  return (
    <>
      <tr>
        <td className="px-4 py-2 text-slate-500">{formatDateTime(entry.createdAt)}</td>
        <td className="px-4 py-2">{entry.changedByName ?? `#${entry.changedByUserId}`}</td>
        <td className="px-4 py-2">{entry.entityType} #{entry.entityId}</td>
        <td className="px-4 py-2"><ActionBadge action={entry.actionType} /></td>
        <td className="px-4 py-2 text-right">
          {hasJson ? (
            <button type="button" className="text-slate-600 underline" onClick={onToggle}>{expanded ? 'Hide' : 'View'}</button>
          ) : (
            <span className="text-slate-300">—</span>
          )}
        </td>
      </tr>
      {expanded && hasJson && (
        <tr className="bg-slate-50">
          <td colSpan={5} className="px-4 py-3">
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <JsonBlock label="Old" json={entry.oldValueJson} />
              <JsonBlock label="New" json={entry.newValueJson} />
            </div>
          </td>
        </tr>
      )}
    </>
  )
}

function JsonBlock({ label, json }: { label: string; json?: string | null }) {
  if (!json) return null
  let pretty = json
  try {
    pretty = JSON.stringify(JSON.parse(json), null, 2)
  } catch {
    /* leave as-is */
  }
  return (
    <div>
      <p className="mb-1 text-xs font-medium text-slate-500">{label}</p>
      <pre className="overflow-x-auto rounded border border-slate-200 bg-white p-2 text-xs text-slate-700">{pretty}</pre>
    </div>
  )
}

function ActionBadge({ action }: { action: string }) {
  const color =
    action === 'delete' ? 'bg-red-50 text-red-700'
      : action === 'admin_override' ? 'bg-amber-50 text-amber-700'
        : action === 'create' ? 'bg-green-50 text-green-700'
          : 'bg-slate-100 text-slate-600'
  return <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${color}`}>{action}</span>
}
