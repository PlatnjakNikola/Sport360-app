import type { ModuleImage } from './module'
import type { TimelineEntry } from './package'

export interface AdminPackageSummary {
  id: number
  packageNumber: string
  companyName?: string | null
  isInternal: boolean
  statusCode: string
  statusName: string
  serviceCenterId?: number | null
  createdAt: string
  receivedAt?: string | null
  shippedAt?: string | null
  deleted: boolean
  totalModules: number
  repairedCount: number
  notRepairableCount: number
  totalValue: number
}

export interface AdminModuleSummary {
  id: number
  moduleNumber: string
  problemTypeName: string
  statusCode: string
  statusName: string
  technicianName: string
  price?: number | null
  thumbnailUrl?: string | null
}

export interface AdminPackageDetail {
  id: number
  packageNumber: string
  clientId?: number | null
  companyName?: string | null
  isInternal: boolean
  serviceCenterId?: number | null
  statusCode: string
  statusName: string
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
  deletedAt?: string | null
  totalModules: number
  repairedCount: number
  notRepairableCount: number
  totalValue: number
  averagePrice: number
  timeline: TimelineEntry[]
  modules: AdminModuleSummary[]
}

export interface AdminModuleDetail {
  id: number
  packageId: number
  packageNumber: string
  moduleNumber: string
  problemTypeId: number
  problemTypeName: string
  statusCode: string
  statusName: string
  technicianId: number
  technicianName: string
  pixelsRepaired?: number | null
  chipsReplaced?: number | null
  repairNote?: string | null
  price?: number | null
  decisionStatusCode?: string | null
  createdAt: string
  completedAt?: string | null
  deletedAt?: string | null
  images: ModuleImage[]
}

export interface TrashItem {
  entityType: 'package' | 'module'
  id: number
  label: string
  packageId?: number | null
  deletedAt?: string | null
}

export interface StatusOption {
  id: number
  code: string
  name: string
  sortOrder: number
}

export interface ProblemTypeAdmin {
  id: number
  code: string
  name: string
  sortOrder: number
  active: boolean
  usageCount: number
}

export interface ServiceCenterAdmin {
  id: number
  code: string
  name: string
  country?: string | null
  city?: string | null
  address?: string | null
  active: boolean
  technicianCount: number
}
