import { api } from './client'
import type { Envelope, PageResponse } from '../types/common'
import type { Client, PendingClientInvite, PendingTechnicianInvite, Technician } from '../types/user'

export interface InviteTechnicianBody {
  email: string
  name: string
  serviceCenterId: number
  phone?: string
}

export interface InviteClientBody {
  email: string
  contactName: string
  companyName: string
  contactPhone?: string
  address?: string
}

export interface UpdateTechnicianBody {
  name?: string
  phone?: string
  serviceCenterId?: number
  isActive?: boolean
}

export const usersApi = {
  async listTechnicians(page = 1, limit = 20): Promise<PageResponse<Technician>> {
    return (await api.get<Envelope<PageResponse<Technician>>>('/admin/technicians', { params: { page, limit } })).data.data
  },
  async getTechnician(id: number): Promise<Technician> {
    return (await api.get<Envelope<Technician>>(`/admin/technicians/${id}`)).data.data
  },
  async updateTechnician(id: number, body: UpdateTechnicianBody): Promise<Technician> {
    return (await api.patch<Envelope<Technician>>(`/admin/technicians/${id}`, body)).data.data
  },
  async inviteTechnician(body: InviteTechnicianBody): Promise<void> {
    await api.post('/admin/technicians/invite', body)
  },
  async pendingTechnicianInvites(): Promise<PendingTechnicianInvite[]> {
    return (await api.get<Envelope<PendingTechnicianInvite[]>>('/admin/technicians/invites')).data.data
  },
  async resendTechnicianInvite(id: number): Promise<void> {
    await api.post(`/admin/technicians/invites/${id}/resend`)
  },

  async listClients(page = 1, limit = 20): Promise<PageResponse<Client>> {
    return (await api.get<Envelope<PageResponse<Client>>>('/admin/clients', { params: { page, limit } })).data.data
  },
  async getClient(id: number): Promise<Client> {
    return (await api.get<Envelope<Client>>(`/admin/clients/${id}`)).data.data
  },
  async inviteClient(body: InviteClientBody): Promise<void> {
    await api.post('/admin/clients/invite', body)
  },
  async pendingClientInvites(): Promise<PendingClientInvite[]> {
    return (await api.get<Envelope<PendingClientInvite[]>>('/admin/clients/invites')).data.data
  },
  async resendClientInvite(id: number): Promise<void> {
    await api.post(`/admin/clients/invites/${id}/resend`)
  },
}
