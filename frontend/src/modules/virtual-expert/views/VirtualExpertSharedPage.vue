<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import ModuleShell from '../../../components/business/ModuleShell.vue'
import { useRouter } from '../../../router'
import { fetchSharedAgentSession } from '../api'
import { renderMarkdownToHtml } from '../service'
import type { AgentSharedSessionResponse } from '../types'

const router = useRouter()
const shareId = computed(() => router.currentRoute.value.params.shareId || '')
const shared = ref<AgentSharedSessionResponse | null>(null)
const errorMessage = ref('')
const isLoading = ref(true)

const session = computed(() => shared.value?.snapshot?.session || {})
const finalAnswer = computed(() => shared.value?.snapshot?.finalAnswer || '')
const reactTimeline = computed(() => shared.value?.snapshot?.reactTimeline || [])

onMounted(async () => {
  try {
    shared.value = await fetchSharedAgentSession(shareId.value)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '分享内容加载失败'
  } finally {
    isLoading.value = false
  }
})

function eventSummary(event: { type?: string; title?: string; payload?: Record<string, unknown> }) {
  const payload = event.payload || {}
  return String(
    payload.summary
      ?? payload.content
      ?? payload.tool_name
      ?? payload.toolName
      ?? payload.delta
      ?? event.title
      ?? event.type
      ?? '',
  )
}
</script>

<template>
  <ModuleShell active-path="/virtual-expert" eyebrow="Shared Expert Review" title="虚拟专家分享页" ops-label="只读" ops-value="Public">
    <section class="shared-page table-panel">
      <p v-if="isLoading" class="shared-muted">正在加载分享内容...</p>
      <p v-else-if="errorMessage" class="error-banner">{{ errorMessage }}</p>
      <template v-else>
        <header class="shared-head">
          <span class="chip">PUBLIC SHARE</span>
          <h2>{{ session.title || shared?.title || '虚拟专家研判' }}</h2>
          <p v-if="session.summary">{{ session.summary }}</p>
        </header>

        <section v-if="reactTimeline.length" class="shared-section">
          <h3>ReAct 过程线</h3>
          <article v-for="event in reactTimeline" :key="event.id" class="shared-event">
            <span>{{ event.type }}</span>
            <strong>{{ event.title || event.type }}</strong>
            <p>{{ eventSummary(event) }}</p>
          </article>
        </section>

        <section v-if="finalAnswer" class="shared-section">
          <h3>最终答案</h3>
          <div class="markdown-answer" v-html="renderMarkdownToHtml(finalAnswer)"></div>
        </section>

        <section v-if="shared?.markdown" class="shared-section">
          <h3>Markdown</h3>
          <pre>{{ shared.markdown }}</pre>
        </section>
      </template>
    </section>
  </ModuleShell>
</template>

<style scoped>
.shared-page {
  display: grid;
  align-content: start;
  gap: var(--space-4);
  min-height: 100%;
}

.shared-page h2,
.shared-page h3,
.shared-page p {
  margin: 0;
}

.shared-head {
  display: grid;
  gap: var(--space-2);
}

.shared-head h2 {
  font-size: var(--text-title);
}

.shared-page p,
.shared-muted {
  color: var(--color-text-muted);
  font-size: var(--text-meta);
}

.shared-section {
  display: grid;
  gap: 10px;
}

.shared-section h3 {
  color: var(--color-text);
  font-size: var(--text-body);
}

.shared-event {
  display: grid;
  grid-template-columns: 130px minmax(0, 180px) minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  padding: 10px 12px;
  border: 1px solid rgba(110, 202, 212, 0.16);
  background: rgba(255, 255, 255, 0.016);
}

.shared-event span,
.shared-event p {
  color: var(--color-text-muted);
  font-size: var(--text-micro);
}

.shared-event strong {
  color: var(--color-text);
  font-size: var(--text-meta);
}

.shared-section pre {
  overflow: auto;
  max-height: 360px;
  padding: 12px;
  border: 1px solid rgba(110, 202, 212, 0.16);
  color: var(--color-text-muted);
  background: rgba(5, 10, 15, 0.72);
  font-size: var(--text-micro);
}
</style>
