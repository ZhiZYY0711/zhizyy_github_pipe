import { apiRequest, type ApiEnvelope } from '../shared/apiClient'
import type {
  AreaOption,
  KpiRankItem,
  PagedTasks,
  TaskAreaContrast,
  TaskChartQuery,
  TaskIndicator,
  TaskItem,
  TaskQuery,
  TaskStatusSlice,
  TaskTrendPoint,
} from './types'

export function fetchProvinces() {
  return apiRequest<ApiEnvelope<AreaOption[]>>('/area_details/provinces')
}

export function fetchCities(provinceCode: string) {
  return apiRequest<ApiEnvelope<AreaOption[]>>(`/area_details/citys/${provinceCode}`)
}

export function fetchDistricts(cityCode: string) {
  return apiRequest<ApiEnvelope<AreaOption[]>>(`/area_details/districts/${cityCode}`)
}

export function fetchTasks(query: TaskQuery) {
  return apiRequest<ApiEnvelope<PagedTasks>>('/data_management/tasks/find_task_params', {
    method: 'POST',
    body: query,
  })
}

export function fetchTaskById(id: string | number) {
  return apiRequest<ApiEnvelope<TaskItem>>('/data_management/tasks/find_task_id', {
    method: 'POST',
    body: { id },
  })
}

export function addTask(payload: Partial<TaskItem>) {
  return apiRequest<ApiEnvelope<string>>('/data_management/tasks/add_task', {
    method: 'POST',
    body: payload,
  })
}

export function updateTask(payload: Partial<TaskItem> & { id: string }) {
  return apiRequest<ApiEnvelope<string>>('/data_management/tasks/update_task', {
    method: 'POST',
    body: payload,
  })
}

export function deleteTask(id: string | number) {
  return apiRequest<ApiEnvelope<string>>(`/data_management/tasks/delete_task/${id}`)
}

export function fetchTaskIndicators(areaId?: number) {
  return apiRequest<ApiEnvelope<TaskIndicator>>('/data_management/tasks/Indicator_card', {
    query: { area_id: areaId },
  })
}

export function fetchTaskStatus(query: TaskChartQuery) {
  return apiRequest<ApiEnvelope<TaskStatusSlice[]>>('/data_visualization/task_details/task_status', {
    method: 'POST',
    body: query,
  })
}

export function fetchTaskTrend(query: TaskChartQuery) {
  return apiRequest<ApiEnvelope<TaskTrendPoint[]>>('/data_visualization/task_details/task_nums', {
    method: 'POST',
    body: query,
  })
}

export function fetchTaskContrast(query: TaskChartQuery) {
  return apiRequest<ApiEnvelope<TaskAreaContrast[]>>('/data_visualization/task_details/task_contrast', {
    method: 'POST',
    body: query,
  })
}

export function fetchKpiList(query: TaskChartQuery) {
  return apiRequest<ApiEnvelope<KpiRankItem[]>>('/data_visualization/task_details/KPI_list', {
    method: 'POST',
    body: query,
  })
}
