import Vue from 'vue'
import Vuex from 'vuex'

// 导入各模块的状态管理
import auth from '@/modules/auth/store'
import dashboard from '@/modules/dashboard/store'
import monitoring from '@/modules/monitoring/store'
import management from '@/modules/management/store'
import emergency from '@/modules/emergency/store'
import logs from '@/modules/logs/store'

Vue.use(Vuex)

export default new Vuex.Store({
  modules: {
    auth,
    dashboard,
    monitoring,
    management,
    emergency,
    logs
  },
  state: {
    // 全局应用状态
    loading: false,
    sidebarCollapsed: false
  },
  mutations: {
    SET_LOADING(state, loading) {
      state.loading = loading
    },
    TOGGLE_SIDEBAR(state) {
      state.sidebarCollapsed = !state.sidebarCollapsed
    },
    SET_SIDEBAR_COLLAPSED(state, collapsed) {
      state.sidebarCollapsed = collapsed
    }
  },
  actions: {
    // 全局loading控制
    setLoading({ commit }, loading) {
      commit('SET_LOADING', loading)
    },
    // 侧边栏控制
    toggleSidebar({ commit }) {
      commit('TOGGLE_SIDEBAR')
    },
    setSidebarCollapsed({ commit }, collapsed) {
      commit('SET_SIDEBAR_COLLAPSED', collapsed)
    }
  },
  getters: {
    // 全局状态获取器
    loading: state => state.loading,
    sidebarCollapsed: state => state.sidebarCollapsed
  }
})