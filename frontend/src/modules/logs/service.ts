import { fallbackLogIndicator, fallbackLogPage } from '../../data/businessFallback'
import { unwrapData } from '../shared/apiClient'
import { readPageMeta, toNumber } from '../shared/normalize'
import { fetchLogIndicators, fetchLogs } from './api'
import type { LogIndicator, LogQuery, PagedLogs } from './types'

function normalizeIndicators(payload: LogIndicator & Record<string, unknown>): LogIndicator {
  return {
    total: toNumber(payload.total),
    all: toNumber(payload.all),
    today: toNumber(payload.today),
    success: toNumber(payload.success),
    failed: toNumber(payload.failed),
    error: toNumber(payload.error),
    warning: toNumber(payload.warning),
    debugging: toNumber(payload.debugging),
    avg_duration: toNumber(payload.avg_duration),
  }
}

function formatTimestamp(value: unknown) {
  const numeric = toNumber(value)
  if (!numeric) return '-'

  const date = new Date(numeric)
  if (Number.isNaN(date.getTime())) return String(value ?? '-')

  return date.toLocaleString('zh-CN', { hour12: false })
}

function userTypeText(value: unknown) {
  return String(value ?? '') === '1' ? '检修员' : '管理员'
}

function logStatusText(value: unknown) {
  const normalized = String(value ?? '')
  if (normalized === '0') return '成功'
  if (normalized === '1') return '失败'
  if (normalized === '2') return '告警'
  if (normalized === '3') return '调试'
  return normalized || '-'
}

function normalizeRecord(payload: Record<string, unknown>) {
  return {
    id: String(payload.id ?? ''),
    user_id: String(payload.user_id ?? ''),
    user_name: String(payload.username ?? '-'),
    user_type: String(payload.user_type_text ?? userTypeText(payload.type)),
    operation: String(payload.operate ?? '-'),
    ip: String(payload.ip_address ?? '-'),
    status: String(payload.status_text ?? logStatusText(payload.status)),
    duration: toNumber(payload.period),
    create_time: formatTimestamp(payload.operate_time),
    details: String(payload.details ?? '-'),
  }
}

function normalizePage(payload: PagedLogs & Record<string, unknown>): PagedLogs {
  return {
    records: Array.isArray(payload.records)
      ? payload.records.map((record) => normalizeRecord(record as Record<string, unknown>))
      : [],
    ...readPageMeta(payload, { defaultPage: 1, defaultPageSize: 50 }),
  }
}

export async function loadLogs(query: LogQuery = { page: 1, page_size: 50 }) {
  const [indicators, page] = await Promise.allSettled([fetchLogIndicators(), fetchLogs(query)])

  return {
    indicators:
      indicators.status === 'fulfilled'
        ? normalizeIndicators(unwrapData(indicators.value, fallbackLogIndicator) as LogIndicator & Record<string, unknown>)
        : fallbackLogIndicator,
    page:
      page.status === 'fulfilled'
        ? normalizePage(unwrapData(page.value, fallbackLogPage) as PagedLogs & Record<string, unknown>)
        : fallbackLogPage,
    isFallback: indicators.status === 'rejected' || page.status === 'rejected',
  }
}
