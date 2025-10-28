import { getDashboardOverview, getMapData, getFlowTrendData, getChartData } from '../api'

const state = {
  // 仪表板概览数据
  overviewData: {
    totalPipelines: 0,
    activePipelines: 0,
    totalDevices: 0,
    onlineDevices: 0,
    todayAlerts: 0,
    totalFlow: 0
  },
  // 地图数据
  mapData: {
    areas: [],
    pipelines: [],
    devices: []
  },
  // 流量趋势数据
  flowTrendData: [],
  // 图表数据
  chartData: {
    pieData: [],
    barData: [],
    lineData: []
  },
  // 加载状态
  loading: {
    overview: false,
    map: false,
    flowTrend: false,
    chart: false
  }
}

const mutations = {
  SET_OVERVIEW_DATA(state, data) {
    state.overviewData = { ...state.overviewData, ...data }
  },
  SET_MAP_DATA(state, data) {
    state.mapData = { ...state.mapData, ...data }
  },
  SET_FLOW_TREND_DATA(state, data) {
    state.flowTrendData = data
  },
  SET_CHART_DATA(state, data) {
    state.chartData = { ...state.chartData, ...data }
  },
  SET_LOADING(state, { type, loading }) {
    state.loading[type] = loading
  }
}

const actions = {
  // 获取仪表板概览数据
  async fetchOverviewData({ commit }) {
    commit('SET_LOADING', { type: 'overview', loading: true })
    try {
      const data = await getDashboardOverview()
      commit('SET_OVERVIEW_DATA', data)
      return data
    } catch (error) {
      console.error('获取概览数据失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'overview', loading: false })
    }
  },

  // 获取地图数据
  async fetchMapData({ commit }) {
    commit('SET_LOADING', { type: 'map', loading: true })
    try {
      const data = await getMapData()
      commit('SET_MAP_DATA', data)
      return data
    } catch (error) {
      console.error('获取地图数据失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'map', loading: false })
    }
  },

  // 获取流量趋势数据
  async fetchFlowTrendData({ commit }, params) {
    commit('SET_LOADING', { type: 'flowTrend', loading: true })
    try {
      const data = await getFlowTrendData(params)
      commit('SET_FLOW_TREND_DATA', data)
      return data
    } catch (error) {
      console.error('获取流量趋势数据失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'flowTrend', loading: false })
    }
  },

  // 获取图表数据
  async fetchChartData({ commit }, params) {
    commit('SET_LOADING', { type: 'chart', loading: true })
    try {
      const data = await getChartData(params)
      commit('SET_CHART_DATA', data)
      return data
    } catch (error) {
      console.error('获取图表数据失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'chart', loading: false })
    }
  }
}

const getters = {
  // 获取概览数据
  overviewData: state => state.overviewData,
  // 获取地图数据
  mapData: state => state.mapData,
  // 获取流量趋势数据
  flowTrendData: state => state.flowTrendData,
  // 获取图表数据
  chartData: state => state.chartData,
  // 获取加载状态
  isLoading: state => type => state.loading[type] || false,
  // 获取在线设备比例
  onlineDeviceRate: state => {
    const { totalDevices, onlineDevices } = state.overviewData
    return totalDevices > 0 ? ((onlineDevices / totalDevices) * 100).toFixed(1) : 0
  },
  // 获取活跃管道比例
  activePipelineRate: state => {
    const { totalPipelines, activePipelines } = state.overviewData
    return totalPipelines > 0 ? ((activePipelines / totalPipelines) * 100).toFixed(1) : 0
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}