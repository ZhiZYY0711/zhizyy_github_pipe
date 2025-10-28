import Vue from 'vue'
import VueRouter from 'vue-router'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'Login',
    component: () => import('../modules/auth/views/LoginPage.vue')
  },
  {
    path: '/main',
    component: () => import('../modules/shared/components/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: 'visualization'
      },
      // 仪表板模块
      {
        path: 'visualization',
        name: 'DataVisualization',
        component: () => import('../modules/dashboard/views/DataVisualization.vue')
      },
      {
        path: 'area-details',
        name: 'AreaDetails',
        component: () => import('../modules/dashboard/views/AreaDetails.vue')
      },
      {
        path: 'pipeline-details',
        name: 'PipelineDetails',
        component: () => import('../modules/dashboard/views/PipelineDetails.vue')
      },
      // 监控模块
      {
        path: 'data-monitoring',
        name: 'DataMonitoring',
        component: () => import('../modules/monitoring/views/DataMonitoring.vue')
      },
      {
        path: 'monitoring',
        name: 'Monitoring',
        component: () => import('../modules/monitoring/views/Monitoring.vue')
      },
      // 管理模块
      {
        path: 'equipment',
        name: 'Equipment',
        component: () => import('../modules/management/views/Equipment.vue')
      },
      {
        path: 'tasks',
        name: 'Tasks',
        component: () => import('../modules/management/views/Tasks.vue')
      },
      {
        path: 'task-details',
        name: 'TaskDetails',
        component: () => import('../modules/management/views/TaskDetails.vue')
      },
      {
        path: 'repairman',
        name: 'Repairman',
        component: () => import('../modules/management/views/Repairman.vue')
      },
      // 应急模块
      {
        path: 'virtual-expert',
        name: 'VirtualExpert',
        component: () => import('../modules/emergency/views/VirtualExpert.vue')
      },
      {
        path: 'simulation-drill',
        name: 'SimulationDrill',
        component: () => import('../modules/emergency/views/SimulationDrill.vue')
      },
      {
        path: 'emergency',
        name: 'Emergency',
        component: () => import('../modules/emergency/views/AccidentResponse.vue')
      },
      // 日志模块
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('../modules/logs/views/LogRecording.vue')
      }
    ]
  },
  // 保持向后兼容的路由
  {
    path: '/visualization',
    redirect: '/main/visualization'
  },
  {
    path: '/simulation-drill',
    redirect: '/main/simulation-drill'
  }
]

const router = new VueRouter({
  mode: 'hash',
  base: process.env.BASE_URL,
  routes
})

// 路由守卫，检查用户是否已登录
router.beforeEach((to, from, next) => {
  const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true'

  if (to.matched.some(record => record.meta.requiresAuth)) {
    // 需要登录的页面
    if (!isLoggedIn) {
      next({ name: 'Login' })
    } else {
      next()
    }
  } else {
    // 不需要登录的页面
    if (isLoggedIn && to.name === 'Login') {
      next('/main/visualization')
    } else {
      next()
    }
  }
})

export default router