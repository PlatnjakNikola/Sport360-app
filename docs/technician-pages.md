# Technician Pages — Final Specification

## Overview
This document defines the final structure and behavior of all technician (service) pages.

## Pages Structure
1. Service Dashboard
2. Active Packages (external client packages)
3. Package Service Detail
4. Module Detail (Repair Screen)
5. Archive
6. Internal Packages (Sport360)
7. Notifications

---

# 1. Service Dashboard

## Purpose
Quick overview of service status. No editing.

## Content
- Packages waiting receipt (count)
- Packages received (count)
- Packages in service (count)
- Packages finished — waiting shipment (count)

## Personal Stats
- My modules today
- My modules this week
- My repair rate (repaired / total)
- My total value this week

## Activity Feed
- Recent repairs (mine)
- Recent status changes

## Quick Links
- Active Packages
- Archive

## Notifications
- Unread notifications badge (exclamation icon / counter) — visible on login
- Recent notifications (new package received, status changes)
- Notifications are in-app only (no real-time push). User sees badge when they log in or navigate.

---

# 2. Active Packages

## Purpose
Main work queue for technicians.

## Visible statuses
- Sent to service
- Received by service
- On service
- Repaired — waiting shipment

## Filtering
Packages are filtered by the technician's assigned service center. A technician only sees packages for their own service center.

## Table
- Package number
- Status (badge)
- Created date
- Received date
- Approx quantity (informational)
- Scanned modules count (real, from modules table)
- Finished modules count (repaired + not_repairable)
- Action: Open

## Features
- Search (by package number)
- Filter by status
- Sorting (by date, status)
- Pagination

---

# 3. Package Service Detail

## Purpose
Main working screen for a package.

---

## A. Header
- Package number
- Status (badge)
- Tracking link (outbound)
- Description
- Note
- Dates (created, sent, received)
- Approx quantity
- Actual module count (from modules)

---

## B. Status Control
Single forward action button. Label depends on current status:

| Current status | Button label | Next status |
|---|---|---|
| sent_to_service | Mark as received | received_by_service |
| received_by_service | Start service | on_service |
| on_service | Finish service | repaired_waiting_shipment |
| repaired_waiting_shipment | Mark as shipped | shipped_to_client |

Rules:
- Only one step forward at a time
- No skipping
- No going back
- Finish service requires: at least 1 module AND all modules in final state
- Mark as shipped: optional return tracking link input

---

## C. Module Scan

### Entry modes
- QR scan input (primary)
- Manual input fallback (text field)

### Fields
- Module number (from scan or manual)
- Problem type (dropdown, from problem_types table)
- Image upload (max 5 images, max 20MB each)

### Auto-filled
- Package number (from current package)
- Service center (from technician profile)
- Technician (current user)

### After submit
- Module created with status = waiting_for_repair
- Module appears in module list below
- Form stays open for next scan (batch scanning mode)

### Error handling
- QR scan failure: show error message, offer manual input
- Duplicate module_number within package: show error, do not create
- Invalid problem type: show validation error

### Batch scanning
- After successful scan, form clears module_number field but keeps problem type selection
- Technician can scan next module without navigating away
- Success toast for each created module

---

## D. Module List

### Table
- Module number
- Problem type
- Status (badge)
- Technician
- Thumbnail (first image, if exists)
- Price (if finished)
- Action

### Action behavior by status
- Waiting for repair → "Repair" button → opens repair screen
- Repaired → "View" link → opens read-only detail
- Not repairable → "View" link → opens read-only detail

### Pagination
For packages with many modules.

---

## E. Package Completion

### Condition
All modules must be in final state (repaired or not_repairable).

### Button
"Finish Service" becomes enabled only when condition is met.

### Confirmation Modal
Shows:
- Total modules
- Repaired count
- Not repairable count
- Total repair value

Buttons:
- Confirm → status = repaired_waiting_shipment, service_completed_at set
- Cancel → close modal

---

## F. Shipping

### Button
"Mark as Shipped" (visible when status = repaired_waiting_shipment)

### Modal
- Optional: return tracking link input
- Confirm → status = shipped_to_client, shipped_at set
- Cancel → close modal

### Result
Package moves to Archive.

---

# 4. Module Detail (Repair Screen)

## Read-only section
- Module number
- Package number (link)
- Service center
- Problem type
- Images (gallery view, expandable)
- Assigned technician
- Status (badge)

---

## Edit Mode (only when status = waiting_for_repair)

### Fields
- Pixels repaired (number input)
- Chips replaced (number input)
- Technician (dropdown, pre-filled with current user)
- Note (textarea)
- Price (decimal input)
- Decision:
  - Repaired
  - Not repairable

### Logic

#### If decision = Repaired
- All fields enabled
- Price required

#### If decision = Not repairable
- Pixels repaired: disabled, set to 0
- Chips replaced: disabled, set to 0
- Price: disabled, set to 0 or null
- Note: enabled (can explain why not repairable)

### Submit
- Creates module_repairs row
- Updates module status
- Creates status history + audit log
- Redirects back to package detail

---

## Finished Module View
- All fields read-only
- Decision shown as badge
- No edit actions

---

# 5. Archive

## Purpose
View completed shipments. Read-only.

## Filtering
Filtered by technician's service center (same as active packages).

## Visible statuses
- Shipped to client
- Arrived

## Table
- Package number
- Shipped date
- Status (badge)
- Total modules
- Repaired count
- Not repairable count
- Total value
- Action: Open

## Features
- Search (by package number)
- Sorting (by date)
- Pagination

---

## Archive Package View
- Full package details (read-only)
- Timeline (full status history)
- Repair history (all modules)
- Summary (counts, values)

## Archive Module View
- Full module details
- Images (gallery)
- Repair data
- Read-only

## Rules
- No editing
- No status changes

---

# 6. Internal Packages (Sport360)

## Purpose
Manage packages for Sport360's own modules. These are not client shipments — they are internal batches of modules that need repair.

## Differences from external packages
- Created by technician (not client)
- No client assigned (client_id = null)
- Package number is a descriptive label (e.g. "5mm novi", "5mm stari", "16mm stari", "osijek moduli", "rijeka moduli")
- Technician can confirm arrival (not just client)
- Same status workflow applies

## Create Internal Package
Button: "Create Internal Package"

### Form
- Package number / label (free text, required — e.g. "5mm novi")
- Description (optional)
- Note (optional)
- Approx quantity (optional)

### Auto-filled
- Service center (from technician profile)
- is_internal = true
- Status = created

## Internal Packages List
Filtered by technician's service center (same as active packages).

### Table
- Package number / label
- Status (badge)
- Created date
- Module count
- Finished modules count
- Total value
- Action: Open

### Features
- Search (by package number)
- Filter by status (all statuses including arrived)
- Sorting (by date, status)
- Pagination

## Package Detail
Same as regular Package Service Detail (section 3) with these differences:
- No outbound tracking link (internal, not shipped from client)
- Module scan, module list, repair — all the same
- Status control — same forward-only flow
- **Confirm Arrival** button visible at shipped_to_client status (technician can click, unlike external packages)
- Return tracking link optional at shipping step

## Module Repair
Uses the same repair screen as external packages (section 4). Module workflow is identical.

---

# 7. Notifications Page

## Content
- List of notifications (paginated)
- Filter: read/unread
- Mark as read (individual and mark all)
- Click → navigate to related package

## Notification Types for Technician
- New package status: sent_to_service (new work incoming)
- Package status changes relevant to technician's work

---

# 8. Final Notes

- Modules created only in service context
- Client input (approx_quantity) is informational, not source of truth
- Strict forward-only status flow
- No approval system
- Repair data visible to client only after package reaches repaired_waiting_shipment
- Archive is mandatory and read-only
- Batch scanning reduces repetitive navigation
- Personal stats help technicians track their productivity
- All terminology uses "problem type" (not "error type")
- Internal packages (Sport360) use separate page but share module repair workflow
- Technician can confirm arrival only for internal packages
