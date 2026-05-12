import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'

/** Requires an authenticated user; otherwise redirects to login. */
export function ProtectedRoute() {
  const { user, isLoading } = useAuth()
  if (isLoading) return <LoadingSpinner />
  if (!user) return <Navigate to="/login" replace />
  return <Outlet />
}
