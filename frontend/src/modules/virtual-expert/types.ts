export type VirtualExpertMessage = {
  id: string
  role: 'user' | 'expert' | 'system'
  content: string
  timestamp: string
}

export type SendMessageRequest = {
  conversationId: string
  userId?: string
  type?: 'chat'
  content: string
}

export type SendMessageResponse = {
  success: boolean
  message: string
  conversationId: string
  timestamp: number
}

export type CloseConversationResponse = {
  success: boolean
  message: string
  conversationId: string
  timestamp: number
}

export type ChatLogFile = {
  fileName: string
  fullPath: string
  size: number
  lastModified: string
  etag: string
  timestamp: string
  userId: string
  sessionId: string
}

export type ChatLogsResponse = {
  success: boolean
  message: string
  totalCount: number
  fileList: ChatLogFile[]
  timestamp: number
}

export type ChatLogContent = {
  success: boolean
  message: string
  content: string
  metadata: {
    fileName: string
    size: number
    lastModified: string
    contentType: string
    etag: string
  }
  timestamp: number
}

export type SseStatus = {
  success: boolean
  message: string
  onlineCount: number
  sseUrl: string
  timestamp: number
}

export type AgentSession = {
  id: string
  title: string
  status: 'created' | 'running' | 'awaiting_user' | 'completed' | 'failed' | 'cancelled' | 'archived'
  incidentType?: string
  severity?: string
  objectName?: string
  summary?: string
  pinned?: boolean
  archivedAt?: string
  updatedAt?: string
}

export type AgentSessionsResponse = {
  sessions: Array<{
    id: string
    title: string
    status: AgentSession['status']
    summary?: string
    sourceType?: string
    source_type?: string
    objectName?: string
    object_name?: string
    pinned?: boolean
    archivedAt?: string
    archived_at?: string
    updatedAt?: string
    updated_at?: string
  }>
}

export type AgentModelTier = 'auto' | 'light' | 'standard' | 'performance' | 'ultimate'

export type AgentMessage = {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  messageType?: string
  createdAt: string
}

export type AgentEvent = {
  id: string
  sessionId: string
  runId: string
  seq: number
  type: string
  level: 'info' | 'warn' | 'error'
  title?: string
  message?: string
  payload?: Record<string, unknown>
  createdAt?: string
}

export type AgentMemoryType = 'preference' | 'export_preference' | 'analysis_preference' | 'interaction_preference'

export type AgentMemoryStatus = 'active' | 'pending' | 'deleted' | 'expired' | 'accepted' | 'rejected'

export type AgentMemoryItem = {
  id: string
  memoryType: AgentMemoryType
  preferenceKey?: string
  content: string
  status: AgentMemoryStatus
  riskLevel?: 'low' | 'high'
  reason?: string
  createdAt?: string
  updatedAt?: string
}

export type AgentMemoriesResponse = {
  items: AgentMemoryItem[]
}

export type AgentMemoryMutationResponse = {
  memory?: AgentMemoryItem
  candidate?: AgentMemoryItem
}

export type AgentRunStatus = 'running' | 'completed' | 'failed' | 'cancelled' | 'awaiting_user'

export type AgentRunSummary = {
  finalAnswer?: string
  riskLevel?: 'low' | 'medium' | 'high' | 'critical'
  judgment?: string
  recommendedActions: string[]
  keyEvidence?: AgentEvidenceItem[]
  openQuestions?: string[]
}

export type AgentRunStreamState = {
  thinkingText: string
  thinkingStep?: number
  thinkingSeq?: number
  finalText: string
  finalSeq?: number
  finalizing?: boolean
  finalAnswerPending?: boolean
}

export type AgentConversationTurn =
  | {
      kind: 'user_message'
      id: string
      messageId?: string
      content: string
      createdAt: string
    }
  | {
      kind: 'agent_run'
      id: string
      runId: string
      triggeringMessageId?: string
      status: AgentRunStatus
      events: AgentEvent[]
      eventsLoaded?: boolean
      summary?: AgentRunSummary
      streamState: AgentRunStreamState
      collapsed: boolean
    }

export type AgentEvidenceItem = {
  id: string
  title: string
  summary: string
  sourceType: 'tool' | 'rag' | 'business_data' | 'user_input'
  sourceRef: string
  confidence?: number
}

export type AgentEvidenceDisplayKind = 'tool' | 'knowledge' | 'business_data' | 'generic'

export type AgentEvidenceDisplayItem = {
  id: string
  kind: AgentEvidenceDisplayKind
  title: string
  summary: string
  sourceLabel: string
  score?: number
  meta?: Array<{
    label: string
    value: string
  }>
  facts: Array<{
    label: string
    value: string
    status?: 'normal' | 'warning' | 'critical'
  }>
  records?: Array<Record<string, unknown>>
  raw?: Record<string, unknown>
}

export type AgentComposerAttachment = {
  id: string
  name: string
  type: string
  size: number
  previewUrl: string
}

export type AgentReactRound = {
  id: string
  step: number
  title: string
  events: AgentEvent[]
  summary: {
    toolCount: number
    knowledgeCount: number
    warningCount: number
    errorCount: number
  }
  collapsed: boolean
  userPinned?: boolean
}

export type AgentRecommendation = {
  summary: string
  riskLevel?: 'low' | 'medium' | 'high' | 'critical'
  judgment: string
  recommendedActions: string[]
  missingInformation: string[]
  humanConfirmationRequired: boolean
}

export type CreateAgentSessionPayload = {
  rawInput?: string
  title?: string
  sourceType?: string
  sourceId?: string
  objectType?: string
  objectId?: string
  objectName?: string
  modelTier?: AgentModelTier
}

export type AgentSessionResponse = {
  sessionId: string
  status: string
}

export type AgentRunResponse = {
  sessionId: string
  runId: string
  events: AgentEvent[]
}

export type CreateAgentMessagePayload = {
  content: string
  messageType?: 'text'
  modelTier?: AgentModelTier
}

export type UpdateAgentSessionPayload = {
  title?: string
  pinned?: boolean
  archived?: boolean
}

export type AgentShareResponse = {
  shareId: string
  sessionId: string
  type: 'link' | 'md'
  title: string
  shareUrl: string
  markdown?: string | null
  createdAt?: string
  expiresAt?: string
}

export type AgentSharedSessionResponse = {
  shareId: string
  type: 'link' | 'md'
  title: string
  snapshot: {
    session?: {
      id?: string
      title?: string
      summary?: string
      status?: string
    }
    timeline?: AgentTimelineItem[]
    reactTimeline?: AgentEvent[]
    finalAnswer?: string
    createdAt?: string
  }
  markdown?: string | null
  createdAt?: string
}

export type AgentMessageRunResponse = {
  message: AgentMessage
  run?: {
    id: string
    sessionId: string
    status: 'created'
    streamUrl: string
  } | null
  exportPlan?: AgentExportPlan
}

export type AgentTimelineItem = {
  turnId: string
  cursor: string
  userMessage: {
    id: string
    content: string
    createdAt: string
  }
  run?: {
    id: string
    status: AgentRunStatus
    summary?: AgentRunSummary
    eventCount: number
    createdAt?: string
    completedAt?: string
  } | null
}

export type AgentTimelineResponse = {
  sessionId: string
  items: AgentTimelineItem[]
  hasMoreBefore: boolean
  beforeCursor?: string
}

export type AgentEventsResponse = {
  sessionId: string
  runId?: string
  events: AgentEvent[]
}

export type AgentCancelResponse = {
  sessionId: string
  runId: string
  status: 'cancel_requested'
}

export type AgentDeleteSessionResponse = {
  sessionId?: string
  session_id?: string
  status: 'deleted'
}

export type AgentExportFormat = 'pdf' | 'excel' | 'txt' | 'md' | 'docx'

export type AgentExportPlan = {
  format: AgentExportFormat
  scope?: 'agent_selected' | 'latest_turn' | 'full_session' | 'all_conversations'
  title?: string
  style?: 'standard_report' | 'formal_report' | 'brief_summary' | 'operation_sheet'
  audience?: string
  purpose?: string
  tone?: string
  detailLevel?: 'brief' | 'standard' | 'detailed'
  sections?: Array<{
    type: string
    title: string
    contentPolicy?: string
  }>
  tables?: Array<{
    type: string
    title: string
  }>
  evidencePolicy?: 'include_key_evidence' | 'exclude_evidence'
  includeEvidence?: boolean
  includeTimeline?: boolean
  maxSessions?: number
  maxTurnsPerSession?: number
  requiresConfirmation?: boolean
  reason?: string
}

export type AgentExportResponse = {
  exportId: string
  fileName: string
  contentType: string
  size: number
  downloadUrl: string
}
