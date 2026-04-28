<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { LineChart, PieChart } from 'echarts/charts'
import { DataZoomComponent, GraphicComponent, GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { init, use, type ECharts, type EChartsCoreOption } from 'echarts/core'
import { LegacyGridContainLabel } from 'echarts/features'
import { CanvasRenderer } from 'echarts/renderers'
import ModuleShell from '../../../components/business/ModuleShell.vue'
import type {
  AreaOption,
  KpiRankBoard,
  PagedTasks,
  TaskAreaContrast,
  TaskChartQuery,
  TaskIndicator,
  TaskItem,
  TaskQuery,
  TaskStatusSlice,
  TaskTrendPoint,
} from '../types'
import { loadTaskCities, loadTaskDashboard, loadTaskDistricts, loadTaskProvinces } from '../service'

use([
  PieChart,
  LineChart,
  GridComponent,
  DataZoomComponent,
  GraphicComponent,
  LegendComponent,
  TooltipComponent,
  LegacyGridContainLabel,
  CanvasRenderer,
])

type TimePreset = 'week' | 'month' | 'quarter' | 'all'

const indicators = ref<TaskIndicator>({})
const page = ref<PagedTasks>({ records: [], total: 0, page: 1, pageSize: 20 })
const status = ref<TaskStatusSlice[]>([])
const trend = ref<TaskTrendPoint[]>([])
const contrast = ref<TaskAreaContrast[]>([])
const rankBoards = ref<KpiRankBoard[]>([])
const selected = ref<TaskItem | null>(null)
const isFallback = ref(false)
const isLoading = ref(false)

const provinces = ref<AreaOption[]>([])
const cities = ref<AreaOption[]>([])
const districts = ref<AreaOption[]>([])
const selectedProvince = ref('')
const selectedCity = ref('')
const selectedDistrict = ref('')
const activeTimePreset = ref<TimePreset>('all')
const startTime = ref('')
const endTime = ref('')
const statusChartEl = ref<HTMLDivElement | null>(null)
const trendChartEl = ref<HTMLDivElement | null>(null)
let statusChart: ECharts | null = null
let trendChart: ECharts | null = null
let statusResizeObserver: ResizeObserver | null = null
let trendResizeObserver: ResizeObserver | null = null

const timePresets: Array<{ label: string; value: TimePreset }> = [
  { label: '近一周', value: 'week' },
  { label: '近一月', value: 'month' },
  { label: '近三月', value: 'quarter' },
  { label: '全部', value: 'all' },
]

const selectedAreaId = computed(() => selectedDistrict.value || selectedCity.value || selectedProvince.value || '')
const selectedAreaName = computed(() => {
  const district = districts.value.find((item) => item.code === selectedDistrict.value)
  const city = cities.value.find((item) => item.code === selectedCity.value)
  const province = provinces.value.find((item) => item.code === selectedProvince.value)
  return district?.name || city?.name || province?.name || '全国'
})

const indicatorCards = computed(() => [
  { label: '任务总数', value: indicators.value.total || page.value.total, tone: '' },
  { label: '待处理', value: indicators.value.pending || 0, tone: 'is-warn' },
  { label: '处理中', value: indicators.value.in_progress || 0, tone: 'is-signal' },
  { label: '已完成', value: indicators.value.completed || 0, tone: 'is-good' },
  { label: '紧急任务', value: indicators.value.urgent || 0, tone: 'is-danger' },
])

const statusColors = ['#e6b85c', '#63c9d5', '#75d492', '#8a93a3', '#e7682d']

const trendStats = computed(() => {
  const counts = trend.value.map((item) => item.count)
  return {
    min: counts.length ? Math.min(...counts) : 0,
    max: counts.length ? Math.max(...counts) : 0,
  }
})

const maxContrast = computed(() => Math.max(...contrast.value.map((item) => item.count), 1))

function toTimestamp(value: string, endOfDay = false) {
  if (!value) return undefined
  const suffix = endOfDay ? 'T23:59:59' : 'T00:00:00'
  const timestamp = new Date(`${value}${suffix}`).getTime()
  return Number.isNaN(timestamp) ? undefined : timestamp
}

function toDateTime(value: string, endOfDay = false) {
  if (!value) return undefined
  return `${value} ${endOfDay ? '23:59:59' : '00:00:00'}`
}

function resolveTimestampRange() {
  if (startTime.value || endTime.value) {
    return {
      start_time: toTimestamp(startTime.value),
      end_time: toTimestamp(endTime.value, true),
    }
  }

  if (activeTimePreset.value === 'all') return {}

  const days = activeTimePreset.value === 'week' ? 7 : activeTimePreset.value === 'quarter' ? 90 : 30
  return {
    start_time: Date.now() - days * 24 * 60 * 60 * 1000,
    end_time: Date.now(),
  }
}

function buildTaskQuery(): TaskQuery {
  return {
    page: 1,
    page_size: 10,
    area_id: selectedAreaId.value ? Number(selectedAreaId.value) : undefined,
    start_time: toDateTime(startTime.value),
    end_time: toDateTime(endTime.value, true),
  }
}

function buildChartQuery(): TaskChartQuery {
  return {
    area_id: selectedAreaId.value ? Number(selectedAreaId.value) : 0,
    ...resolveTimestampRange(),
  }
}

async function refreshDashboard() {
  isLoading.value = true
  const model = await loadTaskDashboard(buildTaskQuery(), buildChartQuery())
  indicators.value = model.indicators
  page.value = model.page
  status.value = model.status
  trend.value = model.trend
  contrast.value = model.contrast
  rankBoards.value = model.rankBoards
  isFallback.value = model.isFallback
  selected.value = null
  isLoading.value = false
  await nextTick()
  renderStatusChart()
  renderTrendChart()
}

async function selectProvince(code: string) {
  selectedProvince.value = code
  selectedCity.value = ''
  selectedDistrict.value = ''
  cities.value = selectedProvince.value ? await loadTaskCities(selectedProvince.value).catch(() => []) : []
  districts.value = []
}

async function selectCity(code: string) {
  selectedCity.value = code
  selectedDistrict.value = ''
  districts.value = code ? await loadTaskDistricts(code).catch(() => []) : []
}

function selectDistrict(code: string) {
  selectedDistrict.value = code
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
  cities.value = []
  districts.value = []
  activeTimePreset.value = 'all'
  startTime.value = ''
  endTime.value = ''
  await refreshDashboard()
}

function priorityClass(priority: string) {
  if (priority.includes('紧急')) return 'is-danger'
  if (priority.includes('高') || priority.includes('中')) return 'is-warn'
  return 'is-good'
}

function statusClass(value: string) {
  if (value.includes('待')) return 'is-warn'
  if (value.includes('进行') || value.includes('处理')) return 'is-signal'
  if (value.includes('完成')) return 'is-good'
  return ''
}

function formatKpiValue(board: KpiRankBoard, value: number) {
  return board.type === 0 ? `${value}${board.unit}` : `${value.toFixed(2)}${board.unit}`
}

function buildStatusChartOption(): EChartsCoreOption {
  const total = status.value.reduce((sum, item) => sum + item.count, 0)

  return {
    color: statusColors,
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(5, 10, 16, 0.94)',
      borderColor: 'rgba(110, 202, 212, 0.32)',
      textStyle: { color: '#dfe7ed' },
      formatter: '{b}<br />{c} 项 / {d}%',
    },
    legend: {
      orient: 'vertical',
      right: 6,
      top: 'middle',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#9cafbd', fontSize: 12 },
    },
    series: [
      {
        name: '任务状态',
        type: 'pie',
        radius: ['46%', '72%'],
        center: ['34%', '50%'],
        minAngle: 6,
        avoidLabelOverlap: true,
        label: {
          color: '#dfe7ed',
          formatter: (params: { value?: number }) => String(params.value ?? 0),
        },
        labelLine: {
          lineStyle: { color: 'rgba(143, 166, 182, 0.42)' },
          length: 8,
          length2: 8,
        },
        itemStyle: {
          borderColor: '#101b25',
          borderWidth: 2,
        },
        emphasis: {
          scaleSize: 6,
          itemStyle: {
            shadowBlur: 18,
            shadowColor: 'rgba(110, 202, 212, 0.24)',
          },
        },
        data: status.value.map((item) => ({
          name: item.status,
          value: item.count,
        })),
      },
    ],
    graphic: total
      ? [
          {
            type: 'text',
            left: '28%',
            top: '43%',
            style: {
              text: `${total}`,
              fill: '#dfe7ed',
              fontSize: 24,
              fontWeight: 700,
              textAlign: 'center',
            },
          },
          {
            type: 'text',
            left: '27%',
            top: '57%',
            style: {
              text: '总量',
              fill: '#8fa6b6',
              fontSize: 12,
              textAlign: 'center',
            },
          },
        ]
      : [],
  }
}

function renderStatusChart() {
  if (!statusChartEl.value || !statusChart) {
    return
  }

  statusChart.setOption(buildStatusChartOption(), true)
}

function buildTrendChartOption(): EChartsCoreOption {
  const counts = trend.value.map((item) => item.count)
  const min = counts.length ? Math.min(...counts) : 0
  const max = counts.length ? Math.max(...counts) : 0
  const padding = Math.max(Math.ceil((max - min) * 0.18), 1)
  const shouldZoom = trend.value.length > 10

  return {
    color: ['#63c9d5'],
    grid: { top: 26, right: 28, bottom: shouldZoom ? 48 : 30, left: 42, containLabel: true },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(5, 10, 16, 0.94)',
      borderColor: 'rgba(110, 202, 212, 0.32)',
      textStyle: { color: '#dfe7ed' },
      axisPointer: {
        type: 'line',
        lineStyle: { color: 'rgba(110, 202, 212, 0.38)', type: 'dashed' },
      },
      formatter: (params: unknown) => {
        const row = (Array.isArray(params) ? params[0] : params) as { axisValueLabel?: string; value?: number }
        return `${row.axisValueLabel ?? '-'}<br />任务数：${row.value ?? 0}`
      },
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.value.map((item) => item.date ?? '-'),
      axisLine: { lineStyle: { color: 'rgba(143, 166, 182, 0.22)' } },
      axisTick: { show: false },
      axisLabel: {
        color: '#8fa6b6',
        interval: 0,
        hideOverlap: true,
        margin: 12,
        overflow: 'truncate',
        width: 58,
      },
    },
    yAxis: {
      type: 'value',
      min: Math.max(0, min - padding),
      max: max + padding,
      splitNumber: 4,
      splitLine: { lineStyle: { color: 'rgba(143, 166, 182, 0.12)' } },
      axisLabel: { color: '#8fa6b6' },
    },
    dataZoom: shouldZoom
      ? [
          {
            type: 'slider',
            height: 14,
            bottom: 12,
            start: Math.max(0, 100 - (10 / trend.value.length) * 100),
            end: 100,
            borderColor: 'rgba(110, 202, 212, 0.22)',
            fillerColor: 'rgba(110, 202, 212, 0.22)',
            backgroundColor: 'rgba(255, 255, 255, 0.035)',
            handleSize: 12,
            handleStyle: { color: '#63c9d5', borderColor: '#63c9d5' },
            moveHandleStyle: { color: 'rgba(110, 202, 212, 0.5)' },
            textStyle: { color: '#8fa6b6' },
            brushSelect: false,
          },
          { type: 'inside', start: Math.max(0, 100 - (10 / trend.value.length) * 100), end: 100 },
        ]
      : [],
    series: [
      {
        name: '任务数量',
        type: 'line',
        smooth: 0.28,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 3, shadowBlur: 10, shadowColor: 'rgba(99, 201, 213, 0.32)' },
        itemStyle: { borderWidth: 2, borderColor: '#0d1822' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(99, 201, 213, 0.22)' },
              { offset: 1, color: 'rgba(99, 201, 213, 0.02)' },
            ],
          },
        },
        data: counts,
      },
    ],
  }
}

function renderTrendChart() {
  if (!trendChartEl.value || !trendChart) {
    return
  }

  trendChart.setOption(buildTrendChartOption(), true)
}

onMounted(async () => {
  provinces.value = await loadTaskProvinces().catch(() => [])
  await refreshDashboard()
  await nextTick()

  if (statusChartEl.value) {
    statusChart = init(statusChartEl.value, undefined, { renderer: 'canvas' })
    renderStatusChart()
    statusResizeObserver = new ResizeObserver(() => statusChart?.resize())
    statusResizeObserver.observe(statusChartEl.value)
  }

  if (trendChartEl.value) {
    trendChart = init(trendChartEl.value, undefined, { renderer: 'canvas' })
    renderTrendChart()
    trendResizeObserver = new ResizeObserver(() => trendChart?.resize())
    trendResizeObserver.observe(trendChartEl.value)
  }
})

watch(status, renderStatusChart, { deep: true })
watch(trend, renderTrendChart, { deep: true })

onBeforeUnmount(() => {
  statusResizeObserver?.disconnect()
  trendResizeObserver?.disconnect()
  statusChart?.dispose()
  trendChart?.dispose()
})
</script>

<template>
  <ModuleShell active-path="/tasks" eyebrow="Task Analysis" title="任务态势分析工作台" ops-label="数据源" :ops-value="isFallback ? 'Fallback' : 'API'">
    <section class="task-filter-panel">
      <div class="filter-head">
        <div>
          <span class="eyebrow">Filter</span>
          <h2 class="section-title-text">筛选控制台</h2>
        </div>
        <div class="button-strip">
          <span class="chip">{{ selectedAreaName }}</span>
          <a class="control-button" href="/tasks/all">所有任务</a>
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
          <select :value="selectedCity" :disabled="!selectedProvince" @change="selectCity(($event.target as HTMLSelectElement).value)">
            <option value="">全部城市</option>
            <option v-for="city in cities" :key="city.code" :value="city.code">{{ city.name }}</option>
          </select>
        </label>
        <label class="select-field">
          <span>区县</span>
          <select :value="selectedDistrict" :disabled="!selectedCity" @change="selectDistrict(($event.target as HTMLSelectElement).value)">
            <option value="">全部区县</option>
            <option v-for="district in districts" :key="district.code" :value="district.code">{{ district.name }}</option>
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
        <label class="select-field">
          <span>开始日期</span>
          <input v-model="startTime" type="date" @change="activeTimePreset = 'all'" />
        </label>
        <label class="select-field">
          <span>结束日期</span>
          <input v-model="endTime" type="date" @change="activeTimePreset = 'all'" />
        </label>
        <button class="control-button is-primary" type="button" :disabled="isLoading" @click="refreshDashboard">
          {{ isLoading ? '加载中' : '应用' }}
        </button>
        <button class="control-button" type="button" @click="resetFilters">重置</button>
      </div>
    </section>

    <section class="indicator-grid">
      <article v-for="card in indicatorCards" :key="card.label" class="metric-card" :class="card.tone">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
      </article>
    </section>

    <section class="task-workbench">
      <aside class="task-panel task-list-panel">
        <div class="task-panel__header">
          <div>
            <span class="eyebrow">Task Queue</span>
            <h3 class="panel-title-text">重点任务</h3>
          </div>
          <span class="chip">{{ page.records.length }} / {{ page.total }}</span>
        </div>
        <div class="task-list-scroll task-scrollbar">
          <button v-for="item in page.records" :key="item.id" class="task-card" @click="selected = item">
            <div class="task-card__top">
              <strong>{{ item.title }}</strong>
              <span class="state-pill" :class="priorityClass(item.priority)">{{ item.priority }}</span>
            </div>
            <div class="task-card__meta">
              <span>{{ item.area_name }}</span>
              <span>{{ item.pipe_name }}</span>
              <span>{{ item.pipe_segment_name || '未绑定管段' }}</span>
            </div>
            <div class="task-card__foot">
              <span class="state-pill" :class="statusClass(item.status)">{{ item.status }}</span>
              <span>{{ item.assignee }} / {{ item.phone }}</span>
            </div>
            <p>{{ item.feedback_information || '暂无反馈信息' }}</p>
          </button>
        </div>
      </aside>

      <section class="chart-grid">
        <article class="task-panel status-panel">
          <div class="task-panel__header"><h3 class="section-title-text">任务状态分布</h3><span class="chip">task_status</span></div>
          <div ref="statusChartEl" class="status-chart"></div>
        </article>

        <article class="task-panel trend-panel">
          <div class="task-panel__header">
            <h3 class="section-title-text">任务数量趋势</h3>
            <span class="chip">Y {{ trendStats.min }}-{{ trendStats.max }}</span>
          </div>
          <div ref="trendChartEl" class="trend-chart"></div>
        </article>

        <article class="task-panel area-panel">
          <div class="task-panel__header"><h3 class="section-title-text">区域任务对比</h3><span class="chip">下一级区域</span></div>
          <div class="area-bars task-scrollbar">
            <div v-for="item in contrast" :key="item.area_name">
              <span>{{ item.area_name }}</span>
              <i :style="{ width: `${Math.max((item.count / maxContrast) * 100, 4)}%` }"></i>
              <strong>{{ item.count }}</strong>
            </div>
          </div>
        </article>

        <article class="task-panel rank-panel">
          <div class="task-panel__header"><h3 class="section-title-text">KPI 榜单</h3><span class="chip">KPI_list x3</span></div>
          <div class="rank-board-grid task-scrollbar">
            <section v-for="board in rankBoards" :key="board.type" class="rank-board">
              <h4>{{ board.title }}</h4>
              <div v-for="item in board.rows.slice(0, 5)" :key="`${board.type}-${item.rank}-${item.name}`" class="rank-row">
                <strong>{{ item.rank }}</strong>
                <span>{{ item.name }}</span>
                <b>{{ formatKpiValue(board, item.value) }}</b>
              </div>
            </section>
          </div>
        </article>
      </section>
    </section>

    <aside v-if="selected" class="detail-drawer">
      <div class="panel-header"><h3 class="section-title-text">任务详情</h3><button class="text-button" @click="selected = null">关闭</button></div>
      <div class="kv-list">
        <div class="kv-item"><span>管段</span><strong>{{ selected.pipe_segment_name || '-' }}</strong></div>
        <div class="kv-item"><span>管段ID</span><strong>{{ selected.pipe_segment_id || '-' }}</strong></div>
        <div v-for="value, key in selected" :key="key" class="kv-item"><span>{{ key }}</span><strong>{{ value || '-' }}</strong></div>
      </div>
    </aside>
  </ModuleShell>
</template>

<style scoped>
.task-filter-panel {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--color-line);
  background:
    linear-gradient(90deg, rgba(110, 202, 212, 0.055), transparent 38%),
    rgba(255, 255, 255, 0.012);
}

.filter-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.compact-filter {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(154px, 1fr));
  gap: 8px;
  align-items: end;
}

.compact-filter > .control-button {
  width: 100%;
  min-width: 86px;
  min-height: 36px;
  padding-inline: 12px;
  white-space: nowrap;
}

.select-field {
  display: grid;
  gap: 6px;
}

.select-field span {
  color: var(--color-text-muted);
  font-size: var(--text-meta);
}

.select-field select,
.select-field input {
  width: 100%;
  min-height: 36px;
  border: 1px solid var(--color-line);
  color: var(--color-text);
  background: rgba(2, 8, 13, 0.82);
  padding: 0 10px;
  color-scheme: dark;
}

.select-field select:disabled {
  opacity: 0.58;
}

.segmented-control {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  grid-column: span 2;
  min-width: 0;
}

.segmented-control button {
  min-height: 36px;
  border: 1px solid var(--color-line);
  color: var(--color-text-muted);
  background: rgba(255, 255, 255, 0.02);
  white-space: nowrap;
}

.segmented-control button:hover,
.segmented-control button.is-active {
  color: var(--color-text);
  border-color: var(--color-line-strong);
  background: linear-gradient(180deg, rgba(110, 202, 212, 0.08), rgba(255, 255, 255, 0.01));
}

.indicator-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin-top: 10px;
}

.indicator-grid :deep(.metric-card),
.indicator-grid .metric-card {
  min-height: 92px;
  padding: 14px;
}

.metric-card.is-good {
  border-color: rgba(117, 212, 146, 0.28);
}

.task-workbench {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(0, 2.1fr);
  gap: 12px;
  margin-top: 12px;
  height: min(900px, calc(100dvh - 252px));
  min-height: 720px;
  min-width: 0;
}

.task-panel {
  min-width: 0;
  min-height: 0;
  border: 1px solid var(--color-line);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.022), transparent 42%),
    rgba(12, 23, 32, 0.82);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.025);
}

.task-panel__header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px 10px;
}

.task-panel__header .section-title-text,
.task-panel__header .panel-title-text {
  margin: 0;
}

.task-list-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  align-content: start;
  gap: 0;
  overflow: hidden;
  padding-bottom: 12px;
}

.task-list-scroll {
  display: grid;
  align-content: start;
  gap: 8px;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 0 0 8px;
}

.task-card {
  width: calc(100% - 24px);
  min-width: 0;
  min-height: 118px;
  margin: 0 12px;
  padding: 12px;
  border: 1px solid var(--color-line);
  color: var(--color-text);
  background:
    linear-gradient(90deg, rgba(110, 202, 212, 0.045), transparent 42%),
    rgba(255, 255, 255, 0.014);
  text-align: left;
  display: grid;
  gap: 8px;
  overflow: hidden;
}

.task-card:hover {
  border-color: var(--color-line-strong);
  background: rgba(110, 202, 212, 0.045);
}

.task-card__top,
.task-card__foot {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.task-card__foot > span:last-child {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-card__top strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-card__meta {
  display: grid;
  grid-template-columns: minmax(0, 0.8fr) minmax(0, 0.7fr) minmax(0, 1.25fr);
  gap: 6px;
}

.task-card__meta span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-muted);
  font-size: var(--text-meta);
}

.task-card p {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--color-text-muted);
  font-size: var(--text-meta);
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.task-list-panel > .muted-text {
  margin: 0 14px;
  font-size: var(--text-micro);
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  grid-template-rows: minmax(0, 0.92fr) minmax(0, 1.08fr);
  gap: 12px;
  min-width: 0;
  min-height: 0;
}

.status-panel,
.trend-panel,
.area-panel,
.rank-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
}

.status-panel {
  grid-column: span 3;
}

.trend-panel {
  grid-column: span 5;
}

.area-panel {
  grid-column: span 5;
}

.rank-panel {
  grid-column: span 3;
}

.status-chart,
.trend-chart {
  width: 100%;
  height: 100%;
  min-height: 0;
  padding: 0 10px 10px;
}

.area-bars {
  display: grid;
  gap: 8px;
  min-height: 0;
}

.area-bars div {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr) 42px;
  gap: 8px;
  align-items: center;
}

.area-bars span {
  color: var(--color-text-muted);
  font-size: var(--text-meta);
}

.area-bars i {
  height: 8px;
  border: 1px solid rgba(110, 202, 212, 0.22);
  background: linear-gradient(90deg, rgba(110, 202, 212, 0.2), var(--color-accent-cyan));
}

.area-bars {
  align-content: start;
  overflow: auto;
  padding: 2px 18px 16px;
}

.rank-board-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  overflow: auto;
  padding: 2px 12px 14px;
}

.rank-board {
  display: grid;
  gap: 6px;
  padding: 10px;
  border: 1px solid var(--color-line);
  background: rgba(255, 255, 255, 0.014);
}

.rank-board h4 {
  margin: 0 0 var(--space-1);
  color: var(--color-accent-cyan);
  font-size: var(--text-meta);
}

.rank-row {
  display: grid;
  grid-template-columns: 26px minmax(0, 1fr) 64px;
  gap: 8px;
  padding: 7px 8px;
  border: 1px solid var(--color-line);
  font-size: var(--text-meta);
}

.rank-row span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-row b {
  color: var(--color-accent-cyan);
  text-align: right;
}

.task-scrollbar {
  scrollbar-width: thin;
  scrollbar-color: rgba(110, 202, 212, 0.46) rgba(255, 255, 255, 0.035);
}

.task-scrollbar::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.task-scrollbar::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid rgba(110, 202, 212, 0.08);
}

.task-scrollbar::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(110, 202, 212, 0.62), rgba(110, 202, 212, 0.22));
  border: 1px solid rgba(110, 202, 212, 0.28);
}

.task-scrollbar::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(110, 202, 212, 0.78), rgba(110, 202, 212, 0.34));
}

@media (max-width: 1279px) {
  .compact-filter,
  .task-workbench,
  .chart-grid,
  .rank-board-grid {
    grid-template-columns: 1fr;
  }

  .task-workbench {
    height: auto;
  }

  .chart-grid {
    grid-template-rows: none;
  }

  .status-panel,
  .trend-panel,
  .area-panel,
  .rank-panel {
    grid-column: span 1;
    min-height: 320px;
  }

  .indicator-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .filter-head,
  .task-card__top,
  .task-card__foot {
    grid-template-columns: 1fr;
    display: grid;
  }

  .indicator-grid,
  .task-card__meta {
    grid-template-columns: 1fr;
  }

  .segmented-control {
    grid-column: span 1;
  }
}
</style>
