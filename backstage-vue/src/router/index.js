import Vue from 'vue'
import VueRouter from 'vue-router'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'Login',
    component: () => import('../views/login/LoginPage.vue')
  },
  {
    path: '/main',
    component: () => import('../views/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: 'visualization'
      },

      {
        path: 'visualization',
        name: 'DataVisualization',
        component: () => import('../views/data-visualization/DataVisualization.vue')
      },
      {
        path: 'data-monitoring',
        name: 'DataMonitoring',
        component: () => import('../views/data-visualization/DataMonitoring.vue')
      },
      {
        path: 'area-details',
        name: 'AreaDetails',
        component: () => import('../views/data-visualization/details/AreaDetails.vue')
      },
      {
        path: 'pipeline-details',
        name: 'PipelineDetails',
        component: () => import('../views/data-visualization/details/PipelineDetails.vue')
      },
      {
        path: 'task-details',
        name: 'TaskDetails',
        component: () => import('../views/data-visualization/TaskDetails.vue')
      },
      {
        path: 'monitoring',
        name: 'Monitoring',
        component: () => import('../views/data-management/Monitoring.vue')
      },
      {
        path: 'equipment',
        name: 'Equipment',
        component: () => import('../views/data-management/Equipment.vue')
      },
      {
        path: 'tasks',
        name: 'Tasks',
        component: () => import('../views/data-management/Tasks.vue')
      },
      {
        path: 'repairman',
        name: 'Repairman',
        component: () => import('../views/data-management/Repairman.vue')
      },
      {
        path: 'virtual-expert',
        name: 'VirtualExpert',
        component: () => import('../views/virtual-expert/VirtualExpert.vue')
      },
      {
        path: 'simulation-drill',
        name: 'SimulationDrill',
        component: () => import('../views/simulation-drill/SimulationDrill.vue')
      },
      {
        path: 'emergency',
        name: 'Emergency',
        component: () => import('../views/emergency/AccidentResponse.vue')
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('../views/logs/LogRecording.vue')
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