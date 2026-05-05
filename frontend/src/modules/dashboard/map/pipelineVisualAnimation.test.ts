import { describe, expect, it } from 'vitest'
import type { PipelineVisualNode, PipelineVisualRoute } from './pipelineVisualData'
import {
  buildPipelineFlowSegments,
  pipelineNodeVisualProfile,
  pipelineStatusVisualProfile,
  smoothPipelineRoutes,
  visiblePipelineNodes,
} from './pipelineVisualAnimation'

const route: PipelineVisualRoute = {
  id: 'test-route',
  name: '测试主干线',
  status: 'critical',
  coords: [
    [100, 30],
    [101, 30],
    [102, 31],
    [103, 31],
  ],
}

const nodes: PipelineVisualNode[] = [
  { id: 'hub', name: '枢纽', type: 'hub', status: 'normal', coord: [100, 30], priority: 3 },
  { id: 'station', name: '压气站', type: 'station', status: 'warning', coord: [101, 30], priority: 2 },
  { id: 'valve', name: '阀室', type: 'valve', status: 'normal', coord: [102, 31], priority: 1 },
]

describe('pipeline visual animation', () => {
  it('builds short line segments for flow instead of point particles', () => {
    const segments = buildPipelineFlowSegments([route], 0.35)

    expect(segments.length).toBeGreaterThan(0)
    expect(segments.every((segment) => segment.path.length >= 2)).toBe(true)
    expect(segments.every((segment) => segment.path.length < route.coords.length)).toBe(true)
    expect(segments[0]).toMatchObject({
      routeId: route.id,
      status: route.status,
    })
  })

  it('gives critical pipelines faster and stronger industrial flow profiles', () => {
    const normal = pipelineStatusVisualProfile('normal')
    const critical = pipelineStatusVisualProfile('critical')

    expect(critical.flowCount).toBeGreaterThan(normal.flowCount)
    expect(critical.flowSpeed).toBeGreaterThan(normal.flowSpeed)
    expect(critical.coreWidth).toBeGreaterThan(normal.coreWidth)
  })

  it('keeps high camera views focused on trunk lines and priority nodes', () => {
    expect(visiblePipelineNodes(nodes, 3.8).map((node) => node.id)).toEqual(['hub'])
    expect(visiblePipelineNodes(nodes, 5.2).map((node) => node.id)).toEqual(['hub', 'station'])
    expect(visiblePipelineNodes(nodes, 6.4).map((node) => node.id)).toEqual(['hub', 'station', 'valve'])
  })

  it('uses stronger symbols for hubs than valves', () => {
    const hub = pipelineNodeVisualProfile('hub', 3)
    const valve = pipelineNodeVisualProfile('valve', 1)

    expect(hub.coreRadius).toBeGreaterThan(valve.coreRadius)
    expect(hub.ringRadius).toBeGreaterThan(valve.ringRadius)
    expect(hub.scanRadius).toBeGreaterThan(0)
    expect(valve.scanRadius).toBe(0)
  })

  it('keeps node halos restrained for an industrial equipment symbol feel', () => {
    const hub = pipelineNodeVisualProfile('hub', 3)
    const station = pipelineNodeVisualProfile('station', 2)

    expect(hub.ringRadius).toBeLessThanOrEqual(13)
    expect(hub.scanRadius).toBeLessThanOrEqual(21)
    expect(station.scanRadius).toBeLessThanOrEqual(15)
  })

  it('smooths pipeline render paths while preserving original endpoints', () => {
    const [smoothed] = smoothPipelineRoutes([route], 5)

    expect(smoothed.coords.length).toBeGreaterThan(route.coords.length)
    expect(smoothed.coords[0]).toEqual(route.coords[0])
    expect(smoothed.coords.at(-1)).toEqual(route.coords.at(-1))
    expect(smoothed.coords.some((coord) => coord[0] > 101 && coord[0] < 102 && coord[1] > 30 && coord[1] < 31)).toBe(true)
  })
})
