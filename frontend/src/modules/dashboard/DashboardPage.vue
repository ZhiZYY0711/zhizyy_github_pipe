<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import KpiStrip from '../../components/shell/KpiStrip.vue'
import SideStack from '../../components/shell/SideStack.vue'
import TopBar from '../../components/shell/TopBar.vue'
import { dashboardPageModel } from '../../data/mockShell'
import AreaFilterPanel from './components/AreaFilterPanel.vue'
import DashboardAlarmPanel from './components/DashboardAlarmPanel.vue'
import DashboardGeoMap from './components/DashboardGeoMap.vue'
import { trunkPipelines } from './pipelineOverlayData'
import {
  loadCities,
  loadDashboardAlarms,
  loadDashboardKpi,
  loadDistricts,
  loadGeoIndex,
  loadMapTooltipData,
  loadAreaOptions,
} from './service'
import type {
  AreaOption,
  DashboardAlarm,
  DashboardKpi,
  DashboardMapLayerKey,
  DashboardKpiItem,
  GeoIndex,
  MapTimePreset,
  MapTimeRange,
  MapRegion,
  MapTooltipData,
  TrunkPipeline,
} from './types'

const provinces = ref<AreaOption[]>([])
const cities = ref<AreaOption[]>([])
const districts = ref<AreaOption[]>([])
const selectedProvince = ref('')
const selectedCity = ref('')
const selectedDistrict = ref('')
const geoIndex = ref<GeoIndex | null>(null)
const mapRegion = ref<MapRegion>({
  code: '100000',
  name: '全国',
  level: 'country',
  path: '',
})
const focusRegion = ref<MapRegion | null>(null)
const kpi = ref<Required<DashboardKpi>>({
  sensor_numbers: 0,
  abnormal_sensor_numbers: 0,
  warnings: 0,
  underway_task: 0,
  overtime_task: 0,
})
const alarms = ref<DashboardAlarm[]>([])
const alarmLoading = ref(false)
const tooltipData = ref<MapTooltipData | null>(null)
const tooltipLoading = ref(false)
const tooltipCache = new Map<string, { data: MapTooltipData; expiresAt: number }>()
let hoverTimer: number | undefined
let hoverRequestId = 0

const tooltipDebounceMs = 260
const tooltipCacheTtlMs = 5 * 60 * 1000
const timeRange = ref<MapTimeRange>(createDefaultTimeRange())
const activeTimePreset = ref<MapTimePreset>('all')
const visibleMapLayers = ref<Record<DashboardMapLayerKey, boolean>>({
  regions: true,
  pipelines: true,
  nodes: true,
  alarms: true,
})
const selectedPipeline = ref<TrunkPipeline | null>(null)

const hotRegions: AreaOption[] = [
  { code: '130000', name: '河北省' },
  { code: '130100', name: '石家庄市' },
  { code: '370000', name: '山东省' },
  { code: '110000', name: '北京市' },
  { code: '310000', name: '上海市' },
  { code: '440000', name: '广东省' },
]

const activeAreaId = computed(() => selectedDistrict.value || selectedCity.value || selectedProvince.value || undefined)
const activeAreaName = computed(() => focusRegion.value?.name ?? '全国')
const tooltipQueryRange = computed(() => toTimestampRange(timeRange.value))
const selectedPipelineStatusText = computed(() => {
  if (!selectedPipeline.value) {
    return '行政区域监控态势'
  }

  if (selectedPipeline.value.status === 'critical') {
    return '严重异常'
  }

  if (selectedPipeline.value.status === 'warning') {
    return '风险关注'
  }

  return '运行正常'
})
const kpiItems = computed<DashboardKpiItem[]>(() => [
  { label: '传感器总数', value: formatCount(kpi.value.sensor_numbers), tone: 'default' },
  { label: '异常传感器', value: formatCount(kpi.value.abnormal_sensor_numbers), tone: 'warn' },
  { label: '今日告警', value: formatCount(kpi.value.warnings), tone: 'warn' },
  { label: '进行任务', value: formatCount(kpi.value.underway_task), tone: 'signal' },
  { label: '超时任务', value: formatCount(kpi.value.overtime_task), tone: 'warn' },
])

onMounted(async () => {
  const [index, provinceOptions] = await Promise.all([
    loadGeoIndex(),
    loadAreaOptions().catch(() => []),
  ])
  geoIndex.value = index
  provinces.value = provinceOptions
  mapRegion.value = {
    code: index.china.code,
    name: index.china.name,
    level: 'country',
    path: index.china.path,
  }
  syncMapRegion()
  await refreshDashboardData()
})

onBeforeUnmount(() => {
  clearHoverTimer()
})

async function resetRegion() {
  selectedPipeline.value = null
  selectedProvince.value = ''
  selectedCity.value = ''
  selectedDistrict.value = ''
  cities.value = []
  districts.value = []
  tooltipData.value = null
  tooltipCache.clear()
  syncMapRegion()

  await refreshDashboardData()
}

async function selectProvince(code: string) {
  selectedPipeline.value = null
  selectedProvince.value = code
  selectedCity.value = ''
  selectedDistrict.value = ''
  districts.value = []
  tooltipData.value = null
  tooltipCache.clear()

  if (!code) {
    cities.value = []
    await resetRegion()
    return
  }

  cities.value = await loadCities(code).catch(() => [])
  syncMapRegion()
  await refreshDashboardData()
}

async function selectCity(code: string) {
  selectedPipeline.value = null
  selectedCity.value = code
  selectedDistrict.value = ''
  tooltipData.value = null
  tooltipCache.clear()

  if (!code) {
    districts.value = []
    syncMapRegion()
    await refreshDashboardData()
    return
  }

  districts.value = await loadDistricts(code).catch(() => [])
  syncMapRegion()
  await refreshDashboardData()
}

async function selectDistrict(code: string) {
  selectedPipeline.value = null
  selectedDistrict.value = code
  tooltipData.value = null
  tooltipCache.clear()

  if (!code) {
    syncMapRegion()
    await refreshDashboardData()
    return
  }

  syncMapRegion()
  await refreshDashboardData()
}

async function selectHotRegion(code: string) {
  selectedPipeline.value = null

  if (isProvinceCode(code)) {
    await selectProvince(code)
    return
  }

  const provinceCode = `${code.slice(0, 2)}0000`

  if (selectedProvince.value !== provinceCode) {
    selectedProvince.value = provinceCode
    cities.value = await loadCities(provinceCode).catch(() => [])
  }

  if (isCityCode(code)) {
    await selectCity(code)
  }
}

async function handleRegionClick(payload: { code: string }) {
  selectedPipeline.value = null
  const code = payload.code

  if (isProvinceCode(code)) {
    await selectProvince(code)
    return
  }

  if (isCityCode(code)) {
    const provinceCode = `${code.slice(0, 2)}0000`

    if (selectedProvince.value !== provinceCode) {
      selectedProvince.value = provinceCode
      cities.value = await loadCities(provinceCode).catch(() => [])
    }

    await selectCity(code)
    return
  }

  if (code.length === 6) {
    await selectDistrict(code)
  }
}

async function handleRegionHover(payload: { code: string; name: string }) {
  clearHoverTimer()
  const requestId = ++hoverRequestId

  const cached = readTooltipCache(payload.code)
  if (cached) {
    tooltipData.value = cached
    tooltipLoading.value = false
    return
  }

  hoverTimer = window.setTimeout(() => {
    void loadHoveredRegion(payload, requestId)
  }, tooltipDebounceMs)
}

function updateTimeRange(range: MapTimeRange) {
  activeTimePreset.value = 'custom'
  timeRange.value = normalizeTimeRange(range)
  tooltipData.value = null
  tooltipCache.clear()
  void refreshDashboardData()
}

function selectTimePreset(preset: MapTimePreset) {
  activeTimePreset.value = preset
  timeRange.value = createPresetTimeRange(preset)
  tooltipData.value = null
  tooltipCache.clear()
  void refreshDashboardData()
}

function toggleMapLayer(layer: DashboardMapLayerKey) {
  visibleMapLayers.value = {
    ...visibleMapLayers.value,
    [layer]: !visibleMapLayers.value[layer],
  }
}

function handlePipelineClick(pipeline: TrunkPipeline) {
  selectedPipeline.value = pipeline
}

async function refreshDashboardData() {
  alarmLoading.value = true

  try {
    const [kpiResult, alarmResult] = await Promise.allSettled([
      loadDashboardKpi(activeAreaId.value),
      loadDashboardAlarms(activeAreaId.value, tooltipQueryRange.value),
    ])

    if (kpiResult.status === 'fulfilled') {
      kpi.value = kpiResult.value
    }

    alarms.value = alarmResult.status === 'fulfilled' ? alarmResult.value : []
  } finally {
    alarmLoading.value = false
  }
}

function syncMapRegion() {
  const index = geoIndex.value

  if (!index) {
    return
  }

  const focusCode = activeAreaId.value
  focusRegion.value = focusCode ? createMapRegion(focusCode) : null

  if (selectedDistrict.value && selectedCity.value) {
    mapRegion.value = createMapRegion(selectedCity.value) ?? mapRegion.value
    return
  }

  if (selectedCity.value && selectedProvince.value) {
    mapRegion.value = createMapRegion(selectedProvince.value) ?? mapRegion.value
    return
  }

  mapRegion.value = {
    code: index.china.code,
    name: index.china.name,
    level: 'country',
    path: index.china.path,
  }
}

function createMapRegion(code: string) {
  const index = geoIndex.value

  if (!index || !code) {
    return null
  }

  const entry = isProvinceCode(code)
    ? index.province[code]
    : isCityCode(code)
      ? index.city[code]
      : index.district[code]

  if (!entry) {
    return null
  }

  return {
    code: entry.code,
    name: entry.name,
    level: isProvinceCode(code) ? 'province' : isCityCode(code) ? 'city' : 'district',
    path: entry.path,
  } satisfies MapRegion
}

function isProvinceCode(code: string) {
  return /^\d{2}0000$/.test(code)
}

function isCityCode(code: string) {
  return /^\d{4}00$/.test(code) && !isProvinceCode(code)
}

function formatCount(value: number | undefined) {
  return new Intl.NumberFormat('zh-CN').format(value ?? 0)
}

async function loadHoveredRegion(payload: { code: string; name: string }, requestId: number) {
  tooltipLoading.value = true

  try {
    const data = await loadMapTooltipData(payload.code, payload.name, tooltipQueryRange.value)

    tooltipCache.set(createTooltipCacheKey(payload.code), {
      data,
      expiresAt: Date.now() + tooltipCacheTtlMs,
    })

    if (requestId === hoverRequestId) {
      tooltipData.value = data
    }
  } finally {
    if (requestId === hoverRequestId) {
      tooltipLoading.value = false
    }
  }
}

function readTooltipCache(code: string) {
  const cached = tooltipCache.get(createTooltipCacheKey(code))

  if (!cached) {
    return null
  }

  if (cached.expiresAt <= Date.now()) {
    tooltipCache.delete(code)
    return null
  }

  return cached.data
}

function clearHoverTimer() {
  if (hoverTimer) {
    window.clearTimeout(hoverTimer)
    hoverTimer = undefined
  }
}

function createTooltipCacheKey(code: string) {
  const range = tooltipQueryRange.value

  return `${code}:${range.startTime ?? ''}:${range.endTime ?? ''}`
}

function createDefaultTimeRange() {
  return {
    startDate: '',
    endDate: '',
  }
}

function createPresetTimeRange(preset: MapTimePreset) {
  if (preset === 'all') {
    return createDefaultTimeRange()
  }

  const endDate = new Date()
  const days = preset === 'week' ? 7 : preset === 'month' ? 30 : 90
  const startDate = new Date(endDate.getTime() - days * 24 * 60 * 60 * 1000)

  return {
    startDate: formatDateInput(startDate),
    endDate: formatDateInput(endDate),
  }
}

function normalizeTimeRange(range: MapTimeRange) {
  if (range.startDate && range.endDate && range.startDate > range.endDate) {
    return {
      startDate: range.endDate,
      endDate: range.startDate,
    }
  }

  return range
}

function toTimestampRange(range: MapTimeRange) {
  return {
    startTime: range.startDate ? new Date(`${range.startDate}T00:00:00`).getTime() : undefined,
    endTime: range.endDate ? new Date(`${range.endDate}T23:59:59.999`).getTime() : undefined,
  }
}

function formatDateInput(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}
</script>

<template>
  <section class="dashboard-page-module">
    <div class="dashboard-page-module__frame">
      <TopBar
        :nav-items="dashboardPageModel.navItems"
      />

      <section class="dashboard-page-module__stage">
        <DashboardGeoMap
          :region="mapRegion"
          :focus-code="activeAreaId"
          :geo-index="geoIndex"
          :tooltip-data="tooltipData"
          :loading="tooltipLoading"
          :pipelines="trunkPipelines"
          :visible-layers="visibleMapLayers"
          :selected-pipeline-id="selectedPipeline?.id"
          @region-click="handleRegionClick"
          @region-hover="handleRegionHover"
          @pipeline-click="handlePipelineClick"
          @reset-view="resetRegion"
        />

        <div class="dashboard-page-module__hud dashboard-page-module__hud--kpis">
          <KpiStrip :items="kpiItems" />
        </div>

        <div class="dashboard-page-module__hud dashboard-page-module__hud--filters">
          <AreaFilterPanel
            :provinces="provinces"
            :cities="cities"
            :districts="districts"
            :selected-province="selectedProvince"
            :selected-city="selectedCity"
            :selected-district="selectedDistrict"
            :hot-regions="hotRegions"
            :active-area-name="activeAreaName"
            :time-range="timeRange"
            :active-time-preset="activeTimePreset"
            @select-province="selectProvince"
            @select-city="selectCity"
            @select-district="selectDistrict"
            @select-hot-region="selectHotRegion"
            @update-time-range="updateTimeRange"
            @select-time-preset="selectTimePreset"
          />
        </div>

        <div class="dashboard-page-module__hud dashboard-page-module__hud--events">
          <SideStack :panels="[dashboardPageModel.sidePanels[0]]" />
          <DashboardAlarmPanel :alarms="alarms" :loading="alarmLoading" />
        </div>

        <section class="dashboard-page-module__focus-card panel">
          <span class="eyebrow">Current Focus</span>
          <h2>{{ selectedPipeline?.name ?? activeAreaName }}</h2>
          <p>{{ selectedPipelineStatusText }}</p>
        </section>

        <section class="dashboard-page-module__layer-control panel" aria-label="地图图层控制">
          <button
            type="button"
            :class="{ 'is-active': visibleMapLayers.regions }"
            @click="toggleMapLayer('regions')"
          >
            行政区
          </button>
          <button
            type="button"
            :class="{ 'is-active': visibleMapLayers.pipelines }"
            @click="toggleMapLayer('pipelines')"
          >
            主干线
          </button>
          <button
            type="button"
            :class="{ 'is-active': visibleMapLayers.nodes }"
            @click="toggleMapLayer('nodes')"
          >
            节点
          </button>
          <button
            type="button"
            :class="{ 'is-active': visibleMapLayers.alarms }"
            @click="toggleMapLayer('alarms')"
          >
            告警
          </button>
        </section>
      </section>
    </div>
  </section>
</template>

<style scoped>
.dashboard-page-module {
  position: relative;
  min-block-size: 100dvh;
  block-size: 100dvh;
  overflow: hidden;
  padding: var(--space-5);
  background-image:
    linear-gradient(rgba(96, 121, 139, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(96, 121, 139, 0.06) 1px, transparent 1px);
  background-size: 28px 28px;
  animation: dashboard-grid-drift 26s linear infinite;
}

.dashboard-page-module::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 16% 14%, rgba(110, 202, 212, 0.06), transparent 24%),
    radial-gradient(circle at 84% 18%, rgba(231, 104, 45, 0.08), transparent 20%),
    radial-gradient(circle at 58% 78%, rgba(110, 202, 212, 0.04), transparent 18%);
  opacity: 0.9;
  animation: dashboard-scan 7s ease-in-out infinite alternate;
}

@keyframes dashboard-grid-drift {
  from {
    background-position: 0 0;
  }

  to {
    background-position: 28px 28px;
  }
}

@keyframes dashboard-scan {
  from {
    opacity: 0.7;
    transform: translateY(-6px);
  }

  to {
    opacity: 1;
    transform: translateY(6px);
  }
}

.dashboard-page-module__frame {
  position: relative;
  z-index: 1;
  block-size: 100%;
  display: grid;
  grid-template-rows: 72px minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid var(--color-line);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.015), rgba(255, 255, 255, 0.004)),
    var(--color-bg-elevated);
  box-shadow: var(--shadow-shell);
}

.dashboard-page-module__stage {
  position: relative;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.dashboard-page-module__stage::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background:
    linear-gradient(90deg, rgba(5, 10, 16, 0.68), transparent 24%, transparent 64%, rgba(5, 10, 16, 0.72)),
    linear-gradient(180deg, rgba(5, 10, 16, 0.28), transparent 42%, rgba(5, 10, 16, 0.48));
}

.dashboard-page-module__hud,
.dashboard-page-module__focus-card,
.dashboard-page-module__layer-control {
  position: absolute;
  z-index: 2;
}

.dashboard-page-module__hud--kpis {
  inset: var(--space-4) 360px auto 340px;
}

.dashboard-page-module__hud--filters {
  inset: var(--space-4) auto var(--space-4) var(--space-4);
  inline-size: 300px;
}

.dashboard-page-module__hud--events {
  inset: var(--space-4) var(--space-4) var(--space-4) auto;
  inline-size: 336px;
  display: grid;
  grid-template-rows: minmax(0, 1fr) minmax(0, 1fr);
  gap: var(--space-3);
}

.dashboard-page-module__focus-card {
  inset: auto auto var(--space-4) 336px;
  inline-size: min(340px, 30vw);
  padding: var(--space-4);
  border-color: rgba(110, 202, 212, 0.28);
  background:
    linear-gradient(135deg, rgba(110, 202, 212, 0.12), transparent 40%),
    rgba(6, 11, 17, 0.72);
  backdrop-filter: blur(18px);
}

.dashboard-page-module__focus-card h2 {
  margin: var(--space-2) 0 var(--space-1);
  color: var(--color-text);
  font-size: clamp(1rem, 1.4vw, 1.45rem);
  line-height: 1.1;
}

.dashboard-page-module__focus-card p {
  margin: 0;
  color: var(--color-text-muted);
}

.dashboard-page-module__layer-control {
  inset: auto var(--space-4) var(--space-4) auto;
  display: flex;
  gap: 6px;
  padding: 8px;
  border-color: rgba(110, 202, 212, 0.24);
  background: rgba(6, 11, 17, 0.76);
  backdrop-filter: blur(18px);
}

.dashboard-page-module__layer-control button {
  min-block-size: 32px;
  padding: 0 10px;
  border: 1px solid rgba(143, 166, 182, 0.22);
  color: var(--color-text-muted);
  background: rgba(8, 13, 19, 0.62);
}

.dashboard-page-module__layer-control button.is-active {
  border-color: rgba(110, 202, 212, 0.62);
  color: var(--color-text);
  background: rgba(110, 202, 212, 0.16);
  box-shadow: 0 0 18px rgba(110, 202, 212, 0.12);
}

.dashboard-page-module :deep(.area-filter),
.dashboard-page-module :deep(.side-stack),
.dashboard-page-module :deep(.alarm-panel),
.dashboard-page-module :deep(.geo-map-panel),
.dashboard-page-module :deep(.panel),
.dashboard-page-module :deep(.map-body) {
  min-height: 0;
}

.dashboard-page-module :deep(.area-filter) {
  block-size: 100%;
  border: 1px solid rgba(110, 202, 212, 0.2);
  background: rgba(6, 11, 17, 0.76);
  backdrop-filter: blur(18px);
}

.dashboard-page-module :deep(.side-stack) {
  block-size: 100%;
  grid-template-rows: minmax(0, 1fr);
}

@media (max-width: 1439px) {
  .dashboard-page-module__hud--kpis {
    inset-inline: 320px 332px;
  }

  .dashboard-page-module__hud--filters {
    inline-size: 282px;
  }

  .dashboard-page-module__hud--events {
    inline-size: 310px;
  }
}

@media (max-width: 1179px) {
  .dashboard-page-module {
    block-size: auto;
    min-block-size: 100dvh;
    padding: var(--space-4);
    overflow: auto;
  }

  .dashboard-page-module__frame {
    block-size: auto;
    grid-template-rows: auto minmax(0, 1fr);
  }

  .dashboard-page-module__stage {
    display: grid;
    grid-template-columns: 1fr;
    gap: var(--space-3);
    padding: var(--space-3);
    overflow: visible;
  }

  .dashboard-page-module__hud,
  .dashboard-page-module__focus-card,
  .dashboard-page-module__layer-control {
    position: relative;
    inset: auto;
    inline-size: auto;
    z-index: 2;
  }

  .dashboard-page-module :deep(.area-filter) {
    border-right: 0;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-page-module :deep(.side-stack) {
    grid-template-rows: none;
  }

  .dashboard-page-module__hud--events {
    grid-template-rows: none;
  }
}

@media (max-width: 719px) {
  .dashboard-page-module :deep(.area-filter) {
    grid-template-columns: 1fr;
  }

  .dashboard-page-module__layer-control {
    grid-template-columns: 1fr;
  }

  .dashboard-page-module__layer-control {
    display: grid;
  }
}
</style>
