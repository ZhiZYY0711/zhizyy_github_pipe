import type { AppRouteMeta } from '../../router/types'
import { hasAuthSession } from './storage'

export function getAuthRedirect(meta: AppRouteMeta) {
  const isAuthenticated = hasAuthSession()

  if (meta.requiresAuth && !isAuthenticated) {
    return '/login'
  }

  if (meta.guestOnly && isAuthenticated) {
    return '/dashboard'
  }

  return null
}
