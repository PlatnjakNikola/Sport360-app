import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { QrCode, Search } from 'lucide-react'
import { publicApi } from '../../api/public'
import { getErrorMessage } from '../../api/client'
import type { ModuleVisit } from '../../types/public'
import { ModuleStatusBadge } from '../../components/ui/ModuleStatusBadge'
import { QrScanModal } from '../../components/ui/QrScanModal'
import { formatDate } from '../../utils/formatDate'
import { inputClass, primaryButtonClass, secondaryButtonClass } from '../../components/ui/formStyles'

export function PublicLookupPage() {
  const [input, setInput] = useState('')
  const [query, setQuery] = useState('')
  const [scanOpen, setScanOpen] = useState(false)

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['public-history', query],
    queryFn: () => publicApi.moduleHistory(query),
    enabled: query.length > 0,
    retry: false,
    meta: { suppressErrorToast: true },
  })

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    if (input.trim()) setQuery(input.trim())
  }

  const onScan = (text: string) => {
    const value = text.trim()
    setInput(value)
    setQuery(value)
    setScanOpen(false)
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-2xl items-center justify-between px-4 py-3">
          <span className="text-lg font-semibold">Module Service · History lookup</span>
          <Link to="/login" className="text-sm text-slate-600 hover:text-slate-900">Login</Link>
        </div>
      </header>

      <main className="mx-auto max-w-2xl px-4 py-8">
        <h1 className="text-2xl font-semibold">Check a module's service history</h1>
        <p className="mt-1 text-sm text-slate-500">
          Enter the module number from the label, or scan its QR code.
        </p>

        <form onSubmit={submit} className="mt-5 flex flex-wrap items-center gap-2">
          <input
            className={`${inputClass} mt-0 flex-1`}
            placeholder="Module number…"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            autoFocus
          />
          <button type="submit" className={`${primaryButtonClass} w-auto`}>
            <Search className="h-4 w-4" /> Look up
          </button>
          <button type="button" className={secondaryButtonClass} onClick={() => setScanOpen(true)}>
            <QrCode className="h-4 w-4" /> Scan QR
          </button>
        </form>

        <div className="mt-8">
          {isLoading && <p className="text-slate-400">Searching…</p>}
          {isError && (
            <div className="rounded-lg border border-slate-200 bg-white p-6 text-center">
              <p className="font-medium text-slate-700">No service records found</p>
              <p className="mt-1 text-sm text-slate-500">{getErrorMessage(error)}</p>
            </div>
          )}
          {data && data.length > 0 && (
            <>
              <h2 className="mb-3 text-sm font-medium text-slate-500">
                Service history for <span className="font-semibold text-slate-700">{query}</span> · {data.length} visit{data.length > 1 ? 's' : ''}
              </h2>
              <ol className="space-y-4">
                {data.map((visit, i) => <VisitCard key={i} visit={visit} />)}
              </ol>
            </>
          )}
        </div>
      </main>

      {scanOpen && <QrScanModal onResult={onScan} onClose={() => setScanOpen(false)} />}
    </div>
  )
}

function VisitCard({ visit }: { visit: ModuleVisit }) {
  return (
    <li className="rounded-lg border border-slate-200 bg-white p-5">
      <div className="flex items-center justify-between">
        <span className="font-medium">{visit.problemTypeName}</span>
        <ModuleStatusBadge code={visit.statusCode} label={visit.statusName} />
      </div>
      <dl className="mt-3 grid grid-cols-2 gap-3 text-sm sm:grid-cols-3">
        <Field label="Pixels repaired" value={visit.pixelsRepaired?.toString() ?? '—'} />
        <Field label="Chips replaced" value={visit.chipsReplaced?.toString() ?? '—'} />
        <Field label="Repaired on" value={visit.completedAt ? formatDate(visit.completedAt) : '—'} />
        <Field label="Received" value={visit.receivedAt ? formatDate(visit.receivedAt) : '—'} />
        <Field label="Service completed" value={visit.serviceCompletedAt ? formatDate(visit.serviceCompletedAt) : '—'} />
        <Field label="Shipped" value={visit.shippedAt ? formatDate(visit.shippedAt) : '—'} />
        <Field label="Arrived" value={visit.arrivedAt ? formatDate(visit.arrivedAt) : '—'} />
      </dl>
      {visit.repairNote && <p className="mt-3 text-sm text-slate-600"><span className="text-slate-400">Note:</span> {visit.repairNote}</p>}
    </li>
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
