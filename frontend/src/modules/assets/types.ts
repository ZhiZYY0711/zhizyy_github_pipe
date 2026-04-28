export type EquipmentIndicator = {
  total?: number
  normal?: number
  fault?: number
  maintenance?: number
  offline?: number
}

export type EquipmentItem = {
  id: string
  type: string
  status: string
  area_name: string
  pipe_name: string
  pipe_segment_id?: string
  pipe_segment_name?: string
  responsible: string
  last_check: string
}

export type EquipmentQuery = {
  id?: string
  type?: string
  status?: string
  area_id?: string
  pipe_id?: string
  pipe_segment_id?: string
  responsible?: string
  last_check_start?: string
  last_check_end?: string
  page?: string
  page_size?: string
}

export type RepairmanIndicator = {
  total?: number
  male?: number
  female?: number
  avg_age?: number
  new_this_month?: number
}

export type RepairmanItem = {
  id: string
  name: string
  age: string
  sex: string
  phone: string
  area_name: string
  entry_time: string
}

export type RepairmanQuery = {
  page: number
  page_size: number
  name?: string
  sex?: string
  min_age?: string
  max_age?: string
  area_id?: string
  entry_start_time?: string
  entry_end_time?: string
  phone?: string
}
