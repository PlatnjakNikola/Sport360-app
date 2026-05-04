export type Role = 'admin' | 'technician' | 'client'

export interface User {
  id: number
  name: string
  email: string
  role: Role
}

export interface LoginResult {
  mfaRequired: boolean
  mfaToken?: string
  user?: User
}
