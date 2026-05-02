import { fallbackAgentEvents, fallbackAgentSessions } from '../../data/businessFallback'
import { logClientEvent } from '../shared/clientLog'
import {
  cancelAgentRun,
  acceptAgentMemoryCandidate,
  createAgentExport,
  createAgentMessage,
  createAgentSession,
  deleteAgentMemory,
  deleteAgentSession,
  fetchAgentEvents,
  fetchAgentMemories,
  fetchAgentRunEvents,
  fetchAgentSessions,
  fetchAgentTimeline,
  getAgentExportDownloadUrl,
  getAgentRunStreamUrl,
  getSpecificAgentRunStreamUrl,
  normalizeAgentEvent,
  rejectAgentMemoryCandidate,
  runAgentSession,
} from './api'
import type {
  AgentEvent,
  AgentComposerAttachment,
  AgentEvidenceDisplayItem,
  AgentEvidenceItem,
  AgentExportFormat,
  AgentExportPlan,
  AgentExportResponse,
  AgentMemoryItem,
  AgentMemoryStatus,
  AgentMemoryType,
  AgentReactRound,
  AgentRecommendation,
  AgentRunStatus,
  AgentSession,
  AgentTimelineItem,
  AgentTimelineResponse,
  CreateAgentSessionPayload,
} from './types'

export type ComposerKeyState = {
  key: string
  shiftKey: boolean
  isComposing?: boolean
}

export type ConversationScrollState = {
  scrollTop: number
  clientHeight: number
  scrollHeight: number
}

export function getFallbackAgentSessions() {
  return fallbackAgentSessions.map((session) => ({ ...session }))
}

export function getFallbackAgentEvents() {
  return fallbackAgentEvents.map((event) => ({ ...event }))
}

export async function loadAgentSessions(limit = 20): Promise<AgentSession[]> {
  const response = await fetchAgentSessions(limit)
  return response.sessions.map((session) => ({
    id: session.id,
    title: session.title,
    status: session.status,
    summary: session.summary,
    objectName: session.objectName || session.object_name,
    updatedAt: session.updatedAt || session.updated_at,
  }))
}

export async function requestAgentSessionDelete(sessionId: string) {
  return await deleteAgentSession(sessionId)
}

export function memoryTypeLabel(type?: string) {
  const labels: Record<string, string> = {
    preference: '输出偏好',
    export_preference: '导出偏好',
    analysis_preference: '分析偏好',
    interaction_preference: '交互偏好',
  }
  return labels[type || ''] || '偏好'
}

export function normalizeAgentMemoryItem(raw: Record<string, unknown>): AgentMemoryItem {
  return {
    id: String(raw.id ?? raw.candidateId ?? raw.memoryId ?? ''),
    memoryType: String(raw.memoryType ?? raw.memory_type ?? 'preference') as AgentMemoryType,
    preferenceKey: typeof raw.preferenceKey === 'string'
      ? raw.preferenceKey
      : typeof raw.preference_key === 'string'
        ? raw.preference_key
        : undefined,
    content: String(raw.content ?? ''),
    status: String(raw.status ?? 'active') as AgentMemoryStatus,
    riskLevel: raw.riskLevel === 'high' || raw.risk_level === 'high' ? 'high' : 'low',
    reason: typeof raw.reason === 'string' ? raw.reason : undefined,
    createdAt: typeof raw.createdAt === 'string'
      ? raw.createdAt
      : typeof raw.created_at === 'string'
        ? raw.created_at
        : undefined,
    updatedAt: typeof raw.updatedAt === 'string'
      ? raw.updatedAt
      : typeof raw.updated_at === 'string'
        ? raw.updated_at
        : undefined,
  }
}

export function memoryNoticeFromEvent(event: AgentEvent): AgentMemoryItem | undefined {
  if (!['memory_accepted', 'memory_candidate_created', 'memory_recalled'].includes(event.type)) {
    return undefined
  }
  return normalizeAgentMemoryItem(event.payload || {})
}

export async function loadAgentMemories(status: 'active' | 'pending' = 'active') {
  const response = await fetchAgentMemories(status)
  return response.items.map((item) => normalizeAgentMemoryItem(item as unknown as Record<string, unknown>))
}

export async function requestAgentMemoryAccept(candidateId: string) {
  const response = await acceptAgentMemoryCandidate(candidateId)
  return response.memory ? normalizeAgentMemoryItem(response.memory as unknown as Record<string, unknown>) : undefined
}

export async function requestAgentMemoryReject(candidateId: string) {
  const response = await rejectAgentMemoryCandidate(candidateId)
  return response.candidate ? normalizeAgentMemoryItem(response.candidate as unknown as Record<string, unknown>) : undefined
}

export async function requestAgentMemoryDelete(memoryId: string) {
  const response = await deleteAgentMemory(memoryId)
  return response.memory ? normalizeAgentMemoryItem(response.memory as unknown as Record<string, unknown>) : undefined
}

export function isSubmitComposerKey(event: ComposerKeyState) {
  return event.key === 'Enter' && !event.shiftKey && !event.isComposing
}

export function isNearConversationBottom(scroll: ConversationScrollState, tolerance = 36) {
  return scroll.scrollHeight - scroll.scrollTop - scroll.clientHeight <= tolerance
}

export function buildComposerMessageContent(rawInput: string, attachments: AgentComposerAttachment[]) {
  const content = rawInput.trim()
  if (!attachments.length) {
    return content
  }
  const imageLines = attachments.map((item) => `- ${item.name} (${item.type || 'image/*'}, ${formatAttachmentSize(item.size)})`)
  return [content, '', '[图片附件]', ...imageLines].filter(Boolean).join('\n')
}

function formatAttachmentSize(size: number) {
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${Math.round(size / 1024)} KB`
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

export async function startAgentAnalysis(payload: CreateAgentSessionPayload) {
  const session = await createAgentSession(payload)
  const run = await runAgentSession(session.sessionId)
  return { session, run }
}

export async function startAgentAnalysisStream(
  payload: CreateAgentSessionPayload,
  onEvent: (event: AgentEvent) => void,
) {
  const session = await createAgentSession(payload)
  const events: AgentEvent[] = []

  return await new Promise<{
    session: Awaited<ReturnType<typeof createAgentSession>>
    run: { sessionId: string; runId: string; events: AgentEvent[]; status: AgentRunStatus }
  }>((resolve, reject) => {
    const source = new EventSource(getAgentRunStreamUrl(session.sessionId))
    let runId = ''
    let settled = false

    source.addEventListener('agent_event', (message) => {
      const event = normalizeAgentEvent(JSON.parse(message.data) as Record<string, unknown>)
      runId = event.runId || runId
      events.push(event)
      logClientEvent('virtual-expert.sse', 'agent_event received', {
        seq: event.seq,
        type: event.type,
        runId: event.runId,
        payload: event.payload,
      })
      onEvent(event)
    })

    source.addEventListener('run_status', (message) => {
      const payload = JSON.parse(message.data) as Record<string, unknown>
      const finalRunId = String(payload.run_id ?? payload.runId ?? runId)
      const status = normalizeRunStatus(payload.status)
      logClientEvent('virtual-expert.sse', 'run_status received', payload)
      settled = true
      source.close()
      resolve({
        session,
        run: {
          sessionId: session.sessionId,
          runId: finalRunId,
          events,
          status,
        },
      })
    })

    source.addEventListener('agent_error', (message) => {
      const payload = JSON.parse(message.data) as Record<string, unknown>
      logClientEvent('virtual-expert.sse', 'agent_error received', payload)
      const event = normalizeAgentEvent({
        id: `evt_error_${Date.now()}`,
        sessionId: session.sessionId,
        runId,
        seq: events.length + 1,
        type: String(payload.type ?? 'run_failed'),
        level: 'error',
        title: 'Agent stream failed',
        message: String(payload.message ?? 'Agent stream failed'),
        payload,
      })
      events.push(event)
      onEvent(event)
      settled = true
      source.close()
      resolve({
        session,
        run: {
          sessionId: session.sessionId,
          runId,
          events,
          status: 'failed',
        },
      })
    })

    source.onerror = () => {
      logClientEvent('virtual-expert.sse', 'event source error', { sessionId: session.sessionId, runId })
      source.close()
      if (!settled) {
        reject(new Error('Agent 事件流连接中断，请稍后重试。'))
      }
    }
  })
}

export async function sendAgentMessageStream(
  sessionId: string,
  content: string,
  onEvent: (event: AgentEvent) => void,
) {
  const created = await createAgentMessage(sessionId, { content })
  const events: AgentEvent[] = []

  return await new Promise<{
    message: typeof created.message
    exportPlan?: AgentExportPlan
    run: { sessionId: string; runId: string; events: AgentEvent[]; status: AgentRunStatus } | null
  }>((resolve, reject) => {
    if (created.exportPlan || !created.run) {
      resolve({
        message: created.message,
        exportPlan: created.exportPlan,
        run: null,
      })
      return
    }

    const createdRun = created.run
    const source = new EventSource(getSpecificAgentRunStreamUrl(sessionId, createdRun.id))
    let settled = false

    source.addEventListener('agent_event', (message) => {
      const event = normalizeAgentEvent(JSON.parse(message.data) as Record<string, unknown>)
      events.push(event)
      logClientEvent('virtual-expert.sse', 'agent_event received', {
        seq: event.seq,
        type: event.type,
        runId: event.runId,
        payload: event.payload,
      })
      onEvent(event)
    })

    source.addEventListener('run_status', (message) => {
      const payload = JSON.parse(message.data) as Record<string, unknown>
      const status = normalizeRunStatus(payload.status)
      logClientEvent('virtual-expert.sse', 'run_status received', payload)
      settled = true
      source.close()
      resolve({
        message: created.message,
        run: {
          sessionId,
          runId: createdRun.id,
          events,
          status,
        },
      })
    })

    source.addEventListener('agent_error', (message) => {
      const payload = JSON.parse(message.data) as Record<string, unknown>
      const event = normalizeAgentEvent({
        id: `evt_error_${Date.now()}`,
        sessionId,
        runId: createdRun.id,
        seq: events.length + 1,
        type: String(payload.type ?? 'run_failed'),
        level: 'error',
        title: 'Agent stream failed',
        message: String(payload.message ?? 'Agent stream failed'),
        payload,
      })
      events.push(event)
      onEvent(event)
      settled = true
      source.close()
      resolve({
        message: created.message,
        run: {
          sessionId,
          runId: createdRun.id,
          events,
          status: 'failed',
        },
      })
    })

    source.onerror = () => {
      logClientEvent('virtual-expert.sse', 'event source error', { sessionId, runId: createdRun.id })
      source.close()
      if (!settled) {
        reject(new Error('Agent 事件流连接中断，请稍后重试。'))
      }
    }
  })
}

function normalizeRunStatus(status: unknown): AgentRunStatus {
  if (
    status === 'completed' ||
    status === 'failed' ||
    status === 'cancelled' ||
    status === 'awaiting_user'
  ) {
    return status
  }
  return 'failed'
}

export async function loadAgentEvents(sessionId: string, afterSeq = 0) {
  try {
    return await fetchAgentEvents(sessionId, afterSeq)
  } catch {
    return { sessionId, events: getFallbackAgentEvents() }
  }
}

export async function loadAgentRunEvents(sessionId: string, runId: string, afterSeq = 0) {
  try {
    return await fetchAgentRunEvents(sessionId, runId, afterSeq)
  } catch {
    return { sessionId, runId, events: getFallbackAgentEvents() }
  }
}

export async function loadAgentTimeline(
  sessionId: string,
  query: { beforeCursor?: string; limit?: number } = {},
): Promise<AgentTimelineResponse> {
  try {
    return await fetchAgentTimeline(sessionId, query)
  } catch {
    const fallbackEvents = getFallbackAgentEvents()
    return {
      sessionId,
      items: [
        {
          turnId: `${sessionId}_fallback_turn`,
          cursor: `${new Date().toISOString()}|${sessionId}_fallback_turn`,
          userMessage: {
            id: `${sessionId}_fallback_message`,
            content: 'CP-04 阴保电位波动超过阈值',
            createdAt: '2026-04-23 16:42:29',
          },
          run: {
            id: fallbackEvents[0]?.runId || 'run_demo_001',
            status: 'completed',
            summary: timelineSummaryFromEvents(fallbackEvents),
            eventCount: fallbackEvents.length,
            createdAt: fallbackEvents[0]?.createdAt,
            completedAt: fallbackEvents.at(-1)?.createdAt,
          },
        },
      ],
      hasMoreBefore: false,
      beforeCursor: undefined,
    }
  }
}

export function timelineItemToTurns(item: AgentTimelineItem) {
  return [
    {
      kind: 'user_message' as const,
      id: item.userMessage.id,
      messageId: item.userMessage.id,
      content: item.userMessage.content,
      createdAt: item.userMessage.createdAt,
    },
    {
      kind: 'agent_run' as const,
      id: item.run?.id || `${item.userMessage.id}_run`,
      runId: item.run?.id || '',
      triggeringMessageId: item.userMessage.id,
      status: item.run?.status || 'failed',
      events: [],
      eventsLoaded: false,
      summary: item.run?.summary,
      streamState: createEmptyStreamState(),
      collapsed: item.run?.status !== 'running',
    },
  ]
}

export function createEmptyStreamState() {
  return {
    thinkingText: '',
    finalText: '',
  }
}

export function timelineSummaryFromEvents(events: AgentEvent[]) {
  const recommendation = normalizeRecommendation(
    events.find((event) => event.type === 'recommendation_generated')?.payload,
  )
  const finalAnswer = events
    .filter((event) => event.type === 'final_answer_delta')
    .map((event) => String(event.payload?.delta ?? ''))
    .join('')
  return {
    finalAnswer: finalAnswer || recommendation?.judgment || recommendation?.summary || '',
    riskLevel: recommendation?.riskLevel,
    judgment: recommendation?.judgment,
    recommendedActions: recommendation?.recommendedActions || [],
  }
}

export async function requestAgentCancel(sessionId: string, runId: string) {
  return cancelAgentRun(sessionId, runId)
}

export async function requestAgentExport(
  sessionId: string,
  format: AgentExportFormat,
  runId?: string,
  exportPlan?: AgentExportPlan,
): Promise<AgentExportResponse> {
  const response = await createAgentExport({ sessionId, format, runId, exportPlan })
  return {
    ...response,
    downloadUrl: getAgentExportDownloadUrl(response.downloadUrl),
  }
}

export function readableAgentError(error: unknown) {
  if (error instanceof Error && error.message) {
    if (error.message.includes('SESSION_RUN_IN_PROGRESS') || error.message.includes('409')) {
      return '当前会话仍有研判在运行，请等待完成或取消后继续追问。'
    }
    return error.message
  }
  return '虚拟专家服务暂时不可用，请稍后重试。'
}

export function normalizeRecommendation(payload: Record<string, unknown> | undefined): AgentRecommendation | undefined {
  if (!payload) {
    return undefined
  }

  const recommendedActions = payload.recommended_actions ?? payload.recommendedActions ?? payload.actions
  const missingInformation = payload.missing_information ?? payload.missingInformation
  const riskLevel = String(payload.risk_level ?? payload.riskLevel ?? 'medium')

  return {
    summary: String(payload.summary ?? payload.conclusion ?? ''),
    riskLevel: isRiskLevel(riskLevel) ? riskLevel : 'medium',
    judgment: String(payload.judgment ?? payload.conclusion ?? payload.summary ?? ''),
    recommendedActions: Array.isArray(recommendedActions) ? recommendedActions.map(String) : [],
    missingInformation: Array.isArray(missingInformation) ? missingInformation.map(String) : [],
    humanConfirmationRequired: Boolean(
      payload.human_confirmation_required ?? payload.humanConfirmationRequired ?? true,
    ),
  }
}

export function groupEventsIntoReactRounds(
  events: AgentEvent[],
  options: { status: AgentRunStatus; roundOverrides?: Record<string, boolean> },
): AgentReactRound[] {
  const rounds: AgentReactRound[] = []
  let currentStep = 1

  const ensureRound = (step: number) => {
    let round = rounds.find((item) => item.step === step)
    if (!round) {
      round = {
        id: `round_${step}`,
        step,
        title: `第 ${step} 轮`,
        events: [],
        summary: {
          toolCount: 0,
          knowledgeCount: 0,
          warningCount: 0,
          errorCount: 0,
        },
        collapsed: false,
      }
      rounds.push(round)
    }
    return round
  }

  events.forEach((event) => {
    const payloadStep = toNumber(event.payload?.step)
    if (event.type === 'llm_step_started' && payloadStep) {
      currentStep = payloadStep
    }
    const round = ensureRound(payloadStep || currentStep)
    round.events.push(event)

    if (event.type === 'tool_completed') {
      round.summary.toolCount += 1
    }
    if (['knowledge_search_completed', 'retrieval_completed'].includes(event.type)) {
      const documentCount = Array.isArray(event.payload?.documents) ? event.payload.documents.length : 0
      round.summary.knowledgeCount += documentCount
      if (documentCount === 0) {
        round.summary.warningCount += 1
      }
    }
    if (event.level === 'error' || event.type.endsWith('_failed')) {
      round.summary.errorCount += 1
    }
  })

  const finalRound = rounds.at(-1)
  return rounds.map((round) => {
    const override = options.roundOverrides?.[round.id]
    if (typeof override === 'boolean') {
      return { ...round, collapsed: !override, userPinned: true }
    }

    const hasIssue = round.summary.errorCount > 0 || round.summary.warningCount > 0
    const shouldExpand = round === finalRound || hasIssue
    return { ...round, collapsed: !shouldExpand }
  })
}

export function normalizeEvidenceDisplayItems(event: AgentEvent): AgentEvidenceDisplayItem[] {
  const payload = event.payload || {}

  if (event.type === 'tool_completed') {
    const facts = Array.isArray(payload.facts) ? payload.facts : []
    const raw = isPlainRecord(payload.raw) ? payload.raw : payload
    const rawRef = isPlainRecord(payload.raw_ref) ? payload.raw_ref : undefined
    return [
      {
        id: event.id,
        kind: 'tool',
        title: toolDisplayName(String(payload.tool_name ?? payload.toolName ?? event.title ?? '工具结果')),
        summary: String(payload.summary ?? event.title ?? '工具已返回结果'),
        sourceLabel: toolSourceLabel(rawRef),
        score: toNumber(payload.confidence),
        meta: normalizeToolMeta(payload, raw, rawRef),
        facts: facts.flatMap((fact) => normalizeDisplayFacts(fact as Record<string, unknown>)),
        records: normalizeToolRecords(raw.records ?? payload.records),
        raw: payload,
      },
    ]
  }

  if (['knowledge_search_completed', 'retrieval_completed'].includes(event.type)) {
    const documents = Array.isArray(payload.documents) ? payload.documents : []
    if (!documents.length) {
      return [
        {
          id: `${event.id}_empty`,
          kind: 'knowledge',
          title: '未命中可用知识',
          summary: '本次检索没有返回可用文档，后续判断需要依赖工具结果或人工补充。',
          sourceLabel: String(payload.source ?? '知识库'),
          facts: [],
          raw: payload,
        },
      ]
    }

    return documents.map((item, index) => {
      const document = item as Record<string, unknown>
      return {
        id: String(document.doc_id ?? `${event.id}_doc_${index}`),
        kind: 'knowledge',
        title: String(document.title ?? document.citation ?? '知识命中'),
        summary: String(document.summary ?? document.content ?? ''),
        sourceLabel: String(document.citation ?? document.source ?? payload.source ?? '知识库'),
        score: toNumber(document.score),
        facts: [],
        raw: document,
      }
    })
  }

  return []
}

function normalizeDisplayFacts(fact: Record<string, unknown>) {
  const points = fact.points
  if (Array.isArray(points) && points.length) {
    return points
      .filter(isPlainRecord)
      .map((point) => normalizeDisplayFact(point))
  }
  return [normalizeDisplayFact(fact)]
}

function normalizeDisplayFact(fact: Record<string, unknown>) {
  const status = String(fact.status ?? '')
  return {
    label: String(fact.label ?? fact.metric ?? fact.name ?? '指标'),
    value: formatToolValue(fact.value ?? fact.current ?? fact.text ?? ''),
    status: ['normal', 'warning', 'critical'].includes(status)
      ? status as 'normal' | 'warning' | 'critical'
      : undefined,
  }
}

function normalizeToolMeta(
  payload: Record<string, unknown>,
  raw: Record<string, unknown>,
  rawRef?: Record<string, unknown>,
) {
  const input = isPlainRecord(payload.input) ? payload.input : {}
  const context = isPlainRecord(payload.context) ? payload.context : {}
  const rows = [
    { label: '对象', value: input.object_id ?? input.objectId ?? context.object_id ?? context.objectId },
    { label: '指标', value: input.metric ?? raw.metric },
    { label: '窗口', value: raw.window },
    { label: '管段', value: context.segment_id ?? context.segmentId },
    { label: '来源', value: rawRef?.source },
  ]
  return rows
    .filter((row) => row.value !== undefined && row.value !== null && row.value !== '')
    .map((row) => ({ label: row.label, value: formatToolValue(row.value) }))
}

function normalizeToolRecords(records: unknown): Array<Record<string, unknown>> {
  if (!Array.isArray(records)) {
    return []
  }
  return records.filter(isPlainRecord)
}

function toolSourceLabel(rawRef?: Record<string, unknown>) {
  const source = String(rawRef?.source ?? '')
  const path = String(rawRef?.path ?? '')
  if (source && path) {
    return `${source} · ${path}`
  }
  return source || path || '工具调用'
}

function toolDisplayName(toolName: string) {
  const labels: Record<string, string> = {
    query_monitoring_trend: '监测趋势查询',
    monitoring_trend: '监测趋势查询',
    query_pipe_segment_context: '管段上下文查询',
    pipe_segment_context: '管段上下文查询',
    query_equipment_status: '设备状态查询',
    equipment_status: '设备状态查询',
    query_related_tasks: '关联任务查询',
    related_tasks: '关联任务查询',
    search_similar_cases: '相似案例检索',
    similar_cases: '相似案例检索',
    retrieve_domain_knowledge: '领域知识检索',
  }
  return labels[toolName] || toolName
}

function formatToolValue(value: unknown) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }
  if (typeof value === 'string') {
    return value
  }
  return JSON.stringify(value)
}

function isPlainRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

export function extractEvidenceItems(events: AgentEvent[]): AgentEvidenceItem[] {
  return events.flatMap((event) => {
    if (event.type === 'tool_completed') {
      return normalizeToolEvidence(event)
    }
    if (event.type === 'retrieval_completed') {
      return normalizeRetrievalEvidence(event)
    }
    if (event.type === 'knowledge_search_completed') {
      return normalizeRetrievalEvidence(event)
    }
    return []
  })
}

function normalizeToolEvidence(event: AgentEvent): AgentEvidenceItem[] {
  const payload = event.payload || {}
  const facts = Array.isArray(payload.facts) ? payload.facts : []
  return [
    {
      id: event.id,
      title: String(payload.tool_name ?? payload.toolName ?? event.title ?? 'tool'),
      summary: String(payload.summary ?? event.title ?? ''),
      sourceType: 'tool',
      sourceRef: String(payload.tool_name ?? payload.toolName ?? event.type),
      confidence: toNumber(payload.confidence),
    },
    ...facts.map((fact, index) => ({
      id: `${event.id}_fact_${index}`,
      title: String((fact as Record<string, unknown>).metric ?? 'fact'),
      summary: JSON.stringify(fact),
      sourceType: 'business_data' as const,
      sourceRef: String(payload.tool_name ?? payload.toolName ?? event.type),
      confidence: toNumber(payload.confidence),
    })),
  ]
}

function normalizeRetrievalEvidence(event: AgentEvent): AgentEvidenceItem[] {
  const documents = event.payload?.documents
  if (!Array.isArray(documents)) {
    return []
  }
  return documents.map((item, index) => {
    const document = item as Record<string, unknown>
    return {
      id: String(document.doc_id ?? `${event.id}_doc_${index}`),
      title: String(document.title ?? document.citation ?? 'retrieved knowledge'),
      summary: String(document.summary ?? ''),
      sourceType: 'rag',
      sourceRef: String(document.citation ?? document.source ?? 'rag'),
      confidence: toNumber(document.score),
    }
  })
}

function isRiskLevel(value: string): value is AgentRecommendation['riskLevel'] {
  return ['low', 'medium', 'high', 'critical'].includes(value)
}

function toNumber(value: unknown) {
  return typeof value === 'number' ? value : undefined
}
