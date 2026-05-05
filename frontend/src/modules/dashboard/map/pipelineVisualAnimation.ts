import type {
  PipelineVisualNode,
  PipelineVisualNodeType,
  PipelineVisualRoute,
  PipelineVisualStatus,
} from './pipelineVisualData'

export type PipelineStatusVisualProfile = {
  coreColor: [number, number, number]
  glowColor: [number, number, number]
  flowColor: [number, number, number]
  coreWidth: number
  casingWidth: number
  glowWidth: number
  flowWidth: number
  flowCount: number
  flowSpeed: number
  flowSpan: number
}

export type PipelineFlowSegment = {
  id: string
  routeId: string
  status: PipelineVisualStatus
  path: Array<[number, number]>
}

export type PipelineNodeVisualProfile = {
  coreRadius: number
  ringRadius: number
  scanRadius: number
  strokeWidth: number
}

export function pipelineStatusVisualProfile(status: PipelineVisualStatus): PipelineStatusVisualProfile {
  if (status === 'critical') {
    return {
      coreColor: [255, 122, 53],
      glowColor: [255, 96, 42],
      flowColor: [255, 188, 112],
      coreWidth: 5.8,
      casingWidth: 9.8,
      glowWidth: 22,
      flowWidth: 4.6,
      flowCount: 7,
      flowSpeed: 0.22,
      flowSpan: 0.075,
    }
  }

  if (status === 'warning') {
    return {
      coreColor: [226, 178, 86],
      glowColor: [255, 204, 102],
      flowColor: [255, 226, 146],
      coreWidth: 5,
      casingWidth: 8.8,
      glowWidth: 18,
      flowWidth: 3.9,
      flowCount: 6,
      flowSpeed: 0.16,
      flowSpan: 0.065,
    }
  }

  return {
    coreColor: [110, 202, 212],
    glowColor: [143, 232, 240],
    flowColor: [218, 252, 255],
    coreWidth: 4.2,
    casingWidth: 7.6,
    glowWidth: 15,
    flowWidth: 3.2,
    flowCount: 5,
    flowSpeed: 0.1,
    flowSpan: 0.055,
  }
}

export function buildPipelineFlowSegments(routes: PipelineVisualRoute[], phase: number): PipelineFlowSegment[] {
  return routes.flatMap((route) => {
    const profile = pipelineStatusVisualProfile(route.status)

    return Array.from({ length: profile.flowCount }, (_, index) => {
      const start = wrapRatio(phase * profile.flowSpeed + index / profile.flowCount)
      const end = Math.min(start + profile.flowSpan, 1)
      const path = sliceRoutePath(route.coords, start, end)

      return {
        id: `${route.id}-flow-${index}`,
        routeId: route.id,
        status: route.status,
        path,
      }
    })
  })
}

export function visiblePipelineNodes(nodes: PipelineVisualNode[], zoom: number) {
  if (zoom < 4.6) {
    return nodes.filter((node) => node.priority >= 3)
  }

  if (zoom < 6) {
    return nodes.filter((node) => node.priority >= 2)
  }

  return nodes
}

export function pipelineNodeVisualProfile(
  type: PipelineVisualNodeType,
  priority: number,
): PipelineNodeVisualProfile {
  const priorityBoost = Math.max(0, priority - 1)

  if (type === 'hub') {
    return {
      coreRadius: 6.2 + priorityBoost * 0.8,
      ringRadius: 10.5 + priorityBoost * 1.1,
      scanRadius: 16 + priorityBoost * 2.2,
      strokeWidth: 1.35,
    }
  }

  if (type === 'station') {
    return {
      coreRadius: 5 + priorityBoost * 0.65,
      ringRadius: 8.8 + priorityBoost,
      scanRadius: 12 + priorityBoost * 1.8,
      strokeWidth: 1.15,
    }
  }

  if (type === 'offtake') {
    return {
      coreRadius: 4.6 + priorityBoost * 0.55,
      ringRadius: 7.8 + priorityBoost,
      scanRadius: 10 + priorityBoost * 1.4,
      strokeWidth: 1.05,
    }
  }

  return {
    coreRadius: 3.4 + priorityBoost * 0.35,
    ringRadius: 5.4 + priorityBoost * 0.6,
    scanRadius: 0,
    strokeWidth: 0.9,
  }
}

export function routeColor(color: [number, number, number], opacity: number): [number, number, number, number] {
  return [...color, Math.round(Math.max(0, Math.min(opacity, 1)) * 255)]
}

export function smoothPipelineRoutes(routes: PipelineVisualRoute[], samplesPerSegment = 6): PipelineVisualRoute[] {
  return routes.map((route) => ({
    ...route,
    coords: smoothPath(route.coords, samplesPerSegment),
  }))
}

function smoothPath(coords: Array<[number, number]>, samplesPerSegment: number) {
  if (coords.length < 3) {
    return coords
  }

  const sampleCount = Math.max(2, Math.round(samplesPerSegment))
  const smoothed: Array<[number, number]> = [coords[0]]

  for (let index = 0; index < coords.length - 1; index += 1) {
    const previous = coords[Math.max(0, index - 1)]
    const current = coords[index]
    const next = coords[index + 1]
    const after = coords[Math.min(coords.length - 1, index + 2)]

    for (let sample = 1; sample <= sampleCount; sample += 1) {
      smoothed.push(catmullRomPoint(previous, current, next, after, sample / sampleCount))
    }
  }

  smoothed[smoothed.length - 1] = coords[coords.length - 1]
  return dedupeAdjacentPoints(smoothed)
}

function catmullRomPoint(
  p0: [number, number],
  p1: [number, number],
  p2: [number, number],
  p3: [number, number],
  t: number,
): [number, number] {
  const t2 = t * t
  const t3 = t2 * t

  return [
    0.5 * ((2 * p1[0]) + (-p0[0] + p2[0]) * t + (2 * p0[0] - 5 * p1[0] + 4 * p2[0] - p3[0]) * t2 + (-p0[0] + 3 * p1[0] - 3 * p2[0] + p3[0]) * t3),
    0.5 * ((2 * p1[1]) + (-p0[1] + p2[1]) * t + (2 * p0[1] - 5 * p1[1] + 4 * p2[1] - p3[1]) * t2 + (-p0[1] + 3 * p1[1] - 3 * p2[1] + p3[1]) * t3),
  ]
}

function sliceRoutePath(coords: Array<[number, number]>, startRatio: number, endRatio: number) {
  if (coords.length <= 1) {
    return coords
  }

  const start = pointAtRouteRatio(coords, startRatio)
  const end = pointAtRouteRatio(coords, endRatio)
  const path: Array<[number, number]> = [start]
  const startIndex = Math.floor(startRatio * (coords.length - 1))
  const endIndex = Math.floor(endRatio * (coords.length - 1))

  for (let index = startIndex + 1; index <= endIndex; index += 1) {
    if (coords[index]) {
      path.push(coords[index])
    }
  }

  path.push(end)
  return dedupeAdjacentPoints(path)
}

function pointAtRouteRatio(coords: Array<[number, number]>, ratio: number): [number, number] {
  if (coords.length === 0) {
    return [0, 0]
  }

  if (coords.length === 1) {
    return coords[0]
  }

  const clampedRatio = Math.max(0, Math.min(ratio, 1))
  const segmentRatio = clampedRatio * (coords.length - 1)
  const index = Math.min(Math.floor(segmentRatio), coords.length - 2)
  const localRatio = segmentRatio - index
  const start = coords[index]
  const end = coords[index + 1]

  return [
    start[0] + (end[0] - start[0]) * localRatio,
    start[1] + (end[1] - start[1]) * localRatio,
  ]
}

function dedupeAdjacentPoints(path: Array<[number, number]>) {
  return path.filter((point, index) => {
    const previous = path[index - 1]
    return !previous || previous[0] !== point[0] || previous[1] !== point[1]
  })
}

function wrapRatio(ratio: number) {
  return ((ratio % 1) + 1) % 1
}
