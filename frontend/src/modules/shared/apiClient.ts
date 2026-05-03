import { getStoredToken } from '../auth/storage'

export type ApiEnvelope<T> = {
  code?: number
  message?: string
  data?: T
  success?: boolean
  timestamp?: number
  [key: string]: unknown
}

type ApiRequestOptions = {
  method?: 'GET' | 'POST' | 'PATCH' | 'DELETE'
  body?: unknown
  query?: Record<string, string | number | boolean | null | undefined>
  headers?: HeadersInit
  requiresAuth?: boolean
}

const API_PREFIX = '/api'
const MANAGER_PREFIX = '/manager'

function buildApiUrl(path: string, query?: ApiRequestOptions['query']) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const managerScopedPath = normalizedPath.startsWith(MANAGER_PREFIX)
    ? normalizedPath
    : `${MANAGER_PREFIX}${normalizedPath}`
  const url = new URL(`${API_PREFIX}${managerScopedPath}`, window.location.origin)

  if (query) {
    Object.entries(query).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        url.searchParams.set(key, String(value))
      }
    })
  }

  return `${url.pathname}${url.search}`
}

export function getAuthorizedUrl(
  path: string,
  query?: ApiRequestOptions['query'],
  includeTokenQuery = false,
) {
  const token = getStoredToken()
  const mergedQuery = includeTokenQuery && token ? { ...query, token } : query

  return buildApiUrl(path, mergedQuery)
}

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}) {
  const method = options.method || 'GET'
  const headers = new Headers(options.headers)
  const token = getStoredToken()

  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }

  if (token && options.requiresAuth !== false) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(buildApiUrl(path, options.query), {
    method,
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  })

  const rawBody = await response.text()
  const payload = rawBody ? (JSON.parse(rawBody) as T) : (null as T)
  const envelope = payload as ApiEnvelope<unknown> | null

  if (!response.ok) {
    const message =
      envelope && typeof envelope.message === 'string'
        ? envelope.message
        : response.statusText || 'Request failed.'

    throw new Error(message)
  }

  if (envelope && typeof envelope === 'object') {
    if (typeof envelope.code === 'number' && envelope.code !== 0 && envelope.code !== 200) {
      throw new Error(envelope.message || `Request failed with code ${envelope.code}.`)
    }

    if (envelope.success === false) {
      throw new Error(envelope.message || 'Request failed.')
    }
  }

  return payload
}

export function unwrapData<T>(payload: ApiEnvelope<T> | T, fallback: T) {
  if (payload && typeof payload === 'object' && 'data' in payload) {
    return (payload as ApiEnvelope<T>).data ?? fallback
  }

  return (payload as T) ?? fallback
}
