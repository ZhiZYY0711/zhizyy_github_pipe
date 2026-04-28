import type { EquipmentIndicator, EquipmentItem, RepairmanIndicator, RepairmanItem } from '../modules/assets/types'
import type { ManoeuvreItem } from '../modules/emergency/types'
import type { LogIndicator, LogItem, PagedLogs } from '../modules/logs/types'
import type {
  DimensionData,
  GlobalKpi,
  MonitoringIndicator,
  MonitoringRecord,
  PagedResult,
  PipeKeyIndicators,
} from '../modules/monitoring/types'
import type {
  KpiRankItem,
  PagedTasks,
  TaskAreaContrast,
  TaskIndicator,
  TaskItem,
  TaskStatusSlice,
  TaskTrendPoint,
} from '../modules/tasks/types'
import type {
  AgentEvent,
  AgentSession,
  ChatLogContent,
  ChatLogFile,
  SseStatus,
  VirtualExpertMessage,
} from '../modules/virtual-expert/types'

export const fallbackGlobalKpi: GlobalKpi = {
  sensor_numbers: 2318,
  abnormal_sensor_numbers: 43,
  warnings: 7,
  underway_task: 26,
  overtime_task: 3,
}

export const fallbackMonitoringIndicator: MonitoringIndicator = {
  total_records: 12468,
  today: 2184,
  safe_count: 8500,
  good_count: 3120,
  danger_count: 692,
  critical_count: 156,
}

export const fallbackAreaDimensions: DimensionData[] = [
  { area_name: '华北一区', pressure: 1.12, flow: 4216, temperature: 42.1, vibration: 1.8, time: 1776664800000 },
  { area_name: '京沪干线', pressure: 1.04, flow: 4568, temperature: 39.8, vibration: 4.7, time: 1776668400000 },
  { area_name: '西南段', pressure: 0.96, flow: 3934, temperature: 73.6, vibration: 1.2, time: 1776672000000 },
]

export const fallbackPipeDimensions: DimensionData[] = [
  { pipe_name: '冀北支线', pressure: 1.82, flow: 4216, temperature: 42.1, vibration: 3.8, time: 1776664800000 },
  { pipe_name: '京津主干线', pressure: 1.14, flow: 4568, temperature: 39.8, vibration: 4.7, time: 1776668400000 },
  { pipe_name: '西气联络线', pressure: 0.96, flow: 3934, temperature: 73.6, vibration: 1.2, time: 1776672000000 },
]

export const fallbackPipeKeyIndicators: PipeKeyIndicators = {
  ave_temperature: 42.6,
  temperature_status: 1,
  ave_flow: 4239,
  flow_status: 0,
  ave_pressure: 1.18,
  pressure_status: 0,
  ave_vibration: 3.2,
  vibration_status: 1,
}

export const fallbackMonitoringRecords: MonitoringRecord[] = [
  {
    id: '1',
    sensor_id: 'P-071',
    sensor_name: '北段压力点 071',
    pipeline_name: '冀北支线',
    pipe_segment_id: '3001',
    pipe_segment_name: '石家庄市-唐山市段',
    pressure: 1.82,
    flow: 4216,
    temperature: 42.1,
    vibration: 3.8,
    data_status: '高危',
    monitor_time: '2026-04-20 17:24:32',
  },
  {
    id: '2',
    sensor_id: 'V-204',
    sensor_name: '主干线振动点 204',
    pipeline_name: '京津主干线',
    pipe_segment_id: '3002',
    pipe_segment_name: '北京市-天津市段',
    pressure: 1.14,
    flow: 4568,
    temperature: 39.8,
    vibration: 4.7,
    data_status: '危险',
    monitor_time: '2026-04-20 17:24:30',
  },
  {
    id: '3',
    sensor_id: 'T-118',
    sensor_name: '联络线温度点 118',
    pipeline_name: '西气联络线',
    pipe_segment_id: '3003',
    pipe_segment_name: '成都市-绵阳市段',
    pressure: 0.96,
    flow: 3934,
    temperature: 73.6,
    vibration: 1.2,
    data_status: '危险',
    monitor_time: '2026-04-20 17:24:28',
  },
  {
    id: '4',
    sensor_id: 'F-332',
    sensor_name: '南环流量点 332',
    pipeline_name: '京津主干线',
    pipe_segment_id: '3004',
    pipe_segment_name: '上海市-苏州市段',
    pressure: 1.03,
    flow: 6880,
    temperature: 45.2,
    vibration: 0.9,
    data_status: '高危',
    monitor_time: '2026-04-20 17:24:20',
  },
]

export const fallbackMonitoringPage: PagedResult<MonitoringRecord> = {
  records: fallbackMonitoringRecords,
  total: 12468,
  page: 1,
  pageSize: 50,
}

export const fallbackEquipmentIndicator: EquipmentIndicator = {
  total: 200,
  normal: 180,
  fault: 10,
  maintenance: 8,
  offline: 2,
}

export const fallbackEquipment: EquipmentItem[] = [
  {
    id: '1001',
    type: '压缩机组',
    status: '运行中',
    area_name: '华北一区',
    pipe_name: '北站',
    pipe_segment_id: '3001',
    pipe_segment_name: '石家庄市-唐山市段',
    responsible: '王建',
    last_check: '2026-04-08',
  },
  {
    id: '1002',
    type: '阀室',
    status: '待检',
    area_name: '京沪干线',
    pipe_name: '9#阀室',
    pipe_segment_id: '3002',
    pipe_segment_name: '北京市-天津市段',
    responsible: '刘敏',
    last_check: '2026-03-21',
  },
  {
    id: '1003',
    type: '阴保站',
    status: '异常',
    area_name: '西南段',
    pipe_name: '里程 184',
    pipe_segment_id: '3003',
    pipe_segment_name: '成都市-绵阳市段',
    responsible: '赵磊',
    last_check: '2026-04-01',
  },
  {
    id: '1004',
    type: '流量计',
    status: '在线',
    area_name: '华东二区',
    pipe_name: '东站',
    pipe_segment_id: '3004',
    pipe_segment_name: '上海市-苏州市段',
    responsible: '陈雪',
    last_check: '2026-02-28',
  },
]

export const fallbackRepairmanIndicator: RepairmanIndicator = {
  total: 50,
  male: 30,
  female: 20,
  avg_age: 35,
  new_this_month: 4,
}

export const fallbackRepairmen: RepairmanItem[] = [
  { id: '501', name: '王建', age: '36', sex: '0', phone: '138****6172', area_name: '华北一区', entry_time: '2020-05-15' },
  { id: '502', name: '刘敏', age: '34', sex: '1', phone: '139****2281', area_name: '京沪干线', entry_time: '2021-04-12' },
  { id: '503', name: '赵磊', age: '39', sex: '0', phone: '136****9022', area_name: '西南段', entry_time: '2019-09-01' },
  { id: '504', name: '陈雪', age: '31', sex: '1', phone: '137****7740', area_name: '华东二区', entry_time: '2022-03-18' },
]

export const fallbackTaskIndicator: TaskIndicator = {
  total: 150,
  pending: 15,
  in_progress: 8,
  completed: 120,
  urgent: 7,
}

export const fallbackTasks: TaskItem[] = [
  {
    id: '801',
    title: '阴保站 CP-04 电位异常复核',
    type: '异常复核',
    area_name: '西南段',
    pipe_name: 'SW-184',
    pipe_segment_id: '3003',
    pipe_segment_name: '成都市-绵阳市段',
    priority: '紧急',
    status: '处理中',
    assignee: '赵磊',
    phone: '136****9022',
    public_time: '2026-04-20 09:20',
    response_time: '2026-04-20 09:26',
    accomplish_time: null,
    feedback_information: '现场复核中，剩余响应 00:34。',
  },
  {
    id: '802',
    title: '阀室 V-09 周期检修确认',
    type: '周期检修',
    area_name: '京沪干线',
    pipe_name: 'JH-009',
    pipe_segment_id: '3002',
    pipe_segment_name: '北京市-天津市段',
    priority: '高',
    status: '待处理',
    assignee: '刘敏',
    phone: '139****2281',
    public_time: '2026-04-20 08:40',
    response_time: null,
    accomplish_time: null,
    feedback_information: '需补充阀门开度与现场照片。',
  },
  {
    id: '803',
    title: '站控柜 SC-12 通信短报排查',
    type: '通信排查',
    area_name: '山东支线',
    pipe_name: 'SD-012',
    pipe_segment_id: '3005',
    pipe_segment_name: '济南市-青岛市段',
    priority: '中',
    status: '处理中',
    assignee: '周宁',
    phone: '137****1412',
    public_time: '2026-04-20 07:50',
    response_time: '2026-04-20 08:04',
    accomplish_time: null,
    feedback_information: '通信组确认链路抖动。',
  },
  {
    id: '804',
    title: '流量计 F-33 数据完整率复核',
    type: '数据核验',
    area_name: '华东二区',
    pipe_name: 'HD-033',
    pipe_segment_id: '3004',
    pipe_segment_name: '上海市-苏州市段',
    priority: '普通',
    status: '已完成',
    assignee: '陈雪',
    phone: '137****7740',
    public_time: '2026-04-19 16:10',
    response_time: '2026-04-19 16:32',
    accomplish_time: '2026-04-20 10:00',
    feedback_information: '数据完整率 99.4%。',
  },
]

export const fallbackTaskPage: PagedTasks = {
  records: fallbackTasks,
  total: 150,
  page: 1,
  pageSize: 20,
}

export const fallbackTaskStatus: TaskStatusSlice[] = [
  { status: '待处理', count: 15 },
  { status: '处理中', count: 18 },
  { status: '已完成', count: 42 },
  { status: '已取消', count: 7 },
]

export const fallbackTaskTrend: TaskTrendPoint[] = [
  { date: '04-14', count: 12 },
  { date: '04-15', count: 18 },
  { date: '04-16', count: 16 },
  { date: '04-17', count: 24 },
  { date: '04-18', count: 21 },
  { date: '04-19', count: 29 },
  { date: '04-20', count: 26 },
]

export const fallbackTaskContrast: TaskAreaContrast[] = [
  { area_name: '华北', count: 45 },
  { area_name: '京沪', count: 38 },
  { area_name: '西南', count: 31 },
  { area_name: '华东', count: 22 },
]

export const fallbackKpiRanks: KpiRankItem[] = [
  { rank: 1, name: '陈雪', value: 98 },
  { rank: 2, name: '王建', value: 97 },
  { rank: 3, name: '郭琳', value: 96 },
  { rank: 4, name: '刘敏', value: 95 },
]

export const fallbackManoeuvres: ManoeuvreItem[] = [
  {
    id: '1',
    name: '华北站场泄漏处置综合演练',
    type: '综合演练',
    status: '进行中',
    area_name: '华北一区',
    location: '3#站场',
    start_time: '2026-04-22 09:00:00',
    end_time: '2026-04-22 11:00:00',
    participants: 42,
    organizer: '调度中心',
    description: '验证站场泄漏上报链路、应急处置流程与联络机制。',
    result: '',
    issues: '',
  },
  {
    id: '2',
    name: '京沪干线阀室火情桌面演练',
    type: '桌面演练',
    status: '计划中',
    area_name: '京沪干线',
    location: '9#阀室',
    start_time: '2026-04-28 14:00:00',
    end_time: '2026-04-28 16:00:00',
    participants: 18,
    organizer: '安全管理部',
    description: '重点验证上报链路和联络机制。',
  },
  {
    id: '3',
    name: '西南段阴保异常应急复盘',
    type: '复盘演练',
    status: '已完成',
    area_name: '西南段',
    location: '里程 184',
    start_time: '2026-04-12 09:00:00',
    end_time: '2026-04-12 10:30:00',
    participants: 21,
    organizer: '运维中心',
    description: '针对阴保异常处置记录进行演练复盘。',
    result: '通过',
    issues: '发现 2 项记录归档改进点。',
  },
]

export const fallbackLogIndicator: LogIndicator = {
  total: 128904,
  today: 2184,
  success: 126720,
  failed: 2184,
  avg_duration: 125,
}

export const fallbackLogs: LogItem[] = [
  {
    id: '9001',
    user_id: 'admin',
    user_name: '管理员',
    operation: '任务查询',
    method: 'POST',
    url: '/data_management/tasks/find_task_params',
    ip: '10.2.1.18',
    location: '河北省石家庄市',
    status: '成功',
    error_msg: null,
    duration: 38,
    create_time: '2026-04-20 12:12:41',
    params: '{"page":1,"page_size":20}',
  },
  {
    id: '9002',
    user_id: 'dispatcher',
    user_name: '调度员',
    operation: '监测数据查询',
    method: 'POST',
    url: '/data_management/monitoring_data/find_data_params',
    ip: '10.2.4.21',
    location: '天津市',
    status: '成功',
    error_msg: null,
    duration: 216,
    create_time: '2026-04-20 12:09:05',
    params: '{"data_status":"2"}',
  },
  {
    id: '9003',
    user_id: 'operator01',
    user_name: '操作员01',
    operation: '设备更新',
    method: 'POST',
    url: '/data_management/equipment/update_equipment',
    ip: '10.2.8.09',
    location: '山东省济南市',
    status: '失败',
    error_msg: '后端返回 500',
    duration: 92,
    create_time: '2026-04-20 12:06:37',
    params: '{"id":"1003","status":"异常"}',
  },
  {
    id: '9004',
    user_id: 'expert',
    user_name: '专家台',
    operation: '虚拟专家消息',
    method: 'POST',
    url: '/manager/virtual-expert/agent/sessions/demo/messages',
    ip: '10.2.3.15',
    location: '北京市',
    status: '成功',
    error_msg: null,
    duration: 65,
    create_time: '2026-04-20 12:01:44',
    params: '{"conversationId":"conv-20260420-01"}',
  },
]

export const fallbackLogPage: PagedLogs = {
  records: fallbackLogs,
  total: 128904,
  page: 1,
  pageSize: 50,
}

export const fallbackExpertMessages: VirtualExpertMessage[] = [
  {
    id: 'm1',
    role: 'system',
    content: '已连接管网虚拟专家。请描述管线、设备或任务现象。',
    timestamp: '2026-04-20 12:00:00',
  },
  {
    id: 'm2',
    role: 'user',
    content: 'CP-04 阴保电位波动超过阈值，优先检查什么？',
    timestamp: '2026-04-20 12:01:08',
  },
  {
    id: 'm3',
    role: 'expert',
    content: '建议先核对采集时间窗口、参比电极状态和最近检修记录，再比对同区段相邻测点趋势。',
    timestamp: '2026-04-20 12:01:12',
  },
]

export const fallbackChatLogs: ChatLogFile[] = [
  {
    fileName: 'chat_20260420_ops.txt',
    fullPath: 'chat-logs/chat_20260420_ops.txt',
    size: 2048,
    lastModified: '2026-04-20T12:01:00Z',
    etag: 'local-fallback-1',
    timestamp: '20260420',
    userId: 'ops',
    sessionId: 'conv-20260420-01',
  },
  {
    fileName: 'chat_20260419_shift.txt',
    fullPath: 'chat-logs/chat_20260419_shift.txt',
    size: 1880,
    lastModified: '2026-04-19T20:30:00Z',
    etag: 'local-fallback-2',
    timestamp: '20260419',
    userId: 'shift',
    sessionId: 'conv-20260419-02',
  },
]

export const fallbackChatLogContent: ChatLogContent = {
  success: true,
  message: 'fallback chat log content',
  content: '用户: 请帮我分析管道压力\nAI: 建议先查看传感器状态、上下游阀门和最近 6 小时趋势。',
  metadata: {
    fileName: 'chat_20260420_ops.txt',
    size: 2048,
    lastModified: '2026-04-20T12:01:00Z',
    contentType: 'text/plain',
    etag: 'local-fallback-1',
  },
  timestamp: 1776667200000,
}

export const fallbackAgentSessions: AgentSession[] = [
  {
    id: 'ana_demo_001',
    title: 'CP-04 阴保电位波动',
    status: 'completed',
    incidentType: 'cathodic_protection_anomaly',
    severity: 'medium',
    objectName: 'CP-04 管段',
    summary: '需要复核监测趋势、参比电极状态和近期维护记录。',
    updatedAt: '2026-04-23 16:42:45',
  },
  {
    id: 'ana_demo_002',
    title: '北段压力短时波动',
    status: 'created',
    incidentType: 'pressure_anomaly',
    severity: 'low',
    objectName: '冀北支线',
    summary: '等待补充近 24 小时趋势和上下游阀室状态。',
    updatedAt: '2026-04-23 15:18:20',
  },
]

export const fallbackAgentEvents: AgentEvent[] = [
  {
    id: 'evt_demo_001',
    sessionId: 'ana_demo_001',
    runId: 'run_demo_001',
    seq: 1,
    type: 'run_started',
    level: 'info',
    title: '开始异常分析',
    payload: { rawInput: 'CP-04 阴保电位波动超过阈值' },
    createdAt: '2026-04-23T08:42:29Z',
  },
  {
    id: 'evt_demo_002',
    sessionId: 'ana_demo_001',
    runId: 'run_demo_001',
    seq: 2,
    type: 'plan_created',
    level: 'info',
    title: '生成分析计划',
    payload: { steps: ['识别异常类型', '查询业务上下文', '检索领域知识', '生成处置建议'] },
    createdAt: '2026-04-23T08:42:30Z',
  },
  {
    id: 'evt_demo_003',
    sessionId: 'ana_demo_001',
    runId: 'run_demo_001',
    seq: 3,
    type: 'tool_completed',
    level: 'info',
    title: '工具完成：query_monitoring_trend',
    payload: {
      toolName: 'query_monitoring_trend',
      summary: '近 24 小时指标存在持续波动，需要复核趋势。',
      input: { object_id: 'CP-04', metric: 'cathodic_potential' },
      context: { object_id: 'CP-04', segment_id: '4' },
      raw_ref: { path: '/internal/virtual-expert/tools/monitoring-trend', source: 'fallback' },
      raw: {
        metric: 'cathodic_potential',
        window: '24h',
        records: [
          { metric: 'cathodic_potential', value: '-0.78V', status: 'warning', segment_id: 4 },
          { metric: 'temperature', value: '22.8°C', status: 'normal', segment_id: 4 },
        ],
      },
      facts: [
        { label: '阴保电位', value: '-0.78V', status: 'warning' },
        { label: '温度', value: '22.8°C', status: 'normal' },
      ],
      confidence: 0.72,
      evidence: { confidence: 0.72, relevanceScore: 0.81 },
    },
    createdAt: '2026-04-23T08:42:34Z',
  },
  {
    id: 'evt_demo_004',
    sessionId: 'ana_demo_001',
    runId: 'run_demo_001',
    seq: 4,
    type: 'recommendation_generated',
    level: 'info',
    title: '生成处置建议',
    payload: {
      riskLevel: 'medium',
      conclusion: '当前异常需要现场复核，优先确认监测趋势与设备状态。',
      actions: ['复核传感器读数', '检查关联设备维护记录', '补充近 24 小时趋势数据'],
    },
    createdAt: '2026-04-23T08:42:45Z',
  },
]

export const fallbackSseStatus: SseStatus = {
  success: true,
  message: 'SSE 服务状态 fallback',
  onlineCount: 0,
  sseUrl: '/manager/virtual-expert/agent/sessions/{id}/runs/stream',
  timestamp: 1776667200000,
}
