import type { Role } from '../types/auth'

/** The landing route for a given role. */
export function roleHome(role: Role): string {
  return `/${role}`
}
