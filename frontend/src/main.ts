import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { installClientFileLogger } from './modules/shared/clientLog'
import './styles/tokens.css'
import './styles/base.css'
import './styles/business.css'

installClientFileLogger()
createApp(App).use(router).mount('#app')
