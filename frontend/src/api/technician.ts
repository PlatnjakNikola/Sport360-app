import { api } from './client'
import type { Envelope, PageResponse } from '../types/common'
import type { ClientModule, ModuleDetail, ModuleImage, ModuleSummary, ProblemType } from '../types/module'
import type { TechnicianDashboard, TechnicianPackageDetail, TechnicianPackageSummary } from '../types/technician'

export interface CreateModuleBody {
  moduleNumber: string
  problemTypeId: number
}

export interface RepairBody {
  decision: 'repaired' | 'not_repairable'
  pixelsRepaired?: number
  chipsReplaced?: number
  repairNote?: string
  price?: number
}

export const technicianApi = {
  async dashboard(): Promise<TechnicianDashboard> {
    return (await api.get<Envelope<TechnicianDashboard>>('/technician/dashboard')).data.data
  },
  async activePackages(page = 1, limit = 20, search?: string): Promise<PageResponse<TechnicianPackageSummary>> {
    return (await api.get<Envelope<PageResponse<TechnicianPackageSummary>>>('/technician/packages/active', { params: { page, limit, search: search || undefined } })).data.data
  },
  async archivePackages(page = 1, limit = 20, search?: string): Promise<PageResponse<TechnicianPackageSummary>> {
    return (await api.get<Envelope<PageResponse<TechnicianPackageSummary>>>('/technician/packages/archive', { params: { page, limit, search: search || undefined } })).data.data
  },
  async packageDetail(id: number): Promise<TechnicianPackageDetail> {
    return (await api.get<Envelope<TechnicianPackageDetail>>(`/technician/packages/${id}`)).data.data
  },
  async nextStatus(id: number, returnTrackingLink?: string): Promise<TechnicianPackageDetail> {
    return (await api.post<Envelope<TechnicianPackageDetail>>(`/technician/packages/${id}/next-status`, { returnTrackingLink })).data.data
  },
  async listModules(id: number, page = 1, limit = 50): Promise<PageResponse<ModuleSummary>> {
    return (await api.get<Envelope<PageResponse<ModuleSummary>>>(`/technician/packages/${id}/modules`, { params: { page, limit } })).data.data
  },
  async findModule(id: number, number: string): Promise<ModuleSummary | null> {
    return (await api.get<Envelope<ModuleSummary | null>>(`/technician/packages/${id}/modules/find`, { params: { number } })).data.data ?? null
  },
  async createModule(id: number, body: CreateModuleBody): Promise<ModuleDetail> {
    return (await api.post<Envelope<ModuleDetail>>(`/technician/packages/${id}/modules`, body)).data.data
  },
  async moduleDetail(id: number): Promise<ModuleDetail> {
    return (await api.get<Envelope<ModuleDetail>>(`/technician/modules/${id}`)).data.data
  },
  async repair(id: number, body: RepairBody): Promise<ModuleDetail> {
    return (await api.post<Envelope<ModuleDetail>>(`/technician/modules/${id}/repair`, body)).data.data
  },
  async uploadModuleImages(id: number, files: File[]): Promise<ModuleImage[]> {
    const form = new FormData()
    files.forEach((file) => form.append('files', file))
    return (await api.post<Envelope<ModuleImage[]>>(`/technician/modules/${id}/images`, form)).data.data
  },
  async problemTypes(): Promise<ProblemType[]> {
    return (await api.get<Envelope<ProblemType[]>>('/technician/problem-types')).data.data
  },
  async listInternalPackages(page = 1, limit = 20, status?: string, search?: string): Promise<PageResponse<TechnicianPackageSummary>> {
    return (await api.get<Envelope<PageResponse<TechnicianPackageSummary>>>('/technician/internal-packages', { params: { page, limit, status: status || undefined, search: search || undefined } })).data.data
  },
  async createInternalPackage(body: CreateInternalPackageBody): Promise<TechnicianPackageDetail> {
    return (await api.post<Envelope<TechnicianPackageDetail>>('/technician/internal-packages', body)).data.data
  },
  async confirmArrivalInternal(id: number): Promise<TechnicianPackageDetail> {
    return (await api.post<Envelope<TechnicianPackageDetail>>(`/technician/internal-packages/${id}/confirm-arrival`)).data.data
  },
}

export interface CreateInternalPackageBody {
  packageNumber: string
  description?: string
  note?: string
  approxQuantity?: number
}

export const clientModulesApi = {
  async confirmArrival(id: number): Promise<void> {
    await api.post(`/client/packages/${id}/confirm-arrival`)
  },
  async listModules(id: number, page = 1, limit = 50): Promise<PageResponse<ClientModule>> {
    return (await api.get<Envelope<PageResponse<ClientModule>>>(`/client/packages/${id}/modules`, { params: { page, limit } })).data.data
  },
  async moduleDetail(moduleId: number): Promise<ClientModule> {
    return (await api.get<Envelope<ClientModule>>(`/client/modules/${moduleId}`)).data.data
  },
}
