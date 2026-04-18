<script setup lang="ts">
import { computed } from 'vue'
import type { DashboardNavItem, DashboardStatusItem } from '../../modules/dashboard/types'

const props = defineProps<{
  systemEyebrow: string
  systemName: string
  navItems: readonly DashboardNavItem[]
  statusItems: readonly DashboardStatusItem[]
}>()

const normalizedNavItems = computed(() =>
  props.navItems.map((item, index) => ({
    ...item,
    active: item.active ?? index === 0,
  })),
)
</script>

<template>
  <header class="topbar">
    <div class="system-block">
      <span class="eyebrow">{{ props.systemEyebrow }}</span>
      <h1 class="system-name">{{ props.systemName }}</h1>
    </div>

    <nav class="topbar-nav" aria-label="一级模块">
      <a
        v-for="item in normalizedNavItems"
        :key="item.label"
        :href="item.href ?? '#'"
        class="topbar-nav__item"
        :class="{
          'is-active': item.active,
          'is-expert': item.expert,
        }"
        :aria-current="item.active ? 'page' : undefined"
      >
        {{ item.label }}
      </a>
    </nav>

    <div class="status-bar">
      <div
        v-for="item in props.statusItems"
        :key="`${item.label}-${item.value}`"
        class="status-chip"
        :class="{ 'is-alert': item.tone === 'alert' }"
      >
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>
  </header>
</template>

<style scoped>
.topbar {
  position: relative;
  overflow: hidden;
}

.topbar::after {
  content: '';
  position: absolute;
  inset: auto 0 0;
  block-size: 1px;
  background: linear-gradient(90deg, transparent, rgba(110, 202, 212, 0.3), transparent);
  opacity: 0.5;
}

.system-name {
  letter-spacing: 0.12em;
  text-shadow: 0 0 18px rgba(110, 202, 212, 0.06);
}

.topbar-nav {
  flex-wrap: wrap;
  align-items: center;
}

.topbar-nav__item {
  position: relative;
  overflow: hidden;
  transition:
    border-color 180ms ease,
    color 180ms ease,
    background-color 180ms ease,
    box-shadow 180ms ease;
}

.topbar-nav__item::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(110, 202, 212, 0.05), transparent 62%);
  opacity: 0;
  transition: opacity 180ms ease;
}

.topbar-nav__item:hover::before,
.topbar-nav__item.is-active::before,
.topbar-nav__item.is-expert::before {
  opacity: 1;
}

.topbar-nav__item.is-expert {
  color: #ffd39b;
  border-color: rgba(231, 104, 45, 0.28);
  background: linear-gradient(135deg, rgba(231, 104, 45, 0.14), rgba(110, 202, 212, 0.08));
  box-shadow: 0 0 0 1px rgba(231, 104, 45, 0.08);
}

.status-chip.is-alert {
  border-color: rgba(231, 104, 45, 0.24);
  background: linear-gradient(180deg, rgba(231, 104, 45, 0.08), rgba(255, 255, 255, 0.02));
}
</style>
