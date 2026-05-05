<script setup lang="ts">
import 'maplibre-gl/dist/maplibre-gl.css'
import type { Layer } from '@deck.gl/core'
import { ScatterplotLayer, PathLayer } from '@deck.gl/layers'
import { MapboxOverlay } from '@deck.gl/mapbox'
import maplibregl, { type MapLayerMouseEvent, type Map as MapLibreMap } from 'maplibre-gl'
import { computed, nextTick, onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { loadAdminBoundaryIndex, resolveGeoTileAssetPath } from '../map/adminIndex'
import {
  adminLevelFilter as buildAdminLevelFilter,
  adminLabelFeatureCollection,
  adminLabelFilter as buildAdminLabelFilter,
  dashboardMapCamera,
  focusAdminBoundaryVisible,
  floatingTooltipPosition,
  parentAdminLineFilters,
  visibleAdminLevel,
} from '../map/adminViewport'
import {
  buildPipelineFlowSegments,
  pipelineNodeVisualProfile,
  pipelineStatusVisualProfile,
  routeColor,
  smoothPipelineRoutes,
  visiblePipelineNodes,
  type PipelineFlowSegment,
} from '../map/pipelineVisualAnimation'
import {
  pipelineVisualNodes,
  trunkPipelineRoutes,
  type PipelineVisualNode,
  type PipelineVisualRoute,
} from '../map/pipelineVisualData'
import type {
  AdminBoundaryIndex,
  GeoIndex,
  MapRegion,
  MapTooltipData,
} from '../types'

const props = defineProps<{
  region: MapRegion
  focusCode?: string
  geoIndex: GeoIndex | null
  tooltipData: MapTooltipData | null
  loading: boolean
  visibleLayers: { regions: boolean; pipelines?: boolean; nodes?: boolean }
  fullscreen?: boolean
}>()

const emit = defineEmits<{
  regionClick: [payload: { code: string; name: string }]
  regionHover: [payload: { code: string; name: string }]
  resetView: []
  toggleFullscreen: []
}>()

type AdminFeatureProperties = {
  code?: string | number
  name?: string
  level?: string
}

const mapEl = ref<HTMLDivElement | null>(null)
const zoomRatio = ref(100)
const adminIndex = shallowRef<AdminBoundaryIndex>({ entries: {} })
const hoverPoint = ref({ x: 0, y: 0 })
const hoverName = ref('')
const hoverCode = ref('')
const smoothedTrunkPipelineRoutes = smoothPipelineRoutes(trunkPipelineRoutes, 6)

let map: MapLibreMap | null = null
let resizeObserver: ResizeObserver | null = null
let currentAdminLevel: ReturnType<typeof visibleAdminLevel> | '' = ''
let pipelineOverlay: MapboxOverlay | null = null
let pipelineAnimationFrame = 0

const tooltipStyle = computed(() => ({
  left: `${hoverPoint.value.x}px`,
  top: `${hoverPoint.value.y}px`,
}))
const activeTooltipData = computed(() => {
  if (!hoverCode.value || String(props.tooltipData?.areaId ?? '') !== hoverCode.value) {
    return null
  }

  return props.tooltipData
})
const showFloatingTooltip = computed(() => Boolean(hoverCode.value))

watch(
  () => mapEl.value,
  async () => {
    await nextTick()
    await ensureMap()
  },
  { immediate: true },
)

watch(
  () => [props.focusCode, props.region.code, adminIndex.value],
  () => {
    syncAdminLevelFilter()
    syncFocus()
  },
)

watch(
  () => props.visibleLayers.regions,
  (visible) => {
    if (!map?.getLayer('admin-fill') || !map.getLayer('admin-line')) {
      return
    }

    map.setPaintProperty('admin-fill', 'fill-opacity', visible ? 1 : 0.12)
    map.setPaintProperty('admin-line', 'line-opacity', visible ? 1 : 0.18)
    if (map.getLayer('admin-label')) {
      map.setPaintProperty('admin-label', 'text-opacity', visible ? 1 : 0)
    }
    setLineOpacity('admin-parent-line-near', visible ? 0.98 : 0)
    setLineOpacity('admin-parent-line-root', visible ? 0.9 : 0)
  },
)

watch(
  () => [props.visibleLayers.pipelines, props.visibleLayers.nodes],
  () => {
    syncPipelineOverlay()
  },
)

watch(
  () => props.fullscreen,
  async () => {
    await nextTick()
    map?.resize()
  },
)

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  if (pipelineAnimationFrame) {
    cancelAnimationFrame(pipelineAnimationFrame)
  }
  if (map && pipelineOverlay) {
    map.removeControl(pipelineOverlay as unknown as maplibregl.IControl)
  }
  pipelineOverlay = null
  map?.remove()
  map = null
})

async function ensureMap() {
  if (!mapEl.value || map) {
    return
  }

  adminIndex.value = await loadAdminBoundaryIndex()
  map = new maplibregl.Map({
    container: mapEl.value,
    style: buildMapStyle(),
    center: dashboardMapCamera.center,
    zoom: dashboardMapCamera.initialZoom,
    minZoom: dashboardMapCamera.minZoom,
    maxZoom: dashboardMapCamera.maxZoom,
    attributionControl: false,
    dragRotate: false,
    pitchWithRotate: false,
    renderWorldCopies: false,
    maxBounds: dashboardMapCamera.maxBounds,
  })

  map.on('load', () => {
    if (!map) return

    addAdminLayers(map)
    addPipelineOverlay(map)
    syncAdminLevelFilter()
    syncFocus()
  })
  map.on('mousemove', handleRegionHover)
  map.on('mouseout', clearRegionHover)
  map.on('click', handleRegionClick)
  map.on('zoom', handleMapZoom)
  map.on('move', syncZoomRatio)

  resizeObserver = new ResizeObserver(() => map?.resize())
  resizeObserver.observe(mapEl.value)
}

function buildMapStyle() {
  return {
    version: 8,
    sources: {
      admin: {
        type: 'vector',
        tiles: [resolveGeoTileAssetPath('/geo-tiles/v1/admin/{z}/{x}/{y}.pbf')],
        minzoom: 3,
        maxzoom: 9,
        promoteId: 'code',
      },
      'admin-labels': {
        type: 'geojson',
        data: adminLabelFeatureCollection(adminIndex.value),
        promoteId: 'code',
      },
    },
    layers: [
      {
        id: 'background',
        type: 'background',
        paint: {
          'background-color': '#050a10',
        },
      },
    ],
  } satisfies maplibregl.StyleSpecification
}

function addAdminLayers(target: MapLibreMap) {
  if (target.getLayer('admin-fill')) {
    return
  }

  target.addLayer({
    id: 'admin-fill',
    type: 'fill',
    source: 'admin',
    'source-layer': 'admin',
    filter: adminLevelFilter(),
    paint: {
      'fill-color': [
        'match',
        ['get', 'level'],
        'province', 'rgba(11, 26, 36, 0.72)',
        'city', 'rgba(10, 35, 46, 0.58)',
        'district', 'rgba(10, 28, 38, 0.42)',
        'rgba(11, 26, 36, 0.52)',
      ],
      'fill-opacity': props.visibleLayers.regions ? 1 : 0.12,
    },
  })
  target.addLayer({
    id: 'admin-line',
    type: 'line',
    source: 'admin',
    'source-layer': 'admin',
    filter: adminLevelFilter(),
    paint: {
      'line-color': [
        'match',
        ['get', 'level'],
        'province', 'rgba(110, 202, 212, 0.58)',
        'city', 'rgba(143, 232, 240, 0.38)',
        'district', 'rgba(233, 247, 248, 0.28)',
        'rgba(110, 202, 212, 0.36)',
      ],
      'line-width': [
        'match',
        ['get', 'level'],
        'province', 1.3,
        'city', 0.9,
        'district', 0.55,
        0.8,
      ],
      'line-opacity': props.visibleLayers.regions ? 1 : 0.18,
    },
  })
  target.addLayer({
    id: 'admin-label',
    type: 'symbol',
    source: 'admin-labels',
    filter: adminLabelFilter(),
    layout: {
      'text-field': ['get', 'name'],
      'text-font': ['sans-serif'],
      'text-size': [
        'match',
        ['get', 'level'],
        'province', 13,
        'city', 12,
        'district', 10.5,
        11,
      ],
      'text-letter-spacing': 0.06,
      'text-anchor': 'center',
      'text-allow-overlap': false,
      'text-ignore-placement': false,
      'symbol-placement': 'point',
    },
    paint: {
      'text-color': [
        'match',
        ['get', 'level'],
        'province', 'rgba(233, 247, 248, 0.78)',
        'city', 'rgba(210, 236, 240, 0.72)',
        'district', 'rgba(190, 220, 224, 0.62)',
        'rgba(210, 236, 240, 0.68)',
      ],
      'text-halo-color': 'rgba(5, 10, 16, 0.86)',
      'text-halo-width': 1.4,
      'text-halo-blur': 0.4,
      'text-opacity': props.visibleLayers.regions ? 1 : 0,
    },
  })
  target.addLayer({
    id: 'admin-hover',
    type: 'line',
    source: 'admin',
    'source-layer': 'admin',
    filter: ['==', ['get', 'code'], ''],
    paint: {
      'line-color': '#e9f7f8',
      'line-width': 2.4,
      'line-opacity': 0.96,
    },
  })
  target.addLayer({
    id: 'admin-parent-line-near',
    type: 'line',
    source: 'admin',
    'source-layer': 'admin',
    filter: emptyAdminFilter(),
    paint: {
      'line-color': 'rgba(143, 232, 240, 0.82)',
      'line-width': 2.4,
      'line-opacity': props.visibleLayers.regions ? 0.98 : 0,
    },
  })
  target.addLayer({
    id: 'admin-parent-line-root',
    type: 'line',
    source: 'admin',
    'source-layer': 'admin',
    filter: emptyAdminFilter(),
    paint: {
      'line-color': 'rgba(233, 247, 248, 0.68)',
      'line-width': 3,
      'line-opacity': props.visibleLayers.regions ? 0.9 : 0,
    },
  })
  target.addLayer({
    id: 'admin-hover-glow',
    type: 'line',
    source: 'admin',
    'source-layer': 'admin',
    filter: ['==', ['get', 'code'], ''],
    paint: {
      'line-color': '#8fe8f0',
      'line-width': 6,
      'line-opacity': 0.28,
      'line-blur': 2.6,
    },
  })
  target.addLayer({
    id: 'admin-focus',
    type: 'line',
    source: 'admin',
    'source-layer': 'admin',
    filter: ['==', ['get', 'code'], props.focusCode ?? ''],
    paint: {
      'line-color': '#ff7a35',
      'line-width': 3,
      'line-opacity': 0.98,
    },
  })
  target.moveLayer('admin-hover', 'admin-focus')
}

function addPipelineOverlay(target: MapLibreMap) {
  if (pipelineOverlay) {
    return
  }

  pipelineOverlay = new MapboxOverlay({ interleaved: false, layers: [] })
  target.addControl(pipelineOverlay as unknown as maplibregl.IControl)
  animatePipelineOverlay()
}

function animatePipelineOverlay() {
  syncPipelineOverlay()
  pipelineAnimationFrame = requestAnimationFrame(animatePipelineOverlay)
}

function syncPipelineOverlay() {
  if (!pipelineOverlay) {
    return
  }

  pipelineOverlay.setProps({
    layers: buildPipelineLayers(performance.now()),
  })
}

function buildPipelineLayers(now: number): Layer[] {
  const layers: Layer[] = []
  const showPipelines = props.visibleLayers.pipelines !== false
  const showNodes = props.visibleLayers.nodes !== false
  const pulse = (Math.sin(now / 720) + 1) / 2
  const scanPulse = (Math.sin(now / 1100) + 1) / 2
  const phase = now / 1000
  const zoom = map?.getZoom() ?? dashboardMapCamera.initialZoom
  const visibleNodes = visiblePipelineNodes(pipelineVisualNodes, zoom)
  const flowSegments = buildPipelineFlowSegments(smoothedTrunkPipelineRoutes, phase)

  if (showPipelines) {
    layers.push(
      new PathLayer<PipelineVisualRoute>({
        id: 'dashboard-trunk-pipeline-casing',
        data: smoothedTrunkPipelineRoutes,
        getPath: (item) => item.coords,
        getColor: [5, 13, 17, 210],
        getWidth: (item) => pipelineStatusVisualProfile(item.status).casingWidth,
        widthUnits: 'pixels',
        capRounded: true,
        jointRounded: true,
        pickable: false,
      }),
      new PathLayer<PipelineVisualRoute>({
        id: 'dashboard-trunk-pipeline-outer-glow',
        data: smoothedTrunkPipelineRoutes,
        getPath: (item) => item.coords,
        getColor: (item) => routeColor(pipelineStatusVisualProfile(item.status).glowColor, item.status === 'normal' ? 0.14 : 0.22),
        getWidth: (item) => pipelineStatusVisualProfile(item.status).glowWidth,
        widthUnits: 'pixels',
        capRounded: true,
        jointRounded: true,
        pickable: false,
      }),
      new PathLayer<PipelineVisualRoute>({
        id: 'dashboard-trunk-pipeline-core',
        data: smoothedTrunkPipelineRoutes,
        getPath: (item) => item.coords,
        getColor: (item) => routeColor(pipelineStatusVisualProfile(item.status).coreColor, 0.88),
        getWidth: (item) => pipelineStatusVisualProfile(item.status).coreWidth,
        widthUnits: 'pixels',
        capRounded: true,
        jointRounded: true,
        pickable: false,
      }),
      new PathLayer<PipelineVisualRoute>({
        id: 'dashboard-trunk-pipeline-inner-trace',
        data: smoothedTrunkPipelineRoutes,
        getPath: (item) => item.coords,
        getColor: (item) => routeColor(pipelineStatusVisualProfile(item.status).flowColor, 0.34),
        getWidth: (item) => Math.max(1.4, pipelineStatusVisualProfile(item.status).coreWidth - 2.6),
        widthUnits: 'pixels',
        capRounded: true,
        jointRounded: true,
        pickable: false,
      }),
      new PathLayer<PipelineFlowSegment>({
        id: 'dashboard-trunk-pipeline-flow-bloom',
        data: flowSegments,
        getPath: (item) => item.path,
        getColor: (item) => routeColor(pipelineStatusVisualProfile(item.status).flowColor, item.status === 'normal' ? 0.16 : 0.26),
        getWidth: (item) => pipelineStatusVisualProfile(item.status).flowWidth + 6,
        widthUnits: 'pixels',
        capRounded: true,
        jointRounded: true,
        pickable: false,
      }),
      new PathLayer<PipelineFlowSegment>({
        id: 'dashboard-trunk-pipeline-flow-core',
        data: flowSegments,
        getPath: (item) => item.path,
        getColor: (item) => routeColor(pipelineStatusVisualProfile(item.status).flowColor, item.status === 'normal' ? 0.82 : 0.94),
        getWidth: (item) => pipelineStatusVisualProfile(item.status).flowWidth,
        widthUnits: 'pixels',
        capRounded: true,
        jointRounded: true,
        pickable: false,
      }),
    )
  }

  if (showNodes) {
    layers.push(
      new ScatterplotLayer<PipelineVisualNode>({
        id: 'dashboard-pipeline-node-status-wave',
        data: visibleNodes.filter((item) => item.status !== 'normal'),
        getPosition: (item) => item.coord,
        getRadius: (item) => {
          const profile = pipelineNodeVisualProfile(item.type, item.priority)
          return profile.scanRadius + scanPulse * (item.status === 'critical' ? 16 : 10)
        },
        radiusUnits: 'pixels',
        getFillColor: (item) => routeColor(pipelineStatusVisualProfile(item.status).glowColor, item.status === 'critical' ? 0.075 : 0.045),
        getLineColor: (item) => routeColor(pipelineStatusVisualProfile(item.status).flowColor, item.status === 'critical' ? 0.32 : 0.22),
        lineWidthUnits: 'pixels',
        getLineWidth: 0.8,
        stroked: true,
        pickable: false,
      }),
      new ScatterplotLayer<PipelineVisualNode>({
        id: 'dashboard-pipeline-node-scan',
        data: visibleNodes.filter((item) => pipelineNodeVisualProfile(item.type, item.priority).scanRadius > 0),
        getPosition: (item) => item.coord,
        getRadius: (item) => {
          const profile = pipelineNodeVisualProfile(item.type, item.priority)
          return profile.scanRadius * (0.78 + scanPulse * 0.22)
        },
        radiusUnits: 'pixels',
        getFillColor: (item) => routeColor(pipelineStatusVisualProfile(item.status).glowColor, item.status === 'normal' ? 0.018 : 0.055),
        getLineColor: (item) => routeColor(pipelineStatusVisualProfile(item.status).flowColor, item.status === 'normal' ? 0.16 : 0.26),
        lineWidthUnits: 'pixels',
        getLineWidth: 0.8,
        stroked: true,
        pickable: false,
      }),
      new ScatterplotLayer<PipelineVisualNode>({
        id: 'dashboard-pipeline-node-ring',
        data: visibleNodes,
        getPosition: (item) => item.coord,
        getRadius: (item) => pipelineNodeVisualProfile(item.type, item.priority).ringRadius + pulse * 0.7,
        radiusUnits: 'pixels',
        getFillColor: [5, 13, 17, 72],
        getLineColor: (item) => routeColor(pipelineStatusVisualProfile(item.status).flowColor, item.type === 'valve' ? 0.38 : 0.64),
        lineWidthUnits: 'pixels',
        getLineWidth: (item) => pipelineNodeVisualProfile(item.type, item.priority).strokeWidth,
        stroked: true,
        pickable: false,
      }),
      new ScatterplotLayer<PipelineVisualNode>({
        id: 'dashboard-pipeline-node-socket',
        data: visibleNodes,
        getPosition: (item) => item.coord,
        getRadius: (item) => pipelineNodeVisualProfile(item.type, item.priority).coreRadius + 2,
        radiusUnits: 'pixels',
        getFillColor: [5, 13, 17, 218],
        getLineColor: [233, 247, 248, 72],
        lineWidthUnits: 'pixels',
        getLineWidth: 1,
        stroked: true,
        pickable: false,
      }),
      new ScatterplotLayer<PipelineVisualNode>({
        id: 'dashboard-pipeline-node-core',
        data: visibleNodes,
        getPosition: (item) => item.coord,
        getRadius: (item) => pipelineNodeVisualProfile(item.type, item.priority).coreRadius,
        radiusUnits: 'pixels',
        getFillColor: (item) => routeColor(pipelineStatusVisualProfile(item.status).coreColor, 0.96),
        getLineColor: [233, 247, 248, 210],
        lineWidthUnits: 'pixels',
        getLineWidth: 1.1,
        stroked: true,
        pickable: false,
      }),
    )
  }

  return layers
}

function syncAdminLevelFilter() {
  if (!map?.getLayer('admin-fill') || !map.getLayer('admin-line')) {
    return
  }

  const nextLevel = readVisibleAdminLevel()
  if (nextLevel === currentAdminLevel) {
    return
  }

  currentAdminLevel = nextLevel
  if (mapEl.value) {
    mapEl.value.dataset.adminLevel = nextLevel
  }
  const filter = adminLevelFilter()
  map.setFilter('admin-fill', filter)
  map.setFilter('admin-line', filter)
  syncAdminLabelFilter(nextLevel)
  syncParentAdminLines(nextLevel)
  clearRegionHover()
}

function adminLevelFilter() {
  return buildAdminLevelFilter(readVisibleAdminLevel()) as maplibregl.FilterSpecification
}

function adminLabelFilter(level = readVisibleAdminLevel()) {
  return buildAdminLabelFilter(level) as maplibregl.FilterSpecification
}

function readVisibleAdminLevel() {
  return visibleAdminLevel(props.region.level, map?.getZoom() ?? dashboardMapCamera.initialZoom)
}

function handleMapZoom() {
  syncZoomRatio()
  syncAdminLevelFilter()
  syncFocusBoundaryVisibility()
}

function handleRegionHover(event: MapLayerMouseEvent) {
  hoverPoint.value = floatingTooltipPosition(event.point, {
    width: map?.getContainer().clientWidth ?? 0,
    height: map?.getContainer().clientHeight ?? 0,
  })
  const feature = readFirstAdminFeature(event)
  const code = readFeatureCode(feature)

  if (!code) {
    clearRegionHover()
    return
  }

  if (code === hoverCode.value) {
    return
  }

  hoverCode.value = code
  hoverName.value = readFeatureName(feature)
  setHoverFilter(code)
  emit('regionHover', { code, name: hoverName.value })
}

function handleRegionClick(event: MapLayerMouseEvent) {
  const feature = readFirstAdminFeature(event)
  const code = readFeatureCode(feature)

  if (code) {
    emit('regionClick', { code, name: readFeatureName(feature) })
  }
}

function readFirstAdminFeature(event: MapLayerMouseEvent) {
  if (!map?.getLayer('admin-fill')) {
    return undefined
  }

  return map?.queryRenderedFeatures(event.point, {
    layers: ['admin-fill'],
  })[0]
}

function readFeatureCode(feature: ReturnType<typeof readFirstAdminFeature>) {
  const properties = feature?.properties as AdminFeatureProperties | undefined

  return properties?.code ? String(properties.code) : ''
}

function readFeatureName(feature: ReturnType<typeof readFirstAdminFeature>) {
  const properties = feature?.properties as AdminFeatureProperties | undefined

  return String(properties?.name ?? '')
}

function syncFocus() {
  if (!map?.getLayer('admin-focus')) {
    return
  }

  syncFocusBoundaryVisibility()
  const target = props.focusCode ? adminIndex.value.entries[props.focusCode] : null

  if (target?.bbox) {
    map.fitBounds([[target.bbox[0], target.bbox[1]], [target.bbox[2], target.bbox[3]]], {
      padding: { top: 90, right: 440, bottom: 90, left: 390 },
      duration: 420,
      maxZoom: target.level === 'district' ? 8.4 : target.level === 'city' ? 7 : 5.6,
    })
    return
  }

  if (!props.focusCode) {
    map.easeTo({ center: dashboardMapCamera.center, zoom: dashboardMapCamera.initialZoom, duration: 420 })
  }
}

function syncFocusBoundaryVisibility() {
  if (!map?.getLayer('admin-focus')) {
    return
  }

  const target = props.focusCode ? adminIndex.value.entries[props.focusCode] : null
  const zoom = map.getZoom()
  const visible = target ? focusAdminBoundaryVisible(target.level, zoom, target.code) : false
  map.setFilter('admin-focus', visible ? ['==', ['get', 'code'], props.focusCode ?? ''] : emptyAdminFilter())
}

function clearRegionHover() {
  if (!hoverCode.value) {
    return
  }

  hoverCode.value = ''
  hoverName.value = ''
  if (map?.getLayer('admin-hover')) {
    setHoverFilter('')
  }
}

function zoomIn() {
  map?.easeTo({ zoom: map.getZoom() + 0.75, duration: 220 })
}

function zoomOut() {
  map?.easeTo({ zoom: map.getZoom() - 0.75, duration: 220 })
}

function syncParentAdminLines(level: ReturnType<typeof visibleAdminLevel>) {
  if (!map?.getLayer('admin-parent-line-near') || !map.getLayer('admin-parent-line-root')) {
    return
  }

  const filters = parentAdminLineFilters(level)
  map.setFilter('admin-parent-line-near', (filters[0] ?? emptyAdminFilter()) as maplibregl.FilterSpecification)
  map.setFilter('admin-parent-line-root', (filters[1] ?? emptyAdminFilter()) as maplibregl.FilterSpecification)
}

function syncAdminLabelFilter(level: ReturnType<typeof visibleAdminLevel>) {
  if (map?.getLayer('admin-label')) {
    map.setFilter('admin-label', adminLabelFilter(level))
  }
}

function setHoverFilter(code: string) {
  const filter = ['==', ['get', 'code'], code] as maplibregl.FilterSpecification
  if (map?.getLayer('admin-hover')) {
    map.setFilter('admin-hover', filter)
  }
  if (map?.getLayer('admin-hover-glow')) {
    map.setFilter('admin-hover-glow', filter)
  }
}

function setLineOpacity(layerId: string, opacity: number) {
  if (map?.getLayer(layerId)) {
    map.setPaintProperty(layerId, 'line-opacity', opacity)
  }
}

function emptyAdminFilter() {
  return ['==', ['get', 'code'], ''] as maplibregl.FilterSpecification
}

function syncZoomRatio() {
  zoomRatio.value = Math.round((map?.getZoom() ?? dashboardMapCamera.initialZoom) * 100)
}

function scaleBarLabel() {
  const zoom = map?.getZoom() ?? dashboardMapCamera.initialZoom

  if (zoom >= 8) return '10 km'
  if (zoom >= 6) return '50 km'
  if (zoom >= 4) return '200 km'
  return '800 km'
}
</script>

<template>
  <section class="panel geo-map-panel" :class="{ 'is-fullscreen': fullscreen }">
    <div class="panel-head">
      <h2 class="panel-title">区域监控主画面</h2>
      <span class="eyebrow">{{ region.name }}</span>
    </div>
    <div class="geo-map-panel__body">
      <div class="geo-map-panel__grid"></div>
      <div ref="mapEl" class="geo-map-panel__chart"></div>
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
        <button
          type="button"
          :title="fullscreen ? '退出全屏地图' : '全屏展示地图'"
          :aria-label="fullscreen ? '退出全屏地图' : '全屏展示地图'"
          :class="{ 'is-active': fullscreen }"
          @click="emit('toggleFullscreen')"
        >
          <span class="geo-map-panel__icon geo-map-panel__icon--fullscreen"></span>
        </button>
      </div>
      <div class="geo-map-panel__scale" aria-label="地图比例尺">
        <span>{{ zoomRatio }}%</span>
        <i></i>
        <strong>{{ scaleBarLabel() }}</strong>
      </div>
      <div v-if="showFloatingTooltip" class="geo-map-panel__tooltip" :style="tooltipStyle">
        <strong>{{ activeTooltipData?.areaName ?? (hoverName || '读取中') }}</strong>
        <span v-if="!activeTooltipData">正在读取区域数据...</span>
        <template v-else>
          <span>传感器 {{ activeTooltipData.sensorNumbers }}</span>
          <span>异常 {{ activeTooltipData.abnormalSensorNumbers }}</span>
          <span>告警 {{ activeTooltipData.warnings }}</span>
          <span>压力 {{ activeTooltipData.pressure.toFixed(2) }} MPa</span>
          <span>流量 {{ activeTooltipData.flow.toFixed(2) }} m3/h</span>
        </template>
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
}

.geo-map-panel__chart {
  position: absolute;
  inset: 6px;
  z-index: 0;
  min-width: 0;
  min-height: 0;
}

.geo-map-panel__toolbar {
  position: absolute;
  inset: auto auto 22px calc(50% - 242px);
  z-index: 4;
  display: flex;
  gap: 6px;
  padding: 6px;
  border: 1px solid rgba(110, 202, 212, 0.24);
  border-right: 0;
  background: rgba(6, 11, 17, 0.66);
  backdrop-filter: blur(16px);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.025),
    0 18px 36px rgba(0, 0, 0, 0.32);
}

.geo-map-panel__toolbar button {
  min-height: 34px;
  inline-size: 34px;
  padding: 0;
  border: 1px solid rgba(110, 202, 212, 0.28);
  color: var(--color-text);
  background: rgba(8, 13, 19, 0.82);
  transition:
    border-color 160ms ease,
    background 160ms ease,
    box-shadow 160ms ease,
    color 160ms ease;
}

.geo-map-panel__toolbar button:hover,
.geo-map-panel__toolbar button.is-active {
  border-color: rgba(143, 232, 240, 0.72);
  color: #e9f7f8;
  background:
    linear-gradient(180deg, rgba(110, 202, 212, 0.18), rgba(110, 202, 212, 0.06)),
    rgba(8, 13, 19, 0.92);
  box-shadow:
    inset 0 -2px 0 rgba(255, 122, 53, 0.82),
    0 0 22px rgba(110, 202, 212, 0.18);
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

.geo-map-panel__scale span,
.geo-map-panel__scale strong {
  font-size: var(--text-meta);
  letter-spacing: var(--tracking-panel);
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.geo-map-panel__scale i {
  display: block;
  height: 8px;
  border-inline: 1px solid rgba(110, 202, 212, 0.72);
  border-bottom: 2px solid rgba(110, 202, 212, 0.72);
}

.geo-map-panel__tooltip {
  position: absolute;
  z-index: 4;
  inline-size: 216px;
  display: grid;
  gap: 5px;
  padding: 12px 14px;
  border: 1px solid rgba(110, 202, 212, 0.34);
  color: var(--color-text-muted);
  background: rgba(5, 10, 16, 0.9);
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.38);
  pointer-events: none;
  transition: opacity 120ms ease;
  backdrop-filter: blur(14px);
}

.geo-map-panel__tooltip::before {
  content: '';
  position: absolute;
  inset: -1px;
  pointer-events: none;
  border: 1px solid rgba(143, 232, 240, 0.18);
  box-shadow: inset 0 -2px 0 rgba(255, 122, 53, 0.52);
}

.geo-map-panel__tooltip strong {
  color: var(--color-text);
  font-size: var(--text-small);
}

.geo-map-panel__tooltip span {
  font-size: var(--text-meta);
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

.geo-map-panel__icon--fullscreen {
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

.geo-map-panel__toolbar button.is-active .geo-map-panel__icon--fullscreen {
  transform: scale(0.82);
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
    border-right: 1px solid rgba(110, 202, 212, 0.24);
  }
}
</style>
