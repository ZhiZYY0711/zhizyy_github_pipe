export type GlobalKpi = {
  sensor_numbers?: number
  abnormal_sensor_numbers?: number
  warnings?: number
  underway_task?: number
  overtime_task?: number
}

export type MonitoringIndicator = {
  total_records?: number
  today?: number
  safe_count?: number
  good_count?: number
  danger_count?: number
  critical_count?: number
}

export type DimensionData = {
  area_name?: string
  pipe_name?: string
  pressure?: number
  flow?: number
  temperature?: number
  vibration?: number
  time?: number
}

export type AreaOption = {
  code: string
  name: string
}

export type PipeKeyIndicators = {
  ave_temperature?: number
  temperature_status?: number
  ave_flow?: number
  flow_status?: number
  ave_pressure?: number
  pressure_status?: number
  ave_vibration?: number
  vibration_status?: number
}

export type PipeDimensionQuery = {
  id?: string | number
  segment_id?: string | number
  segment_ids?: Array<string | number>
  start_time?: number
  end_time?: number
}

export type PipeKeyIndicatorQuery = {
  id?: string | number
  segment_id?: string | number
  segment_ids?: string
}

export type MonitoringMetricKey = 'pressure' | 'flow' | 'temperature' | 'vibration'

export type MonitoringSegmentOption = {
  id: string
  name: string
  segment_order: number
  start_area_id?: string
  end_area_id?: string
  segment_level?: string
}

export type MonitoringPipeOption = {
  id: string
  name: string
  pipe_level?: string
  segment_level?: string
  segments: MonitoringSegmentOption[]
}

export type MonitoringFilterOptions = {
  scope_level: 'NATIONAL' | 'PROVINCE' | 'CITY' | 'DISTRICT'
  pipes: MonitoringPipeOption[]
}

export type MonitoringOverviewQuery = {
  area_id?: string
  pipe_id?: string
  segment_ids?: string[]
  start_time?: number
  end_time?: number
}

export type MonitoringRecord = {
  id: string
  sensor_id: string
  sensor_name: string
  pipeline_name: string
  pipe_segment_id?: string
  pipe_segment_name?: string
  pressure: number
  flow: number
  temperature: number
  vibration: number
  data_status: string
  monitor_time: string
}

export type MonitoringQuery = {
  min_pressure?: string
  max_pressure?: string
  min_flow?: string
  max_flow?: string
  min_temperature?: string
  max_temperature?: string
  min_vibration?: string
  max_vibration?: string
  data_status?: string
  sensor_id?: string
  area_id?: string
  pipeline_id?: string
  pipe_segment_id?: string
  pipe_segment_ids?: string[]
  pipeline_name?: string
  monitor_start_time?: string
  monitor_end_time?: string
  page?: number
  page_size?: number
}

export type PagedResult<T> = {
  records: T[]
  total: number
  page: number
  pageSize: number
}
