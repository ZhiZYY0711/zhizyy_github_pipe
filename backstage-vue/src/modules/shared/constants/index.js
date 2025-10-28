// 共享常量定义

// API状态码
export const API_CODES = {
  SUCCESS: 200,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  SERVER_ERROR: 500
}

// 设备状态
export const DEVICE_STATUS = {
  ONLINE: 'online',
  OFFLINE: 'offline',
  MAINTENANCE: 'maintenance',
  ERROR: 'error'
}

// 任务状态
export const TASK_STATUS = {
  PENDING: 'pending',
  IN_PROGRESS: 'in_progress',
  COMPLETED: 'completed',
  CANCELLED: 'cancelled'
}

// 任务优先级
export const TASK_PRIORITY = {
  LOW: 'low',
  MEDIUM: 'medium',
  HIGH: 'high',
  URGENT: 'urgent'
}

// 日志类型
export const LOG_TYPES = {
  OPERATION: 'operation',
  SYSTEM: 'system',
  INSPECTION: 'inspection',
  ALARM: 'alarm'
}

// 用户角色
export const USER_ROLES = {
  ADMIN: 'admin',
  OPERATOR: 'operator',
  VIEWER: 'viewer',
  REPAIRMAN: 'repairman'
}

// 管道状态
export const PIPELINE_STATUS = {
  NORMAL: 'normal',
  WARNING: 'warning',
  ALARM: 'alarm',
  MAINTENANCE: 'maintenance'
}

// 传感器类型
export const SENSOR_TYPES = {
  PRESSURE: 'pressure',
  TEMPERATURE: 'temperature',
  FLOW: 'flow',
  VIBRATION: 'vibration',
  LEAK: 'leak'
}

// 应急等级
export const EMERGENCY_LEVELS = {
  LEVEL_1: 'level_1', // 一般事故
  LEVEL_2: 'level_2', // 较大事故
  LEVEL_3: 'level_3', // 重大事故
  LEVEL_4: 'level_4'  // 特别重大事故
}

// 演练状态
export const DRILL_STATUS = {
  PLANNED: 'planned',
  IN_PROGRESS: 'in_progress',
  COMPLETED: 'completed',
  CANCELLED: 'cancelled'
}

// 分页默认配置
export const PAGINATION = {
  PAGE_SIZE: 20,
  PAGE_SIZES: [10, 20, 50, 100]
}

// 图表颜色配置
export const CHART_COLORS = {
  PRIMARY: '#409EFF',
  SUCCESS: '#67C23A',
  WARNING: '#E6A23C',
  DANGER: '#F56C6C',
  INFO: '#909399'
}