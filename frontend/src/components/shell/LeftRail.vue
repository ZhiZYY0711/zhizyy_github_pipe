<script setup lang="ts">
import SummaryCard from './SummaryCard.vue'
import type { DashboardSummaryCard } from '../../modules/dashboard/types'

defineProps<{
  cards: readonly DashboardSummaryCard[]
}>()
</script>

<template>
  <aside class="left-rail">
    <SummaryCard
      v-for="card in cards"
      :key="`${card.type}-${card.title}`"
      :card="card"
    />
  </aside>
</template>

<style scoped>
.left-rail {
  position: relative;
}

.left-rail::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  inline-size: 1px;
  background: linear-gradient(180deg, rgba(110, 202, 212, 0.14), transparent 30%, rgba(231, 104, 45, 0.1) 72%, transparent);
  opacity: 0.6;
}
</style>
