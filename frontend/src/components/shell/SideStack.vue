<script setup lang="ts">
import type { DashboardSidePanel } from '../../modules/dashboard/types'

defineProps<{
  panels: readonly DashboardSidePanel[]
}>()
</script>

<template>
  <div class="side-stack">
    <section
      v-for="panel in panels"
      :key="panel.title"
      class="panel side-panel"
    >
      <div class="panel-head">
        <h2 class="panel-title">{{ panel.title }}</h2>
        <span class="eyebrow">{{ panel.eyebrow }}</span>
      </div>

      <div class="side-panel__body">
        <article
          v-for="item in panel.items"
          :key="item.title"
          class="signal-card"
          :class="`is-${item.tone}`"
        >
          <strong>{{ item.title }}</strong>
          <p>{{ item.body }}</p>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.side-panel {
  position: relative;
}

.side-panel__body {
  min-height: 0;
}

.signal-card {
  position: relative;
  overflow: hidden;
}

.signal-card::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  inline-size: 3px;
  background: rgba(110, 202, 212, 0.26);
}

.signal-card.is-critical::before {
  background: rgba(231, 104, 45, 0.7);
}

.signal-card.is-warning::before {
  background: rgba(221, 176, 84, 0.66);
}
</style>
