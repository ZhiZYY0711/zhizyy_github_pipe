import Vue from 'vue'
import App from './App.vue'
import store from './store'
import router from './router'
import request from '@shared/utils/request'
import Toasted from 'vue-toasted'

// 将请求工具挂载到Vue原型上
Vue.prototype.$http = request

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
