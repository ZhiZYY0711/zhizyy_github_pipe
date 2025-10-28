# 监控模块 (Monitoring Module)

## 模块概述

监控模块负责实时数据监控、设备管理和传感器数据处理，是系统数据采集和监控的核心模块。

## 目录结构

```
monitoring/
├── api/
│   └── index.js              # 监控API接口
├── components/
│   └── WeatherDate.vue       # 天气日期组件
├── store/
│   └── index.js              # 监控状态管理
└── views/
    └── DataMonitoring.vue    # 数据监控主页
```

## 功能特性

### 📡 实时监控
- **传感器数据**: 压力、温度、流量、振动实时监控
- **设备状态**: 传感器设备在线状态监控
- **数据采集**: 自动化数据采集和存储
- **异常检测**: 实时异常数据检测和报警

### 🌡️ 环境监控
- **天气数据**: 实时天气信息获取
- **环境参数**: 温度、湿度、风速等环境数据
- **气象预警**: 恶劣天气预警提醒

### ⚙️ 设备管理
- **设备信息**: 传感器设备基本信息管理
- **设备配置**: 监控参数配置和调整
- **设备维护**: 设备维护记录和计划

### 📊 数据分析
- **历史数据**: 传感器历史数据查询
- **数据统计**: 数据统计分析和报表
- **趋势分析**: 数据变化趋势分析

## API接口

### 实时数据接口
```javascript
// 获取实时数据
getRealtimeData()
// 返回: { pressure, temperature, flow, vibration }

// 获取设备信息
getDeviceInfo(deviceId)
// 参数: deviceId (可选，获取所有或指定设备)
// 返回: { devices: [] }

// 获取传感器数据
getSensorData(sensorId, timeRange)
// 参数: { sensorId, startTime, endTime }
// 返回: { data: [], timestamps: [] }

// 获取传感器日志
getSensorLogs(params)
// 参数: { page, size, filters }
// 返回: { logs: [], total, page, size }

// 获取天气数据
getWeatherData(location)
// 参数: location (位置信息)
// 返回: { weather, temperature, humidity, ... }
```

### 配置管理接口
```javascript
// 获取监控配置
getMonitoringConfig()
// 返回: { thresholds, intervals, alerts }

// 更新监控配置
updateMonitoringConfig(config)
// 参数: 配置对象
// 返回: 更新结果
```

## 状态管理

### State
```javascript
{
  // 实时数据
  realTimeData: {
    pressure: 0,
    temperature: 0,
    flow: 0,
    vibration: 0
  },
  
  // 设备信息
  deviceInfo: [],
  
  // 传感器数据
  sensorData: [],
  sensorLogs: [],
  
  // 天气数据
  weatherData: {},
  
  // 监控配置
  monitoringConfig: {},
  
  // 状态管理
  loading: false,
  connected: false
}
```

### Mutations
- `SET_REAL_TIME_DATA`: 设置实时数据
- `SET_DEVICE_INFO`: 设置设备信息
- `SET_SENSOR_DATA`: 设置传感器数据
- `SET_SENSOR_LOGS`: 设置传感器日志
- `SET_WEATHER_DATA`: 设置天气数据
- `SET_MONITORING_CONFIG`: 设置监控配置
- `SET_LOADING`: 设置加载状态
- `SET_CONNECTION_STATUS`: 设置连接状态

### Actions
- `fetchRealTimeData`: 获取实时数据
- `fetchDeviceInfo`: 获取设备信息
- `fetchSensorData`: 获取传感器数据
- `fetchSensorLogs`: 获取传感器日志
- `fetchWeatherData`: 获取天气数据
- `fetchMonitoringConfig`: 获取监控配置
- `updateMonitoringConfig`: 更新监控配置
- `connectWebSocket`: 连接WebSocket
- `disconnectWebSocket`: 断开WebSocket

## 组件说明

### DataMonitoring.vue
数据监控主页，包含：
- 实时数据展示面板
- 传感器状态列表
- 数据图表展示
- 设备管理界面
- 配置参数设置

### WeatherDate.vue
天气日期组件，包含：
- 当前日期时间显示
- 实时天气信息
- 温度和天气图标
- 自动更新功能

## 使用示例

### 在组件中使用监控数据
```javascript
import { mapState, mapActions } from 'vuex'

export default {
  computed: {
    ...mapState('monitoring', ['realTimeData', 'connected'])
  },
  methods: {
    ...mapActions('monitoring', [
      'fetchRealTimeData', 
      'connectWebSocket'
    ]),
    
    async initMonitoring() {
      try {
        await this.fetchRealTimeData()
        await this.connectWebSocket()
      } catch (error) {
        this.$toast.error('监控初始化失败')
      }
    }
  },
  mounted() {
    this.initMonitoring()
  }
}
```

### WebSocket连接使用
```javascript
// 在store action中
connectWebSocket({ commit }) {
  const ws = new WebSocket('ws://localhost:8080/monitoring')
  
  ws.onmessage = (event) => {
    const data = JSON.parse(event.data)
    commit('SET_REAL_TIME_DATA', data)
  }
  
  ws.onopen = () => {
    commit('SET_CONNECTION_STATUS', true)
  }
  
  ws.onclose = () => {
    commit('SET_CONNECTION_STATUS', false)
  }
}
```

## 配置说明

### 监控参数配置
- 数据采集间隔设置
- 报警阈值配置
- 设备超时设置
- 数据保存策略

### WebSocket配置
- 实时数据推送
- 连接重试机制
- 心跳检测
- 断线重连

### 路由配置
- 监控主页: `/monitoring`
- 设备详情: `/monitoring/device/:id`
- 数据查询: `/monitoring/data`