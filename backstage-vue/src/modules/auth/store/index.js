// 认证模块状态管理
import { login, logout, getUserInfo } from '../api'

const state = {
  token: localStorage.getItem('token') || localStorage.getItem('jwt') || '',
  userInfo: null,
  isAuthenticated: false
}

const mutations = {
  SET_TOKEN(state, token) {
    state.token = token
    localStorage.setItem('token', token)
  },
  
  CLEAR_TOKEN(state) {
    state.token = ''
    localStorage.removeItem('token')
    localStorage.removeItem('jwt')
  },
  
  SET_USER_INFO(state, userInfo) {
    state.userInfo = userInfo
    state.isAuthenticated = true
  },
  
  CLEAR_USER_INFO(state) {
    state.userInfo = null
    state.isAuthenticated = false
  }
}

const actions = {
  // 登录
  async login({ commit }, loginForm) {
    try {
      const response = await login(loginForm)
      const { token, jwt, userInfo } = response.data || {}
      const finalToken = token || jwt
      
      if (!finalToken) {
        throw new Error('登录响应缺少 token/jwt 字段')
      }

      commit('SET_TOKEN', finalToken)
      commit('SET_USER_INFO', userInfo)
      
      return response
    } catch (error) {
      throw error
    }
  },
  
  // 登出
  async logout({ commit }) {
    try {
      await logout()
    } catch (error) {
      console.error('Logout error:', error)
    } finally {
      commit('CLEAR_TOKEN')
      commit('CLEAR_USER_INFO')
      localStorage.removeItem('isLoggedIn')
      localStorage.removeItem('username')
    }
  },
  
  // 获取用户信息
  async getUserInfo({ commit }) {
    try {
      const response = await getUserInfo()
      commit('SET_USER_INFO', response.data)
      return response.data
    } catch (error) {
      commit('CLEAR_TOKEN')
      commit('CLEAR_USER_INFO')
      localStorage.removeItem('isLoggedIn')
      localStorage.removeItem('username')
      throw error
    }
  }
}

const getters = {
  isAuthenticated: state => state.isAuthenticated,
  userInfo: state => state.userInfo,
  token: state => state.token
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}