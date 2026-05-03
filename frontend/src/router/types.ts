import type { Component } from 'vue'

export type AppRouteMeta = {
  requiresAuth?: boolean
  guestOnly?: boolean
}

export type AppRouteRecord = {
  path: string
  name: string
  component?: Component
  meta?: AppRouteMeta
  redirect?: string | (() => string)
}

export type AppRouteLocation = {
  path: string
  name: string
  component: Component
  meta: AppRouteMeta
  params: Record<string, string>
}
