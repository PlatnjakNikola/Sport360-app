export interface Technician {
  userId: number
  name: string
  email: string
  phone?: string | null
  serviceCenterId: number
  serviceCenterName: string
  active: boolean
  createdAt: string
}

export interface Client {
  userId: number
  companyName: string
  contactName: string
  email: string
  contactPhone?: string | null
  address?: string | null
  active: boolean
  createdAt: string
}

export interface PendingTechnicianInvite {
  id: number
  email: string
  name: string
  serviceCenterId: number
  createdAt: string
  expiresAt: string
}

export interface PendingClientInvite {
  id: number
  email: string
  contactName: string
  companyName: string
  createdAt: string
  expiresAt: string
}

export interface ServiceCenter {
  id: number
  code: string
  name: string
  country?: string | null
  city?: string | null
  active: boolean
}

export interface InviteValidation {
  type: 'technician' | 'client'
  email: string
  name?: string
  serviceCenterId?: number
  serviceCenterName?: string
  companyName?: string
  contactPhone?: string
  address?: string
}
