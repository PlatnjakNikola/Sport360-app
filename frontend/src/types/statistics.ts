import type { ActivityItem } from './package'

export interface CountItem {
  label: string
  count: number
}

export interface StatusCountItem {
  code: string
  name: string
  count: number
}

export interface PeriodCountItem {
  period: string
  count: number
}

export interface GlobalStatistics {
  totalPackages: number
  packagesByStatus: StatusCountItem[]
  packagesByClient: CountItem[]
  packagesByMonth: PeriodCountItem[]
  totalModules: number
  repairedModules: number
  notRepairableModules: number
  modulesByProblemType: CountItem[]
}

export interface TechnicianBreakdownItem {
  technicianId: number
  technicianName: string
  moduleCount: number
  repairedCount: number
  notRepairableCount: number
  totalPixels: number
  totalChips: number
  totalValue: number
}

export interface PackageStatistics {
  packageId: number
  totalModules: number
  repairedCount: number
  notRepairableCount: number
  totalPixels: number
  totalChips: number
  totalValue: number
  modulesByProblemType: CountItem[]
  technicianBreakdown: TechnicianBreakdownItem[]
}

export interface DashboardStats {
  totalPackages: number
  activePackages: number
  completedPackages: number
  packagesByStatus: StatusCountItem[]
  totalModules: number
  repairedModules: number
  notRepairableModules: number
  pendingTechnicianInvites: number
  pendingClientInvites: number
  recentActivity: ActivityItem[]
}

export interface StatisticsFilters {
  filter?: string
  dateFrom?: string
  dateTo?: string
}
