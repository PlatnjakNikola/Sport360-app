# Admin Pages — Final Specification

## Overview

Admin has full visibility and controlled override capabilities across the system.
Admin is the control layer, not a workflow actor.

---

# 1. Role of Admin

Admin is responsible for:
- Full system visibility
- Data correction
- Performance analysis
- Audit control
- User management (technician + client invites)

Admin is NOT part of normal workflow:
- Does not create packages (client does)
- Does not perform service (technician does)

---

# 2. Permissions

## Admin CAN:
- View all packages (including soft-deleted)
- View all statuses
- Open any package
- View repair history
- View module details
- Edit package data
- Edit module data
- Override statuses (with audit logging)
- Add correction modules
- Soft delete and restore packages/modules
- View statistics with date range filters and CSV export
- Access audit logs with filters
- Create technician accounts via invite
- Create client accounts via invite
- Manage technicians (edit, deactivate/activate)
- Manage clients (view, deactivate/activate)
- Manage catalogs (problem types, service centers)
- View and manage notifications

## Admin CANNOT:
- Create packages via normal flow
- Run technician workflow (scan/repair as standard)
- Register through public registration (no public registration exists)

---

# 3. Pages Structure

1. Admin Dashboard
2. User Management (Technicians + Clients)
3. All Packages
4. Package Detail
5. Module Detail
6. Statistics / Reports
7. Audit Log
8. Trash (soft-deleted items)
9. Catalogs (problem types & service centers)
10. Notifications

---

# 4. Admin Dashboard

## Purpose
System overview at a glance.

## Content
- Total packages
- Packages by status (cards/badges)
- Total modules
- Repaired vs not repairable counts
- Active vs completed packages

## Pending Actions
- Pending invites — technicians and clients who have not yet accepted (count + quick link with resend option)

## Activity Feed
- Latest package changes
- Latest repairs
- Latest technician invites
- Latest accepted client invites

## Notifications Panel
- Unread notifications badge (exclamation icon / counter) — visible on login
- Recent notifications list
- Link to all notifications
- Notifications are in-app only (no real-time push). User sees badge when they log in or navigate.

---

# 5. User Management

## 5A. Technicians Tab

### Table
- Name
- Email
- Service center
- Phone
- Status (active / inactive)
- Created date
- Modules repaired (count)
- Action: View / Edit / Deactivate

### Actions
- **Invite Technician** button → modal:
  - Email
  - Name
  - Service center (dropdown)
  - Phone
  - Submit → sends invite email (valid 48 hours)
- **Pending invites** list (sent, not yet accepted) with **Resend** action
- **Edit Technician** → modal:
  - Name, phone, service center
  - Save changes (logged in audit)
- **Deactivate / Activate** toggle (with confirmation)

### Technician Detail View
- Profile info
- Assigned service center
- Statistics: modules repaired, scrapped, total value, avg price
- Recent activity

## 5B. Clients Tab

### Table
- Company name
- Contact name
- Email
- Contact phone
- Status (invited / active)
- Packages count
- Created date
- Action: View / Edit / Deactivate

### Actions
- **Invite Client** button → modal:
  - Company name
  - Contact person name
  - Email
  - Contact phone
  - Address
  - Submit → creates invite (valid 48 hours) and sends invite email
- **Pending invites** list (sent, not yet accepted) with **Resend** action

(Clients are not deactivated from the admin UI — there is no client-update endpoint;
only technicians have an activate/deactivate toggle.)

### Client Detail View
- Profile info (company data)
- Package list (link to package detail)
- Statistics: total packages, active packages, total modules

---

# 6. All Packages

## Shows ALL statuses (including soft-deleted with filter)
- Created
- Sent to service
- Received by service
- On service
- Repaired — waiting shipment
- Shipped to client
- Arrived

## Table
- Package number
- Client (company name)
- Service center
- Status
- Dates (created, received, shipped)
- Module count
- Repaired count
- Not repairable count
- Action: Open / Edit / Delete

## Features
- Search (by package number, client name)
- Filter by status
- Filter by client
- Filter by date range
- Filter by service center
- Filter by package type: internal / external / all
- Sorting (by date, status, client)
- Pagination
- Toggle: show/hide soft-deleted

## Bulk Actions
- Bulk soft delete (with confirmation modal)
- Bulk status override (with confirmation modal, logged)

---

# 7. Package Detail

## Sections

### A. Info
- Package number
- Client (with link to client detail)
- Service center
- Status (with badge)
- Tracking links (outbound + return)
- Description
- Note
- Dates (created, received, service started, completed, shipped, arrived)

### B. Timeline
Full status history with:
- Status name
- Changed by (user name)
- Changed at (datetime)

### C. Repair History
Table of modules:
- Module number
- Status (badge)
- Technician
- Problem type
- Price
- Action: Open module detail

### D. Summary
- Total modules
- Repaired count
- Not repairable count
- Total cost
- Average price
- Breakdown per technician (modules count, total value)

### E. Admin Actions
- Edit package (modal with editable fields)
- Change status (dropdown, any status — override with audit log)
- Edit tracking links
- Add correction module
- Soft delete (with confirmation)
- Restore (if soft-deleted)

---

# 8. Module Detail

## Shows
- Module number
- Package (with link)
- Technician
- Problem type
- Status (badge)
- Pixels repaired
- Chips replaced
- Repair note
- Price
- Images (gallery view)
- Created at
- Completed at

## Admin Actions
- Edit all fields (modal)
- Change technician
- Change status
- Edit price
- Delete module (soft, with confirmation)
- Restore (if soft-deleted)

All edits logged in audit.

---

# 9. Statistics / Reports

## Filters
- Date range picker: from / to
- Quick presets: this week, this month, this quarter, this year, all time
- Package type filter: internal / external / all
- CSV export button for each section

## Global Package Stats
- Total packages
- By status (chart + table)
- By client (table)
- By period (chart — packages per week/month)

## Global Module Stats
- Total modules
- Repaired vs not repairable (pie chart)
- By problem type (bar chart + table)

## Per-Package Breakdown (select a package to view)
Opens detailed view for a specific package:

### Module Details
- Total modules in this package
- Repaired count, not repairable count
- Total pixels repaired (sum)
- Total chips replaced (sum)
- Modules by problem type (table)

### Technician Breakdown
- Per technician: modules count (repaired / not repairable)
- Per technician: total repair value
- Per technician: pixels repaired, chips replaced

---

# 10. Audit Log

## Table
- Timestamp
- User (who)
- Entity type (package / module / user)
- Entity ID (with link)
- Action (create / update / delete / status_change / admin_override)
- Old value (expandable JSON)
- New value (expandable JSON)

## Filters
- Entity type dropdown
- Action type dropdown
- User dropdown
- Date range picker
- Search by entity ID

## Pagination
Standard pagination with configurable page size.

## Entities tracked
- Package
- Module
- Module repair
- User (technician/client invites, deactivation)
- Catalogs (problem types, service centers)

---

# 11. Trash (Soft-Deleted Items)

## Purpose
View and manage soft-deleted packages and modules.

## Table
- Entity type
- Name/Number
- Deleted by
- Deleted at
- Action: View / Restore / Permanent delete (future)

## Features
- Filter by entity type
- Search
- Bulk restore

---

# 12. Catalogs (Problem Types & Service Centers)

## Purpose
Admin manages the lookup data used by the workflow. One page, two tabs.

## Tab 1: Problem Types
### Table
- Code
- Name
- Sort order
- Active (badge)
- Usage count (modules using this type)

### Actions
- **Add Problem Type** → modal: code + name + sort order. Code is immutable after creation.
- **Edit** → modal: name, sort order
- **Deactivate / Activate** toggle — deactivated types are hidden from new module forms but remain on historical records
- No hard delete (history integrity)

## Tab 2: Service Centers
### Table
- Code
- Name
- Country, City
- Active (badge)
- Technicians count

### Actions
- **Add Service Center** → modal: code, name, country, city, address
- **Edit** → modal: name, country, city, address
- **Deactivate** — blocked while active technicians are still assigned to the center

All catalog changes are logged in audit.

---

# 13. Notifications

## Page
- List of all notifications (paginated)
- Filter: read/unread
- Mark as read (individual and bulk)
- Click notification → navigate to related entity

## Notification Types
- Client joined (invite accepted)
- New package created
- Package status changed
- Client confirmed arrival
- Technician joined (invite accepted)

---

# 14. Rules

- All edits logged in audit
- Package/module delete = soft delete (restorable from Trash)
- User deactivation = is_active flag (no soft delete on users)
- Status override allowed but logged as admin_override
- Admin corrections allowed but tracked
- Bulk actions require confirmation modal

---

# 15. Final Notes

- Admin is control layer, not workflow actor
- Statistics = key business value
- Audit log = mandatory, with full filtering
- User management = technician + client invites (no public registration, no approval queue)
- Trash = safety net for soft deletes
- Catalogs keep problem types and service centers maintainable without developer involvement
