import {
  fallbackAreaDimensions,
  fallbackMonitoringIndicator,
  fallbackMonitoringPage,
  fallbackPipeDimensions,
  fallbackPipeKeyIndicators,
} from '../../data/businessFallback'
import { unwrapData } from '../shared/apiClient'
import { readPageMeta, toNumber } from '../shared/normalize'
import {
  fetchAreaDimensionData,
  fetchCities,
  fetchDistricts,
  fetchMonitoringFilterOptions,
  fetchMonitoringIndicators,
  fetchMonitoringRows,
  fetchPipeDimensionData,
  fetchPipeKeyIndicators,
  fetchProvinces,
} from './api'
import type {
  AreaOption,
  MonitoringFilterOptions,
  MonitoringOverviewQuery,
  MonitoringPipeOption,
  MonitoringQuery,
  MonitoringSegmentOption,
} from './types'

const monitoringStatusText: Record<string, string> = {
  '0': '安全',
  '1': '良好',
  '2': '危险',
  '3': '高危',
}

function normalizeDimensionData(payload: unknown) {
  const rows = Array.isArray(payload) ? payload : []
  return rows.map((item) => {
    const record = item as Record<string, unknown>
    return {
      area_name: record.area_name ? String(record.area_name) : undefined,
      pipe_name: record.pipe_name ? String(record.pipe_name) : undefined,
      pressure: toNumber(record.ave_pressure ?? record.pressure),
      flow: toNumber(record.ave_flow ?? record.flow),
      temperature: toNumber(record.ave_temperature ?? record.temperature),
      vibration: toNumber(record.ave_vibration ?? record.vibration),
      time: toNumber(record.time),
    }
  })
}

function normalizeKeyIndicators(payload: Record<string, unknown>) {
  return {
    ave_temperature: toNumber(payload.ave_temperature),
    temperature_status: toNumber(payload.temperature_status),
    ave_flow: toNumber(payload.ave_flow),
    flow_status: toNumber(payload.flow_status),
    ave_pressure: toNumber(payload.ave_pressure),
    pressure_status: toNumber(payload.pressure_status),
    ave_vibration: toNumber(payload.ave_vibration),
    vibration_status: toNumber(payload.vibration_status),
  }
}

function normalizeAreaOptions(payload: unknown): AreaOption[] {
  const rows = Array.isArray(payload) ? payload : []
  return rows
    .map((item) => {
      const record = item as Record<string, unknown>
      return {
        code: String(record.code ?? ''),
        name: String(record.name ?? ''),
      }
    })
    .filter((item) => item.code && item.name)
}

function normalizeMonitoringFilterOptions(payload: unknown): MonitoringFilterOptions {
  const record = payload as Record<string, unknown>
  const pipes = Array.isArray(record.pipes) ? record.pipes : []

  return {
    scope_level: String(record.scope_level ?? 'NATIONAL') as MonitoringFilterOptions['scope_level'],
    pipes: pipes.map((pipe) => {
      const pipeRecord = pipe as Record<string, unknown>
      const segments = Array.isArray(pipeRecord.segments) ? pipeRecord.segments : []

      return {
        id: String(pipeRecord.id ?? ''),
        name: String(pipeRecord.name ?? ''),
        pipe_level: pipeRecord.pipe_level ? String(pipeRecord.pipe_level) : undefined,
        segment_level: pipeRecord.segment_level ? String(pipeRecord.segment_level) : undefined,
        segments: segments.map((segment) => {
          const segmentRecord = segment as Record<string, unknown>
          return {
            id: String(segmentRecord.id ?? ''),
            name: String(segmentRecord.name ?? ''),
            segment_order: toNumber(segmentRecord.segment_order),
            start_area_id: segmentRecord.start_area_id ? String(segmentRecord.start_area_id) : undefined,
            end_area_id: segmentRecord.end_area_id ? String(segmentRecord.end_area_id) : undefined,
            segment_level: segmentRecord.segment_level ? String(segmentRecord.segment_level) : undefined,
          } satisfies MonitoringSegmentOption
        }),
      } satisfies MonitoringPipeOption
    }),
  }
}

function normalizeMonitoringIndicators(payload: Record<string, unknown>) {
  return {
    total_records: toNumber(payload.total_records),
    today: toNumber(payload.today),
    safe_count: toNumber(payload.safe_count),
    good_count: toNumber(payload.good_count),
    danger_count: toNumber(payload.danger_count),
    critical_count: toNumber(payload.critical_count),
  }
}

function normalizeMonitoringRows(payload: Record<string, unknown>) {
  const records = Array.isArray(payload.records) ? payload.records : []
  const pageMeta = readPageMeta(payload, {
    defaultPage: 1,
    defaultPageSize: records.length || 50,
    pageSizeKeys: ['pageSize', 'page_size', 'page_size_num', 'recordsSize'],
  })

  return {
    records: records.map((item) => {
      const record = item as Record<string, unknown>
      const sensorId = String(record.sensor_id ?? '')
      const status = String(record.data_status ?? '')

      return {
        id: String(record.id ?? ''),
        sensor_id: sensorId,
        sensor_name: String(record.sensor_name ?? `传感器 ${sensorId || '-'}`),
        pipeline_name: String(record.pipeline_name ?? '-'),
        pipe_segment_id: String(record.pipe_segment_id ?? ''),
        pipe_segment_name: String(record.pipe_segment_name ?? '-'),
        pressure: toNumber(record.pressure),
        flow: toNumber(record.flow),
        temperature: toNumber(record.temperature),
        vibration: toNumber(record.vibration),
        data_status: String(record.status_text ?? monitoringStatusText[status] ?? (status || '未知')),
        monitor_time: String(record.monitor_time ?? '-'),
      }
    }),
    ...pageMeta,
  }
}

function buildPipeQuery(query: MonitoringOverviewQuery) {
  const segmentIds = query.segment_ids ?? []
  return {
    id: query.pipe_id || 1,
    segment_id: segmentIds.length === 1 ? segmentIds[0] : undefined,
    segment_ids: segmentIds.length > 1 ? segmentIds : undefined,
    start_time: query.start_time,
    end_time: query.end_time,
  }
}

function buildKeyIndicatorQuery(query: MonitoringOverviewQuery) {
  const segmentIds = query.segment_ids ?? []
  return {
    id: query.pipe_id || 1,
    segment_id: segmentIds.length === 1 ? segmentIds[0] : undefined,
    segment_ids: segmentIds.length > 1 ? segmentIds.join(',') : undefined,
  }
}

export async function loadMonitoringOverview(query: MonitoringOverviewQuery = {}) {
  const [areaData, pipeData, keyIndicators] = await Promise.allSettled([
    fetchAreaDimensionData(query.area_id, { start_time: query.start_time, end_time: query.end_time }),
    fetchPipeDimensionData(buildPipeQuery(query)),
    fetchPipeKeyIndicators(buildKeyIndicatorQuery(query)),
  ])

  return {
    areaData:
      areaData.status === 'fulfilled'
        ? normalizeDimensionData(unwrapData(areaData.value, fallbackAreaDimensions))
        : normalizeDimensionData(fallbackAreaDimensions),
    pipeData:
      pipeData.status === 'fulfilled'
        ? normalizeDimensionData(unwrapData(pipeData.value, fallbackPipeDimensions))
        : normalizeDimensionData(fallbackPipeDimensions),
    keyIndicators:
      keyIndicators.status === 'fulfilled'
        ? normalizeKeyIndicators(unwrapData(keyIndicators.value, fallbackPipeKeyIndicators) as Record<string, unknown>)
        : normalizeKeyIndicators(fallbackPipeKeyIndicators),
    isFallback: areaData.status === 'rejected' || pipeData.status === 'rejected' || keyIndicators.status === 'rejected',
  }
}

export async function loadMonitoringProvinces() {
  return normalizeAreaOptions(unwrapData(await fetchProvinces(), []))
}

export async function loadMonitoringCities(provinceCode: string) {
  return normalizeAreaOptions(unwrapData(await fetchCities(provinceCode), []))
}

export async function loadMonitoringDistricts(cityCode: string) {
  return normalizeAreaOptions(unwrapData(await fetchDistricts(cityCode), []))
}

export async function loadMonitoringFilterOptions(query: {
  province_code?: string
  city_code?: string
  district_code?: string
}) {
  return normalizeMonitoringFilterOptions(unwrapData(await fetchMonitoringFilterOptions(query), {
    scope_level: 'NATIONAL',
    pipes: [],
  }))
}

export async function loadMonitoringData(query: MonitoringQuery = { page: 1, page_size: 50 }) {
  const [indicators, rows] = await Promise.allSettled([fetchMonitoringIndicators(query.area_id), fetchMonitoringRows(query)])

  return {
    indicators:
      indicators.status === 'fulfilled'
        ? normalizeMonitoringIndicators(unwrapData(indicators.value, fallbackMonitoringIndicator) as Record<string, unknown>)
        : fallbackMonitoringIndicator,
    page:
      rows.status === 'fulfilled'
        ? normalizeMonitoringRows(unwrapData(rows.value, fallbackMonitoringPage) as Record<string, unknown>)
        : fallbackMonitoringPage,
    isFallback: indicators.status === 'rejected' || rows.status === 'rejected',
  }
}

export async function loadMonitoringDataIndicators(areaId?: string) {
  try {
    const indicators = await fetchMonitoringIndicators(areaId)

    return {
      indicators: normalizeMonitoringIndicators(
        unwrapData(indicators, fallbackMonitoringIndicator) as Record<string, unknown>,
      ),
      isFallback: false,
    }
  } catch {
    return {
      indicators: fallbackMonitoringIndicator,
      isFallback: true,
    }
  }
}

export async function loadMonitoringDataRows(query: MonitoringQuery = { page: 1, page_size: 50 }) {
  try {
    const rows = await fetchMonitoringRows(query)

    return {
      page: normalizeMonitoringRows(unwrapData(rows, fallbackMonitoringPage) as Record<string, unknown>),
      isFallback: false,
    }
  } catch {
    return {
      page: fallbackMonitoringPage,
      isFallback: true,
    }
  }
}
