import { mkdir, readFile, rm, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const frontendRoot = path.resolve(scriptDir, '..')
const sourceRoot = path.resolve(frontendRoot, 'public', 'geo')
const outputRoot = path.resolve(frontendRoot, 'dist', 'geo-cdn', 'v1')

const sourceIndexPath = path.join(sourceRoot, 'index.json')
const outputIndexPath = path.join(outputRoot, 'index.json')

await rm(outputRoot, { recursive: true, force: true })
await mkdir(outputRoot, { recursive: true })

const sourceIndex = await readJson(sourceIndexPath)

const chinaEntry = await writeEntry(sourceIndex.china, 'china')
const provinceEntries = await writeEntryRecord(sourceIndex.province, 'provinces')
const cityEntries = await writeEntryRecord(sourceIndex.city, 'cities')
const districtEntries = await buildDistrictEntries(sourceIndex.district, cityEntries)

await writeJson(outputIndexPath, {
  china: chinaEntry,
  province: provinceEntries,
  city: cityEntries,
  district: districtEntries,
})

const fileCount = 1 + Object.keys(provinceEntries).length + Object.keys(cityEntries).length + 1
console.log(`Geo CDN bundle generated: ${path.relative(frontendRoot, outputRoot)}`)
console.log(`Files: ${fileCount}`)
console.log('Upload this directory to the CDN root configured by VITE_GEO_CDN_BASE_URL.')

async function writeEntryRecord(record, folder) {
  const entries = await Promise.all(
    Object.entries(record).map(async ([code, entry]) => [code, await writeEntry(entry, folder)]),
  )

  return Object.fromEntries(entries)
}

async function writeEntry(entry, folder) {
  const sourcePath = resolveGeoPath(entry.path)
  const outputPath = path.join(outputRoot, folder, `${entry.code}.json`)
  const geoJson = await readJson(sourcePath)

  await writeJson(outputPath, geoJson)

  return {
    code: String(entry.code),
    name: String(entry.name),
    path: `/geo/${folder}/${entry.code}.json`,
  }
}

async function buildDistrictEntries(districtRecord, cityEntries) {
  const districtToCity = new Map()

  await Promise.all(
    Object.values(cityEntries).map(async (cityEntry) => {
      const cityGeoJson = await readJson(path.join(outputRoot, cityEntry.path.replace(/^\/geo\//, '')))

      for (const feature of cityGeoJson.features ?? []) {
        const code = String(feature?.properties?.adcode ?? feature?.properties?.id ?? '')

        if (code) {
          districtToCity.set(code, cityEntry)
        }
      }
    }),
  )

  return Object.fromEntries(
    Object.entries(districtRecord).map(([code, entry]) => {
      const cityEntry = districtToCity.get(code)

      return [
        code,
        {
          code: String(entry.code),
          name: String(entry.name),
          path: cityEntry?.path ?? entry.path,
        },
      ]
    }),
  )
}

function resolveGeoPath(geoPath) {
  return path.join(sourceRoot, geoPath.replace(/^\/geo\//, '').replace(/^\/+/, ''))
}

async function readJson(filePath) {
  const content = await readFile(filePath, 'utf8')
  return JSON.parse(content.replace(/^\uFEFF/, ''))
}

async function writeJson(filePath, payload) {
  await mkdir(path.dirname(filePath), { recursive: true })
  await writeFile(filePath, `${JSON.stringify(payload)}\n`, 'utf8')
}
