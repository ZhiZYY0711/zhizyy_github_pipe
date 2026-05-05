import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  loadDashboardAlarms,
  loadDashboardSummary,
  loadGeoIndex,
  loadMapTooltipData,
  resolveDashboardAlarms,
} from './service'

const originalFetch = globalThis.fetch

beforeEach(() => {
  vi.stubGlobal('window', { location: { origin: 'http://localhost:5173' } })
})

afterEach(() => {
  globalThis.fetch = originalFetch
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})

describe('loadGeoIndex', () => {
  it('falls back instead of throwing when the dev server returns html for the geo index', async () => {
    globalThis.fetch = vi.fn(async () => new Response('<!doctype html><html></html>', {
      headers: { 'Content-Type': 'text/html' },
      status: 200,
    }))

    const index = await loadGeoIndex()

    expect(index.china).toEqual({ code: '100000', name: '全国', path: '' })
    expect(index.province).toEqual({})
  })
})

describe('dashboard summary service', () => {
  it('sends area and time filters when loading running-water alarms', async () => {
    globalThis.fetch = vi.fn(async () => new Response(JSON.stringify({
      code: 200,
      data: [],
    }), { status: 200 }))

    await loadDashboardAlarms('370100', { startTime: 1767225600000, endTime: 1767311999999 })

    const requestUrl = String(vi.mocked(globalThis.fetch).mock.calls[0][0])
    expect(requestUrl).toContain('/data_visualization/all/running_water_alarm')
    expect(requestUrl).toContain('area_id=370100')
    expect(requestUrl).toContain('start_time=1767225600000')
    expect(requestUrl).toContain('end_time=1767311999999')
  })

  it('normalizes merged dashboard summary payload', async () => {
    globalThis.fetch = vi.fn(async () => new Response(JSON.stringify({
      code: 200,
      data: {
        kpi: {
          sensor_numbers: 100,
          abnormal_sensor_numbers: 4,
          warnings: 7,
          underway_task: 2,
          overtime_task: 1,
        },
        alarms: [
          {
            id: 9,
            time: 1767225600000,
            sensorId: 12,
            sensorName: '传感器-12',
            areaId: 370100,
            areaName: '山东济南',
            message: '高危状态检测到严重异常数据',
            level: '高危',
          },
        ],
        areaTrend: [
          {
            ave_flow: 11.5,
            ave_pressure: 6.25,
            ave_temperature: 32.5,
            ave_vibration: 2.5,
            time: 1767225600000,
          },
        ],
        freshness: {
          metricRefreshedAt: '2026-05-04T08:00:00',
          currentRefreshedAt: '2026-05-04T08:01:00',
          alarmRefreshedAt: '2026-05-04T08:01:15',
        },
        generatedAt: '2026-05-04T08:02:00',
      },
    }), { status: 200 }))

    const summary = await loadDashboardSummary('370100', { startTime: 1767225600000, endTime: 1767311999999 })

    expect(summary.kpi.sensor_numbers).toBe(100)
    expect(summary.alarms[0]).toMatchObject({
      id: '9',
      sensor_name: '传感器-12',
      area_id: '370100',
    })
    expect(summary.areaTrend[0].ave_pressure).toBe(6.25)
    expect(summary.freshness.currentRefreshedAt).toBe('2026-05-04T08:01:00')
  })

  it('falls back to running-water alarms when merged summary has no alarms', () => {
    const alarms = resolveDashboardAlarms({
      kpi: {
        sensor_numbers: 0,
        abnormal_sensor_numbers: 0,
        warnings: 0,
        underway_task: 0,
        overtime_task: 0,
      },
      alarms: [],
      areaTrend: [],
      freshness: { alarmRefreshedAt: '2026-05-05T08:00:00' },
    }, [
      {
        id: 'legacy-1',
        message: '旧接口报警',
        level: '高危',
      },
    ])

    expect(alarms).toEqual([
      {
        id: 'legacy-1',
        message: '旧接口报警',
        level: '高危',
      },
    ])
  })

  it('loads tooltip data from the dashboard tooltip endpoint', async () => {
    globalThis.fetch = vi.fn(async () => new Response(JSON.stringify({
      code: 200,
      data: {
        areaId: 370100,
        areaName: '济南市',
        flow: 20.5,
        pressure: 7.25,
        temperature: 28.5,
        vibration: 2.2,
        sensorNumbers: 300,
        abnormalSensorNumbers: 12,
        warnings: 5,
      },
    }), { status: 200 }))

    const tooltip = await loadMapTooltipData('370100', '济南市')

    expect(tooltip).toEqual({
      areaId: '370100',
      areaName: '济南市',
      flow: 20.5,
      pressure: 7.25,
      temperature: 28.5,
      vibration: 2.2,
      sensorNumbers: 300,
      abnormalSensorNumbers: 12,
      warnings: 5,
    })
    expect(vi.mocked(globalThis.fetch).mock.calls[0][0]).toContain('/data_visualization/dashboard/area_tooltip')
  })
})
