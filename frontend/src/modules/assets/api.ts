import { apiRequest, type ApiEnvelope } from '../shared/apiClient'
import type {
  EquipmentIndicator,
  EquipmentItem,
  EquipmentQuery,
  RepairmanIndicator,
  RepairmanItem,
  RepairmanQuery,
} from './types'

export function fetchEquipment(query: EquipmentQuery) {
  return apiRequest<ApiEnvelope<EquipmentItem[]>>('/data_management/equipment/find_equipment_params', {
    method: 'POST',
    body: query,
  })
}

export function fetchEquipmentById(id: string) {
  return apiRequest<ApiEnvelope<EquipmentItem[]>>('/data_management/equipment/find_equipment_id', {
    method: 'POST',
    body: { id },
  })
}

export function addEquipment(payload: Partial<EquipmentItem>) {
  return apiRequest<ApiEnvelope<null>>('/data_management/equipment/add_equipment', {
    method: 'POST',
    body: payload,
  })
}

export function updateEquipment(payload: Partial<EquipmentItem> & { id: string }) {
  return apiRequest<ApiEnvelope<null>>('/data_management/equipment/update_equipment', {
    method: 'POST',
    body: payload,
  })
}

export function deleteEquipment(ids: string) {
  return apiRequest<ApiEnvelope<null>>(`/data_management/equipment/delete_equipment/${ids}`)
}

export function fetchEquipmentIndicators(areaId?: string) {
  return apiRequest<ApiEnvelope<EquipmentIndicator>>('/data_management/equipment/Indicator_card', {
    query: { area_id: areaId },
  })
}

export function fetchRepairmen(query: RepairmanQuery) {
  return apiRequest<ApiEnvelope<Record<string, unknown>>>('/data_management/repairman/find_repairman_params', {
    method: 'POST',
    body: query,
  })
}

export function fetchRepairmanById(id: string) {
  return apiRequest<ApiEnvelope<RepairmanItem>>('/data_management/repairman/find_repairman_id', {
    method: 'POST',
    body: { id },
  })
}

export function addRepairman(payload: Partial<RepairmanItem>) {
  return apiRequest<ApiEnvelope<null>>('/data_management/repairman/add_repairman', {
    method: 'POST',
    body: payload,
  })
}

export function updateRepairman(payload: Partial<RepairmanItem> & { id: string }) {
  return apiRequest<ApiEnvelope<null>>('/data_management/repairman/update_repairman', {
    method: 'POST',
    body: payload,
  })
}

export function deleteRepairmen(ids: string) {
  return apiRequest<ApiEnvelope<null>>(`/data_management/repairman/delete_repairman/${ids}`)
}

export function fetchRepairmanIndicators(areaId?: string) {
  return apiRequest<ApiEnvelope<RepairmanIndicator>>('/data_management/repairman/Indicator_card', {
    query: { area_id: areaId },
  })
}
