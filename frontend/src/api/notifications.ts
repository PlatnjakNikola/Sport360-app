import { api } from './client'
import type { Envelope, PageResponse } from '../types/common'
import type { AppNotification } from '../types/notification'
import type { Role } from '../types/auth'

// The backend exposes the same notification handlers under each role prefix, scoped to the
// current user. We build the path from the caller's role.
const base = (role: Role) => `/${role}/notifications`

export const notificationsApi = {
  async list(role: Role, page = 1, limit = 20, unread = false): Promise<PageResponse<AppNotification>> {
    return (await api.get<Envelope<PageResponse<AppNotification>>>(base(role), { params: { page, limit, unread: unread || undefined } })).data.data
  },
  async unreadCount(role: Role): Promise<number> {
    return (await api.get<Envelope<{ count: number }>>(`${base(role)}/unread-count`)).data.data.count
  },
  async markRead(role: Role, id: number): Promise<void> {
    await api.patch(`${base(role)}/${id}/read`)
  },
  async markAllRead(role: Role): Promise<void> {
    await api.post(`${base(role)}/mark-all-read`)
  },
}
