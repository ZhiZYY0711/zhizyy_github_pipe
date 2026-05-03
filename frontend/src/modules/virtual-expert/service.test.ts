import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  buildComposerMessageContent,
  exportMessageFromEvent,
  getAgentEventRenderDelay,
  getFinalAnswerPendingNotice,
  getAgentStartupNotice,
  groupEventsIntoReactRounds,
  isDisplayableAgentEvent,
  shouldQueueAgentEventForPacing,
  isNearConversationBottom,
  isSubmitComposerKey,
  memoryNoticeFromEvent,
  memoryTypeLabel,
  normalizeAgentMemoryItem,
  normalizeEvidenceDisplayItems,
  readableAgentError,
  renderMarkdownToHtml,
  summarizeRecalledMemoryItems,
} from './service'
import type { AgentComposerAttachment, AgentEvent } from './types'

afterEach(() => {
  vi.unstubAllGlobals()
})

function event(seq: number, type: string, payload: Record<string, unknown> = {}): AgentEvent {
  return {
    id: `evt_${seq}`,
    sessionId: 'ana_001',
    runId: 'run_001',
    seq,
    type,
    level: type.includes('failed') ? 'error' : 'info',
    payload,
  }
}

describe('groupEventsIntoReactRounds', () => {
  it('groups events by step and collapses previous running rounds when a new round starts', () => {
    const rounds = groupEventsIntoReactRounds(
      [
        event(1, 'run_started'),
        event(2, 'llm_step_started', { step: 1 }),
        event(3, 'tool_completed', { tool_name: 'equipment_status', summary: '设备异常', facts: [] }),
        event(4, 'llm_step_started', { step: 2 }),
        event(5, 'knowledge_search_completed', { documents: [{ title: 'SOP', summary: '先复核' }] }),
      ],
      { status: 'running' },
    )

    expect(rounds).toHaveLength(2)
    expect(rounds[0].collapsed).toBe(true)
    expect(rounds[1].collapsed).toBe(false)
    expect(rounds[0].summary.toolCount).toBe(1)
    expect(rounds[1].summary.knowledgeCount).toBe(1)
  })

  it('keeps failed rounds and the final round expanded after completion', () => {
    const rounds = groupEventsIntoReactRounds(
      [
        event(1, 'llm_step_started', { step: 1 }),
        event(2, 'tool_failed', { tool_name: 'monitoring_trend', error: 'timeout' }),
        event(3, 'llm_step_started', { step: 2 }),
        event(4, 'recommendation_generated', { judgment: '需要人工确认' }),
      ],
      { status: 'completed' },
    )

    expect(rounds[0].collapsed).toBe(false)
    expect(rounds[1].collapsed).toBe(false)
    expect(rounds[0].summary.errorCount).toBe(1)
  })

  it('respects manual round expansion overrides', () => {
    const rounds = groupEventsIntoReactRounds(
      [
        event(1, 'llm_step_started', { step: 1 }),
        event(2, 'tool_completed', { tool_name: 'equipment_status' }),
        event(3, 'llm_step_started', { step: 2 }),
      ],
      { status: 'running', roundOverrides: { round_1: true } },
    )

    expect(rounds[0].collapsed).toBe(false)
    expect(rounds[0].userPinned).toBe(true)
  })
})

describe('agent event display helpers', () => {
  it('hides step boundary events from the visible process stream', () => {
    expect(isDisplayableAgentEvent('llm_step_started')).toBe(false)
    expect(isDisplayableAgentEvent('llm_step_completed')).toBe(false)
    expect(isDisplayableAgentEvent('recommendation_generated')).toBe(false)
    expect(isDisplayableAgentEvent('run_completed')).toBe(false)
    expect(isDisplayableAgentEvent('tool_completed')).toBe(true)
    expect(isDisplayableAgentEvent('export_created')).toBe(true)
  })

  it('summarizes recalled memory contents instead of only showing counts', () => {
    expect(summarizeRecalledMemoryItems([
      '回答先给结论。',
      { content: '导出报告默认包含证据表。' },
      { content: '' },
    ])).toBe('回答先给结论。；导出报告默认包含证据表。')
  })

  it('explains the startup wait before the event stream emits', () => {
    const notice = getAgentStartupNotice()

    expect(notice.title).toBe('运行中')
    expect(notice.steps).toContain('创建或恢复会话')
    expect(notice.steps).toContain('建立事件流')
    expect(notice.reason).toContain('LLM')
  })

  it('explains the wait before the first final-answer token', () => {
    const notice = getFinalAnswerPendingNotice()

    expect(notice.title).toBe('正在生成最终回答')
    expect(notice.detail).toContain('工具结果')
    expect(notice.detail).toContain('LLM')
  })

  it('paces block-level events but keeps token deltas immediate', () => {
    expect(shouldQueueAgentEventForPacing('tool_completed')).toBe(true)
    expect(shouldQueueAgentEventForPacing('llm_thinking_completed')).toBe(true)
    expect(shouldQueueAgentEventForPacing('final_answer_started')).toBe(true)
    expect(shouldQueueAgentEventForPacing('llm_thinking_delta')).toBe(false)
    expect(shouldQueueAgentEventForPacing('final_answer_delta')).toBe(false)
  })

  it('calculates the remaining render delay between visible blocks', () => {
    expect(getAgentEventRenderDelay(1000, 1100, 180)).toBe(80)
    expect(getAgentEventRenderDelay(1000, 1300, 180)).toBe(0)
  })

  it('normalizes agent-created export download links through the backend proxy', () => {
    vi.stubGlobal('window', {
      location: { origin: 'http://localhost:5173' },
      localStorage: {
        getItem: (key: string) => key === 'token' ? 'jwt-token' : null,
      },
    })

    const message = exportMessageFromEvent(event(1, 'export_created', {
      fileName: 'virtual-expert.pdf',
      downloadUrl: '/manager/virtual-expert/agent/exports/exp_001/download',
    }))

    expect(message).toEqual({
      text: 'virtual-expert.pdf 已生成',
      url: '/api/manager/virtual-expert/agent/exports/exp_001/download?token=jwt-token',
    })
  })

  it('renders markdown final answers with tables and escapes unsafe html', () => {
    const html = renderMarkdownToHtml([
      '## 查询结果',
      '',
      '已定位 **济南市**，`area_id=370100`。',
      '',
      '| 数据 | 数量 |',
      '| --- | ---: |',
      '| 管段 | 3 |',
      '',
      '- 可继续查看告警',
      '',
      '<script>alert(1)</script>',
    ].join('\n'))

    expect(html).toContain('<h3>查询结果</h3>')
    expect(html).toContain('<strong>济南市</strong>')
    expect(html).toContain('<code>area_id=370100</code>')
    expect(html).toContain('<table>')
    expect(html).toContain('<td>管段</td>')
    expect(html).toContain('<ul><li>可继续查看告警</li></ul>')
    expect(html).toContain('&lt;script&gt;alert(1)&lt;/script&gt;')
  })
})

describe('normalizeEvidenceDisplayItems', () => {
  it('turns a tool event into a readable display item', () => {
    const items = normalizeEvidenceDisplayItems(event(1, 'tool_completed', {
      tool_name: 'equipment_status',
      displayHint: 'equipment_status',
      summary: '设备压力波动明显',
      facts: [{ label: '入口压力', value: '0.42MPa', status: 'warning' }],
    }))

    expect(items[0]).toMatchObject({
      kind: 'tool',
      title: '设备状态查询',
      summary: '设备压力波动明显',
      sourceLabel: '工具调用',
    })
    expect(items[0].facts[0]).toMatchObject({ label: '入口压力', value: '0.42MPa', status: 'warning' })
  })

  it('extracts tool metadata and record previews for rich result cards', () => {
    const items = normalizeEvidenceDisplayItems(event(4, 'tool_completed', {
      tool_name: 'query_monitoring_trend',
      summary: 'Monitoring indicators readonly query completed.',
      input: { object_id: 'P003', metric: 'temperature' },
      context: { object_id: 'P003', segment_id: '3' },
      raw_ref: { path: '/internal/virtual-expert/tools/monitoring-trend', source: 'server-web' },
      raw: {
        metric: 'temperature',
        window: '24h',
        records: [{ ave_temperature: 22.96, temperature_status: 'normal' }],
      },
      confidence: 0.9,
    }))

    expect(items[0]).toMatchObject({
      title: '监测趋势查询',
      sourceLabel: 'server-web · /internal/virtual-expert/tools/monitoring-trend',
      score: 0.9,
      records: [{ ave_temperature: 22.96, temperature_status: 'normal' }],
    })
    expect(items[0].meta).toContainEqual({ label: '对象', value: 'P003' })
    expect(items[0].meta).toContainEqual({ label: '指标', value: 'temperature' })
    expect(items[0].meta).toContainEqual({ label: '窗口', value: '24h' })
  })

  it('turns knowledge documents into citation display items', () => {
    const items = normalizeEvidenceDisplayItems(event(2, 'knowledge_search_completed', {
      source: 'domain_knowledge',
      documents: [{
        doc_id: 'sop_001',
        title: '阴保异常排查 SOP',
        summary: '先复核参比电极和近期施工影响',
        citation: 'SOP-CP-001',
        score: 0.82,
      }],
    }))

    expect(items[0]).toMatchObject({
      kind: 'knowledge',
      title: '阴保异常排查 SOP',
      summary: '先复核参比电极和近期施工影响',
      sourceLabel: 'SOP-CP-001',
      score: 0.82,
    })
  })

  it('returns a readable empty knowledge item when retrieval has no documents', () => {
    const items = normalizeEvidenceDisplayItems(event(3, 'knowledge_search_completed', {
      source: 'domain_knowledge',
      documents: [],
    }))

    expect(items[0]).toMatchObject({
      kind: 'knowledge',
      title: '未命中可用知识',
      sourceLabel: 'domain_knowledge',
    })
  })
})

describe('readableAgentError', () => {
  it('maps active run conflicts to a Chinese recovery hint', () => {
    expect(readableAgentError(new Error('409 SESSION_RUN_IN_PROGRESS'))).toBe(
      '当前会话仍有研判在运行，请等待完成或取消后继续追问。',
    )
  })

  it('keeps known error messages readable', () => {
    expect(readableAgentError(new Error('Agent 事件流连接中断，请稍后重试。'))).toBe(
      'Agent 事件流连接中断，请稍后重试。',
    )
  })
})

describe('composer interaction helpers', () => {
  it('submits on Enter but keeps Shift+Enter for multiline input', () => {
    expect(isSubmitComposerKey({ key: 'Enter', shiftKey: false, isComposing: false })).toBe(true)
    expect(isSubmitComposerKey({ key: 'Enter', shiftKey: true, isComposing: false })).toBe(false)
    expect(isSubmitComposerKey({ key: 'Enter', shiftKey: false, isComposing: true })).toBe(false)
    expect(isSubmitComposerKey({ key: 'a', shiftKey: false, isComposing: false })).toBe(false)
  })

  it('detects when the conversation should keep auto-following the bottom', () => {
    expect(isNearConversationBottom({ scrollTop: 700, clientHeight: 280, scrollHeight: 1000 })).toBe(true)
    expect(isNearConversationBottom({ scrollTop: 520, clientHeight: 280, scrollHeight: 1000 })).toBe(false)
  })

  it('adds uploaded image metadata to the message sent to Agent', () => {
    const attachments: AgentComposerAttachment[] = [
      {
        id: 'img_1',
        name: '阀室温度.jpg',
        type: 'image/jpeg',
        size: 153600,
        previewUrl: 'blob:test',
      },
    ]

    expect(buildComposerMessageContent('请结合图片判断风险', attachments)).toContain(
      '- 阀室温度.jpg (image/jpeg, 150 KB)',
    )
  })
})

describe('memory helpers', () => {
  it('normalizes memory accepted payloads', () => {
    const item = normalizeAgentMemoryItem({
      memoryId: 'mem_1',
      memoryType: 'preference',
      content: '回答先给结论。',
      riskLevel: 'low',
    })

    expect(item).toMatchObject({
      id: 'mem_1',
      memoryType: 'preference',
      content: '回答先给结论。',
      riskLevel: 'low',
    })
  })

  it('extracts a memory notice from stream events', () => {
    const notice = memoryNoticeFromEvent(event(8, 'memory_candidate_created', {
      candidateId: 'cand_1',
      content: '以后风险都按高等级处理',
      riskLevel: 'high',
      reason: '需要确认',
    }))

    expect(notice).toMatchObject({
      id: 'cand_1',
      content: '以后风险都按高等级处理',
      riskLevel: 'high',
      reason: '需要确认',
    })
  })

  it('labels known memory types in Chinese', () => {
    expect(memoryTypeLabel('export_preference')).toBe('导出偏好')
    expect(memoryTypeLabel('interaction_preference')).toBe('交互偏好')
  })
})
