import { hasAuthSession } from '../modules/auth/storage'
import { defineAsyncComponent } from 'vue'
import type { AppRouteRecord } from './types'

const LoginPage = defineAsyncComponent(() => import('../modules/auth/views/LoginPage.vue'))
const DashboardPage = defineAsyncComponent(() => import('../modules/dashboard/DashboardPage.vue'))
const MonitoringPage = defineAsyncComponent(() => import('../modules/monitoring/views/MonitoringPage.vue'))
const MonitoringDataPage = defineAsyncComponent(() => import('../modules/monitoring/views/MonitoringDataPage.vue'))
const AssetsPage = defineAsyncComponent(() => import('../modules/assets/views/AssetsPage.vue'))
const EquipmentPage = defineAsyncComponent(() => import('../modules/assets/views/EquipmentPage.vue'))
const RepairmenPage = defineAsyncComponent(() => import('../modules/assets/views/RepairmenPage.vue'))
const TasksPage = defineAsyncComponent(() => import('../modules/tasks/views/TasksPage.vue'))
const AllTasksPage = defineAsyncComponent(() => import('../modules/tasks/views/AllTasksPage.vue'))
const EmergencyPage = defineAsyncComponent(() => import('../modules/emergency/views/EmergencyPage.vue'))
const LogsPage = defineAsyncComponent(() => import('../modules/logs/views/LogsPage.vue'))
const VirtualExpertPage = defineAsyncComponent(() => import('../modules/virtual-expert/views/VirtualExpertPage.vue'))
const VirtualExpertSharedPage = defineAsyncComponent(() => import('../modules/virtual-expert/views/VirtualExpertSharedPage.vue'))

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
  {
    path: '/monitoring',
    name: 'monitoring',
    component: MonitoringPage,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/monitoring/data',
    name: 'monitoring-data',
    component: MonitoringDataPage,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/assets',
    name: 'assets',
    component: AssetsPage,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/assets/equipment',
    name: 'assets-equipment',
    component: EquipmentPage,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/assets/repairmen',
    name: 'assets-repairmen',
    component: RepairmenPage,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/tasks',
    name: 'tasks',
    component: TasksPage,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/tasks/all',
    name: 'tasks-all',
    component: AllTasksPage,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/emergency',
    name: 'emergency',
    component: EmergencyPage,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/logs',
    name: 'logs',
    component: LogsPage,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/virtual-expert/share/:shareId',
    name: 'virtual-expert-share',
    component: VirtualExpertSharedPage,
  },
  {
    path: '/virtual-expert/:sessionId',
    name: 'virtual-expert-session',
    component: VirtualExpertPage,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/virtual-expert',
    name: 'virtual-expert',
    component: VirtualExpertPage,
    meta: {
      requiresAuth: true,
    },
  },
]
