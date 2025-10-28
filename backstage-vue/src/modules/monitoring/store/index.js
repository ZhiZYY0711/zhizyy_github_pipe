import { 
  getRealtimeData, 
  getDeviceInfo, 
  getSensorData, 
  getSensorLogs, 
  getWeatherData,
  getMonitoringConfig,
  updateMonitoringConfig
} from '../api'

const state = {
  // 实时监测数据
  realTimeData: {
    pressure: [],
    temperature: [],
    flow: [],
    vibration: []
  },
  // 设备信息
  deviceInfo: [],
  // 传感器数据
  sensorData: [],
  // 传感器日志
  sensorLogs: [],
  // 天气数据
  weatherData: null,
  // 监测配置
  monitoringConfig: {
    alertThresholds: {},
    samplingInterval: 60,
    dataRetentionDays: 30
  },
  // 加载状态
  loading: {
    realTime: false,
    device: false,
    sensor: false,
    logs: false,
    weather: false,
    config: false
  },
  // 连接状态
  connectionStatus: {
    websocket: false,
    lastUpdate: null
  }
}

const mutations = {
  SET_REAL_TIME_DATA(state, data) {
    state.realTimeData = { ...state.realTimeData, ...data }
  },
  UPDATE_REAL_TIME_DATA(state, { type, data }) {
    if (state.realTimeData[type]) {
      // 保持最新的100条数据
      state.realTimeData[type] = [...state.realTimeData[type], ...data].slice(-100)
    }
  },
  SET_DEVICE_INFO(state, data) {
    state.deviceInfo = data
  },
  UPDATE_DEVICE_STATUS(state, { deviceId, status }) {
    const device = state.deviceInfo.find(d => d.id === deviceId)
    if (device) {
      device.status = status
      device.lastUpdate = new Date().toISOString()
    }
  },
  SET_SENSOR_DATA(state, data) {
    state.sensorData = data
  },
  SET_SENSOR_LOGS(state, data) {
    state.sensorLogs = data
  },
  ADD_SENSOR_LOG(state, log) {
    state.sensorLogs.unshift(log)
  },
  SET_WEATHER_DATA(state, data) {
    state.weatherData = data
  },
  SET_MONITORING_CONFIG(state, config) {
    state.monitoringConfig = { ...state.monitoringConfig, ...config }
  },
  SET_LOADING(state, { type, loading }) {
    state.loading[type] = loading
  },
  SET_CONNECTION_STATUS(state, { websocket, lastUpdate }) {
    state.connectionStatus.websocket = websocket
    if (lastUpdate) {
      state.connectionStatus.lastUpdate = lastUpdate
    }
  }
}

const actions = {
  // 获取实时数据
  async fetchRealTimeData({ commit }) {
    commit('SET_LOADING', { type: 'realTime', loading: true })
    try {
      const data = await getRealtimeData()
      commit('SET_REAL_TIME_DATA', data)
      commit('SET_CONNECTION_STATUS', { websocket: true, lastUpdate: new Date().toISOString() })
      return data
    } catch (error) {
      console.error('获取实时数据失败:', error)
      commit('SET_CONNECTION_STATUS', { websocket: false })
      throw error
    } finally {
      commit('SET_LOADING', { type: 'realTime', loading: false })
    }
  },

  // 获取设备信息
  async fetchDeviceInfo({ commit }) {
    commit('SET_LOADING', { type: 'device', loading: true })
    try {
      const data = await getDeviceInfo()
      commit('SET_DEVICE_INFO', data)
      return data
    } catch (error) {
      console.error('获取设备信息失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'device', loading: false })
    }
  },

  // 获取传感器数据
  async fetchSensorData({ commit }, params) {
    commit('SET_LOADING', { type: 'sensor', loading: true })
    try {
      const data = await getSensorData(params)
      commit('SET_SENSOR_DATA', data)
      return data
    } catch (error) {
      console.error('获取传感器数据失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'sensor', loading: false })
    }
  },

  // 获取传感器日志
  async fetchSensorLogs({ commit }, params) {
    commit('SET_LOADING', { type: 'logs', loading: true })
    try {
      const data = await getSensorLogs(params)
      commit('SET_SENSOR_LOGS', data)
      return data
    } catch (error) {
      console.error('获取传感器日志失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'logs', loading: false })
    }
  },

  // 获取天气数据
  async fetchWeatherData({ commit }) {
    commit('SET_LOADING', { type: 'weather', loading: true })
    try {
      const data = await getWeatherData()
      commit('SET_WEATHER_DATA', data)
      return data
    } catch (error) {
      console.error('获取天气数据失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'weather', loading: false })
    }
  },

  // 获取监测配置
  async fetchMonitoringConfig({ commit }) {
    commit('SET_LOADING', { type: 'config', loading: true })
    try {
      const config = await getMonitoringConfig()
      commit('SET_MONITORING_CONFIG', config)
      return config
    } catch (error) {
      console.error('获取监测配置失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'config', loading: false })
    }
  },

  // 更新监测配置
  async updateConfig({ commit }, config) {
    commit('SET_LOADING', { type: 'config', loading: true })
    try {
      const updatedConfig = await updateMonitoringConfig(config)
      commit('SET_MONITORING_CONFIG', updatedConfig)
      return updatedConfig
    } catch (error) {
      console.error('更新监测配置失败:', error)
      throw error
    } finally {
      commit('SET_LOADING', { type: 'config', loading: false })
    }
  },

  // WebSocket连接管理
  connectWebSocket({ commit, dispatch }) {
    // 这里实现WebSocket连接逻辑
    commit('SET_CONNECTION_STATUS', { websocket: true })
    // 定期获取实时数据
    setInterval(() => {
      dispatch('fetchRealTimeData')
    }, 5000)
  },

  disconnectWebSocket({ commit }) {
    commit('SET_CONNECTION_STATUS', { websocket: false })
  }
}

const getters = {
  // 获取实时数据
  realTimeData: state => state.realTimeData,
  // 获取设备信息
  deviceInfo: state => state.deviceInfo,
  // 获取在线设备
  onlineDevices: state => state.deviceInfo.filter(device => device.status === 'online'),
  // 获取离线设备
  offlineDevices: state => state.deviceInfo.filter(device => device.status === 'offline'),
  // 获取传感器数据
  sensorData: state => state.sensorData,
  // 获取传感器日志
  sensorLogs: state => state.sensorLogs,
  // 获取天气数据
  weatherData: state => state.weatherData,
  // 获取监测配置
  monitoringConfig: state => state.monitoringConfig,
  // 获取加载状态
  isLoading: state => type => state.loading[type] || false,
  // 获取连接状态
  connectionStatus: state => state.connectionStatus,
  // 获取最新的传感器数据
  latestSensorData: state => type => {
    const data = state.realTimeData[type]
    return data && data.length > 0 ? data[data.length - 1] : null
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}