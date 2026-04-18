import type {
  DashboardKpiItem,
  DashboardMapPanel,
  DashboardNavItem,
  DashboardPageModel,
  DashboardSidePanel,
  DashboardStatusItem,
  DashboardSummaryCard,
} from '../modules/dashboard/types'

export const topNavItems = [
  { label: '总览', active: true },
  { label: '监测' },
  { label: '资产' },
  { label: '任务' },
  { label: '应急' },
  { label: '日志' },
  { label: '虚拟专家', expert: true },
] as const satisfies readonly DashboardNavItem[]

export const globalStatusItems = [
  { label: '高风险告警', value: '07', tone: 'alert' },
  { label: '系统状态', value: 'STABLE', tone: 'default' },
  { label: '当前时间', value: '17:24:36', tone: 'default' },
] as const satisfies readonly DashboardStatusItem[]

export const leftRailCards = [
  {
    type: 'hero',
    eyebrow: 'Current Lens',
    title: '华北主干网',
    ringLabel: '近 6 小时',
    ringValue: '98',
    ringMeta: '重点监测点',
    tags: ['西气东输一线 A 段', '主干管', '一级响应', '空间锁定'],
  },
  {
    type: 'bars',
    eyebrow: 'System Posture',
    title: '运行态势',
    bars: [
      { label: '在线设备', value: '2,318', width: '82%', tone: 'cyan' },
      { label: '异常监测点', value: '43', width: '34%', tone: 'orange' },
      { label: '高风险区域', value: '3', width: '18%', tone: 'yellow' },
    ],
  },
  {
    type: 'radar',
    eyebrow: 'Priority Focus',
    title: '关注事项',
    tags: ['压力抬升', '逾期闭环', '阀组异常'],
  },
  {
    type: 'switches',
    eyebrow: 'Quick Switch',
    title: '快速切换',
    chips: ['华北', '华东', '西南', '主干管', '场站', '阀室'],
  },
] as const satisfies readonly DashboardSummaryCard[]

export const dashboardKpis = [
  { label: '管线总数', value: '148', tone: 'default' },
  { label: '在线监测点', value: '2,318', tone: 'signal' },
  { label: '高风险告警', value: '07', tone: 'warn' },
  { label: '活跃任务', value: '26', tone: 'default' },
] as const satisfies readonly DashboardKpiItem[]

export const dashboardMapPanel = {
  title: '区域监控主画面',
  eyebrow: 'Map-Centric Overview',
  callouts: [
    {
      eyebrow: '重点区域',
      title: '燕山分输站集群',
      body: '一级告警 3 / 待确认任务 5 / 压力波动持续抬升',
      placement: 'north-east',
    },
    {
      eyebrow: '当前焦点',
      title: '西气东输一线 A3',
      body: '波动指数 8.4% / 异常点位 46 / 需要现场复核',
      placement: 'south-west',
    },
  ],
  legend: {
    title: '当前视图说明',
    body: '地图保持唯一主角，左侧负责图形摘要，右侧承接风险与处置，不再让长文案挤占首屏。',
  },
} as const satisfies DashboardMapPanel

export const sidePanels = [
  {
    title: '重点风险',
    eyebrow: 'Priority / Alerts',
    items: [
      {
        title: '燕山分输站 A3 压力异常抬升',
        body: '异常点位 46，近 12 分钟持续抬升，建议维持一级关注。',
        tone: 'critical',
      },
      {
        title: '津沽支线 2 号阀室波动扩大',
        body: '通信稳定，趋势未回落，需要继续跟踪阀室状态。',
        tone: 'warning',
      },
      {
        title: '京沪监测链路短时离线',
        body: '已触发备用链路，当前不影响核心监控，但需要后续复核。',
        tone: 'default',
      },
    ],
  },
  {
    title: '处置进度',
    eyebrow: 'Response / Tasks',
    items: [
      {
        title: '处理中任务 / 26',
        body: '现场派发 8 项，待确认 3 项，任务闭环速率保持稳定。',
        tone: 'default',
      },
      {
        title: '待调度确认 / 3',
        body: '燕山分输站与华北巡检队已建立联动，预计 18:00 前完成复核。',
        tone: 'default',
      },
      {
        title: '逾期闭环提醒',
        body: '2 项旧任务仍待指挥中心确认，建议在本班次完成状态更新。',
        tone: 'default',
      },
    ],
  },
] as const satisfies readonly DashboardSidePanel[]

export const dashboardPageModel = {
  systemEyebrow: 'National Energy Corridor / Command Layer',
  systemName: '油气管网监测指挥中枢',
  navItems: topNavItems,
  statusItems: globalStatusItems,
  leftRailCards,
  kpis: dashboardKpis,
  mapPanel: dashboardMapPanel,
  sidePanels,
} as const satisfies DashboardPageModel

export const loginStory = {
  eyebrow: 'Mission Entry',
  title: '守护主干能源网络的持续安全运行',
  lead: '面向区域主干网的监测、预警、调度与处置协同平台。',
  ringLabel: '当前联控区域',
  ringValue: '08',
  ringMeta: '重点区域在线',
  metaItems: [
    { label: '系统职责', value: '监测 / 预警 / 调度 / 处置' },
    { label: '当前状态', value: '主控链路稳定' },
  ],
  tags: ['主干网', '风险预警', '联动调度'],
} as const

export const loginForm = {
  eyebrow: 'Access Node',
  title: '进入控制中心',
  note: '请使用已授权账号进入管网监测与调度工作台。',
  accountLabel: '账号',
  accountPlaceholder: '请输入操作员账号',
  passwordLabel: '密码',
  passwordPlaceholder: '请输入访问密码',
  captchaLabel: '验证码',
  captchaPlaceholder: '输入验证码',
  captchaValue: '7G4M2N',
  actionText: '身份校验并进入',
} as const

export const loginAccessMeta = [
  '当前系统状态：稳定运行',
  '高风险告警：07',
] as const
