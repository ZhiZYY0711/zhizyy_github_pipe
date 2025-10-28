// 仪表板相关API
import request from '@/modules/shared/utils/request'

// 获取概览数据
export function getDashboardOverview() {
  return request({
    url: '/dashboard/overview',
    method: 'get'
  })
}

// 获取地图数据
export function getMapData() {
  return request({
    url: '/dashboard/map-data',
    method: 'get'
  })
}

// 获取区域详情
export function getAreaDetails(areaId) {
  return request({
    url: `/dashboard/area/${areaId}`,
    method: 'get'
  })
}

// 获取管道详情
export function getPipelineDetails(pipelineId) {
  return request({
    url: `/dashboard/pipeline/${pipelineId}`,
    method: 'get'
  })
}

// 获取流量趋势数据
export function getFlowTrendData(params) {
  return request({
    url: '/dashboard/flow-trend',
    method: 'get',
    params
  })
}

// 获取统计图表数据
export function getChartData(chartType) {
  return request({
    url: `/dashboard/chart/${chartType}`,
    method: 'get'
  })
}