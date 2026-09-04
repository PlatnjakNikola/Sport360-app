# Client Pages — Final Specification

## Overview
This document defines the final structure, logic, and UX behavior of the client-side pages for tracking service packages and modules.

The client:
- Sends packages to service
- Tracks package status
- Views service results AFTER service is completed (status >= repaired_waiting_shipment)

The client DOES NOT:
- Enter modules
- Edit service data
- See technician names

---

## Core Principles

1. Separation of concerns:
   - Package = logistics (client domain)
   - Module = service (technician domain)

2. Data visibility control:
   - Module data is hidden until status >= repaired_waiting_shipment

3. Minimal UI:
   - Few pages, deep structure

4. Privacy:
   - Client does not see technician names

---

## Pages Structure

1. Dashboard
2. Create Package
3. All Packages
4. Package Detail (with tabs)
5. Module Detail
6. Notifications

---

# 1. Dashboard

## Purpose
Quick overview of client's packages.

## Content
- Package counts by status (cards/badges):
  - Created
  - Sent to service
  - Received by service
  - On service
  - Repaired — waiting shipment
  - Shipped to client
  - Arrived

## Activity Feed
- Recent status changes on client's packages

## Quick Actions
- Create package (button)
- View all packages (link)

## Notifications
- Unread notifications badge (exclamation icon / counter) — visible on login
- Recent notifications preview
- Notifications are in-app only (no real-time push). User sees badge when they log in or navigate.

---

# 2. Create Package

## Form Fields
- Package / pallet number (required, unique)
- Outbound tracking link (optional, editable later)
- Description — contents + quantity info (required)
- Note — optional free text
- Approx quantity (optional, informational)

## Rules
- No module input (client only describes shipment)
- Status set to "created" automatically
- client_id set from authenticated user

## After Submit
- Redirect to package detail
- Success message

---

# Account Creation (Invite Flow)

## How a client gets an account
- There is NO public registration.
- The admin creates the client invite: company name, contact person name, email, phone, address.
- The system emails a one-time invite link (valid 48 hours, resendable by admin).
- The client opens the link, sees their pre-filled company data, and sets a password.
- The account is active immediately — there is no approval step.

## Login
- Email + password (no MFA for clients — MFA applies to admin only).
- Deactivated accounts cannot log in.
- Forgot password → standard reset link flow.

---

# 3. All Packages

## Table
- Package number
- Status (badge)
- Date created
- Last update
- Tracking link
- Module count (visible only if status >= repaired_waiting_shipment)

## Features
- Search by package number
- Filter by status
- Filter by date range
- Sorting (by date, status)
- Pagination

---

# 4. Package Detail

## Always Visible
- Package number
- Status (badge)
- Outbound tracking link (editable — pencil icon)
- Return tracking link (if exists)
- Description
- Note
- Approx quantity

## Editable Fields
Client can edit these at any time:
- Outbound tracking link
- Note
- Description

Edits are saved via PATCH endpoint, logged in audit.

---

## Timeline
Full status history:
- Created
- Sent to service
- Received by service
- On service
- Repaired — waiting shipment
- Shipped to client
- Arrived

Each entry shows:
- Status name
- Date/time

(Client does NOT see who changed the status — only the status and timestamp)

---

## Tabs

### Tab 1: Overview
- Basic info (all fields above)
- Timeline
- Shipment data (tracking links)

### Tab 2: Repair History

#### When locked (status < repaired_waiting_shipment):
Show message:
"Service is still in progress. Module details will be available after completion."

#### When unlocked (status >= repaired_waiting_shipment):
Show table:
- Module number
- Status (Repaired / Not repairable) — badge
- Problem type
- Pixels repaired
- Chips replaced
- Price
- Action: View detail

### Tab 3: Summary (visible only when unlocked)
- Total modules
- Repaired count
- Not repairable count
- Total repair cost
- Repair vs scrap ratio (percentage)

---

## Mark as Sent

### Condition
Only visible when status = created

### Button
"Mark as Sent"

### Confirmation Modal
- Text: "Are you sure you want to mark this package as sent to service?"
- Optional: confirm/update outbound tracking link
- Confirm → status = sent_to_service
- Cancel → close modal

### After Confirmation
- Status updates to "Sent to service"
- Success message
- Timeline updates
- Notification sent to admin

---

## Confirm Arrival

### Condition
Only visible when status = shipped_to_client

### Button
"Confirm Package Arrival"

### Confirmation Modal
- Text: "Are you sure you want to confirm that this package has arrived?"
- Confirm → status = arrived, arrived_at set
- Cancel → close modal

### After Confirmation
- Status updates to "Arrived"
- Success message
- Timeline updates

---

# 5. Module Detail

## Accessible only when package status >= repaired_waiting_shipment

## Fields
- Module number
- Package reference (link back to package)
- Status (badge: Repaired / Not repairable)
- Problem type
- Pixels repaired
- Chips replaced
- Repair note
- Price

## Privacy
- Technician name is NOT shown to client

## Logic
If pixels_repaired = 0 AND chips_replaced = 0:
- Repair note becomes primary info (e.g. "housing replaced", "connector replaced", "cleaned contacts")

---

# 6. Notifications

## Page
- List of all notifications (paginated)
- Filter: read/unread
- Mark as read (individual and mark all)
- Click notification → navigate to related package

## Notification Types
- Package received by service
- Service started
- Service completed
- Package shipped to client

---

# 7. User Flow Summary

1. Admin creates client invite (company data + contact email)
2. Client opens the email link and sets a password
3. Client logs in
4. Client creates package (status = created)
5. Client adds tracking link
6. Client marks package as sent (status = sent_to_service)
7. Client tracks status via dashboard/package detail
8. While status < repaired_waiting_shipment:
   - No module visibility
   - Timeline shows progress
9. When status = repaired_waiting_shipment:
   - Modules become visible
   - Repair history unlocked
   - Summary tab appears
10. Package shipped back to client
11. Client confirms arrival (button, confirmation modal)
12. Status = arrived

---

# 8. Statuses (consistent with all documents)

| Code | Display Name | Sort |
|---|---|---|
| created | Created | 1 |
| sent_to_service | Sent to service | 2 |
| received_by_service | Received by service | 3 |
| on_service | On service | 4 |
| repaired_waiting_shipment | Repaired — waiting shipment | 5 |
| shipped_to_client | Shipped to client | 6 |
| arrived | Arrived | 7 |
