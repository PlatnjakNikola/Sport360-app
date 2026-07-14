import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import toast from 'react-hot-toast'
import { usersApi } from '../../api/users'
import { getErrorMessage } from '../../api/client'
import type { Technician } from '../../types/user'
import { Pagination } from '../../components/ui/Pagination'
import { ConfirmModal } from '../../components/ui/ConfirmModal'
import { StatusBadge } from '../../components/ui/StatusBadge'
import { InviteTechnicianModal } from './InviteTechnicianModal'
import { EditTechnicianModal } from './EditTechnicianModal'
import { buttonClass, secondaryButtonClass } from '../../components/ui/formStyles'

export function TechniciansTab() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(1)
  const [showInvite, setShowInvite] = useState(false)
  const [editing, setEditing] = useState<Technician | null>(null)
  const [toggling, setToggling] = useState<Technician | null>(null)

  const techniciansQuery = useQuery({ queryKey: ['technicians', page], queryFn: () => usersApi.listTechnicians(page) })
  const invitesQuery = useQuery({ queryKey: ['technician-invites'], queryFn: usersApi.pendingTechnicianInvites })

  const resend = useMutation({
    mutationFn: usersApi.resendTechnicianInvite,
    onSuccess: () => toast.success('Invite resent'),
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  const toggleActive = useMutation({
    mutationFn: (technician: Technician) => usersApi.updateTechnician(technician.userId, { isActive: !technician.active }),
    onSuccess: () => {
      toast.success('Technician updated')
      queryClient.invalidateQueries({ queryKey: ['technicians'] })
      setToggling(null)
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  const technicians = techniciansQuery.data
  const invites = invitesQuery.data ?? []

  return (
    <div className="space-y-6">
      <div className="flex justify-end">
        <button type="button" className={buttonClass} onClick={() => setShowInvite(true)}>
          Invite technician
        </button>
      </div>

      {invites.length > 0 && (
        <section className="rounded-lg border border-slate-200 bg-white p-4">
          <h3 className="mb-2 text-sm font-medium text-slate-500">Pending invites</h3>
          <ul className="divide-y divide-slate-100 text-sm">
            {invites.map((invite) => (
              <li key={invite.id} className="flex items-center justify-between py-2">
                <span>
                  {invite.name} · {invite.email}
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
              <th className="px-4 py-2 font-medium">Name</th>
              <th className="px-4 py-2 font-medium">Email</th>
              <th className="px-4 py-2 font-medium">Service center</th>
              <th className="px-4 py-2 font-medium">Status</th>
              <th className="px-4 py-2 text-right font-medium">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {techniciansQuery.isLoading && (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-slate-400">
                  Loading…
                </td>
              </tr>
            )}
            {technicians?.items.length === 0 && (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-slate-400">
                  No technicians yet.
                </td>
              </tr>
            )}
            {technicians?.items.map((technician) => (
              <tr key={technician.userId}>
                <td className="px-4 py-2">
                  <Link
                    className="font-medium text-slate-700 hover:underline"
                    to={`/admin/users/technicians/${technician.userId}`}
                  >
                    {technician.name}
                  </Link>
                </td>
                <td className="px-4 py-2">{technician.email}</td>
                <td className="px-4 py-2">{technician.serviceCenterName}</td>
                <td className="px-4 py-2">
                  <StatusBadge active={technician.active} />
                </td>
                <td className="px-4 py-2">
                  <div className="flex justify-end gap-2">
                    <button type="button" className={secondaryButtonClass} onClick={() => setEditing(technician)}>
                      Edit
                    </button>
                    <button type="button" className={secondaryButtonClass} onClick={() => setToggling(technician)}>
                      {technician.active ? 'Deactivate' : 'Activate'}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      {technicians && (
        <Pagination page={technicians.pagination.page} totalPages={technicians.pagination.totalPages} onChange={setPage} />
      )}

      {showInvite && <InviteTechnicianModal onClose={() => setShowInvite(false)} />}
      {editing && <EditTechnicianModal technician={editing} onClose={() => setEditing(null)} />}
      {toggling && (
        <ConfirmModal
          title={toggling.active ? 'Deactivate technician' : 'Activate technician'}
          message={`Are you sure you want to ${toggling.active ? 'deactivate' : 'activate'} ${toggling.name}?`}
          confirmLabel={toggling.active ? 'Deactivate' : 'Activate'}
          loading={toggleActive.isPending}
          onConfirm={() => toggleActive.mutate(toggling)}
          onClose={() => setToggling(null)}
        />
      )}
    </div>
  )
}
