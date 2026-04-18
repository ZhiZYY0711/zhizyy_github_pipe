import type { AuthSession } from './types'

const STORAGE_KEYS = {
  token: 'token',
  jwt: 'jwt',
  username: 'username',
  isLoggedIn: 'isLoggedIn',
} as const

function getStorage(): Storage | null {
  if (typeof window === 'undefined') {
    return null
  }

  return window.localStorage
}

export function persistAuthSession(session: AuthSession) {
  const storage = getStorage()

  if (!storage) {
    return
  }

  storage.setItem(STORAGE_KEYS.token, session.token)
  storage.setItem(STORAGE_KEYS.jwt, session.jwt)
  storage.setItem(STORAGE_KEYS.username, session.username)
  storage.setItem(STORAGE_KEYS.isLoggedIn, session.isLoggedIn)
}

export function clearAuthSession() {
  const storage = getStorage()

  if (!storage) {
    return
  }

  storage.removeItem(STORAGE_KEYS.token)
  storage.removeItem(STORAGE_KEYS.jwt)
  storage.removeItem(STORAGE_KEYS.username)
  storage.removeItem(STORAGE_KEYS.isLoggedIn)
}

export function getStoredToken() {
  const storage = getStorage()

  if (!storage) {
    return ''
  }

  return (
    storage.getItem(STORAGE_KEYS.token) ||
    storage.getItem(STORAGE_KEYS.jwt) ||
    ''
  )
}

export function getStoredUsername() {
  const storage = getStorage()

  if (!storage) {
    return ''
  }

  return storage.getItem(STORAGE_KEYS.username) || ''
}

export function hasAuthSession() {
  const storage = getStorage()
  const token = getStoredToken()

  if (!storage) {
    return false
  }

  if (!token) {
    clearAuthSession()
    return false
  }

  if (storage.getItem(STORAGE_KEYS.isLoggedIn) !== 'true') {
    storage.setItem(STORAGE_KEYS.isLoggedIn, 'true')
  }

  return true
}
