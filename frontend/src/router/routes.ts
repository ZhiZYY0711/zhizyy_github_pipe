import { hasAuthSession } from '../modules/auth/storage'
import LoginPage from '../modules/auth/views/LoginPage.vue'
import { DashboardPage } from '../modules/dashboard'
import type { AppRouteRecord } from './types'

export const routes: AppRouteRecord[] = [
  {
    path: '/',
    name: 'root',
    redirect: () => (hasAuthSession() ? '/dashboard' : '/login'),
  },
  {
    path: '/login',
    name: 'login',
    component: LoginPage,
    meta: {
      guestOnly: true,
    },
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: DashboardPage,
    meta: {
      requiresAuth: true,
    },
  },
]
