import { QueryCache, QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import toast, { Toaster } from 'react-hot-toast'
import { getErrorMessage } from './api/client'
import type { ApiErrorShape } from './types/common'
import { AuthProvider } from './context/AuthContext'
import { ProtectedRoute } from './routes/ProtectedRoute'
import { PublicRoute } from './routes/PublicRoute'
import { RoleRoute } from './routes/RoleRoute'
import { AppLayout } from './components/layout/AppLayout'
import { LoginPage } from './pages/auth/LoginPage'
import { ForgotPasswordPage } from './pages/auth/ForgotPasswordPage'
import { ResetPasswordPage } from './pages/auth/ResetPasswordPage'
import { AcceptInvitePage } from './pages/auth/AcceptInvitePage'
import { ProfilePage } from './pages/profile/ProfilePage'
import { AdminDashboardPage } from './pages/admin/DashboardPage'
import { UsersPage } from './pages/admin/UsersPage'
import { TechnicianDetailPage } from './pages/admin/TechnicianDetailPage'
import { ClientDetailPage } from './pages/admin/ClientDetailPage'
import { AllPackagesPage } from './pages/admin/AllPackagesPage'
import { AdminPackageDetailPage } from './pages/admin/AdminPackageDetailPage'
import { AdminModuleDetailPage } from './pages/admin/AdminModuleDetailPage'
import { TrashPage } from './pages/admin/TrashPage'
import { CatalogsPage } from './pages/admin/CatalogsPage'
import { StatisticsPage } from './pages/admin/StatisticsPage'
import { AuditLogPage } from './pages/admin/AuditLogPage'
import { NotificationsPage } from './pages/NotificationsPage'
import { ClientDashboardPage } from './pages/client/DashboardPage'
import { PackagesPage } from './pages/client/PackagesPage'
import { CreatePackagePage } from './pages/client/CreatePackagePage'
import { PackageDetailPage } from './pages/client/PackageDetailPage'
import { ClientModuleDetailPage } from './pages/client/ClientModuleDetailPage'
import { TechnicianDashboardPage } from './pages/technician/DashboardPage'
import { ActivePackagesPage } from './pages/technician/ActivePackagesPage'
import { ArchivePage } from './pages/technician/ArchivePage'
import { TechnicianPackageDetailPage } from './pages/technician/PackageDetailPage'
import { RepairPage } from './pages/technician/RepairPage'
import { TechnicianModuleDetailPage } from './pages/technician/ModuleDetailPage'
import { InternalPackagesPage } from './pages/technician/InternalPackagesPage'
import { CreateInternalPackagePage } from './pages/technician/CreateInternalPackagePage'
import { PublicLookupPage } from './pages/public/PublicLookupPage'
import { NotFoundPage } from './pages/error/NotFoundPage'
import { UnauthorizedPage } from './pages/error/UnauthorizedPage'

const queryClient = new QueryClient({
  // Surface failed data loads as a toast. Auth failures are handled by the axios interceptor,
  // and a query can opt out (e.g. the public lookup shows its own inline "not found" state).
  queryCache: new QueryCache({
    onError: (error, query) => {
      if (query.meta?.suppressErrorToast) return
      if ((error as unknown as ApiErrorShape)?.code === 'UNAUTHORIZED') return
      toast.error(getErrorMessage(error))
    },
  }),
  defaultOptions: { queries: { staleTime: 30_000, retry: 1 } },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            {/* Public — redirect to dashboard if already signed in */}
            <Route element={<PublicRoute />}>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/forgot-password" element={<ForgotPasswordPage />} />
              <Route path="/reset-password/:token" element={<ResetPasswordPage />} />
              <Route path="/accept-invite/:token" element={<AcceptInvitePage />} />
            </Route>

            {/* Authenticated */}
            <Route element={<ProtectedRoute />}>
              <Route element={<AppLayout />}>
                <Route path="/profile" element={<ProfilePage />} />
                <Route element={<RoleRoute role="admin" />}>
                  <Route path="/admin" element={<AdminDashboardPage />} />
                  <Route path="/admin/users" element={<UsersPage />} />
                  <Route path="/admin/users/technicians/:id" element={<TechnicianDetailPage />} />
                  <Route path="/admin/users/clients/:id" element={<ClientDetailPage />} />
                  <Route path="/admin/statistics" element={<StatisticsPage />} />
                  <Route path="/admin/packages" element={<AllPackagesPage />} />
                  <Route path="/admin/packages/:id" element={<AdminPackageDetailPage />} />
                  <Route path="/admin/modules/:id" element={<AdminModuleDetailPage />} />
                  <Route path="/admin/trash" element={<TrashPage />} />
                  <Route path="/admin/catalogs" element={<CatalogsPage />} />
                  <Route path="/admin/audit-logs" element={<AuditLogPage />} />
                  <Route path="/admin/notifications" element={<NotificationsPage />} />
                </Route>
                <Route element={<RoleRoute role="technician" />}>
                  <Route path="/technician" element={<TechnicianDashboardPage />} />
                  <Route path="/technician/packages" element={<ActivePackagesPage />} />
                  <Route path="/technician/packages/:id" element={<TechnicianPackageDetailPage />} />
                  <Route path="/technician/archive" element={<ArchivePage />} />
                  <Route path="/technician/internal" element={<InternalPackagesPage />} />
                  <Route path="/technician/internal/new" element={<CreateInternalPackagePage />} />
                  <Route path="/technician/internal/:id" element={<TechnicianPackageDetailPage />} />
                  <Route path="/technician/modules/:id" element={<TechnicianModuleDetailPage />} />
                  <Route path="/technician/modules/:id/repair" element={<RepairPage />} />
                  <Route path="/technician/notifications" element={<NotificationsPage />} />
                </Route>
                <Route element={<RoleRoute role="client" />}>
                  <Route path="/client" element={<ClientDashboardPage />} />
                  <Route path="/client/packages" element={<PackagesPage />} />
                  <Route path="/client/packages/new" element={<CreatePackagePage />} />
                  <Route path="/client/packages/:id" element={<PackageDetailPage />} />
                  <Route path="/client/packages/:id/modules/:moduleId" element={<ClientModuleDetailPage />} />
                  <Route path="/client/notifications" element={<NotificationsPage />} />
                </Route>
              </Route>
            </Route>

            {/* Public module-history lookup — no auth */}
            <Route path="/" element={<PublicLookupPage />} />
            <Route path="/unauthorized" element={<UnauthorizedPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
          <Toaster position="top-right" />
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  )
}
