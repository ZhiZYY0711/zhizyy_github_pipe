import { unwrapData } from '../shared/apiClient'
import { toNumber } from '../shared/normalize'
import {
  fetchAreaDimensionData,
  fetchCities,
  fetchDistricts,
  fetchProvinces,
  fetchRunningWaterAlarm,
  fetchWholeKpi,
} from './api'
import type {
  AreaDimensionData,
  AreaOption,
  DashboardAlarm,
  DashboardKpi,
  GeoIndex,
  MapTooltipData,
  TimeRange,
} from './types'

const emptyKpi: Required<DashboardKpi> = {
  sensor_numbers: 0,
  abnormal_sensor_numbers: 0,
  warnings: 0,
  underway_task: 0,
  overtime_task: 0,
}

const emptyDimension: Required<AreaDimensionData> = {
  ave_flow: 0,
  ave_pressure: 0,
  ave_temperature: 0,
  ave_vibration: 0,
  time: 0,
}

const geoCdnBaseUrl = String(import.meta.env.VITE_GEO_CDN_BASE_URL ?? '').replace(/\/+$/, '')

export async function loadGeoIndex() {
  const response = await fetch(resolveGeoAssetPath('/geo/index.json'))

  if (!response.ok) {
    throw new Error('地图索引加载失败')
  }

  return normalizeGeoIndex((await response.json()) as GeoIndex)
}

export function resolveGeoAssetPath(path: string) {
  if (!geoCdnBaseUrl || isAbsoluteUrl(path)) {
    return path
  }

  const relativePath = path.replace(/^\/+/, '').replace(/^geo\//, '')
  return `${geoCdnBaseUrl}/${relativePath}`
}

export async function loadAreaOptions() {
  return normalizeAreaOptions(unwrapData(await fetchProvinces(), []))
}

export async function loadCities(parentCode: string) {
  return normalizeAreaOptions(unwrapData(await fetchCities(parentCode), []))
}

export async function loadDistricts(parentCode: string) {
  return normalizeAreaOptions(unwrapData(await fetchDistricts(parentCode), []))
}

export async function loadDashboardKpi(areaId?: string) {
  return normalizeKpi(unwrapData(await fetchWholeKpi(areaId), emptyKpi))
}

export async function loadDashboardAlarms(areaId?: string, range?: TimeRange) {
  return unwrapData(await fetchRunningWaterAlarm(areaId, range), []).map(normalizeAlarm)
}

export async function loadMapTooltipData(areaId: string, areaName: string, range?: TimeRange) {
  const [dimensionResult, kpiResult] = await Promise.allSettled([
    fetchAreaDimensionData(areaId, range),
    fetchWholeKpi(areaId),
  ])
  const dimension =
    dimensionResult.status === 'fulfilled'
      ? normalizeDimension(unwrapData(dimensionResult.value, [emptyDimension]))
      : emptyDimension
  const kpi =
    kpiResult.status === 'fulfilled'
      ? normalizeKpi(unwrapData(kpiResult.value, emptyKpi))
      : emptyKpi

  return {
    areaId,
    areaName,
    flow: dimension.ave_flow,
    pressure: dimension.ave_pressure,
    temperature: dimension.ave_temperature,
    vibration: dimension.ave_vibration,
    sensorNumbers: kpi.sensor_numbers,
    abnormalSensorNumbers: kpi.abnormal_sensor_numbers,
    warnings: kpi.warnings,
  } satisfies MapTooltipData
}

function normalizeAreaOptions(items: AreaOption[]) {
  return items.map((item) => ({
    code: String(item.code ?? ''),
    name: String(item.name ?? ''),
    level: toNumber(item.level),
    parent_code: item.parent_code ? String(item.parent_code) : undefined,
  })).filter((item) => item.code && item.name)
}

function normalizeGeoIndex(index: GeoIndex) {
  return {
    china: normalizeGeoIndexEntry(index.china),
    province: normalizeGeoIndexRecord(index.province),
    city: normalizeGeoIndexRecord(index.city),
    district: normalizeGeoIndexRecord(index.district),
  } satisfies GeoIndex
}

function normalizeGeoIndexRecord(record: GeoIndex['province']) {
  return Object.fromEntries(
    Object.entries(record).map(([code, entry]) => [code, normalizeGeoIndexEntry(entry)]),
  )
}

function normalizeGeoIndexEntry(entry: GeoIndex['china']) {
  return {
    ...entry,
    path: resolveGeoAssetPath(entry.path),
  }
}

function isAbsoluteUrl(path: string) {
  return /^(?:[a-z]+:)?\/\//i.test(path) || path.startsWith('data:') || path.startsWith('blob:')
}

function normalizeKpi(payload: DashboardKpi) {
  return {
    sensor_numbers: toNumber(payload.sensor_numbers),
    abnormal_sensor_numbers: toNumber(payload.abnormal_sensor_numbers),
    warnings: toNumber(payload.warnings),
    underway_task: toNumber(payload.underway_task),
    overtime_task: toNumber(payload.overtime_task),
  }
}

function normalizeDimension(items: AreaDimensionData[]) {
  if (!items.length) {
    return emptyDimension
  }

  const totals = items.reduce<Required<AreaDimensionData>>(
    (acc, item) => ({
      ave_flow: acc.ave_flow + toNumber(item.ave_flow),
      ave_pressure: acc.ave_pressure + toNumber(item.ave_pressure),
      ave_temperature: acc.ave_temperature + toNumber(item.ave_temperature),
      ave_vibration: acc.ave_vibration + toNumber(item.ave_vibration),
      time: Math.max(acc.time, toNumber(item.time)),
    }),
    { ...emptyDimension },
  )
  const count = items.length

  return {
    ave_flow: totals.ave_flow / count,
    ave_pressure: totals.ave_pressure / count,
    ave_temperature: totals.ave_temperature / count,
    ave_vibration: totals.ave_vibration / count,
    time: totals.time,
  }
}

function normalizeAlarm(item: DashboardAlarm) {
  return {
    ...item,
    id: String(item.id ?? ''),
    time: formatAlarmTime(item.time),
    sensor_id: String(item.sensor_id ?? ''),
    sensor_name: String(item.sensor_name ?? '未知传感器'),
    area_id: String(item.area_id ?? ''),
    area_name: String(item.area_name ?? '未知区域'),
    message: String(item.message ?? '检测到异常数据'),
    level: String(item.level ?? '异常'),
  }
}

function formatAlarmTime(value: string | number | undefined) {
  if (typeof value === 'number') {
    return new Intl.DateTimeFormat('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    }).format(new Date(value))
  }

  return value || '-'
}
