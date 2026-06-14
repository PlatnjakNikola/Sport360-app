import type { TimelineEntry } from '../../types/package'
import { formatDateTime } from '../../utils/formatDate'

export function Timeline({ entries }: { entries: TimelineEntry[] }) {
  if (entries.length === 0) return <p className="text-sm text-slate-400">No history yet.</p>
  return (
    <ol className="space-y-3">
      {entries.map((entry, index) => (
        <li key={`${entry.statusCode}-${index}`} className="flex gap-3">
          <div className="mt-1 flex flex-col items-center">
            <span className="h-2 w-2 rounded-full bg-slate-400" />
            {index < entries.length - 1 && <span className="mt-1 h-full w-px flex-1 bg-slate-200" />}
          </div>
          <div className="pb-1">
            <p className="text-sm font-medium text-slate-700">{entry.statusName}</p>
            <p className="text-xs text-slate-400">{formatDateTime(entry.changedAt)}</p>
          </div>
        </li>
      ))}
    </ol>
  )
}
