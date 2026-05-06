import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { roleHome } from '../utils/roleHome'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'

/** For login / password pages: sends already-authenticated users to their dashboard. */
export function PublicRoute() {
  const { user, isLoading } = useAuth()
  if (isLoading) return <LoadingSpinner />
  if (user) return <Navigate to={roleHome(user.role)} replace />
  return <Outlet />
}
