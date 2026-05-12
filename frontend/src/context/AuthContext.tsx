import { createContext, useEffect, useState, type ReactNode } from 'react'
import { authApi } from '../api/auth'
import type { LoginResult, User } from '../types/auth'

interface AuthContextValue {
  user: User | null
  isLoading: boolean
  login: (email: string, password: string) => Promise<LoginResult>
  verifyMfa: (mfaToken: string, code: string) => Promise<User>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    authApi
      .me()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setIsLoading(false))
  }, [])

  const login = async (email: string, password: string): Promise<LoginResult> => {
    const result = await authApi.login(email, password)
    if (!result.mfaRequired && result.user) setUser(result.user)
    return result
  }

  const verifyMfa = async (mfaToken: string, code: string): Promise<User> => {
    const result = await authApi.verifyMfa(mfaToken, code)
    if (!result.user) throw new Error('Verification failed')
    setUser(result.user)
    return result.user
  }

  const logout = async (): Promise<void> => {
    await authApi.logout().catch(() => undefined)
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, isLoading, login, verifyMfa, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
