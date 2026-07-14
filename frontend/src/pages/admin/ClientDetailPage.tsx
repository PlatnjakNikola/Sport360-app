import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { usersApi } from '../../api/users'
import { StatusBadge } from '../../components/ui/StatusBadge'

export function ClientDetailPage() {
  const { id } = useParams<{ id: string }>()
  const clientId = Number(id)
  const { data, isLoading, isError } = useQuery({
    queryKey: ['client', clientId],
    queryFn: () => usersApi.getClient(clientId),
  })

  if (isLoading) return <p className="text-slate-400">Loading…</p>
  if (isError || !data) return <p className="text-slate-500">Client not found.</p>

  return (
    <div className="space-y-4">
      <Link to="/admin/users" className="text-sm text-slate-500 hover:underline">
        ← Back to users
      </Link>
      <div className="flex items-center gap-3">
        <h1 className="text-2xl font-semibold">{data.companyName}</h1>
        <StatusBadge active={data.active} />
      </div>
      <section className="rounded-lg border border-slate-200 bg-white p-5">
        <dl className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
          <Field label="Contact" value={data.contactName} />
          <Field label="Email" value={data.email} />
          <Field label="Phone" value={data.contactPhone ?? '—'} />
          <Field label="Address" value={data.address ?? '—'} />
        </dl>
      </section>
    </div>
  )
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-slate-500">{label}</dt>
      <dd className="font-medium">{value}</dd>
    </div>
  )
}
