import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { clientPackagesApi } from '../../api/packages'
import { PackageStatusBadge } from '../../components/ui/PackageStatusBadge'
import { formatDateTime } from '../../utils/formatDate'
import { buttonClass, secondaryButtonClass } from '../../components/ui/formStyles'

export function ClientDashboardPage() {
  const { user } = useAuth()
  const { data } = useQuery({ queryKey: ['client-dashboard'], queryFn: clientPackagesApi.dashboard })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Welcome, {user?.name}</h1>
        <div className="flex gap-2">
          <Link to="/client/packages/new" className={buttonClass}>
            Create package
          </Link>
          <Link to="/client/packages" className={secondaryButtonClass}>
            All packages
          </Link>
        </div>
      </div>

      <section>
        <h2 className="mb-2 text-sm font-medium text-slate-500">Packages by status</h2>
        {data && data.statusCounts.length === 0 ? (
          <p className="text-sm text-slate-400">No packages yet. Create your first one.</p>
        ) : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {(data?.statusCounts ?? []).map((status) => (
              <div key={status.code} className="rounded-lg border border-slate-200 bg-white p-4">
                <div className="text-2xl font-semibold">{status.count}</div>
                <div className="mt-1">
                  <PackageStatusBadge code={status.code} label={status.name} />
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="rounded-lg border border-slate-200 bg-white p-4">
        <h2 className="mb-2 text-sm font-medium text-slate-500">Recent activity</h2>
        {data && data.recentActivity.length === 0 ? (
          <p className="text-sm text-slate-400">No recent activity.</p>
        ) : (
          <ul className="divide-y divide-slate-100 text-sm">
            {(data?.recentActivity ?? []).map((activity, index) => (
              <li key={index} className="flex items-center justify-between py-2">
                <Link
                  to={`/client/packages/${activity.packageId}`}
                  className="font-medium text-slate-700 hover:underline"
                >
                  {activity.packageNumber}
                </Link>
                <span className="flex items-center gap-3 text-slate-500">
                  <PackageStatusBadge code={activity.statusCode} label={activity.statusName} />
                  {formatDateTime(activity.changedAt)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
