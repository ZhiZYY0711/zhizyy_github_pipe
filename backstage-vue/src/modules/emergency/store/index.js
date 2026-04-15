import {
  getAccidentList,
  createAccidentReport,
  updateAccidentStatus,
  getVirtualExpertAdvice,
  getKnowledgeBase,
  getDrillList,
  createDrill,
  startDrill,
  endDrill,
  getEmergencyPlans,
  getEmergencyPlanById
} from '../api'

const state = {
  // 事故响应
  accidents: {
    list: [],
    total: 0,
    currentPage: 1,
    pageSize: 10,
    currentAccident: null
  },
  // 虚拟专家
  virtualExpert: {
    advice: null,
    knowledgeBase: [],
    currentQuery: ''
  },
  // 模拟演练
  drills: {
    list: [],
    total: 0,
    currentPage: 1,
    pageSize: 10,
    currentDrill: null,
    activeDrill: null
  },
  // 应急预案
  emergencyPlans: {
    list: [],
    currentPlan: null
  },
  // 加载状态
  loading: {
    accidents: false,
    expert: false,
    drills: false,
    plans: false
  },
  // 实时状态
  realTimeStatus: {
    activeAlerts: 0,
    ongoingDrills: 0,
    emergencyLevel: 'normal' // normal, warning, critical, emergency
  }
}

const mutations = {
  SET_ACCIDENT_LIST(state, { list, total }) {
    state.accidents.list = list
    state.accidents.total = total
  },
  SET_ACCIDENT_PAGE(state, { currentPage, pageSize }) {
    state.accidents.currentPage = currentPage
    if (pageSize) state.accidents.pageSize = pageSize
  },
  SET_CURRENT_ACCIDENT(state, accident) {
    state.accidents.currentAccident = accident
  },
  ADD_ACCIDENT(state, accident) {
    state.accidents.list.unshift(accident)
    state.accidents.total += 1
  },
  UPDATE_ACCIDENT(state, updatedAccident) {
    const index = state.accidents.list.findIndex(item => item.id === updatedAccident.id)
    if (index !== -1) {
      state.accidents.list.splice(index, 1, updatedAccident)
    }
    if (state.accidents.currentAccident && state.accidents.currentAccident.id === updatedAccident.id) {
      state.accidents.currentAccident = updatedAccident
    }
  },

  // 虚拟专家
  SET_EXPERT_ADVICE(state, advice) {
    state.virtualExpert.advice = advice
  },
  SET_KNOWLEDGE_BASE(state, knowledgeBase) {
    state.virtualExpert.knowledgeBase = knowledgeBase
  },
  SET_CURRENT_QUERY(state, query) {
    state.virtualExpert.currentQuery = query
  },

  // 模拟演练
  SET_DRILL_LIST(state, { list, total }) {
    state.drills.list = list
    state.drills.total = total
  },
  SET_DRILL_PAGE(state, { currentPage, pageSize }) {
    state.drills.currentPage = currentPage
    if (pageSize) state.drills.pageSize = pageSize
  },
  SET_CURRENT_DRILL(state, drill) {
    state.drills.currentDrill = drill
  },
  SET_ACTIVE_DRILL(state, drill) {
    state.drills.activeDrill = drill
  },
  ADD_DRILL(state, drill) {
    state.drills.list.unshift(drill)
    state.drills.total += 1
  },
  UPDATE_DRILL(state, updatedDrill) {
    const index = state.drills.list.findIndex(item => item.id === updatedDrill.id)
    if (index !== -1) {
      state.drills.list.splice(index, 1, updatedDrill)
    }
    if (state.drills.currentDrill && state.drills.currentDrill.id === updatedDrill.id) {
      state.drills.currentDrill = updatedDrill
    }
    if (state.drills.activeDrill && state.drills.activeDrill.id === updatedDrill.id) {
      state.drills.activeDrill = updatedDrill
    }
  },

  // 应急预案
  SET_EMERGENCY_PLANS(state, plans) {
    state.emergencyPlans.list = plans
  },
  SET_CURRENT_PLAN(state, plan) {
    state.emergencyPlans.currentPlan = plan
  },

  // 加载状态
  SET_LOADING(state, { type, loading }) {
    state.loading[type] = loading
  },

  // 实时状态
  SET_REAL_TIME_STATUS(state, status) {
    state.realTimeStatus = { ...state.realTimeStatus, ...status }
  },
  UPDATE_EMERGENCY_LEVEL(state, level) {
    state.realTimeStatus.emergencyLevel = level
  }
}

const actions = {
  // 事故响应操作
  async fetchAccidentList({ commit, state }, params = {}) {
    commit('SET_LOADING', { type: 'accidents', loading: true })
    try {
      const queryParams = {
        page: state.accidents.currentPage,
        pageSize: state.accidents.pageSize,
        ...params
      }
      const data = await getAccidentList(queryParams)
      commit('SET_ACCIDENT_LIST', data)
      return data
    } catch (error) {
      console.error('获取事故列表失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'accidents', loading: false })
    }
  },

  async createAccident({ commit }, accidentData) {
    try {
      const accident = await createAccidentReport(accidentData)
      commit('ADD_ACCIDENT', accident)
      // 更新实时状态
      commit('SET_REAL_TIME_STATUS', { activeAlerts: state.realTimeStatus.activeAlerts + 1 })
      return accident
    } catch (error) {
      console.error('创建事故报告失败:', error)
      throw error
    }
  },

  async updateAccidentStatus({ commit }, { id, status }) {
    try {
      const accident = await updateAccidentStatus(id, status)
      commit('UPDATE_ACCIDENT', accident)
      return accident
    } catch (error) {
      console.error('更新事故状态失败:', error)
      throw error
    }
  },

  // 虚拟专家操作
  async getExpertAdvice({ commit }, query) {
    commit('SET_LOADING', { type: 'expert', loading: true })
    commit('SET_CURRENT_QUERY', query)
    try {
      const advice = await getVirtualExpertAdvice(query)
      commit('SET_EXPERT_ADVICE', advice)
      return advice
    } catch (error) {
      console.error('获取专家建议失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'expert', loading: false })
    }
  },

  async fetchKnowledgeBase({ commit }) {
    try {
      const knowledgeBase = await getKnowledgeBase()
      commit('SET_KNOWLEDGE_BASE', knowledgeBase)
      return knowledgeBase
    } catch (error) {
      console.error('获取知识库失败:', error)
      throw error
    }
  },

  // 模拟演练操作
  async fetchDrillList({ commit, state }, params = {}) {
    commit('SET_LOADING', { type: 'drills', loading: true })
    try {
      const queryParams = {
        page: state.drills.currentPage,
        pageSize: state.drills.pageSize,
        ...params
      }
      const data = await getDrillList(queryParams)
      commit('SET_DRILL_LIST', data)
      return data
    } catch (error) {
      console.error('获取演练列表失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'drills', loading: false })
    }
  },

  async createDrill({ commit }, drillData) {
    try {
      const drill = await createDrill(drillData)
      commit('ADD_DRILL', drill)
      return drill
    } catch (error) {
      console.error('创建演练失败:', error)
      throw error
    }
  },

  async startDrill({ commit }, drillId) {
    try {
      const drill = await startDrill(drillId)
      commit('UPDATE_DRILL', drill)
      commit('SET_ACTIVE_DRILL', drill)
      // 更新实时状态
      commit('SET_REAL_TIME_STATUS', { ongoingDrills: state.realTimeStatus.ongoingDrills + 1 })
      return drill
    } catch (error) {
      console.error('开始演练失败:', error)
      throw error
    }
  },

  async endDrill({ commit }, drillId) {
    try {
      const drill = await endDrill(drillId)
      commit('UPDATE_DRILL', drill)
      if (state.drills.activeDrill && state.drills.activeDrill.id === drillId) {
        commit('SET_ACTIVE_DRILL', null)
      }
      // 更新实时状态
      commit('SET_REAL_TIME_STATUS', { ongoingDrills: Math.max(0, state.realTimeStatus.ongoingDrills - 1) })
      return drill
    } catch (error) {
      console.error('结束演练失败:', error)
      throw error
    }
  },

  // 应急预案操作
  async fetchEmergencyPlans({ commit }) {
    commit('SET_LOADING', { type: 'plans', loading: true })
    try {
      const plans = await getEmergencyPlans()
      commit('SET_EMERGENCY_PLANS', plans)
      return plans
    } catch (error) {
      console.error('获取应急预案失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'plans', loading: false })
    }
  },

  async fetchEmergencyPlan({ commit }, planId) {
    commit('SET_LOADING', { type: 'plans', loading: true })
    try {
      const plan = await getEmergencyPlanById(planId)
      commit('SET_CURRENT_PLAN', plan)
      return plan
    } catch (error) {
      console.error('获取应急预案详情失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'plans', loading: false })
    }
  },

  // 应急级别评估
  assessEmergencyLevel({ commit }, { accidents, sensorData }) {
    let level = 'normal'

    // 根据事故数量和严重程度评估
    const criticalAccidents = accidents.filter(acc => acc.severity === 'critical').length
    const warningAccidents = accidents.filter(acc => acc.severity === 'warning').length

    if (criticalAccidents > 0) {
      level = 'emergency'
    } else if (warningAccidents > 2) {
      level = 'critical'
    } else if (warningAccidents > 0) {
      level = 'warning'
    }

    commit('UPDATE_EMERGENCY_LEVEL', level)
    return level
  }
}

const getters = {
  // 事故响应
  accidentList: state => state.accidents.list,
  accidentTotal: state => state.accidents.total,
  currentAccident: state => state.accidents.currentAccident,
  accidentPagination: state => ({
    currentPage: state.accidents.currentPage,
    pageSize: state.accidents.pageSize,
    total: state.accidents.total
  }),
  // 按严重程度分组的事故
  accidentsBySeverity: state => {
    const groups = {}
    state.accidents.list.forEach(accident => {
      if (!groups[accident.severity]) {
        groups[accident.severity] = []
      }
      groups[accident.severity].push(accident)
    })
    return groups
  },

  // 虚拟专家
  expertAdvice: state => state.virtualExpert.advice,
  knowledgeBase: state => state.virtualExpert.knowledgeBase,
  currentQuery: state => state.virtualExpert.currentQuery,

  // 模拟演练
  drillList: state => state.drills.list,
  drillTotal: state => state.drills.total,
  currentDrill: state => state.drills.currentDrill,
  activeDrill: state => state.drills.activeDrill,
  drillPagination: state => ({
    currentPage: state.drills.currentPage,
    pageSize: state.drills.pageSize,
    total: state.drills.total
  }),

  // 应急预案
  emergencyPlans: state => state.emergencyPlans.list,
  currentPlan: state => state.emergencyPlans.currentPlan,

  // 加载状态
  isLoading: state => type => state.loading[type] || false,

  // 实时状态
  realTimeStatus: state => state.realTimeStatus,
  emergencyLevel: state => state.realTimeStatus.emergencyLevel,
  isEmergencyActive: state => state.realTimeStatus.emergencyLevel !== 'normal',

  // 统计信息
  emergencyStats: state => ({
    totalAccidents: state.accidents.total,
    activeAlerts: state.realTimeStatus.activeAlerts,
    ongoingDrills: state.realTimeStatus.ongoingDrills,
    emergencyLevel: state.realTimeStatus.emergencyLevel
  })
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}