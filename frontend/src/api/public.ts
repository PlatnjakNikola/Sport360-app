import { api } from './client'
import type { Envelope } from '../types/common'
import type { ModuleVisit } from '../types/public'

export const publicApi = {
  async moduleHistory(moduleNumber: string): Promise<ModuleVisit[]> {
    return (await api.get<Envelope<ModuleVisit[]>>(`/public/modules/${encodeURIComponent(moduleNumber)}/history`)).data.data
  },
}
