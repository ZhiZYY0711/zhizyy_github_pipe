import {
  fallbackEquipment,
  fallbackEquipmentIndicator,
  fallbackRepairmanIndicator,
  fallbackRepairmen,
} from '../../data/businessFallback'
import { unwrapData } from '../shared/apiClient'
import { toNumber } from '../shared/normalize'
import {
  fetchEquipment,
  fetchEquipmentIndicators,
  fetchRepairmanIndicators,
  fetchRepairmen,
} from './api'
import type { EquipmentQuery, RepairmanQuery } from './types'

const defaultEquipmentQuery: EquipmentQuery = { page: '1', page_size: '50' }
const defaultRepairmanQuery: RepairmanQuery = { page: 1, page_size: 50 }

function equipmentTypeText(value: unknown) {
  const normalized = String(value ?? '')
  if (normalized === '0') return '温度传感器'
  if (normalized === '1') return '压力传感器'
  if (normalized === '2') return '流量传感器'
  if (normalized === '3') return '振动传感器'
  if (normalized === '4') return '位移传感器'
  return normalized || '设备'
}

function equipmentStatusText(value: unknown) {
  const normalized = String(value ?? '')
  if (normalized === '0') return '故障'
  if (normalized === '1') return '正常'
  if (normalized === '2') return '维护中'
  return normalized || '未知'
}

function normalizeEquipmentIndicators(payload: Record<string, unknown>) {
  return {
    total: toNumber(payload.total),
    normal: toNumber(payload.normal),
    fault: toNumber(payload.fault),
    maintenance: toNumber(payload.maintenance),
    offline: toNumber(payload.offline),
  }
}

function normalizeEquipmentRows(payload: unknown, limit?: number) {
  if (!Array.isArray(payload)) return fallbackEquipment

  const rows = typeof limit === 'number' ? payload.slice(0, limit) : payload

  return rows.map((item) => {
    const record = item as Record<string, unknown>

    return {
      id: String(record.id ?? ''),
      type: equipmentTypeText(record.type),
      status: equipmentStatusText(record.status),
      area_name: String(record.area_name ?? '-'),
      pipe_name: String(record.pipe_name ?? '-'),
      pipe_segment_id: String(record.pipe_segment_id ?? ''),
      pipe_segment_name: String(record.pipe_segment_name ?? '-'),
      responsible: String(record.responsible ?? '-'),
      last_check: String(record.last_check ?? '-'),
    }
  })
}

function normalizeRepairmanIndicators(payload: Record<string, unknown>) {
  return {
    total: toNumber(payload.total),
    male: toNumber(payload.male),
    female: toNumber(payload.female),
    avg_age: toNumber(payload.avg_age),
    new_this_month: toNumber(payload.new_this_month),
  }
}

function normalizeRepairmanRows(payload: Record<string, unknown>) {
  const records = Array.isArray(payload.records) ? payload.records : Array.isArray(payload) ? payload : []

  return records.map((item) => {
    const record = item as Record<string, unknown>

    return {
      id: String(record.id ?? ''),
      name: String(record.name ?? '-'),
      age: String(record.age ?? '-'),
      sex: String(record.sex ?? ''),
      phone: String(record.phone ?? '-'),
      area_name: String(record.area_name ?? '-'),
      entry_time: String(record.entry_time ?? '-'),
    }
  })
}

export async function loadAssetWorkbench() {
  const [equipmentIndicators, equipment, repairmanIndicators, repairmen] = await Promise.allSettled([
    fetchEquipmentIndicators(),
    fetchEquipment(defaultEquipmentQuery),
    fetchRepairmanIndicators(),
    fetchRepairmen(defaultRepairmanQuery),
  ])

  return {
    equipmentIndicators:
      equipmentIndicators.status === 'fulfilled'
        ? normalizeEquipmentIndicators(unwrapData(equipmentIndicators.value, fallbackEquipmentIndicator) as Record<string, unknown>)
        : fallbackEquipmentIndicator,
    equipment:
      equipment.status === 'fulfilled'
        ? normalizeEquipmentRows(unwrapData(equipment.value, fallbackEquipment), 18)
        : fallbackEquipment,
    repairmanIndicators:
      repairmanIndicators.status === 'fulfilled'
        ? normalizeRepairmanIndicators(unwrapData(repairmanIndicators.value, fallbackRepairmanIndicator) as Record<string, unknown>)
        : fallbackRepairmanIndicator,
    repairmen:
      repairmen.status === 'fulfilled'
        ? normalizeRepairmanRows(unwrapData(repairmen.value, { records: fallbackRepairmen }) as unknown as Record<string, unknown>)
        : fallbackRepairmen,
    isFallback:
      equipmentIndicators.status === 'rejected' ||
      equipment.status === 'rejected' ||
      repairmanIndicators.status === 'rejected' ||
      repairmen.status === 'rejected',
  }
}

export async function loadEquipmentTable(query: EquipmentQuery = defaultEquipmentQuery) {
  const [indicators, equipment] = await Promise.allSettled([fetchEquipmentIndicators(), fetchEquipment(query)])

  return {
    indicators:
      indicators.status === 'fulfilled'
        ? normalizeEquipmentIndicators(unwrapData(indicators.value, fallbackEquipmentIndicator) as Record<string, unknown>)
        : fallbackEquipmentIndicator,
    rows:
      equipment.status === 'fulfilled'
        ? normalizeEquipmentRows(unwrapData(equipment.value, fallbackEquipment), 50)
        : fallbackEquipment,
    isFallback: indicators.status === 'rejected' || equipment.status === 'rejected',
  }
}

export async function loadRepairmanTable(query: RepairmanQuery = defaultRepairmanQuery) {
  const [indicators, repairmen] = await Promise.allSettled([fetchRepairmanIndicators(), fetchRepairmen(query)])

  return {
    indicators:
      indicators.status === 'fulfilled'
        ? normalizeRepairmanIndicators(unwrapData(indicators.value, fallbackRepairmanIndicator) as Record<string, unknown>)
        : fallbackRepairmanIndicator,
    rows:
      repairmen.status === 'fulfilled'
        ? normalizeRepairmanRows(unwrapData(repairmen.value, { records: fallbackRepairmen }) as unknown as Record<string, unknown>)
        : fallbackRepairmen,
    isFallback: indicators.status === 'rejected' || repairmen.status === 'rejected',
  }
}
