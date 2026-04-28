import { apiRequest, type ApiEnvelope } from '../shared/apiClient'
import type {
  AreaOption,
  DimensionData,
  MonitoringFilterOptions,
  MonitoringIndicator,
  MonitoringQuery,
  MonitoringRecord,
  PagedResult,
  PipeDimensionQuery,
  PipeKeyIndicators,
  PipeKeyIndicatorQuery,
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

export function fetchMonitoringFilterOptions(query: {
  province_code?: string
  city_code?: string
  district_code?: string
}) {
  return apiRequest<ApiEnvelope<MonitoringFilterOptions>>('/pipeline_topology/monitoring_filter_options', {
    query,
  })
}

export function fetchAreaDimensionData(areaId?: string, range?: { start_time?: number; end_time?: number }) {
  return apiRequest<ApiEnvelope<DimensionData[]>>('/data_visualization/data_monitoring/area_data', {
    method: 'POST',
    body: {
      area_id: areaId ? Number(areaId) : undefined,
      start_time: range?.start_time,
      end_time: range?.end_time,
    },
  })
}

export function fetchPipeDimensionData(query: PipeDimensionQuery) {
  return apiRequest<ApiEnvelope<DimensionData[]>>('/data_visualization/data_monitoring/pipe_data', {
    method: 'POST',
    body: query,
  })
}

export function fetchPipeKeyIndicators(query: PipeKeyIndicatorQuery = {}) {
  return apiRequest<ApiEnvelope<PipeKeyIndicators>>('/data_visualization/data_monitoring/key_indicators', {
    query,
  })
}

export function fetchMonitoringRows(query: MonitoringQuery) {
  return apiRequest<ApiEnvelope<PagedResult<MonitoringRecord>>>('/data_management/monitoring_data/find_data_params', {
    method: 'POST',
    body: query,
  })
}

export function fetchMonitoringRowById(id: string) {
  return apiRequest<ApiEnvelope<MonitoringRecord[]>>('/data_management/monitoring_data/find_data_id', {
    method: 'POST',
    body: { id },
  })
}

export function createMonitoringRow(payload: Partial<MonitoringRecord>) {
  return apiRequest<ApiEnvelope<null>>('/data_management/monitoring_data/add_data', {
    method: 'POST',
    body: payload,
  })
}

export function updateMonitoringRow(payload: Partial<MonitoringRecord> & { id: string }) {
  return apiRequest<ApiEnvelope<null>>('/data_management/monitoring_data/update_data', {
    method: 'POST',
    body: payload,
  })
}

export function deleteMonitoringRows(ids: string) {
  return apiRequest<ApiEnvelope<null>>(`/data_management/monitoring_data/delete_data/${ids}`)
}

export function fetchMonitoringIndicators(areaId?: string) {
  return apiRequest<ApiEnvelope<MonitoringIndicator>>('/data_management/monitoring_data/indicator_card', {
    query: { area_id: areaId },
  })
}
