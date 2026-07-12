import { api } from './client'
import type { Envelope, PageResponse } from '../types/common'
import type {
  AdminModuleDetail,
  AdminPackageDetail,
  AdminPackageSummary,
  ProblemTypeAdmin,
  ServiceCenterAdmin,
  StatusOption,
  TrashItem,
} from '../types/admin'
import type { DashboardStats, GlobalStatistics, PackageStatistics, StatisticsFilters } from '../types/statistics'
import type { AuditLogEntry } from '../types/notification'

export interface AuditFilters {
  page?: number
  limit?: number
  entityType?: string
  actionType?: string
  dateFrom?: string
  dateTo?: string
}

export interface PackageFilters {
  page?: number
  limit?: number
  includeDeleted?: boolean
  status?: string
  serviceCenterId?: number
  type?: string
  search?: string
}

export interface UpdatePackageBody {
  packageNumber?: string
  description?: string
  note?: string
  outboundTrackingLink?: string
  returnTrackingLink?: string
}

export interface CorrectionModuleBody {
  moduleNumber: string
  problemTypeId: number
  assignedTechnicianId: number
}

export interface UpdateModuleBody {
  problemTypeId?: number
  assignedTechnicianId?: number
  statusCode?: string
  pixelsRepaired?: number
  chipsReplaced?: number
  price?: number
  repairNote?: string
}

export interface CreateProblemTypeBody {
  code: string
  name: string
  sortOrder: number
}

export interface UpdateProblemTypeBody {
  name?: string
  sortOrder?: number
  active?: boolean
}

export interface CreateServiceCenterBody {
  code: string
  name: string
  country?: string
  city?: string
  address?: string
}

export interface UpdateServiceCenterBody {
  name?: string
  country?: string
  city?: string
  address?: string
  active?: boolean
}

export const adminApi = {
  // packages
  async listPackages(filters: PackageFilters): Promise<PageResponse<AdminPackageSummary>> {
    const params = {
      page: filters.page ?? 1,
      limit: filters.limit ?? 20,
      includeDeleted: filters.includeDeleted || undefined,
      status: filters.status || undefined,
      serviceCenterId: filters.serviceCenterId || undefined,
      type: filters.type && filters.type !== 'all' ? filters.type : undefined,
      search: filters.search || undefined,
    }
    return (await api.get<Envelope<PageResponse<AdminPackageSummary>>>('/admin/packages', { params })).data.data
  },
  async packageDetail(id: number): Promise<AdminPackageDetail> {
    return (await api.get<Envelope<AdminPackageDetail>>(`/admin/packages/${id}`)).data.data
  },
  async updatePackage(id: number, body: UpdatePackageBody): Promise<AdminPackageDetail> {
    return (await api.patch<Envelope<AdminPackageDetail>>(`/admin/packages/${id}`, body)).data.data
  },
  async overrideStatus(id: number, statusId: number): Promise<AdminPackageDetail> {
    return (await api.patch<Envelope<AdminPackageDetail>>(`/admin/packages/${id}/status`, { statusId })).data.data
  },
  async deletePackage(id: number): Promise<void> {
    await api.delete(`/admin/packages/${id}`)
  },
  async restorePackage(id: number): Promise<AdminPackageDetail> {
    return (await api.post<Envelope<AdminPackageDetail>>(`/admin/packages/${id}/restore`)).data.data
  },
  async bulkDelete(packageIds: number[]): Promise<number> {
    return (await api.post<Envelope<{ affected: number }>>('/admin/packages/bulk-delete', { packageIds })).data.data.affected
  },
  async bulkStatus(packageIds: number[], statusId: number): Promise<number> {
    return (await api.post<Envelope<{ affected: number }>>('/admin/packages/bulk-status', { packageIds, statusId })).data.data.affected
  },
  async addCorrectionModule(packageId: number, body: CorrectionModuleBody): Promise<AdminModuleDetail> {
    return (await api.post<Envelope<AdminModuleDetail>>(`/admin/packages/${packageId}/modules`, body)).data.data
  },

  // modules
  async moduleDetail(id: number): Promise<AdminModuleDetail> {
    return (await api.get<Envelope<AdminModuleDetail>>(`/admin/modules/${id}`)).data.data
  },
  async updateModule(id: number, body: UpdateModuleBody): Promise<AdminModuleDetail> {
    return (await api.patch<Envelope<AdminModuleDetail>>(`/admin/modules/${id}`, body)).data.data
  },
  async deleteModule(id: number): Promise<void> {
    await api.delete(`/admin/modules/${id}`)
  },
  async restoreModule(id: number): Promise<AdminModuleDetail> {
    return (await api.post<Envelope<AdminModuleDetail>>(`/admin/modules/${id}/restore`)).data.data
  },

  // trash
  async trash(): Promise<TrashItem[]> {
    return (await api.get<Envelope<TrashItem[]>>('/admin/trash')).data.data
  },

  // lookups
  async packageStatuses(): Promise<StatusOption[]> {
    return (await api.get<Envelope<StatusOption[]>>('/admin/package-statuses')).data.data
  },
  async moduleStatuses(): Promise<StatusOption[]> {
    return (await api.get<Envelope<StatusOption[]>>('/admin/module-statuses')).data.data
  },

  // catalogs
  async problemTypes(): Promise<ProblemTypeAdmin[]> {
    return (await api.get<Envelope<ProblemTypeAdmin[]>>('/admin/problem-types')).data.data
  },
  async createProblemType(body: CreateProblemTypeBody): Promise<ProblemTypeAdmin> {
    return (await api.post<Envelope<ProblemTypeAdmin>>('/admin/problem-types', body)).data.data
  },
  async updateProblemType(id: number, body: UpdateProblemTypeBody): Promise<ProblemTypeAdmin> {
    return (await api.patch<Envelope<ProblemTypeAdmin>>(`/admin/problem-types/${id}`, body)).data.data
  },
  async serviceCenters(): Promise<ServiceCenterAdmin[]> {
    return (await api.get<Envelope<ServiceCenterAdmin[]>>('/admin/service-centers')).data.data
  },
  async createServiceCenter(body: CreateServiceCenterBody): Promise<ServiceCenterAdmin> {
    return (await api.post<Envelope<ServiceCenterAdmin>>('/admin/service-centers', body)).data.data
  },
  async updateServiceCenter(id: number, body: UpdateServiceCenterBody): Promise<ServiceCenterAdmin> {
    return (await api.patch<Envelope<ServiceCenterAdmin>>(`/admin/service-centers/${id}`, body)).data.data
  },

  // statistics
  async dashboard(): Promise<DashboardStats> {
    return (await api.get<Envelope<DashboardStats>>('/admin/dashboard')).data.data
  },
  async statistics(filters: StatisticsFilters): Promise<GlobalStatistics> {
    const params = {
      filter: filters.filter && filters.filter !== 'all' ? filters.filter : undefined,
      dateFrom: filters.dateFrom || undefined,
      dateTo: filters.dateTo || undefined,
    }
    return (await api.get<Envelope<GlobalStatistics>>('/admin/statistics', { params })).data.data
  },
  async packageStatistics(id: number): Promise<PackageStatistics> {
    return (await api.get<Envelope<PackageStatistics>>(`/admin/packages/${id}/statistics`)).data.data
  },
  async auditLogs(filters: AuditFilters): Promise<PageResponse<AuditLogEntry>> {
    const params = {
      page: filters.page ?? 1,
      limit: filters.limit ?? 20,
      entityType: filters.entityType || undefined,
      actionType: filters.actionType || undefined,
      dateFrom: filters.dateFrom || undefined,
      dateTo: filters.dateTo || undefined,
    }
    return (await api.get<Envelope<PageResponse<AuditLogEntry>>>('/admin/audit-logs', { params })).data.data
  },
  async downloadStatisticsCsv(filters: StatisticsFilters): Promise<void> {
    const response = await api.get('/admin/statistics/export.csv', {
      params: {
        filter: filters.filter && filters.filter !== 'all' ? filters.filter : undefined,
        dateFrom: filters.dateFrom || undefined,
        dateTo: filters.dateTo || undefined,
      },
      responseType: 'blob',
    })
    const url = URL.createObjectURL(response.data as Blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'statistics.csv'
    link.click()
    URL.revokeObjectURL(url)
  },
}
