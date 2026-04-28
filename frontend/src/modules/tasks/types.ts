export type TaskIndicator = {
  total?: number
  pending?: number
  in_progress?: number
  completed?: number
  urgent?: number
}

export type AreaOption = {
  code: string
  name: string
}

export type TaskItem = {
  id: string
  title: string
  type: string
  area_name: string
  pipe_name: string
  pipe_segment_id?: string
  pipe_segment_name?: string
  priority: string
  status: string
  assignee: string
  phone: string
  public_time: string
  response_time: string | null
  accomplish_time: string | null
  feedback_information: string | null
}

export type TaskQuery = {
  page?: number
  page_size?: number
  inspection_id?: number
  repairman_id?: number
  area_id?: number
  pipe_id?: number
  pipe_segment_id?: number
  type?: number
  priority?: number
  status?: number
  start_time?: string
  end_time?: string
}

export type TaskChartQuery = {
  area_id: number
  type?: number
  start_time?: number
  end_time?: number
}

export type TaskStatusSlice = {
  status: string
  count: number
}

export type TaskTrendPoint = {
  date?: string
  time?: number
  count: number
}

export type TaskAreaContrast = {
  area_name: string
  count: number
}

export type KpiRankItem = {
  rank: number
  name: string
  value: number
  repairman_id?: string
}

export type KpiRankBoard = {
  type: 0 | 1 | 2
  title: string
  unit: string
  rows: KpiRankItem[]
}

export type PagedTasks = {
  records: TaskItem[]
  total: number
  page: number
  pageSize: number
}
