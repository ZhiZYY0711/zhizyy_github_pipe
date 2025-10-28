import Vue from 'vue'
import App from './App.vue'
import store from './store'
import router from './router'
import axios from 'axios'
import Toasted from 'vue-toasted'

// 配置axios
axios.defaults.baseURL = 'http://localhost:8080' // 根据实际后端地址调整
axios.defaults.timeout = 10000

// 添加请求拦截器
axios.interceptors.request.use(
  config => {
    const token = localStorage.getItem('jwt')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 添加响应拦截器
axios.interceptors.response.use(
  response => {
    return response
  },
  error => {
    if (error.response && error.response.status === 401) {
      // token过期或无效，清除本地存储并跳转到登录页
      localStorage.removeItem('jwt')
      localStorage.removeItem('isLoggedIn')
      localStorage.removeItem('username')
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

Vue.prototype.$http = axios

// 配置vue-toasted
Vue.use(Toasted, {
  position: 'top-right',
  duration: 3000,
  theme: 'outline'
})

// 添加全局消息方法
Vue.prototype.$message = {
  success: (message) => Vue.toasted.success(message),
  error: (message) => Vue.toasted.error(message),
  info: (message) => Vue.toasted.info(message)
}

Vue.config.productionTip = false

new Vue({
  router,
  store,
  render: h => h(App),
}).$mount('#app')
