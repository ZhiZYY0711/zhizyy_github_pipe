<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import ModuleShell from '../../../components/business/ModuleShell.vue'
import MonitoringLineChart from '../components/MonitoringLineChart.vue'
import type {
  AreaOption,
  DimensionData,
  MonitoringMetricKey,
  MonitoringPipeOption,
  MonitoringSegmentOption,
  PipeKeyIndicators,
} from '../types'
import {
  loadMonitoringCities,
  loadMonitoringDistricts,
  loadMonitoringFilterOptions,
  loadMonitoringOverview,
  loadMonitoringProvinces,
} from '../service'

type TimePreset = '1h' | '6h' | '24h' | 'all'

const areaData = ref<DimensionData[]>([])
const pipeData = ref<DimensionData[]>([])
const keyIndicators = ref<PipeKeyIndicators>({})
const isFallback = ref(false)
const isLoading = ref(false)

const provinces = ref<AreaOption[]>([])
const cities = ref<AreaOption[]>([])
const districts = ref<AreaOption[]>([])
const pipes = ref<MonitoringPipeOption[]>([])

const selectedProvince = ref('')
const selectedCity = ref('')
const selectedDistrict = ref('')
const selectedPipeId = ref('')
const selectedSegmentIds = ref<string[]>([])
const segmentAnchorId = ref('')
const activeTimePreset = ref<TimePreset>('all')
const startTime = ref('')
const endTime = ref('')
const visibleMetrics = ref<MonitoringMetricKey[]>(['pressure', 'flow', 'temperature', 'vibration'])
const autoRefresh = ref(false)

const statusText: Record<number, string> = {
  0: '正常',
  1: '关注',
  2: '预警',
  3: '严重',
}

const timePresets: Array<{ label: string; value: TimePreset }> = [
  { label: '1h', value: '1h' },
  { label: '6h', value: '6h' },
  { label: '24h', value: '24h' },
  { label: '全部', value: 'all' },
]

const metricOptions: Array<{ label: string; value: MonitoringMetricKey }> = [
  { label: '压力', value: 'pressure' },
  { label: '流量', value: 'flow' },
  { label: '温度', value: 'temperature' },
  { label: '震动', value: 'vibration' },
]

const selectedPipe = computed(() => pipes.value.find((pipe) => pipe.id === selectedPipeId.value))
const segmentOptions = computed(() => selectedPipe.value?.segments ?? [])
const isPipeFilterEnabled = computed(() => Boolean(selectedCity.value || selectedDistrict.value))
const areaId = computed(() => selectedDistrict.value || selectedCity.value || selectedProvince.value || undefined)
const selectedAreaName = computed(() => {
  const district = districts.value.find((item) => item.code === selectedDistrict.value)
  const city = cities.value.find((item) => item.code === selectedCity.value)
  const province = provinces.value.find((item) => item.code === selectedProvince.value)
  return district?.name || city?.name || province?.name || '全国'
})

const selectedSegmentSummary = computed(() => {
  if (!selectedSegmentIds.value.length) {
    return '未选择管段'
  }

  const selected = segmentOptions.value.filter((segment) => selectedSegmentIds.value.includes(segment.id))
  if (selected.length === 1) {
    return selected[0]?.name ?? '已选 1 段'
  }

  return `S${selected[0]?.segment_order}-S${selected[selected.length - 1]?.segment_order}，已选 ${selected.length} 段`
})

const indicatorCards = computed(() => [
  {
    label: '平均温度',
    value: keyIndicators.value.ave_temperature ?? 0,
    unit: '℃',
    status: keyIndicators.value.temperature_status ?? 0,
    tone: 'is-warn',
  },
  {
    label: '平均流量',
    value: keyIndicators.value.ave_flow ?? 0,
    unit: 'm3/h',
    status: keyIndicators.value.flow_status ?? 0,
    tone: 'is-signal',
  },
  {
    label: '平均压力',
    value: keyIndicators.value.ave_pressure ?? 0,
    unit: 'MPa',
    status: keyIndicators.value.pressure_status ?? 0,
    tone: '',
  },
  {
    label: '平均震动',
    value: keyIndicators.value.ave_vibration ?? 0,
    unit: 'mm/s',
    status: keyIndicators.value.vibration_status ?? 0,
    tone: 'is-danger',
  },
])

function toTimestamp(value: string, endOfDay = false) {
  if (!value) {
    return undefined
  }

  const suffix = endOfDay ? 'T23:59:59' : 'T00:00:00'
  const timestamp = new Date(`${value}${suffix}`).getTime()
  return Number.isNaN(timestamp) ? undefined : timestamp
}

function resolveTimeRange() {
  if (activeTimePreset.value === 'all') {
    return {}
  }

  if (startTime.value || endTime.value) {
    return {
      start_time: toTimestamp(startTime.value),
      end_time: toTimestamp(endTime.value, true),
    }
  }

  const hours = activeTimePreset.value === '1h' ? 1 : activeTimePreset.value === '24h' ? 24 : 6
  return {
    start_time: Date.now() - hours * 60 * 60 * 1000,
    end_time: Date.now(),
  }
}

function buildOverviewQuery() {
  return {
    area_id: areaId.value,
    pipe_id: selectedPipeId.value || undefined,
    segment_ids: selectedSegmentIds.value,
    ...resolveTimeRange(),
  }
}

async function refreshOverview() {
  isLoading.value = true
  const model = await loadMonitoringOverview(buildOverviewQuery())
  areaData.value = model.areaData
  pipeData.value = model.pipeData
  keyIndicators.value = model.keyIndicators
  isFallback.value = model.isFallback
  isLoading.value = false
}

async function refreshTopologyOptions() {
  selectedPipeId.value = ''
  selectedSegmentIds.value = []
  segmentAnchorId.value = ''

  if (!isPipeFilterEnabled.value) {
    pipes.value = []
    return
  }

  const options = await loadMonitoringFilterOptions({
    province_code: selectedProvince.value || undefined,
    city_code: selectedCity.value || undefined,
    district_code: selectedDistrict.value || undefined,
  }).catch(() => ({ scope_level: 'NATIONAL' as const, pipes: [] }))
  pipes.value = options.pipes
}

async function selectProvince(code: string) {
  selectedProvince.value = code
  selectedCity.value = ''
  selectedDistrict.value = ''
  cities.value = code ? await loadMonitoringCities(code).catch(() => []) : []
  districts.value = []
  await refreshTopologyOptions()
}

async function selectCity(code: string) {
  selectedCity.value = code
  selectedDistrict.value = ''
  districts.value = code ? await loadMonitoringDistricts(code).catch(() => []) : []
  await refreshTopologyOptions()
}

async function selectDistrict(code: string) {
  selectedDistrict.value = code
  await refreshTopologyOptions()
}

function selectPipe(pipeId: string) {
  selectedPipeId.value = pipeId
  selectedSegmentIds.value = []
  segmentAnchorId.value = ''
}

function selectSegment(segment: MonitoringSegmentOption) {
  const segmentIndex = segmentOptions.value.findIndex((item) => item.id === segment.id)
  const anchorIndex = segmentOptions.value.findIndex((item) => item.id === segmentAnchorId.value)

  if (!segmentAnchorId.value || anchorIndex < 0 || selectedSegmentIds.value.length > 1) {
    segmentAnchorId.value = segment.id
    selectedSegmentIds.value = [segment.id]
    return
  }

  const [start, end] = [anchorIndex, segmentIndex].sort((left, right) => left - right)
  selectedSegmentIds.value = segmentOptions.value.slice(start, end + 1).map((item) => item.id)
}

function toggleMetric(metric: MonitoringMetricKey) {
  if (visibleMetrics.value.includes(metric)) {
    if (visibleMetrics.value.length > 1) {
      visibleMetrics.value = visibleMetrics.value.filter((item) => item !== metric)
    }
    return
  }

  visibleMetrics.value = [...visibleMetrics.value, metric]
}

function selectTimePreset(preset: TimePreset) {
  activeTimePreset.value = preset
  if (preset !== 'all') {
    startTime.value = ''
    endTime.value = ''
  }
}

async function resetFilters() {
  selectedProvince.value = ''
  selectedCity.value = ''
  selectedDistrict.value = ''
  selectedPipeId.value = ''
  selectedSegmentIds.value = []
  segmentAnchorId.value = ''
  cities.value = []
  districts.value = []
  pipes.value = []
  activeTimePreset.value = 'all'
  startTime.value = ''
  endTime.value = ''
  visibleMetrics.value = ['pressure', 'flow', 'temperature', 'vibration']
  autoRefresh.value = false
  await refreshOverview()
}

watch(autoRefresh, (enabled, _previous, onCleanup) => {
  if (!enabled) {
    return
  }

  const timer = window.setInterval(() => {
    void refreshOverview()
  }, 30000)
  onCleanup(() => window.clearInterval(timer))
})

onMounted(async () => {
  provinces.value = await loadMonitoringProvinces().catch(() => [])
  await refreshOverview()
})
</script>

<template>
  <ModuleShell class="monitoring-shell" active-path="/monitoring" eyebrow="Pipeline Monitoring" title="油气管网监测调度台" ops-label="刷新策略" ops-value="30S">
    <section class="monitoring-layout">
      <aside class="ops-panel filter-column">
        <div class="filter-head">
          <div>
            <span class="eyebrow">Filter</span>
            <h3 class="section-title-text">筛选控制台</h3>
          </div>
          <span class="chip">{{ selectedAreaName }}</span>
        </div>

        <section class="filter-group area-filter-group">
          <div class="filter-group__title">行政范围</div>
          <div class="area-select-grid">
            <label class="select-field">
              <span>省</span>
              <select :value="selectedProvince" @change="selectProvince(($event.target as HTMLSelectElement).value)">
                <option value="">全国</option>
                <option v-for="province in provinces" :key="province.code" :value="province.code">{{ province.name }}</option>
              </select>
            </label>
            <label class="select-field">
              <span>市</span>
              <select
                :value="selectedCity"
                :disabled="!selectedProvince"
                @change="selectCity(($event.target as HTMLSelectElement).value)"
              >
                <option value="">全部城市</option>
                <option v-for="city in cities" :key="city.code" :value="city.code">{{ city.name }}</option>
              </select>
            </label>
            <label class="select-field">
              <span>区</span>
              <select
                :value="selectedDistrict"
                :disabled="!selectedCity"
                @change="selectDistrict(($event.target as HTMLSelectElement).value)"
              >
                <option value="">全部区县</option>
                <option v-for="district in districts" :key="district.code" :value="district.code">{{ district.name }}</option>
              </select>
            </label>
          </div>
        </section>

        <section class="filter-group">
          <div class="filter-group__title">管网对象</div>
          <label class="select-field">
            <span>管道</span>
            <select
              :value="selectedPipeId"
              :disabled="!isPipeFilterEnabled || !pipes.length"
              @change="selectPipe(($event.target as HTMLSelectElement).value)"
            >
              <option value="">{{ isPipeFilterEnabled ? '请选择管道' : '请选择城市或区县' }}</option>
              <option v-for="pipe in pipes" :key="pipe.id" :value="pipe.id">{{ pipe.name }}</option>
            </select>
          </label>

          <div class="segment-picker">
            <div class="segment-picker__head">
              <span>管段范围</span>
              <small>{{ selectedSegmentSummary }}</small>
            </div>
            <div v-if="selectedPipeId && segmentOptions.length" class="segment-list">
              <button
                v-for="segment in segmentOptions"
                :key="segment.id"
                type="button"
                :class="{ 'is-active': selectedSegmentIds.includes(segment.id) }"
                @click="selectSegment(segment)"
              >
                <span>S{{ segment.segment_order }}</span>
                <strong>{{ segment.name }}</strong>
              </button>
            </div>
            <p v-else class="muted-text">{{ isPipeFilterEnabled ? '选择管道后可选择连续管段' : '全国或省级不启用管网筛选' }}</p>
          </div>
        </section>

        <section class="filter-group">
          <div class="filter-group__title">时间范围</div>
          <div class="segmented-control">
            <button
              v-for="preset in timePresets"
              :key="preset.value"
              type="button"
              :class="{ 'is-active': activeTimePreset === preset.value }"
              @click="selectTimePreset(preset.value)"
            >
              {{ preset.label }}
            </button>
          </div>
          <div class="date-range-grid">
            <label class="select-field">
              <span>开始</span>
              <input v-model="startTime" type="date" @change="activeTimePreset = 'all'" />
            </label>
            <label class="select-field">
              <span>结束</span>
              <input v-model="endTime" type="date" @change="activeTimePreset = 'all'" />
            </label>
          </div>
        </section>

        <section class="filter-group">
          <div class="filter-group__title">指标</div>
          <div class="metric-toggle-grid">
            <button
              v-for="metric in metricOptions"
              :key="metric.value"
              type="button"
              :class="{ 'is-active': visibleMetrics.includes(metric.value) }"
              @click="toggleMetric(metric.value)"
            >
              {{ metric.label }}
            </button>
          </div>
        </section>

        <section class="filter-actions">
          <button class="control-button is-primary" type="button" :disabled="isLoading" @click="refreshOverview">
            {{ isLoading ? '加载中' : '应用' }}
          </button>
          <button class="control-button" type="button" @click="resetFilters">重置</button>
          <label class="switch-field">
            <input v-model="autoRefresh" type="checkbox" />
            <span>自动刷新</span>
          </label>
        </section>
      </aside>

      <section class="monitor-stage console-scrollbar">
        <section class="monitor-screen">
          <div class="screen-header">
            <div>
              <span class="eyebrow">Key Indicators</span>
              <h2>关键指标卡与区域四维趋势</h2>
            </div>
            <div class="button-strip">
              <a class="control-button is-primary detail-entry" href="/monitoring/data">监测数据明细</a>
              <span v-if="isFallback" class="chip is-warn">Fallback</span>
            </div>
          </div>

          <div class="indicator-grid">
            <article
              v-for="card in indicatorCards"
              :key="card.label"
              class="metric-card"
              :class="card.tone"
            >
              <span>{{ card.label }}</span>
              <strong>{{ card.value }}<small>{{ card.unit }}</small></strong>
              <small class="muted-text">状态：{{ statusText[card.status] ?? '未知' }}</small>
            </article>
          </div>

          <MonitoringLineChart
            eyebrow="Area Trend"
            title="区域四维度数据"
            :points="areaData"
            :visible-metrics="visibleMetrics"
          />
        </section>

        <section class="monitor-screen pipe-screen">
          <MonitoringLineChart
            eyebrow="Pipe Trend"
            title="管道四维度数据"
            :points="pipeData"
            :visible-metrics="visibleMetrics"
          />
        </section>
      </section>
    </section>
  </ModuleShell>
</template>

<style scoped>
.monitoring-shell {
  block-size: 100dvh;
  min-block-size: 0;
  overflow: hidden;
}

.monitoring-shell :deep(.business-frame) {
  block-size: calc(100dvh - var(--space-5) * 2);
  min-block-size: 0;
  overflow: hidden;
}

.monitoring-shell :deep(.business-main) {
  display: grid;
  grid-template-rows: minmax(0, 1fr);
  overflow: hidden;
}

.monitoring-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: var(--space-3);
  height: calc(100dvh - var(--space-5) * 2 - 72px - var(--space-4) * 2);
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.filter-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.filter-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: var(--space-3);
}

.filter-head h3 {
  line-height: 1.1;
}

.filter-group {
  display: grid;
  gap: 6px;
  padding: 7px;
  border: 1px solid var(--color-line);
  background: rgba(255, 255, 255, 0.014);
}

.area-filter-group {
  gap: 8px;
}

.area-select-grid {
  display: grid;
  gap: 6px;
}

.filter-group__title,
.segment-picker__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--color-accent-cyan);
  font-size: var(--text-micro);
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
}

.segment-picker__head small {
  max-width: 150px;
  color: var(--color-text-muted);
  text-align: right;
  text-transform: none;
  letter-spacing: 0;
}

.select-field,
.segment-picker {
  display: grid;
  gap: 4px;
}

.select-field {
  grid-template-columns: 42px minmax(0, 1fr);
  align-items: center;
}

.select-field span,
.switch-field span {
  color: var(--color-text-muted);
  font-size: var(--text-meta);
}

.select-field select,
.select-field input {
  width: 100%;
  min-width: 0;
  min-height: 28px;
  border: 1px solid var(--color-line);
  color: var(--color-text);
  background: #091018;
  padding: 0 10px;
  box-sizing: border-box;
  color-scheme: dark;
}

.date-range-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 4px;
}

.date-range-grid .select-field {
  grid-template-columns: 1fr;
}

.date-range-grid .select-field span {
  display: none;
}

.select-field select:disabled {
  opacity: 0.58;
}

.segmented-control,
.metric-toggle-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
}

.segmented-control button,
.metric-toggle-grid button,
.segment-list button {
  min-height: 30px;
  border: 1px solid var(--color-line);
  color: var(--color-text-muted);
  background: rgba(255, 255, 255, 0.02);
}

.segmented-control button:hover,
.segmented-control button.is-active,
.metric-toggle-grid button:hover,
.metric-toggle-grid button.is-active,
.segment-list button:hover,
.segment-list button.is-active {
  color: var(--color-text);
  border-color: var(--color-line-strong);
  background: linear-gradient(180deg, rgba(110, 202, 212, 0.08), rgba(255, 255, 255, 0.01));
}

.segment-list {
  display: grid;
  max-height: 56px;
  overflow: hidden;
  gap: 6px;
}

.segment-list button {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  min-height: 30px;
  padding: 4px 8px;
  text-align: left;
}

.segment-list button span {
  color: var(--color-accent-orange);
  font-size: var(--text-meta);
}

.segment-list button strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--text-meta);
}

.filter-actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-2);
  margin-top: auto;
}

.switch-field {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 28px;
  white-space: nowrap;
}

.monitor-stage {
  display: block;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 4px;
}

.monitor-screen {
  display: grid;
  grid-template-rows: auto auto 420px;
  gap: var(--space-3);
  min-height: 620px;
  overflow: hidden;
}

.monitor-screen + .monitor-screen {
  grid-template-rows: 460px;
  margin-top: 6px;
}

.screen-header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: var(--space-3);
}

.screen-header h2 {
  margin: 4px 0 0;
  font-size: 20px;
  line-height: var(--leading-display);
}

.indicator-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-3);
}

.metric-card strong {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.metric-card strong small {
  font-size: var(--text-meta);
  color: var(--color-text-muted);
}

.detail-entry {
  min-height: 40px;
  padding-inline: var(--space-4);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 28px rgba(231, 104, 45, 0.2);
}

.detail-entry:hover {
  color: #160b06;
  border-color: transparent;
  background: linear-gradient(135deg, #f18a55, var(--color-accent-orange));
  box-shadow: 0 12px 30px rgba(231, 104, 45, 0.26);
}

@media (max-width: 1179px) {
  .monitoring-layout {
    grid-template-columns: 1fr;
    height: auto;
    min-height: 0;
    overflow: visible;
  }

  .filter-column {
    height: auto;
    overflow: visible;
  }

  .monitor-stage {
    height: auto;
    overflow: visible;
    display: grid;
    padding-right: 0;
  }

  .monitor-screen {
    min-height: 620px;
  }

  .indicator-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 719px) {
  .indicator-grid,
  .segmented-control,
  .metric-toggle-grid {
    grid-template-columns: 1fr;
  }

  .screen-header {
    align-items: start;
    flex-direction: column;
  }
}
</style>
