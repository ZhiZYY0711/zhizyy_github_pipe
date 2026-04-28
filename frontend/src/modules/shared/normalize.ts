export function toNumber(value: unknown, fallback = 0) {
  if (typeof value === 'number') return value
  if (typeof value === 'string' && value.trim() !== '') {
    const parsed = Number(value)
    return Number.isNaN(parsed) ? fallback : parsed
  }
  return fallback
}

type PageMetaOptions = {
  defaultPage: number
  defaultPageSize: number
  pageSizeKeys?: string[]
}

export function readPageMeta(payload: Record<string, unknown>, options: PageMetaOptions) {
  const pageSizeKeys = options.pageSizeKeys ?? ['pageSize', 'page_size']
  const pageSizeKey = pageSizeKeys.find((key) => key in payload)
  const pageSizeValue = pageSizeKey ? payload[pageSizeKey] : undefined

  return {
    total: toNumber(payload.total),
    page: toNumber(payload.page ?? payload.pageNum, options.defaultPage) || options.defaultPage,
    pageSize: toNumber(pageSizeValue, options.defaultPageSize) || options.defaultPageSize,
  }
}
