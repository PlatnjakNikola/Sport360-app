export interface AppNotification {
  id: number
  type: string
  title: string
  message?: string | null
  entityType?: string | null
  entityId?: number | null
  isRead: boolean
  createdAt: string
}

export interface AuditLogEntry {
  id: number
  entityType: string
  entityId: number
  actionType: string
  changedByUserId: number
  changedByName?: string | null
  oldValueJson?: string | null
  newValueJson?: string | null
  createdAt: string
}
