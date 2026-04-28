<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import KpiStrip from '../../components/shell/KpiStrip.vue'
import SideStack from '../../components/shell/SideStack.vue'
import TopBar from '../../components/shell/TopBar.vue'
import { dashboardPageModel } from '../../data/mockShell'
import AreaFilterPanel from './components/AreaFilterPanel.vue'
import DashboardAlarmPanel from './components/DashboardAlarmPanel.vue'
import DashboardGeoMap from './components/DashboardGeoMap.vue'
import {
  loadCities,
  loadDashboardAlarms,
  loadDashboardKpi,
  loadDistricts,
  loadGeoIndex,
  loadMapTooltipData,
  loadAreaOptions,
  resolveGeoAssetPath,
} from './service'
import type {
  AreaOption,
  DashboardAlarm,
  DashboardKpi,
  DashboardKpiItem,
  GeoIndex,
  MapTimePreset,
  MapTimeRange,
  MapRegion,
  MapTooltipData,
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
  path: resolveGeoAssetPath('/geo/china/100000_full.json'),
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

      <div class="dashboard-page-module__workspace">
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

        <section class="dashboard-page-module__main">
          <KpiStrip :items="kpiItems" />

          <div class="dashboard-page-module__board">
            <DashboardGeoMap
              :region="mapRegion"
              :focus-code="activeAreaId"
              :geo-index="geoIndex"
              :tooltip-data="tooltipData"
              :loading="tooltipLoading"
              @region-click="handleRegionClick"
              @region-hover="handleRegionHover"
              @reset-view="resetRegion"
            />
            <div class="dashboard-page-module__right-stack">
              <SideStack :panels="[dashboardPageModel.sidePanels[0]]" />
              <DashboardAlarmPanel :alarms="alarms" :loading="alarmLoading" />
            </div>
          </div>
        </section>
      </div>
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

.dashboard-page-module__workspace {
  display: grid;
  grid-template-columns: 284px minmax(0, 1fr);
  min-block-size: 0;
  overflow: hidden;
}

.dashboard-page-module__main {
  min-width: 0;
  min-height: 0;
  padding: var(--space-4);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: var(--space-3);
  overflow: hidden;
}

.dashboard-page-module__board {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1.62fr) minmax(320px, 0.84fr);
  gap: var(--space-3);
  overflow: hidden;
}

.dashboard-page-module__right-stack {
  min-height: 0;
  display: grid;
  grid-template-rows: minmax(0, 1fr) minmax(0, 1fr);
  gap: var(--space-3);
  overflow: hidden;
}

.dashboard-page-module :deep(.left-rail),
.dashboard-page-module :deep(.area-filter),
.dashboard-page-module :deep(.side-stack),
.dashboard-page-module :deep(.alarm-panel),
.dashboard-page-module :deep(.map-panel),
.dashboard-page-module :deep(.geo-map-panel),
.dashboard-page-module :deep(.panel),
.dashboard-page-module :deep(.map-body) {
  min-height: 0;
}

.dashboard-page-module :deep(.left-rail),
.dashboard-page-module :deep(.area-filter) {
  block-size: 100%;
}

.dashboard-page-module :deep(.side-stack) {
  block-size: 100%;
  grid-template-rows: minmax(0, 1fr);
}

@media (max-width: 1439px) {
  .dashboard-page-module__workspace {
    grid-template-columns: 264px minmax(0, 1fr);
  }

  .dashboard-page-module__board {
    grid-template-columns: minmax(0, 1.5fr) minmax(300px, 0.88fr);
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

  .dashboard-page-module__workspace,
  .dashboard-page-module__board {
    grid-template-columns: 1fr;
  }

  .dashboard-page-module__main {
    overflow: visible;
  }

  .dashboard-page-module :deep(.left-rail),
  .dashboard-page-module :deep(.area-filter) {
    border-right: 0;
    border-bottom: 1px solid var(--color-line);
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-page-module :deep(.side-stack) {
    grid-template-rows: none;
  }

  .dashboard-page-module__right-stack {
    grid-template-rows: none;
  }
}

@media (max-width: 719px) {
  .dashboard-page-module :deep(.left-rail),
  .dashboard-page-module :deep(.area-filter) {
    grid-template-columns: 1fr;
  }
}
</style>
