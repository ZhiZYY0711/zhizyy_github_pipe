export type FeatureBounds = {
  minLng: number
  maxLng: number
  minLat: number
  maxLat: number
}

type GeoFeatureLike = {
  geometry?: {
    coordinates?: unknown
  }
  properties?: Record<string, unknown>
}

const boundsCache = new WeakMap<object, FeatureBounds | null>()
const centerCache = new WeakMap<object, number[] | undefined>()
const layerCache = new Map<string, string>()
const maxLayerEntries = 48

export function getFeatureBounds(feature: GeoFeatureLike): FeatureBounds | null {
  if (boundsCache.has(feature)) {
    return boundsCache.get(feature) ?? null
  }

  const points = flattenCoordinates(feature.geometry?.coordinates)
  const bounds = points.length
    ? {
        minLng: Math.min(...points.map((point) => point[0])),
        maxLng: Math.max(...points.map((point) => point[0])),
        minLat: Math.min(...points.map((point) => point[1])),
        maxLat: Math.max(...points.map((point) => point[1])),
      }
    : null

  boundsCache.set(feature, bounds)
  return bounds
}

export function getFeatureCenter(feature: GeoFeatureLike): number[] | undefined {
  if (centerCache.has(feature)) {
    return centerCache.get(feature)
  }

  const center = feature.properties?.center ?? feature.properties?.cp
  if (Array.isArray(center)) {
    const value = [Number(center[0]), Number(center[1])]
    centerCache.set(feature, value)
    return value
  }

  const bounds = getFeatureBounds(feature)
  const value = bounds
    ? [(bounds.minLng + bounds.maxLng) / 2, (bounds.minLat + bounds.maxLat) / 2]
    : undefined

  centerCache.set(feature, value)
  return value
}

export function rememberLayer(key: string, value: string) {
  if (layerCache.has(key)) {
    layerCache.delete(key)
  }

  layerCache.set(key, value)

  while (layerCache.size > maxLayerEntries) {
    const oldestKey = layerCache.keys().next().value
    if (!oldestKey) {
      return
    }
    layerCache.delete(oldestKey)
  }
}

export function readLayer(key: string) {
  const value = layerCache.get(key)

  if (value) {
    layerCache.delete(key)
    layerCache.set(key, value)
  }

  return value
}

function flattenCoordinates(coordinates: unknown): number[][] {
  if (!Array.isArray(coordinates)) {
    return []
  }

  if (typeof coordinates[0] === 'number' && typeof coordinates[1] === 'number') {
    return [[Number(coordinates[0]), Number(coordinates[1])]]
  }

  return coordinates.flatMap((item) => flattenCoordinates(item))
}
