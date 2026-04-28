import {
  fallbackKpiRanks,
  fallbackTaskContrast,
  fallbackTaskIndicator,
  fallbackTaskPage,
  fallbackTaskStatus,
  fallbackTaskTrend,
} from '../../data/businessFallback'
import { unwrapData } from '../shared/apiClient'
import { readPageMeta, toNumber } from '../shared/normalize'
import {
  fetchCities,
  fetchDistricts,
  fetchKpiList,
  fetchProvinces,
  fetchTaskContrast,
  fetchTaskIndicators,
  fetchTaskStatus,
  fetchTaskTrend,
  fetchTasks,
} from './api'
import type { AreaOption, KpiRankBoard, KpiRankItem, TaskChartQuery, TaskQuery, TaskStatusSlice } from './types'

const defaultTaskQuery: TaskQuery = { page: 1, page_size: 20 }
const defaultChartQuery: TaskChartQuery = { area_id: 130100, type: 0 }
const taskStatusText: Record<string, string> = {
  0: '待处理',
  1: '进行中',
  2: '已完成',
  3: '已取消',
}

const priorityText: Record<string, string> = {
  0: '普通',
  1: '紧急',
  2: '高',
  3: '中',
  4: '低',
}

const kpiBoards: Array<{ type: 0 | 1 | 2; title: string; unit: string }> = [
  { type: 0, title: '完成任务数', unit: '项' },
  { type: 1, title: '平均响应时间', unit: 'h' },
  { type: 2, title: '平均完成时间', unit: 'h' },
]

function normalizeTaskIndicators(payload: Record<string, unknown>) {
  return {
    total: toNumber(payload.total),
    pending: toNumber(payload.pending),
    in_progress: toNumber(payload.in_progress),
    completed: toNumber(payload.completed),
    urgent: toNumber(payload.urgent),
  }
}

function normalizeTaskStatus(value: unknown) {
  const raw = String(value ?? '')
  return taskStatusText[raw] ?? (raw || '未知')
}

function normalizePriority(value: unknown) {
  const raw = String(value ?? '')
  return priorityText[raw] ?? (raw || '-')
}

function normalizeTaskPage(payload: Record<string, unknown>) {
  const records = Array.isArray(payload.records) ? payload.records : []

  return {
    records: records.map((item) => {
      const record = item as Record<string, unknown>

      return {
        ...record,
        id: String(record.id ?? ''),
        title: String(record.title ?? '-'),
        type: String(record.type ?? '-'),
        area_name: String(record.area_name ?? '-'),
        pipe_name: String(record.pipe_name ?? '-'),
        pipe_segment_id: String(record.pipe_segment_id ?? ''),
        pipe_segment_name: String(record.pipe_segment_name ?? '-'),
        priority: normalizePriority(record.priority),
        status: normalizeTaskStatus(record.status),
        assignee: String(record.assignee ?? '-'),
        phone: String(record.phone ?? '-'),
        public_time: String(record.public_time ?? '-'),
        response_time: record.response_time == null ? null : String(record.response_time),
        accomplish_time: record.accomplish_time == null ? null : String(record.accomplish_time),
        feedback_information: record.feedback_information == null ? null : String(record.feedback_information),
      }
    }),
    ...readPageMeta(payload, { defaultPage: 1, defaultPageSize: 20 }),
  }
}

function normalizeTaskStatusSlices(payload: unknown): TaskStatusSlice[] {
  const rows = Array.isArray(payload) ? payload : []
  return rows.map((item) => {
    const record = item as Record<string, unknown>
    return {
      status: normalizeTaskStatus(record.status),
      count: toNumber(record.count),
    }
  })
}

function normalizeTaskTrend(payload: unknown) {
  const rows = Array.isArray(payload) ? payload : []
  return rows.map((item) => {
    const record = item as Record<string, unknown>
    const time = toNumber(record.time)
    const timestamp = time > 0 && time < 10_000_000_000 ? time * 1000 : time

    return {
      date: timestamp ? formatDate(timestamp) : '-',
      time,
      count: toNumber(record.count),
    }
  })
}

function normalizeTaskContrast(payload: unknown) {
  const rows = Array.isArray(payload) ? payload : []
  return rows.map((item) => {
    const record = item as Record<string, unknown>

    return {
      area_name: String(record.area_name ?? '-'),
      count: toNumber(record.count),
    }
  })
}

function normalizeKpiRows(payload: unknown): KpiRankItem[] {
  const rows = Array.isArray(payload) ? payload : []
  return rows.map((item, index) => {
    const record = item as Record<string, unknown>
    return {
      rank: index + 1,
      name: String(record.name ?? '-'),
      value: toNumber(record.data),
      repairman_id: record.repairman_id == null ? undefined : String(record.repairman_id),
    }
  })
}

function normalizeKpiBoards(results: Array<PromiseSettledResult<unknown>>): KpiRankBoard[] {
  return kpiBoards.map((board, index) => ({
    ...board,
    rows:
      results[index]?.status === 'fulfilled'
        ? normalizeKpiRows(unwrapData(results[index].value, fallbackKpiRanks))
        : fallbackKpiRanks,
  }))
}

function normalizeAreaOptions(payload: unknown): AreaOption[] {
  const rows = Array.isArray(payload) ? payload : []
  return rows
    .map((item) => {
      const record = item as Record<string, unknown>
      return {
        code: String(record.code ?? ''),
        name: String(record.name ?? ''),
      }
    })
    .filter((item) => item.code && item.name)
}

function formatDate(timestamp: number) {
  const date = new Date(timestamp)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${month}-${day}`
}

function withKpiType(query: TaskChartQuery, type: 0 | 1 | 2) {
  return { ...query, type }
}

export async function loadTaskDashboard(taskQuery: TaskQuery = defaultTaskQuery, chartQuery: TaskChartQuery = defaultChartQuery) {
  const [indicators, page, status, trend, contrast, ...rankResults] = await Promise.allSettled([
    fetchTaskIndicators(chartQuery.area_id || undefined),
    fetchTasks(taskQuery),
    fetchTaskStatus(chartQuery),
    fetchTaskTrend(chartQuery),
    fetchTaskContrast(chartQuery),
    ...kpiBoards.map((board) => fetchKpiList(withKpiType(chartQuery, board.type))),
  ])

  return {
    indicators:
      indicators.status === 'fulfilled'
        ? normalizeTaskIndicators(unwrapData(indicators.value, fallbackTaskIndicator) as Record<string, unknown>)
        : fallbackTaskIndicator,
    page:
      page.status === 'fulfilled'
        ? normalizeTaskPage(unwrapData(page.value, fallbackTaskPage) as Record<string, unknown>)
        : fallbackTaskPage,
    status: status.status === 'fulfilled' ? normalizeTaskStatusSlices(unwrapData(status.value, fallbackTaskStatus)) : fallbackTaskStatus,
    trend: trend.status === 'fulfilled' ? normalizeTaskTrend(unwrapData(trend.value, fallbackTaskTrend)) : fallbackTaskTrend,
    contrast: contrast.status === 'fulfilled' ? normalizeTaskContrast(unwrapData(contrast.value, fallbackTaskContrast)) : fallbackTaskContrast,
    rankBoards: normalizeKpiBoards(rankResults),
    isFallback:
      indicators.status === 'rejected' ||
      page.status === 'rejected' ||
      status.status === 'rejected' ||
      trend.status === 'rejected' ||
      contrast.status === 'rejected' ||
      rankResults.some((result) => result.status === 'rejected'),
  }
}

export async function loadTaskTable(query: TaskQuery = defaultTaskQuery) {
  try {
    const page = await fetchTasks(query)

    return { page: normalizeTaskPage(unwrapData(page, fallbackTaskPage) as Record<string, unknown>), isFallback: false }
  } catch {
    return { page: fallbackTaskPage, isFallback: true }
  }
}

export async function loadTaskProvinces() {
  return normalizeAreaOptions(unwrapData(await fetchProvinces(), []))
}

export async function loadTaskCities(provinceCode: string) {
  return normalizeAreaOptions(unwrapData(await fetchCities(provinceCode), []))
}

export async function loadTaskDistricts(cityCode: string) {
  return normalizeAreaOptions(unwrapData(await fetchDistricts(cityCode), []))
}
