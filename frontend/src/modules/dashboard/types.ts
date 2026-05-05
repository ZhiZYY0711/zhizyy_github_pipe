export type DashboardNavItem = {
  label: string
  href?: string
  active?: boolean
  expert?: boolean
}

export type DashboardStatusTone = 'default' | 'alert' | 'signal'

export type DashboardStatusItem = {
  label: string
  value: string
  tone: DashboardStatusTone
}

export type DashboardBarTone = 'cyan' | 'orange' | 'yellow'

export type DashboardSummaryCard =
  | {
      type: 'hero'
      eyebrow: string
      title: string
      ringLabel: string
      ringValue: string
      ringMeta: string
      tags: readonly string[]
    }
  | {
      type: 'bars'
      eyebrow: string
      title: string
      bars: readonly {
        label: string
        value: string
        width: string
        tone: DashboardBarTone
      }[]
    }
  | {
      type: 'radar'
      eyebrow: string
      title: string
      tags: readonly string[]
    }
  | {
      type: 'switches'
      eyebrow: string
      title: string
      chips: readonly string[]
    }

export type DashboardKpiTone = 'default' | 'signal' | 'warn'

export type DashboardKpiItem = {
  label: string
  value: string
  tone: DashboardKpiTone
}

export type DashboardMapCalloutPlacement = 'north-east' | 'south-west'

export type DashboardMapCallout = {
  eyebrow: string
  title: string
  body: string
  placement: DashboardMapCalloutPlacement
}

export type DashboardMapLegend = {
  title: string
  body: string
}

export type DashboardMapPanel = {
  title: string
  eyebrow: string
  callouts: readonly DashboardMapCallout[]
  legend: DashboardMapLegend
}

export type DashboardSidePanelTone = 'default' | 'critical' | 'warning'

export type DashboardSidePanelItem = {
  title: string
  body: string
  tone: DashboardSidePanelTone
}

export type DashboardSidePanel = {
  title: string
  eyebrow: string
  items: readonly DashboardSidePanelItem[]
}

export type DashboardPageModel = {
  systemEyebrow: string
  systemName: string
  navItems: readonly DashboardNavItem[]
  statusItems: readonly DashboardStatusItem[]
  leftRailCards: readonly DashboardSummaryCard[]
  kpis: readonly DashboardKpiItem[]
  mapPanel: DashboardMapPanel
  sidePanels: readonly DashboardSidePanel[]
}

export type AreaLevel = 'country' | 'province' | 'city' | 'district'

export type AreaOption = {
  code: string
  name: string
  level?: number
  parent_code?: string
}

export type DashboardKpi = {
  sensor_numbers?: number
  abnormal_sensor_numbers?: number
  warnings?: number
  underway_task?: number
  overtime_task?: number
}

export type DashboardAlarm = {
  id?: string
  time?: string | number
  sensor_id?: string
  sensor_name?: string
  area_id?: string
  area_name?: string
  message?: string
  level?: string
}

export type AreaDimensionData = {
  ave_flow?: number
  ave_pressure?: number
  ave_temperature?: number
  ave_vibration?: number
  time?: number
}

export type TimeRange = {
  startTime?: number
  endTime?: number
}

export type DashboardFreshness = {
  metricRefreshedAt?: string
  currentRefreshedAt?: string
  alarmRefreshedAt?: string
  generatedAt?: string
}

export type DashboardSummaryResponse = {
  kpi?: DashboardKpi
  alarms?: DashboardAlarm[]
  areaTrend?: AreaDimensionData[]
  freshness?: DashboardFreshness
  generatedAt?: string
}

export type DashboardSummary = {
  kpi: Required<DashboardKpi>
  alarms: DashboardAlarm[]
  areaTrend: AreaDimensionData[]
  freshness: DashboardFreshness
  generatedAt?: string
}

export type DashboardTooltipResponse = {
  areaId?: string | number
  areaName?: string
  flow?: number
  pressure?: number
  temperature?: number
  vibration?: number
  sensorNumbers?: number
  abnormalSensorNumbers?: number
  warnings?: number
}

export type AdminBoundaryEntry = {
  code: string
  name: string
  level: AreaLevel
  parentCode?: string
  bbox: [number, number, number, number]
  center: [number, number]
}

export type AdminBoundaryIndex = {
  generatedAt?: string
  source?: string
  tileUrl?: string
  entries: Record<string, AdminBoundaryEntry>
}

export type GeoIndexEntry = {
  code: string
  name: string
  path: string
}

export type GeoIndex = {
  china: GeoIndexEntry
  province: Record<string, GeoIndexEntry>
  city: Record<string, GeoIndexEntry>
  district: Record<string, GeoIndexEntry>
}

export type MapRegion = {
  code: string
  name: string
  level: AreaLevel
  path: string
}

export type MapTimeRange = {
  startDate: string
  endDate: string
}

export type MapTimePreset = 'all' | 'week' | 'month' | 'quarter' | 'custom'

export type MapTooltipData = {
  areaId: string
  areaName: string
  flow: number
  pressure: number
  temperature: number
  vibration: number
  sensorNumbers: number
  abnormalSensorNumbers: number
  warnings: number
}

export type DashboardMapLayerKey = 'regions' | 'pipelines' | 'nodes' | 'alarms'

export type PipelineStatus = 'normal' | 'warning' | 'critical'

export type PipelineNodeType = 'hub' | 'station' | 'valve' | 'offtake'

export type PipelineNode = {
  id: string
  name: string
  type: PipelineNodeType
  status: PipelineStatus
  coord: [number, number]
  priority: number
  provinceCode: string
}

export type TrunkPipeline = {
  id: string
  name: string
  status: PipelineStatus
  pressure: number
  flow: number
  riskCount: number
  coords: Array<[number, number]>
  nodes: PipelineNode[]
}

export type DashboardMapFocus =
  | { type: 'region'; code: string; name: string }
  | { type: 'pipeline'; pipeline: TrunkPipeline }
  | null
