import { mkdir, readFile, rm, writeFile } from 'node:fs/promises'
import { dirname, join, resolve } from 'node:path'
import geojsonvt from 'geojson-vt'
import vtpbf from 'vt-pbf'

const rootDir = resolve(import.meta.dirname, '..')
const geoDir = join(rootDir, 'public', 'geo')
const outputDir = resolveOutputDir()
const tileDir = join(outputDir, 'admin')
const indexPath = join(geoDir, 'index.json')
const minZoom = 3
const maxZoom = 9
const tileCoverageBounds = [72, 12, 137, 56]
const tileOptions = {
  version: 2,
  extent: 4096,
}

const levelRank = {
  province: 1,
  city: 2,
  district: 3,
}

function stripBom(value) {
  return value.replace(/^\uFEFF/, '')
}

function resolveOutputDir() {
  const outFlagIndex = process.argv.indexOf('--out')
  const outValue = outFlagIndex >= 0
    ? process.argv[outFlagIndex + 1]
    : process.argv.find((arg) => arg.startsWith('--out='))?.slice('--out='.length)

  return outValue ? resolve(rootDir, outValue) : join(rootDir, 'dist', 'geo-tiles', 'v1')
}

async function readJson(path) {
  return JSON.parse(stripBom(await readFile(path, 'utf8')))
}

function resolveGeoPath(path) {
  return join(rootDir, 'public', path.replace(/^\/+/, ''))
}

function readCode(properties, fallbackCode) {
  return String(properties?.adcode ?? properties?.code ?? fallbackCode ?? '')
}

function readName(properties, fallbackName) {
  return String(properties?.name ?? fallbackName ?? '')
}

function readLevel(properties, fallbackLevel) {
  const rawLevel = String(properties?.level ?? fallbackLevel ?? '').toLowerCase()

  if (rawLevel.includes('province')) return 'province'
  if (rawLevel.includes('city')) return 'city'
  if (rawLevel.includes('district')) return 'district'

  return fallbackLevel
}

function readParentCode(properties, code) {
  const parent = properties?.parent
  const parentCode = typeof parent === 'object' && parent ? parent.adcode : properties?.parentCode

  if (parentCode) {
    return String(parentCode)
  }

  if (/^\d{2}0000$/.test(code)) return '100000'
  if (/^\d{4}00$/.test(code)) return `${code.slice(0, 2)}0000`
  if (/^\d{6}$/.test(code)) return `${code.slice(0, 4)}00`

  return ''
}

function updateBounds(bounds, coordinates) {
  if (!Array.isArray(coordinates)) return

  if (typeof coordinates[0] === 'number' && typeof coordinates[1] === 'number') {
    const [lng, lat] = coordinates
    bounds[0] = Math.min(bounds[0], lng)
    bounds[1] = Math.min(bounds[1], lat)
    bounds[2] = Math.max(bounds[2], lng)
    bounds[3] = Math.max(bounds[3], lat)
    return
  }

  for (const child of coordinates) {
    updateBounds(bounds, child)
  }
}

function featureBounds(feature) {
  const bounds = [Infinity, Infinity, -Infinity, -Infinity]
  updateBounds(bounds, feature.geometry?.coordinates)

  if (!Number.isFinite(bounds[0])) {
    return null
  }

  return bounds
}

function centerOfBounds(bounds) {
  return [
    Number(((bounds[0] + bounds[2]) / 2).toFixed(6)),
    Number(((bounds[1] + bounds[3]) / 2).toFixed(6)),
  ]
}

function normalizeFeature(feature, entry, fallbackLevel) {
  const properties = feature.properties ?? {}
  const code = readCode(properties, entry.code)
  const name = readName(properties, entry.name)
  const level = readLevel(properties, fallbackLevel)
  const parentCode = readParentCode(properties, code)

  if (!code || !name || !levelRank[level] || !feature.geometry) {
    return null
  }

  return {
    type: 'Feature',
    geometry: feature.geometry,
    properties: {
      code,
      name,
      level,
      parentCode,
    },
  }
}

async function loadFeatures(index) {
  const featuresByCode = new Map()
  const chinaGeoJson = await readJson(resolveGeoPath(index.china.path))

  for (const feature of chinaGeoJson.features ?? []) {
    const normalized = normalizeFeature(feature, {
      code: readCode(feature.properties, ''),
      name: readName(feature.properties, ''),
    }, 'province')

    if (normalized) {
      featuresByCode.set(normalized.properties.code, normalized)
    }
  }

  for (const [level, record] of [
    ['city', index.province ?? {}],
    ['district', index.city ?? {}],
  ]) {
    for (const entry of Object.values(record)) {
      const path = resolveGeoPath(entry.path)

      try {
        const geoJson = await readJson(path)

        for (const feature of geoJson.features ?? []) {
          const normalized = normalizeFeature(feature, entry, level)

          if (normalized) {
            featuresByCode.set(normalized.properties.code, normalized)
          }
        }
      } catch (error) {
        if (error?.code !== 'ENOENT') {
          throw error
        }
      }
    }
  }

  return [...featuresByCode.values()]
}

function buildAdminIndex(features) {
  const entries = {}

  for (const feature of features) {
    const bounds = featureBounds(feature)

    if (!bounds) continue

    const props = feature.properties
    entries[props.code] = {
      code: props.code,
      name: props.name,
      level: props.level,
      parentCode: props.parentCode,
      bbox: bounds.map((value) => Number(value.toFixed(6))),
      center: centerOfBounds(bounds),
    }
  }

  return {
    generatedAt: new Date().toISOString(),
    source: 'frontend/public/geo',
    tileUrl: '/geo-tiles/v1/admin/{z}/{x}/{y}.pbf',
    entries,
  }
}

async function writeTiles(features) {
  const tileIndex = geojsonvt({ type: 'FeatureCollection', features }, {
    minZoom,
    maxZoom,
    indexMaxZoom: maxZoom,
    tolerance: 4,
    extent: 4096,
    buffer: 64,
  })

  let featureTileCount = 0
  let emptyTileCount = 0
  for (const coord of enumerateCoveredTiles(tileCoverageBounds)) {
    const tile = tileIndex.getTile(coord.z, coord.x, coord.y)
    const hasFeatures = Boolean(tile?.features?.length)
    const buffer = hasFeatures
      ? vtpbf.fromGeojsonVt({ admin: tile }, tileOptions)
      : vtpbf.fromGeojsonVt({ admin: { features: [] } }, tileOptions)
    const path = join(tileDir, String(coord.z), String(coord.x), `${coord.y}.pbf`)

    await mkdir(dirname(path), { recursive: true })
    await writeFile(path, buffer)
    if (hasFeatures) {
      featureTileCount += 1
    } else {
      emptyTileCount += 1
    }
  }

  return { featureTileCount, emptyTileCount }
}

function enumerateCoveredTiles(bounds) {
  const [west, south, east, north] = bounds
  const coords = []

  for (let z = minZoom; z <= maxZoom; z += 1) {
    const minX = longitudeToTileX(west, z)
    const maxX = longitudeToTileX(east, z)
    const minY = latitudeToTileY(north, z)
    const maxY = latitudeToTileY(south, z)

    for (let x = minX; x <= maxX; x += 1) {
      for (let y = minY; y <= maxY; y += 1) {
        coords.push({ z, x, y })
      }
    }
  }

  return coords
}

function longitudeToTileX(longitude, zoom) {
  return clampTileIndex(Math.floor(((longitude + 180) / 360) * 2 ** zoom), zoom)
}

function latitudeToTileY(latitude, zoom) {
  const latRad = latitude * Math.PI / 180
  const y = (1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * 2 ** zoom
  return clampTileIndex(Math.floor(y), zoom)
}

function clampTileIndex(value, zoom) {
  return Math.min(Math.max(value, 0), 2 ** zoom - 1)
}

async function main() {
  const index = await readJson(indexPath)
  const features = await loadFeatures(index)

  if (!features.length) {
    throw new Error('No administrative GeoJSON features found.')
  }

  await rm(outputDir, { recursive: true, force: true })
  await mkdir(outputDir, { recursive: true })
  await writeFile(join(outputDir, 'admin-index.json'), `${JSON.stringify(buildAdminIndex(features), null, 2)}\n`)

  const { featureTileCount, emptyTileCount } = await writeTiles(features)
  if (!featureTileCount) {
    throw new Error('No vector tiles were generated.')
  }

  console.log(`Dashboard map tiles generated: ${features.length} features, ${featureTileCount} feature tiles, ${emptyTileCount} empty tiles`)
  console.log(`Output: ${outputDir}`)
}

await main()
