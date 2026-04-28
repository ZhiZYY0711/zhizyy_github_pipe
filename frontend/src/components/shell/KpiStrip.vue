<script setup lang="ts">
import type { DashboardKpiItem } from '../../modules/dashboard/types'

defineProps<{
  items: readonly DashboardKpiItem[]
}>()
</script>

<template>
  <div class="kpi-strip">
    <div
      v-for="item in items"
      :key="item.label"
      class="kpi-card"
      :class="`is-${item.tone}`"
    >
      <span class="kpi-card__label">{{ item.label }}</span>
      <strong class="kpi-card__value">{{ item.value }}</strong>
    </div>
  </div>
</template>

<style scoped>
.kpi-card {
  position: relative;
  overflow: hidden;
}

.kpi-card::after {
  content: '';
  position: absolute;
  inset: auto 14px 0;
  block-size: 2px;
  background: linear-gradient(90deg, rgba(110, 202, 212, 0.32), transparent);
  opacity: 0.38;
}

.kpi-card.is-warn::after {
  background: linear-gradient(90deg, rgba(231, 104, 45, 0.46), transparent);
  opacity: 0.6;
}

.kpi-card__value {
  letter-spacing: 0.04em;
}
</style>
