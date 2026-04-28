<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { LineChart } from 'echarts/charts'
import { DataZoomComponent, GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { init, use, type ECharts, type EChartsCoreOption } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import type { DimensionData, MonitoringMetricKey } from '../types'

use([LineChart, DataZoomComponent, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{
  eyebrow: string
  title: string
  points: DimensionData[]
  visibleMetrics?: MonitoringMetricKey[]
}>()

const chartEl = ref<HTMLDivElement | null>(null)
let chart: ECharts | null = null
let resizeObserver: ResizeObserver | null = null

const metrics = [
  { key: 'pressure', name: '压力', unit: 'MPa', color: '#71ccd6' },
  { key: 'flow', name: '流量', unit: 'm3/h', color: '#e7682d' },
  { key: 'temperature', name: '温度', unit: 'C', color: '#ddb054' },
  { key: 'vibration', name: '震动', unit: 'mm/s', color: '#92d47d' },
] as const

const chartLabels = computed(() =>
  props.points.map((item, index) => {
    if (item.time) {
      return new Intl.DateTimeFormat('zh-CN', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
      }).format(new Date(item.time))
    }

    return item.area_name || item.pipe_name || `T+${index + 1}`
  }),
)

const visibleMetricSet = computed(() => new Set(props.visibleMetrics ?? metrics.map((metric) => metric.key)))
const activeMetrics = computed(() => metrics.filter((metric) => visibleMetricSet.value.has(metric.key)))

function buildOption(): EChartsCoreOption {
  return {
    color: activeMetrics.value.map((metric) => metric.color),
    grid: { top: 58, right: 28, bottom: props.points.length > 12 ? 72 : 42, left: 46, containLabel: true },
    legend: {
      top: 10,
      right: 16,
      textStyle: { color: '#8fa6b6' },
      itemWidth: 16,
      itemHeight: 8,
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(5, 10, 16, 0.94)',
      borderColor: 'rgba(143, 166, 182, 0.28)',
      textStyle: { color: '#dfe7ed' },
      formatter: (params: unknown) => {
        const items = Array.isArray(params) ? params : [params]
        const first = items[0] as { axisValueLabel?: string; dataIndex?: number } | undefined
        const rows = items.map((item) => {
          const row = item as { marker?: string; seriesName?: string; dataIndex?: number }
          const metric = activeMetrics.value.find((entry) => entry.name === row.seriesName)
          const rawValue = metric ? props.points[row.dataIndex ?? 0]?.[metric.key] ?? 0 : 0

          return `${row.marker ?? ''}${row.seriesName ?? ''}: ${rawValue} ${metric?.unit ?? ''}`
        })

        return [`<strong>${first?.axisValueLabel ?? ''}</strong>`, ...rows].join('<br />')
      },
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: chartLabels.value,
      axisLine: { lineStyle: { color: 'rgba(143, 166, 182, 0.24)' } },
      axisTick: { show: false },
      axisLabel: { color: '#8fa6b6' },
    },
    yAxis: {
      type: 'value',
      name: '波动指数',
      min: (value: { min: number; max: number }) => axisMin(value.min, value.max),
      max: (value: { min: number; max: number }) => axisMax(value.min, value.max),
      splitLine: { lineStyle: { color: 'rgba(143, 166, 182, 0.13)' } },
      axisLabel: {
        color: '#8fa6b6',
        formatter: (value: number) => Math.round(value).toString(),
      },
      nameTextStyle: { color: '#8fa6b6' },
    },
    dataZoom: props.points.length > 12
      ? [
          {
            type: 'inside',
            xAxisIndex: 0,
            minSpan: 10,
          },
          {
            type: 'slider',
            xAxisIndex: 0,
            height: 18,
            bottom: 18,
            borderColor: 'rgba(143, 166, 182, 0.18)',
            backgroundColor: 'rgba(255,255,255,0.02)',
            fillerColor: 'rgba(110, 202, 212, 0.16)',
            handleStyle: { color: '#6ecad4' },
            textStyle: { color: '#8fa6b6' },
          },
        ]
      : [],
    series: activeMetrics.value.map((metric) => ({
      name: metric.name,
      type: 'line',
      smooth: 0.18,
      showSymbol: props.points.length <= 18,
      symbolSize: 5,
      lineStyle: { width: 2.6, shadowBlur: 8, shadowColor: `${metric.color}55` },
      emphasis: { focus: 'series' },
      data: buildDisplaySeries(metric.key),
    })),
  }
}

function buildDisplaySeries(key: MonitoringMetricKey) {
  const values = props.points.map((item) => item[key] ?? 0)
  const min = Math.min(...values)
  const max = Math.max(...values)

  if (!values.length || min === max) {
    return values
  }

  return values.map((value) => Number((22 + ((value - min) / (max - min)) * 76).toFixed(2)))
}

function axisMin(min: number, max: number) {
  if (!Number.isFinite(min) || !Number.isFinite(max)) {
    return undefined
  }

  if (min === max) {
    return Math.max(0, Math.floor(min - 1))
  }

  return Math.max(0, Math.floor(min - (max - min) * 0.24))
}

function axisMax(min: number, max: number) {
  if (!Number.isFinite(min) || !Number.isFinite(max)) {
    return undefined
  }

  if (min === max) {
    return Math.ceil(max + 1)
  }

  return Math.ceil(max + (max - min) * 0.24)
}

function renderChart() {
  if (!chartEl.value || !chart) {
    return
  }

  chart.setOption(buildOption(), true)
}

onMounted(async () => {
  await nextTick()

  if (!chartEl.value) {
    return
  }

  chart = init(chartEl.value, undefined, { renderer: 'canvas' })
  renderChart()
  resizeObserver = new ResizeObserver(() => chart?.resize())
  resizeObserver.observe(chartEl.value)
})

watch(() => [props.points, props.visibleMetrics], renderChart, { deep: true })

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
})
</script>

<template>
  <section class="table-panel chart-panel">
    <div class="panel-header">
      <div>
        <span class="eyebrow">{{ eyebrow }}</span>
        <h3 class="panel-title-text">{{ title }}</h3>
      </div>
      <div class="chart-legend-note">自适应波动 / 横轴缩放</div>
    </div>
    <div ref="chartEl" class="monitoring-line-chart"></div>
  </section>
</template>

<style scoped>
.chart-panel {
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
}

.chart-legend-note {
  color: var(--color-text-muted);
  font-size: var(--text-meta);
}

.monitoring-line-chart {
  min-height: 0;
  height: 100%;
  border: 1px solid var(--color-line);
  background:
    linear-gradient(rgba(110, 202, 212, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(110, 202, 212, 0.04) 1px, transparent 1px),
    #09121a;
  background-size: 28px 28px;
}
</style>
