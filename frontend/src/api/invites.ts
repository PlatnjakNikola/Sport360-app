import { api } from './client'
import type { Envelope } from '../types/common'
import type { InviteValidation } from '../types/user'

export const inviteApi = {
  async validate(token: string): Promise<InviteValidation> {
    return (await api.get<Envelope<InviteValidation>>(`/auth/invite/${token}`)).data.data
  },
  async accept(token: string, password: string): Promise<void> {
    await api.post('/auth/accept-invite', { token, password })
  },
}
