// 管理相关API
import request from '@/modules/shared/utils/request'

// 设备管理
export function getEquipmentList(params) {
  return request({
    url: '/management/equipment',
    method: 'get',
    params
  })
}

export function createEquipment(data) {
  return request({
    url: '/management/equipment',
    method: 'post',
    data
  })
}

export function updateEquipment(id, data) {
  return request({
    url: `/management/equipment/${id}`,
    method: 'put',
    data
  })
}

export function deleteEquipment(id) {
  return request({
    url: `/management/equipment/${id}`,
    method: 'delete'
  })
}

// 任务管理
export function getTaskList(params) {
  return request({
    url: '/management/tasks',
    method: 'get',
    params
  })
}

export function createTask(data) {
  return request({
    url: '/management/tasks',
    method: 'post',
    data
  })
}

export function updateTask(id, data) {
  return request({
    url: `/management/tasks/${id}`,
    method: 'put',
    data
  })
}

export function deleteTask(id) {
  return request({
    url: `/management/tasks/${id}`,
    method: 'delete'
  })
}

export function getTaskDetails(id) {
  return request({
    url: `/management/tasks/${id}`,
    method: 'get'
  })
}

// 维修人员管理
export function getRepairmanList(params) {
  return request({
    url: '/management/repairman',
    method: 'get',
    params
  })
}

export function createRepairman(data) {
  return request({
    url: '/management/repairman',
    method: 'post',
    data
  })
}

export function updateRepairman(id, data) {
  return request({
    url: `/management/repairman/${id}`,
    method: 'put',
    data
  })
}

export function deleteRepairman(id) {
  return request({
    url: `/management/repairman/${id}`,
    method: 'delete'
  })
}