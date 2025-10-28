// 监控相关API
import request from '@/modules/shared/utils/request'

// 获取实时监控数据
export function getRealtimeData() {
  return request({
    url: '/monitoring/realtime',
    method: 'get'
  })
}

// 获取设备信息
export function getDeviceInfo(deviceId) {
  return request({
    url: `/monitoring/device/${deviceId}`,
    method: 'get'
  })
}

// 获取传感器数据
export function getSensorData(sensorId, params) {
  return request({
    url: `/monitoring/sensor/${sensorId}`,
    method: 'get',
    params
  })
}

// 获取传感器日志
export function getSensorLogs(params) {
  return request({
    url: '/monitoring/sensor-logs',
    method: 'get',
    params
  })
}

// 获取天气数据
export function getWeatherData(location) {
  return request({
    url: '/monitoring/weather',
    method: 'get',
    params: { location }
  })
}

// 获取监控配置
export function getMonitoringConfig() {
  return request({
    url: '/monitoring/config',
    method: 'get'
  })
}

// 更新监控配置
export function updateMonitoringConfig(config) {
  return request({
    url: '/monitoring/config',
    method: 'put',
    data: config
  })
}