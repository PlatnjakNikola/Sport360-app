import { api } from './client'
import type { Envelope } from '../types/common'
import type { LoginResult, User } from '../types/auth'

export const authApi = {
  async login(email: string, password: string): Promise<LoginResult> {
    return (await api.post<Envelope<LoginResult>>('/auth/login', { email, password })).data.data
  },

  async verifyMfa(mfaToken: string, code: string): Promise<LoginResult> {
    return (await api.post<Envelope<LoginResult>>('/auth/mfa/verify', { mfaToken, code })).data.data
  },

  async resendMfa(mfaToken: string): Promise<void> {
    await api.post('/auth/mfa/resend', { mfaToken })
  },

  async logout(): Promise<void> {
    await api.post('/auth/logout')
  },

  async me(): Promise<User> {
    return (await api.get<Envelope<User>>('/auth/me')).data.data
  },

  async forgotPassword(email: string): Promise<void> {
    await api.post('/auth/forgot-password', { email })
  },

  async resetPassword(token: string, newPassword: string): Promise<void> {
    await api.post('/auth/reset-password', { token, newPassword })
  },

  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    await api.post('/auth/change-password', { currentPassword, newPassword })
  },
}
