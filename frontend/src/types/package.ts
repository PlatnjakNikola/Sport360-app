export interface PackageSummary {
  id: number
  packageNumber: string
  statusCode: string
  statusName: string
  outboundTrackingLink?: string | null
  returnTrackingLink?: string | null
  createdAt: string
  updatedAt: string
}

export interface TimelineEntry {
  statusCode: string
  statusName: string
  changedAt: string
}

export interface PackageDetail {
  id: number
  packageNumber: string
  statusCode: string
  statusName: string
  outboundTrackingLink?: string | null
  returnTrackingLink?: string | null
  description?: string | null
  note?: string | null
  approxQuantity?: number | null
  createdAt: string
  receivedAt?: string | null
  serviceCompletedAt?: string | null
  shippedAt?: string | null
  arrivedAt?: string | null
  timeline: TimelineEntry[]
}

export interface StatusCount {
  code: string
  name: string
  count: number
}

export interface ActivityItem {
  packageId: number
  packageNumber: string
  statusCode: string
  statusName: string
  changedAt: string
}

export interface ClientDashboard {
  statusCounts: StatusCount[]
  recentActivity: ActivityItem[]
}
