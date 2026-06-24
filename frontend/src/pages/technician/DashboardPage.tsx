import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { technicianApi } from '../../api/technician'
import { PackageStatusBadge } from '../../components/ui/PackageStatusBadge'
import { formatDateTime } from '../../utils/formatDate'
import { secondaryButtonClass } from '../../components/ui/formStyles'

export function TechnicianDashboardPage() {
  const { user } = useAuth()
  const { data } = useQuery({ queryKey: ['tech-dashboard'], queryFn: technicianApi.dashboard })
  const stats = data?.personalStats

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Service dashboard</h1>
        <p className="text-sm text-slate-500">{user?.name}</p>
      </div>

      <section className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard label="My modules today" value={stats?.modulesToday ?? 0} />
        <StatCard label="My modules this week" value={stats?.modulesThisWeek ?? 0} />
        <StatCard label="My repair rate" value={`${stats?.repairRatePercent ?? 0}%`} />
        <StatCard label="My value this week" value={`€${stats?.totalValueThisWeek ?? 0}`} />
      </section>

      <section>
        <h2 className="mb-2 text-sm font-medium text-slate-500">Packages in your center</h2>
        {data && data.statusCounts.length === 0 ? (
          <p className="text-sm text-slate-400">No active packages.</p>
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
        <Link to="/technician/packages" className={`${secondaryButtonClass} mt-3`}>
          Open active packages
        </Link>
      </section>

      <section className="rounded-lg border border-slate-200 bg-white p-4">
        <h2 className="mb-2 text-sm font-medium text-slate-500">Recent activity</h2>
        {data && data.recentActivity.length === 0 ? (
          <p className="text-sm text-slate-400">No recent activity.</p>
        ) : (
          <ul className="divide-y divide-slate-100 text-sm">
            {(data?.recentActivity ?? []).map((activity, index) => (
              <li key={index} className="flex items-center justify-between py-2">
                <Link to={`/technician/packages/${activity.packageId}`} className="font-medium text-slate-700 hover:underline">
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

function StatCard({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <div className="text-2xl font-semibold">{value}</div>
      <div className="mt-1 text-xs text-slate-500">{label}</div>
    </div>
  )
}
