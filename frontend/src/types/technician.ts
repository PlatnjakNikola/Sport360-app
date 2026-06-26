import type { ActivityItem, StatusCount, TimelineEntry } from './package'

export interface TechnicianPackageSummary {
  id: number
  packageNumber: string
  statusCode: string
  statusName: string
  isInternal: boolean
  createdAt: string
  receivedAt?: string | null
  totalModules: number
  finishedModules: number
  totalValue: number
}

export interface TechnicianPackageDetail {
  id: number
  packageNumber: string
  statusCode: string
  statusName: string
  isInternal: boolean
  outboundTrackingLink?: string | null
  returnTrackingLink?: string | null
  description?: string | null
  note?: string | null
  approxQuantity?: number | null
  createdAt: string
  receivedAt?: string | null
  serviceStartedAt?: string | null
  serviceCompletedAt?: string | null
  shippedAt?: string | null
  arrivedAt?: string | null
  totalModules: number
  finishedModules: number
  timeline: TimelineEntry[]
}

export interface PersonalStats {
  modulesToday: number
  modulesThisWeek: number
  repairRatePercent: number
  totalValueThisWeek: number
}

export interface TechnicianDashboard {
  statusCounts: StatusCount[]
  personalStats: PersonalStats
  recentActivity: ActivityItem[]
}
