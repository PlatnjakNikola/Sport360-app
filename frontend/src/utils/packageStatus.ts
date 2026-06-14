export const PACKAGE_STATUSES = [
  { code: 'created', name: 'Created' },
  { code: 'sent_to_service', name: 'Sent to service' },
  { code: 'received_by_service', name: 'Received by service' },
  { code: 'on_service', name: 'On service' },
  { code: 'repaired_waiting_shipment', name: 'Repaired — waiting shipment' },
  { code: 'shipped_to_client', name: 'Shipped to client' },
  { code: 'arrived', name: 'Arrived' },
] as const

export const PACKAGE_STATUS_ORDER: Record<string, number> = Object.fromEntries(
  PACKAGE_STATUSES.map((status, index) => [status.code, index + 1]),
)

/** Module/repair data becomes visible to the client at this status and beyond. */
export function isRepairUnlocked(statusCode: string): boolean {
  return (PACKAGE_STATUS_ORDER[statusCode] ?? 0) >= PACKAGE_STATUS_ORDER.repaired_waiting_shipment
}
