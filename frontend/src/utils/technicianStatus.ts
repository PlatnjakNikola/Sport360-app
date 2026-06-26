export type TechnicianActionKind = 'next' | 'confirm-arrival'

export interface TechnicianAction {
  label: string
  kind: TechnicianActionKind
}

/**
 * The single forward action available to a technician for a package, by status.
 * Internal packages have no client, so the technician also drives created→sent and
 * confirms arrival; external packages leave those steps to the client.
 */
export function technicianAction(statusCode: string, isInternal: boolean): TechnicianAction | null {
  switch (statusCode) {
    case 'created':
      return isInternal ? { label: 'Mark as sent', kind: 'next' } : null
    case 'sent_to_service':
      return { label: 'Mark as received', kind: 'next' }
    case 'received_by_service':
      return { label: 'Start service', kind: 'next' }
    case 'on_service':
      return { label: 'Finish service', kind: 'next' }
    case 'repaired_waiting_shipment':
      return { label: 'Mark as shipped', kind: 'next' }
    case 'shipped_to_client':
      return isInternal ? { label: 'Confirm arrival', kind: 'confirm-arrival' } : null
    default:
      return null
  }
}
