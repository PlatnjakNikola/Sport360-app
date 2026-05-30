import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Check, CheckCheck } from 'lucide-react'
import toast from 'react-hot-toast'
import { notificationsApi } from '../api/notifications'
import { getErrorMessage } from '../api/client'
import { useAuth } from '../hooks/useAuth'
import type { Role } from '../types/auth'
import type { AppNotification } from '../types/notification'
import { Pagination } from '../components/ui/Pagination'
import { formatDateTime } from '../utils/formatDate'
import { secondaryButtonClass } from '../components/ui/formStyles'

/** Builds the in-app link for a notification's related entity, or null if it has none. */
function linkFor(role: Role, n: AppNotification): string | null {
  if (n.entityType === 'package' && n.entityId != null) return `/${role}/packages/${n.entityId}`
  if (n.entityType === 'user' && role === 'admin') return '/admin/users'
  return null
}

export function NotificationsPage() {
  const { user } = useAuth()
  const role = (user?.role ?? 'client') as Role
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [page, setPage] = useState(1)
  const [unreadOnly, setUnreadOnly] = useState(false)

  const { data, isLoading } = useQuery({
    queryKey: ['notifications', role, page, unreadOnly],
    queryFn: () => notificationsApi.list(role, page, 20, unreadOnly),
  })

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['notifications'] })
    queryClient.invalidateQueries({ queryKey: ['unread-count'] })
  }

  const markRead = useMutation({
    mutationFn: (id: number) => notificationsApi.markRead(role, id),
    onSuccess: invalidate,
    onError: (e) => toast.error(getErrorMessage(e)),
  })
  const markAll = useMutation({
    mutationFn: () => notificationsApi.markAllRead(role),
    onSuccess: () => { toast.success('All marked as read'); invalidate() },
    onError: (e) => toast.error(getErrorMessage(e)),
  })

  const open = (n: AppNotification) => {
    if (!n.isRead) markRead.mutate(n.id)
    const link = linkFor(role, n)
    if (link) navigate(link)
  }

  const items = data?.items ?? []

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Notifications</h1>
        <button type="button" className={secondaryButtonClass} disabled={markAll.isPending} onClick={() => markAll.mutate()}>
          <CheckCheck className="h-4 w-4" /> Mark all read
        </button>
      </div>

      <div className="flex gap-2 text-sm">
        <button type="button" onClick={() => { setUnreadOnly(false); setPage(1) }} className={`rounded-md px-3 py-1.5 ${!unreadOnly ? 'bg-slate-800 text-white' : 'border border-slate-300 text-slate-600'}`}>All</button>
        <button type="button" onClick={() => { setUnreadOnly(true); setPage(1) }} className={`rounded-md px-3 py-1.5 ${unreadOnly ? 'bg-slate-800 text-white' : 'border border-slate-300 text-slate-600'}`}>Unread</button>
      </div>

      <section className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        {isLoading && <p className="px-4 py-6 text-center text-slate-400">Loading…</p>}
        {!isLoading && items.length === 0 && <p className="px-4 py-6 text-center text-slate-400">No notifications.</p>}
        <ul className="divide-y divide-slate-100">
          {items.map((n) => (
            <li
              key={n.id}
              onClick={() => open(n)}
              className={`flex cursor-pointer items-start gap-3 px-4 py-3 hover:bg-slate-50 ${n.isRead ? '' : 'bg-slate-50/60'}`}
            >
              <span className={`mt-1.5 h-2 w-2 flex-shrink-0 rounded-full ${n.isRead ? 'bg-transparent' : 'bg-blue-500'}`} />
              <div className="flex-1">
                <p className={`text-sm ${n.isRead ? 'text-slate-600' : 'font-medium text-slate-800'}`}>{n.title}</p>
                {n.message && <p className="text-sm text-slate-500">{n.message}</p>}
                <p className="mt-0.5 text-xs text-slate-400">{formatDateTime(n.createdAt)}</p>
              </div>
              {!n.isRead && (
                <button
                  type="button"
                  onClick={(e) => { e.stopPropagation(); markRead.mutate(n.id) }}
                  className="text-slate-400 hover:text-slate-700"
                  title="Mark as read"
                >
                  <Check className="h-4 w-4" />
                </button>
              )}
            </li>
          ))}
        </ul>
      </section>
      <Pagination page={page} totalPages={data?.pagination.totalPages ?? 1} onChange={setPage} />
    </div>
  )
}
