import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { adminApi } from '../../api/admin'
import { useAuth } from '../../hooks/useAuth'
import { PackageStatusBadge } from '../../components/ui/PackageStatusBadge'
import { formatDateTime } from '../../utils/formatDate'

export function AdminDashboardPage() {
  const { user } = useAuth()
  const { data, isLoading } = useQuery({ queryKey: ['admin-dashboard'], queryFn: adminApi.dashboard })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Admin dashboard</h1>
        <p className="mt-1 text-sm text-slate-500">Welcome, {user?.name}.</p>
      </div>

      {isLoading && <p className="text-slate-400">Loading…</p>}

      {data && (
        <>
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            <StatCard label="Total packages" value={data.totalPackages} />
            <StatCard label="Active" value={data.activePackages} />
            <StatCard label="Completed" value={data.completedPackages} />
            <StatCard label="Modules" value={data.totalModules} hint={`${data.repairedModules}✓ / ${data.notRepairableModules}✗`} />
          </div>

          <div className="grid grid-cols-1 gap-5 lg:grid-cols-3">
            <section className="rounded-lg border border-slate-200 bg-white p-5 lg:col-span-2">
              <h2 className="mb-3 text-sm font-medium text-slate-500">Packages by status</h2>
              <div className="space-y-2">
                {data.packagesByStatus.length === 0 && <p className="text-sm text-slate-400">No packages yet.</p>}
                {data.packagesByStatus.map((s) => (
                  <div key={s.code} className="flex items-center gap-3 text-sm">
                    <div className="w-48"><PackageStatusBadge code={s.code} label={s.name} /></div>
                    <div className="h-2 flex-1 overflow-hidden rounded bg-slate-100">
                      <div className="h-full bg-slate-400" style={{ width: barWidth(s.count, data.totalPackages) }} />
                    </div>
                    <span className="w-8 text-right font-medium">{s.count}</span>
                  </div>
                ))}
              </div>
            </section>

            <section className="rounded-lg border border-slate-200 bg-white p-5">
              <h2 className="mb-3 text-sm font-medium text-slate-500">Pending invites</h2>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between"><span className="text-slate-600">Technicians</span><span className="font-medium">{data.pendingTechnicianInvites}</span></div>
                <div className="flex justify-between"><span className="text-slate-600">Clients</span><span className="font-medium">{data.pendingClientInvites}</span></div>
              </div>
              <Link to="/admin/users" className="mt-3 inline-block text-sm text-slate-700 underline">Manage users</Link>
            </section>
          </div>

          <section className="rounded-lg border border-slate-200 bg-white p-5">
            <h2 className="mb-3 text-sm font-medium text-slate-500">Recent activity</h2>
            {data.recentActivity.length === 0 && <p className="text-sm text-slate-400">No recent activity.</p>}
            <ul className="divide-y divide-slate-100">
              {data.recentActivity.map((a, i) => (
                <li key={`${a.packageId}-${i}`} className="flex items-center justify-between py-2 text-sm">
                  <Link to={`/admin/packages/${a.packageId}`} className="font-medium hover:underline">{a.packageNumber}</Link>
                  <span className="flex items-center gap-3">
                    <PackageStatusBadge code={a.statusCode} label={a.statusName} />
                    <span className="text-slate-400">{formatDateTime(a.changedAt)}</span>
                  </span>
                </li>
              ))}
            </ul>
          </section>
        </>
      )}
    </div>
  )
}

function StatCard({ label, value, hint }: { label: string; value: number; hint?: string }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-1 text-2xl font-semibold">{value}</p>
      {hint && <p className="text-xs text-slate-400">{hint}</p>}
    </div>
  )
}

function barWidth(count: number, total: number): string {
  if (total <= 0) return '0%'
  return `${Math.round((count / total) * 100)}%`
}
