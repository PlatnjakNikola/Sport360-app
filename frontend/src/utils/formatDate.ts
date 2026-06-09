import { format } from 'date-fns'

export function formatDateTime(iso: string): string {
  try {
    return format(new Date(iso), 'd MMM yyyy, HH:mm')
  } catch {
    return iso
  }
}

export function formatDate(iso: string): string {
  try {
    return format(new Date(iso), 'd MMM yyyy')
  } catch {
    return iso
  }
}
