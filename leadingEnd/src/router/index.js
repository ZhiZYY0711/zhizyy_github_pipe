import Vue from 'vue'
import VueRouter from 'vue-router'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'Login',
    component: () => import('../components/LoginPage.vue')
  },
  {
    path: '/main',
    component: () => import('../views/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: 'dashboard'
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue')
      },
      {
        path: 'visualization',
        name: 'DataVisualization',
        component: () => import('../views/DataVisualization.vue')
      },
      {
        path: 'data-monitoring',
        name: 'DataMonitoring',
        component: () => import('../views/DataMonitoring.vue')
      },
      {
        path: 'area-details',
        name: 'AreaDetails',
        component: () => import('../views/AreaDetails.vue')
      },
      {
        path: 'pipeline-details',
        name: 'PipelineDetails',
        component: () => import('../views/PipelineDetails.vue')
      },
      {
        path: 'task-details',
        name: 'TaskDetails',
        component: () => import('../views/TaskDetails.vue')
      },
      {
        path: 'monitoring',
        name: 'Monitoring',
        component: () => import('../views/Monitoring.vue')
      },
      {
        path: 'equipment',
        name: 'Equipment',
        component: () => import('../views/Equipment.vue')
      },
      {
        path: 'tasks',
        name: 'Tasks',
        component: () => import('../views/Tasks.vue')
      },
      {
        path: 'maintenance',
        name: 'Maintenance',
        component: () => import('../views/Maintenance.vue')
      },
      {
        path: 'virtual-expert',
        name: 'VirtualExpert',
        component: () => import('../views/VirtualExpert.vue')
      },
      {
        path: 'simulation-drill',
        name: 'SimulationDrill',
        component: () => import('../views/SimulationDrill.vue')
      },
      {
        path: 'emergency',
        name: 'Emergency',
        component: () => import('../views/AccidentResponse.vue')
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('../views/LogRecording.vue')
      }
    ]
  },
  // 保持向后兼容的路由
  {
    path: '/dashboard',
    redirect: '/main/dashboard'
  },
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
  mode: 'history',
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
      next('/main/dashboard')
    } else {
      next()
    }
  }
})

export default router