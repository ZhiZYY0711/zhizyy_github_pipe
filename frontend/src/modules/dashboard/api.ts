import { apiRequest, type ApiEnvelope } from '../shared/apiClient'
import type { AreaDimensionData, AreaOption, DashboardAlarm, DashboardKpi, TimeRange } from './types'

export function fetchProvinces() {
  return apiRequest<ApiEnvelope<AreaOption[]>>('/area_details/provinces')
}

export function fetchCities(provinceCode: string) {
  return apiRequest<ApiEnvelope<AreaOption[]>>(`/area_details/citys/${provinceCode}`)
}

export function fetchDistricts(cityCode: string) {
  return apiRequest<ApiEnvelope<AreaOption[]>>(`/area_details/districts/${cityCode}`)
}

export function fetchWholeKpi(areaId?: string) {
  return apiRequest<ApiEnvelope<DashboardKpi>>('/data_visualization/all/whole_kpi', {
    query: { area_id: areaId },
  })
}

export function fetchRunningWaterAlarm(areaId?: string, range?: TimeRange) {
  return apiRequest<ApiEnvelope<DashboardAlarm[]>>('/data_visualization/all/running_water_alarm', {
    query: { area_id: areaId, start_time: range?.startTime, end_time: range?.endTime },
  })
}

export function fetchAreaDimensionData(areaId: string, range?: TimeRange) {
  return apiRequest<ApiEnvelope<AreaDimensionData[]>>('/data_visualization/data_monitoring/area_data', {
    method: 'POST',
    body: {
      area_id: Number(areaId),
      start_time: range?.startTime,
      end_time: range?.endTime,
    },
  })
}
