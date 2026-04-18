<script setup lang="ts">
import { dashboardMapPanel as defaultPanel } from '../../data/mockShell'
import type { DashboardMapPanel } from '../../modules/dashboard/types'

const props = withDefaults(
  defineProps<{
    panel?: DashboardMapPanel
  }>(),
  {
    panel: () => defaultPanel,
  },
)
</script>

<template>
  <section class="panel map-panel">
    <div class="panel-head">
      <h2 class="panel-title">{{ props.panel.title }}</h2>
      <span class="eyebrow">{{ props.panel.eyebrow }}</span>
    </div>

    <div class="map-body">
      <div class="map-body__grid"></div>
      <div class="map-shape"></div>
      <div class="map-ring is-r1"></div>
      <div class="map-ring is-r2"></div>
      <div class="map-path is-a"></div>
      <div class="map-path is-b"></div>

      <article
        v-for="callout in props.panel.callouts"
        :key="`${callout.placement}-${callout.title}`"
        class="map-callout"
        :class="{
          'is-a': callout.placement === 'north-east',
          'is-b': callout.placement === 'south-west',
        }"
      >
        <small>{{ callout.eyebrow }}</small>
        <strong>{{ callout.title }}</strong>
        <span>{{ callout.body }}</span>
      </article>

      <div class="legend">
        <strong>{{ props.panel.legend.title }}</strong>
        <span>{{ props.panel.legend.body }}</span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.map-body {
  overflow: hidden;
}

.map-body__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(110, 202, 212, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(110, 202, 212, 0.04) 1px, transparent 1px);
  background-size: 32px 32px;
  mask-image: radial-gradient(circle at center, rgba(0, 0, 0, 0.9), transparent 92%);
}

.map-callout {
  backdrop-filter: blur(14px);
}

.legend {
  position: absolute;
  inset: auto 26px 26px auto;
  max-inline-size: 240px;
  padding: 12px 14px;
  display: grid;
  gap: 6px;
  border: 1px solid rgba(143, 166, 182, 0.2);
  background: rgba(8, 13, 19, 0.84);
}

.legend strong {
  font-size: var(--text-meta);
  letter-spacing: var(--tracking-panel);
  text-transform: uppercase;
  color: var(--color-text);
}

.legend span {
  color: var(--color-text-muted);
  font-size: var(--text-meta);
  line-height: 1.5;
}

@media (max-width: 719px) {
  .map-callout,
  .legend {
    position: relative;
    inset: auto;
    inline-size: auto;
    max-inline-size: none;
  }

  .map-body {
    display: grid;
    gap: var(--space-3);
    align-content: start;
  }

  .map-shape {
    position: relative;
    inset: auto;
    min-block-size: 280px;
  }
}
</style>
