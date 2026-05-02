import { apiRequest, getAuthorizedUrl } from '../shared/apiClient'
import type {
  AgentCancelResponse,
  AgentDeleteSessionResponse,
  AgentEvent,
  AgentEventsResponse,
  AgentExportFormat,
  AgentExportPlan,
  AgentExportResponse,
  AgentMemoriesResponse,
  AgentMemoryMutationResponse,
  AgentMessageRunResponse,
  AgentRunResponse,
  AgentSessionResponse,
  AgentSessionsResponse,
  AgentTimelineResponse,
  CreateAgentMessagePayload,
  CreateAgentSessionPayload,
} from './types'

export function fetchAgentSessions(limit = 20) {
  return apiRequest<AgentSessionsResponse>('/manager/virtual-expert/agent/sessions', {
    query: { limit },
  })
}

export function createAgentSession(payload: CreateAgentSessionPayload) {
  return apiRequest<AgentSessionResponse>('/manager/virtual-expert/agent/sessions', {
    method: 'POST',
    body: {
      ...payload,
      sourceType: payload.sourceType || 'manual',
    },
  })
}

export function runAgentSession(sessionId: string) {
  return apiRequest<AgentRunResponse>(
    `/manager/virtual-expert/agent/sessions/${encodeURIComponent(sessionId)}/runs`,
    { method: 'POST' },
  )
}

export function createAgentMessage(sessionId: string, payload: CreateAgentMessagePayload) {
  return apiRequest<AgentMessageRunResponse>(
    `/manager/virtual-expert/agent/sessions/${encodeURIComponent(sessionId)}/messages`,
    {
      method: 'POST',
      body: {
        content: payload.content,
        messageType: payload.messageType || 'text',
      },
    },
  )
}

export function getAgentRunStreamUrl(sessionId: string) {
  return getAuthorizedUrl(
    `/manager/virtual-expert/agent/sessions/${encodeURIComponent(sessionId)}/runs/stream`,
    undefined,
    true,
  )
}

export function getSpecificAgentRunStreamUrl(sessionId: string, runId: string) {
  return getAuthorizedUrl(
    `/manager/virtual-expert/agent/sessions/${encodeURIComponent(sessionId)}/runs/${encodeURIComponent(runId)}/stream`,
    undefined,
    true,
  )
}

export function normalizeAgentEvent(event: Record<string, unknown>): AgentEvent {
  return {
    ...(event as AgentEvent),
    sessionId: String(event.sessionId ?? event.session_id ?? ''),
    runId: String(event.runId ?? event.run_id ?? ''),
    createdAt: typeof event.createdAt === 'string'
      ? event.createdAt
      : typeof event.created_at === 'string'
        ? event.created_at
        : undefined,
  }
}

export function fetchAgentEvents(sessionId: string, afterSeq = 0) {
  return apiRequest<AgentEventsResponse>(
    `/manager/virtual-expert/agent/sessions/${encodeURIComponent(sessionId)}/events`,
    { query: { afterSeq } },
  )
}

export function fetchAgentRunEvents(sessionId: string, runId: string, afterSeq = 0) {
  return apiRequest<AgentEventsResponse>(
    `/manager/virtual-expert/agent/sessions/${encodeURIComponent(sessionId)}/runs/${encodeURIComponent(runId)}/events`,
    { query: { afterSeq } },
  )
}

export function fetchAgentTimeline(sessionId: string, query: { beforeCursor?: string; limit?: number } = {}) {
  return apiRequest<AgentTimelineResponse>(
    `/manager/virtual-expert/agent/sessions/${encodeURIComponent(sessionId)}/timeline`,
    { query },
  )
}

export function cancelAgentRun(sessionId: string, runId: string) {
  return apiRequest<AgentCancelResponse>(
    `/manager/virtual-expert/agent/sessions/${encodeURIComponent(sessionId)}/runs/${encodeURIComponent(runId)}/cancel`,
    { method: 'POST' },
  )
}

export function deleteAgentSession(sessionId: string) {
  return apiRequest<AgentDeleteSessionResponse>(
    `/manager/virtual-expert/agent/sessions/${encodeURIComponent(sessionId)}`,
    { method: 'DELETE' },
  )
}

export function fetchAgentMemories(status: 'active' | 'pending' = 'active') {
  return apiRequest<AgentMemoriesResponse>('/manager/virtual-expert/agent/memories', {
    query: { status },
  })
}

export function acceptAgentMemoryCandidate(candidateId: string) {
  return apiRequest<AgentMemoryMutationResponse>(
    `/manager/virtual-expert/agent/memory-candidates/${encodeURIComponent(candidateId)}/accept`,
    { method: 'POST' },
  )
}

export function rejectAgentMemoryCandidate(candidateId: string) {
  return apiRequest<AgentMemoryMutationResponse>(
    `/manager/virtual-expert/agent/memory-candidates/${encodeURIComponent(candidateId)}/reject`,
    { method: 'POST' },
  )
}

export function deleteAgentMemory(memoryId: string) {
  return apiRequest<AgentMemoryMutationResponse>(
    `/manager/virtual-expert/agent/memories/${encodeURIComponent(memoryId)}`,
    { method: 'DELETE' },
  )
}

export function createAgentExport(payload: { sessionId: string; runId?: string; format: AgentExportFormat; exportPlan?: AgentExportPlan }) {
  return apiRequest<AgentExportResponse>('/manager/virtual-expert/agent/exports', {
    method: 'POST',
    body: payload,
  })
}

export function getAgentExportDownloadUrl(downloadUrl: string) {
  return getAuthorizedUrl(downloadUrl, undefined, true)
}
