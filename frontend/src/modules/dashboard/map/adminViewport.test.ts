import { describe, expect, it } from 'vitest'
import {
  adminLevelFilter,
  adminLabelFeatureCollection,
  adminLabelFilter,
  dashboardLayerButtonState,
  dashboardMapCamera,
  focusAdminBoundaryVisible,
  floatingTooltipPosition,
  parentAdminLineFilters,
  visibleAdminLevel,
} from './adminViewport'

describe('dashboard admin viewport', () => {
  it('shows city boundaries before district boundaries in province view', () => {
    expect(visibleAdminLevel('province', 5.35)).toBe('city')
    expect(visibleAdminLevel('province', 7.1)).toBe('city')
    expect(visibleAdminLevel('province', 7.4)).toBe('district')
  })

  it('delays city and district boundaries until the camera is lower', () => {
    expect(visibleAdminLevel('country', 5.05)).toBe('province')
    expect(visibleAdminLevel('country', 5.35)).toBe('city')
    expect(visibleAdminLevel('country', 7.15)).toBe('city')
    expect(visibleAdminLevel('country', 7.45)).toBe('district')
  })

  it('does not force district boundaries after clicking city or district at high camera', () => {
    expect(visibleAdminLevel('city', 5.15)).toBe('province')
    expect(visibleAdminLevel('city', 5.85)).toBe('city')
    expect(visibleAdminLevel('city', 7.45)).toBe('district')
    expect(visibleAdminLevel('district', 6.45)).toBe('city')
    expect(visibleAdminLevel('district', 7.45)).toBe('district')
  })

  it('uses a wider low-zoom camera so country edge provinces fit', () => {
    expect(dashboardMapCamera.initialZoom).toBeLessThanOrEqual(2.65)
    expect(dashboardMapCamera.minZoom).toBeLessThanOrEqual(1.6)
    expect(dashboardMapCamera.maxBounds[0][0]).toBeLessThan(72)
    expect(dashboardMapCamera.maxBounds[1][0]).toBeGreaterThan(137)
  })

  it('keeps municipalities and special administrative regions visible at city zoom', () => {
    expect(adminLevelFilter('city')).toEqual([
      'any',
      ['==', ['get', 'level'], 'city'],
      ['in', ['get', 'code'], ['literal', ['110000', '120000', '310000', '500000', '810000', '820000']]],
    ])
  })

  it('uses one administrative label level at a time', () => {
    expect(adminLabelFilter('province')).toEqual(['==', ['get', 'level'], 'province'])
    expect(adminLabelFilter('city')).toEqual(adminLevelFilter('city'))
    expect(adminLabelFilter('district')).toEqual(['==', ['get', 'level'], 'district'])
  })

  it('builds one point label feature per administrative region', () => {
    const collection = adminLabelFeatureCollection({
      entries: {
        '150900': {
          code: '150900',
          name: '乌兰察布市',
          level: 'city',
          parentCode: '150000',
          bbox: [109.3, 39.6, 114.8, 43.5],
          center: [112.1, 41.55],
        },
        '130700': {
          code: '130700',
          name: '张家口市',
          level: 'city',
          parentCode: '130000',
          bbox: [113.8, 39.5, 116.4, 42.2],
          center: [115.1, 40.85],
        },
      },
    })

    expect(collection.features).toHaveLength(2)
    expect(new Set(collection.features.map((feature) => feature.properties.code))).toEqual(new Set(['150900', '130700']))
    expect(collection.features.find((feature) => feature.properties.code === '150900')).toMatchObject({
      type: 'Feature',
      geometry: {
        type: 'Point',
        coordinates: [112.1, 41.55],
      },
      properties: {
        code: '150900',
        name: '乌兰察布市',
        level: 'city',
      },
    })
  })

  it('keeps parent administrative boundary lines visible below the active level', () => {
    expect(parentAdminLineFilters('province')).toEqual([])
    expect(parentAdminLineFilters('city')).toEqual([['==', ['get', 'level'], 'province']])
    expect(parentAdminLineFilters('district')).toEqual([
      ['==', ['get', 'level'], 'city'],
      ['==', ['get', 'level'], 'province'],
    ])
  })

  it('hides clicked city and district focus boundaries when camera is raised', () => {
    expect(focusAdminBoundaryVisible('city', 5.15)).toBe(false)
    expect(focusAdminBoundaryVisible('city', 5.85)).toBe(true)
    expect(focusAdminBoundaryVisible('district', 6.45)).toBe(false)
    expect(focusAdminBoundaryVisible('district', 7.2)).toBe(true)
  })

  it('treats directly administered regions as city focus boundaries', () => {
    expect(focusAdminBoundaryVisible('province', 5.15, '500000')).toBe(false)
    expect(focusAdminBoundaryVisible('province', 5.85, '500000')).toBe(true)
  })

  it('describes layer button state for operational styling', () => {
    expect(dashboardLayerButtonState(true, 'critical')).toEqual({
      className: 'is-active is-critical',
      ariaPressed: 'true',
    })
    expect(dashboardLayerButtonState(false, 'default')).toEqual({
      className: 'is-muted',
      ariaPressed: 'false',
    })
  })

  it('keeps floating tooltip inside the map viewport', () => {
    expect(floatingTooltipPosition({ x: 40, y: 40 }, { width: 1000, height: 600 })).toEqual({ x: 58, y: 58 })
    expect(floatingTooltipPosition({ x: 970, y: 580 }, { width: 1000, height: 600 })).toEqual({ x: 772, y: 406 })
  })
})
