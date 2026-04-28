<script setup lang="ts">
import { MapChart } from 'echarts/charts'
import { GeoComponent, TooltipComponent } from 'echarts/components'
import { init, registerMap, use, type ECharts } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { resolveGeoAssetPath } from '../service'
import type { GeoIndex, GeoIndexEntry, MapRegion, MapTooltipData } from '../types'

use([MapChart, GeoComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{
  region: MapRegion
  focusCode?: string
  geoIndex: GeoIndex | null
  tooltipData: MapTooltipData | null
  loading: boolean
}>()

const emit = defineEmits<{
  regionClick: [payload: { code: string; name: string }]
  regionHover: [payload: { code: string; name: string }]
  resetView: []
}>()

type GeoFeature = {
  type?: string
  geometry?: {
    type?: string
    coordinates?: unknown
  }
  properties?: Record<string, unknown>
}

type GeoJsonLike = {
  type?: string
  features?: GeoFeature[]
}

type Bounds = {
  minLng: number
  maxLng: number
  minLat: number
  maxLat: number
}

type FeatureLayer = 'province' | 'city' | 'district'

type MapSeriesDataItem = {
  name: string
  code: string
  showLabel: boolean
}

const chartEl = ref<HTMLDivElement | null>(null)
const zoomRatio = ref(100)
let chart: ECharts | null = null
let resizeObserver: ResizeObserver | null = null
let currentZoom = 1
let baseGeoJson: GeoJsonLike | null = null
let activeMapName = ''
let detailTimer: number | undefined
let renderVersion = 0
let wheelTarget: HTMLDivElement | null = null
let visibleLabelCodes = new Set<string>()
let visibleLabelNames = new Set<string>()

const geoJsonCache = new Map<string, GeoJsonLike>()
const minZoom = 1
const maxZoom = 140
const wheelZoomStep = 1.18

watch(
  () => [props.region.code, props.region.path, props.focusCode, props.geoIndex],
  async () => {
    await nextTick()
    await renderBaseMap()
  },
  { immediate: true },
)

watch(
  () => props.tooltipData,
  () => {
    chart?.setOption({ tooltip: buildTooltipOption() })
  },
)

onBeforeUnmount(() => {
  clearDetailTimer()
  detachWheelZoom()
  resizeObserver?.disconnect()
  chart?.dispose()
})

async function renderBaseMap() {
  if (!chartEl.value) {
    return
  }

  ensureChart()
  clearDetailTimer()

  const version = ++renderVersion
  const geoJson = await fetchGeoJson(props.region.path)

  if (version !== renderVersion) {
    return
  }

  baseGeoJson = geoJson
  activeMapName = props.region.code

  const view = resolveView(geoJson)
  currentZoom = view.zoom
  syncZoomRatio()
  registerMap(activeMapName, toMapInput(geoJson))
  chart?.setOption(buildMapOption(activeMapName, markFeatures(geoJson.features ?? [], baseFeatureLayer()), view.center), true)
  scheduleDetailUpdate()
}

function ensureChart() {
  if (!chartEl.value || chart) {
    return
  }

  chart = init(chartEl.value, undefined, { renderer: 'canvas' })
  chart.on('click', (params) => {
    const code = readRegionCode(params.data, params.name)

    if (code) {
      emit('regionClick', { code, name: String(params.name ?? '') })
    }
  })
  chart.on('mouseover', (params) => {
    const code = readRegionCode(params.data, params.name)

    if (code) {
      emit('regionHover', { code, name: String(params.name ?? '') })
    }
  })
  chart.on('georoam', () => {
    currentZoom = readChartZoom()
    syncZoomRatio()
    scheduleDetailUpdate()
  })
  resizeObserver = new ResizeObserver(() => {
    chart?.resize()
    scheduleDetailUpdate()
  })
  resizeObserver.observe(chartEl.value)
  attachWheelZoom(chartEl.value)
}

async function fetchGeoJson(path: string): Promise<GeoJsonLike> {
  const cached = geoJsonCache.get(path)

  if (cached) {
    return cached
  }

  const response = await fetch(resolveGeoAssetPath(path))

  if (!response.ok) {
    throw new Error('地图数据加载失败')
  }

  const geoJson = (await response.json()) as GeoJsonLike
  geoJsonCache.set(path, geoJson)
  return geoJson
}

function buildMapOption(mapName: string, features: GeoFeature[], center?: number[]) {
  const seriesData = buildSeriesData(features)

  syncVisibleLabelKeys(seriesData)

  return {
    backgroundColor: 'transparent',
    tooltip: buildTooltipOption(),
    geo: {
      map: mapName,
      roam: 'move',
      zoom: currentZoom,
      center,
      scaleLimit: {
        min: minZoom,
        max: maxZoom,
      },
      layoutCenter: ['50%', '51%'],
      layoutSize: props.region.level === 'country' ? '104%' : '96%',
      itemStyle: {
        areaColor: '#0b1a24',
        borderColor: 'rgba(110, 202, 212, 0.44)',
        borderWidth: 1,
        shadowBlur: 18,
        shadowColor: 'rgba(110, 202, 212, 0.18)',
      },
      emphasis: {
        itemStyle: {
          areaColor: '#143441',
          borderColor: '#71ccd6',
          borderWidth: 1.6,
        },
        label: {
          show: true,
          formatter: formatRegionLabel,
          color: '#e9f7f8',
          textBorderColor: 'rgba(5, 10, 16, 0.82)',
          textBorderWidth: 2,
        },
      },
      label: {
        show: true,
        formatter: formatRegionLabel,
        color: 'rgba(230, 241, 244, 0.76)',
        fontSize: 11,
        textBorderColor: 'rgba(5, 10, 16, 0.82)',
        textBorderWidth: 2,
      },
    },
    series: [
      {
        type: 'map',
        map: mapName,
        geoIndex: 0,
        selectedMode: false,
        label: {
          show: true,
          formatter: formatRegionLabel,
          color: 'rgba(230, 241, 244, 0.76)',
          fontSize: 11,
          textBorderColor: 'rgba(5, 10, 16, 0.82)',
          textBorderWidth: 2,
        },
        emphasis: {
          label: {
            show: true,
            formatter: formatRegionLabel,
            color: '#e9f7f8',
          },
        },
        data: seriesData,
      },
    ],
  }
}

function buildTooltipOption() {
  return {
    trigger: 'item',
    borderWidth: 1,
    borderColor: 'rgba(110, 202, 212, 0.34)',
    backgroundColor: 'rgba(5, 10, 16, 0.92)',
    textStyle: {
      color: '#d8e6ea',
      fontSize: 12,
    },
    extraCssText: 'box-shadow:0 16px 36px rgba(0,0,0,.38);',
    formatter: (params: { name?: string; data?: { code?: string } }) => {
      const data = props.tooltipData
      const name = String(params.name ?? '')

      if (props.loading && data?.areaName !== name) {
        return `<strong>${name}</strong><br/>正在读取区域数据...`
      }

      if (!data || data.areaName !== name) {
        return `<strong>${name}</strong><br/>悬浮后加载区间平均数据`
      }

      return [
        `<strong>${data.areaName}</strong>`,
        `传感器总数：${data.sensorNumbers}`,
        `异常传感器：${data.abnormalSensorNumbers}`,
        `告警数量：${data.warnings}`,
        `平均压力：${formatMetric(data.pressure)}`,
        `平均流量：${formatMetric(data.flow)}`,
        `平均温度：${formatMetric(data.temperature)}`,
        `平均震动：${formatMetric(data.vibration)}`,
      ].join('<br/>')
    },
  }
}

function buildSeriesData(features: GeoFeature[]) {
  return features.map((feature) => {
    const properties = feature.properties ?? {}
    const code = String(properties.adcode ?? properties.id ?? '')
    const layer = String(properties.__layer ?? baseFeatureLayer()) as FeatureLayer
    const focused = props.focusCode === code

    return {
      name: String(properties.name ?? ''),
      code,
      showLabel: shouldShowLabel(layer),
      value: syntheticRegionValue(code),
      label: {
        color: layer === 'district' ? 'rgba(233, 247, 248, 0.84)' : 'rgba(230, 241, 244, 0.7)',
        fontSize: layer === 'district' ? 10 : 11,
      },
      itemStyle: layer !== baseFeatureLayer()
        ? {
            areaColor: 'rgba(10, 28, 38, 0.2)',
            borderColor: layer === 'district' ? 'rgba(233, 247, 248, 0.42)' : 'rgba(233, 247, 248, 0.34)',
            borderWidth: layer === 'district' ? 0.7 : 0.85,
          }
        : focused
          ? {
              areaColor: '#163d49',
              borderColor: '#e7682d',
              borderWidth: 1.8,
              shadowBlur: 24,
              shadowColor: 'rgba(231, 104, 45, 0.34)',
            }
          : undefined,
    }
  })
}

function formatRegionLabel(params: { name?: string; data?: MapSeriesDataItem }) {
  const code = params.data?.code
  const name = String(params.name ?? '')

  if (params.data?.showLabel || (code && visibleLabelCodes.has(code)) || visibleLabelNames.has(name)) {
    return name
  }

  return ''
}

function syncVisibleLabelKeys(seriesData: MapSeriesDataItem[]) {
  const visibleItems = seriesData.filter((item) => item.showLabel)
  visibleLabelCodes = new Set(visibleItems.map((item) => item.code))
  visibleLabelNames = new Set(visibleItems.map((item) => item.name))
}

function scheduleDetailUpdate() {
  clearDetailTimer()
  detailTimer = window.setTimeout(() => {
    void updateDetailLayer()
  }, 120)
}

function attachWheelZoom(target: HTMLDivElement) {
  detachWheelZoom()
  wheelTarget = target
  wheelTarget.addEventListener('wheel', handleWheelZoom, { passive: false })
}

function detachWheelZoom() {
  if (wheelTarget) {
    wheelTarget.removeEventListener('wheel', handleWheelZoom)
    wheelTarget = null
  }
}

function handleWheelZoom(event: WheelEvent) {
  if (!chart) {
    return
  }

  event.preventDefault()
  const factor = event.deltaY < 0 ? wheelZoomStep : 1 / wheelZoomStep
  applyZoom(currentZoom * factor)
}

function applyZoom(value: number) {
  if (!chart) {
    return
  }

  currentZoom = clampZoom(value)
  syncZoomRatio()
  chart.setOption({
    geo: {
      zoom: currentZoom,
      center: readChartCenter(),
    },
  })
  scheduleDetailUpdate()
}

function zoomIn() {
  applyZoom(currentZoom * wheelZoomStep)
}

function zoomOut() {
  applyZoom(currentZoom / wheelZoomStep)
}

function syncZoomRatio() {
  zoomRatio.value = Math.round(currentZoom * 100)
}

function scaleBarLabel() {
  if (currentZoom >= 20) return '2 km'
  if (currentZoom >= 8) return '10 km'
  if (currentZoom >= 3) return '50 km'
  return '200 km'
}

async function updateDetailLayer() {
  const baseFeatures = baseGeoJson?.features ?? []

  if (!chart || !baseGeoJson || !baseFeatures.length) {
    return
  }

  const center = readChartCenter()
  const visibleFeatures = await buildVisibleFeatures(baseFeatures)
  const detailKey = visibleFeatures
    .filter((feature) => feature.properties?.__layer !== baseFeatureLayer())
    .map(readFeatureCode)
    .sort()
    .join(',')

  if (!detailKey) {
    activeMapName = props.region.code
    registerMap(activeMapName, toMapInput(baseGeoJson))
    chart.setOption(buildMapOption(activeMapName, markFeatures(baseFeatures, baseFeatureLayer()), center))
    return
  }

  activeMapName = `${props.region.code}-detail-${hashText(detailKey)}`

  const mergedGeoJson = {
    type: 'FeatureCollection',
    features: visibleFeatures,
  }
  registerMap(activeMapName, toMapInput(mergedGeoJson))
  chart.setOption(buildMapOption(activeMapName, mergedGeoJson.features, center))
}

async function buildVisibleFeatures(baseFeatures: GeoFeature[]) {
  const baseLayer = baseFeatureLayer()
  const features = markFeatures(baseFeatures, baseLayer)

  if (props.region.level === 'country' && currentZoom >= cityBoundaryZoom()) {
    const cityFeatures = await loadChildFeatures(
      findVisibleParentFeatures(baseFeatures, 'province'),
      'city',
    )
    features.push(...cityFeatures)

    if (currentZoom >= districtBoundaryZoom()) {
      features.push(...await loadChildFeatures(findVisibleParentFeatures(cityFeatures, 'city'), 'district'))
    }
  }

  if (props.region.level === 'province' && currentZoom >= districtBoundaryZoom()) {
    features.push(...await loadChildFeatures(findVisibleParentFeatures(baseFeatures, 'city'), 'district'))
  }

  return features
}

async function loadChildFeatures(parentFeatures: GeoFeature[], targetLayer: FeatureLayer) {
  const entries = parentFeatures
    .map((feature) => resolveChildEntry(feature, targetLayer))
    .filter(Boolean) as GeoIndexEntry[]

  if (!entries.length) {
    return []
  }

  const geoJsons = await Promise.all(entries.map((entry) => fetchGeoJson(entry.path).catch(() => null)))

  return geoJsons
    .filter(Boolean)
    .flatMap((geoJson) => markFeatures((geoJson as GeoJsonLike).features ?? [], targetLayer))
}

function findVisibleParentFeatures(features: GeoFeature[], layer: FeatureLayer) {
  const bounds = readViewportBounds()
  const limit = maxVisibleParents(layer)

  if (!bounds) {
    return features.slice(0, limit)
  }

  const centerLng = (bounds.minLng + bounds.maxLng) / 2
  const centerLat = (bounds.minLat + bounds.maxLat) / 2

  return features
    .filter((feature) => {
      const featureBounds = readFeatureBounds(feature)

      return featureBounds ? boundsIntersect(bounds, featureBounds) : centerInBounds(readFeatureCenter(feature), bounds)
    })
    .sort((a, b) => distanceToCenter(a, centerLng, centerLat) - distanceToCenter(b, centerLng, centerLat))
    .slice(0, limit)
}

function resolveChildEntry(feature: GeoFeature, targetLayer: FeatureLayer) {
  const code = readFeatureCode(feature)

  if (!code || !props.geoIndex) {
    return null
  }

  if (targetLayer === 'city') {
    return props.geoIndex.province[code] ?? null
  }

  if (targetLayer === 'district') {
    return props.geoIndex.city[code] ?? null
  }

  return null
}

function baseFeatureLayer(): FeatureLayer {
  if (props.region.level === 'country') {
    return 'province'
  }

  if (props.region.level === 'province') {
    return 'city'
  }

  return 'district'
}

function shouldShowLabel(layer: FeatureLayer) {
  if (layer === 'province') {
    return props.region.level === 'country' && currentZoom >= provinceLabelZoom() && currentZoom < cityLabelZoom()
  }

  if (layer === 'city') {
    if (props.region.level === 'country') {
      return currentZoom >= cityLabelZoom() && currentZoom < districtLabelZoom()
    }

    return props.region.level === 'province' && currentZoom >= cityLabelZoom() && currentZoom < districtLabelZoom()
  }

  return currentZoom >= districtLabelZoom()
}

function provinceLabelZoom() {
  return 2.15
}

function cityBoundaryZoom() {
  return props.region.level === 'country' ? 5.3 : 0
}

function cityLabelZoom() {
  return props.region.level === 'country' ? 8.2 : 3.4
}

function districtBoundaryZoom() {
  return props.region.level === 'country' ? 13.5 : 7.2
}

function districtLabelZoom() {
  return props.region.level === 'country' ? 22 : props.region.level === 'province' ? 13 : 5.2
}

function maxVisibleParents(layer: FeatureLayer) {
  if (layer === 'province') {
    return 8
  }

  if (layer === 'city') {
    return props.region.level === 'country' ? 12 : 18
  }

  return 24
}

function resolveView(geoJson: GeoJsonLike) {
  const center = findFeatureCenter(geoJson, props.focusCode)

  if (!props.focusCode || !center) {
    return {
      center: undefined,
      zoom: props.region.level === 'country' ? 1.05 : 1.15,
    }
  }

  const zoomByLevel = props.region.level === 'country' ? 3.25 : props.region.level === 'province' ? 4.2 : 6.4

  return {
    center,
    zoom: zoomByLevel,
  }
}

function findFeatureCenter(geoJson: GeoJsonLike, code?: string) {
  if (!code) {
    return undefined
  }

  const feature = (geoJson.features ?? []).find((item) => readFeatureCode(item) === code)

  return readFeatureCenter(feature)
}

function readFeatureCenter(feature?: GeoFeature) {
  const properties = feature?.properties ?? {}
  const center = properties.center ?? properties.cp

  if (Array.isArray(center)) {
    return [Number(center[0]), Number(center[1])]
  }

  const bounds = feature ? readFeatureBounds(feature) : null

  return bounds ? [(bounds.minLng + bounds.maxLng) / 2, (bounds.minLat + bounds.maxLat) / 2] : undefined
}

function readFeatureCode(feature: GeoFeature) {
  const properties = feature.properties ?? {}

  return String(properties.adcode ?? properties.id ?? '')
}

function readViewportBounds() {
  if (!chart) {
    return null
  }

  const width = chart.getWidth()
  const height = chart.getHeight()
  const points = [
    [0, 0],
    [width, 0],
    [width, height],
    [0, height],
  ].map((point) => chart?.convertFromPixel({ geoIndex: 0 }, point) as number[] | undefined)
    .filter((point): point is number[] => Array.isArray(point) && Number.isFinite(point[0]) && Number.isFinite(point[1]))

  if (!points.length) {
    return null
  }

  const lngValues = points.map((point) => point[0])
  const latValues = points.map((point) => point[1])
  const bounds = {
    minLng: Math.min(...lngValues),
    maxLng: Math.max(...lngValues),
    minLat: Math.min(...latValues),
    maxLat: Math.max(...latValues),
  }
  const paddingLng = (bounds.maxLng - bounds.minLng) * 0.12
  const paddingLat = (bounds.maxLat - bounds.minLat) * 0.12

  return {
    minLng: bounds.minLng - paddingLng,
    maxLng: bounds.maxLng + paddingLng,
    minLat: bounds.minLat - paddingLat,
    maxLat: bounds.maxLat + paddingLat,
  }
}

function readFeatureBounds(feature: GeoFeature) {
  const points = flattenCoordinates(feature.geometry?.coordinates)

  if (!points.length) {
    return null
  }

  const lngValues = points.map((point) => point[0])
  const latValues = points.map((point) => point[1])

  return {
    minLng: Math.min(...lngValues),
    maxLng: Math.max(...lngValues),
    minLat: Math.min(...latValues),
    maxLat: Math.max(...latValues),
  }
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

function boundsIntersect(a: Bounds, b: Bounds) {
  return a.minLng <= b.maxLng && a.maxLng >= b.minLng && a.minLat <= b.maxLat && a.maxLat >= b.minLat
}

function centerInBounds(center: number[] | undefined, bounds: Bounds) {
  return Boolean(center && center[0] >= bounds.minLng && center[0] <= bounds.maxLng && center[1] >= bounds.minLat && center[1] <= bounds.maxLat)
}

function distanceToCenter(feature: GeoFeature, lng: number, lat: number) {
  const center = readFeatureCenter(feature)

  if (!center) {
    return Number.MAX_SAFE_INTEGER
  }

  return Math.hypot(center[0] - lng, center[1] - lat)
}

function markFeatures(features: GeoFeature[], layer: FeatureLayer) {
  return features.map((feature) => ({
    ...feature,
    properties: {
      ...(feature.properties ?? {}),
      __layer: layer,
    },
  }))
}

function readChartZoom() {
  const option = chart?.getOption() as { geo?: Array<{ zoom?: number }> } | undefined

  return Number(option?.geo?.[0]?.zoom ?? currentZoom)
}

function clampZoom(value: number) {
  return Math.min(maxZoom, Math.max(minZoom, value))
}

function readChartCenter() {
  const option = chart?.getOption() as { geo?: Array<{ center?: number[] }> } | undefined
  const center = option?.geo?.[0]?.center

  return Array.isArray(center) ? center : undefined
}

function clearDetailTimer() {
  if (detailTimer) {
    window.clearTimeout(detailTimer)
    detailTimer = undefined
  }
}

function syntheticRegionValue(code: string) {
  const seed = Number(code.slice(-4)) || 100

  return seed % 90 + 10
}

function readRegionCode(data: unknown, name: unknown) {
  if (data && typeof data === 'object' && 'code' in data) {
    return String((data as { code?: string }).code ?? '')
  }

  return typeof name === 'string' ? name : ''
}

function formatMetric(value: number) {
  return Number.isFinite(value) ? value.toFixed(2) : '-'
}

function hashText(value: string) {
  let hash = 0

  for (let index = 0; index < value.length; index += 1) {
    hash = (hash * 31 + value.charCodeAt(index)) >>> 0
  }

  return hash.toString(36)
}

function toMapInput(geoJson: GeoJsonLike) {
  return geoJson as Parameters<typeof registerMap>[1]
}
</script>

<template>
  <section class="panel geo-map-panel">
    <div class="panel-head">
      <h2 class="panel-title">区域监控主画面</h2>
      <span class="eyebrow">{{ region.name }}</span>
    </div>
    <div class="geo-map-panel__body">
      <div class="geo-map-panel__grid"></div>
      <div ref="chartEl" class="geo-map-panel__chart"></div>
      <div class="geo-map-panel__toolbar" aria-label="地图工具">
        <button type="button" title="放大地图" @click="zoomIn">+</button>
        <button type="button" title="缩小地图" @click="zoomOut">-</button>
        <button type="button" title="归位到全国视图" @click="emit('resetView')">归位</button>
      </div>
      <div class="geo-map-panel__scale" aria-label="地图比例尺">
        <span>{{ zoomRatio }}%</span>
        <i></i>
        <strong>{{ scaleBarLabel() }}</strong>
      </div>
    </div>
  </section>
</template>

<style scoped>
.geo-map-panel {
  display: grid;
  grid-template-rows: 48px minmax(0, 1fr);
  background:
    radial-gradient(circle at center, rgba(110, 202, 212, 0.07), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.015), transparent),
    var(--color-panel-2);
}

.geo-map-panel__body {
  position: relative;
  min-height: 0;
  overflow: hidden;
}

.geo-map-panel__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(110, 202, 212, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(110, 202, 212, 0.04) 1px, transparent 1px);
  background-size: 32px 32px;
  mask-image: radial-gradient(circle at center, rgba(0, 0, 0, 0.9), transparent 92%);
  animation: map-grid-drift 18s linear infinite;
}

.geo-map-panel__chart {
  position: absolute;
  inset: 12px;
  min-width: 0;
  min-height: 0;
}

.geo-map-panel__scale {
  position: absolute;
  inset: auto 22px 22px auto;
  min-inline-size: 138px;
  padding: 10px 12px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 10px;
  border: 1px solid rgba(143, 166, 182, 0.2);
  background: rgba(8, 13, 19, 0.84);
  backdrop-filter: blur(14px);
}

.geo-map-panel__toolbar {
  position: absolute;
  inset: 18px 20px auto auto;
  z-index: 3;
  display: flex;
  gap: 6px;
}

.geo-map-panel__toolbar button {
  min-height: 34px;
  min-width: 34px;
  padding: 0 10px;
  border: 1px solid rgba(110, 202, 212, 0.28);
  color: var(--color-text);
  background: rgba(8, 13, 19, 0.82);
  backdrop-filter: blur(12px);
}

.geo-map-panel__toolbar button:hover {
  border-color: var(--color-line-strong);
  background: rgba(110, 202, 212, 0.12);
}

.geo-map-panel__scale span,
.geo-map-panel__scale strong {
  font-size: var(--text-meta);
  letter-spacing: var(--tracking-panel);
  text-transform: uppercase;
}

.geo-map-panel__scale span,
.geo-map-panel__scale strong {
  color: var(--color-text-muted);
}

.geo-map-panel__scale i {
  display: block;
  height: 8px;
  border-inline: 1px solid rgba(110, 202, 212, 0.72);
  border-bottom: 2px solid rgba(110, 202, 212, 0.72);
}

@keyframes map-grid-drift {
  from {
    background-position: 0 0;
  }

  to {
    background-position: 32px 32px;
  }
}

@media (max-width: 719px) {
  .geo-map-panel__chart {
    position: relative;
    inset: auto;
    min-block-size: 360px;
  }

  .geo-map-panel__scale {
    position: relative;
    inset: auto;
    margin: 0 var(--space-3) var(--space-3);
  }

  .geo-map-panel__toolbar {
    inset: 14px 14px auto auto;
  }
}
</style>
