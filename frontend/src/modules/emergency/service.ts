import { fallbackManoeuvres } from '../../data/businessFallback'
import { unwrapData } from '../shared/apiClient'
import { fetchCities, fetchDistricts, fetchManoeuvreById, fetchManoeuvres, fetchProvinces } from './api'
import type { AreaOption, ManoeuvreItem, ManoeuvreQuery } from './types'

function manoeuvreTypeText(value: unknown) {
  const normalized = String(value ?? '')
  if (normalized === '0') return '状态模拟'
  if (normalized === '1') return '事故模拟'
  if (normalized === '2') return '紧急事件'
  if (normalized === '3') return '日常演练'
  return normalized || '演练'
}

function manoeuvreStatusText(value: unknown) {
  const normalized = String(value ?? '')
  if (normalized === '0') return '成功'
  if (normalized === '1') return '失败'
  if (normalized === '2') return '进行中'
  return normalized || '未知'
}

function formatDateTime(value: unknown) {
  if (typeof value !== 'number') return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString('zh-CN', { hour12: false })
}

function normalizeRow(item: unknown): ManoeuvreItem {
  const record = item as Record<string, unknown>
  const repairmans = Array.isArray(record.repairmans) ? record.repairmans : []

  return {
    id: String(record.id ?? ''),
    name: String(record.name ?? `演练 #${record.id ?? '-'}`),
    type: manoeuvreTypeText(record.type),
    status: manoeuvreStatusText(record.status),
    area_name: String(record.areaName ?? '-'),
    location: String(record.location ?? '-'),
    start_time: formatDateTime(record.startTime),
    end_time: formatDateTime(record.endTime),
    participants: repairmans.length,
    organizer: String(record.organizer ?? '演练调度'),
    description: String(record.details ?? '-'),
    result: record.result == null ? undefined : String(record.result),
    issues: record.issues == null ? undefined : String(record.issues),
  }
}

function normalizeRows(payload: unknown) {
  if (!Array.isArray(payload)) return fallbackManoeuvres

  return payload.map(normalizeRow)
}

function normalizeDetail(payload: unknown, fallback: ManoeuvreItem) {
  if (Array.isArray(payload)) return payload[0] ? normalizeRow(payload[0]) : fallback
  if (payload && typeof payload === 'object') return normalizeRow(payload)
  return fallback
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

export async function loadEmergencyTimeline(query: ManoeuvreQuery = {}) {
  try {
    const response = await fetchManoeuvres({
      area_id: query.area_id,
      type: query.type,
      status: query.status,
      start_time_min: query.start_time_min,
      start_time_max: query.start_time_max,
      end_time_min: query.end_time_min,
      end_time_max: query.end_time_max,
      page: 1,
      page_size: 50,
    })

    return { rows: normalizeRows(unwrapData(response, fallbackManoeuvres)), isFallback: false }
  } catch {
    return { rows: fallbackManoeuvres, isFallback: true }
  }
}

export async function loadEmergencyDetail(item: ManoeuvreItem) {
  try {
    const response = await fetchManoeuvreById(item.id)
    return normalizeDetail(unwrapData(response, [item]), item)
  } catch {
    return item
  }
}

export async function loadEmergencyProvinces() {
  return normalizeAreaOptions(unwrapData(await fetchProvinces(), []))
}

export async function loadEmergencyCities(provinceCode: string) {
  return normalizeAreaOptions(unwrapData(await fetchCities(provinceCode), []))
}

export async function loadEmergencyDistricts(cityCode: string) {
  return normalizeAreaOptions(unwrapData(await fetchDistricts(cityCode), []))
}
