# Database Schema — Final Optimized (PostgreSQL)

This document describes the final database design for the service modules application.

Optimized for:
- PostgreSQL-specific features (JSONB, partial indexes, TIMESTAMPTZ, CHECK constraints)
- Spring Boot + JPA/Hibernate integration
- practical normalization (no redundant columns)
- role separation and auditability

---

## Core decisions

- **BIGINT** for all primary keys (scale: ~10,000 modules/year, ~200 clients)
- **TIMESTAMPTZ** for all timestamps (timezone-safe, PostgreSQL best practice)
- **JSONB** for audit log values (binary, indexable, faster than JSON)
- **Role stored directly on users** (role_id column, not N:M — every user has exactly one role)
- Status lookup tables use `id`, `code`, and ordering fields
- No `permissions` / `role_permissions` tables (3 fixed roles, middleware is sufficient)
- Key dates denormalized on `packages` for fast reads
- `module_repairs` is 1:1 with modules (no re-repair — new request or handled externally)
- Internal packages (Sport360) use `is_internal = true` and `client_id = NULL`
- Images stored on external drive/storage — database holds only URLs
- Service center on packages, not on modules (avoids redundancy)
- No stored procedures — views for reads, JPA for writes
- **Clients are invite-only** (no public self-registration) — admin creates the invite with company data, client only sets a password
- **Admin MFA**: 6-digit email code on every admin login (technicians and clients: password only)
- **Public module lookup**: service history by module_number across packages, no login, privacy-filtered
- All prices are in **EUR** — single currency, no currency column

---

# 1. Table: users

## Purpose
Stores the base identity for every system user.

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | Unique user identifier |
| role_id | SMALLINT | FK -> roles.id, NOT NULL | User role (admin/technician/client) |
| name | VARCHAR(150) | NOT NULL | Full name |
| email | VARCHAR(255) | UNIQUE, NOT NULL | User email |
| password_hash | VARCHAR(72) | NOT NULL | BCrypt hash (always 60 chars) |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | Whether user is active |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Record creation time |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Last update time |

---

# 2. Table: roles

## Purpose
Fixed system roles. Seeded, never modified at runtime.

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | SMALLINT | PK | Role ID |
| code | VARCHAR(50) | UNIQUE, NOT NULL | Role code |

## Seed values
| id | code |
|---|---|
| 1 | admin |
| 2 | technician |
| 3 | client |

---

# 3. Table: clients

## Purpose
Client-specific profile data. 1:1 with users.

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| user_id | BIGINT | PK, FK -> users.id | Client user |
| company_name | VARCHAR(255) | NOT NULL | Company name |
| contact_phone | VARCHAR(50) | | Contact phone |
| address | TEXT | | Address |

---

# 4. Table: service_centers

## Purpose
Service center data.

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | Service center ID |
| code | VARCHAR(50) | UNIQUE, NOT NULL | Stable code, e.g. CROATIA |
| name | VARCHAR(255) | NOT NULL | Service center name |
| country | VARCHAR(100) | | Country |
| city | VARCHAR(100) | | City |
| address | TEXT | | Address |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | Whether center is active |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Record creation time |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Last update time |

---

# 5. Table: technicians

## Purpose
Technician-specific profile data. 1:1 with users.

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| user_id | BIGINT | PK, FK -> users.id | Technician user |
| phone | VARCHAR(50) | | Technician phone |
| service_center_id | BIGINT | FK -> service_centers.id, NOT NULL | Assigned service center |

---

# 6. Table: package_statuses

## Purpose
Lookup table for package statuses. Seeded, never modified at runtime.

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | SMALLINT | PK | Status ID |
| code | VARCHAR(50) | UNIQUE, NOT NULL | Stable technical code |
| name | VARCHAR(100) | NOT NULL | Display name |
| sort_order | SMALLINT | UNIQUE, NOT NULL | Status order in workflow |

## Seed values

| id | code | name | sort_order |
|---|---|---|---|
| 1 | created | Created | 1 |
| 2 | sent_to_service | Sent to service | 2 |
| 3 | received_by_service | Received by service | 3 |
| 4 | on_service | On service | 4 |
| 5 | repaired_waiting_shipment | Repaired — waiting shipment | 5 |
| 6 | shipped_to_client | Shipped to client | 6 |
| 7 | arrived | Arrived | 7 |

---

# 7. Table: packages

## Purpose
Package / shipment records. Created by clients (external) or technicians (internal).

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | Package ID |
| package_number | VARCHAR(100) | NOT NULL | Package number (external) or descriptive label (internal). Uniqueness enforced among non-deleted rows via a partial unique index — see Indexes |
| client_id | BIGINT | FK -> clients.user_id, NULL | Package owner (NULL for internal) |
| is_internal | BOOLEAN | NOT NULL, DEFAULT false | True for Sport360 internal packages |
| service_center_id | BIGINT | FK -> service_centers.id, NOT NULL | Destination service center (default center for client packages, technician's center for internal) |
| current_status_id | SMALLINT | FK -> package_statuses.id, NOT NULL, DEFAULT 1 | Current status |
| outbound_tracking_link | VARCHAR(2048) | | Tracking to service |
| return_tracking_link | VARCHAR(2048) | | Tracking back to client |
| description | TEXT | | Package content description |
| note | TEXT | | Optional client note |
| approx_quantity | INT | CHECK (approx_quantity >= 0) | Approximate quantity, informational |
| created_by_user_id | BIGINT | FK -> users.id, NOT NULL | Record creator |
| received_at | TIMESTAMPTZ | NULL | When service received |
| service_started_at | TIMESTAMPTZ | NULL | When service started |
| service_completed_at | TIMESTAMPTZ | NULL | When all modules finished |
| shipped_at | TIMESTAMPTZ | NULL | When shipped back |
| arrived_at | TIMESTAMPTZ | NULL | When client confirmed arrival |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Record creation time |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Last update time |
| deleted_at | TIMESTAMPTZ | NULL | Soft delete timestamp |

## Constraints
- CHECK `chk_packages_internal_client`: an internal package has no client, an
  external package must have one —
  `(is_internal AND client_id IS NULL) OR (NOT is_internal AND client_id IS NOT NULL)`
- `package_number` is unique among non-deleted packages (partial unique index, see Indexes)

---

# 8. Table: package_status_history

## Purpose
Every package status change.

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | History row ID |
| package_id | BIGINT | FK -> packages.id, NOT NULL | Package |
| status_id | SMALLINT | FK -> package_statuses.id, NOT NULL | Applied status |
| changed_by_user_id | BIGINT | FK -> users.id, NOT NULL | User who changed it |
| changed_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Change time |

---

# 9. Table: problem_types

## Purpose
Lookup table for module problem types. Seeded, admin can add/deactivate.

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | SMALLINT | PK | Problem type ID |
| code | VARCHAR(50) | UNIQUE, NOT NULL | Stable technical code |
| name | VARCHAR(255) | NOT NULL | Display name |
| sort_order | SMALLINT | UNIQUE, NOT NULL | UI ordering |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | Whether selectable for new records |

## Seed values

| id | code | name | sort_order | is_active |
|---|---|---|---|---|
| 1 | black_screen | Black screen | 1 | true |
| 2 | blue_screen | Blue screen | 2 | true |
| 3 | garbled_screen | Garbled screen | 3 | true |
| 4 | flickering | Flickering | 4 | true |
| 5 | color_loss | Partial area color loss or color difference | 5 | true |
| 6 | bright_line_dead_pixels | Bright line or dead pixels | 6 | true |
| 7 | image_stutter | Image stuttering or delay | 7 | true |
| 8 | display_misalignment | Display misalignment | 8 | true |
| 9 | resolution_offset | Resolution, offset or rotation issues | 9 | true |
| 10 | signal_issues | Signal issues | 10 | true |
| 11 | brightness_sensor | Brightness sensor issues | 11 | true |
| 12 | power_issues | Power issues | 12 | true |
| 13 | other | Other | 13 | true |

---

# 10. Table: module_statuses

## Purpose
Lookup table for module statuses. Seeded, never modified at runtime.

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | SMALLINT | PK | Status ID |
| code | VARCHAR(50) | UNIQUE, NOT NULL | Stable technical code |
| name | VARCHAR(100) | NOT NULL | Display name |
| sort_order | SMALLINT | UNIQUE, NOT NULL | Workflow order |
| is_final | BOOLEAN | NOT NULL | Whether this is a final status |

## Seed values

| id | code | name | sort_order | is_final |
|---|---|---|---|---|
| 1 | waiting_for_repair | Waiting for repair | 1 | false |
| 2 | repaired | Repaired | 2 | true |
| 3 | not_repairable | Not repairable | 3 | true |

---

# 11. Table: modules

## Purpose
Module record created in service. Service center derived from package or technician — not stored on module.

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | Module ID |
| package_id | BIGINT | FK -> packages.id, NOT NULL | Parent package |
| module_number | VARCHAR(150) | NOT NULL | Module number from QR/manual |
| problem_type_id | SMALLINT | FK -> problem_types.id, NOT NULL | Selected problem type |
| current_status_id | SMALLINT | FK -> module_statuses.id, NOT NULL, DEFAULT 1 | Current status |
| assigned_technician_id | BIGINT | FK -> technicians.user_id, NOT NULL | Assigned technician |
| created_by_user_id | BIGINT | FK -> users.id, NOT NULL | User who created module |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Record creation time |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Last update time |
| deleted_at | TIMESTAMPTZ | NULL | Soft delete timestamp |

## Constraints
- UNIQUE(package_id, module_number) WHERE deleted_at IS NULL — unique within a
  package among non-deleted modules (partial unique index, so a soft-deleted
  module_number can be scanned again)

---

# 12. Table: module_status_history

## Purpose
Every module status change.

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | History row ID |
| module_id | BIGINT | FK -> modules.id, NOT NULL | Module |
| status_id | SMALLINT | FK -> module_statuses.id, NOT NULL | Applied status |
| changed_by_user_id | BIGINT | FK -> users.id, NOT NULL | User who changed it |
| changed_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Change time |

---

# 13. Table: module_repairs

## Purpose
Final repair outcome. 1:1 with modules. No re-repair.

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| module_id | BIGINT | PK, FK -> modules.id | Module |
| technician_id | BIGINT | FK -> technicians.user_id, NOT NULL | Technician who completed repair |
| decision_status_id | SMALLINT | FK -> module_statuses.id, NOT NULL | repaired or not_repairable |
| pixels_repaired | SMALLINT | NOT NULL, DEFAULT 0, CHECK (pixels_repaired >= 0) | Repaired pixels |
| chips_replaced | SMALLINT | NOT NULL, DEFAULT 0, CHECK (chips_replaced >= 0) | Replaced chips |
| repair_note | TEXT | | Repair description |
| price | DECIMAL(10,2) | NOT NULL, DEFAULT 0, CHECK (price >= 0) | Final repair price (EUR) |
| completed_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Completion time |

---

# 14. Table: module_images

## Purpose
Image links for modules. Images stored on external drive/storage, database holds only URLs.

## Business rules
- Maximum 5 images per module (enforced in backend)
- Maximum 20MB per image (enforced in backend at upload)
- Auto-deleted 30 days after package arrival

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | Image ID |
| module_id | BIGINT | FK -> modules.id, NOT NULL | Related module |
| file_path | VARCHAR(2048) | NOT NULL | Storage URL/path |
| uploaded_by_user_id | BIGINT | FK -> users.id, NOT NULL | Uploader |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Upload time |

---

# 15. Table: audit_logs

## Purpose
Critical system change logs.

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | Audit log ID |
| entity_type | VARCHAR(50) | NOT NULL | package, module, repair, user |
| entity_id | BIGINT | NOT NULL | Related entity ID |
| action_type | VARCHAR(50) | NOT NULL | create, update, delete, status_change, admin_override |
| changed_by_user_id | BIGINT | FK -> users.id, NOT NULL | User who made the change |
| old_value_json | JSONB | | Previous value |
| new_value_json | JSONB | | New value |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Change time |

---

# 16. Table: refresh_tokens

## Purpose
JWT refresh tokens for rotation and revocation.

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | Token ID |
| user_id | BIGINT | FK -> users.id, NOT NULL | Token owner |
| token_hash | VARCHAR(255) | UNIQUE, NOT NULL | SHA-256 hash |
| expires_at | TIMESTAMPTZ | NOT NULL | Expiration time |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Creation time |
| revoked_at | TIMESTAMPTZ | NULL | Revocation time (null = active) |

---

# 17. Table: password_reset_tokens

## Purpose
Password reset request tokens.

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | Token ID |
| user_id | BIGINT | FK -> users.id, NOT NULL | User requesting reset |
| token_hash | VARCHAR(255) | UNIQUE, NOT NULL | SHA-256 hash |
| expires_at | TIMESTAMPTZ | NOT NULL | Expiration (1 hour) |
| used_at | TIMESTAMPTZ | NULL | When used (null = unused) |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Request time |

---

# 18. Table: technician_invite_tokens

## Purpose
Admin-created invite tokens for technician account setup.

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | Token ID |
| created_by_user_id | BIGINT | FK -> users.id, NOT NULL | Admin who created invite |
| email | VARCHAR(255) | NOT NULL | Invited technician email |
| name | VARCHAR(150) | NOT NULL | Technician name |
| service_center_id | BIGINT | FK -> service_centers.id, NOT NULL | Assigned service center |
| phone | VARCHAR(50) | | Technician phone |
| token_hash | VARCHAR(255) | UNIQUE, NOT NULL | SHA-256 hash |
| expires_at | TIMESTAMPTZ | NOT NULL | Expiration (48 hours) |
| used_at | TIMESTAMPTZ | NULL | When accepted (null = pending) |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Creation time |

---

# 19. Table: client_invite_tokens

## Purpose
Admin-created invite tokens for client account setup. Company data is stored on the invite and copied to `clients` on acceptance.

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | Token ID |
| created_by_user_id | BIGINT | FK -> users.id, NOT NULL | Admin who created invite |
| email | VARCHAR(255) | NOT NULL | Invited contact email |
| contact_name | VARCHAR(150) | NOT NULL | Contact person name |
| company_name | VARCHAR(255) | NOT NULL | Company name |
| contact_phone | VARCHAR(50) | | Contact phone |
| address | TEXT | | Company address |
| token_hash | VARCHAR(255) | UNIQUE, NOT NULL | SHA-256 hash |
| expires_at | TIMESTAMPTZ | NOT NULL | Expiration (48 hours) |
| used_at | TIMESTAMPTZ | NULL | When accepted (null = pending) |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Creation time |

---

# 20. Table: mfa_codes

## Purpose
6-digit email MFA codes for **admin logins only**. Cookies are issued only after successful verification.

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | Code ID |
| user_id | BIGINT | FK -> users.id, NOT NULL | Admin user |
| code_hash | VARCHAR(255) | NOT NULL | BCrypt hash of the 6-digit code (salted) |
| expires_at | TIMESTAMPTZ | NOT NULL | Expiration (10 minutes) |
| used_at | TIMESTAMPTZ | NULL | When used (null = unused) |
| attempts | SMALLINT | NOT NULL, DEFAULT 0 | Failed verification attempts (max 5) |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Creation time |

---

# 21. Table: notifications

## Purpose
In-app notifications. Displayed as badge/counter on login. Not real-time push.

## Columns

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | Notification ID |
| user_id | BIGINT | FK -> users.id, NOT NULL | Recipient |
| type | VARCHAR(50) | NOT NULL | status_change, client_joined, etc. |
| title | VARCHAR(255) | NOT NULL | Short title |
| message | TEXT | | Body |
| entity_type | VARCHAR(50) | | Related entity type |
| entity_id | BIGINT | | Related entity ID |
| is_read | BOOLEAN | NOT NULL, DEFAULT false | Read status |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Notification time |

---

# Indexes

## Standard indexes

| Table | Index | Columns |
|---|---|---|
| packages | idx_packages_client_id | client_id |
| packages | idx_packages_status | current_status_id |
| packages | idx_packages_service_center | service_center_id |
| modules | idx_modules_package_id | package_id |
| modules | idx_modules_technician | assigned_technician_id |
| modules | idx_modules_number | module_number |
| client_invite_tokens | idx_cit_email | email |
| mfa_codes | idx_mfa_user | user_id |
| audit_logs | idx_audit_entity | (entity_type, entity_id) |
| audit_logs | idx_audit_user | changed_by_user_id |
| audit_logs | idx_audit_created | created_at |
| package_status_history | idx_psh_package | package_id |
| module_status_history | idx_msh_module | module_id |
| module_images | idx_mi_module | module_id |
| audit_logs | idx_audit_action_type | action_type |

## Composite indexes

| Table | Index | Definition |
|---|---|---|
| packages | idx_packages_client_status | (client_id, current_status_id) WHERE deleted_at IS NULL |
| packages | idx_packages_sc_status | (service_center_id, current_status_id) WHERE deleted_at IS NULL |
| modules | idx_modules_package_status | (package_id, current_status_id) WHERE deleted_at IS NULL |

## Partial indexes (PostgreSQL-specific)

| Table | Index | Definition |
|---|---|---|
| packages | idx_packages_active | (id) WHERE deleted_at IS NULL |
| modules | idx_modules_active | (id) WHERE deleted_at IS NULL |
| modules | idx_modules_waiting | (package_id) WHERE current_status_id = 1 |
| notifications | idx_notif_unread | (user_id, created_at DESC) WHERE is_read = false |
| refresh_tokens | idx_rt_user_active | (user_id) WHERE revoked_at IS NULL |
| notifications | idx_notif_user_created | (user_id, created_at DESC) |
| packages | idx_packages_arrived_cleanup | (arrived_at) WHERE arrived_at IS NOT NULL AND deleted_at IS NULL |

## Partial unique indexes (uniqueness among live rows)

| Table | Index | Definition |
|---|---|---|
| packages | uq_packages_number_active | UNIQUE (package_number) WHERE deleted_at IS NULL |
| modules | uq_modules_pkg_number_active | UNIQUE (package_id, module_number) WHERE deleted_at IS NULL |

---

# Views

## v_package_summary
Used for: package lists (all roles), dashboards, statistics.

```sql
CREATE VIEW v_package_summary AS
SELECT
  p.id, p.package_number, p.client_id, p.is_internal,
  p.current_status_id, ps.code AS status_code, ps.name AS status_name,
  p.service_center_id, p.created_at,
  p.received_at, p.service_completed_at, p.shipped_at, p.arrived_at,
  COUNT(m.id) AS total_modules,
  COUNT(m.id) FILTER (WHERE ms.code = 'repaired') AS repaired_count,
  COUNT(m.id) FILTER (WHERE ms.code = 'not_repairable') AS not_repairable_count,
  COALESCE(SUM(mr.price), 0) AS total_value
FROM packages p
JOIN package_statuses ps ON p.current_status_id = ps.id
LEFT JOIN modules m ON m.package_id = p.id AND m.deleted_at IS NULL
LEFT JOIN module_statuses ms ON m.current_status_id = ms.id
LEFT JOIN module_repairs mr ON mr.module_id = m.id
WHERE p.deleted_at IS NULL
GROUP BY p.id, ps.code, ps.name;
```

## v_package_technician_breakdown
Used for: admin per-package statistics, technician breakdown per package.

```sql
CREATE VIEW v_package_technician_breakdown AS
SELECT
  m.package_id,
  m.assigned_technician_id,
  u.name AS technician_name,
  COUNT(m.id) AS module_count,
  COUNT(m.id) FILTER (WHERE ms.code = 'repaired') AS repaired_count,
  COUNT(m.id) FILTER (WHERE ms.code = 'not_repairable') AS not_repairable_count,
  COALESCE(SUM(mr.pixels_repaired), 0) AS total_pixels,
  COALESCE(SUM(mr.chips_replaced), 0) AS total_chips,
  COALESCE(SUM(mr.price), 0) AS total_value
FROM modules m
JOIN users u ON m.assigned_technician_id = u.id
LEFT JOIN module_statuses ms ON m.current_status_id = ms.id
LEFT JOIN module_repairs mr ON mr.module_id = m.id
WHERE m.deleted_at IS NULL
GROUP BY m.package_id, m.assigned_technician_id, u.name;
```

## v_module_detail
Used for: module lists, module detail pages, client repair history.

```sql
CREATE VIEW v_module_detail AS
SELECT
  m.id, m.package_id, m.module_number,
  m.current_status_id, ms.code AS status_code, ms.name AS status_name,
  m.assigned_technician_id, u.name AS technician_name,
  m.problem_type_id, pt.name AS problem_type_name,
  mr.pixels_repaired, mr.chips_replaced,
  mr.repair_note, mr.price, mr.completed_at,
  mr.decision_status_id,
  m.created_at
FROM modules m
JOIN module_statuses ms ON m.current_status_id = ms.id
JOIN problem_types pt ON m.problem_type_id = pt.id
LEFT JOIN users u ON m.assigned_technician_id = u.id
LEFT JOIN module_repairs mr ON mr.module_id = m.id
WHERE m.deleted_at IS NULL;
```

---

# Relationship summary

| Table A | Table B | Relationship |
|---|---|---|
| roles | users | 1:N (role_id on users) |
| users | clients | 1:1 |
| users | technicians | 1:1 |
| service_centers | technicians | 1:N |
| service_centers | packages | 1:N |
| clients | packages | 1:N |
| package_statuses | packages | 1:N |
| packages | package_status_history | 1:N |
| packages | modules | 1:N |
| problem_types | modules | 1:N |
| module_statuses | modules | 1:N |
| technicians | modules | 1:N |
| modules | module_status_history | 1:N |
| modules | module_repairs | 1:1 |
| technicians | module_repairs | 1:N |
| modules | module_images | 1:N |
| users | audit_logs | 1:N |
| users | refresh_tokens | 1:N |
| users | password_reset_tokens | 1:N |
| users | technician_invite_tokens | 1:N (created_by) |
| users | client_invite_tokens | 1:N (created_by) |
| users | mfa_codes | 1:N |
| users | notifications | 1:N |

---

# Image storage strategy

Images are NOT stored in the database. Database holds only URLs/paths.

- **Storage:** Local filesystem (with `ImageStorageService` abstraction — swappable to S3/R2 later without code changes)
- **Path format:** `/{year}/{month}/{module_id}/{uuid}.{ext}`
- **Capacity:** ~600 GB/year uploaded. With auto-cleanup, ~100-200 GB active on disk at any time.
- **Upload:** Backend validates size (max 20MB) and MIME type, saves to storage, stores URL in module_images
- **Access:** Backend serves via endpoint with ownership + status checks
- **Auto-cleanup:** Spring @Scheduled job runs daily. Deletes images from storage + database where package arrived_at < NOW() - 30 days

---

# Implementation notes

- Package and module workflow rules enforced in backend (Spring service layer)
- Lookup tables define valid values; business logic defines valid transitions
- `approx_quantity` is informational only; real count from modules table
- Archive is application logic, not a separate entity
- Denormalized dates on packages updated atomically with status changes (single @Transactional)
- All status changes + history + audit = one database transaction
- **No stored procedures** — use views for reads, JPA repositories for writes
- Views managed via Flyway migrations
- JPA maps views as @Immutable entities (read-only). `v_package_technician_breakdown`
  has no single-column key — map it with an `@EmbeddedId (package_id, assigned_technician_id)`
- `created_at` / `updated_at` are maintained by the ORM via Hibernate
  `@CreationTimestamp` / `@UpdateTimestamp` on the Java entities — no database triggers

---

# Seed data

Seeded by the Flyway V1 migration (lookup data only):

1. **roles**: admin (1), technician (2), client (3)
2. **package_statuses**: all 7 statuses with sort_order
3. **module_statuses**: all 3 statuses with sort_order and is_final
4. **problem_types**: all active problem types
5. **service_centers**: at least one active center (`ZAGREB` / Zagreb / Croatia)

The **admin account is NOT seeded via Flyway**. It is created on first startup by
a Spring `ApplicationRunner` that reads `APP_BOOTSTRAP_ADMIN_EMAIL` and
`APP_BOOTSTRAP_ADMIN_PASSWORD` env vars and inserts the admin (role_id = 1) only
if none exists. This keeps the BCrypt password hash out of migrations and out of
the repo. The admin logs in with password + email MFA code.
