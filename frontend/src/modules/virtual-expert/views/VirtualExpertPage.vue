<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import ModuleShell from '../../../components/business/ModuleShell.vue'
import { createAgentSession } from '../api'
import type {
  AgentConversationTurn,
  AgentComposerAttachment,
  AgentEvent,
  AgentEvidenceDisplayItem,
  AgentExportFormat,
  AgentExportPlan,
  AgentMemoryItem,
  AgentReactRound,
  AgentRecommendation,
  AgentRunSummary,
  AgentRunStatus,
  AgentSession,
} from '../types'
import {
  buildComposerMessageContent,
  createEmptyStreamState,
  getFallbackAgentEvents,
  getFallbackAgentSessions,
  groupEventsIntoReactRounds,
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
  sendAgentMessageStream,
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
      kind: 'event'
      event: AgentEvent
      displaySeq: number
      label: string
      caption: string
      detail: string
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
const roundExpansionOverrides = ref<Record<string, Record<string, boolean>>>({})
const memoryPanelOpen = ref(false)
const activeMemories = ref<AgentMemoryItem[]>([])
const pendingMemories = ref<AgentMemoryItem[]>([])
const memoryNotices = ref<AgentMemoryItem[]>([])
const recalledMemorySummary = ref('')

const selectedSession = computed(() =>
  sessions.value.find((session) => session.id === selectedSessionId.value),
)
const speechSupported = computed(() => Boolean(getSpeechRecognitionConstructor()))

const samplePrompts = [
  '3 号管段压力波动并伴随传感器告警，先判断是否需要停输',
  '东区泵站流量突降，帮我判断故障范围和排查顺序',
  '今日巡检发现阀室温度异常升高，请给出风险和处置建议',
]

const quickExportFormats: Array<{ format: AgentExportFormat; label: string }> = [
  { format: 'pdf', label: '导出 PDF' },
  { format: 'excel', label: '导出 Excel' },
]

onMounted(() => {
  void loadInitialSessions()
  void refreshMemories()
})

onBeforeUnmount(() => {
  speechRecognition.value?.stop()
  imageAttachments.value.forEach((item) => URL.revokeObjectURL(item.previewUrl))
})

async function loadInitialSessions() {
  try {
    const loadedSessions = await loadAgentSessions(20)
    if (!loadedSessions.length) {
      return
    }
    sessions.value = loadedSessions
    await selectSession(loadedSessions[0])
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
  if (event.type === 'memory_recalled') {
    const items = Array.isArray(event.payload?.items) ? event.payload.items : []
    recalledMemorySummary.value = items
      .map((item) => String((item as Record<string, unknown>).content ?? ''))
      .filter(Boolean)
      .slice(0, 2)
      .join('、')
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

function eventLabel(type: string) {
  const labels: Record<string, string> = {
    run_started: '任务启动',
    context_built: '上下文准备',
    llm_step_started: '思考回合开始',
    llm_thinking_started: '开始思考',
    llm_thinking_delta: '思考中',
    llm_thinking_completed: '思考完成',
    action_text_delta: '动作生成中',
    action_selected: '动作选择',
    tool_started: '工具启动',
    tool_progress: '工具进度',
    tool_completed: '工具完成',
    tool_failed: '工具失败',
    retrieval_completed: '证据检索',
    knowledge_search_started: '开始检索',
    knowledge_search_completed: '知识命中',
    memory_search_completed: '记忆召回',
    memory_accepted: '已记住偏好',
    memory_candidate_created: '偏好待确认',
    memory_recalled: '偏好召回',
    final_answer_started: '输出开始',
    final_answer_delta: '输出中',
    final_answer_completed: '输出完成',
    run_completed: '运行完成',
    reasoning_completed: '推理完成',
    recommendation_generated: '建议生成',
    awaiting_user: '等待确认',
    run_failed: '运行失败',
    run_cancelled: '已取消',
  }
  return labels[type] || type
}

function eventCaption(event: AgentEvent) {
  const payload = event.payload || {}
  if (event.type === 'action_selected') {
    return String(payload.thought_summary ?? payload.action ?? 'Agent 已选择下一步动作')
  }
  if (event.type === 'tool_started') {
    return String(payload.tool_name ?? payload.toolName ?? '工具开始执行')
  }
  if (event.type === 'tool_completed') {
    return String(payload.summary ?? payload.tool_name ?? payload.toolName ?? '工具已返回业务数据')
  }
  if (event.type === 'tool_failed') {
    return String(payload.error ?? '工具调用失败')
  }
  if (event.type === 'retrieval_completed' || event.type === 'knowledge_search_completed') {
    const documents = Array.isArray(payload.documents) ? payload.documents.length : 0
    return documents ? `命中 ${documents} 条证据` : '已完成知识检索'
  }
  if (event.type === 'memory_search_completed') {
    const items = Array.isArray(payload.items) ? payload.items.length : 0
    return items ? `召回 ${items} 条记忆` : '没有召回摘要记忆'
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
  if (event.type === 'run_completed') {
    return '本轮研判已完成'
  }
  return event.message || event.title || eventLabel(event.type)
}

function eventDisplayTitle(event: AgentEvent, fallback: string) {
  if (event.type === 'tool_completed') {
    return `工具完成：${normalizeEvidenceDisplayItems(event)[0]?.title || fallback}`
  }
  if (event.type === 'tool_started') {
    const toolName = String(event.payload?.tool_name ?? event.payload?.toolName ?? '')
    return toolName ? `工具启动：${toolName}` : event.title || fallback
  }
  if (event.type === 'tool_failed') {
    const toolName = String(event.payload?.tool_name ?? event.payload?.toolName ?? '')
    return toolName ? `工具失败：${toolName}` : event.title || fallback
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

  if (turn.status === 'running') {
    const runningItems: TimelineItem[] = []
    turn.events
      .filter((event) => !isHiddenEvent(event.type))
      .forEach((event) => {
        runningItems.push({
          kind: 'event',
          event,
          displaySeq: nextDisplaySeq(),
          label: eventLabel(event.type),
          caption: eventCaption(event),
          detail: event.payload ? JSON.stringify(event.payload, null, 2) : '',
        })
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
    if (includeLiveStream && turn.streamState.finalText) {
      const liveFinalText = readableFinalAnswer(turn.streamState.finalText)
        || recommendation?.judgment
        || recommendation?.summary
        || ''
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
    }
    if (includeLiveStream && turn.streamState.finalizing) {
      runningItems.push({
        kind: 'finalizing',
        id: `${turn.id}_finalizing`,
        text: '正在整理最终结果',
        detail: 'ReAct 已完成，正在保存研判摘要、会话消息和记忆候选。',
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

    if (!isHiddenEvent(event.type)) {
      items.push({
        kind: 'event',
        event,
        displaySeq: nextDisplaySeq(),
        label: eventLabel(event.type),
        caption: eventCaption(event),
        detail: event.payload ? JSON.stringify(event.payload, null, 2) : '',
      })
    }
  })

  flushThinking(false)
  flushFinal(false)
  return items
}

function buildReactRounds(turn: Extract<AgentConversationTurn, { kind: 'agent_run' }>) {
  return groupEventsIntoReactRounds(turn.events, {
    status: turn.status,
    roundOverrides: roundExpansionOverrides.value[turn.runId || turn.id],
  })
}

function buildRoundTimelineItems(
  turn: Extract<AgentConversationTurn, { kind: 'agent_run' }>,
  round: AgentReactRound,
) {
  const rounds = buildReactRounds(turn)
  const finalRoundId = rounds.at(-1)?.id
  const isFinalRound = round.id === finalRoundId
  return buildTimelineItems(
    {
      ...turn,
      events: round.events,
      collapsed: false,
      streamState: isFinalRound ? turn.streamState : createEmptyStreamState(),
    },
    {
      includeLiveStream: isFinalRound,
      includeSummaryFallback: isFinalRound,
    },
  )
}

function toggleRound(runId: string, roundId: string) {
  const currentRun = roundExpansionOverrides.value[runId] || {}
  const currentValue = currentRun[roundId]
  roundExpansionOverrides.value = {
    ...roundExpansionOverrides.value,
    [runId]: {
      ...currentRun,
      [roundId]: currentValue === true ? false : true,
    },
  }
}

function recommendationFromSummary(summary?: AgentRunSummary): AgentRecommendation | undefined {
  if (!summary) {
    return undefined
  }
  return {
    summary: summary.finalAnswer || summary.judgment || '',
    riskLevel: summary.riskLevel || 'medium',
    judgment: summary.judgment || summary.finalAnswer || '',
    recommendedActions: summary.recommendedActions || [],
    missingInformation: summary.openQuestions || [],
    humanConfirmationRequired: false,
  }
}

function isHiddenEvent(type: string) {
  return [
    'llm_thinking_started',
    'llm_thinking_delta',
    'llm_thinking_completed',
    'action_text_delta',
    'final_answer_started',
    'final_answer_delta',
    'final_answer_completed',
    'memory_accepted',
    'memory_candidate_created',
    'memory_recalled',
  ].includes(type)
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
    turn.streamState.finalText += String(event.payload?.delta ?? '')
    return
  }

  if (event.type === 'final_answer_completed') {
    turn.summary = {
      ...(turn.summary || { recommendedActions: [] }),
      finalAnswer: turn.streamState.finalText || String(event.payload?.content ?? ''),
      recommendedActions: turn.summary?.recommendedActions || [],
    }
    return
  }

  if (event.type === 'run_completed') {
    turn.streamState.finalizing = true
  }

  if (isHiddenEvent(event.type)) {
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

async function selectSession(session: AgentSession) {
  selectedSessionId.value = session.id
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
}

function startNewConversation() {
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
  autoFollowConversation.value = true
  errorMessage.value = ''
  exportMessage.value = null
  pendingExportPlan.value = null
  input.value = ''

  let sessionId = selectedSessionId.value
  if (!sessionId || sessionId.startsWith('ana_demo')) {
    let session
    try {
      session = await createAgentSession({ title: sessionTitle, sourceType: 'manual' })
    } catch (error) {
      errorMessage.value = readableAgentError(error)
      input.value = textInput
      isRunning.value = false
      return
    }
    sessionId = session.sessionId
    const nextSession: AgentSession = {
      id: session.sessionId,
      title: sessionTitle,
      status: 'running',
      summary: 'Agent 正在研判',
      updatedAt: new Date().toLocaleString(),
    }
    sessions.value = [nextSession, ...sessions.value.filter((item) => !item.id.startsWith('ana_demo'))]
    selectedSessionId.value = sessionId
    turns.value = []
    beforeCursor.value = undefined
    hasMoreBefore.value = false
  }

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
  turns.value = [...turns.value, userTurn, runTurn]
  clearImageAttachments()
  activeRunId.value = ''
  await nextTick()
  scrollConversationToBottom()

  try {
    const { run, exportPlan } = await sendAgentMessageStream(
      sessionId,
      rawInput,
      (event) => {
        activeRunId.value = event.runId || activeRunId.value
        updateCurrentRun(event)
        void nextTick(() => {
          if (autoFollowConversation.value) {
            scrollConversationToBottom()
          }
        })
      },
    )

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
    <section class="agent-layout">
      <aside class="agent-sidebar table-panel" aria-label="Agent sessions">
        <div class="panel-header">
          <div class="panel-title-stack">
            <span class="eyebrow">SESSION QUEUE</span>
            <h3 class="panel-title-text">研判会话</h3>
          </div>
          <span class="chip">{{ sessions.length }}</span>
        </div>

        <button class="session-new" :disabled="isRunning" @click="startNewConversation">
          开启新对话
        </button>

        <button class="memory-entry" type="button" @click="memoryPanelOpen = true">
          <span>我的偏好</span>
          <strong>{{ activeMemories.length }}</strong>
          <small v-if="pendingMemories.length">{{ pendingMemories.length }} 条待确认</small>
        </button>

        <div class="session-list">
          <article
            v-for="session in sessions"
            :key="session.id"
            class="session-item"
            :class="{ 'is-active': session.id === selectedSessionId }"
          >
            <button class="session-open" @click="selectSession(session)">
              <div class="session-item__head">
                <strong class="session-title">{{ session.title }}</strong>
                <span class="chip" :class="statusClass(session.status)">{{ statusLabel(session.status) }}</span>
              </div>
              <div class="session-item__meta">
                <span>{{ session.objectName || '手动输入' }}</span>
                <span>{{ session.updatedAt || '刚刚更新' }}</span>
              </div>
              <p class="session-summary">{{ session.summary || '等待 Agent 生成分析结论' }}</p>
            </button>
            <button
              class="session-delete"
              :disabled="isRunning || session.id.startsWith('ana_demo')"
              :aria-label="`删除会话 ${session.title}`"
              @click="deleteSession(session)"
            >
              删除
            </button>
          </article>
        </div>
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
                  <h3>ReAct 研判过程</h3>
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

              <div class="timeline">
                <template v-if="turn.collapsed">
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
                        <p class="stream-text">{{ item.content }}<span v-if="item.running" class="cursor">|</span></p>
                        <ol v-if="item.recommendation?.recommendedActions.length" class="action-list">
                          <li v-for="action in item.recommendation.recommendedActions" :key="action">{{ action }}</li>
                        </ol>
                      </div>
                    </template>
                  </article>
                </template>

                <template v-else>
                  <section
                    v-for="round in buildReactRounds(turn)"
                    :key="round.id"
                    class="react-round"
                    :class="{ 'is-collapsed': round.collapsed }"
                  >
                    <button
                      class="react-round__head"
                      type="button"
                      @click="toggleRound(turn.runId || turn.id, round.id)"
                    >
                      <span>{{ round.title }}</span>
                      <small>
                        工具 {{ round.summary.toolCount }} 次 · 知识 {{ round.summary.knowledgeCount }} 条 · 异常 {{ round.summary.errorCount + round.summary.warningCount }} 条
                      </small>
                    </button>

                    <div v-if="!round.collapsed" class="react-round__body">
                      <article
                        v-for="item in buildRoundTimelineItems(turn, round)"
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

                        <template v-else-if="item.kind === 'thinking'">
                          <div class="timeline-rail"><span>{{ item.seq }}</span></div>
                          <div class="timeline-body">
                            <div class="event-head">
                              <span class="chip">LLM 思考</span>
                              <small>STEP {{ item.step || '-' }}</small>
                            </div>
                            <p class="stream-text">{{ item.content }}<span v-if="item.running" class="cursor">|</span></p>
                          </div>
                        </template>

                        <template v-else-if="item.kind === 'final'">
                          <div class="timeline-rail"><span>{{ item.seq || 'AI' }}</span></div>
                          <div class="timeline-body">
                            <div class="event-head">
                              <span class="chip" :class="riskClass(item.recommendation?.riskLevel)">最终回答</span>
                              <small>{{ riskLabel(item.recommendation?.riskLevel) }}</small>
                            </div>
                            <p class="stream-text">{{ item.content }}<span v-if="item.running" class="cursor">|</span></p>
                            <ol v-if="item.recommendation?.recommendedActions.length" class="action-list">
                              <li v-for="action in item.recommendation.recommendedActions" :key="action">{{ action }}</li>
                            </ol>
                          </div>
                        </template>

                        <template v-else-if="item.kind === 'finalizing'">
                          <div class="timeline-rail"><span>...</span></div>
                          <div class="timeline-body">
                            <div class="event-head">
                              <span class="chip is-warn">收尾中</span>
                              <small>正在归档</small>
                            </div>
                            <h3>{{ item.text }}<span class="cursor">|</span></h3>
                            <p>{{ item.detail }}</p>
                          </div>
                        </template>

                        <template v-else>
                          <div class="timeline-rail"><span>{{ item.displaySeq }}</span></div>
                          <div class="timeline-body">
                            <div class="event-head">
                              <span class="chip">{{ item.label }}</span>
                              <small>{{ item.event.createdAt || `SEQ ${item.event.seq}` }}</small>
                            </div>
                            <h3>{{ eventDisplayTitle(item.event, item.label) }}</h3>
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
                            <details v-if="item.detail" class="event-detail">
                              <summary>查看调试详情</summary>
                              <pre>{{ item.detail }}</pre>
                            </details>
                          </div>
                        </template>
                      </article>
                    </div>
                  </section>
                </template>
              </div>
            </article>
          </template>

          <p v-if="errorMessage" class="error-banner">{{ errorMessage }}</p>
          <p v-if="exportMessage" class="export-banner">
            {{ exportMessage.text }}
            <a :href="exportMessage.url" target="_blank" rel="noreferrer">下载</a>
          </p>
          <article v-if="pendingExportPlan" class="export-plan">
            <div>
              <strong>{{ pendingExportPlan.title || '导出计划' }}</strong>
              <p>{{ pendingExportPlan.reason || 'Agent 已生成结构化导出计划，确认后开始生成文件。' }}</p>
            </div>
            <dl>
              <div>
                <dt>格式</dt>
                <dd>{{ pendingExportPlan.format === 'excel' ? 'Excel' : 'PDF' }}</dd>
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

        <div class="composer">
          <div class="composer__field">
            <div v-if="imageAttachments.length" class="attachment-list" aria-label="图片附件">
              <article v-for="attachment in imageAttachments" :key="attachment.id" class="attachment-chip">
                <img :src="attachment.previewUrl" :alt="attachment.name">
                <span>{{ attachment.name }}</span>
                <button type="button" aria-label="移除图片" @click="removeImageAttachment(attachment)">×</button>
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
                class="sample-chip"
                :class="{ 'is-live': isListening }"
                :disabled="isRunning || isExporting || !speechSupported"
                @click="toggleVoiceInput"
              >
                {{ isListening ? '停止语音' : '语音输入' }}
              </button>
              <button
                type="button"
                class="sample-chip"
                :disabled="isRunning || isExporting || imageAttachments.length >= 4"
                @click="triggerImageUpload"
              >
                上传图片
              </button>
              <input
                ref="fileInput"
                class="composer-file-input"
                type="file"
                accept="image/*"
                multiple
                @change="handleImageUpload"
              >
            </div>
            <div class="composer__samples">
              <button
                v-for="sample in samplePrompts"
                :key="sample"
                class="sample-chip"
                :disabled="isRunning || isExporting"
                @click="input = sample"
              >
                {{ sample }}
              </button>
              <button
                v-for="item in quickExportFormats"
                :key="item.format"
                class="sample-chip is-export"
                :disabled="isRunning || isExporting || !selectedSessionId || selectedSessionId.startsWith('ana_demo')"
                @click="exportSelectedSession(item.format)"
              >
                {{ isExporting ? '导出中' : item.label }}
              </button>
            </div>
          </div>
          <div class="composer__actions">
            <button class="control-button is-primary" :disabled="isRunning" @click="submitAnalysis">
              {{ isRunning ? '分析中' : '发起研判' }}
            </button>
            <button class="ghost-action" :disabled="!activeRunId || !isRunning" @click="cancelRun">停止运行</button>
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

.agent-main {
  grid-template-rows: minmax(0, 1fr) auto;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.018), rgba(255, 255, 255, 0.006)),
    linear-gradient(180deg, rgba(110, 202, 212, 0.04), transparent 30%),
    var(--color-panel-2);
}

.panel-title-stack {
  display: grid;
  gap: 4px;
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
  gap: var(--space-2);
}

.session-new {
  inline-size: 100%;
  min-height: 38px;
  margin-bottom: var(--space-2);
  border: 1px solid rgba(231, 104, 45, 0.38);
  color: var(--color-text);
  background:
    linear-gradient(180deg, rgba(231, 104, 45, 0.14), rgba(255, 255, 255, 0.018)),
    rgba(5, 10, 15, 0.86);
  font-weight: 700;
}

.memory-entry {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 4px 10px;
  align-items: center;
  inline-size: 100%;
  min-height: 52px;
  margin-bottom: var(--space-2);
  padding: 10px 12px;
  border: 1px solid rgba(110, 202, 212, 0.24);
  color: var(--color-text);
  background: rgba(5, 12, 18, 0.74);
  text-align: left;
}

.memory-entry strong {
  color: var(--color-accent-cyan);
  font-family: var(--font-mono);
}

.memory-entry small {
  grid-column: 1 / -1;
  color: var(--color-warning);
  font-size: var(--text-micro);
}

.session-item {
  position: relative;
  display: grid;
  inline-size: 100%;
  border: 1px solid var(--color-line);
  color: var(--color-text);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.028), rgba(255, 255, 255, 0.01)),
    rgba(4, 9, 14, 0.8);
  transition: border-color 160ms ease, background 160ms ease, transform 160ms ease;
}

.session-item:hover {
  border-color: rgba(110, 202, 212, 0.34);
  transform: translateY(-1px);
}

.session-item.is-active {
  border-color: rgba(231, 104, 45, 0.52);
  background:
    linear-gradient(180deg, rgba(231, 104, 45, 0.12), rgba(255, 255, 255, 0.01)),
    rgba(8, 10, 13, 0.92);
}

.session-open {
  display: grid;
  gap: 10px;
  inline-size: 100%;
  min-width: 0;
  padding: 14px 54px 14px 14px;
  border: 0;
  color: inherit;
  background: transparent;
  text-align: left;
}

.session-delete {
  position: absolute;
  inset-block-start: 10px;
  inset-inline-end: 10px;
  min-height: 26px;
  padding: 0 8px;
  border: 1px solid rgba(231, 78, 45, 0.28);
  color: var(--color-danger);
  background: rgba(231, 78, 45, 0.08);
  font-size: var(--text-micro);
}

.session-delete:disabled {
  opacity: 0.45;
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
  gap: var(--space-2);
}

.react-round {
  display: grid;
  gap: var(--space-2);
  min-width: 0;
  border: 1px solid rgba(96, 121, 139, 0.16);
  background: rgba(255, 255, 255, 0.012);
}

.react-round.is-collapsed {
  background: rgba(255, 255, 255, 0.008);
}

.react-round__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  width: 100%;
  min-width: 0;
  padding: var(--space-3);
  border: 0;
  color: var(--color-text);
  background: transparent;
  cursor: pointer;
}

.react-round__head span,
.react-round__head small {
  min-width: 0;
}

.react-round__head small {
  color: var(--color-text-muted);
  text-align: right;
}

.react-round__body {
  display: grid;
  gap: var(--space-3);
  min-width: 0;
  padding: 0 var(--space-3) var(--space-3);
}

.timeline-card {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: var(--space-3);
  padding: var(--space-3);
  border: 1px solid rgba(96, 121, 139, 0.18);
  background: rgba(255, 255, 255, 0.014);
}

.timeline-card.is-thinking {
  border-color: rgba(110, 202, 212, 0.24);
  background:
    linear-gradient(135deg, rgba(110, 202, 212, 0.07), rgba(255, 255, 255, 0.01)),
    rgba(4, 8, 12, 0.82);
}

.timeline-card.is-final {
  border-color: rgba(231, 104, 45, 0.28);
  background:
    linear-gradient(180deg, rgba(231, 104, 45, 0.09), rgba(255, 255, 255, 0.01)),
    rgba(7, 10, 13, 0.9);
}

.timeline-card.is-finalizing {
  border-color: rgba(213, 171, 76, 0.32);
  background:
    linear-gradient(180deg, rgba(213, 171, 76, 0.08), rgba(255, 255, 255, 0.01)),
    rgba(7, 10, 13, 0.9);
}

.timeline-card.is-summary {
  border-color: rgba(110, 202, 212, 0.3);
}

.timeline-rail {
  display: grid;
  justify-items: center;
}

.timeline-rail span {
  display: grid;
  place-items: center;
  width: 32px;
  min-width: 32px;
  height: 32px;
  border: 1px solid rgba(110, 202, 212, 0.24);
  color: var(--color-accent-cyan);
  font-family: var(--font-mono);
  font-size: var(--text-meta);
  background: rgba(255, 255, 255, 0.02);
}

.timeline-body {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.stream-text {
  white-space: pre-wrap;
}

.cursor {
  color: var(--color-accent-cyan);
}

.action-list {
  margin: 0;
  padding-left: 18px;
  color: var(--color-text);
  line-height: var(--leading-body);
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

.event-detail {
  display: grid;
  gap: var(--space-2);
}

.event-detail summary {
  color: var(--color-text-muted);
  font-size: var(--text-meta);
  cursor: pointer;
}

.event-detail pre {
  max-height: 180px;
  margin: 0;
  padding: var(--space-2);
  overflow: auto;
  border: 1px solid rgba(110, 202, 212, 0.12);
  color: var(--color-text-muted);
  background: rgba(2, 6, 10, 0.5);
  white-space: pre-wrap;
}

.composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 128px;
  gap: var(--space-3);
  padding-top: var(--space-2);
  border-top: 1px solid var(--color-line);
}

.composer__field {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.attachment-list,
.composer__tools {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
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
  border: 1px solid rgba(110, 202, 212, 0.18);
  background: rgba(255, 255, 255, 0.018);
}

.attachment-chip img {
  width: 34px;
  height: 34px;
  object-fit: cover;
  border: 1px solid rgba(110, 202, 212, 0.18);
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
  border: 1px solid rgba(110, 202, 212, 0.2);
  color: var(--color-text);
  background: rgba(5, 10, 15, 0.82);
}

.composer textarea::placeholder {
  color: var(--color-text-dim);
}

.composer-file-input {
  display: none;
}

.composer__samples {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.sample-chip,
.ghost-action {
  min-height: 34px;
  border: 1px solid rgba(110, 202, 212, 0.18);
  color: var(--color-text-muted);
  background: rgba(255, 255, 255, 0.018);
  padding: 0 12px;
}

.sample-chip {
  min-height: 28px;
  padding: 0 10px;
  font-size: var(--text-micro);
  letter-spacing: 0.02em;
  text-align: left;
}

.sample-chip.is-export {
  border-color: rgba(231, 104, 45, 0.32);
  color: var(--color-text);
  background: rgba(231, 104, 45, 0.08);
}

.sample-chip.is-live {
  border-color: rgba(34, 197, 94, 0.34);
  color: var(--color-text);
  background: rgba(34, 197, 94, 0.1);
}

.composer__actions {
  display: grid;
  gap: 10px;
  align-content: start;
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
  .composer {
    grid-template-columns: 1fr;
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
