// 日志相关API
import request from '@/modules/shared/utils/request'

// 获取日志列表
export function getLogList(params) {
  return request({
    url: '/logs',
    method: 'get',
    params
  })
}

// 获取操作日志
export function getOperationLogs(params) {
  return request({
    url: '/logs/operations',
    method: 'get',
    params
  })
}

// 获取系统日志
export function getSystemLogs(params) {
  return request({
    url: '/logs/system',
    method: 'get',
    params
  })
}

// 获取巡检日志
export function getInspectionLogs(params) {
  return request({
    url: '/logs/inspections',
    method: 'get',
    params
  })
}

// 创建巡检日志
export function createInspectionLog(data) {
  return request({
    url: '/logs/inspections',
    method: 'post',
    data
  })
}

// 导出日志
export function exportLogs(params) {
  return request({
    url: '/logs/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

// 删除日志
export function deleteLogs(ids) {
  return request({
    url: '/logs',
    method: 'delete',
    data: { ids }
  })
}

// 获取日志统计
export function getLogStatistics(params) {
  return request({
    url: '/logs/statistics',
    method: 'get',
    params
  })
}