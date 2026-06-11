import { api } from './client'
import type { Envelope, PageResponse } from '../types/common'
import type { ClientDashboard, PackageDetail, PackageSummary } from '../types/package'

export interface CreatePackageBody {
  packageNumber: string
  description: string
  note?: string
  outboundTrackingLink?: string
  approxQuantity?: number
}

export interface UpdatePackageBody {
  outboundTrackingLink?: string
  note?: string
  description?: string
}

export const clientPackagesApi = {
  async dashboard(): Promise<ClientDashboard> {
    return (await api.get<Envelope<ClientDashboard>>('/client/dashboard')).data.data
  },
  async list(page = 1, limit = 20, status?: string, search?: string): Promise<PageResponse<PackageSummary>> {
    return (
      await api.get<Envelope<PageResponse<PackageSummary>>>('/client/packages', {
        params: { page, limit, status: status || undefined, search: search || undefined },
      })
    ).data.data
  },
  async create(body: CreatePackageBody): Promise<PackageDetail> {
    return (await api.post<Envelope<PackageDetail>>('/client/packages', body)).data.data
  },
  async get(id: number): Promise<PackageDetail> {
    return (await api.get<Envelope<PackageDetail>>(`/client/packages/${id}`)).data.data
  },
  async update(id: number, body: UpdatePackageBody): Promise<PackageDetail> {
    return (await api.patch<Envelope<PackageDetail>>(`/client/packages/${id}`, body)).data.data
  },
  async markSent(id: number): Promise<PackageDetail> {
    return (await api.post<Envelope<PackageDetail>>(`/client/packages/${id}/mark-sent`)).data.data
  },
}
