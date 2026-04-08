# Backend API — Final Specification

This document defines the complete backend specification for the service modules application.

Based on:
- the approved database schema (database-schema.md)
- the approved client pages (client-pages.md)
- the approved technician pages (technician-pages.md)
- the approved admin pages (admin-pages.md)

---

# 0. Tech Stack

## Database
PostgreSQL

## Backend
Spring Boot (Java + Kotlin)
- **Spring Security** — authentication, authorization, CORS, filter chain
- **Spring Data JPA (Hibernate)** — ORM, automatic SQL injection protection via PreparedStatement
- **Bean Validation (Jakarta Validation)** — @Valid + custom validators on all request DTOs
- **Flyway** — database migrations
- **Spring Boot Starter Mail** — email sending (password reset, technician invite)
- **BCryptPasswordEncoder** — password hashing (Spring Security built-in)
- **bucket4j** — rate limiting

## Frontend
React + Vite + TypeScript

---

# 1. Core backend goals

The backend must provide:

- authentication (JWT with secure httpOnly cookies)
- admin MFA via 6-digit email code (technicians and clients: password only)
- client invite flow (admin-only — no public self-registration)
- login for all existing users
- password reset flow
- technician invite flow (admin-only)
- role-based authorization (middleware, no permission tables)
- package workflow with strict rules
- module workflow with strict rules
- admin management
- statistics
- audit logging
- archive support (filtered view, not separate entity)
- image upload support
- in-app notifications (badge/counter shown on login, not real-time push)
- email sending (password reset, technician invite)
- internal packages support (Sport360 own modules)
- public module history lookup (no auth, privacy-filtered)

---

# 2. Technical decisions

## 2.1 Authentication method
JWT with secure httpOnly cookies.
- Access token: 15 minutes, stored in httpOnly secure cookie
- Refresh token: 7 days, stored in database (`refresh_tokens` table) and httpOnly secure cookie
- On refresh: old refresh token is revoked, new pair issued (token rotation)
- On logout: refresh token revoked, cookies cleared
- **Admin MFA**: admin login additionally requires a 6-digit code sent by email (10 min validity, max 5 attempts). Cookies are issued only after successful code verification. Technicians and clients log in with password only.

## 2.2 Authorization method
Role-based middleware only.
- 3 fixed roles: admin, technician, client
- No permission tables (over-engineering for 3 roles)
- Middleware checks role from JWT claims
- Ownership checks for client resources (client can only see own packages)
- Admin override logging via audit_logs

## 2.3 API prefix
All endpoints prefixed with `/api/v1/`

## 2.4 Pagination
All list endpoints return paginated results wrapped in the standard success
envelope (§2.6). The list lives under `data.items` (never `data.data`) so the
envelope shape stays consistent across every endpoint:
```
{
  success: true,
  data: {
    items: [...],
    pagination: {
      page: 1,
      limit: 20,
      total: 150,
      totalPages: 8
    }
  }
}
```
Query params: `?page=1&limit=20`

## 2.5 Error response format
All errors use consistent format:
```
{
  success: false,
  error: {
    code: "VALIDATION_ERROR",
    message: "Human readable message",
    details: [{ field: "email", message: "Email already exists" }]
  }
}
```

Standard `error.code` values (each maps to an HTTP status):
`VALIDATION_ERROR` (400), `UNAUTHORIZED` (401), `FORBIDDEN` (403),
`NOT_FOUND` (404), `CONFLICT` (409), `RATE_LIMITED` (429), `INTERNAL_ERROR` (500).
`details` is present only for `VALIDATION_ERROR`.

## 2.6 Success response format
```
{
  success: true,
  data: { ... }
}
```

## 2.7 Rate limiting
- Login: 5 attempts/minute per IP
- MFA verify: 5 attempts/minute per user; MFA resend: max 3 per pending login
- Forgot password: 3/hour per email
- Public module lookup: 20 requests/minute per IP
- General API: 100 requests/minute per user

## 2.8 Transactions
All operations that modify multiple tables must be wrapped in a single database transaction:
- Status change + status history + audit log
- Module repair completion + status change + history + audit
- User creation + role assignment + profile creation

---

# 3. User creation rules

## 3.1 Clients
Clients CANNOT register themselves. There is no public registration endpoint or page.

Flow:
1. Admin creates client invite (company_name, contact name, email, phone, address)
2. System sends email with invite link (valid 48 hours, resendable by admin)
3. Client opens link, sees pre-filled company data, sets a password
4. Account is active immediately — client logs in with email + password (no approval step)
5. Create packages, track packages
6. View repair results only after status >= repaired_waiting_shipment

## 3.2 Technicians
Technicians CANNOT register themselves.

Flow:
1. Admin creates invite (email, name, service center, phone)
2. System sends email with invite link (valid 48 hours)
3. Technician opens link, sets password
4. Technician logs in
5. Technician works only inside technician pages

There is NO public technician registration endpoint or page.

## 3.3 Admins
Exactly **one** admin account, created by the bootstrap seed (env vars on first start). There is no UI or endpoint for creating admin accounts. No public registration.

---

# 4. API endpoints

## 4.1 Auth — Public

| Method | Path | Description |
|---|---|---|
| POST | /api/v1/auth/login | Login for all roles (admin: returns mfaRequired) |
| POST | /api/v1/auth/mfa/verify | Verify admin MFA code, issue cookies |
| POST | /api/v1/auth/mfa/resend | Resend admin MFA code (rate limited) |
| POST | /api/v1/auth/change-password | Change own password (authenticated, all roles) |
| POST | /api/v1/auth/logout | Logout, revoke refresh token |
| POST | /api/v1/auth/refresh-token | Refresh access token |
| GET | /api/v1/auth/me | Current user profile + roles |
| POST | /api/v1/auth/forgot-password | Request password reset email |
| POST | /api/v1/auth/reset-password | Reset password with token |
| GET | /api/v1/auth/invite/{token} | Validate invite token (technician or client) |
| POST | /api/v1/auth/accept-invite | Accept invite (technician or client), set password, create account |

### POST /api/v1/auth/mfa/verify
Input: mfaToken, code
Behavior:
1. Validate mfaToken (short-lived signed token from the login step, not expired)
2. Load the latest unused mfa_code for the user; check expiry and attempts < 5
3. Compare code against stored hash; on mismatch increment attempts and return error
4. On match: mark code used, create access + refresh tokens, set httpOnly cookies
5. Return user profile + role

### POST /api/v1/auth/mfa/resend
Input: mfaToken
Behavior:
1. Validate mfaToken
2. Invalidate the previous code, create and email a new 6-digit code
3. Max 3 resends per pending login, rate limited

### POST /api/v1/auth/change-password
Authenticated (all roles).
Input: currentPassword, newPassword
Behavior:
1. Verify currentPassword against the stored hash
2. Validate newPassword (min 8 characters)
3. Hash and save the new password (bcrypt)
4. Revoke all refresh tokens except the current session's
5. Create audit_log
6. Return success

### POST /api/v1/auth/login
Input: email, password
Behavior:
1. Validate credentials
2. Check user is_active
3. If role = admin: do NOT set cookies yet. Create mfa_code (6 digits, hashed, 10 min expiry), email it, return { mfaRequired: true, mfaToken } — mfaToken is a short-lived (10 min) signed token identifying the pending login
4. Otherwise (technician, client): create access + refresh tokens, set httpOnly cookies, return user profile + role

### POST /api/v1/auth/refresh-token
Input: none (refresh token from httpOnly cookie)
Behavior:
1. Read refresh token from cookie
2. Validate token exists in database, not revoked, not expired
3. Revoke old refresh token
4. Create new access token (15 min) + new refresh token (7 days)
5. Set new httpOnly secure cookies
6. Return success

### POST /api/v1/auth/forgot-password
Input: email
Behavior:
1. Find user by email
2. If exists: create password_reset_token (expire 1 hour), send email
3. Always return success (prevent email enumeration)

### POST /api/v1/auth/reset-password
Input: token, new_password
Behavior:
1. Hash token, find matching unused, non-expired record
2. Update user password_hash
3. Mark token as used
4. Revoke all user refresh tokens
5. Return success

### POST /api/v1/auth/accept-invite
Input: token, password
Behavior:
1. Hash token; look it up in technician_invite_tokens, then client_invite_tokens (unused, non-expired)
2. Technician invite: create user (technician role) + technicians row (service_center_id, phone from invite)
3. Client invite: create user (client role) + clients row (company_name, contact_phone, address from invite)
4. Hash password (bcrypt), mark invite as used, create audit_log
5. Return success (user must then login normally)

### POST /api/v1/auth/logout
Behavior:
1. Read refresh token from cookie
2. Revoke refresh token in database (set revoked_at)
3. Clear httpOnly cookies (access + refresh)
4. Return success

### GET /api/v1/auth/me
Behavior:
1. Extract user ID from JWT access token
2. Load user with role
3. Return user profile (id, name, email, role)

### GET /api/v1/auth/invite/{token}
Behavior:
1. Hash token and look it up in technician_invite_tokens, then client_invite_tokens
2. Validate token is not used and not expired
3. Return { type: technician | client } + invite data (name, email, and service_center or company data) for frontend to pre-fill the form
4. If invalid or expired: return error

---

## 4.2 Client endpoints (role: client)

| Method | Path | Description |
|---|---|---|
| GET | /api/v1/client/dashboard | Dashboard (counts by status, recent activity) |
| GET | /api/v1/client/packages | List own packages (paginated) |
| POST | /api/v1/client/packages | Create package |
| GET | /api/v1/client/packages/{id} | Package detail (own only) |
| PATCH | /api/v1/client/packages/{id} | Edit tracking link / note / description |
| POST | /api/v1/client/packages/{id}/mark-sent | Mark package as sent to service |
| GET | /api/v1/client/packages/{id}/modules | List modules for package (when status >= repaired_waiting_shipment) |
| GET | /api/v1/client/modules/{id} | Module detail (when package status >= repaired_waiting_shipment) |
| POST | /api/v1/client/packages/{id}/confirm-arrival | Confirm arrival |
| GET | /api/v1/client/notifications | Client notifications (paginated) |
| PATCH | /api/v1/client/notifications/{id}/read | Mark notification as read |
| POST | /api/v1/client/notifications/mark-all-read | Mark all notifications as read |

### POST /api/v1/client/packages
Input: package_number, outbound_tracking_link, description, note, approx_quantity
Behavior:
1. Validate input
2. Ensure package_number is unique
3. Create package (status = created, client_id from auth, service_center_id = default center)
4. Create package_status_history row
5. Create audit_log
6. Return package

Note: service_center_id is set to the default service center (e.g. "CRO") at creation. Currently one center exists; when multiple centers are added, this can be changed to admin assignment or client selection.

### PATCH /api/v1/client/packages/{id}
Allowed fields: outbound_tracking_link, note, description
Behavior:
1. Verify ownership
2. Update allowed fields only
3. Create audit_log
4. Return updated package

### GET /api/v1/client/dashboard
Returns:
- Package counts by status (own packages only)
- Recent activity feed (latest status changes on own packages)

### POST /api/v1/client/packages/{id}/mark-sent
Behavior:
1. Verify ownership
2. Verify current status = created
3. In transaction: update status to sent_to_service, create history, create audit_log
4. Create notification for admin: "Package {number} sent to service"
5. Create notification for technicians in package's service center: new work incoming
6. Return updated package

### POST /api/v1/client/packages/{id}/confirm-arrival
Behavior:
1. Verify ownership
2. Verify current status = shipped_to_client
3. In transaction: update status to arrived, set arrived_at, create history, create audit_log, create notification for admin
4. Return updated package

### GET /api/v1/client/packages/{id}/modules
Behavior:
1. Verify ownership
2. Verify package status >= repaired_waiting_shipment
3. Return paginated module list (from v_module_detail view, excluding technician_name)

### GET /api/v1/client/modules/{id}
Behavior:
1. Verify module belongs to client's package
2. Verify package status >= repaired_waiting_shipment
3. Return module detail (excluding technician_name)

### Client data visibility rules
- Client sees only own packages
- Module data hidden until status >= repaired_waiting_shipment
- Client does NOT see technician names in module detail (privacy)
- Client sees: module number, status, problem type, pixels repaired, chips replaced, repair note, price

---

## 4.3 Technician endpoints (role: technician)

| Method | Path | Description |
|---|---|---|
| GET | /api/v1/technician/dashboard | Dashboard (counts, personal stats) |
| GET | /api/v1/technician/packages/active | Active packages (paginated, filtered by service center) |
| GET | /api/v1/technician/packages/archive | Archived packages (paginated, read-only) |
| GET | /api/v1/technician/packages/{id} | Package service detail |
| GET | /api/v1/technician/packages/{id}/modules | Modules for package (paginated) |
| POST | /api/v1/technician/packages/{id}/next-status | Move package forward one step |
| POST | /api/v1/technician/packages/{id}/modules | Create module (scan) |
| GET | /api/v1/technician/modules/{id} | Module detail |
| POST | /api/v1/technician/modules/{id}/repair | Complete repair |
| POST | /api/v1/technician/modules/{id}/images | Upload module images |
| GET | /api/v1/technician/notifications | Technician notifications (paginated) |
| PATCH | /api/v1/technician/notifications/{id}/read | Mark notification as read |
| POST | /api/v1/technician/notifications/mark-all-read | Mark all notifications as read |
| GET | /api/v1/technician/internal-packages | Internal packages list (paginated, filtered by service center) |
| POST | /api/v1/technician/internal-packages | Create internal package |
| POST | /api/v1/technician/internal-packages/{id}/confirm-arrival | Confirm arrival for internal packages |

### GET /api/v1/technician/dashboard
Returns:
- Package counts by status (for technician's service center)
- Personal stats: my modules today, my modules this week, my repair rate (repaired / total), my total value this week
- Recent activity: my latest repairs, recent status changes

### GET /api/v1/technician/packages/active
Query params: status, search, sort_by, sort_order, page, limit
Behavior:
1. Filter packages by technician's service_center_id
2. Include statuses: sent_to_service, received_by_service, on_service, repaired_waiting_shipment
3. Apply optional status filter, search by package_number
4. Return paginated list (from v_package_summary view)

### GET /api/v1/technician/packages/archive
Query params: search, sort_by, sort_order, page, limit
Behavior:
1. Filter packages by technician's service_center_id
2. Include statuses: shipped_to_client, arrived
3. Apply optional search by package_number
4. Return paginated list (read-only, from v_package_summary view)

### Service center filtering
All technician package lists (active, archive, internal) are automatically filtered by the technician's assigned service_center_id. A technician in Osijek does not see packages from Zagreb. Client packages are assigned the default service center (e.g. "CRO") at creation, so they are always visible to technicians in that center.

### POST /api/v1/technician/internal-packages
Input: package_number (free text, e.g. "5mm novi", "rijeka moduli"), description, note, approx_quantity
Behavior:
1. Validate input
2. Create package with is_internal = true, client_id = null, status = created
3. Service center from technician profile
4. Create package_status_history row
5. Create audit_log
6. Return package

Note: package_number for internal packages is a descriptive label, not a shipment code.

### POST /api/v1/technician/internal-packages/{id}/confirm-arrival
Behavior:
1. Verify package is_internal = true
2. Verify current status = shipped_to_client
3. In transaction: update status to arrived, set arrived_at, create history, create audit_log
4. Return updated package

Note: For external packages, only client can confirm arrival. For internal packages, technician can.

### POST /api/v1/technician/packages/{id}/next-status
Behavior:
1. Load package with current status
2. Calculate next status (current sort_order + 1)
3. Validate transition is allowed:
   - Forward only, one step
   - repaired_waiting_shipment requires: at least 1 module AND all modules in final state
   - shipped_to_client requires: current = repaired_waiting_shipment
4. In transaction: update status + denormalized date, create history, create audit_log
5. Create notification for client (if relevant status change and package is not internal)
6. Return updated package

### POST /api/v1/technician/packages/{id}/modules
Input: module_number, problem_type_id, images (optional, max 5)
Behavior:
1. Validate package is in status on_service
2. Validate module_number unique within package
3. Validate problem_type exists and is_active
4. Create module (status = waiting_for_repair)
5. Upload images if provided
6. Create module_status_history
7. Create audit_log
8. Return module

### POST /api/v1/technician/modules/{id}/repair
Input: decision (repaired | not_repairable), pixels_repaired, chips_replaced, repair_note, price (EUR)
Behavior:
1. Validate module is in waiting_for_repair — if not, return 409 CONFLICT ("Module already completed by another technician")
2. If repaired: all fields allowed
3. If not_repairable: pixels, chips, price ignored/zeroed, note allowed
4. In transaction: create module_repairs row, update module status, create history, create audit_log
5. Return module with repair data

### POST /api/v1/technician/modules/{id}/images
Input: files (multipart, max 5 per module total, max 20MB each)
Behavior:
1. Validate current image count + new <= 5
2. Validate file size <= 20MB
3. Validate MIME type (image/jpeg, image/png, image/webp)
4. Store files via the storage abstraction (local filesystem path from IMAGE_STORAGE_PATH env var; in cloud deployments this path MUST be a mounted persistent volume — e.g. Render Disk / Railway Volume — or swapped for S3-compatible storage)
5. Create module_images rows
6. Return image metadata

### Technician restrictions
- Cannot register
- Cannot create users
- Can only create internal packages (not client packages)
- Cannot move status backwards or skip steps
- Cannot edit archive records
- Cannot access client or admin routes

---

## 4.4 Admin endpoints (role: admin)

| Method | Path | Description |
|---|---|---|
| GET | /api/v1/admin/dashboard | Dashboard statistics |
| GET | /api/v1/admin/packages | All packages (paginated, filterable) |
| GET | /api/v1/admin/packages/{id} | Package detail (full) |
| PATCH | /api/v1/admin/packages/{id} | Edit package |
| DELETE | /api/v1/admin/packages/{id} | Soft delete package |
| POST | /api/v1/admin/packages/{id}/restore | Restore soft-deleted package |
| PATCH | /api/v1/admin/packages/{id}/status | Override package status (logged) |
| GET | /api/v1/admin/modules/{id} | Module detail (full) |
| PATCH | /api/v1/admin/modules/{id} | Edit module |
| DELETE | /api/v1/admin/modules/{id} | Soft delete module |
| POST | /api/v1/admin/modules/{id}/restore | Restore soft-deleted module |
| GET | /api/v1/admin/technicians | List technicians (paginated) |
| GET | /api/v1/admin/technicians/{id} | Technician detail |
| PATCH | /api/v1/admin/technicians/{id} | Update technician |
| POST | /api/v1/admin/technicians/invite | Create technician invite |
| GET | /api/v1/admin/clients | List clients (paginated) |
| GET | /api/v1/admin/clients/{id} | Client detail |
| POST | /api/v1/admin/clients/invite | Create client invite |
| GET | /api/v1/admin/clients/invites | Pending client invites |
| POST | /api/v1/admin/clients/invites/{id}/resend | Resend client invite |
| GET | /api/v1/admin/technicians/invites | Pending technician invites |
| POST | /api/v1/admin/technicians/invites/{id}/resend | Resend technician invite |
| GET | /api/v1/admin/problem-types | List problem types (incl. inactive) |
| POST | /api/v1/admin/problem-types | Create problem type |
| PATCH | /api/v1/admin/problem-types/{id} | Update name / sort_order / is_active |
| GET | /api/v1/admin/service-centers | List service centers |
| POST | /api/v1/admin/service-centers | Create service center |
| PATCH | /api/v1/admin/service-centers/{id} | Update / deactivate service center |
| GET | /api/v1/admin/statistics | Global statistics (filterable by date range) |
| GET | /api/v1/admin/packages/{id}/statistics | Per-package statistics breakdown |
| GET | /api/v1/admin/audit-logs | Audit logs (paginated, filterable) |
| GET | /api/v1/admin/notifications | Admin notifications (paginated) |
| PATCH | /api/v1/admin/notifications/{id}/read | Mark notification as read |
| POST | /api/v1/admin/notifications/mark-all-read | Mark all notifications as read |
| POST | /api/v1/admin/packages/bulk-delete | Bulk soft delete packages |
| POST | /api/v1/admin/packages/bulk-status | Bulk status override |
| POST | /api/v1/admin/packages/{id}/modules | Add correction module (admin) |
| GET | /api/v1/admin/trash | List soft-deleted items |

### POST /api/v1/admin/packages/{id}/restore
Behavior:
1. Load the soft-deleted package
2. **Uniqueness guard**: the partial unique index (`uq_packages_number_active`)
   only covers non-deleted rows, so while this package was deleted another
   package may have taken its `package_number`. Before clearing `deleted_at`,
   verify no non-deleted package uses the same `package_number` — if one does,
   return **409 CONFLICT** ("Package number already in use — cannot restore")
3. In transaction: clear `deleted_at`, create audit_log (action = update)
4. Return restored package

### POST /api/v1/admin/modules/{id}/restore
Behavior:
1. Load the soft-deleted module
2. **Uniqueness guard**: same reasoning via `uq_modules_pkg_number_active`. Verify
   no non-deleted module in the same package uses the same `module_number` — if
   one does, return **409 CONFLICT** ("Module number already in use in this
   package — cannot restore")
3. In transaction: clear `deleted_at`, create audit_log (action = update)
4. Return restored module

### PATCH /api/v1/admin/packages/{id}/status
Input: status_id
Behavior:
1. Admin can set ANY valid status (override, not forward-only)
2. In transaction: update status + denormalized dates, create history, create audit_log with admin_override action
3. Create notification for client (skip if internal package)
4. Return updated package

### POST /api/v1/admin/packages/bulk-delete
Input: package_ids (array of BIGINT)
Behavior:
1. Validate all package IDs exist
2. Soft delete all packages (set deleted_at)
3. Create audit_log entry for each package
4. Return count of deleted packages

### POST /api/v1/admin/packages/bulk-status
Input: package_ids (array of BIGINT), status_id
Behavior:
1. Validate all package IDs exist and status_id is valid
2. For each package: update status + denormalized dates, create history, create audit_log with admin_override action
3. Create notifications for affected clients (skip internal packages)
4. Return count of updated packages

### POST /api/v1/admin/packages/{id}/modules
Input: module_number, problem_type_id, assigned_technician_id, images (optional, max 5)
Behavior:
1. Validate package exists
2. Validate module_number is unique within package
3. Validate assigned_technician_id exists and is active
4. Create module with status = waiting_for_repair
5. Create audit_log entry with action = create, noting admin correction
6. Return created module

### POST /api/v1/admin/technicians/invite
Input: email, name, service_center_id, phone
Behavior:
1. Validate email not already in use
2. Validate service center exists and is active
3. Create technician_invite_token (expire 48 hours)
4. Send invite email with link
5. Create audit_log
6. Return invite status

### PATCH /api/v1/admin/technicians/{id}
Input: name, phone, service_center_id, is_active (all optional)
Behavior:
1. Validate service_center_id exists and is active (if provided)
2. Update provided fields
3. If is_active changed: create audit_log with action = update
4. Return updated technician

### GET /api/v1/admin/statistics
Query params: date_from, date_to, filter (internal/external/all), export (csv)
Returns:

**Package stats:**
- Package count by status
- Package count by client
- Package count by period (per week/month)

**Module stats:**
- Total modules
- Repaired vs not_repairable
- Modules by problem type

**CSV export** available for all sections

### GET /api/v1/admin/packages/{id}/statistics
Returns per-package breakdown:

**Module details:**
- Total modules in package
- Repaired count, not_repairable count
- Total pixels repaired, total chips replaced
- Modules by problem type

**Technician breakdown:**
- Per technician: modules count, repaired count, not_repairable count
- Per technician: total repair value
- Per technician: pixels repaired, chips replaced

### POST /api/v1/admin/clients/invite
Input: email, contact_name, company_name, contact_phone, address
Behavior:
1. Validate email is not already in use (users + pending invites of both types)
2. Create client_invite_token (expire 48 hours)
3. Send invite email with link
4. Create audit_log
5. Return invite status

### Catalogs (problem types & service centers)
- Problem types: code is immutable after creation; deactivate instead of delete so historical records stay intact. Deactivated types are hidden from new module forms.
- Service centers: deactivation is blocked while active technicians are still assigned to the center.
- All catalog changes create audit_log entries.

### GET /api/v1/admin/audit-logs
Query params: entity_type, entity_id, user_id, action_type, date_from, date_to, page, limit
Returns: paginated audit log entries with user info

### Admin restrictions
- Admin does not use public registration flow
- Admin is control layer, not workflow actor

---

## 4.5 System endpoints

| Method | Path | Description |
|---|---|---|
| GET | /api/v1/health | Health check |
| GET | /api/v1/images/{id} | Serve image (authenticated, ownership + status checks) |

---

## 4.6 Public endpoints (no auth)

| Method | Path | Description |
|---|---|---|
| GET | /api/v1/public/modules/{moduleNumber}/history | Public module service history |

### GET /api/v1/public/modules/{moduleNumber}/history
No authentication. Rate limited (20/minute per IP).
Behavior:
1. Find all non-deleted modules with module_number = {moduleNumber} whose package is not deleted
2. Order by created_at DESC (newest service visit first)
3. For each visit return: module status, problem type name, pixels_repaired, chips_replaced, repair_note, repair completed_at, and the package phase dates (received_at, service_completed_at, shipped_at, arrived_at)
4. NEVER return: technician names, client/company names, package numbers, prices, images
5. No matches: return 404 with a friendly "no service records found" message

Used by the public lookup page (manual input + camera QR scan). The QR code on the module label encodes the module number.

---

# 5. Backend module structure

```
src/
  auth/
  users/
  roles/
  clients/
  technicians/
  service-centers/
  packages/
  modules/
  module-repairs/
  uploads/
  statistics/
  audit/
  notifications/
  email/
  admin/
```

Each module should contain:
- controller / route layer
- service layer
- repository / ORM access layer (Spring Data JPA)
- validation schemas (@Valid + Jakarta Bean Validation)
- authorization middleware (@PreAuthorize or custom filter)

## Database access strategy
- **Writes:** Spring Data JPA repositories (save, update, delete)
- **Simple reads:** JPA repositories with derived query methods
- **Complex reads:** PostgreSQL Views mapped as @Immutable JPA entities:
  - `v_package_summary` — package lists, dashboards, statistics
  - `v_package_technician_breakdown` — per-package technician stats
  - `v_module_detail` — module lists, detail pages, client repair history
- **Custom queries:** @Query annotation (JPQL or native SQL)
- **Transactions:** @Transactional on service methods for atomic operations

---

# 6. Service-layer logic

Important logic must live in services, not controllers:

- loginUser()
- verifyMfaCode()
- refreshToken()
- forgotPassword()
- resetPassword()
- createTechnicianInvite()
- createClientInvite()
- acceptInvite()  // technician or client invites
- createPackageForClient()
- updateClientPackage()
- movePackageToNextStatus()
- overridePackageStatus() — admin only, with audit
- createModuleForPackage()
- completeModuleRepair()
- finishPackageIfEligible()
- softDeletePackage()
- restorePackage()
- computeAdminStatistics()
- getPublicModuleHistory()
- createNotification()
- sendEmail()

---

# 7. Notification triggers

Notifications are in-app only (badge/counter visible on login or page navigation, not real-time push).
They are created automatically at these events:

| Event | Recipient | Type |
|---|---|---|
| Client invite accepted (client joined) | Admin | client_joined |
| Package sent to service (client marked as sent) | Admin | package_sent |
| Package sent to service (new work incoming) | Technician (service center) | package_sent |
| Package received by service | Client | status_change |
| Service started | Client | status_change |
| Service completed (all modules done) | Client | status_change |
| Package shipped to client | Client | status_change |
| New package created | Admin | package_created |
| Package arrived (confirmed by client) | Admin | status_change |
| Admin status override | Client | status_change |
| New technician invite accepted | Admin | technician_joined |

---

# 8. Validation rules

## User/auth
- Unique email (also checked against pending invites)
- Password min 8 characters
- No public registration of any kind — accounts exist only via invite (technician, client) or seed (admin)
- Admin MFA code: 6 digits, 10 min expiry, max 5 attempts per code

## Package
- package_number unique
- Client ownership enforced on all client endpoints
- current_status_id must be valid
- No illegal status transitions (technician: forward-only one step)
- repaired_waiting_shipment requires all modules in final state

## Module
- Package must exist and be in on_service status
- module_number unique within package
- Problem type must exist and be active
- Service center must exist
- Max 5 images per module, max 20MB each
- Repair decision must be valid (repaired or not_repairable)

---

# 9. Database migrations and seeding

Migrations managed by **Flyway** (SQL-based migration files in `src/main/resources/db/migration/`).

System must be seeded with:
1. roles: admin, technician, client
2. package_statuses: all 7 with sort_order
3. module_statuses: all 3 with sort_order and is_final
4. problem_types: all active types
5. service_centers: at least one active center
6. initial admin account

This is critical because:
- Technician accounts depend on admin creation via invite
- Status tables must exist before workflow starts
- Problem types must exist before module creation

---

# 10. Security (Spring Boot specific)

## Authentication
- Password hashing: BCryptPasswordEncoder (Spring Security built-in)
- JWT in httpOnly secure cookies (SameSite=Strict)
- Spring Security filter chain for JWT validation

## CSRF
- NOT needed. SameSite=Strict cookies prevent cross-origin cookie sending.
- Disable CSRF in Spring Security config: `http.csrf().disable()`

## SQL Injection
- Spring Data JPA uses PreparedStatement by default — safe
- NEVER use string concatenation in @Query annotations
- Use JPA Criteria API or QueryDSL for dynamic queries

## XSS
- React escapes output by default on frontend
- Backend returns JSON only, no HTML rendering
- Use @JsonProperty for controlled serialization

## Authorization
- Spring Security @PreAuthorize or custom SecurityFilter for role checks
- Ownership checks via service layer (client can only see own packages)
- Audit logs for all admin overrides

## Rate Limiting
- bucket4j library with Spring Boot integration
- Applied via servlet filter on auth endpoints

## Input Validation
- @Valid + Jakarta Bean Validation on all @RequestBody DTOs
- Custom validators for business rules (status transitions, etc.)

## General Rules
- No public registration of any kind (all accounts via invite or seed)
- Admin login requires a 6-digit email MFA code
- Public lookup endpoint never returns technician names, client/company names, package numbers, or prices
- Secrets via environment variables (never hardcoded in application.yml)

---

# 11. CORS configuration

Configured via Spring Security `CorsConfigurationSource` bean.

Allowed origins per environment:
- development: http://localhost:5173 (Vite default)
- production: actual frontend domain

Allowed methods: GET, POST, PATCH, DELETE
Allowed headers: Content-Type, Authorization
Allow credentials: true (for cookies)

---

# 12. Email service

Backend must support sending emails for:
- Password reset (link with token)
- Technician invite (link with token)
- Client invite (link with token)
- Admin MFA code (6-digit code)

Implementation: Spring Boot Starter Mail with JavaMailSender.
Use an interface/service abstraction so the email provider can be swapped (e.g. SMTP, SendGrid, Resend).

---

# 13. Image storage

Images stored on local filesystem. Database holds only file paths/URLs.

## Capacity estimate
- ~10,000 modules/year × ~3 images × ~20MB = **~600 GB/year** uploaded
- Auto-cleanup removes images 30 days after package arrival
- **~100-200 GB active on disk** at any time

## Architecture
- `ImageStorageService` interface with `save()`, `delete()`, `getUrl()` methods
- Implementation: `LocalFileStorageService` (saves to local filesystem)
- Swappable to `S3StorageService` (Cloudflare R2, AWS S3, MinIO) later without changing any other code
- Path format: `/{year}/{month}/{module_id}/{uuid}.{ext}`

## Upload flow
1. Technician uploads image (max 5 per module, max 20MB each)
2. Backend validates file size and MIME type (image/jpeg, image/png, image/webp)
3. `ImageStorageService.save()` writes to disk
4. Backend stores path in `module_images.file_path`

## Serving images
- Backend endpoint: `GET /api/v1/images/{id}` — reads file from disk, streams to client
- Checks: user must be authenticated, ownership verified, status check for client role (>= repaired_waiting_shipment)

## Auto-cleanup (30 days after arrival)
- Spring `@Scheduled` job, cron: once daily (e.g. 3:00 AM)
- Query: find images where `package.arrived_at < NOW() - INTERVAL '30 days'`
- For each: `ImageStorageService.delete()` removes from disk + hard delete from database
- Audit log entry for batch cleanup

---

# 14. Scheduled / background jobs

Spring `@Scheduled` jobs run by the backend. Each writes a single audit_log batch entry.

- **Image cleanup** (Phase 7) — detailed in §13: removes module images 30 days
  after `package.arrived_at` (storage delete + DB hard delete).
- **Expired-token cleanup** (Phase 2) — runs daily (e.g. 3:30 AM). Hard-deletes
  rows that are expired or already consumed, keeping the auth tables small:
  - `refresh_tokens` — `expires_at < NOW()` OR `revoked_at IS NOT NULL`
  - `mfa_codes` — `expires_at < NOW()` OR `used_at IS NOT NULL`
  - `technician_invite_tokens` — `expires_at < NOW()` OR `used_at IS NOT NULL`
  - `client_invite_tokens` — `expires_at < NOW()` OR `used_at IS NOT NULL`
  - `password_reset_tokens` — `expires_at < NOW()` OR `used_at IS NOT NULL` (same pattern)

---

# 15. Final implementation order

1. Database schema + migrations + seed data
2. Auth module (login + admin MFA, logout, refresh, me)
3. Password reset flow
4. Role middleware
5. Client package CRUD
6. Technician package workflow
7. Module CRUD + repair
8. Image upload
9. Admin CRUD + overrides
10. Invite flows (technician + client)
11. Statistics
12. Audit logging
13. Notifications
14. Email service integration
15. Public module lookup (history endpoint + page)
