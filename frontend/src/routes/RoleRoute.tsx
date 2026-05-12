import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import type { Role } from '../types/auth'

/** Guards a branch by role. Assumes it is nested inside ProtectedRoute. */
export function RoleRoute({ role }: { role: Role }) {
  const { user } = useAuth()
  if (user && user.role !== role) return <Navigate to="/unauthorized" replace />
  return <Outlet />
}
