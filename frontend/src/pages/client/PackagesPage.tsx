import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { clientPackagesApi } from '../../api/packages'
import { useDebounce } from '../../hooks/useDebounce'
import { PackageStatusBadge } from '../../components/ui/PackageStatusBadge'
import { Pagination } from '../../components/ui/Pagination'
import { PACKAGE_STATUSES } from '../../utils/packageStatus'
import { formatDate } from '../../utils/formatDate'
import { buttonClass, inputClass } from '../../components/ui/formStyles'

export function PackagesPage() {
  const [page, setPage] = useState(1)
  const [status, setStatus] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const search = useDebounce(searchInput, 300)

  const { data, isLoading } = useQuery({
    queryKey: ['client-packages', page, status, search],
    queryFn: () => clientPackagesApi.list(page, 20, status, search),
    placeholderData: keepPreviousData,
  })

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Packages</h1>
        <Link to="/client/packages/new" className={buttonClass}>
          Create package
        </Link>
      </div>

      <div className="flex flex-wrap gap-3">
        <input
          className={`${inputClass} max-w-xs`}
          placeholder="Search by number…"
          value={searchInput}
          onChange={(event) => {
            setSearchInput(event.target.value)
            setPage(1)
          }}
        />
        <select
          className={`${inputClass} max-w-xs`}
          value={status}
          onChange={(event) => {
            setStatus(event.target.value)
            setPage(1)
          }}
        >
          <option value="">All statuses</option>
          {PACKAGE_STATUSES.map((option) => (
            <option key={option.code} value={option.code}>
              {option.name}
            </option>
          ))}
        </select>
      </div>

      <section className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">Package</th>
              <th className="px-4 py-2 font-medium">Status</th>
              <th className="px-4 py-2 font-medium">Created</th>
              <th className="px-4 py-2 font-medium">Updated</th>
              <th className="px-4 py-2 font-medium">Tracking</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {isLoading && (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-slate-400">
                  Loading…
                </td>
              </tr>
            )}
            {data?.items.length === 0 && (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-slate-400">
                  No packages found.
                </td>
              </tr>
            )}
            {data?.items.map((pkg) => (
              <tr key={pkg.id}>
                <td className="px-4 py-2">
                  <Link to={`/client/packages/${pkg.id}`} className="font-medium text-slate-700 hover:underline">
                    {pkg.packageNumber}
                  </Link>
                </td>
                <td className="px-4 py-2">
                  <PackageStatusBadge code={pkg.statusCode} label={pkg.statusName} />
                </td>
                <td className="px-4 py-2">{formatDate(pkg.createdAt)}</td>
                <td className="px-4 py-2">{formatDate(pkg.updatedAt)}</td>
                <td className="px-4 py-2">
                  {pkg.outboundTrackingLink ? (
                    <a href={pkg.outboundTrackingLink} target="_blank" rel="noreferrer" className="text-slate-600 underline">
                      Link
                    </a>
                  ) : (
                    '—'
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      {data && <Pagination page={data.pagination.page} totalPages={data.pagination.totalPages} onChange={setPage} />}
    </div>
  )
}
