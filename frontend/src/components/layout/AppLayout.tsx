import { useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Bell, LogOut, Menu, UserRound, X } from 'lucide-react'
import toast from 'react-hot-toast'
import { useAuth } from '../../hooks/useAuth'
import { roleHome } from '../../utils/roleHome'
import { notificationsApi } from '../../api/notifications'
import type { Role } from '../../types/auth'

function navLinksFor(role: Role): { to: string; label: string }[] {
  const links = [{ to: roleHome(role), label: 'Dashboard' }]
  if (role === 'admin') {
    links.push(
      { to: '/admin/users', label: 'Users' },
      { to: '/admin/packages', label: 'Packages' },
      { to: '/admin/statistics', label: 'Statistics' },
      { to: '/admin/catalogs', label: 'Catalogs' },
      { to: '/admin/trash', label: 'Trash' },
      { to: '/admin/audit-logs', label: 'Audit' },
    )
  } else if (role === 'client') {
    links.push(
      { to: '/client/packages', label: 'Packages' },
      { to: '/client/packages/new', label: 'Create package' },
    )
  } else if (role === 'technician') {
    links.push(
      { to: '/technician/packages', label: 'Active packages' },
      { to: '/technician/internal', label: 'Internal packages' },
      { to: '/technician/archive', label: 'Archive' },
    )
  }
  return links
}

/** Shell for authenticated pages: header with responsive nav + content area. */
export function AppLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [menuOpen, setMenuOpen] = useState(false)

  // Unread count is refreshed on navigation (no real-time push), per the spec.
  const { data: unread = 0 } = useQuery({
    queryKey: ['unread-count', user?.role, location.pathname],
    queryFn: () => notificationsApi.unreadCount(user!.role),
    enabled: !!user,
  })

  const handleLogout = async () => {
    await logout()
    toast.success('Logged out')
    navigate('/login', { replace: true })
  }

  const links = user ? navLinksFor(user.role) : []

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <div className="flex items-center gap-3 sm:gap-6">
            <button
              type="button"
              className="text-slate-600 hover:text-slate-900 sm:hidden"
              onClick={() => setMenuOpen((open) => !open)}
              aria-label="Toggle menu"
              aria-expanded={menuOpen}
            >
              {menuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
            </button>
            <Link to={user ? roleHome(user.role) : '/'} className="text-lg font-semibold">
              Module Service
            </Link>
            <nav className="hidden gap-4 text-sm text-slate-600 sm:flex">
              {links.map((link) => (
                <Link key={link.to} to={link.to} className="hover:text-slate-900">
                  {link.label}
                </Link>
              ))}
            </nav>
          </div>
          <div className="flex items-center gap-4 text-sm">
            <span className="hidden text-slate-500 lg:inline">
              {user?.name} · {user?.role}
            </span>
            {user && (
              <Link
                to={`/${user.role}/notifications`}
                className="relative inline-flex items-center text-slate-600 hover:text-slate-900"
                title="Notifications"
                aria-label="Notifications"
              >
                <Bell className="h-5 w-5" />
                {unread > 0 && (
                  <span className="absolute -right-2 -top-2 inline-flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-medium text-white">
                    {unread > 99 ? '99+' : unread}
                  </span>
                )}
              </Link>
            )}
            <Link to="/profile" className="inline-flex items-center gap-1 text-slate-600 hover:text-slate-900">
              <UserRound className="h-4 w-4" /> <span className="hidden sm:inline">Profile</span>
            </Link>
            <button
              type="button"
              onClick={handleLogout}
              className="inline-flex items-center gap-1 text-slate-600 hover:text-slate-900"
            >
              <LogOut className="h-4 w-4" /> <span className="hidden sm:inline">Logout</span>
            </button>
          </div>
        </div>

        {/* Mobile nav panel */}
        {menuOpen && (
          <nav className="border-t border-slate-100 px-4 py-2 sm:hidden">
            {links.map((link) => (
              <Link
                key={link.to}
                to={link.to}
                onClick={() => setMenuOpen(false)}
                className="block rounded-md px-2 py-2 text-sm text-slate-700 hover:bg-slate-50"
              >
                {link.label}
              </Link>
            ))}
          </nav>
        )}
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
