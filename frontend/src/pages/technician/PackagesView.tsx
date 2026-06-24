import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { technicianApi } from '../../api/technician'
import { useDebounce } from '../../hooks/useDebounce'
import { PackageStatusBadge } from '../../components/ui/PackageStatusBadge'
import { Pagination } from '../../components/ui/Pagination'
import { formatDate } from '../../utils/formatDate'
import { inputClass } from '../../components/ui/formStyles'

export function PackagesView({ mode }: { mode: 'active' | 'archive' }) {
  const [page, setPage] = useState(1)
  const [searchInput, setSearchInput] = useState('')
  const search = useDebounce(searchInput, 300)

  const { data, isLoading } = useQuery({
    queryKey: ['tech-packages', mode, page, search],
    queryFn: () =>
      mode === 'active'
        ? technicianApi.activePackages(page, 20, search)
        : technicianApi.archivePackages(page, 20, search),
    placeholderData: keepPreviousData,
  })

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">{mode === 'active' ? 'Active packages' : 'Archive'}</h1>

      <input
        className={`${inputClass} max-w-xs`}
        placeholder="Search by number…"
        value={searchInput}
        onChange={(event) => {
          setSearchInput(event.target.value)
          setPage(1)
        }}
      />

      <section className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">Package</th>
              <th className="px-4 py-2 font-medium">Status</th>
              <th className="px-4 py-2 font-medium">Created</th>
              <th className="px-4 py-2 font-medium">Modules</th>
              <th className="px-4 py-2 font-medium">Finished</th>
              <th className="px-4 py-2 font-medium">Value</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {isLoading && (
              <tr>
                <td colSpan={6} className="px-4 py-6 text-center text-slate-400">
                  Loading…
                </td>
              </tr>
            )}
            {data?.items.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-6 text-center text-slate-400">
                  No packages.
                </td>
              </tr>
            )}
            {data?.items.map((pkg) => (
              <tr key={pkg.id}>
                <td className="px-4 py-2">
                  <Link to={`/technician/packages/${pkg.id}`} className="font-medium text-slate-700 hover:underline">
                    {pkg.packageNumber}
                  </Link>
                </td>
                <td className="px-4 py-2">
                  <PackageStatusBadge code={pkg.statusCode} label={pkg.statusName} />
                </td>
                <td className="px-4 py-2">{formatDate(pkg.createdAt)}</td>
                <td className="px-4 py-2">{pkg.totalModules}</td>
                <td className="px-4 py-2">{pkg.finishedModules}</td>
                <td className="px-4 py-2">€{pkg.totalValue}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      {data && <Pagination page={data.pagination.page} totalPages={data.pagination.totalPages} onChange={setPage} />}
    </div>
  )
}
