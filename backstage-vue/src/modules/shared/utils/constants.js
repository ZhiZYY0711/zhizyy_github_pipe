// API 配置
export const API_CONFIG = {
  BASE_URL: 'http://localhost:8080',
  ENDPOINTS: {
    PROVINCES: '/manager/area_details/provinces',
    CITIES: '/manager/area_details/citys',
    DISTRICTS: '/manager/area_details/districts'
  }
};

// 路径配置
export const ROUTE_PATHS = {
  // 数据可视化模块路径
  VISUALIZATION: {
    MAIN: '/main/visualization',
    AREA_DETAILS: '/main/area-details',
    PIPELINE_DETAILS: '/main/pipeline-details',
    TASK_DETAILS: '/main/task-details',
    DATA_MONITORING: '/main/data-monitoring'
  },
  
  // 数据管理模块路径
  DATA_MANAGEMENT: {
    MONITORING: '/main/monitoring',
    EQUIPMENT: '/main/equipment',
    TASKS: '/main/tasks',
    MAINTENANCE: '/main/maintenance',
    REPAIRMAN: '/main/repairman'
  },
  
  // 其他模块路径
  OTHER: {
    VIRTUAL_EXPERT: '/main/virtual-expert',
    SIMULATION_DRILL: '/main/simulation-drill',
    EMERGENCY: '/main/emergency',
    LOGS: '/main/logs'
  }
};

// 菜单配置
export const MENU_CONFIG = {
  VISUALIZATION: 'visualization',
  DATA_MANAGEMENT: 'dataManagement'
};

// 认证相关常量
export const AUTH_CONFIG = {
  TOKEN_KEYS: ['jwt', 'authToken', 'token'],
  BEARER_PREFIX: 'Bearer '
};

// 事件名称常量
export const EVENT_NAMES = {
  AREA_CHANGED: 'areaChanged'
};

// 默认值
export const DEFAULT_VALUES = {
  EMPTY_STRING: '',
  PROVINCE_PLACEHOLDER: '省份不限',
  CITY_PLACEHOLDER: '城市不限',
  DISTRICT_PLACEHOLDER: '区县不限'
};