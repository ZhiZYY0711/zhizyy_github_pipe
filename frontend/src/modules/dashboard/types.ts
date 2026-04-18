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
