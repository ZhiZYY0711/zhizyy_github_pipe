import { 
  getLogList, 
  getOperationLogs, 
  getSystemLogs, 
  getInspectionLogs,
  createInspectionLog,
  exportLogs,
  deleteLogs,
  getLogStatistics
} from '../api'

const state = {
  // 日志列表
  logs: {
    list: [],
    total: 0,
    currentPage: 1,
    pageSize: 20
  },
  // 操作日志
  operationLogs: {
    list: [],
    total: 0,
    currentPage: 1,
    pageSize: 20
  },
  // 系统日志
  systemLogs: {
    list: [],
    total: 0,
    currentPage: 1,
    pageSize: 20
  },
  // 巡检日志
  inspectionLogs: {
    list: [],
    total: 0,
    currentPage: 1,
    pageSize: 20
  },
  // 日志统计
  statistics: {
    totalLogs: 0,
    todayLogs: 0,
    errorLogs: 0,
    warningLogs: 0,
    logsByType: {},
    logsByLevel: {},
    recentActivity: []
  },
  // 筛选条件
  filters: {
    dateRange: [],
    logType: '',
    logLevel: '',
    keyword: '',
    userId: ''
  },
  // 加载状态
  loading: {
    logs: false,
    operation: false,
    system: false,
    inspection: false,
    statistics: false,
    export: false
  },
  // 导出状态
  exportStatus: {
    isExporting: false,
    progress: 0,
    downloadUrl: ''
  }
}

const mutations = {
  // 通用日志列表
  SET_LOG_LIST(state, { list, total }) {
    state.logs.list = list
    state.logs.total = total
  },
  SET_LOG_PAGE(state, { currentPage, pageSize }) {
    state.logs.currentPage = currentPage
    if (pageSize) state.logs.pageSize = pageSize
  },

  // 操作日志
  SET_OPERATION_LOGS(state, { list, total }) {
    state.operationLogs.list = list
    state.operationLogs.total = total
  },
  SET_OPERATION_LOG_PAGE(state, { currentPage, pageSize }) {
    state.operationLogs.currentPage = currentPage
    if (pageSize) state.operationLogs.pageSize = pageSize
  },

  // 系统日志
  SET_SYSTEM_LOGS(state, { list, total }) {
    state.systemLogs.list = list
    state.systemLogs.total = total
  },
  SET_SYSTEM_LOG_PAGE(state, { currentPage, pageSize }) {
    state.systemLogs.currentPage = currentPage
    if (pageSize) state.systemLogs.pageSize = pageSize
  },

  // 巡检日志
  SET_INSPECTION_LOGS(state, { list, total }) {
    state.inspectionLogs.list = list
    state.inspectionLogs.total = total
  },
  SET_INSPECTION_LOG_PAGE(state, { currentPage, pageSize }) {
    state.inspectionLogs.currentPage = currentPage
    if (pageSize) state.inspectionLogs.pageSize = pageSize
  },
  ADD_INSPECTION_LOG(state, log) {
    state.inspectionLogs.list.unshift(log)
    state.inspectionLogs.total += 1
  },

  // 日志统计
  SET_STATISTICS(state, statistics) {
    state.statistics = { ...state.statistics, ...statistics }
  },

  // 筛选条件
  SET_FILTERS(state, filters) {
    state.filters = { ...state.filters, ...filters }
  },
  CLEAR_FILTERS(state) {
    state.filters = {
      dateRange: [],
      logType: '',
      logLevel: '',
      keyword: '',
      userId: ''
    }
  },

  // 加载状态
  SET_LOADING(state, { type, loading }) {
    state.loading[type] = loading
  },

  // 导出状态
  SET_EXPORT_STATUS(state, status) {
    state.exportStatus = { ...state.exportStatus, ...status }
  },

  // 实时日志更新
  ADD_REAL_TIME_LOG(state, log) {
    // 根据日志类型添加到相应列表
    switch (log.type) {
      case 'operation':
        state.operationLogs.list.unshift(log)
        if (state.operationLogs.list.length > state.operationLogs.pageSize) {
          state.operationLogs.list.pop()
        }
        break
      case 'system':
        state.systemLogs.list.unshift(log)
        if (state.systemLogs.list.length > state.systemLogs.pageSize) {
          state.systemLogs.list.pop()
        }
        break
      case 'inspection':
        state.inspectionLogs.list.unshift(log)
        if (state.inspectionLogs.list.length > state.inspectionLogs.pageSize) {
          state.inspectionLogs.list.pop()
        }
        break
    }
    
    // 更新统计信息
    state.statistics.totalLogs += 1
    state.statistics.todayLogs += 1
    
    if (log.level === 'error') {
      state.statistics.errorLogs += 1
    } else if (log.level === 'warning') {
      state.statistics.warningLogs += 1
    }
  }
}

const actions = {
  // 获取日志列表
  async fetchLogList({ commit, state }, params = {}) {
    commit('SET_LOADING', { type: 'logs', loading: true })
    try {
      const queryParams = {
        page: state.logs.currentPage,
        pageSize: state.logs.pageSize,
        ...state.filters,
        ...params
      }
      const data = await getLogList(queryParams)
      commit('SET_LOG_LIST', data)
      return data
    } catch (error) {
      console.error('获取日志列表失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'logs', loading: false })
    }
  },

  // 获取操作日志
  async fetchOperationLogs({ commit, state }, params = {}) {
    commit('SET_LOADING', { type: 'operation', loading: true })
    try {
      const queryParams = {
        page: state.operationLogs.currentPage,
        pageSize: state.operationLogs.pageSize,
        ...state.filters,
        ...params
      }
      const data = await getOperationLogs(queryParams)
      commit('SET_OPERATION_LOGS', data)
      return data
    } catch (error) {
      console.error('获取操作日志失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'operation', loading: false })
    }
  },

  // 获取系统日志
  async fetchSystemLogs({ commit, state }, params = {}) {
    commit('SET_LOADING', { type: 'system', loading: true })
    try {
      const queryParams = {
        page: state.systemLogs.currentPage,
        pageSize: state.systemLogs.pageSize,
        ...state.filters,
        ...params
      }
      const data = await getSystemLogs(queryParams)
      commit('SET_SYSTEM_LOGS', data)
      return data
    } catch (error) {
      console.error('获取系统日志失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'system', loading: false })
    }
  },

  // 获取巡检日志
  async fetchInspectionLogs({ commit, state }, params = {}) {
    commit('SET_LOADING', { type: 'inspection', loading: true })
    try {
      const queryParams = {
        page: state.inspectionLogs.currentPage,
        pageSize: state.inspectionLogs.pageSize,
        ...state.filters,
        ...params
      }
      const data = await getInspectionLogs(queryParams)
      commit('SET_INSPECTION_LOGS', data)
      return data
    } catch (error) {
      console.error('获取巡检日志失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'inspection', loading: false })
    }
  },

  // 创建巡检日志
  async createInspectionLog({ commit }, logData) {
    try {
      const log = await createInspectionLog(logData)
      commit('ADD_INSPECTION_LOG', log)
      return log
    } catch (error) {
      console.error('创建巡检日志失败:', error)
      throw error
    }
  },

  // 获取日志统计
  async fetchStatistics({ commit }) {
    commit('SET_LOADING', { type: 'statistics', loading: true })
    try {
      const statistics = await getLogStatistics()
      commit('SET_STATISTICS', statistics)
      return statistics
    } catch (error) {
      console.error('获取日志统计失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'statistics', loading: false })
    }
  },

  // 导出日志
  async exportLogs({ commit }, params) {
    commit('SET_EXPORT_STATUS', { isExporting: true, progress: 0 })
    try {
      // 模拟导出进度
      const progressInterval = setInterval(() => {
        commit('SET_EXPORT_STATUS', { 
          progress: Math.min(state.exportStatus.progress + 10, 90) 
        })
      }, 500)

      const result = await exportLogs(params)
      
      clearInterval(progressInterval)
      commit('SET_EXPORT_STATUS', { 
        isExporting: false, 
        progress: 100,
        downloadUrl: result.downloadUrl
      })
      
      return result
    } catch (error) {
      commit('SET_EXPORT_STATUS', { isExporting: false, progress: 0 })
      console.error('导出日志失败:', error)
      throw error
    }
  },

  // 删除日志
  async deleteLogs({ dispatch }, logIds) {
    try {
      await deleteLogs(logIds)
      // 重新获取日志列表
      await dispatch('fetchLogList')
      await dispatch('fetchStatistics')
    } catch (error) {
      console.error('删除日志失败:', error)
      throw error
    }
  },

  // 设置筛选条件
  setFilters({ commit, dispatch }, filters) {
    commit('SET_FILTERS', filters)
    // 重新获取数据
    dispatch('fetchLogList')
  },

  // 清除筛选条件
  clearFilters({ commit, dispatch }) {
    commit('CLEAR_FILTERS')
    dispatch('fetchLogList')
  },

  // 分页操作
  setLogPage({ commit, dispatch }, { currentPage, pageSize }) {
    commit('SET_LOG_PAGE', { currentPage, pageSize })
    dispatch('fetchLogList')
  },

  setOperationLogPage({ commit, dispatch }, { currentPage, pageSize }) {
    commit('SET_OPERATION_LOG_PAGE', { currentPage, pageSize })
    dispatch('fetchOperationLogs')
  },

  setSystemLogPage({ commit, dispatch }, { currentPage, pageSize }) {
    commit('SET_SYSTEM_LOG_PAGE', { currentPage, pageSize })
    dispatch('fetchSystemLogs')
  },

  setInspectionLogPage({ commit, dispatch }, { currentPage, pageSize }) {
    commit('SET_INSPECTION_LOG_PAGE', { currentPage, pageSize })
    dispatch('fetchInspectionLogs')
  },

  // 实时日志监听
  startRealTimeLogMonitoring({ commit }) {
    // 这里可以实现WebSocket连接来接收实时日志
    // 示例：模拟实时日志接收
    setInterval(() => {
      const mockLog = {
        id: Date.now(),
        type: ['operation', 'system', 'inspection'][Math.floor(Math.random() * 3)],
        level: ['info', 'warning', 'error'][Math.floor(Math.random() * 3)],
        message: '模拟实时日志消息',
        timestamp: new Date().toISOString(),
        userId: 'system'
      }
      commit('ADD_REAL_TIME_LOG', mockLog)
    }, 10000) // 每10秒添加一条模拟日志
  }
}

const getters = {
  // 日志列表
  logList: state => state.logs.list,
  logTotal: state => state.logs.total,
  logPagination: state => ({
    currentPage: state.logs.currentPage,
    pageSize: state.logs.pageSize,
    total: state.logs.total
  }),

  // 操作日志
  operationLogList: state => state.operationLogs.list,
  operationLogTotal: state => state.operationLogs.total,
  operationLogPagination: state => ({
    currentPage: state.operationLogs.currentPage,
    pageSize: state.operationLogs.pageSize,
    total: state.operationLogs.total
  }),

  // 系统日志
  systemLogList: state => state.systemLogs.list,
  systemLogTotal: state => state.systemLogs.total,
  systemLogPagination: state => ({
    currentPage: state.systemLogs.currentPage,
    pageSize: state.systemLogs.pageSize,
    total: state.systemLogs.total
  }),

  // 巡检日志
  inspectionLogList: state => state.inspectionLogs.list,
  inspectionLogTotal: state => state.inspectionLogs.total,
  inspectionLogPagination: state => ({
    currentPage: state.inspectionLogs.currentPage,
    pageSize: state.inspectionLogs.pageSize,
    total: state.inspectionLogs.total
  }),

  // 日志统计
  statistics: state => state.statistics,
  
  // 筛选条件
  filters: state => state.filters,
  hasActiveFilters: state => {
    return state.filters.dateRange.length > 0 ||
           state.filters.logType !== '' ||
           state.filters.logLevel !== '' ||
           state.filters.keyword !== '' ||
           state.filters.userId !== ''
  },

  // 加载状态
  isLoading: state => type => state.loading[type] || false,

  // 导出状态
  exportStatus: state => state.exportStatus,

  // 按级别分组的日志
  logsByLevel: state => {
    const groups = {}
    state.logs.list.forEach(log => {
      if (!groups[log.level]) {
        groups[log.level] = []
      }
      groups[log.level].push(log)
    })
    return groups
  },

  // 最近的错误日志
  recentErrorLogs: state => {
    return state.logs.list
      .filter(log => log.level === 'error')
      .slice(0, 10)
  },

  // 今日日志统计
  todayLogStats: state => ({
    total: state.statistics.todayLogs,
    errors: state.statistics.errorLogs,
    warnings: state.statistics.warningLogs,
    info: state.statistics.todayLogs - state.statistics.errorLogs - state.statistics.warningLogs
  })
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}