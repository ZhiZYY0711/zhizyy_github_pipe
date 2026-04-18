<script setup lang="ts">
import type { DashboardSummaryCard } from '../../modules/dashboard/types'

defineProps<{
  card: DashboardSummaryCard
}>()
</script>

<template>
  <article class="summary-card" :class="`is-${card.type}`">
    <div class="summary-card__head">
      <span class="eyebrow">{{ card.eyebrow }}</span>
      <strong>{{ card.title }}</strong>
    </div>

    <template v-if="card.type === 'hero'">
      <div class="lens-ring">
        <div class="lens-ring__inner">
          <span>{{ card.ringLabel }}</span>
          <strong>{{ card.ringValue }}</strong>
          <small>{{ card.ringMeta }}</small>
        </div>
      </div>

      <div class="mini-grid">
        <span v-for="tag in card.tags" :key="tag">{{ tag }}</span>
      </div>
    </template>

    <template v-else-if="card.type === 'bars'">
      <div class="bars">
        <div
          v-for="bar in card.bars"
          :key="bar.label"
          class="bar-row"
        >
          <label>{{ bar.label }}</label>
          <div class="bar" :class="`is-${bar.tone}`">
            <i :style="{ width: bar.width }"></i>
          </div>
          <strong>{{ bar.value }}</strong>
        </div>
      </div>
    </template>

    <template v-else-if="card.type === 'radar'">
      <div class="radar">
        <span class="radar-dot is-a"></span>
        <span class="radar-dot is-b"></span>
        <span class="radar-dot is-c"></span>
      </div>

      <div class="focus-tags">
        <span v-for="tag in card.tags" :key="tag">{{ tag }}</span>
      </div>
    </template>

    <template v-else>
      <div class="switch-row">
        <span v-for="chip in card.chips" :key="chip" class="mini-chip">{{ chip }}</span>
      </div>
    </template>
  </article>
</template>

<style scoped>
.summary-card {
  position: relative;
  overflow: hidden;
}

.summary-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 12% 14%, rgba(110, 202, 212, 0.08), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.028), transparent 54%);
  pointer-events: none;
}

.summary-card.is-hero::after,
.summary-card.is-radar::after {
  content: '';
  position: absolute;
  inset: auto 14px 14px auto;
  inline-size: 44px;
  block-size: 1px;
  background: linear-gradient(90deg, rgba(231, 104, 45, 0.3), transparent);
}

.summary-card__head,
.lens-ring,
.bars,
.radar,
.focus-tags,
.switch-row,
.mini-grid {
  position: relative;
  z-index: 1;
}

.mini-grid span,
.focus-tags span,
.mini-chip {
  backdrop-filter: blur(8px);
}
</style>
