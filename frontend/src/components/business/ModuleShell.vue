<script setup lang="ts">
import { computed } from 'vue'
import TopBar from '../shell/TopBar.vue'
import { topNavItems } from '../../data/mockShell'

const props = defineProps<{
  activePath: string
  eyebrow: string
  title: string
  opsLabel?: string
  opsValue?: string
  expertMode?: boolean
}>()

const navItems = computed(() =>
  topNavItems.map((item) => ({
    label: item.label,
    href: item.href,
    expert: 'expert' in item ? item.expert : undefined,
    active:
      props.activePath === item.href ||
      (item.href !== '/dashboard' && props.activePath.startsWith(`${item.href}/`)),
  })),
)
</script>

<template>
  <section class="business-page" :class="{ 'is-expert': props.expertMode }">
    <div class="business-frame">
      <TopBar
        :nav-items="navItems"
      />

      <main class="business-main">
        <slot />
      </main>
    </div>
  </section>
</template>

<style scoped>
.business-page {
  position: relative;
  min-block-size: 100dvh;
  padding: var(--space-5);
  background-image:
    linear-gradient(rgba(96, 121, 139, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(96, 121, 139, 0.055) 1px, transparent 1px);
  background-size: 28px 28px;
  animation: business-grid-drift 26s linear infinite;
}

.business-page::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 18% 12%, rgba(110, 202, 212, 0.055), transparent 24%),
    radial-gradient(circle at 84% 20%, rgba(231, 104, 45, 0.06), transparent 22%);
  animation: business-scan 7s ease-in-out infinite alternate;
}

.business-page.is-expert::before {
  background:
    radial-gradient(circle at 20% 14%, rgba(231, 104, 45, 0.07), transparent 22%),
    radial-gradient(circle at 78% 18%, rgba(110, 202, 212, 0.055), transparent 24%);
}

.business-page.is-expert {
  block-size: 100dvh;
  min-block-size: 0;
  overflow: hidden;
}

.business-frame {
  position: relative;
  z-index: 1;
  min-block-size: calc(100dvh - var(--space-5) * 2);
  display: grid;
  grid-template-rows: 72px minmax(0, 1fr);
  border: 1px solid var(--color-line);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.014), rgba(255, 255, 255, 0.004)),
    var(--color-bg-elevated);
  box-shadow: var(--shadow-shell);
}

.business-page.is-expert .business-frame {
  block-size: 100%;
  min-block-size: 0;
}

.business-main {
  min-width: 0;
  min-height: 0;
  overflow: auto;
  padding: var(--space-4);
}

@keyframes business-grid-drift {
  from {
    background-position: 0 0;
  }

  to {
    background-position: 28px 28px;
  }
}

@keyframes business-scan {
  from {
    opacity: 0.7;
    transform: translateY(-6px);
  }

  to {
    opacity: 1;
    transform: translateY(6px);
  }
}

.business-page.is-expert .business-main {
  display: grid;
  grid-template-rows: minmax(0, 1fr);
  overflow: hidden;
}

@media (max-width: 1179px) {
  .business-frame {
    grid-template-rows: auto minmax(0, 1fr);
  }
}

@media (max-width: 719px) {
  .business-page {
    padding: var(--space-3);
  }

  .business-main {
    padding: var(--space-3);
  }
}
</style>
