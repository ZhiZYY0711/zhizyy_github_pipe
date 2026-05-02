import { afterEach, describe, expect, it, vi } from 'vitest'
import { loadGeoIndex } from './service'

const originalFetch = globalThis.fetch

afterEach(() => {
  globalThis.fetch = originalFetch
  vi.restoreAllMocks()
})

describe('loadGeoIndex', () => {
  it('falls back instead of throwing when the dev server returns html for the geo index', async () => {
    globalThis.fetch = vi.fn(async () => new Response('<!doctype html><html></html>', {
      headers: { 'Content-Type': 'text/html' },
      status: 200,
    }))

    const index = await loadGeoIndex()

    expect(index.china).toEqual({ code: '100000', name: '全国', path: '' })
    expect(index.province).toEqual({})
  })
})
