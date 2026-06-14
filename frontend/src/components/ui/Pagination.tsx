import { secondaryButtonClass } from './formStyles'

export function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number
  totalPages: number
  onChange: (page: number) => void
}) {
  if (totalPages <= 1) return null
  return (
    <div className="mt-4 flex items-center justify-between text-sm text-slate-600">
      <button type="button" disabled={page <= 1} onClick={() => onChange(page - 1)} className={secondaryButtonClass}>
        Previous
      </button>
      <span>
        Page {page} of {totalPages}
      </span>
      <button type="button" disabled={page >= totalPages} onClick={() => onChange(page + 1)} className={secondaryButtonClass}>
        Next
      </button>
    </div>
  )
}
