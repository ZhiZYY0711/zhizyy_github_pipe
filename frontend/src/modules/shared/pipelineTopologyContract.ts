import type { EquipmentItem, EquipmentQuery } from '../assets/types'
import { addEquipment } from '../assets/api'
import { fetchPipeDimensionData, fetchPipeKeyIndicators } from '../monitoring/api'
import type { MonitoringQuery, MonitoringRecord } from '../monitoring/types'
import { addTask } from '../tasks/api'
import type { TaskItem, TaskQuery } from '../tasks/types'

type AddEquipmentPayload = Parameters<typeof addEquipment>[0]
type AddTaskPayload = Parameters<typeof addTask>[0]
type PipeDimensionPayload = Parameters<typeof fetchPipeDimensionData>[0]
type PipeKeyIndicatorPayload = Parameters<typeof fetchPipeKeyIndicators>[0]

export const equipmentSegmentContract: EquipmentItem = {
  id: '1',
  type: '压力传感器',
  status: '正常',
  area_name: '石家庄市',
  pipe_name: '石家庄-唐山输油管道',
  pipe_segment_id: '3',
  pipe_segment_name: '石家庄市-唐山市段',
  responsible: '张三',
  last_check: '2026-04-22',
}

export const equipmentSegmentQueryContract: EquipmentQuery = {
  page: '1',
  page_size: '20',
  pipe_segment_id: '3',
}

export const taskSegmentContract: TaskItem = {
  id: '1',
  title: '石家庄市-唐山市段压力异常检测',
  type: '检修',
  area_name: '石家庄市',
  pipe_name: '石家庄-唐山输油管道',
  pipe_segment_id: '3',
  pipe_segment_name: '石家庄市-唐山市段',
  priority: '紧急',
  status: '进行中',
  assignee: '张三',
  phone: '13800138000',
  public_time: '2026-04-22 10:00:00',
  response_time: null,
  accomplish_time: null,
  feedback_information: null,
}

export const taskSegmentQueryContract: TaskQuery = {
  page: 1,
  page_size: 20,
  pipe_segment_id: 3,
}

export const monitoringSegmentContract: MonitoringRecord = {
  id: '1',
  sensor_id: '1001',
  sensor_name: '压力传感器 1001',
  pipeline_name: '石家庄-唐山输油管道',
  pipe_segment_id: '3',
  pipe_segment_name: '石家庄市-唐山市段',
  pressure: 1.1,
  flow: 4200,
  temperature: 35,
  vibration: 0.3,
  data_status: '安全',
  monitor_time: '2026-04-22 10:00:00',
}

export const monitoringSegmentQueryContract: MonitoringQuery = {
  page: 1,
  page_size: 50,
  pipe_segment_id: '3',
}

export const addEquipmentSegmentPayloadContract: AddEquipmentPayload = {
  type: '压力传感器',
  status: '正常',
  area_name: '石家庄市',
  pipe_name: '石家庄-唐山输油管道',
  pipe_segment_id: '3',
  responsible: '1',
  last_check: '2026-04-22',
}

export const addTaskSegmentPayloadContract: AddTaskPayload = {
  title: '石家庄市-唐山市段压力异常检测',
  type: '检修',
  area_name: '石家庄市',
  pipe_name: '石家庄-唐山输油管道',
  pipe_segment_id: '3',
  priority: '紧急',
  status: '进行中',
  assignee: '张三',
  phone: '13800138000',
  public_time: '2026-04-22 10:00:00',
  response_time: null,
  accomplish_time: null,
  feedback_information: null,
}

export const pipeDimensionSegmentPayloadContract: PipeDimensionPayload = {
  id: 1,
  segment_id: 3,
}

export const pipeKeyIndicatorSegmentPayloadContract: PipeKeyIndicatorPayload = {
  id: 1,
  segment_id: 3,
}
