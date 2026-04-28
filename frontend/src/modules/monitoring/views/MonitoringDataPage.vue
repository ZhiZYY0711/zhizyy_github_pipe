<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import ModuleShell from '../../../components/business/ModuleShell.vue'
import type {
  AreaOption,
  MonitoringIndicator,
  MonitoringPipeOption,
  MonitoringQuery,
  MonitoringRecord,
  MonitoringSegmentOption,
  PagedResult,
} from '../types'
import {
  loadMonitoringCities,
  loadMonitoringDataIndicators,
  loadMonitoringDataRows,
  loadMonitoringDistricts,
  loadMonitoringFilterOptions,
  loadMonitoringProvinces,
} from '../service'

type TimePreset = '1h' | '6h' | '24h' | 'all'
type TableMode = 'paged' | 'long'

const indicators = ref<MonitoringIndicator>({})
const page = ref<PagedResult<MonitoringRecord>>({ records: [], total: 0, page: 1, pageSize: 50 })
const selected = ref<MonitoringRecord | null>(null)
const isFallback = ref(false)
const isLoadingIndicators = ref(true)
const isLoadingRows = ref(true)
const isLoadingMore = ref(false)
const showMoreFilters = ref(false)
const showBackTop = ref(false)
const tableMode = ref<TableMode>('paged')

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

const pageSize = 50
const timePresets: Array<{ label: string; value: TimePreset }> = [
  { label: '1h', value: '1h' },
  { label: '6h', value: '6h' },
  { label: '24h', value: '24h' },
  { label: '全部', value: 'all' },
]

const selectedPipe = computed(() => pipes.value.find((pipe) => pipe.id === selectedPipeId.value))
const segmentOptions = computed(() => selectedPipe.value?.segments ?? [])
const isPipeFilterEnabled = computed(() => Boolean(selectedCity.value || selectedDistrict.value))
const areaId = computed(() => selectedDistrict.value || selectedCity.value || selectedProvince.value || undefined)
const hasMore = computed(() => page.value.records.length < page.value.total)

const selectedAreaName = computed(() => {
  const district = districts.value.find((item) => item.code === selectedDistrict.value)
  const city = cities.value.find((item) => item.code === selectedCity.value)
  const province = provinces.value.find((item) => item.code === selectedProvince.value)
  return district?.name || city?.name || province?.name || '全国'
})

const selectedSegmentSummary = computed(() => {
  if (!selectedSegmentIds.value.length) {
    return '全部管段'
  }

  const selectedSegments = segmentOptions.value.filter((segment) => selectedSegmentIds.value.includes(segment.id))
  if (selectedSegments.length === 1) {
    return selectedSegments[0]?.name ?? '已选 1 段'
  }

  return `S${selectedSegments[0]?.segment_order}-S${selectedSegments[selectedSegments.length - 1]?.segment_order}`
})

function statusRank(status: string) {
  if (status.includes('高危')) return 3
  if (status.includes('危险')) return 2
  if (status.includes('良好')) return 1
  return 0
}

function compareRisk(left: MonitoringRecord, right: MonitoringRecord) {
  const riskDiff = statusRank(right.data_status) - statusRank(left.data_status)
  if (riskDiff !== 0) {
    return riskDiff
  }

  return new Date(right.monitor_time).getTime() - new Date(left.monitor_time).getTime()
}

const sortedRecords = computed(() => [...page.value.records].sort(compareRisk))
const priorityRecords = computed(() => sortedRecords.value.slice(0, 3))

function stateClass(status: string) {
  if (status.includes('高危')) return 'is-danger'
  if (status.includes('危险')) return 'is-warn'
  return 'is-good'
}

function toDateTime(value: string, endOfDay = false) {
  if (!value) {
    return undefined
  }

  return `${value} ${endOfDay ? '23:59:59' : '00:00:00'}`
}

function formatDateTime(timestamp: number) {
  const date = new Date(timestamp)
  const pad = (value: number) => String(value).padStart(2, '0')
  return [
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`,
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`,
  ].join(' ')
}

function resolveTimeRange() {
  if (startTime.value || endTime.value) {
    return {
      monitor_start_time: toDateTime(startTime.value),
      monitor_end_time: toDateTime(endTime.value, true),
    }
  }

  if (activeTimePreset.value === 'all') {
    return {}
  }

  const hours = activeTimePreset.value === '1h' ? 1 : activeTimePreset.value === '24h' ? 24 : 6
  return {
    monitor_start_time: formatDateTime(Date.now() - hours * 60 * 60 * 1000),
    monitor_end_time: formatDateTime(Date.now()),
  }
}

function buildQuery(pageNum: number): MonitoringQuery {
  const query: MonitoringQuery = {
    page: pageNum,
    page_size: pageSize,
    area_id: areaId.value,
    pipeline_id: selectedPipeId.value || undefined,
    pipeline_name: selectedPipe.value?.name,
    ...resolveTimeRange(),
  }

  if (selectedSegmentIds.value.length === 1) {
    query.pipe_segment_id = selectedSegmentIds.value[0]
  } else if (selectedSegmentIds.value.length > 1) {
    query.pipe_segment_ids = selectedSegmentIds.value
  }

  return query
}

async function refreshIndicators() {
  isLoadingIndicators.value = true
  const model = await loadMonitoringDataIndicators(areaId.value)
  indicators.value = model.indicators
  isFallback.value = model.isFallback
  isLoadingIndicators.value = false
}

async function loadRows(pageNum = 1, append = false) {
  if (append) {
    isLoadingMore.value = true
  } else {
    isLoadingRows.value = true
  }

  const model = await loadMonitoringDataRows(buildQuery(pageNum))
  page.value = append
    ? {
        ...model.page,
        records: [...page.value.records, ...model.page.records].sort(compareRisk),
      }
    : {
        ...model.page,
        records: [...model.page.records].sort(compareRisk),
      }
  isFallback.value = isFallback.value || model.isFallback
  isLoadingRows.value = false
  isLoadingMore.value = false
}

async function applyFilter() {
  await Promise.all([refreshIndicators(), loadRows(1)])
}

async function resetFilter() {
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
  showMoreFilters.value = false
  await applyFilter()
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

function selectTimePreset(preset: TimePreset) {
  activeTimePreset.value = preset
  if (preset !== 'all') {
    startTime.value = ''
    endTime.value = ''
  }
}

async function switchTableMode(mode: TableMode) {
  if (tableMode.value === mode) {
    return
  }

  tableMode.value = mode
  await loadRows(1)
}

async function goPage(nextPage: number) {
  if (nextPage < 1 || isLoadingRows.value) {
    return
  }

  await loadRows(nextPage)
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function handleScroll() {
  showBackTop.value = window.scrollY > 420

  if (tableMode.value !== 'long' || isLoadingRows.value || isLoadingMore.value || !hasMore.value || isFallback.value) {
    return
  }

  const distanceToBottom = document.documentElement.scrollHeight - window.innerHeight - window.scrollY
  if (distanceToBottom < 260) {
    void loadRows((page.value.page || 1) + 1, true)
  }
}

onMounted(async () => {
  provinces.value = await loadMonitoringProvinces().catch(() => [])
  window.addEventListener('scroll', handleScroll, { passive: true })
  await applyFilter()
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<template>
  <ModuleShell
    active-path="/monitoring/data"
    eyebrow="Monitoring Data"
    title="监测数据详情"
    ops-label="数据源"
    :ops-value="isFallback ? 'Fallback' : 'API'"
  >
    <div class="page-headline">
      <div>
        <span class="eyebrow">Data Core</span>
        <h2>危险数据优先，高密度明细承接</h2>
      </div>
      <a class="control-button" href="/monitoring">返回监测概览</a>
    </div>

    <section class="grid-auto monitoring-metrics">
      <article class="metric-card">
        <span>总记录</span>
        <strong>{{ isLoadingIndicators ? '加载中' : (indicators.total_records || page.total) }}</strong>
        <small class="metric-sub">今日新增 {{ isLoadingIndicators ? '加载中' : (indicators.today || 0) }}</small>
      </article>
      <article class="metric-card is-danger">
        <span>高危</span>
        <strong>{{ isLoadingIndicators ? '加载中' : (indicators.critical_count || 0) }}</strong>
      </article>
      <article class="metric-card is-warn">
        <span>危险</span>
        <strong>{{ isLoadingIndicators ? '加载中' : (indicators.danger_count || 0) }}</strong>
      </article>
      <article class="metric-card">
        <span>良好</span>
        <strong>{{ isLoadingIndicators ? '加载中' : (indicators.good_count || 0) }}</strong>
      </article>
      <article class="metric-card is-signal">
        <span>安全</span>
        <strong>{{ isLoadingIndicators ? '加载中' : (indicators.safe_count || 0) }}</strong>
      </article>
    </section>

    <section v-if="priorityRecords.length" class="risk-strip">
      <article
        v-for="row in priorityRecords"
        :key="row.id"
        class="data-card risk-card"
        :class="stateClass(row.data_status)"
      >
        <span class="eyebrow">{{ row.data_status }}</span>
        <h3>{{ row.sensor_id }} / {{ row.pipeline_name }}</h3>
        <p class="muted-text">{{ row.pipe_segment_name || '未绑定管段' }}</p>
        <strong>压力 {{ row.pressure }} MPa</strong>
        <p class="muted-text">流量 {{ row.flow }} / 温度 {{ row.temperature }} / 振动 {{ row.vibration }}</p>
        <button class="control-button is-primary" @click="selected = row">查看详情</button>
      </article>
    </section>

    <section class="table-panel monitoring-table-panel">
      <div class="panel-header table-toolbar">
        <div>
          <span class="eyebrow">Dense Table</span>
          <h3 class="panel-title-text">全量监测数据</h3>
        </div>
        <div class="table-mode-switch" aria-label="表格模式">
          <button type="button" :class="{ 'is-active': tableMode === 'paged' }" @click="switchTableMode('paged')">分页</button>
          <button type="button" :class="{ 'is-active': tableMode === 'long' }" @click="switchTableMode('long')">长屏</button>
        </div>
      </div>

      <div class="compact-filter">
        <label class="select-field">
          <span>省份</span>
          <select :value="selectedProvince" @change="selectProvince(($event.target as HTMLSelectElement).value)">
            <option value="">全国</option>
            <option v-for="province in provinces" :key="province.code" :value="province.code">{{ province.name }}</option>
          </select>
        </label>
        <label class="select-field">
          <span>城市</span>
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
          <span>区县</span>
          <select
            :value="selectedDistrict"
            :disabled="!selectedCity"
            @change="selectDistrict(($event.target as HTMLSelectElement).value)"
          >
            <option value="">全部区县</option>
            <option v-for="district in districts" :key="district.code" :value="district.code">{{ district.name }}</option>
          </select>
        </label>
        <label class="select-field">
          <span>管道</span>
          <select
            :value="selectedPipeId"
            :disabled="!isPipeFilterEnabled || !pipes.length"
            @change="selectPipe(($event.target as HTMLSelectElement).value)"
          >
            <option value="">{{ isPipeFilterEnabled ? '全部管道' : '市/区后启用' }}</option>
            <option v-for="pipe in pipes" :key="pipe.id" :value="pipe.id">{{ pipe.name }}</option>
          </select>
        </label>
        <div class="segmented-control">
          <button
            v-for="preset in timePresets"
            :key="preset.value"
            type="button"
            :class="{ 'is-active': activeTimePreset === preset.value && !startTime && !endTime }"
            @click="selectTimePreset(preset.value)"
          >
            {{ preset.label }}
          </button>
        </div>
        <button class="control-button" type="button" @click="showMoreFilters = !showMoreFilters">
          {{ showMoreFilters ? '收起' : '更多' }}
        </button>
        <button class="control-button is-primary" type="button" :disabled="isLoadingRows" @click="applyFilter">
          {{ isLoadingRows ? '加载中' : '应用' }}
        </button>
        <button class="control-button" type="button" @click="resetFilter">重置</button>
      </div>

      <div v-if="showMoreFilters" class="advanced-filter">
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
        <label class="select-field">
          <span>开始日期</span>
          <input v-model="startTime" type="date" @change="activeTimePreset = 'all'" />
        </label>
        <label class="select-field">
          <span>结束日期</span>
          <input v-model="endTime" type="date" @change="activeTimePreset = 'all'" />
        </label>
        <span class="field-like">当前范围：{{ selectedAreaName }} / {{ selectedPipe?.name || '全部管道' }} / {{ selectedSegmentSummary }}</span>
      </div>

      <div class="data-table-wrap">
        <div v-if="isLoadingRows" class="table-loading">实时明细加载中...</div>
        <table class="data-table">
          <thead>
            <tr>
              <th>传感器ID</th>
              <th>传感器名称</th>
              <th>管道名称</th>
              <th>管段</th>
              <th>压力</th>
              <th>流量</th>
              <th>温度</th>
              <th>振动</th>
              <th>状态</th>
              <th>监测时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in page.records" :key="row.id">
              <td>{{ row.sensor_id }}</td>
              <td>{{ row.sensor_name }}</td>
              <td>{{ row.pipeline_name }}</td>
              <td>{{ row.pipe_segment_name || '-' }}</td>
              <td>{{ row.pressure }}</td>
              <td>{{ row.flow }}</td>
              <td>{{ row.temperature }}</td>
              <td>{{ row.vibration }}</td>
              <td><span class="state-pill" :class="stateClass(row.data_status)">{{ row.data_status }}</span></td>
              <td>{{ row.monitor_time }}</td>
              <td><button class="text-button" @click="selected = row">详情</button></td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="table-footer">
        <p class="muted-text">
          <template v-if="isLoadingRows">正在拉取实时明细，接口响应较慢时会先展示指标卡。</template>
          <template v-else>显示 {{ page.records.length }} / {{ page.total }}</template>
        </p>
        <div v-if="tableMode === 'paged'" class="pager">
          <button class="control-button" type="button" :disabled="page.page <= 1 || isLoadingRows" @click="goPage(page.page - 1)">上一页</button>
          <span class="field-like">第 {{ page.page }} 页</span>
          <button class="control-button" type="button" :disabled="!hasMore || isLoadingRows" @click="goPage(page.page + 1)">下一页</button>
        </div>
        <div v-else class="muted-text">{{ isLoadingMore ? '继续加载中...' : hasMore ? '向下滚动加载更多' : '已加载全部' }}</div>
      </div>
    </section>

    <aside v-if="selected" class="detail-drawer">
      <div class="panel-header">
        <h3 class="section-title-text">监测详情</h3>
        <button class="text-button" @click="selected = null">关闭</button>
      </div>
      <div class="kv-list">
        <div class="kv-item"><span>ID</span><strong>{{ selected.id }}</strong></div>
        <div class="kv-item"><span>传感器</span><strong>{{ selected.sensor_name }}</strong></div>
        <div class="kv-item"><span>管道</span><strong>{{ selected.pipeline_name }}</strong></div>
        <div class="kv-item"><span>管段</span><strong>{{ selected.pipe_segment_name || '-' }}</strong></div>
        <div class="kv-item"><span>管段ID</span><strong>{{ selected.pipe_segment_id || '-' }}</strong></div>
        <div class="kv-item"><span>状态</span><strong>{{ selected.data_status }}</strong></div>
      </div>
    </aside>

    <button v-if="showBackTop" class="back-top" type="button" aria-label="回到顶部" @click="scrollToTop">↑</button>
  </ModuleShell>
</template>

<style scoped>
.risk-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-3);
  margin-block: var(--space-3);
}

.monitoring-metrics {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.metric-sub {
  color: var(--color-text-muted);
  font-size: var(--text-micro);
  line-height: 1.3;
}

.risk-card h3 {
  margin: var(--space-2) 0;
}

.risk-card strong {
  font-family: var(--font-mono);
}

.risk-card.is-danger {
  border-color: rgba(231, 104, 45, 0.32);
}

.risk-card.is-warn {
  border-color: rgba(221, 176, 84, 0.28);
}

.monitoring-table-panel {
  display: grid;
  gap: var(--space-3);
}

.table-toolbar,
.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.compact-filter,
.advanced-filter {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr)) minmax(220px, 1.2fr) auto auto auto;
  gap: var(--space-2);
  align-items: end;
}

.advanced-filter {
  grid-template-columns: minmax(280px, 1.4fr) repeat(2, minmax(150px, 0.6fr)) minmax(220px, 1fr);
  align-items: stretch;
  padding: var(--space-3);
  border: 1px solid var(--color-line);
  background: rgba(255, 255, 255, 0.014);
}

.select-field,
.segment-picker {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.select-field span {
  color: var(--color-text-muted);
  font-size: var(--text-meta);
}

.select-field select,
.select-field input {
  width: 100%;
  min-height: 38px;
  border: 1px solid var(--color-line);
  color: var(--color-text);
  background: #091018;
  padding: 0 10px;
  color-scheme: dark;
}

.select-field select:disabled {
  opacity: 0.58;
}

.segmented-control,
.table-mode-switch,
.pager {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.segmented-control button,
.table-mode-switch button,
.segment-list button {
  min-height: 34px;
  border: 1px solid var(--color-line);
  color: var(--color-text-muted);
  background: rgba(255, 255, 255, 0.02);
}

.segmented-control button {
  min-width: 48px;
}

.table-mode-switch button {
  min-width: 62px;
}

.segmented-control button:hover,
.segmented-control button.is-active,
.table-mode-switch button:hover,
.table-mode-switch button.is-active,
.segment-list button:hover,
.segment-list button.is-active {
  color: var(--color-text);
  border-color: var(--color-line-strong);
  background: linear-gradient(180deg, rgba(110, 202, 212, 0.08), rgba(255, 255, 255, 0.01));
}

.segment-picker__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  color: var(--color-accent-cyan);
  font-size: var(--text-micro);
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
}

.segment-picker__head small {
  max-width: 180px;
  color: var(--color-text-muted);
  text-align: right;
  text-transform: none;
  letter-spacing: 0;
}

.segment-list {
  display: grid;
  max-height: 154px;
  overflow: auto;
  gap: 6px;
}

.segment-list button {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  min-height: 38px;
  padding: 6px 8px;
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

.table-loading {
  padding: var(--space-4);
  border: 1px dashed var(--color-line);
  color: var(--color-text-muted);
  font-size: var(--text-meta);
  margin-bottom: var(--space-3);
}

.back-top {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 20;
  width: 42px;
  height: 42px;
  border: 1px solid var(--color-line-strong);
  color: var(--color-text);
  background: rgba(8, 15, 22, 0.92);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.28);
}

@media (max-width: 1179px) {
  .risk-strip,
  .compact-filter,
  .advanced-filter {
    grid-template-columns: 1fr;
  }

  .table-toolbar,
  .table-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
