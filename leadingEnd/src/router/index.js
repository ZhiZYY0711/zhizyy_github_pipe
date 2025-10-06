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
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/visualization',
    name: 'DataVisualization',
    component: () => import('../views/DataVisualization.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/area-details',
    name: 'AreaDetails',
    component: () => import('../views/AreaDetails.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/pipeline-details',
    name: 'PipelineDetails',
    component: () => import('../views/PipelineDetails.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/task-details',
    name: 'TaskDetails',
    component: () => import('../views/TaskDetails.vue'),
    meta: { requiresAuth: true }
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
      next({ name: 'Dashboard' })
    } else {
      next()
    }
  }
})

export default router