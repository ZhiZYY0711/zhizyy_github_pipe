export type LogIndicator = {
  total?: number
  all?: number
  today?: number
  success?: number
  failed?: number
  error?: number
  warning?: number
  debugging?: number
  avg_duration?: number
}

export type LogItem = {
  id: string
  user_id: string
  user_name: string
  user_type?: string
  operation: string
  method?: string
  url?: string
  ip: string
  location?: string
  status: string
  error_msg?: string | null
  duration: number
  create_time: string
  details?: string
  params?: string
}

export type LogQuery = {
  page?: number
  page_size?: number
  area_id?: number
  operation_time_min?: number
  operation_time_max?: number
  status?: number
  type?: number
}

export type PagedLogs = {
  records: LogItem[]
  total: number
  page: number
  pageSize: number
}
