export interface ModuleImage {
  id: number
  url: string
}

export interface ModuleSummary {
  id: number
  moduleNumber: string
  problemTypeName: string
  statusCode: string
  statusName: string
  technicianName: string
  price?: number | null
  thumbnailUrl?: string | null
}

export interface ModuleDetail {
  id: number
  packageId: number
  moduleNumber: string
  problemTypeName: string
  statusCode: string
  statusName: string
  technicianName: string
  pixelsRepaired?: number | null
  chipsReplaced?: number | null
  repairNote?: string | null
  price?: number | null
  decisionStatusCode?: string | null
  completedAt?: string | null
  createdAt: string
  images: ModuleImage[]
}

export interface ClientModule {
  id: number
  packageId: number
  moduleNumber: string
  problemTypeName: string
  statusCode: string
  statusName: string
  pixelsRepaired?: number | null
  chipsReplaced?: number | null
  repairNote?: string | null
  price?: number | null
  images: ModuleImage[]
}

export interface ProblemType {
  id: number
  code: string
  name: string
}
