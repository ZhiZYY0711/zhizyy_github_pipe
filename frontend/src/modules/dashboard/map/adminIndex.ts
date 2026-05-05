import type { AdminBoundaryIndex } from '../types'

const emptyAdminIndex: AdminBoundaryIndex = {
  entries: {},
}

const geoTilesBaseUrl = String(import.meta.env.VITE_GEO_TILES_BASE_URL ?? '/geo-tiles/v1').replace(/\/+$/, '')

export function resolveGeoTileAssetPath(path: string) {
  if (isAbsoluteUrl(path)) {
    return path
  }

  const relativePath = path
    .replace(/^\/+/, '')
    .replace(/^geo-tiles\/v1\//, '')

  const absoluteBaseUrl = new URL(`${geoTilesBaseUrl}/`, window.location.href).href.replace(/\/+$/, '')
  return `${absoluteBaseUrl}/${relativePath}`
}

export async function loadAdminBoundaryIndex() {
  try {
    const response = await fetch(resolveGeoTileAssetPath('/geo-tiles/v1/admin-index.json'))

    if (!response.ok) {
      return emptyAdminIndex
    }

    return await response.json() as AdminBoundaryIndex
  } catch {
    return emptyAdminIndex
  }
}

function isAbsoluteUrl(path: string) {
  return /^(?:[a-z]+:)?\/\//i.test(path) || path.startsWith('data:') || path.startsWith('blob:')
}
