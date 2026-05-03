<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import ModuleShell from '../../../components/business/ModuleShell.vue'
import { useRouter } from '../../../router'
import { createAgentSession } from '../api'
import type {
  AgentConversationTurn,
  AgentComposerAttachment,
  AgentEvent,
  AgentEvidenceDisplayItem,
  AgentExportFormat,
  AgentExportPlan,
  AgentMemoryItem,
  AgentModelTier,
  AgentRecommendation,
  AgentRunSummary,
  AgentRunStatus,
  AgentSession,
} from '../types'
import {
  buildComposerMessageContent,
  createEmptyStreamState,
  exportMessageFromEvent,
  getAgentEventRenderDelay,
  getFinalAnswerPendingNotice,
  getAgentStartupNotice,
  getFallbackAgentEvents,
  getFallbackAgentSessions,
  isDisplayableAgentEvent,
  isNearConversationBottom,
  isSubmitComposerKey,
  loadAgentMemories,
  loadAgentSessions,
  loadAgentRunEvents,
  loadAgentTimeline,
  memoryNoticeFromEvent,
  memoryTypeLabel,
  normalizeEvidenceDisplayItems,
  normalizeRecommendation,
  readableAgentError,
  requestAgentMemoryAccept,
  requestAgentMemoryDelete,
  requestAgentMemoryReject,
  requestAgentCancel,
  requestAgentExport,
  requestAgentSessionDelete,
  requestAgentSessionShare,
  requestAgentSessionUpdate,
  renderMarkdownToHtml,
  sendAgentMessageStream,
  shouldQueueAgentEventForPacing,
  summarizeRecalledMemoryItems,
  timelineItemToTurns,
  timelineSummaryFromEvents,
} from '../service'

type BrowserSpeechRecognition = {
  lang: string
  interimResults: boolean
  continuous: boolean
  start: () => void
  stop: () => void
  onresult: ((event: { results: ArrayLike<{ isFinal?: boolean; 0: { transcript: string } }> }) => void) | null
  onend: (() => void) | null
  onerror: (() => void) | null
}

type SpeechWindow = Window & {
  SpeechRecognition?: new () => BrowserSpeechRecognition
  webkitSpeechRecognition?: new () => BrowserSpeechRecognition
}

type TimelineItem =
  | {
      kind: 'startup'
      id: string
      title: string
      steps: string[]
      reason: string
    }
  | {
      kind: 'summary'
      id: string
      text: string
      stats: string
    }
  | {
      kind: 'thinking'
      id: string
      seq: number
      step: number
      content: string
      running: boolean
    }
  | {
      kind: 'final_pending'
      id: string
      title: string
      detail: string
    }
  | {
      kind: 'final'
      id: string
      seq: number
      content: string
      recommendation?: AgentRecommendation
      running: boolean
    }
  | {
      kind: 'finalizing'
      id: string
      text: string
      detail: string
    }
  | {
      kind: 'tool_batch'
      id: string
      seq: number
      step: number
      events: AgentEvent[]
      title: string
      caption: string
    }
  | {
      kind: 'event'
      event: AgentEvent
      displaySeq: number
      label: string
      caption: string
    }

const input = ref('')
const sessions = ref<AgentSession[]>(getFallbackAgentSessions())
const selectedSessionId = ref(sessions.value[0]?.id || '')
const turns = ref<AgentConversationTurn[]>([
  {
    kind: 'user_message',
    id: 'msg_demo_001',
    content: 'CP-04 阴保电位波动超过阈值',
    createdAt: '2026-04-23 16:42:29',
  },
  {
    kind: 'agent_run',
    id: 'turn_demo_001',
    runId: 'run_demo_001',
    status: 'completed',
    events: getFallbackAgentEvents(),
    eventsLoaded: true,
    summary: timelineSummaryFromEvents(getFallbackAgentEvents()),
    streamState: createEmptyStreamState(),
    collapsed: true,
  },
])
const activeRunId = ref('run_demo_001')
const isRunning = ref(false)
const errorMessage = ref('')
const exportMessage = ref<{ text: string; url: string } | null>(null)
const pendingExportPlan = ref<AgentExportPlan | null>(null)
const isExporting = ref(false)
const conversationScroll = ref<HTMLElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const imageAttachments = ref<AgentComposerAttachment[]>([])
const beforeCursor = ref<string | undefined>()
const hasMoreBefore = ref(false)
const isLoadingHistory = ref(false)
const autoFollowConversation = ref(true)
const programmaticScroll = ref(false)
const isListening = ref(false)
const speechRecognition = ref<BrowserSpeechRecognition | null>(null)
const memoryPanelOpen = ref(false)
const activeMemories = ref<AgentMemoryItem[]>([])
const pendingMemories = ref<AgentMemoryItem[]>([])
const memoryNotices = ref<AgentMemoryItem[]>([])
const recalledMemorySummary = ref('')
const thinkingExpansion = ref<Record<string, boolean>>({})
const thinkingAutoFollow = ref<Record<string, boolean>>({})
const pendingStreamEvents = ref<AgentEvent[]>([])
const streamEventTimer = ref<number | undefined>()
const lastStreamBlockRenderedAt = ref(0)
const router = useRouter()
const selectedModelTier = ref<AgentModelTier>('auto')
const activeRunModelLabel = ref('')
const sidebarCollapsed = ref(false)
const sidebarHidden = ref(false)
const sessionSearch = ref('')
const archivedSessions = ref<AgentSession[]>([])
const archivedOpen = ref(false)
const sessionMenuOpenId = ref<string | null>(null)
const searchExpanded = ref(false)
const shareNotice = ref('')

const streamBlockRenderIntervalMs = 280

const modelTierOptions: Array<{ tier: AgentModelTier; label: string; model: string }> = [
  { tier: 'auto', label: 'Auto', model: 'moonshot-v1-auto' },
  { tier: 'light', label: '轻量', model: 'moonshot-v1-8k' },
  { tier: 'standard', label: '标准', model: 'moonshot-v1-32k' },
  { tier: 'performance', label: '性能', model: 'kimi-k2.5' },
  { tier: 'ultimate', label: '极致', model: 'kimi-k2.6' },
]

const selectedSession = computed(() =>
  sessions.value.find((session) => session.id === selectedSessionId.value),
)
const speechSupported = computed(() => Boolean(getSpeechRecognitionConstructor()))
const filteredSessions = computed(() => filterSessions(sessions.value))
const filteredArchivedSessions = computed(() => filterSessions(archivedSessions.value))
const selectedModelLabel = computed(() =>
  modelTierOptions.find((item) => item.tier === selectedModelTier.value)?.label || 'Auto',
)

const quickExportFormats: Array<{ format: AgentExportFormat; label: string }> = [
  { format: 'pdf', label: '导出 PDF' },
  { format: 'excel', label: '导出 Excel' },
  { format: 'docx', label: '导出 Word' },
  { format: 'md', label: '导出 Markdown' },
  { format: 'txt', label: '导出 TXT' },
]

function exportFormatLabel(format: AgentExportFormat) {
  const labels: Record<AgentExportFormat, string> = {
    pdf: 'PDF',
    excel: 'Excel',
    docx: 'Word',
    md: 'Markdown',
    txt: 'TXT',
  }
  return labels[format]
}

onMounted(() => {
  void loadInitialSessions()
  void refreshMemories()
})

onBeforeUnmount(() => {
  speechRecognition.value?.stop()
  if (streamEventTimer.value !== undefined) {
    window.clearTimeout(streamEventTimer.value)
  }
  imageAttachments.value.forEach((item) => URL.revokeObjectURL(item.previewUrl))
})

async function loadInitialSessions() {
  try {
    const loadedSessions = await loadAgentSessions(20)
    archivedSessions.value = await loadAgentSessions(50, true)
    if (!loadedSessions.length) {
      return
    }
    sessions.value = loadedSessions
    const routeSessionId = router.currentRoute.value.params.sessionId
    const target = loadedSessions.find((session) => session.id === routeSessionId) || loadedSessions[0]
    await selectSession(target, { updateRoute: false })
  } catch {
    // Keep the demo fallback available when the agent service is offline.
  }
}

async function refreshMemories() {
  try {
    const [active, pending] = await Promise.all([
      loadAgentMemories('active'),
      loadAgentMemories('pending'),
    ])
    activeMemories.value = active
    pendingMemories.value = pending
  } catch {
    activeMemories.value = []
    pendingMemories.value = []
  }
}

async function acceptMemory(item: AgentMemoryItem) {
  try {
    const accepted = await requestAgentMemoryAccept(item.id)
    pendingMemories.value = pendingMemories.value.filter((memory) => memory.id !== item.id)
    memoryNotices.value = memoryNotices.value.filter((memory) => memory.id !== item.id)
    if (accepted) {
      activeMemories.value = [accepted, ...activeMemories.value.filter((memory) => memory.id !== accepted.id)]
    }
  } catch (error) {
    errorMessage.value = readableAgentError(error)
  }
}

async function rejectMemory(item: AgentMemoryItem) {
  try {
    await requestAgentMemoryReject(item.id)
    pendingMemories.value = pendingMemories.value.filter((memory) => memory.id !== item.id)
    memoryNotices.value = memoryNotices.value.filter((memory) => memory.id !== item.id)
  } catch (error) {
    errorMessage.value = readableAgentError(error)
  }
}

async function removeMemory(item: AgentMemoryItem) {
  try {
    await requestAgentMemoryDelete(item.id)
    activeMemories.value = activeMemories.value.filter((memory) => memory.id !== item.id)
    if (recalledMemorySummary.value.includes(item.content)) {
      recalledMemorySummary.value = ''
    }
  } catch (error) {
    errorMessage.value = readableAgentError(error)
  }
}

function rememberNoticeFromEvent(event: AgentEvent) {
  if (event.type === 'model_selected') {
    activeRunModelLabel.value = `${String(event.payload?.label ?? 'Auto')} · ${String(event.payload?.model ?? '')}`
    return
  }
  if (event.type === 'memory_recalled') {
    const items = Array.isArray(event.payload?.items) ? event.payload.items : []
    recalledMemorySummary.value = summarizeRecalledMemoryItems(items)
    return
  }
  const notice = memoryNoticeFromEvent(event)
  if (!notice || !notice.id) {
    return
  }
  const nextNotice = {
    ...notice,
    status: event.type === 'memory_candidate_created' ? 'pending' as const : 'active' as const,
  }
  memoryNotices.value = [nextNotice, ...memoryNotices.value.filter((item) => item.id !== nextNotice.id)].slice(0, 6)
  if (event.type === 'memory_candidate_created') {
    pendingMemories.value = [nextNotice, ...pendingMemories.value.filter((item) => item.id !== nextNotice.id)]
  }
  if (event.type === 'memory_accepted') {
    activeMemories.value = [nextNotice, ...activeMemories.value.filter((item) => item.id !== nextNotice.id)]
  }
}

function statusLabel(status: AgentSession['status'] | AgentRunStatus) {
  const labels: Record<string, string> = {
    created: '待分析',
    running: '运行中',
    awaiting_user: '待确认',
    completed: '已完成',
    failed: '失败',
    cancelled: '已取消',
    archived: '已归档',
  }
  return labels[status] || status
}

function statusClass(status: AgentSession['status'] | AgentRunStatus) {
  if (status === 'completed' || status === 'awaiting_user') {
    return 'is-good'
  }
  if (status === 'failed' || status === 'cancelled') {
    return 'is-danger'
  }
  if (status === 'running') {
    return 'is-warn'
  }
  return ''
}

function riskLabel(riskLevel?: string) {
  const labels: Record<string, string> = {
    low: '低风险',
    medium: '中风险',
    high: '高风险',
    critical: '严重风险',
  }
  return labels[riskLevel || ''] || '未定级'
}

function riskClass(riskLevel?: string) {
  if (riskLevel === 'low') {
    return 'is-good'
  }
  if (riskLevel === 'medium') {
    return 'is-warn'
  }
  if (riskLevel === 'high' || riskLevel === 'critical') {
    return 'is-danger'
  }
  return ''
}

function scopeLabel(scope?: AgentExportPlan['scope']) {
  const labels: Record<string, string> = {
    agent_selected: 'Agent 自动筛选',
    latest_turn: '当前研判',
    full_session: '整个会话',
    all_conversations: '全部对话',
  }
  return labels[scope || 'agent_selected'] || 'Agent 自动筛选'
}

function styleLabel(style?: AgentExportPlan['style']) {
  const labels: Record<string, string> = {
    standard_report: '标准报告',
    formal_report: '正式汇报',
    brief_summary: '简明摘要',
    operation_sheet: '处置单',
  }
  return labels[style || 'standard_report'] || '标准报告'
}

function detailLabel(detailLevel?: AgentExportPlan['detailLevel']) {
  const labels: Record<string, string> = {
    brief: '简版',
    standard: '标准',
    detailed: '详细',
  }
  return labels[detailLevel || 'standard'] || '标准'
}

function filterSessions(items: AgentSession[]) {
  const query = sessionSearch.value.trim().toLowerCase()
  const filtered = query
    ? items.filter((session) => session.title.toLowerCase().includes(query))
    : items
  return [...filtered].sort((left, right) => Number(Boolean(right.pinned)) - Number(Boolean(left.pinned)))
}

function eventLabel(type: string) {
  const labels: Record<string, string> = {
    run_started: '运行中',
    run_preparation_completed: '准备完成',
    context_built: '上下文',
    model_selected: '模型',
    llm_step_started: '思考开始',
    llm_thinking_started: '开始思考',
    llm_thinking_delta: '思考中',
    llm_thinking_completed: '思考完成',
    llm_step_completed: '思考完成',
    action_text_delta: '动作生成中',
    action_selected: '下一步',
    tool_started: '调用工具',
    tool_progress: '工具进度',
    tool_completed: '工具返回',
    tool_failed: '工具失败',
    retrieval_completed: '检索完成',
    knowledge_search_started: '检索知识',
    knowledge_search_completed: '知识命中',
    memory_search_completed: '记忆召回',
    memory_accepted: '已记住偏好',
    memory_candidate_created: '偏好待确认',
    memory_recalled: '偏好召回',
    final_answer_started: '输出开始',
    final_answer_delta: '输出中',
    final_answer_completed: '输出完成',
    run_completed: '完成',
    reasoning_completed: '推理结束',
    recommendation_generated: '生成建议',
    export_created: '导出完成',
    export_failed: '导出失败',
    awaiting_user: '等待确认',
    run_failed: '运行失败',
    run_cancelled: '已取消',
  }
  return labels[type] || type
}

function eventCaption(event: AgentEvent) {
  const payload = event.payload || {}
  if (event.type === 'run_preparation_completed') {
    return String(payload.detail ?? payload.label ?? event.title ?? '准备完成')
  }
  if (event.type === 'run_started') {
    return '已接收输入，开始装配上下文'
  }
  if (event.type === 'context_built') {
    const toolCount = Array.isArray(payload.available_tools) ? payload.available_tools.length : 0
    const memoryCount = Number(payload.summary_memory_count ?? 0) + Number(payload.preference_memory_count ?? 0)
    return `工具 ${toolCount} 个 · 记忆 ${memoryCount} 条`
  }
  if (event.type === 'model_selected') {
    return `本轮使用 ${String(payload.label ?? '')} · ${String(payload.model ?? '')}`
  }
  if (event.type === 'plan_created') {
    const steps = Array.isArray(payload.steps) ? payload.steps.map(String).slice(0, 3).join('、') : ''
    return steps || '已形成排查顺序'
  }
  if (event.type === 'action_selected') {
    if (payload.thought_summary) {
      return String(payload.thought_summary)
    }
    if (payload.action === 'tool_call') {
      return `调用工具：${String(payload.tool_name ?? '未命名工具')}`
    }
    if (payload.action === 'knowledge_search') {
      return `检索知识：${String(payload.search_query ?? '')}`
    }
    if (payload.action === 'memory_search') {
      return `检索记忆：${String(payload.search_query ?? '')}`
    }
    if (payload.action === 'final_answer') {
      return '信息已够用，准备生成最终回答'
    }
    return String(payload.action ?? 'Agent 已选择下一步')
  }
  if (event.type === 'tool_started') {
    return String(payload.tool_name ?? payload.toolName ?? '工具执行中')
  }
  if (event.type === 'tool_completed') {
    return String(payload.summary ?? payload.tool_name ?? payload.toolName ?? '工具已返回业务数据')
  }
  if (event.type === 'tool_failed') {
    return String(payload.error ?? '工具调用失败')
  }
  if (event.type === 'retrieval_completed' || event.type === 'knowledge_search_completed') {
    const documents = Array.isArray(payload.documents) ? payload.documents : []
    const titles = documents
      .map((document) => String((document as Record<string, unknown>).title ?? ''))
      .filter(Boolean)
      .slice(0, 2)
      .join('、')
    return titles ? `命中：${titles}` : '未命中可用知识'
  }
  if (event.type === 'memory_search_completed') {
    const items = Array.isArray(payload.items) ? payload.items : []
    const summary = summarizeRecalledMemoryItems(items)
    return summary ? `召回：${summary}` : '没有召回摘要记忆'
  }
  if (event.type === 'memory_accepted') {
    return String(payload.content ?? '已保存为你的偏好')
  }
  if (event.type === 'memory_candidate_created') {
    return String(payload.content ?? '这个偏好需要确认')
  }
  if (event.type === 'memory_recalled') {
    const items = Array.isArray(payload.items) ? payload.items.length : 0
    return items ? `本轮参考 ${items} 条偏好` : '本轮没有召回偏好'
  }
  if (event.type === 'recommendation_generated') {
    const recommendation = normalizeRecommendation(payload)
    return recommendation?.judgment || recommendation?.summary || '已生成结构化建议'
  }
  if (event.type === 'export_created') {
    return String(payload.fileName ?? payload.file_name ?? '导出文件已生成')
  }
  if (event.type === 'export_failed') {
    return String(payload.error ?? '导出文件生成失败')
  }
  if (event.type === 'run_completed') {
    return '本轮研判已完成'
  }
  return event.message || event.title || eventLabel(event.type)
}

function eventDisplayTitle(event: AgentEvent, fallback: string) {
  if (event.type === 'action_selected') {
    return ''
  }
  if (event.type === 'run_preparation_completed') {
    return String(event.payload?.label ?? event.title ?? fallback)
  }
  if (event.type === 'memory_search_completed') {
    return event.payload?.query ? '摘要记忆检索' : '摘要记忆已加载'
  }
  if (event.type === 'context_built') {
    return '上下文已准备'
  }
  if (event.type === 'model_selected') {
    return '本轮模型已锁定'
  }
  if (event.type === 'run_started') {
    return '运行中'
  }
  if (event.type === 'tool_completed') {
    return normalizeEvidenceDisplayItems(event)[0]?.title || fallback
  }
  if (event.type === 'tool_started') {
    const toolName = String(event.payload?.tool_name ?? event.payload?.toolName ?? '')
    return toolName || event.title || fallback
  }
  if (event.type === 'tool_failed') {
    const toolName = String(event.payload?.tool_name ?? event.payload?.toolName ?? '')
    return toolName ? `${toolName} 失败` : event.title || fallback
  }
  if (event.type === 'export_created') {
    return '导出文件已生成'
  }
  if (event.type === 'export_failed') {
    return '导出失败'
  }
  return event.title || fallback
}

function evidenceMetaRows(evidence: AgentEvidenceDisplayItem) {
  return evidence.meta || []
}

function evidenceRecords(evidence: AgentEvidenceDisplayItem) {
  return (evidence.records || []).slice(0, 5)
}

function evidenceRecordColumns(evidence: AgentEvidenceDisplayItem) {
  const preferred = [
    'metric',
    'value',
    'status',
    'ave_temperature',
    'temperature_status',
    'ave_pressure',
    'pressure_status',
    'id',
    'title',
    'type',
    'priority',
    'pipe_segment_id',
    'segment_id',
    'segment_name',
  ]
  const keys = new Set<string>()
  evidenceRecords(evidence).forEach((record) => {
    Object.keys(record).forEach((key) => keys.add(key))
  })
  const sorted = [
    ...preferred.filter((key) => keys.has(key)),
    ...Array.from(keys).filter((key) => !preferred.includes(key)).sort(),
  ]
  return sorted.slice(0, 6)
}

function evidenceColumnLabel(column: string) {
  const labels: Record<string, string> = {
    metric: '指标',
    value: '值',
    status: '状态',
    ave_temperature: '均温',
    temperature_status: '温度状态',
    ave_pressure: '均压',
    pressure_status: '压力状态',
    ave_flow: '流量',
    flow_status: '流量状态',
    ave_vibration: '振动',
    vibration_status: '振动状态',
    id: '编号',
    title: '标题',
    type: '类型',
    priority: '优先级',
    pipe_segment_id: '管段',
    segment_id: '管段',
    segment_name: '管段名称',
    assignee: '负责人',
    responsible: '负责人',
  }
  return labels[column] || column
}

function evidenceCellValue(value: unknown) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }
  if (typeof value === 'string') {
    return value.length > 80 ? `${value.slice(0, 80)}...` : value
  }
  const text = JSON.stringify(value)
  return text.length > 80 ? `${text.slice(0, 80)}...` : text
}

function evidenceScoreLabel(score?: number) {
  if (typeof score !== 'number') {
    return ''
  }
  const normalized = score <= 1 ? score * 100 : score
  return `可信度 ${Math.round(normalized)}%`
}

function buildTimelineItems(
  turn: Extract<AgentConversationTurn, { kind: 'agent_run' }>,
  options: { includeLiveStream?: boolean; includeSummaryFallback?: boolean } = {},
) {
  const includeLiveStream = options.includeLiveStream ?? true
  const includeSummaryFallback = options.includeSummaryFallback ?? true
  const recommendation = normalizeRecommendation(
    turn.events.find((event) => event.type === 'recommendation_generated')?.payload,
  )
  const summaryRecommendation = recommendationFromSummary(turn.summary)
  const finalText = collectFinalText(turn.events, recommendation)
    || (includeLiveStream ? turn.streamState.finalText : '')
    || (includeSummaryFallback ? turn.summary?.finalAnswer : '')
    || (includeSummaryFallback ? turn.summary?.judgment : '')
  let displaySeq = 0
  const nextDisplaySeq = () => {
    displaySeq += 1
    return displaySeq
  }
  const pushDisplayEvent = (items: TimelineItem[], event: AgentEvent) => {
    if (isToolTimelineEvent(event.type)) {
      const latest = items[items.length - 1]
      const step = toNumber(event.payload?.step) || 0
      if (latest?.kind === 'tool_batch' && latest.step === step) {
        latest.events = [...latest.events, event]
        latest.title = toolBatchTitle(latest.events)
        latest.caption = toolBatchCaption(latest.events)
        return
      }
      const events = [event]
      items.push({
        kind: 'tool_batch',
        id: `${turn.id}_tool_batch_${event.seq}`,
        seq: nextDisplaySeq(),
        step,
        events,
        title: toolBatchTitle(events),
        caption: toolBatchCaption(events),
      })
      return
    }

    items.push({
      kind: 'event',
      event,
      displaySeq: nextDisplaySeq(),
      label: eventLabel(event.type),
      caption: eventCaption(event),
    })
  }

  if (turn.status === 'running') {
    const runningItems: TimelineItem[] = []
    turn.events.forEach((event) => {
      if (event.type === 'llm_thinking_completed') {
        const content = String(event.payload?.content ?? '')
        if (content) {
          runningItems.push({
            kind: 'thinking',
            id: `${turn.id}_thinking_${event.id}`,
            seq: nextDisplaySeq(),
            step: toNumber(event.payload?.step) || 0,
            content,
            running: false,
          })
        }
        return
      }

      if (!isDisplayableAgentEvent(event.type)) {
        return
      }

      pushDisplayEvent(runningItems, event)
    })
    if (includeLiveStream && turn.streamState.thinkingText) {
      runningItems.push({
        kind: 'thinking',
        id: `${turn.id}_thinking_stream`,
        seq: nextDisplaySeq(),
        step: turn.streamState.thinkingStep || 0,
        content: turn.streamState.thinkingText,
        running: true,
      })
    }
    const liveFinalText = includeLiveStream
      ? readableFinalAnswer(turn.streamState.finalText)
        || recommendation?.judgment
        || recommendation?.summary
        || ''
      : ''
    if (liveFinalText) {
      runningItems.push({
        kind: 'final',
        id: `${turn.id}_final_stream`,
        seq: nextDisplaySeq(),
        content: liveFinalText,
        recommendation: recommendation || summaryRecommendation,
        running: true,
      })
    }
    if (includeLiveStream && turn.streamState.finalAnswerPending && !liveFinalText) {
      runningItems.push({
        kind: 'final_pending',
        id: `${turn.id}_final_pending`,
        ...getFinalAnswerPendingNotice(),
      })
    }
    if (includeLiveStream && turn.streamState.finalizing) {
      runningItems.push({
        kind: 'finalizing',
        id: `${turn.id}_finalizing`,
        text: '正在整理最终结果',
        detail: '正在保存研判摘要、会话消息和记忆候选。',
      })
    }
    if (!runningItems.length) {
      runningItems.push({
        kind: 'startup',
        id: `${turn.id}_startup`,
        ...getAgentStartupNotice(),
      })
    }
    return runningItems
  }

  if (turn.collapsed) {
    return [
      {
        kind: 'summary' as const,
        id: `${turn.id}_summary`,
        text: turn.status === 'completed' ? 'Agent 已完成本轮研判' : `Agent ${statusLabel(turn.status)}`,
        stats: summarizeRun(turn.events),
      },
      {
        kind: 'final' as const,
        id: `${turn.id}_final_collapsed`,
        seq: nextDisplaySeq(),
        content: finalText || `${statusLabel(turn.status)}，展开可查看完整过程。`,
        recommendation: recommendation || summaryRecommendation,
        running: false,
      },
    ]
  }

  const items: TimelineItem[] = []
  let thinkingText = ''
  let thinkingStep = 0
  let thinkingSeq = 0
  let finalBuffer = ''
  let finalSeq = 0

  const flushThinking = (running = false) => {
    if (!thinkingText) {
      return
    }
    items.push({
      kind: 'thinking',
      id: `${turn.id}_thinking_${thinkingSeq}_${items.length}`,
      seq: nextDisplaySeq(),
      step: thinkingStep,
      content: thinkingText,
      running,
    })
    thinkingText = ''
    thinkingStep = 0
    thinkingSeq = 0
  }

  const flushFinal = (running = false) => {
    const content = finalBuffer || finalText
    if (!content) {
      return
    }
    items.push({
      kind: 'final',
      id: `${turn.id}_final_${finalSeq || items.length}`,
      seq: nextDisplaySeq(),
      content,
      recommendation: recommendation || summaryRecommendation,
      running,
    })
    finalBuffer = ''
    finalSeq = 0
  }

  turn.events.forEach((event) => {
    if (event.type === 'llm_thinking_delta') {
      thinkingStep = toNumber(event.payload?.step) || thinkingStep
      thinkingSeq = thinkingSeq || event.seq
      thinkingText += String(event.payload?.delta ?? '')
      return
    }

    if (event.type === 'llm_thinking_completed') {
      thinkingText = String(event.payload?.content ?? thinkingText)
      thinkingStep = toNumber(event.payload?.step) || thinkingStep
      thinkingSeq = thinkingSeq || event.seq
      flushThinking(false)
      return
    }

    if (event.type === 'final_answer_delta') {
      finalSeq = finalSeq || event.seq
      finalBuffer += String(event.payload?.delta ?? '')
      return
    }

    if (event.type === 'final_answer_completed') {
      flushFinal(false)
      return
    }

    flushThinking(false)
    if (event.type === 'recommendation_generated') {
      flushFinal(false)
    }

    if (isDisplayableAgentEvent(event.type)) {
      pushDisplayEvent(items, event)
    }
  })

  flushThinking(false)
  flushFinal(false)
  return items
}

function recommendationFromSummary(summary?: AgentRunSummary): AgentRecommendation | undefined {
  if (!summary) {
    return undefined
  }
  return {
    summary: summary.finalAnswer || summary.judgment || '',
    riskLevel: summary.riskLevel,
    judgment: summary.judgment || summary.finalAnswer || '',
    recommendedActions: summary.recommendedActions || [],
    missingInformation: summary.openQuestions || [],
    humanConfirmationRequired: false,
  }
}

function collectFinalText(events: AgentEvent[], recommendation?: AgentRecommendation) {
  const streamed = events
    .filter((event) => event.type === 'final_answer_delta')
    .map((event) => String(event.payload?.delta ?? ''))
    .join('')
    .trim()

  if (streamed && !looksLikeJson(streamed)) {
    return streamed
  }

  if (recommendation?.judgment || recommendation?.summary) {
    return recommendation.judgment || recommendation.summary
  }

  return streamed
}

function readableFinalAnswer(value?: string) {
  if (!value || looksLikeJson(value)) {
    return ''
  }
  return value
}

function thinkingPreview(content: string) {
  const normalized = content.trim()
  return normalized.length > 180 ? `${normalized.slice(0, 180)}...` : normalized
}

function isThinkingExpanded(id: string, running: boolean) {
  return running || thinkingExpansion.value[id] === true
}

function isThinkingCollapsible(content: string, running: boolean) {
  return !running && content.trim().length > 180
}

function toggleThinking(id: string) {
  thinkingExpansion.value = {
    ...thinkingExpansion.value,
    [id]: thinkingExpansion.value[id] !== true,
  }
}

function setThinkingTextRef(element: unknown, item: Extract<TimelineItem, { kind: 'thinking' }>) {
  if (!element || !(element instanceof HTMLElement)) {
    return
  }
  const textElement = element as HTMLElement & {
    __thinkingScrollBound?: boolean
    __thinkingProgrammaticScroll?: boolean
  }
  if (!textElement.__thinkingScrollBound) {
    textElement.__thinkingScrollBound = true
    textElement.addEventListener('scroll', () => {
      if (textElement.__thinkingProgrammaticScroll) {
        return
      }
      const shouldFollow = isElementNearBottom(textElement)
      thinkingAutoFollow.value = {
        ...thinkingAutoFollow.value,
        [item.id]: shouldFollow,
      }
    }, { passive: true })
  }
  if (item.running && thinkingAutoFollow.value[item.id] !== false) {
    textElement.__thinkingProgrammaticScroll = true
    textElement.scrollTop = textElement.scrollHeight
    window.requestAnimationFrame(() => {
      textElement.__thinkingProgrammaticScroll = false
    })
  }
}

function isElementNearBottom(element: HTMLElement, tolerance = 12) {
  return element.scrollHeight - element.scrollTop - element.clientHeight <= tolerance
}

function isToolTimelineEvent(type: string) {
  return ['tool_started', 'tool_progress', 'tool_completed', 'tool_failed'].includes(type)
}

function toolBatchTitle(events: AgentEvent[]) {
  const completed = events.filter((event) => event.type === 'tool_completed').length
  const failed = events.filter((event) => event.type === 'tool_failed').length
  const running = events.filter((event) => event.type === 'tool_started').length - completed - failed
  const parts = [
    completed ? `${completed} 个完成` : '',
    failed ? `${failed} 个失败` : '',
    running > 0 ? `${running} 个执行中` : '',
  ].filter(Boolean)
  return parts.length ? `本轮工具调用：${parts.join('，')}` : '本轮工具调用'
}

function toolBatchCaption(events: AgentEvent[]) {
  const names = Array.from(new Set(events
    .map((event) => String(event.payload?.tool_name ?? event.payload?.toolName ?? ''))
    .filter(Boolean)))
  return names.length ? names.join('、') : '正在查询业务数据'
}

function looksLikeJson(value: string) {
  const trimmed = value.trim()
  return trimmed.startsWith('{') || trimmed.startsWith('[')
}

function summarizeRun(events: AgentEvent[]) {
  const thinking = events.filter((event) => event.type === 'llm_thinking_completed').length
  const tool = events.filter((event) => event.type === 'tool_completed').length
  const search = events.filter((event) =>
    ['knowledge_search_completed', 'retrieval_completed'].includes(event.type),
  ).length
  return `共 ${events.length} 步 · 思考 ${thinking} 次 · 工具 ${tool} 次 · 检索 ${search} 次`
}

function toNumber(value: unknown) {
  return typeof value === 'number' ? value : undefined
}

function getCurrentRunTurn() {
  for (let index = turns.value.length - 1; index >= 0; index -= 1) {
    const turn = turns.value[index]
    if (turn.kind === 'agent_run') {
      return turn
    }
  }
  return undefined
}

function updateCurrentRun(event: AgentEvent) {
  const currentRun = getCurrentRunTurn()
  if (!currentRun) {
    return
  }
  currentRun.runId = event.runId || currentRun.runId
  rememberNoticeFromEvent(event)
  applyStreamEvent(currentRun, event)
}

function processStreamEvent(event: AgentEvent) {
  activeRunId.value = event.runId || activeRunId.value
  if (event.type === 'export_created') {
    exportMessage.value = exportMessageFromEvent(event) ?? null
    pendingExportPlan.value = null
  }
  updateCurrentRun(event)
  void nextTick(() => {
    if (autoFollowConversation.value) {
      scrollConversationToBottom()
    }
  })
}

function enqueueStreamEvent(event: AgentEvent) {
  if (!shouldQueueAgentEventForPacing(event.type)) {
    processStreamEvent(event)
    return
  }
  pendingStreamEvents.value = [...pendingStreamEvents.value, event]
  scheduleNextStreamEvent()
}

function scheduleNextStreamEvent() {
  if (streamEventTimer.value !== undefined || !pendingStreamEvents.value.length) {
    return
  }
  const delay = getAgentEventRenderDelay(
    lastStreamBlockRenderedAt.value,
    Date.now(),
    streamBlockRenderIntervalMs,
  )
  streamEventTimer.value = window.setTimeout(() => {
    streamEventTimer.value = undefined
    flushNextStreamEvent()
  }, delay)
}

function flushNextStreamEvent() {
  const [event, ...rest] = pendingStreamEvents.value
  if (!event) {
    return
  }
  pendingStreamEvents.value = rest
  processStreamEvent(event)
  lastStreamBlockRenderedAt.value = Date.now()
  scheduleNextStreamEvent()
}

async function drainPendingStreamEvents() {
  if (streamEventTimer.value !== undefined) {
    window.clearTimeout(streamEventTimer.value)
    streamEventTimer.value = undefined
  }
  while (pendingStreamEvents.value.length) {
    flushNextStreamEvent()
    if (streamEventTimer.value !== undefined) {
      window.clearTimeout(streamEventTimer.value)
      streamEventTimer.value = undefined
    }
    if (pendingStreamEvents.value.length) {
      await wait(streamBlockRenderIntervalMs)
    }
  }
}

function resetStreamEventQueue() {
  pendingStreamEvents.value = []
  lastStreamBlockRenderedAt.value = 0
  if (streamEventTimer.value !== undefined) {
    window.clearTimeout(streamEventTimer.value)
    streamEventTimer.value = undefined
  }
}

function wait(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

function applyStreamEvent(turn: Extract<AgentConversationTurn, { kind: 'agent_run' }>, event: AgentEvent) {
  if (event.type === 'llm_thinking_delta') {
    turn.streamState.thinkingSeq = turn.streamState.thinkingSeq || event.seq
    turn.streamState.thinkingStep = toNumber(event.payload?.step) || turn.streamState.thinkingStep
    turn.streamState.thinkingText += String(event.payload?.delta ?? '')
    return
  }

  if (event.type === 'llm_thinking_completed') {
    turn.events = [
      ...turn.events,
      {
        ...event,
        payload: {
          ...event.payload,
          content: String(event.payload?.content ?? turn.streamState.thinkingText),
          step: toNumber(event.payload?.step) || turn.streamState.thinkingStep,
        },
      },
    ]
    turn.streamState.thinkingText = ''
    turn.streamState.thinkingSeq = undefined
    turn.streamState.thinkingStep = undefined
    return
  }

  if (event.type === 'final_answer_delta') {
    turn.streamState.finalSeq = turn.streamState.finalSeq || event.seq
    turn.streamState.finalAnswerPending = false
    turn.streamState.finalText += String(event.payload?.delta ?? '')
    return
  }

  if (event.type === 'final_answer_started') {
    turn.streamState.finalAnswerPending = true
    return
  }

  if (event.type === 'final_answer_completed') {
    turn.streamState.finalAnswerPending = false
    const recommendation = normalizeRecommendation(event.payload)
    turn.summary = {
      ...(turn.summary || { recommendedActions: [] }),
      finalAnswer: readableFinalAnswer(turn.streamState.finalText)
        || recommendation?.judgment
        || recommendation?.summary
        || String(event.payload?.content ?? ''),
      riskLevel: recommendation?.riskLevel || turn.summary?.riskLevel,
      judgment: recommendation?.judgment || turn.summary?.judgment,
      recommendedActions: recommendation?.recommendedActions || turn.summary?.recommendedActions || [],
    }
    return
  }

  if (event.type === 'recommendation_generated') {
    turn.events = [...turn.events, event]
    return
  }

  if (event.type === 'run_completed') {
    turn.streamState.finalizing = true
  }

  if (!isDisplayableAgentEvent(event.type)) {
    return
  }

  turn.events = [...turn.events, event]
}

function setCurrentRunStatus(status: AgentRunStatus, collapsed: boolean) {
  const currentRun = getCurrentRunTurn()
  if (!currentRun) {
    return
  }
  currentRun.status = status
  currentRun.collapsed = collapsed
  currentRun.streamState.finalizing = false
}

async function toggleRun(turnId: string) {
  const turn = turns.value.find((item) => item.kind === 'agent_run' && item.id === turnId)
  if (!turn || turn.kind !== 'agent_run') {
    return
  }
  if (turn.collapsed && !turn.eventsLoaded && selectedSessionId.value && turn.runId) {
    const response = await loadAgentRunEvents(selectedSessionId.value, turn.runId)
    turn.events = response.events
    turn.eventsLoaded = true
  }
  turn.collapsed = !turn.collapsed
}

async function selectSession(session: AgentSession, options: { updateRoute?: boolean } = {}) {
  resetStreamEventQueue()
  selectedSessionId.value = session.id
  sessionMenuOpenId.value = null
  errorMessage.value = ''
  exportMessage.value = null
  pendingExportPlan.value = null
  memoryNotices.value = []
  recalledMemorySummary.value = ''
  const response = await loadAgentTimeline(session.id, { limit: 1 })
  turns.value = response.items.flatMap(timelineItemToTurns)
  beforeCursor.value = response.beforeCursor
  hasMoreBefore.value = response.hasMoreBefore
  activeRunId.value = turns.value.find((turn) => turn.kind === 'agent_run')?.runId || ''
  await nextTick()
  scrollConversationToBottom()
  if (options.updateRoute !== false && !session.id.startsWith('ana_demo')) {
    router.push(`/virtual-expert/${encodeURIComponent(session.id)}`)
  }
}

function startNewConversation() {
  resetStreamEventQueue()
  selectedSessionId.value = ''
  activeRunId.value = ''
  turns.value = []
  beforeCursor.value = undefined
  hasMoreBefore.value = false
  errorMessage.value = ''
  exportMessage.value = null
  pendingExportPlan.value = null
  memoryNotices.value = []
  recalledMemorySummary.value = ''
  sessionMenuOpenId.value = null
  router.push('/virtual-expert')
}

function getSpeechRecognitionConstructor() {
  const speechWindow = window as SpeechWindow
  return speechWindow.SpeechRecognition || speechWindow.webkitSpeechRecognition
}

function handleComposerKeydown(event: KeyboardEvent) {
  if (!isSubmitComposerKey({
    key: event.key,
    shiftKey: event.shiftKey,
    isComposing: event.isComposing,
  })) {
    return
  }
  event.preventDefault()
  void submitAnalysis()
}

function triggerImageUpload() {
  fileInput.value?.click()
}

function handleImageUpload(event: Event) {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files || []).filter((file) => file.type.startsWith('image/'))
  if (!files.length) {
    target.value = ''
    return
  }
  const nextAttachments = files.slice(0, 4 - imageAttachments.value.length).map((file) => ({
    id: `img_${Date.now()}_${Math.random().toString(16).slice(2)}`,
    name: file.name,
    type: file.type,
    size: file.size,
    previewUrl: URL.createObjectURL(file),
  }))
  imageAttachments.value = [...imageAttachments.value, ...nextAttachments]
  target.value = ''
}

function removeImageAttachment(attachment: AgentComposerAttachment) {
  URL.revokeObjectURL(attachment.previewUrl)
  imageAttachments.value = imageAttachments.value.filter((item) => item.id !== attachment.id)
}

function clearImageAttachments() {
  imageAttachments.value.forEach((item) => URL.revokeObjectURL(item.previewUrl))
  imageAttachments.value = []
}

function toggleVoiceInput() {
  if (isListening.value) {
    speechRecognition.value?.stop()
    return
  }

  const Recognition = getSpeechRecognitionConstructor()
  if (!Recognition) {
    errorMessage.value = '当前浏览器不支持语音输入。'
    return
  }

  const recognition = new Recognition()
  speechRecognition.value = recognition
  recognition.lang = 'zh-CN'
  recognition.interimResults = false
  recognition.continuous = false
  recognition.onresult = (event) => {
    const text = Array.from(event.results)
      .map((result) => result[0]?.transcript || '')
      .join('')
      .trim()
    if (text) {
      input.value = input.value ? `${input.value}${text}` : text
    }
  }
  recognition.onerror = () => {
    errorMessage.value = '语音输入未成功，请检查浏览器麦克风权限。'
  }
  recognition.onend = () => {
    isListening.value = false
  }

  try {
    isListening.value = true
    recognition.start()
  } catch {
    isListening.value = false
    errorMessage.value = '语音输入启动失败，请稍后重试。'
  }
}

async function deleteSession(session: AgentSession) {
  if (session.id.startsWith('ana_demo') || isRunning.value) {
    return
  }
  const confirmed = window.confirm(`确认删除「${session.title}」？`)
  if (!confirmed) {
    return
  }
  try {
    await requestAgentSessionDelete(session.id)
    const remaining = sessions.value.filter((item) => item.id !== session.id)
    sessions.value = remaining
    if (selectedSessionId.value === session.id) {
      if (remaining.length) {
        await selectSession(remaining[0])
      } else {
        startNewConversation()
      }
    }
  } catch (error) {
    errorMessage.value = readableAgentError(error)
  }
}

async function renameSession(session: AgentSession) {
  const title = window.prompt('重命名会话', session.title)?.trim()
  if (!title || title === session.title) {
    return
  }
  try {
    const updated = await requestAgentSessionUpdate(session.id, { title })
    applySessionUpdate(updated)
  } catch (error) {
    errorMessage.value = readableAgentError(error)
  }
}

async function togglePinSession(session: AgentSession) {
  try {
    const updated = await requestAgentSessionUpdate(session.id, { pinned: !session.pinned })
    applySessionUpdate(updated)
  } catch (error) {
    errorMessage.value = readableAgentError(error)
  }
}

async function archiveSession(session: AgentSession) {
  try {
    const updated = await requestAgentSessionUpdate(session.id, { archived: true })
    sessions.value = sessions.value.filter((item) => item.id !== session.id)
    archivedSessions.value = [updated, ...archivedSessions.value.filter((item) => item.id !== updated.id)]
    if (selectedSessionId.value === session.id) {
      startNewConversation()
    }
  } catch (error) {
    errorMessage.value = readableAgentError(error)
  }
}

async function restoreSession(session: AgentSession) {
  try {
    const updated = await requestAgentSessionUpdate(session.id, { archived: false })
    archivedSessions.value = archivedSessions.value.filter((item) => item.id !== session.id)
    sessions.value = [updated, ...sessions.value.filter((item) => item.id !== updated.id)]
  } catch (error) {
    errorMessage.value = readableAgentError(error)
  }
}

async function shareSession(session: AgentSession, type: 'link' | 'md') {
  try {
    const share = await requestAgentSessionShare(session.id, type)
    if (type === 'link') {
      const url = new URL(share.shareUrl, window.location.origin).toString()
      await navigator.clipboard?.writeText(url)
      shareNotice.value = '分享链接已生成'
    } else {
      const blob = new Blob([share.markdown || ''], { type: 'text/markdown;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      exportMessage.value = {
        text: `${share.title || session.title}.md 已生成`,
        url,
      }
      shareNotice.value = 'Markdown 分享已生成'
    }
    sessionMenuOpenId.value = null
  } catch (error) {
    errorMessage.value = readableAgentError(error)
  }
}

function applySessionUpdate(session: AgentSession) {
  sessions.value = sessions.value.map((item) => item.id === session.id ? { ...item, ...session } : item)
  archivedSessions.value = archivedSessions.value.map((item) => item.id === session.id ? { ...item, ...session } : item)
  sessionMenuOpenId.value = null
}

async function exportSelectedSession(format: AgentExportFormat, exportPlan?: AgentExportPlan) {
  if (!selectedSessionId.value || selectedSessionId.value.startsWith('ana_demo') || (isRunning.value && !exportPlan) || isExporting.value) {
    return
  }
  const effectivePlan = exportPlan || {
    format,
    scope: 'full_session' as const,
    title: format === 'excel' ? '虚拟专家研判明细' : '虚拟专家研判报告',
    includeEvidence: true,
    includeTimeline: true,
  }
  isExporting.value = true
  errorMessage.value = ''
  exportMessage.value = null
  try {
    const latestRun = [...turns.value].reverse().find((turn) => turn.kind === 'agent_run')
    const response = await requestAgentExport(
      selectedSessionId.value,
      format,
      latestRun?.kind === 'agent_run' ? latestRun.runId : undefined,
      effectivePlan,
    )
    exportMessage.value = {
      text: `${response.fileName} 已生成`,
      url: response.downloadUrl,
    }
    pendingExportPlan.value = null
  } catch (error) {
    errorMessage.value = readableAgentError(error)
  } finally {
    isExporting.value = false
  }
}

async function confirmPendingExport() {
  if (!pendingExportPlan.value) {
    return
  }
  await exportSelectedSession(pendingExportPlan.value.format, pendingExportPlan.value)
}

function retryFailedTurn(turnId: string) {
  const index = turns.value.findIndex((turn) => turn.id === turnId)
  const previousUserTurn = [...turns.value.slice(0, index)]
    .reverse()
    .find((turn) => turn.kind === 'user_message')
  if (previousUserTurn?.kind === 'user_message') {
    input.value = previousUserTurn.content
    errorMessage.value = ''
  }
}

async function submitAnalysis() {
  const textInput = input.value.trim()
  const rawInput = buildComposerMessageContent(textInput, imageAttachments.value)
  if (!rawInput || isRunning.value) {
    return
  }
  const sessionTitle = (textInput || imageAttachments.value[0]?.name || '图片研判').slice(0, 36)

  isRunning.value = true
  resetStreamEventQueue()
  autoFollowConversation.value = true
  errorMessage.value = ''
  exportMessage.value = null
  pendingExportPlan.value = null
  input.value = ''

  const userTurn: AgentConversationTurn = {
    kind: 'user_message',
    id: `msg_${Date.now()}`,
    content: rawInput,
    createdAt: new Date().toLocaleString(),
  }
  const runTurn: AgentConversationTurn = {
    kind: 'agent_run',
    id: `run_turn_${Date.now()}`,
    runId: '',
    status: 'running',
    events: [],
    eventsLoaded: true,
    streamState: createEmptyStreamState(),
    collapsed: false,
  }
  let sessionId = selectedSessionId.value
  const needsNewSession = !sessionId || sessionId.startsWith('ana_demo')
  if (needsNewSession) {
    selectedSessionId.value = ''
    activeRunId.value = ''
    turns.value = []
    beforeCursor.value = undefined
    hasMoreBefore.value = false
  }
  turns.value = [...turns.value, userTurn, runTurn]
  clearImageAttachments()
  activeRunId.value = ''
  await nextTick()
  scrollConversationToBottom()

  if (needsNewSession) {
    try {
      const session = await createAgentSession({ title: sessionTitle, sourceType: 'manual' })
      sessionId = session.sessionId
      const nextSession: AgentSession = {
        id: session.sessionId,
        title: sessionTitle,
        status: 'running',
        summary: '运行中 · 正在创建运行任务',
        updatedAt: new Date().toLocaleString(),
      }
      sessions.value = [nextSession, ...sessions.value.filter((item) => !item.id.startsWith('ana_demo'))]
      selectedSessionId.value = sessionId
    } catch (error) {
      errorMessage.value = readableAgentError(error)
      input.value = textInput
      setCurrentRunStatus('failed', false)
      isRunning.value = false
      return
    }
  }

  try {
    const { run, exportPlan } = await sendAgentMessageStream(
      sessionId,
      rawInput,
      enqueueStreamEvent,
      selectedModelTier.value,
    )
    await drainPendingStreamEvents()

    if (exportPlan && !run) {
      turns.value = turns.value.filter((turn) => turn.id !== runTurn.id)
      activeRunId.value = ''
      if (exportPlan.requiresConfirmation) {
        pendingExportPlan.value = exportPlan
      } else {
        await exportSelectedSession(exportPlan.format, exportPlan)
      }
      sessions.value = [
        {
          id: sessionId,
          title: sessionTitle,
          status: 'completed',
          summary: exportPlan.requiresConfirmation ? '等待确认导出计划' : '导出任务已生成',
          updatedAt: new Date().toLocaleString(),
        },
        ...sessions.value.filter((session) => session.id !== sessionId),
      ]
      return
    }

    if (!run) {
      turns.value = turns.value.filter((turn) => turn.id !== runTurn.id)
      activeRunId.value = ''
      sessions.value = [
        {
          id: sessionId,
          title: sessionTitle,
          status: 'completed',
          summary: '消息已保存',
          updatedAt: new Date().toLocaleString(),
        },
        ...sessions.value.filter((session) => session.id !== sessionId),
      ]
      selectedSessionId.value = sessionId
      return
    }

    const recommendation = normalizeRecommendation(
      run.events.find((event) => event.type === 'recommendation_generated')?.payload,
    )
    runTurn.summary = timelineSummaryFromEvents(run.events)
    const nextSession = {
      id: sessionId,
      title: sessionTitle,
      status: run.status,
      summary: recommendation?.summary || recommendation?.judgment || runTurn.summary?.judgment || readableFinalAnswer(runTurn.summary?.finalAnswer) || '已完成本轮研判',
      updatedAt: new Date().toLocaleString(),
    }
    sessions.value = [
      nextSession,
      ...sessions.value.filter((session) => session.id !== sessionId),
    ]
    selectedSessionId.value = sessionId
    setCurrentRunStatus(run.status, true)
    activeRunId.value = run.runId
  } catch (error) {
    errorMessage.value = readableAgentError(error)
    setCurrentRunStatus('failed', false)
  } finally {
    isRunning.value = false
  }
}

async function cancelRun() {
  if (!selectedSession.value || !activeRunId.value) {
    return
  }
  try {
    await requestAgentCancel(selectedSession.value.id, activeRunId.value)
    setCurrentRunStatus('cancelled', false)
    isRunning.value = false
  } catch (error) {
    errorMessage.value = readableAgentError(error)
  }
}

async function loadOlderTurns() {
  if (!selectedSessionId.value || !hasMoreBefore.value || isLoadingHistory.value) {
    return
  }
  const scroller = conversationScroll.value
  const oldScrollHeight = scroller?.scrollHeight || 0
  const oldScrollTop = scroller?.scrollTop || 0
  isLoadingHistory.value = true
  try {
    const response = await loadAgentTimeline(selectedSessionId.value, {
      beforeCursor: beforeCursor.value,
      limit: 5,
    })
    turns.value = [...response.items.flatMap(timelineItemToTurns), ...turns.value]
    beforeCursor.value = response.beforeCursor
    hasMoreBefore.value = response.hasMoreBefore
    await nextTick()
    if (scroller) {
      scroller.scrollTop = scroller.scrollHeight - oldScrollHeight + oldScrollTop
    }
  } finally {
    isLoadingHistory.value = false
  }
}

function handleConversationScroll() {
  const scroller = conversationScroll.value
  if (!scroller) {
    return
  }
  if (scroller.scrollTop < 24) {
    void loadOlderTurns()
  }
  if (programmaticScroll.value) {
    return
  }
  autoFollowConversation.value = isNearConversationBottom(scroller)
}

function scrollConversationToBottom() {
  const scroller = conversationScroll.value
  if (scroller) {
    programmaticScroll.value = true
    scroller.scrollTop = scroller.scrollHeight
    window.requestAnimationFrame(() => {
      programmaticScroll.value = false
    })
  }
}
</script>

<template>
  <ModuleShell active-path="/virtual-expert" eyebrow="Virtual Expert" title="虚拟专家 Agent 工作台" ops-label="Agent" ops-value="ReAct stream" expert-mode>
    <section class="agent-layout" :class="{ 'is-sidebar-hidden': sidebarHidden }">
      <button
        v-if="sidebarHidden"
        class="sidebar-reveal"
        type="button"
        title="显示会话列表"
        aria-label="展开会话列表"
        @click="sidebarHidden = false"
      >
        显示会话
      </button>

      <aside
        v-if="!sidebarHidden"
        class="agent-sidebar table-panel"
        :class="{ 'is-collapsed': sidebarCollapsed }"
        aria-label="Agent sessions"
      >
        <div class="session-toolbar">
          <button
            type="button"
            class="toolbar-btn"
            :title="sidebarCollapsed ? '展开列表' : '收起列表'"
            @click="sidebarCollapsed = !sidebarCollapsed"
          >
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <rect x="2" y="2" width="12" height="12" rx="0"/>
              <line x1="6" y1="2" x2="6" y2="14"/>
            </svg>
            <span>{{ sidebarCollapsed ? '展开列表' : '收起列表' }}</span>
          </button>
          <button
            type="button"
            class="toolbar-btn"
            title="开启新对话"
            :disabled="isRunning"
            @click="startNewConversation"
          >
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <line x1="8" y1="3" x2="8" y2="13"/>
              <line x1="3" y1="8" x2="13" y2="8"/>
            </svg>
            <span>新对话</span>
          </button>
          <button
            type="button"
            class="toolbar-btn"
            :class="{ 'is-active': searchExpanded }"
            title="搜索历史记录"
            @click="searchExpanded = !searchExpanded"
          >
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="7" cy="7" r="4"/>
              <line x1="10" y1="10" x2="14" y2="14"/>
            </svg>
            <span>搜索</span>
          </button>
          <button
            type="button"
            class="toolbar-btn"
            title="归档聊天"
            @click="archivedOpen = !archivedOpen"
          >
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <rect x="2" y="3" width="12" height="3" rx="0"/>
              <path d="M3 6v7a1 1 0 001 1h8a1 1 0 001-1V6"/>
              <line x1="6" y1="9" x2="10" y2="9"/>
            </svg>
            <span>归档</span>
          </button>
          <button
            type="button"
            class="toolbar-btn"
            title="我的偏好"
            @click="memoryPanelOpen = true"
          >
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <line x1="4" y1="5" x2="12" y2="5"/>
              <line x1="4" y1="8" x2="12" y2="8"/>
              <line x1="4" y1="11" x2="12" y2="11"/>
              <circle cx="6" cy="5" r="1.5" fill="currentColor" stroke="none"/>
              <circle cx="10" cy="8" r="1.5" fill="currentColor" stroke="none"/>
            </svg>
            <span>偏好</span>
          </button>
        </div>

        <template v-if="!sidebarCollapsed">
          <div v-if="searchExpanded" class="session-search">
            <input
              v-model="sessionSearch"
              type="search"
              placeholder="搜索历史记录"
              aria-label="搜索历史记录"
              @keydown.esc="searchExpanded = false"
            >
          </div>

          <div class="session-list">
          <article
            v-for="session in filteredSessions"
            :key="session.id"
            class="session-item"
            :class="{ 'is-active': session.id === selectedSessionId }"
          >
            <button class="session-open" @click="selectSession(session)">
              <span v-if="session.pinned" class="session-pin">置顶</span>
              <strong class="session-title">{{ session.title }}</strong>
            </button>
            <button
              class="session-more"
              type="button"
              :aria-label="`打开会话菜单 ${session.title}`"
              @click.stop="sessionMenuOpenId = sessionMenuOpenId === session.id ? null : session.id"
            >
              更多
            </button>
            <div v-if="sessionMenuOpenId === session.id" class="session-menu">
              <button type="button" @click="renameSession(session)">重命名</button>
              <button type="button" @click="togglePinSession(session)">{{ session.pinned ? '取消置顶' : '置顶聊天' }}</button>
              <button type="button" @click="archiveSession(session)">归档</button>
              <button type="button" @click="shareSession(session, 'link')">分享链接</button>
              <button type="button" @click="shareSession(session, 'md')">分享 Markdown</button>
              <button type="button" :disabled="isRunning || session.id.startsWith('ana_demo')" @click="deleteSession(session)">删除</button>
            </div>
          </article>

          <section class="archive-section">
            <button class="archive-toggle" type="button" @click="archivedOpen = !archivedOpen">
              <span>归档聊天</span>
              <strong>{{ archivedSessions.length }}</strong>
            </button>
            <div v-if="archivedOpen" class="archive-list">
              <article
                v-for="session in filteredArchivedSessions"
                :key="session.id"
                class="session-item is-archived"
              >
                <button class="session-open" @click="selectSession(session)">
                  <strong class="session-title">{{ session.title }}</strong>
                </button>
                <button class="session-more" type="button" @click.stop="restoreSession(session)">恢复</button>
              </article>
            </div>
          </section>
        </div>
        </template>
      </aside>

      <section class="agent-main table-panel">
        <div ref="conversationScroll" class="conversation-scroll" @scroll="handleConversationScroll">
          <button
            v-if="hasMoreBefore"
            class="history-loader"
            :disabled="isLoadingHistory"
            @click="loadOlderTurns"
          >
            {{ isLoadingHistory ? '加载中' : '加载更早对话' }}
          </button>

          <section v-if="recalledMemorySummary" class="memory-banner">
            <span class="chip">记忆召回</span>
            <p>本轮已参考你的偏好：{{ recalledMemorySummary }}</p>
          </section>

          <section v-if="memoryNotices.length" class="memory-notices">
            <article
              v-for="notice in memoryNotices"
              :key="notice.id"
              class="memory-notice"
              :class="{ 'is-pending': notice.status === 'pending' || notice.riskLevel === 'high' }"
            >
              <div>
                <span class="chip">{{ memoryTypeLabel(notice.memoryType) }}</span>
                <strong>{{ notice.riskLevel === 'high' ? '这个偏好需要确认' : '已记住偏好' }}</strong>
                <p>{{ notice.content }}</p>
                <small v-if="notice.reason">{{ notice.reason }}</small>
              </div>
              <div v-if="notice.status === 'pending' || notice.riskLevel === 'high'" class="memory-actions">
                <button class="ghost-action" type="button" @click="acceptMemory(notice)">记住</button>
                <button class="ghost-action" type="button" @click="rejectMemory(notice)">忽略</button>
              </div>
            </article>
          </section>

          <template v-for="turn in turns" :key="turn.id">
            <article v-if="turn.kind === 'user_message'" class="user-message">
              <div class="message-meta">
                <span class="chip">USER</span>
                <small>{{ turn.createdAt }}</small>
              </div>
              <p>{{ turn.content }}</p>
            </article>

            <article v-else class="agent-run" :class="{ 'is-running': turn.status === 'running' }">
              <div class="run-head">
                <div>
                  <span class="eyebrow">AGENT RUN</span>
                  <h3>研判过程</h3>
                </div>
                <div class="run-head__actions">
                  <span class="chip" :class="statusClass(turn.status)">{{ statusLabel(turn.status) }}</span>
                  <button
                    v-if="turn.status !== 'running'"
                    class="ghost-action"
                    @click="toggleRun(turn.id)"
                  >
                    {{ turn.collapsed ? '展开过程' : '折叠过程' }}
                  </button>
                  <button
                    v-if="turn.status === 'failed'"
                    class="ghost-action"
                    :disabled="isRunning"
                    @click="retryFailedTurn(turn.id)"
                  >
                    重试本轮
                  </button>
                </div>
              </div>

              <Transition name="run-collapse" mode="out-in">
                <div v-if="turn.collapsed" class="timeline timeline--collapsed">
                  <article
                    v-for="item in buildTimelineItems(turn)"
                    :key="item.kind === 'event' ? item.event.id : item.id"
                    class="timeline-card"
                    :class="`is-${item.kind}`"
                  >
                    <template v-if="item.kind === 'summary'">
                      <div class="timeline-rail"><span>OK</span></div>
                      <div class="timeline-body">
                        <h3>{{ item.text }}</h3>
                        <p>{{ item.stats }}</p>
                      </div>
                    </template>

                    <template v-else-if="item.kind === 'final'">
                      <div class="timeline-rail"><span>{{ item.seq || 'AI' }}</span></div>
                      <div class="timeline-body">
                        <div class="event-head">
                          <span class="chip" :class="riskClass(item.recommendation?.riskLevel)">最终回答</span>
                          <small>{{ riskLabel(item.recommendation?.riskLevel) }}</small>
                        </div>
                        <div class="markdown-answer" v-html="renderMarkdownToHtml(item.content)"></div>
                        <span v-if="item.running" class="cursor">|</span>
                        <ol v-if="item.recommendation?.recommendedActions.length" class="action-list">
                          <li v-for="action in item.recommendation.recommendedActions" :key="action">{{ action }}</li>
                        </ol>
                      </div>
                    </template>
                  </article>
                </div>

                <div v-else class="timeline timeline--flow">
                  <article
                    v-for="item in buildTimelineItems(turn)"
                    :key="item.kind === 'event' ? item.event.id : item.id"
                    class="timeline-card"
                    :class="`is-${item.kind}`"
                  >
                    <template v-if="item.kind === 'startup'">
                      <div class="timeline-rail"><span></span></div>
                      <div class="timeline-body">
                        <div class="event-head">
                          <span class="chip is-warn">{{ item.title }}</span>
                          <small>启动准备</small>
                        </div>
                        <h3>正在连接后端 Agent</h3>
                        <ul class="startup-steps">
                          <li v-for="step in item.steps" :key="step">{{ step }}</li>
                        </ul>
                        <p>{{ item.reason }}<span class="cursor">|</span></p>
                      </div>
                    </template>

	                    <template v-else-if="item.kind === 'thinking'">
	                      <div class="timeline-rail"><span>{{ item.seq }}</span></div>
	                      <div class="timeline-body">
	                        <div class="event-head">
	                          <span class="chip">LLM 思考</span>
	                          <small>第 {{ item.step || '-' }} 轮</small>
	                        </div>
	                        <p
                            :ref="(element) => setThinkingTextRef(element, item)"
                            class="stream-text"
                            :class="{ 'is-preview': !isThinkingExpanded(item.id, item.running) }"
                          >
	                          {{ isThinkingExpanded(item.id, item.running) ? item.content : thinkingPreview(item.content) }}<span v-if="item.running" class="cursor">|</span>
	                        </p>
	                        <button
	                          v-if="isThinkingCollapsible(item.content, item.running)"
	                          class="inline-toggle"
	                          type="button"
	                          @click="toggleThinking(item.id)"
	                        >
	                          {{ isThinkingExpanded(item.id, item.running) ? '收起思考' : '展开思考' }}
	                        </button>
	                      </div>
	                    </template>

	                    <template v-else-if="item.kind === 'final_pending'">
	                      <div class="timeline-rail"><span></span></div>
	                      <div class="timeline-body">
	                        <div class="event-head">
	                          <span class="chip is-warn">{{ item.title }}</span>
	                          <small>等待首段回答</small>
	                        </div>
	                        <p>{{ item.detail }}<span class="cursor">|</span></p>
	                      </div>
	                    </template>

                    <template v-else-if="item.kind === 'final'">
                      <div class="timeline-rail"><span>{{ item.seq || 'AI' }}</span></div>
                      <div class="timeline-body">
                        <div class="event-head">
                          <span class="chip" :class="riskClass(item.recommendation?.riskLevel)">最终回答</span>
                          <small>{{ riskLabel(item.recommendation?.riskLevel) }}</small>
                        </div>
                        <div class="markdown-answer" v-html="renderMarkdownToHtml(item.content)"></div>
                        <span v-if="item.running" class="cursor">|</span>
                        <ol v-if="item.recommendation?.recommendedActions.length" class="action-list">
                          <li v-for="action in item.recommendation.recommendedActions" :key="action">{{ action }}</li>
                        </ol>
                      </div>
                    </template>

                    <template v-else-if="item.kind === 'finalizing'">
                      <div class="timeline-rail"><span></span></div>
                      <div class="timeline-body">
                        <div class="event-head">
                          <span class="chip is-warn">收尾中</span>
                          <small>正在归档</small>
                        </div>
                        <h3>{{ item.text }}<span class="cursor">|</span></h3>
                        <p>{{ item.detail }}</p>
                      </div>
                    </template>

                    <template v-else-if="item.kind === 'tool_batch'">
                      <div class="timeline-rail"><span>{{ item.seq }}</span></div>
                      <div class="timeline-body tool-batch">
                        <div class="event-head">
                          <span class="chip">工具调用</span>
                          <small>{{ item.step ? `第 ${item.step} 轮` : '当前轮' }}</small>
                        </div>
                        <h3>{{ item.title }}</h3>
                        <p>{{ item.caption }}</p>
                        <div class="tool-batch__items">
                          <article
                            v-for="event in item.events.filter((entry) => entry.type !== 'tool_started' || !item.events.some((next) => next.type !== 'tool_started' && next.payload?.tool_name === entry.payload?.tool_name))"
                            :key="event.id"
                            class="tool-batch__item"
                            :class="{ 'is-error': event.type === 'tool_failed' }"
                          >
                            <strong>{{ eventDisplayTitle(event, eventLabel(event.type)) }}</strong>
                            <span>{{ eventCaption(event) }}</span>
                          </article>
                        </div>
                        <div
                          v-if="item.events.flatMap((event) => normalizeEvidenceDisplayItems(event)).length"
                          class="evidence-stack"
                        >
                          <article
                            v-for="evidence in item.events.flatMap((event) => normalizeEvidenceDisplayItems(event))"
                            :key="evidence.id"
                            class="evidence-card"
                            :class="`is-${evidence.kind}`"
                          >
                            <div class="evidence-card__head">
                              <div>
                                <strong>{{ evidence.title }}</strong>
                                <small>{{ evidence.sourceLabel }}</small>
                              </div>
                              <span v-if="evidenceScoreLabel(evidence.score)" class="evidence-score">
                                {{ evidenceScoreLabel(evidence.score) }}
                              </span>
                            </div>
                            <p>{{ evidence.summary }}</p>
                            <dl v-if="evidenceMetaRows(evidence).length" class="evidence-meta">
                              <div v-for="row in evidenceMetaRows(evidence)" :key="`${evidence.id}-${row.label}-${row.value}`">
                                <dt>{{ row.label }}</dt>
                                <dd>{{ row.value }}</dd>
                              </div>
                            </dl>
                            <dl v-if="evidence.facts.length" class="evidence-facts">
                              <div v-for="fact in evidence.facts" :key="`${fact.label}_${fact.value}`">
                                <dt>{{ fact.label }}</dt>
                                <dd :class="fact.status ? `is-${fact.status}` : ''">{{ fact.value }}</dd>
                              </div>
                            </dl>
                            <div v-if="evidenceRecords(evidence).length" class="tool-records">
                              <div class="tool-records__head">
                                <strong>业务数据预览</strong>
                                <small>{{ evidence.records?.length || 0 }} 条</small>
                              </div>
                              <div class="tool-records__table">
                                <table>
                                  <thead>
                                    <tr>
                                      <th v-for="column in evidenceRecordColumns(evidence)" :key="column">
                                        {{ evidenceColumnLabel(column) }}
                                      </th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    <tr v-for="(record, index) in evidenceRecords(evidence)" :key="`${evidence.id}-record-${index}`">
                                      <td v-for="column in evidenceRecordColumns(evidence)" :key="column">
                                        {{ evidenceCellValue(record[column]) }}
                                      </td>
                                    </tr>
                                  </tbody>
                                </table>
                              </div>
                            </div>
                          </article>
                        </div>
                      </div>
                    </template>

                    <template v-else-if="item.kind === 'summary'">
                      <div class="timeline-rail"><span>OK</span></div>
                      <div class="timeline-body">
                        <h3>{{ item.text }}</h3>
                        <p>{{ item.stats }}</p>
                      </div>
                    </template>

                    <template v-else>
                      <div class="timeline-rail"><span>{{ item.displaySeq }}</span></div>
                      <div class="timeline-body">
                        <div class="event-head">
                          <span class="chip">{{ item.label }}</span>
                          <small>{{ item.event.createdAt || `SEQ ${item.event.seq}` }}</small>
                        </div>
	                        <h3 v-if="eventDisplayTitle(item.event, item.label)">{{ eventDisplayTitle(item.event, item.label) }}</h3>
                        <p>{{ item.caption }}</p>
                        <div v-if="normalizeEvidenceDisplayItems(item.event).length" class="evidence-stack">
                          <article
                            v-for="evidence in normalizeEvidenceDisplayItems(item.event)"
                            :key="evidence.id"
                            class="evidence-card"
                            :class="`is-${evidence.kind}`"
                          >
                            <div class="evidence-card__head">
                              <div>
                                <strong>{{ evidence.title }}</strong>
                                <small>{{ evidence.sourceLabel }}</small>
                              </div>
                              <span v-if="evidenceScoreLabel(evidence.score)" class="evidence-score">
                                {{ evidenceScoreLabel(evidence.score) }}
                              </span>
                            </div>
                            <p>{{ evidence.summary }}</p>
                            <dl v-if="evidenceMetaRows(evidence).length" class="evidence-meta">
                              <div v-for="row in evidenceMetaRows(evidence)" :key="`${evidence.id}-${row.label}-${row.value}`">
                                <dt>{{ row.label }}</dt>
                                <dd>{{ row.value }}</dd>
                              </div>
                            </dl>
                            <dl v-if="evidence.facts.length" class="evidence-facts">
                              <div v-for="fact in evidence.facts" :key="`${fact.label}_${fact.value}`">
                                <dt>{{ fact.label }}</dt>
                                <dd :class="fact.status ? `is-${fact.status}` : ''">{{ fact.value }}</dd>
                              </div>
                            </dl>
                            <div v-if="evidenceRecords(evidence).length" class="tool-records">
                              <div class="tool-records__head">
                                <strong>业务数据预览</strong>
                                <small>{{ evidence.records?.length || 0 }} 条</small>
                              </div>
                              <div class="tool-records__table">
                                <table>
                                  <thead>
                                    <tr>
                                      <th v-for="column in evidenceRecordColumns(evidence)" :key="column">
                                        {{ evidenceColumnLabel(column) }}
                                      </th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    <tr v-for="(record, index) in evidenceRecords(evidence)" :key="`${evidence.id}-record-${index}`">
                                      <td v-for="column in evidenceRecordColumns(evidence)" :key="column">
                                        {{ evidenceCellValue(record[column]) }}
                                      </td>
                                    </tr>
                                  </tbody>
                                </table>
                              </div>
                            </div>
                          </article>
                        </div>
                      </div>
                    </template>
                  </article>
                </div>
              </Transition>
            </article>
          </template>

          <p v-if="errorMessage" class="error-banner">{{ errorMessage }}</p>
          <p v-if="exportMessage" class="export-banner">
            {{ exportMessage.text }}
            <a :href="exportMessage.url" target="_blank" rel="noreferrer">下载</a>
          </p>
          <p v-if="shareNotice" class="export-banner">{{ shareNotice }}</p>
          <article v-if="pendingExportPlan" class="export-plan">
            <div>
              <strong>{{ pendingExportPlan.title || '导出计划' }}</strong>
              <p>{{ pendingExportPlan.reason || 'Agent 已生成结构化导出计划，确认后开始生成文件。' }}</p>
            </div>
            <dl>
              <div>
                <dt>格式</dt>
                <dd>{{ exportFormatLabel(pendingExportPlan.format) }}</dd>
              </div>
              <div>
                <dt>对象</dt>
                <dd>{{ pendingExportPlan.audience || '现场处置人员' }}</dd>
              </div>
              <div>
                <dt>用途</dt>
                <dd>{{ pendingExportPlan.purpose || '研判归档' }}</dd>
              </div>
              <div>
                <dt>范围</dt>
                <dd>{{ scopeLabel(pendingExportPlan.scope) }}</dd>
              </div>
              <div>
                <dt>版式</dt>
                <dd>{{ styleLabel(pendingExportPlan.style) }}</dd>
              </div>
              <div>
                <dt>详略</dt>
                <dd>{{ detailLabel(pendingExportPlan.detailLevel) }}</dd>
              </div>
            </dl>
            <div v-if="pendingExportPlan.sections?.length" class="export-plan__chips">
              <span v-for="section in pendingExportPlan.sections" :key="`section-${section.type}-${section.title}`" class="chip">
                {{ section.title }}
              </span>
            </div>
            <div v-if="pendingExportPlan.tables?.length" class="export-plan__chips">
              <span v-for="table in pendingExportPlan.tables" :key="`table-${table.type}-${table.title}`" class="chip is-good">
                {{ table.title }}
              </span>
            </div>
            <div class="export-plan__actions">
              <button class="control-button is-primary" :disabled="isExporting" @click="confirmPendingExport">
                {{ isExporting ? '导出中' : '确认导出' }}
              </button>
              <button class="ghost-action" :disabled="isExporting" @click="pendingExportPlan = null">取消</button>
            </div>
          </article>
        </div>

        <div class="composer-shell">
          <div class="composer">
            <div v-if="imageAttachments.length" class="attachment-list" aria-label="图片附件">
              <article v-for="attachment in imageAttachments" :key="attachment.id" class="attachment-chip">
                <img :src="attachment.previewUrl" :alt="attachment.name">
                <span>{{ attachment.name }}</span>
                <button type="button" aria-label="移除图片" @click="removeImageAttachment(attachment)">移除</button>
              </article>
            </div>
            <textarea
              v-model="input"
              aria-label="输入异常描述"
              :disabled="isRunning || isExporting"
              placeholder="输入异常现象、管段、设备或风险诉求，Enter 发送，Shift+Enter 换行"
              @keydown="handleComposerKeydown"
            />
            <div class="composer__tools">
              <button
                type="button"
                class="tool-icon-btn"
                :disabled="isRunning || isExporting || imageAttachments.length >= 4"
                title="上传图片"
                aria-label="上传图片"
                @click="triggerImageUpload"
              >
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="2" y="3" width="12" height="10" rx="0"/>
                  <circle cx="5.5" cy="6.5" r="1.5"/>
                  <path d="M14 11l-3-3-4 4-2-2-3 3"/>
                </svg>
              </button>
              <input
                ref="fileInput"
                class="composer-file-input"
                type="file"
                accept="image/*"
                multiple
                @change="handleImageUpload"
              >
              <button
                type="button"
                class="tool-icon-btn"
                :class="{ 'is-live': isListening }"
                :disabled="isRunning || isExporting || !speechSupported"
                :title="isListening ? '停止语音' : '语音输入'"
                :aria-label="isListening ? '停止语音' : '语音输入'"
                @click="toggleVoiceInput"
              >
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="5.5" y="2" width="5" height="8" rx="0"/>
                  <path d="M3 7v1a5 5 0 0010 0V7"/>
                  <line x1="8" y1="12" x2="8" y2="14"/>
                </svg>
              </button>
              <label class="model-select" :title="isRunning ? '将在下一次提问生效' : '选择本轮模型'">
                <span>{{ isRunning && activeRunModelLabel ? activeRunModelLabel : selectedModelLabel }}</span>
                <select v-model="selectedModelTier" aria-label="选择模型挡位">
                  <option v-for="option in modelTierOptions" :key="option.tier" :value="option.tier">
                    {{ option.label }} · {{ option.model }}
                  </option>
                </select>
              </label>
              <div class="composer-spacer"></div>
              <button
                v-if="isRunning"
                type="button"
                class="tool-icon-btn is-stop"
                :disabled="!activeRunId"
                title="停止运行"
                aria-label="停止运行"
                @click="cancelRun"
              >
                <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" stroke="none">
                  <rect x="3" y="3" width="10" height="10"/>
                </svg>
              </button>
              <button
                v-else
                type="button"
                class="tool-icon-btn is-send"
                :disabled="isRunning"
                title="发送"
                aria-label="发送"
                @click="submitAnalysis"
              >
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="8" y1="14" x2="8" y2="3"/>
                  <polyline points="4 7 8 3 12 7"/>
                </svg>
              </button>
            </div>
            <div class="composer__exports">
              <button
                v-for="item in quickExportFormats"
                :key="item.format"
                class="tool-icon-btn is-export"
                :disabled="isRunning || isExporting || !selectedSessionId || selectedSessionId.startsWith('ana_demo')"
                :title="item.label"
                :aria-label="item.label"
                @click="exportSelectedSession(item.format)"
              >
                {{ exportFormatLabel(item.format) }}
              </button>
            </div>
          </div>
        </div>
      </section>

      <aside v-if="memoryPanelOpen" class="memory-panel">
        <div class="memory-panel__head">
          <div>
            <span class="eyebrow">MEMORY</span>
            <h3>我的偏好</h3>
          </div>
          <button class="ghost-action" type="button" @click="memoryPanelOpen = false">关闭</button>
        </div>

        <section v-if="pendingMemories.length" class="memory-section">
          <h4>待确认</h4>
          <article v-for="item in pendingMemories" :key="item.id" class="memory-card is-pending">
            <span class="chip">{{ memoryTypeLabel(item.memoryType) }}</span>
            <p>{{ item.content }}</p>
            <small>{{ item.reason || '该偏好会影响后续行为，请确认是否记住。' }}</small>
            <div class="memory-actions">
              <button class="ghost-action" type="button" @click="acceptMemory(item)">记住</button>
              <button class="ghost-action" type="button" @click="rejectMemory(item)">忽略</button>
            </div>
          </article>
        </section>

        <section class="memory-section">
          <h4>已生效</h4>
          <article v-for="item in activeMemories" :key="item.id" class="memory-card">
            <span class="chip">{{ memoryTypeLabel(item.memoryType) }}</span>
            <p>{{ item.content }}</p>
            <button class="ghost-action" type="button" @click="removeMemory(item)">删除</button>
          </article>
          <p v-if="!activeMemories.length" class="memory-empty">还没有保存偏好。</p>
        </section>
      </aside>
    </section>
  </ModuleShell>
</template>

<style scoped>
.agent-layout {
  display: grid;
  grid-template-columns: 292px minmax(0, 1fr);
  gap: var(--space-3);
  block-size: 100%;
  min-block-size: 0;
  min-inline-size: 0;
  overflow: hidden;
}

.agent-layout.is-sidebar-hidden {
  grid-template-columns: minmax(0, 1fr);
}

.agent-sidebar,
.agent-main {
  display: grid;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.agent-sidebar {
  align-content: start;
}

.agent-sidebar.is-collapsed {
  inline-size: auto;
}

.agent-main {
  grid-template-rows: minmax(0, 1fr) auto;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.018), rgba(255, 255, 255, 0.006)),
    linear-gradient(180deg, rgba(110, 202, 212, 0.04), transparent 30%),
    var(--color-panel-2);
}

.session-list,
.conversation-scroll {
  min-height: 0;
  overflow: auto;
  scrollbar-width: none;
}

.session-list {
  display: grid;
  align-content: start;
  gap: 8px;
}

.session-toolbar {
  display: grid;
  gap: 2px;
  margin-bottom: 8px;
}

.toolbar-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  inline-size: 100%;
  block-size: 36px;
  padding: 0 10px;
  border: 1px solid var(--color-line);
  color: var(--color-text-muted);
  background: transparent;
  font-size: var(--text-meta);
  text-align: left;
  transition: background 80ms step-end, border-color 80ms step-end;
}

.toolbar-btn:hover {
  background: var(--color-panel-3);
  border-color: rgba(110, 202, 212, 0.2);
}

.toolbar-btn:active,
.toolbar-btn.is-active {
  background: rgba(110, 202, 212, 0.12);
}

.toolbar-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.toolbar-btn svg {
  flex: 0 0 16px;
  width: 16px;
  height: 16px;
}

.sidebar-reveal {
  display: inline-grid;
  place-items: center;
  inline-size: auto;
  min-inline-size: 42px;
  block-size: 30px;
  padding: 0 8px;
  border: 1px solid rgba(110, 202, 212, 0.2);
  color: var(--color-text-muted);
  background: rgba(5, 12, 18, 0.72);
  font-size: var(--text-meta);
}

.sidebar-reveal {
  position: absolute;
  inset-block-start: 14px;
  inset-inline-start: 14px;
  z-index: 5;
}

.session-search {
  margin-bottom: 8px;
}

.session-search input {
  inline-size: 100%;
  min-height: 32px;
  border: 1px solid var(--color-line);
  color: var(--color-text);
  background: var(--color-bg-elevated);
  padding: 0 9px;
  font-size: var(--text-meta);
  transition: border-color 80ms step-end, box-shadow 80ms step-end;
}

.session-search input:focus {
  border-color: var(--color-accent-cyan);
  box-shadow: 0 0 6px rgba(110, 202, 212, 0.25);
  outline: none;
}

.session-item {
  position: relative;
  display: flex;
  align-items: center;
  inline-size: 100%;
  border: 1px solid var(--color-line);
  color: var(--color-text);
  background: rgba(4, 9, 14, 0.8);
  transition: background 80ms step-end, border-color 80ms step-end;
}

.session-item:hover {
  background: var(--color-panel-3);
  border-color: rgba(110, 202, 212, 0.2);
}

.session-item.is-active {
  border-left: 2px solid var(--color-accent-orange);
  background: var(--color-panel-2);
  border-color: var(--color-line);
  border-left-color: var(--color-accent-orange);
}

.session-open {
  display: flex;
  align-items: center;
  gap: 6px;
  inline-size: 100%;
  min-width: 0;
  min-height: 32px;
  padding: 0 34px 0 9px;
  border: 0;
  color: inherit;
  background: transparent;
  text-align: left;
}

.session-more {
  position: absolute;
  inset-block-start: 4px;
  inset-inline-end: 4px;
  min-inline-size: 34px;
  block-size: 24px;
  padding: 0 6px;
  border: 1px solid transparent;
  color: var(--color-text-muted);
  background: transparent;
  font-size: var(--text-micro);
  opacity: 0;
  transition: opacity 80ms step-end, background 80ms step-end;
}

.session-item:hover .session-more {
  opacity: 1;
}

.session-more:hover {
  background: var(--color-panel-3);
  border-color: var(--color-line);
}

.session-menu {
  position: absolute;
  inset-block-start: 30px;
  inset-inline-end: 4px;
  z-index: 10;
  display: grid;
  gap: 8px;
  min-inline-size: 116px;
  padding: 6px;
  border: 1px solid var(--color-line);
  background: var(--color-panel-3);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
}

.session-menu button {
  min-height: 28px;
  padding: 0 8px;
  border: 0;
  color: var(--color-text-muted);
  background: transparent;
  text-align: left;
  font-size: var(--text-meta);
  transition: background 80ms step-end, color 80ms step-end;
}

.session-menu button:hover {
  color: var(--color-text);
  background: var(--color-panel-2);
}

.session-pin {
  color: var(--color-accent-cyan);
}

.archive-section {
  display: grid;
  gap: 8px;
  margin-top: 8px;
}

.archive-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 36px;
  padding: 0 10px;
  border: 1px solid var(--color-line);
  color: var(--color-text-muted);
  background: transparent;
  font-size: var(--text-meta);
  transition: background 80ms step-end, border-color 80ms step-end;
}

.archive-toggle:hover {
  background: var(--color-panel-3);
  border-color: rgba(110, 202, 212, 0.2);
}

.archive-list {
  display: grid;
  gap: 8px;
}

.session-item__head,
.session-item__meta,
.message-meta,
.event-head,
.run-head,
.run-head__actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.session-item__head,
.run-head {
  justify-content: space-between;
}

.session-item__meta,
.message-meta,
.event-head small {
  color: var(--color-text-dim);
  font-size: var(--text-micro);
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.session-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--text-meta);
  font-weight: 600;
}

.session-summary,
.timeline-card p,
.user-message p,
.error-banner,
.export-banner {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--text-meta);
  line-height: var(--leading-body);
}

.conversation-scroll {
  display: grid;
  align-content: start;
  gap: var(--space-3);
  padding-right: 2px;
}

.history-loader {
  justify-self: center;
  min-height: 30px;
  padding: 0 12px;
  border: 1px solid rgba(110, 202, 212, 0.18);
  color: var(--color-text-muted);
  background: rgba(255, 255, 255, 0.018);
  font-size: var(--text-micro);
}

.memory-banner,
.memory-notice,
.memory-card {
  border: 1px solid rgba(110, 202, 212, 0.24);
  background: rgba(8, 38, 46, 0.32);
  padding: 12px;
}

.memory-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.memory-banner p,
.memory-notice p,
.memory-card p,
.memory-empty {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--text-meta);
  line-height: var(--leading-body);
}

.memory-notices {
  display: grid;
  gap: 10px;
}

.memory-notice {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
}

.memory-notice > div:first-child {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.memory-notice strong {
  color: var(--color-text);
}

.memory-notice small,
.memory-card small {
  color: var(--color-text-muted);
}

.memory-notice.is-pending,
.memory-card.is-pending {
  border-color: rgba(213, 171, 76, 0.36);
  background: rgba(63, 45, 15, 0.42);
}

.memory-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.user-message,
.agent-run {
  display: grid;
  gap: var(--space-3);
  padding: var(--space-3);
  border: 1px solid var(--color-line);
  background: rgba(4, 8, 12, 0.72);
}

.user-message {
  justify-self: end;
  inline-size: min(760px, 88%);
  border-color: rgba(231, 104, 45, 0.24);
  background:
    linear-gradient(180deg, rgba(231, 104, 45, 0.08), rgba(255, 255, 255, 0.01)),
    rgba(8, 10, 13, 0.86);
}

.agent-run.is-running {
  border-color: rgba(110, 202, 212, 0.34);
}

.run-head h3,
.timeline-card h3 {
  margin: 0;
  font-size: var(--text-section);
  font-family: var(--font-serif);
}

.timeline {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.timeline--flow {
  position: relative;
}

.timeline--flow::before {
  content: "";
  position: absolute;
  inset-block: 14px 14px;
  inset-inline-start: 15px;
  width: 1px;
  background: linear-gradient(180deg, rgba(110, 202, 212, 0.34), rgba(96, 121, 139, 0.06));
}

.run-collapse-enter-active,
.run-collapse-leave-active {
  overflow: hidden;
  transition: opacity 180ms ease, transform 180ms ease, max-height 220ms ease;
}

.run-collapse-enter-from,
.run-collapse-leave-to {
  max-height: 0;
  opacity: 0;
  transform: translateY(-4px);
}

.run-collapse-enter-to,
.run-collapse-leave-from {
  max-height: 72vh;
  opacity: 1;
  transform: translateY(0);
}

.timeline-card {
  position: relative;
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  gap: var(--space-2);
  min-width: 0;
  padding: 8px 0;
  border: 0;
  background: transparent;
}

.timeline--collapsed .timeline-card {
  grid-template-columns: 44px minmax(0, 1fr);
  gap: var(--space-3);
  padding: var(--space-3);
  border: 1px solid rgba(96, 121, 139, 0.16);
  background: rgba(255, 255, 255, 0.014);
}

.timeline-card + .timeline-card {
  border-top: 1px solid rgba(96, 121, 139, 0.09);
}

.timeline--collapsed .timeline-card + .timeline-card {
  border-top: 1px solid rgba(96, 121, 139, 0.16);
}

.timeline-card.is-thinking .stream-text {
  max-block-size: min(42vh, 360px);
  overflow: auto;
  padding-inline-end: 6px;
  scrollbar-width: thin;
  scrollbar-color: rgba(110, 202, 212, 0.42) rgba(5, 12, 18, 0.62);
}

.timeline-card.is-thinking .stream-text::-webkit-scrollbar {
  width: 8px;
}

.timeline-card.is-thinking .stream-text::-webkit-scrollbar-track {
  background: rgba(5, 12, 18, 0.62);
}

.timeline-card.is-thinking .stream-text::-webkit-scrollbar-thumb {
  border: 2px solid rgba(5, 12, 18, 0.62);
  border-radius: 999px;
  background: rgba(110, 202, 212, 0.42);
}

.timeline-card.is-startup .timeline-body,
.timeline-card.is-final_pending .timeline-body {
  padding: 10px 12px;
  border: 1px solid rgba(213, 171, 76, 0.22);
  background: rgba(63, 45, 15, 0.18);
}

.timeline-card.is-final .timeline-body {
  padding-block-start: 2px;
}

.timeline--collapsed .timeline-card.is-final {
  border-color: rgba(231, 104, 45, 0.26);
}

.timeline--collapsed .timeline-card.is-summary {
  border-color: rgba(110, 202, 212, 0.24);
}

.timeline-rail {
  display: grid;
  justify-items: center;
  z-index: 1;
}

.timeline-rail span {
  display: grid;
  place-items: center;
  width: 18px;
  min-width: 18px;
  height: 18px;
  margin-top: 3px;
  border: 1px solid rgba(110, 202, 212, 0.28);
  border-radius: 999px;
  color: var(--color-accent-cyan);
  font-family: var(--font-mono);
  font-size: 0;
  background: rgba(5, 12, 18, 0.96);
}

.timeline--collapsed .timeline-rail span {
  width: 32px;
  min-width: 32px;
  height: 32px;
  margin-top: 0;
  border-radius: 0;
  font-size: var(--text-meta);
  background: rgba(255, 255, 255, 0.02);
}

.timeline-body {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.stream-text {
  white-space: pre-wrap;
}

.stream-text.is-preview {
  color: var(--color-text-muted);
}

.markdown-answer {
  display: grid;
  gap: 8px;
  min-width: 0;
  color: var(--color-text);
  font-size: var(--text-body-sm);
  line-height: 1.62;
}

.markdown-answer :deep(h2),
.markdown-answer :deep(h3),
.markdown-answer :deep(h4),
.markdown-answer :deep(h5) {
  margin: 6px 0 0;
  color: var(--color-text);
  font-size: var(--text-section);
  font-weight: 700;
  letter-spacing: 0;
}

.markdown-answer :deep(p) {
  margin: 0;
  color: var(--color-text);
  white-space: normal;
}

.markdown-answer :deep(ul),
.markdown-answer :deep(ol) {
  display: grid;
  gap: 6px;
  margin: 0;
  padding-inline-start: 20px;
}

.markdown-answer :deep(li) {
  padding-inline-start: 2px;
}

.markdown-answer :deep(code) {
  padding: 1px 5px;
  border: 1px solid rgba(110, 202, 212, 0.14);
  color: var(--color-accent-cyan);
  background: rgba(255, 255, 255, 0.035);
  font-family: var(--font-mono);
  font-size: var(--text-micro);
}

.markdown-answer :deep(pre) {
  max-block-size: min(42vh, 360px);
  margin: 0;
  padding: 10px 12px;
  overflow: auto;
  border: 1px solid rgba(96, 121, 139, 0.16);
  background: rgba(4, 8, 12, 0.7);
}

.markdown-answer :deep(pre code) {
  padding: 0;
  border: 0;
  color: var(--color-text-muted);
  background: transparent;
}

.markdown-answer :deep(.markdown-table-wrap) {
  max-width: 100%;
  overflow: auto;
  border: 1px solid rgba(96, 121, 139, 0.16);
  scrollbar-width: thin;
  scrollbar-color: rgba(110, 202, 212, 0.42) rgba(5, 12, 18, 0.62);
}

.markdown-answer :deep(table) {
  width: 100%;
  min-width: 420px;
  border-collapse: collapse;
  font-size: var(--text-micro);
}

.markdown-answer :deep(th),
.markdown-answer :deep(td) {
  padding: 8px 10px;
  border-bottom: 1px solid rgba(96, 121, 139, 0.12);
  text-align: left;
  vertical-align: top;
}

.markdown-answer :deep(th) {
  color: var(--color-text);
  background: rgba(255, 255, 255, 0.035);
  font-weight: 700;
}

.markdown-answer :deep(td) {
  color: var(--color-text-muted);
}

.inline-toggle {
  justify-self: start;
  min-height: 26px;
  padding: 0;
  border: 0;
  color: var(--color-accent-cyan);
  background: transparent;
  font-size: var(--text-micro);
}

.startup-steps {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.startup-steps li {
  padding: 4px 8px;
  border: 1px solid rgba(213, 171, 76, 0.18);
  color: var(--color-text-muted);
  background: rgba(255, 255, 255, 0.018);
  font-size: var(--text-micro);
}

.cursor {
  color: var(--color-accent-cyan);
}

.action-list {
  margin: 0;
  padding-left: 18px;
  color: var(--color-text);
  font-size: var(--text-body-sm);
  line-height: var(--leading-body);
}

.tool-batch {
  padding: 10px 12px;
  border: 1px solid rgba(110, 202, 212, 0.18);
  background: rgba(8, 38, 46, 0.16);
}

.tool-batch__items {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 8px;
  min-width: 0;
}

.tool-batch__item {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 8px;
  border: 1px solid rgba(96, 121, 139, 0.14);
  background: rgba(5, 12, 18, 0.5);
}

.tool-batch__item strong,
.tool-batch__item span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-batch__item strong {
  color: var(--color-text);
  font-size: var(--text-meta);
}

.tool-batch__item span {
  color: var(--color-text-muted);
  font-size: var(--text-micro);
}

.tool-batch__item.is-error {
  border-color: rgba(231, 78, 45, 0.28);
  background: rgba(231, 78, 45, 0.08);
}

.evidence-stack {
  display: grid;
  gap: var(--space-2);
  min-width: 0;
}

.evidence-card {
  display: grid;
  gap: var(--space-2);
  min-width: 0;
  padding: var(--space-3);
  border: 1px solid rgba(110, 202, 212, 0.16);
  background: rgba(5, 12, 18, 0.72);
}

.evidence-card__head,
.evidence-facts div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  min-width: 0;
}

.evidence-card__head > div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.evidence-card__head strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.evidence-score {
  flex: 0 0 auto;
  padding: 3px 7px;
  border: 1px solid rgba(83, 196, 167, 0.22);
  color: var(--color-success);
  background: rgba(83, 196, 167, 0.08);
  font-size: var(--text-micro);
  font-family: var(--font-mono);
}

.evidence-card__head small,
.evidence-facts dt,
.evidence-meta dt,
.tool-records__head small {
  color: var(--color-text-muted);
}

.evidence-card p {
  margin: 0;
  color: var(--color-text);
  line-height: var(--leading-body);
}

.evidence-facts {
  display: grid;
  gap: 6px;
  margin: 0;
}

.evidence-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(138px, 1fr));
  gap: 6px;
  margin: 0;
}

.evidence-meta div {
  display: grid;
  gap: 3px;
  min-width: 0;
  padding: 8px;
  border: 1px solid rgba(96, 121, 139, 0.14);
  background: rgba(255, 255, 255, 0.012);
}

.evidence-meta dd,
.evidence-facts dd {
  margin: 0;
  color: var(--color-text);
}

.evidence-meta dd {
  min-width: 0;
  overflow: hidden;
  font-family: var(--font-mono);
  font-size: var(--text-meta);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.evidence-facts dd {
  text-align: right;
}

.evidence-facts dd.is-warning {
  color: var(--color-warning);
}

.evidence-facts dd.is-critical {
  color: var(--color-danger);
}

.tool-records {
  display: grid;
  gap: var(--space-2);
  min-width: 0;
}

.tool-records__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.tool-records__head strong {
  font-size: var(--text-meta);
}

.tool-records__table {
  max-width: 100%;
  overflow: auto;
  border: 1px solid rgba(96, 121, 139, 0.14);
  background: rgba(2, 6, 10, 0.42);
}

.tool-records table {
  width: 100%;
  min-width: 520px;
  border-collapse: collapse;
  font-size: var(--text-micro);
}

.tool-records th,
.tool-records td {
  max-width: 220px;
  padding: 7px 9px;
  border-bottom: 1px solid rgba(96, 121, 139, 0.12);
  color: var(--color-text-muted);
  text-align: left;
  white-space: nowrap;
}

.tool-records th {
  color: var(--color-text);
  background: rgba(110, 202, 212, 0.05);
}

.tool-records td {
  overflow: hidden;
  text-overflow: ellipsis;
}

.composer-shell {
  padding-top: var(--space-2);
  border-top: 1px solid var(--color-line);
}

.composer {
  display: grid;
  gap: 8px;
  padding: 12px;
  background: var(--color-panel-2);
  border: 1px solid var(--color-line);
}

.attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.composer__tools {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.attachment-chip {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 26px;
  align-items: center;
  gap: 8px;
  max-width: min(280px, 100%);
  min-height: 42px;
  padding: 4px 6px 4px 4px;
  border: 1px solid var(--color-line);
  background: var(--color-bg-elevated);
}

.attachment-chip img {
  width: 34px;
  height: 34px;
  object-fit: cover;
  border: 1px solid var(--color-line);
}

.attachment-chip span {
  min-width: 0;
  overflow: hidden;
  color: var(--color-text-muted);
  font-size: var(--text-micro);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-chip button {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border: 1px solid rgba(231, 78, 45, 0.24);
  color: var(--color-danger);
  background: rgba(231, 78, 45, 0.08);
}

.composer textarea {
  min-height: 78px;
  max-height: 128px;
  resize: vertical;
  padding: 12px;
  border: 1px solid var(--color-line);
  color: var(--color-text);
  background: var(--color-bg-elevated);
  transition: border-color 80ms step-end, box-shadow 80ms step-end;
}

.composer textarea:focus {
  border-color: var(--color-accent-cyan);
  box-shadow: 0 0 6px rgba(110, 202, 212, 0.25);
  outline: none;
}

.composer textarea::placeholder {
  color: var(--color-text-dim);
}

.composer-file-input {
  display: none;
}

.model-select {
  position: relative;
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  max-width: 160px;
  border: 1px solid var(--color-line);
  color: var(--color-text-muted);
  background: var(--color-bg-elevated);
  padding: 0 10px;
  font-size: var(--text-micro);
}

.model-select span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-select select {
  position: absolute;
  inset: 0;
  inline-size: 100%;
  opacity: 0;
  cursor: pointer;
}

.composer-spacer {
  flex: 1;
}

.tool-icon-btn {
  display: inline-grid;
  place-items: center;
  min-width: 32px;
  height: 32px;
  padding: 0 8px;
  border: 1px solid var(--color-line);
  color: var(--color-text-muted);
  background: transparent;
  font-size: var(--text-micro);
  transition: background 80ms step-end, border-color 80ms step-end, color 80ms step-end;
}

.tool-icon-btn:hover {
  background: var(--color-panel-3);
  border-color: rgba(110, 202, 212, 0.2);
}

.tool-icon-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.tool-icon-btn svg {
  width: 16px;
  height: 16px;
}

.tool-icon-btn.is-send {
  background: var(--color-accent-orange);
  border-color: var(--color-accent-orange);
  color: var(--color-text);
}

.tool-icon-btn.is-send:hover {
  box-shadow: 0 0 8px rgba(231, 104, 45, 0.4);
}

.tool-icon-btn.is-stop {
  border-color: rgba(221, 176, 84, 0.34);
  color: var(--color-warning);
  background: rgba(221, 176, 84, 0.08);
  animation: pulse-stop 1.2s ease-in-out infinite;
}

@keyframes pulse-stop {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.8; }
}

.tool-icon-btn.is-live {
  border-color: rgba(34, 197, 94, 0.34);
  color: var(--color-text);
  background: rgba(34, 197, 94, 0.1);
}

.tool-icon-btn.is-export {
  border-color: rgba(231, 104, 45, 0.32);
  color: var(--color-text);
  background: rgba(231, 104, 45, 0.08);
}

.composer__exports {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.error-banner {
  padding: 12px;
  border: 1px solid rgba(231, 78, 45, 0.34);
  color: var(--color-danger);
  background: rgba(231, 78, 45, 0.08);
}

.export-banner {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid rgba(34, 197, 94, 0.3);
  color: var(--color-text);
  background: rgba(34, 197, 94, 0.08);
}

.export-banner a {
  color: var(--color-accent-cyan);
  font-weight: 700;
  text-decoration: none;
}

.export-plan {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid rgba(110, 202, 212, 0.28);
  background: rgba(110, 202, 212, 0.08);
}

.export-plan strong {
  display: block;
  margin-bottom: 4px;
  color: var(--color-text);
  font-size: var(--text-body);
}

.export-plan dl {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin: 0;
}

.export-plan dl div {
  min-width: 0;
}

.export-plan dt {
  color: var(--color-text-muted);
  font-size: var(--text-micro);
}

.export-plan dd {
  margin: 2px 0 0;
  color: var(--color-text);
  font-size: var(--text-meta);
}

.export-plan__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.export-plan__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.session-list::-webkit-scrollbar,
.conversation-scroll::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.memory-panel {
  position: fixed;
  inset-block: 72px 24px;
  inset-inline-end: 24px;
  z-index: 40;
  display: grid;
  align-content: start;
  gap: 18px;
  inline-size: min(420px, calc(100vw - 32px));
  padding: 16px;
  overflow: auto;
  border: 1px solid rgba(110, 202, 212, 0.28);
  background: rgba(5, 10, 15, 0.96);
  box-shadow: 0 24px 80px rgba(2, 6, 10, 0.52);
}

.memory-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.memory-panel__head h3,
.memory-section h4 {
  margin: 0;
  color: var(--color-text);
}

.memory-panel__head h3 {
  font-family: var(--font-serif);
  font-size: var(--text-section);
}

.memory-section {
  display: grid;
  gap: 12px;
  min-width: 0;
}

.memory-card {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.memory-card .ghost-action {
  justify-self: start;
}

@media (max-width: 1180px) {
  .agent-layout {
    grid-template-columns: 1fr;
    overflow: auto;
  }

  .agent-sidebar {
    max-height: 240px;
  }

  .user-message {
    inline-size: 100%;
  }
}

@media (max-width: 819px) {
  .composer__tools {
    flex-wrap: wrap;
  }

  .timeline-card {
    grid-template-columns: 36px minmax(0, 1fr);
    gap: var(--space-2);
  }

  .memory-banner,
  .memory-notice,
  .memory-panel__head {
    align-items: stretch;
  }

  .memory-banner,
  .memory-notice {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
