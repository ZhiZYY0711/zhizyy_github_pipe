import { apiRequest, type ApiEnvelope } from '../shared/apiClient'
import type { LogIndicator, LogItem, LogQuery, PagedLogs } from './types'

export function fetchLogs(query: LogQuery) {
  return apiRequest<ApiEnvelope<PagedLogs>>('/log/find_logs_params', {
    method: 'POST',
    body: query,
  })
}

export function fetchLogById(id: string | number) {
  return apiRequest<ApiEnvelope<LogItem>>('/log/find_logs_id', {
    method: 'POST',
    query: { id },
  })
}

export function fetchLogIndicators(areaId?: number) {
  return apiRequest<ApiEnvelope<LogIndicator>>('/log/indicator_card', {
    query: { area_id: areaId },
  })
}
