<script setup lang="ts">
import { EffectScatterChart, LinesChart, MapChart } from 'echarts/charts'
import { GeoComponent, TooltipComponent } from 'echarts/components'
import { init, registerMap, use, type ECharts } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { getFeatureBounds, getFeatureCenter, readLayer, rememberLayer, type FeatureBounds } from '../mapLayerCache'
import { resolveGeoAssetPath } from '../service'
import type {
  DashboardMapLayerKey,
  GeoIndex,
  GeoIndexEntry,
  MapRegion,
  MapTooltipData,
  PipelineNode,
  PipelineStatus,
  TrunkPipeline,
} from '../types'

use([MapChart, LinesChart, EffectScatterChart, GeoComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{
  region: MapRegion
  focusCode?: string
  geoIndex: GeoIndex | null
  tooltipData: MapTooltipData | null
  loading: boolean
  pipelines: readonly TrunkPipeline[]
  visibleLayers: Record<DashboardMapLayerKey, boolean>
  selectedPipelineId?: string
}>()

const emit = defineEmits<{
  regionClick: [payload: { code: string; name: string }]
  regionHover: [payload: { code: string; name: string }]
  pipelineClick: [pipeline: TrunkPipeline]
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

type Bounds = FeatureBounds

type FeatureLayer = 'province' | 'city' | 'district'

type MapSeriesDataItem = {
  name: string
  code: string
  showLabel: boolean
}

type PipelineLineDataItem = {
  name: string
  pipelineId: string
  coords: Array<[number, number]>
}

type PipelineNodeDataItem = {
  name: string
  pipelineId: string
  nodeId: string
  status: PipelineStatus
  value: [number, number, number]
}

const chartEl = ref<HTMLDivElement | null>(null)
const zoomRatio = ref(100)
let chart: ECharts | null = null
let resizeObserver: ResizeObserver | null = null
let currentZoom = 1
let baseGeoJson: GeoJsonLike | null = null
let activeMapName = ''
let detailTimer: number | undefined
let renderRetryTimer: number | undefined
let renderVersion = 0
let wheelTarget: HTMLDivElement | null = null
let wheelFrame: number | undefined
let pendingZoom = 1
let detailLayerSignature = ''
let visibleLabelCodes = new Set<string>()
let visibleLabelNames = new Set<string>()

const geoJsonCache = new Map<string, GeoJsonLike>()
const minZoom = 1
const maxZoom = 140
const wheelZoomStep = 1.07

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

watch(
  () => [props.visibleLayers, props.selectedPipelineId, props.pipelines],
  () => {
    refreshCurrentMapOption()
  },
  { deep: true },
)

onBeforeUnmount(() => {
  clearDetailTimer()
  clearRenderRetryTimer()
  clearWheelFrame()
  detachWheelZoom()
  resizeObserver?.disconnect()
  chart?.dispose()
})

async function renderBaseMap() {
  if (!chartEl.value || !props.region.path) {
    return
  }

  if (!hasUsableChartBox()) {
    scheduleRenderRetry()
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
  pendingZoom = currentZoom
  detailLayerSignature = ''
  syncZoomRatio()
  registerMap(activeMapName, toMapInput(geoJson))
  hideTooltip()
  chart?.setOption(buildMapOption(activeMapName, markFeatures(geoJson.features ?? [], baseFeatureLayer()), view.center), true, true)
  scheduleDetailUpdate()
}

function ensureChart() {
  if (!chartEl.value || chart) {
    return
  }

  if (!hasUsableChartBox()) {
    scheduleRenderRetry()
    return
  }

  chart = init(chartEl.value, undefined, { renderer: 'canvas' })
  chart.on('click', (params) => {
    const pipelineId = readPipelineId(params.data)

    if (pipelineId) {
      const pipeline = props.pipelines.find((item) => item.id === pipelineId)

      if (pipeline) {
        emit('pipelineClick', pipeline)
      }

      return
    }

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
    pendingZoom = currentZoom
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

function hasUsableChartBox() {
  const box = chartEl.value?.getBoundingClientRect()

  return Boolean(box && box.width > 80 && box.height > 80)
}

function scheduleRenderRetry() {
  clearRenderRetryTimer()
  renderRetryTimer = window.setTimeout(() => {
    renderRetryTimer = undefined
    void renderBaseMap()
  }, 80)
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

  const regionVisible = props.visibleLayers.regions
  const series = [
    {
      type: 'map',
      map: mapName,
      geoIndex: 0,
      selectedMode: false,
      silent: !regionVisible,
      label: {
        show: regionVisible,
        formatter: formatRegionLabel,
        color: 'rgba(230, 241, 244, 0.76)',
        fontSize: 11,
        textBorderColor: 'rgba(5, 10, 16, 0.82)',
        textBorderWidth: 2,
      },
      emphasis: {
        label: {
          show: regionVisible,
          formatter: formatRegionLabel,
          color: '#e9f7f8',
        },
      },
      itemStyle: regionVisible
        ? undefined
        : {
            areaColor: 'rgba(5, 10, 16, 0)',
            borderColor: 'rgba(110, 202, 212, 0.08)',
          },
      data: regionVisible ? seriesData : [],
    },
    ...buildPipelineSeries(),
  ]

  return {
    animation: false,
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
        areaColor: regionVisible ? '#0b1a24' : 'rgba(5, 10, 16, 0.1)',
        borderColor: regionVisible ? 'rgba(110, 202, 212, 0.44)' : 'rgba(110, 202, 212, 0.08)',
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
        show: regionVisible,
        formatter: formatRegionLabel,
        color: 'rgba(230, 241, 244, 0.76)',
        fontSize: 11,
        textBorderColor: 'rgba(5, 10, 16, 0.82)',
        textBorderWidth: 2,
      },
    },
    series,
  }
}

function buildTooltipOption() {
  return {
    trigger: 'item',
    confine: true,
    transitionDuration: 0,
    borderWidth: 1,
    borderColor: 'rgba(110, 202, 212, 0.34)',
    backgroundColor: 'rgba(5, 10, 16, 0.92)',
    textStyle: {
      color: '#d8e6ea',
      fontSize: 12,
    },
    extraCssText: 'box-shadow:0 16px 36px rgba(0,0,0,.38);',
    formatter: (params: { name?: string; seriesType?: string; data?: { code?: string; pipelineId?: string; nodeId?: string } }) => {
      if (params.data?.nodeId) {
        const nodeInfo = findNodeById(params.data.nodeId)

        if (nodeInfo) {
          return [
            `<strong>${nodeInfo.node.name}</strong>`,
            `所属管线：${nodeInfo.pipeline.name}`,
            `节点类型：${formatNodeType(nodeInfo.node.type)}`,
            `状态：${formatPipelineStatus(nodeInfo.node.status)}`,
          ].join('<br/>')
        }
      }

      if (params.data?.pipelineId) {
        const pipeline = props.pipelines.find((item) => item.id === params.data?.pipelineId)

        if (pipeline) {
          return [
            `<strong>${pipeline.name}</strong>`,
            `状态：${formatPipelineStatus(pipeline.status)}`,
            `压力：${pipeline.pressure.toFixed(2)} MPa`,
            `流量：${pipeline.flow.toLocaleString('zh-CN')} m3/h`,
            `风险点：${pipeline.riskCount}`,
          ].join('<br/>')
        }
      }

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

function buildPipelineSeries() {
  const nodeSeries = props.visibleLayers.nodes ? [buildPipelineNodeSeries()] : []
  const alarmSeries = props.visibleLayers.alarms ? [buildAlarmNodeSeries()] : []

  if (!props.visibleLayers.pipelines) {
    return [...nodeSeries, ...alarmSeries]
  }

  return [buildPipelineLineSeries(), buildPipelineFlowSeries(), ...nodeSeries, ...alarmSeries]
}

function buildPipelineLineSeries() {
  return {
    name: '主干线',
    type: 'lines',
    coordinateSystem: 'geo',
    zlevel: 4,
    polyline: true,
    symbol: ['none', 'none'],
    lineStyle: {
      width: 2.2,
      opacity: 0.78,
      curveness: 0.18,
    },
    emphasis: {
      lineStyle: {
        width: 4,
        opacity: 1,
      },
    },
    data: props.pipelines.map((pipeline) => ({
      name: pipeline.name,
      pipelineId: pipeline.id,
      coords: pipeline.coords,
      lineStyle: {
        color: pipelineColor(pipeline.status, props.selectedPipelineId === pipeline.id),
        width: props.selectedPipelineId === pipeline.id ? 4 : pipeline.status === 'critical' ? 3 : 2.2,
        shadowBlur: props.selectedPipelineId === pipeline.id ? 22 : 14,
        shadowColor: pipelineColor(pipeline.status, true),
      },
    } satisfies PipelineLineDataItem & { lineStyle: Record<string, unknown> })),
  }
}

function buildPipelineFlowSeries() {
  return {
    name: '输送流向',
    type: 'lines',
    coordinateSystem: 'geo',
    zlevel: 5,
    polyline: true,
    symbol: ['none', 'arrow'],
    symbolSize: 7,
    effect: {
      show: true,
      period: 6,
      trailLength: 0.32,
      symbol: 'circle',
      symbolSize: 4,
    },
    lineStyle: {
      width: 0,
      opacity: 0,
    },
    data: props.pipelines.map((pipeline) => ({
      name: pipeline.name,
      pipelineId: pipeline.id,
      coords: pipeline.coords,
      lineStyle: {
        color: pipelineColor(pipeline.status, props.selectedPipelineId === pipeline.id),
      },
      effect: {
        color: pipelineColor(pipeline.status, true),
        period: props.selectedPipelineId === pipeline.id ? 3.2 : pipeline.status === 'critical' ? 4 : 6,
        symbolSize: props.selectedPipelineId === pipeline.id ? 6 : 4,
      },
    } satisfies PipelineLineDataItem & { lineStyle: Record<string, unknown>; effect: Record<string, unknown> })),
  }
}

function buildPipelineNodeSeries() {
  const data = props.pipelines.flatMap((pipeline) => pipeline.nodes
    .filter((node) => shouldShowNode(node, pipeline))
    .map((node) => ({
      name: node.name,
      pipelineId: pipeline.id,
      nodeId: node.id,
      status: node.status,
      value: [node.coord[0], node.coord[1], node.priority],
      itemStyle: {
        color: nodeColor(node.status),
        shadowBlur: props.selectedPipelineId === pipeline.id ? 20 : 12,
        shadowColor: nodeColor(node.status),
      },
    } satisfies PipelineNodeDataItem & { itemStyle: Record<string, unknown> })))

  return {
    name: '管网节点',
    type: 'effectScatter',
    coordinateSystem: 'geo',
    zlevel: 6,
    rippleEffect: {
      scale: 3.4,
      brushType: 'stroke',
    },
    symbolSize: (value: [number, number, number]) => Math.max(5, 12 - value[2] * 1.2),
    label: {
      show: currentZoom >= nodeLabelZoom(),
      formatter: '{b}',
      position: 'right',
      color: 'rgba(233, 247, 248, 0.86)',
      fontSize: 10,
      textBorderColor: 'rgba(5, 10, 16, 0.92)',
      textBorderWidth: 2,
    },
    emphasis: {
      scale: 1.7,
      label: {
        show: true,
      },
    },
    data,
  }
}

function buildAlarmNodeSeries() {
  const data = props.pipelines.flatMap((pipeline) => pipeline.nodes
    .filter((node) => node.status !== 'normal')
    .map((node) => ({
      name: node.name,
      pipelineId: pipeline.id,
      nodeId: node.id,
      status: node.status,
      value: [node.coord[0], node.coord[1], node.priority],
      itemStyle: {
        color: node.status === 'critical' ? '#ff7a35' : '#ffd166',
        shadowBlur: 24,
        shadowColor: node.status === 'critical' ? 'rgba(255, 122, 53, 0.9)' : 'rgba(255, 209, 102, 0.76)',
      },
    } satisfies PipelineNodeDataItem & { itemStyle: Record<string, unknown> })))

  return {
    name: '告警扩散',
    type: 'effectScatter',
    coordinateSystem: 'geo',
    zlevel: 7,
    rippleEffect: {
      scale: 5,
      period: 3,
      brushType: 'stroke',
    },
    symbolSize: (value: [number, number, number]) => Math.max(10, 18 - value[2] * 1.4),
    label: {
      show: false,
    },
    emphasis: {
      scale: 1.3,
      label: {
        show: true,
        formatter: '{b}',
        position: 'top',
        color: '#fff2df',
        fontSize: 10,
        textBorderColor: 'rgba(5, 10, 16, 0.92)',
        textBorderWidth: 2,
      },
    },
    data,
  }
}

function shouldShowNode(node: PipelineNode, pipeline: TrunkPipeline) {
  if (node.status !== 'normal') {
    return true
  }

  if (props.selectedPipelineId === pipeline.id) {
    return node.priority <= (currentZoom >= 3 ? 3 : 2)
  }

  if (props.region.level === 'country') {
    if (currentZoom < 1.8) {
      return node.priority <= 1
    }

    if (currentZoom < 3.2) {
      return node.priority <= 2
    }

    return node.priority <= 3
  }

  const regionProvince = props.region.code.slice(0, 2)
  const nodeProvince = node.provinceCode.slice(0, 2)

  if (regionProvince !== nodeProvince && !props.focusCode?.startsWith(nodeProvince)) {
    return false
  }

  if (props.region.level === 'province') {
    return node.priority <= (currentZoom >= 2.6 ? 3 : 2)
  }

  return currentZoom >= 1.4 || node.priority <= 2
}

function pipelineColor(status: PipelineStatus, active = false) {
  if (status === 'critical') {
    return active ? '#ff7a35' : '#e7682d'
  }

  if (status === 'warning') {
    return active ? '#ffd166' : '#f3a83b'
  }

  return active ? '#e9f7f8' : '#6ecad4'
}

function nodeColor(status: PipelineStatus) {
  if (status === 'critical') {
    return '#ff7a35'
  }

  if (status === 'warning') {
    return '#ffd166'
  }

  return '#8fe8f0'
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

function refreshCurrentMapOption() {
  if (!chart || !activeMapName || !baseGeoJson) {
    return
  }

  hideTooltip()
  chart.setOption(
    buildMapOption(
      activeMapName,
      markFeatures(baseGeoJson.features ?? [], baseFeatureLayer()),
      readChartCenter(),
    ),
    false,
    true,
  )
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
  pendingZoom = clampZoom(pendingZoom * factor)

  if (wheelFrame) {
    return
  }

  wheelFrame = window.requestAnimationFrame(() => {
    wheelFrame = undefined
    applyZoom(pendingZoom)
  })
}

function applyZoom(value: number) {
  if (!chart) {
    return
  }

  const nextZoom = clampZoom(value)

  if (Math.abs(nextZoom - currentZoom) < 0.001) {
    return
  }

  currentZoom = nextZoom
  pendingZoom = currentZoom
  syncZoomRatio()
  chart.setOption({
    geo: {
      zoom: currentZoom,
      center: readChartCenter(),
    },
  }, false, true)
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
  const nextSignature = `${props.region.code}|${detailKey || 'base'}|${currentZoomStage()}|${currentLabelStage()}`
    + `|${currentNodeStage()}|${props.selectedPipelineId ?? 'no-pipeline'}`

  if (nextSignature === detailLayerSignature) {
    return
  }

  detailLayerSignature = nextSignature

  if (!detailKey) {
    const cachedBaseMapName = readLayer(nextSignature)
    activeMapName = cachedBaseMapName ?? props.region.code
    if (!cachedBaseMapName) {
      registerMap(activeMapName, toMapInput(baseGeoJson))
      rememberLayer(nextSignature, activeMapName)
    }
    hideTooltip()
    chart.setOption(buildMapOption(activeMapName, markFeatures(baseFeatures, baseFeatureLayer()), center), false, true)
    return
  }

  const cachedMapName = readLayer(nextSignature)
  activeMapName = cachedMapName ?? `${props.region.code}-detail-${hashText(nextSignature)}`

  const mergedGeoJson = {
    type: 'FeatureCollection',
    features: visibleFeatures,
  }
  if (!cachedMapName) {
    registerMap(activeMapName, toMapInput(mergedGeoJson))
    rememberLayer(nextSignature, activeMapName)
  }
  hideTooltip()
  chart.setOption(buildMapOption(activeMapName, mergedGeoJson.features, center), false, true)
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

function currentLabelStage() {
  return [
    shouldShowLabel('province') ? 'p1' : 'p0',
    shouldShowLabel('city') ? 'c1' : 'c0',
    shouldShowLabel('district') ? 'd1' : 'd0',
  ].join(':')
}

function currentZoomStage() {
  if (currentZoom >= districtLabelZoom()) return 'district-label'
  if (currentZoom >= districtBoundaryZoom()) return 'district-boundary'
  if (currentZoom >= cityLabelZoom()) return 'city-label'
  if (currentZoom >= cityBoundaryZoom()) return 'city-boundary'
  if (currentZoom >= provinceLabelZoom()) return 'province-label'
  return 'base'
}

function currentNodeStage() {
  if (currentZoom >= 3.2) return 'node-dense'
  if (currentZoom >= 1.8) return 'node-medium'
  return 'node-sparse'
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

function nodeLabelZoom() {
  return props.region.level === 'country' ? 3.4 : props.region.level === 'province' ? 2.7 : 1.6
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
  return feature ? getFeatureCenter(feature) : undefined
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
  return getFeatureBounds(feature)
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

function hideTooltip() {
  chart?.dispatchAction({ type: 'hideTip' })
}

function readPipelineId(data: unknown) {
  if (data && typeof data === 'object' && 'pipelineId' in data) {
    return String((data as { pipelineId?: string }).pipelineId ?? '')
  }

  return ''
}

function findNodeById(nodeId: string) {
  for (const pipeline of props.pipelines) {
    const node = pipeline.nodes.find((item) => item.id === nodeId)

    if (node) {
      return { pipeline, node }
    }
  }

  return null
}

function formatPipelineStatus(status: PipelineStatus) {
  if (status === 'critical') {
    return '严重异常'
  }

  if (status === 'warning') {
    return '风险关注'
  }

  return '运行正常'
}

function formatNodeType(type: PipelineNode['type']) {
  if (type === 'hub') {
    return '枢纽'
  }

  if (type === 'valve') {
    return '阀室'
  }

  if (type === 'offtake') {
    return '分输'
  }

  return '站场'
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

function clearRenderRetryTimer() {
  if (renderRetryTimer) {
    window.clearTimeout(renderRetryTimer)
    renderRetryTimer = undefined
  }
}

function clearWheelFrame() {
  if (wheelFrame) {
    window.cancelAnimationFrame(wheelFrame)
    wheelFrame = undefined
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
        <button type="button" title="放大地图" aria-label="放大地图" @click="zoomIn">
          <span class="geo-map-panel__icon geo-map-panel__icon--plus"></span>
        </button>
        <button type="button" title="缩小地图" aria-label="缩小地图" @click="zoomOut">
          <span class="geo-map-panel__icon geo-map-panel__icon--minus"></span>
        </button>
        <button type="button" title="归位到全国视图" aria-label="归位到全国视图" @click="emit('resetView')">
          <span class="geo-map-panel__icon geo-map-panel__icon--target"></span>
        </button>
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
  position: absolute;
  inset: 0;
  display: grid;
  grid-template-rows: 0 minmax(0, 1fr);
  border: 0;
  background:
    radial-gradient(circle at 50% 46%, rgba(110, 202, 212, 0.13), transparent 34%),
    radial-gradient(circle at 72% 32%, rgba(231, 104, 45, 0.1), transparent 20%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.018), transparent),
    #050a10;
  box-shadow: none;
}

.geo-map-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background:
    linear-gradient(105deg, transparent 0%, transparent 44%, rgba(110, 202, 212, 0.08) 50%, transparent 56%, transparent 100%),
    repeating-linear-gradient(180deg, rgba(255, 255, 255, 0.018) 0, rgba(255, 255, 255, 0.018) 1px, transparent 1px, transparent 5px);
  mix-blend-mode: screen;
  opacity: 0.72;
  animation: map-scan-beam 8s steps(30) infinite;
}

.geo-map-panel .panel-head {
  position: absolute;
  block-size: 0;
  overflow: hidden;
  opacity: 0;
}

.geo-map-panel__body {
  position: relative;
  grid-row: 1 / -1;
  min-height: 0;
  overflow: hidden;
}

.geo-map-panel__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(110, 202, 212, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(110, 202, 212, 0.055) 1px, transparent 1px);
  background-size: 32px 32px;
  mask-image: radial-gradient(circle at center, rgba(0, 0, 0, 0.9), transparent 92%);
  animation: map-grid-drift 18s linear infinite;
}

.geo-map-panel__chart {
  position: absolute;
  inset: 6px;
  z-index: 0;
  min-width: 0;
  min-height: 0;
}

.geo-map-panel__scale {
  position: absolute;
  inset: auto auto 22px 22px;
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
  inset: auto auto 22px calc(50% - 190px);
  z-index: 4;
  display: flex;
  gap: 6px;
  padding: 6px;
  border: 1px solid rgba(110, 202, 212, 0.22);
  background: rgba(6, 11, 17, 0.66);
  backdrop-filter: blur(16px);
}

.geo-map-panel__toolbar button {
  min-height: 34px;
  inline-size: 34px;
  padding: 0;
  border: 1px solid rgba(110, 202, 212, 0.28);
  color: var(--color-text);
  background: rgba(8, 13, 19, 0.82);
  backdrop-filter: blur(12px);
}

.geo-map-panel__toolbar button:hover {
  border-color: var(--color-line-strong);
  background: rgba(110, 202, 212, 0.12);
}

.geo-map-panel__icon {
  position: relative;
  display: inline-block;
  inline-size: 16px;
  block-size: 16px;
}

.geo-map-panel__icon--plus::before,
.geo-map-panel__icon--plus::after,
.geo-map-panel__icon--minus::before {
  content: '';
  position: absolute;
  background: currentColor;
}

.geo-map-panel__icon--plus::before,
.geo-map-panel__icon--minus::before {
  inset: 7px 2px auto;
  block-size: 2px;
}

.geo-map-panel__icon--plus::after {
  inset: 2px auto 2px 7px;
  inline-size: 2px;
}

.geo-map-panel__icon--target {
  inline-size: 18px;
  block-size: 18px;
  background:
    linear-gradient(currentColor, currentColor) left top / 7px 2px no-repeat,
    linear-gradient(currentColor, currentColor) left top / 2px 7px no-repeat,
    linear-gradient(currentColor, currentColor) right top / 7px 2px no-repeat,
    linear-gradient(currentColor, currentColor) right top / 2px 7px no-repeat,
    linear-gradient(currentColor, currentColor) left bottom / 7px 2px no-repeat,
    linear-gradient(currentColor, currentColor) left bottom / 2px 7px no-repeat,
    linear-gradient(currentColor, currentColor) right bottom / 7px 2px no-repeat,
    linear-gradient(currentColor, currentColor) right bottom / 2px 7px no-repeat;
}

.geo-map-panel__icon--target::before {
  content: '';
  position: absolute;
  inset: 7px;
  background: currentColor;
  box-shadow: 0 0 8px currentColor;
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

@keyframes map-scan-beam {
  from {
    background-position: -90vw 0, 0 0;
  }

  to {
    background-position: 90vw 0, 0 0;
  }
}

@media (max-width: 719px) {
  .geo-map-panel {
    position: relative;
    min-block-size: 420px;
  }

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
    inset: auto auto 14px 14px;
  }
}
</style>
