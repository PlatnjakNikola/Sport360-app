export interface ModuleVisit {
  statusCode: string
  statusName: string
  problemTypeName: string
  pixelsRepaired?: number | null
  chipsReplaced?: number | null
  repairNote?: string | null
  completedAt?: string | null
  receivedAt?: string | null
  serviceCompletedAt?: string | null
  shippedAt?: string | null
  arrivedAt?: string | null
}
