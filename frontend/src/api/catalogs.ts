import { api } from './client'
import type { Envelope } from '../types/common'
import type { ServiceCenter } from '../types/user'

export const catalogsApi = {
  // Active centers only — used for invite/edit dropdowns. The full catalog (incl. inactive)
  // is fetched via adminApi.serviceCenters() on the Catalogs page.
  async serviceCenters(): Promise<ServiceCenter[]> {
    const all = (await api.get<Envelope<ServiceCenter[]>>('/admin/service-centers')).data.data
    return all.filter((center) => center.active)
  },
}
