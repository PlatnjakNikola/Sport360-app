export interface ApiFieldError {
  field: string
  message: string
}

export interface ApiErrorShape {
  code: string
  message: string
  details?: ApiFieldError[]
}

/** Standard success envelope returned by the backend. */
export interface Envelope<T> {
  success: boolean
  data: T
}

export interface Pagination {
  page: number
  limit: number
  total: number
  totalPages: number
}

export interface PageResponse<T> {
  items: T[]
  pagination: Pagination
}
