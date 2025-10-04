import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    // 用户信息
    userInfo: null,
    // 是否已登录
    isLoggedIn: false,
    // 管理员类型
    adminType: ''
  },
  mutations: {
    // 设置用户信息
    SET_USER_INFO(state, userInfo) {
      state.userInfo = userInfo
    },
    // 设置登录状态
    SET_LOGIN_STATUS(state, status) {
      state.isLoggedIn = status
    },
    // 设置管理员类型
    SET_ADMIN_TYPE(state, type) {
      state.adminType = type
    },
    // 清除用户信息（登出时使用）
    CLEAR_USER_INFO(state) {
      state.userInfo = null
      state.isLoggedIn = false
      state.adminType = ''
    }
  },
  actions: {
    // 登录操作
    login({ commit }, loginData) {
      return new Promise((resolve, reject) => {
        // 这里将来会使用axios发送请求
        // 模拟登录成功
        setTimeout(() => {
          const userInfo = {
            id: 1,
            username: loginData.username,
            adminType: loginData.adminType
          }
          commit('SET_USER_INFO', userInfo)
          commit('SET_LOGIN_STATUS', true)
          commit('SET_ADMIN_TYPE', loginData.adminType)
          resolve(userInfo)
        }, 1000)
      })
    },
    // 登出操作
    logout({ commit }) {
      return new Promise((resolve) => {
        // 这里将来会使用axios发送请求
        commit('CLEAR_USER_INFO')
        resolve()
      })
    }
  },
  getters: {
    // 获取用户信息
    userInfo: state => state.userInfo,
    // 获取登录状态
    isLoggedIn: state => state.isLoggedIn,
    // 获取管理员类型
    adminType: state => state.adminType
  }
})