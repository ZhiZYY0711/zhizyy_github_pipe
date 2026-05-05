import { mkdtemp, readFile, rm } from 'node:fs/promises'
import { execFile } from 'node:child_process'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { promisify } from 'node:util'
import { afterEach, describe, expect, it } from 'vitest'

const tempDirs = []
const execFileAsync = promisify(execFile)

afterEach(async () => {
  await Promise.all(tempDirs.map((dir) => rm(dir, { recursive: true, force: true })))
  tempDirs.length = 0
})

describe('build-dashboard-map-tiles', () => {
  it('indexes all city boundaries from province geojson files', async () => {
    const outputDir = await mkdtemp(join(tmpdir(), 'dashboard-map-tiles-'))
    tempDirs.push(outputDir)

    await execFileAsync('node', ['scripts/build-dashboard-map-tiles.mjs', '--out', outputDir], {
      cwd: new URL('..', import.meta.url),
    })

    const sourceIndex = JSON.parse(stripBom(await readFile(new URL('../public/geo/index.json', import.meta.url), 'utf8')))
    const expectedCityCount = await countSourceFeatures(sourceIndex.province)
    const index = JSON.parse(await readFile(join(outputDir, 'admin-index.json'), 'utf8'))
    const entries = Object.values(index.entries)

    expect(entries.filter((entry) => entry.level === 'city')).toHaveLength(expectedCityCount)
    expect(index.entries['370100']).toMatchObject({
      code: '370100',
      name: '济南市',
      level: 'city',
      parentCode: '370000',
    })
  }, 30_000)
})

async function countSourceFeatures(record) {
  const codes = new Set()

  for (const entry of Object.values(record)) {
    const geoJson = JSON.parse(stripBom(await readFile(new URL(`../public/${entry.path.replace(/^\/+/, '')}`, import.meta.url), 'utf8')))
    for (const feature of geoJson.features ?? []) {
      if (feature.properties?.level !== 'city') {
        continue
      }

      const code = String(feature.properties?.adcode ?? feature.properties?.code ?? '')
      if (code) {
        codes.add(code)
      }
    }
  }

  return codes.size
}

function stripBom(value) {
  return value.replace(/^\uFEFF/, '')
}
