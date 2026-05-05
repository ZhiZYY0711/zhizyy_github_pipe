import type { AdminBoundaryIndex, MapRegion } from '../types'

export type AdminDisplayLevel = 'province' | 'city' | 'district'
export type DashboardLayerTone = 'default' | 'signal' | 'warning' | 'critical'

export const directlyAdministeredRegionCodes = ['110000', '120000', '310000', '500000', '810000', '820000']

export const dashboardMapCamera = {
  center: [104.2, 35.8] as [number, number],
  initialZoom: 2.55,
  minZoom: 1.55,
  maxZoom: 10.5,
  maxBounds: [[60, 4], [148, 62]] as [[number, number], [number, number]],
}

const countryCityZoom = 5.25
const countryDistrictZoom = 7.35
const provinceDistrictZoom = 7.25
const cityFocusBoundaryZoom = 5.65
const districtFocusBoundaryZoom = 6.95

export function visibleAdminLevel(regionLevel: MapRegion['level'], zoom: number): AdminDisplayLevel {
  if (regionLevel === 'country') {
    return visibleAdminLevelByZoom(zoom)
  }

  if (regionLevel === 'province') {
    if (zoom >= provinceDistrictZoom) {
      return 'district'
    }

    return zoom >= countryCityZoom ? 'city' : 'province'
  }

  return visibleAdminLevelByZoom(zoom)
}

function visibleAdminLevelByZoom(zoom: number): AdminDisplayLevel {
  if (zoom >= countryDistrictZoom) {
    return 'district'
  }

  if (zoom >= countryCityZoom) {
    return 'city'
  }

  return 'province'
}

export function adminLevelFilter(level: AdminDisplayLevel) {
  if (level !== 'city') {
    return ['==', ['get', 'level'], level]
  }

  return [
    'any',
    ['==', ['get', 'level'], 'city'],
    ['in', ['get', 'code'], ['literal', directlyAdministeredRegionCodes]],
  ]
}

export function adminLabelFilter(level: AdminDisplayLevel) {
  return adminLevelFilter(level)
}

export type AdminLabelFeatureCollection = {
  type: 'FeatureCollection'
  features: Array<{
    type: 'Feature'
    geometry: {
      type: 'Point'
      coordinates: [number, number]
    }
    properties: {
      code: string
      name: string
      level: AdminDisplayLevel
      parentCode?: string
    }
  }>
}

export function adminLabelFeatureCollection(index: Pick<AdminBoundaryIndex, 'entries'>): AdminLabelFeatureCollection {
  return {
    type: 'FeatureCollection',
    features: Object.values(index.entries)
      .filter((entry): entry is typeof entry & { level: AdminDisplayLevel } => isAdminDisplayLevel(entry.level))
      .map((entry) => ({
        type: 'Feature',
        geometry: {
          type: 'Point',
          coordinates: entry.center,
        },
        properties: {
          code: entry.code,
          name: entry.name,
          level: entry.level,
          parentCode: entry.parentCode,
        },
      })),
  }
}

function isAdminDisplayLevel(level: string): level is AdminDisplayLevel {
  return level === 'province' || level === 'city' || level === 'district'
}

export function parentAdminLineFilters(level: AdminDisplayLevel) {
  if (level === 'province') {
    return []
  }

  if (level === 'city') {
    return [['==', ['get', 'level'], 'province']]
  }

  return [
    ['==', ['get', 'level'], 'city'],
    ['==', ['get', 'level'], 'province'],
  ]
}

export function focusAdminBoundaryVisible(regionLevel: MapRegion['level'], zoom: number, regionCode = '') {
  const effectiveLevel = directlyAdministeredRegionCodes.includes(regionCode) ? 'city' : regionLevel

  if (effectiveLevel === 'district') {
    return zoom >= districtFocusBoundaryZoom
  }

  if (effectiveLevel === 'city') {
    return zoom >= cityFocusBoundaryZoom
  }

  return true
}

export function dashboardLayerButtonState(active: boolean, tone: DashboardLayerTone) {
  return {
    className: active ? `is-active is-${tone}` : 'is-muted',
    ariaPressed: String(active),
  }
}

export function floatingTooltipPosition(
  point: { x: number; y: number },
  viewport: { width: number; height: number },
) {
  const width = 216
  const height = 156
  const gap = 18
  const margin = 12
  const desiredX = point.x + gap
  const desiredY = point.y < height + gap + margin ? point.y + gap : point.y - height - gap

  return {
    x: Math.min(Math.max(desiredX, margin), Math.max(margin, viewport.width - width - margin)),
    y: Math.min(Math.max(desiredY, margin), Math.max(margin, viewport.height - height - margin)),
  }
}
