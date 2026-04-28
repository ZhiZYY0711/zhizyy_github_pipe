import { apiRequest, type ApiEnvelope } from '../shared/apiClient'
import type { AreaOption, ManoeuvreItem, ManoeuvreQuery } from './types'

export function fetchProvinces() {
  return apiRequest<ApiEnvelope<AreaOption[]>>('/area_details/provinces')
}

export function fetchCities(provinceCode: string) {
  return apiRequest<ApiEnvelope<AreaOption[]>>(`/area_details/citys/${provinceCode}`)
}

export function fetchDistricts(cityCode: string) {
  return apiRequest<ApiEnvelope<AreaOption[]>>(`/area_details/districts/${cityCode}`)
}

export function fetchManoeuvres(query: ManoeuvreQuery) {
  return apiRequest<ApiEnvelope<ManoeuvreItem[]>>('/manoeuvre/find_manoeuvre_params', {
    method: 'POST',
    body: query,
  })
}

export function fetchManoeuvreById(id: string | number) {
  return apiRequest<ApiEnvelope<ManoeuvreItem[]>>('/manoeuvre/find_manoeuvre_id', {
    query: { id },
  })
}

export function addManoeuvre(payload: Partial<ManoeuvreItem>) {
  return apiRequest<ApiEnvelope<null>>('/manoeuvre/add_manoeuvre', {
    method: 'POST',
    body: payload,
  })
}

export function updateManoeuvre(payload: Partial<ManoeuvreItem> & { id: string }) {
  return apiRequest<ApiEnvelope<null>>('/manoeuvre/update_manoeuvre', {
    method: 'POST',
    body: payload,
  })
}

export function deleteManoeuvre(id: string | number) {
  return apiRequest<ApiEnvelope<null>>('/manoeuvre/remove_manoeuvre', {
    query: { id },
  })
}
