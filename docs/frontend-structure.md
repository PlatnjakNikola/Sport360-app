# Frontend Structure — Final Specification

## Overview
React + Vite + TypeScript frontend. Organized by user role with shared components.

Based on:
- the approved client pages (client-pages.md)
- the approved technician pages (technician-pages.md)
- the approved admin pages (admin-pages.md)
- the approved backend API (backend-api.md)

---

# 1. Tech Stack

- **React 18+** with TypeScript
- **Vite** — build tool
- **React Router v6** — routing
- **Axios** — HTTP client (interceptors for auth)
- **React Query (TanStack Query)** — server state, caching, refetching
- **React Hook Form** — form handling + validation
- **Zod** — schema validation (pairs with React Hook Form)
- **Tailwind CSS** — styling
- **Lucide React** — icons
- **React Hot Toast** — toast notifications
- **date-fns** — date formatting
- **html5-qrcode** — camera QR scanning (public lookup page + technician module scan)

---

# 2. Project Structure

```
src/
  api/                    # API client and endpoint functions
    client.ts             # Axios instance with interceptors
    auth.ts               # login, MFA verify/resend, logout, refresh, invites, password reset
    public.ts             # public module history lookup (no auth)
    packages.ts           # package CRUD per role
    modules.ts            # module operations per role
    notifications.ts      # notification operations
    statistics.ts         # admin statistics
    users.ts              # admin user management (technicians + clients + invites)
    catalogs.ts           # admin problem types + service centers
    audit.ts              # admin audit logs

  components/             # Shared components (all roles)
    layout/
      AppLayout.tsx       # Shell: sidebar + header + content area
      Sidebar.tsx         # Navigation sidebar (receives links as props)
      Header.tsx          # Top bar: page title, notification bell, user menu
      Footer.tsx          # Footer
    ui/
      StatusBadge.tsx     # Status badge with color by status code
      Pagination.tsx      # Reusable pagination
      ConfirmModal.tsx    # Confirmation modal (used for status changes, deletes)
      DataTable.tsx       # Reusable table with sorting
      SearchInput.tsx     # Search field with debounce
      FilterDropdown.tsx  # Dropdown filter
      LoadingSpinner.tsx  # Loading state
      EmptyState.tsx      # Empty list state
      ImageGallery.tsx    # Image viewer (module images)
      Timeline.tsx        # Status history timeline
      StatCard.tsx        # Dashboard stat card (count + label)

  hooks/                  # Custom hooks
    useAuth.ts            # Auth state, login, logout, current user
    useNotifications.ts   # Notification count, mark read
    useDebounce.ts        # Input debounce

  context/                # React context providers
    AuthContext.tsx        # Auth state + token refresh logic

  pages/                  # Pages organized by role
    public/
      LookupPage.tsx      # Public module history lookup (input + camera QR scan)

    error/
      NotFoundPage.tsx      # 404 — unknown route
      UnauthorizedPage.tsx  # 403 — authenticated but wrong role (RoleRoute target)

    auth/
      LoginPage.tsx       # Login for all roles; includes the MFA code step for admins
      ForgotPasswordPage.tsx
      ResetPasswordPage.tsx
      AcceptInvitePage.tsx  # Technician or client sets password via invite link

    profile/
      ProfilePage.tsx       # Own data + change password (all authenticated roles)

    client/
      DashboardPage.tsx
      PackagesPage.tsx
      CreatePackagePage.tsx
      PackageDetailPage.tsx
      ModuleDetailPage.tsx
      NotificationsPage.tsx

    technician/
      DashboardPage.tsx
      ActivePackagesPage.tsx
      PackageDetailPage.tsx
      ModuleDetailPage.tsx
      RepairPage.tsx
      ArchivePage.tsx
      InternalPackagesPage.tsx
      CreateInternalPackagePage.tsx
      NotificationsPage.tsx

    admin/
      DashboardPage.tsx
      PackagesPage.tsx
      PackageDetailPage.tsx
      ModuleDetailPage.tsx
      UsersPage.tsx
      TechnicianDetailPage.tsx
      ClientDetailPage.tsx
      StatisticsPage.tsx
      AuditLogPage.tsx
      TrashPage.tsx
      CatalogsPage.tsx    # Problem types + service centers (tabs)
      NotificationsPage.tsx

  routes/                 # Route definitions
    index.tsx             # Main router setup
    PublicRoute.tsx        # Redirect to dashboard if already logged in
    ProtectedRoute.tsx    # Redirect to login if not authenticated
    RoleRoute.tsx         # Redirect to unauthorized if wrong role

  types/                  # TypeScript types/interfaces
    auth.ts               # User, LoginRequest, MfaVerifyRequest, InviteData, etc.
    package.ts            # Package, PackageSummary, CreatePackageRequest
    module.ts             # Module, ModuleDetail, RepairRequest, PublicModuleHistory
    notification.ts       # Notification
    common.ts             # PaginatedResponse, ApiError, etc.

  utils/                  # Utility functions
    formatDate.ts         # Date formatting helpers
    statusHelpers.ts      # Status code to display name, color mapping
    validation.ts         # Shared Zod schemas

  App.tsx                 # Root component (providers + router)
  main.tsx                # Entry point
```

---

# 3. Routing

## Public routes (no auth required)

| Path | Page | Description |
|---|---|---|
| / | LookupPage | Public module history lookup (serial input + camera QR scan) |
| /login | LoginPage | Login for all roles (admin gets an MFA code step) |
| /forgot-password | ForgotPasswordPage | Request password reset |
| /reset-password/:token | ResetPasswordPage | Set new password |
| /accept-invite/:token | AcceptInvitePage | Technician or client sets password |

## Shared authenticated routes (all roles)

| Path | Page | Description |
|---|---|---|
| /profile | ProfilePage | Own data + change password |

## Client routes (role: client)

| Path | Page | Description |
|---|---|---|
| /client | DashboardPage | Dashboard |
| /client/packages | PackagesPage | All packages list |
| /client/packages/new | CreatePackagePage | Create package form |
| /client/packages/:id | PackageDetailPage | Package detail with tabs |
| /client/packages/:id/modules/:moduleId | ModuleDetailPage | Module detail (when unlocked) |
| /client/notifications | NotificationsPage | Notifications list |

## Technician routes (role: technician)

| Path | Page | Description |
|---|---|---|
| /technician | DashboardPage | Dashboard with personal stats |
| /technician/packages | ActivePackagesPage | Active packages (filtered by service center) |
| /technician/packages/:id | PackageDetailPage | Package detail + module scan + module list |
| /technician/modules/:id | ModuleDetailPage | Module detail (read-only if finished) |
| /technician/modules/:id/repair | RepairPage | Repair form (only if waiting_for_repair) |
| /technician/archive | ArchivePage | Completed packages (read-only) |
| /technician/internal | InternalPackagesPage | Internal packages list |
| /technician/internal/new | CreateInternalPackagePage | Create internal package form |
| /technician/internal/:id | PackageDetailPage | Internal package detail (reuses PackageDetailPage) |
| /technician/notifications | NotificationsPage | Notifications list |

## Admin routes (role: admin)

| Path | Page | Description |
|---|---|---|
| /admin | DashboardPage | System overview dashboard |
| /admin/packages | PackagesPage | All packages (all statuses, filterable) |
| /admin/packages/:id | PackageDetailPage | Package detail + admin actions |
| /admin/modules/:id | ModuleDetailPage | Module detail + admin edit |
| /admin/users | UsersPage | Technicians + Clients tabs (incl. pending invites) |
| /admin/users/technicians/:id | TechnicianDetailPage | Technician profile + stats |
| /admin/users/clients/:id | ClientDetailPage | Client profile + packages |
| /admin/statistics | StatisticsPage | Reports with date filters + CSV export |
| /admin/audit-log | AuditLogPage | Audit log with filters |
| /admin/trash | TrashPage | Soft-deleted packages/modules |
| /admin/catalogs | CatalogsPage | Problem types + service centers |
| /admin/notifications | NotificationsPage | Admin notifications |

---

# 4. Authentication Flow

## Axios interceptor

```
1. Every request: cookies are auto-sent (httpOnly, SameSite=Strict)
2. On 401 response:
   a. Call POST /api/v1/auth/refresh-token
   b. If refresh succeeds: retry original request
   c. If refresh fails (401): clear auth state, redirect to /login
3. No manual token handling — cookies managed by browser
```

## Auth context

```
AuthContext provides:
  - user: User | null (current user profile + role)
  - isLoading: boolean (initial auth check in progress)
  - login(email, password): Promise (may return { mfaRequired, mfaToken })
  - verifyMfa(mfaToken, code): Promise
  - logout(): Promise
  - isAuthenticated: boolean
```

## Login flow (MFA for admin)

```
1. POST /api/v1/auth/login with email + password
2. If response contains mfaRequired: LoginPage shows a second step (6-digit code input);
   mfaToken is kept in component state (memory only, never persisted)
3. POST /api/v1/auth/mfa/verify with mfaToken + code → cookies set → redirect to role dashboard
4. "Resend code" link calls /auth/mfa/resend (rate limited)
5. Technicians and clients skip steps 2-4 — cookies are set directly on login
```

## Initial load

```
1. App mounts → call GET /api/v1/auth/me
2. If success: set user in context, redirect to role dashboard
3. If 401: user is not logged in, show public routes
```

---

# 5. Shared Component Behavior

## AppLayout

- Sidebar with navigation links (different per role)
- Header with: page title, notification bell (unread count badge), user dropdown (profile, logout)
- Content area renders current page
- Footer at bottom

## Sidebar navigation

### Client
- Dashboard
- Packages
- Create Package
- Notifications

### Technician
- Dashboard
- Active Packages
- Internal Packages
- Archive
- Notifications

### Admin
- Dashboard
- Packages
- Users
- Statistics
- Audit Log
- Trash
- Catalogs
- Notifications

## StatusBadge

Maps status code to color:
- created → gray
- sent_to_service → blue
- received_by_service → indigo
- on_service → yellow
- repaired_waiting_shipment → green
- shipped_to_client → purple
- arrived → emerald
- waiting_for_repair → yellow
- repaired → green
- not_repairable → red

## Notification bell

- Shows unread count from `GET /notifications` (polled on page navigation, not real-time)
- Click → navigates to notifications page

---

# 6. API Client

## Axios instance (api/client.ts)

```
- baseURL: /api/v1
- withCredentials: true (sends cookies)
- Response interceptor: handle 401 → refresh → retry
- Response interceptor: unwrap { success, data } envelope
  (list endpoints expose the array under data.items with data.pagination)
- Error interceptor: parse { error: { code, message, details } }
```

## React Query defaults

```
- staleTime: 30 seconds (data considered fresh)
- retry: 1 (one retry on failure)
- refetchOnWindowFocus: true
- All mutations invalidate related queries on success
```

---

# 7. Key Patterns

## Form handling
- React Hook Form + Zod schema validation
- Server errors mapped to form fields via `details[].field`
- Submit button disabled while loading

## Pagination
- Query params: `?page=1&limit=20`
- Component shows page numbers + prev/next
- URL-synced (page number in URL search params)

## Search and filters
- Debounced search input (300ms)
- Filters as URL search params (shareable URLs)
- Reset filters button

## Toast notifications
- Success: green toast (auto-dismiss 3s)
- Error: red toast (manual dismiss)
- Used after: create, update, delete, status change

## Module scan (technician)
- QR scan input field (auto-focus)
- On scan/enter: submit form, create module
- Form clears module_number but keeps problem_type selection (batch mode)
- Success toast per scanned module

## Public module lookup
- Route: / (public, no auth, mobile-friendly)
- Module number input + "Scan QR" button that opens the camera (html5-qrcode);
  the QR code on the module label encodes the module number
- On result: service visits timeline (newest first) — dates, status, problem type,
  pixels repaired, chips replaced, repair note, package phase dates
- Never shows: technician names, company names, package numbers, prices, images
- "No service records found" empty state for unknown numbers
- Header has a Login link

## Profile page
- Read-only own data: name, email, role; technician also sees their service center; client also sees their company data
- Change password form: current password, new password, confirm (Zod, min 8 chars)
- On success: toast, user stays logged in (other sessions are revoked server-side)

## Image upload
- Drag & drop or click to upload
- Preview before submit
- Max 5 per module, max 20MB each
- Accepts: JPEG, PNG, WebP

---

# 8. What NOT to do

- Do not store JWT tokens in localStorage or sessionStorage — cookies only
- Do not show module data to clients before status >= repaired_waiting_shipment
- Do not show technician names to clients
- Do not show technician names, company names, package numbers, or prices on the public lookup page
- Do not allow navigation to wrong role pages (RoleRoute guard)
- Do not poll for notifications in real-time — check on page navigation only
- Do not use "error type" — use "problem type"
- Do not use "Draft" — use "created"
