import { getStoredToken } from './storage'
import type { LoginRequest, LoginResponse } from './types'

async function requestJson<T>(path: string, init: RequestInit) {
  const headers = new Headers(init.headers)
  const token = getStoredToken()

  headers.set('Content-Type', 'application/json')

  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  let response: Response

  try {
    response = await fetch(path, {
      ...init,
      headers,
    })
  } catch {
    throw new Error('Unable to reach the login service.')
  }

  const rawBody = await response.text()
  const payload = rawBody ? (JSON.parse(rawBody) as T) : null

  if (!response.ok) {
    const message =
      typeof payload === 'object' &&
      payload !== null &&
      'message' in payload &&
      typeof payload.message === 'string'
        ? payload.message
        : response.statusText || 'Request failed.'

    throw new Error(message)
  }

  return payload as T
}

export function postLogin(payload: LoginRequest) {
  return requestJson<LoginResponse>('/api/manager/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
