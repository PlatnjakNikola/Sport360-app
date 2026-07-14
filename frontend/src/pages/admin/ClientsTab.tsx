import { useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import toast from 'react-hot-toast'
import { usersApi } from '../../api/users'
import { getErrorMessage } from '../../api/client'
import { Pagination } from '../../components/ui/Pagination'
import { StatusBadge } from '../../components/ui/StatusBadge'
import { InviteClientModal } from './InviteClientModal'
import { buttonClass, secondaryButtonClass } from '../../components/ui/formStyles'

export function ClientsTab() {
  const [page, setPage] = useState(1)
  const [showInvite, setShowInvite] = useState(false)

  const clientsQuery = useQuery({ queryKey: ['clients', page], queryFn: () => usersApi.listClients(page) })
  const invitesQuery = useQuery({ queryKey: ['client-invites'], queryFn: usersApi.pendingClientInvites })

  const resend = useMutation({
    mutationFn: usersApi.resendClientInvite,
    onSuccess: () => toast.success('Invite resent'),
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  const clients = clientsQuery.data
  const invites = invitesQuery.data ?? []

  return (
    <div className="space-y-6">
      <div className="flex justify-end">
        <button type="button" className={buttonClass} onClick={() => setShowInvite(true)}>
          Invite client
        </button>
      </div>

      {invites.length > 0 && (
        <section className="rounded-lg border border-slate-200 bg-white p-4">
          <h3 className="mb-2 text-sm font-medium text-slate-500">Pending invites</h3>
          <ul className="divide-y divide-slate-100 text-sm">
            {invites.map((invite) => (
              <li key={invite.id} className="flex items-center justify-between py-2">
                <span>
                  {invite.companyName} · {invite.email}
                </span>
                <button
                  type="button"
                  className={secondaryButtonClass}
                  disabled={resend.isPending}
                  onClick={() => resend.mutate(invite.id)}
                >
                  Resend
                </button>
              </li>
            ))}
          </ul>
        </section>
      )}

      <section className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">Company</th>
              <th className="px-4 py-2 font-medium">Contact</th>
              <th className="px-4 py-2 font-medium">Email</th>
              <th className="px-4 py-2 font-medium">Phone</th>
              <th className="px-4 py-2 font-medium">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {clientsQuery.isLoading && (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-slate-400">
                  Loading…
                </td>
              </tr>
            )}
            {clients?.items.length === 0 && (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-slate-400">
                  No clients yet.
                </td>
              </tr>
            )}
            {clients?.items.map((client) => (
              <tr key={client.userId}>
                <td className="px-4 py-2">
                  <Link
                    className="font-medium text-slate-700 hover:underline"
                    to={`/admin/users/clients/${client.userId}`}
                  >
                    {client.companyName}
                  </Link>
                </td>
                <td className="px-4 py-2">{client.contactName}</td>
                <td className="px-4 py-2">{client.email}</td>
                <td className="px-4 py-2">{client.contactPhone ?? '—'}</td>
                <td className="px-4 py-2">
                  <StatusBadge active={client.active} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      {clients && (
        <Pagination page={clients.pagination.page} totalPages={clients.pagination.totalPages} onChange={setPage} />
      )}

      {showInvite && <InviteClientModal onClose={() => setShowInvite(false)} />}
    </div>
  )
}
