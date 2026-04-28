export type ManoeuvreItem = {
  id: string
  name: string
  type: string
  status: string
  area_name: string
  location: string
  start_time: string
  end_time: string
  participants: number
  organizer: string
  description: string
  result?: string
  issues?: string
}

export type AreaOption = {
  code: string
  name: string
}

export type ManoeuvreQuery = {
  type?: number
  status?: number
  area_id?: number
  start_time_min?: number
  start_time_max?: number
  end_time_min?: number
  end_time_max?: number
  page?: number
  page_size?: number
}
