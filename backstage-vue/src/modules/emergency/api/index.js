// 应急相关API
import request from '@/modules/shared/utils/request'

// 事故响应
export function getAccidentList(params) {
  return request({
    url: '/emergency/accidents',
    method: 'get',
    params
  })
}

export function createAccidentReport(data) {
  return request({
    url: '/emergency/accidents',
    method: 'post',
    data
  })
}

export function updateAccidentStatus(id, status) {
  return request({
    url: `/emergency/accidents/${id}/status`,
    method: 'put',
    data: { status }
  })
}

// 虚拟专家
export function getExpertAdvice(query) {
  return request({
    url: '/emergency/expert/advice',
    method: 'post',
    data: { query }
  })
}

export function getExpertKnowledge(category) {
  return request({
    url: '/emergency/expert/knowledge',
    method: 'get',
    params: { category }
  })
}

// 模拟演练
export function getDrillList(params) {
  return request({
    url: '/emergency/drills',
    method: 'get',
    params
  })
}

export function createDrill(data) {
  return request({
    url: '/emergency/drills',
    method: 'post',
    data
  })
}

export function startDrill(id) {
  return request({
    url: `/emergency/drills/${id}/start`,
    method: 'post'
  })
}

export function endDrill(id, results) {
  return request({
    url: `/emergency/drills/${id}/end`,
    method: 'post',
    data: results
  })
}

// 应急预案
export function getEmergencyPlans() {
  return request({
    url: '/emergency/plans',
    method: 'get'
  })
}

export function getEmergencyPlan(id) {
  return request({
    url: `/emergency/plans/${id}`,
    method: 'get'
  })
}