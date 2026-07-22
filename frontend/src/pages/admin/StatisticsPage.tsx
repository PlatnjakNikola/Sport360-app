import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Download } from 'lucide-react'
import toast from 'react-hot-toast'
import { adminApi } from '../../api/admin'
import { getErrorMessage } from '../../api/client'
import type { CountItem, StatisticsFilters } from '../../types/statistics'
import { PackageStatusBadge } from '../../components/ui/PackageStatusBadge'
import { inputClass, labelClass, secondaryButtonClass } from '../../components/ui/formStyles'

export function StatisticsPage() {
  const [type, setType] = useState('all')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [downloading, setDownloading] = useState(false)

  const filters: StatisticsFilters = {
    filter: type,
    dateFrom: from ? `${from}T00:00:00Z` : undefined,
    dateTo: to ? `${to}T23:59:59Z` : undefined,
  }

  const { data, isLoading } = useQuery({
    queryKey: ['admin-statistics', filters],
    queryFn: () => adminApi.statistics(filters),
  })

  const download = async () => {
    setDownloading(true)
    try {
      await adminApi.downloadStatisticsCsv(filters)
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setDownloading(false)
    }
  }

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Statistics</h1>
        <button type="button" className={`${secondaryButtonClass}`} disabled={downloading} onClick={download}>
          <Download className="h-4 w-4" /> Export CSV
        </button>
      </div>

      <div className="flex flex-wrap items-end gap-3 rounded-lg border border-slate-200 bg-white p-4">
        <div>
          <label className={labelClass} htmlFor="type">Type</label>
          <select id="type" className={inputClass} value={type} onChange={(e) => setType(e.target.value)}>
            <option value="all">All</option>
            <option value="external">External</option>
            <option value="internal">Internal</option>
          </select>
        </div>
        <div>
          <label className={labelClass} htmlFor="from">From</label>
          <input id="from" type="date" className={inputClass} value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div>
          <label className={labelClass} htmlFor="to">To</label>
          <input id="to" type="date" className={inputClass} value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
        {(from || to) && (
          <button type="button" className="pb-2 text-sm text-slate-500 hover:underline" onClick={() => { setFrom(''); setTo('') }}>
            Clear dates
          </button>
        )}
      </div>

      {isLoading && <p className="text-slate-400">Loading…</p>}

      {data && (
        <>
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            <StatCard label="Packages" value={data.totalPackages} />
            <StatCard label="Modules" value={data.totalModules} />
            <StatCard label="Repaired" value={data.repairedModules} />
            <StatCard label="Not repairable" value={data.notRepairableModules} />
          </div>

          <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
            <Panel title="Packages by status">
              {data.packagesByStatus.length === 0 ? <Empty /> : data.packagesByStatus.map((s) => (
                <div key={s.code} className="flex items-center gap-3 text-sm">
                  <div className="w-44"><PackageStatusBadge code={s.code} label={s.name} /></div>
                  <Bar count={s.count} total={data.totalPackages} />
                  <span className="w-8 text-right font-medium">{s.count}</span>
                </div>
              ))}
            </Panel>

            <Panel title="Modules by problem type">
              <CountBars items={data.modulesByProblemType} />
            </Panel>

            <Panel title="Packages by client">
              <CountBars items={data.packagesByClient} />
            </Panel>

            <Panel title="Packages by month">
              {data.packagesByMonth.length === 0 ? <Empty /> : (
                <CountBars items={data.packagesByMonth.map((m) => ({ label: m.period, count: m.count }))} />
              )}
            </Panel>
          </div>
        </>
      )}
    </div>
  )
}

function CountBars({ items }: { items: CountItem[] }) {
  if (items.length === 0) return <Empty />
  const max = Math.max(...items.map((i) => i.count), 1)
  return (
    <>
      {items.map((i) => (
        <div key={i.label} className="flex items-center gap-3 text-sm">
          <span className="w-44 truncate text-slate-600" title={i.label}>{i.label}</span>
          <Bar count={i.count} total={max} />
          <span className="w-8 text-right font-medium">{i.count}</span>
        </div>
      ))}
    </>
  )
}

function Bar({ count, total }: { count: number; total: number }) {
  const width = total > 0 ? `${Math.round((count / total) * 100)}%` : '0%'
  return (
    <div className="h-2 flex-1 overflow-hidden rounded bg-slate-100">
      <div className="h-full bg-slate-400" style={{ width }} />
    </div>
  )
}

function Panel({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-5">
      <h2 className="mb-3 text-sm font-medium text-slate-500">{title}</h2>
      <div className="space-y-2">{children}</div>
    </section>
  )
}

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-1 text-2xl font-semibold">{value}</p>
    </div>
  )
}

function Empty() {
  return <p className="text-sm text-slate-400">No data.</p>
}
