import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ApiErrorShape } from '../types/common'

export const api = axios.create({
  baseURL: '/api/v1',
  withCredentials: true,
})

// Auth endpoints whose 401s are meaningful (bad credentials / expired link) and
// must not trigger the refresh-and-retry dance.
const NO_AUTO_REFRESH = [
  '/auth/login',
  '/auth/refresh-token',
  '/auth/mfa/verify',
  '/auth/mfa/resend',
  '/auth/forgot-password',
  '/auth/reset-password',
]

let refreshPromise: Promise<unknown> | null = null

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<{ error?: ApiErrorShape }>) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined
    const status = error.response?.status
    const url = original?.url ?? ''
    const skipRefresh = NO_AUTO_REFRESH.some((path) => url.includes(path))

    if (status === 401 && original && !original._retry && !skipRefresh) {
      original._retry = true
      try {
        refreshPromise = refreshPromise ?? api.post('/auth/refresh-token')
        await refreshPromise
        refreshPromise = null
        return api(original)
      } catch {
        refreshPromise = null
      }
    }
    return Promise.reject(toApiError(error))
  },
)

/** Normalizes any thrown value into the API error shape. */
export function toApiError(error: unknown): ApiErrorShape {
  const axiosError = error as AxiosError<{ error?: ApiErrorShape }>
  return axiosError?.response?.data?.error ?? { code: 'NETWORK_ERROR', message: 'Unable to reach the server' }
}

/** Best-effort human message from a rejected API call (already-normalized or raw). */
export function getErrorMessage(error: unknown): string {
  if (error && typeof error === 'object' && 'message' in error) {
    return String((error as ApiErrorShape).message)
  }
  return 'Something went wrong'
}
