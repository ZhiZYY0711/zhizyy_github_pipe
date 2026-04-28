<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { dashboardPageModel } from '../../data/mockShell'
import type { DashboardNavItem } from '../../modules/dashboard/types'
import { useRouter } from '../../router'

const props = defineProps<{
  navItems?: readonly DashboardNavItem[]
}>()
let router: ReturnType<typeof useRouter> | null = null
let clockTimer: number | undefined

try {
  router = useRouter()
} catch {
  router = null
}

const currentTime = ref(formatCurrentTime())
const weatherText = '晴 21℃'

const normalizedNavItems = computed<DashboardNavItem[]>(() =>
  ((props.navItems ?? dashboardPageModel.navItems) as readonly DashboardNavItem[]).map((item, index) => ({
    ...item,
    active: item.active ?? index === 0,
  })),
)

onMounted(() => {
  clockTimer = window.setInterval(() => {
    currentTime.value = formatCurrentTime()
  }, 1000)
})

onBeforeUnmount(() => {
  if (clockTimer !== undefined) {
    window.clearInterval(clockTimer)
  }
})

function formatCurrentTime() {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(new Date())
}

function navigateNav(item: DashboardNavItem) {
  if (item.href) {
    if (router) {
      router.push(item.href)
      return
    }

    if (typeof window !== 'undefined') {
      window.location.href = item.href
    }
  }
}

function refreshPage() {
  window.location.reload()
}
</script>

<template>
  <header class="topbar">
    <div class="system-block">
      <span class="eyebrow">{{ dashboardPageModel.systemEyebrow }}</span>
      <h1 class="system-name">{{ dashboardPageModel.systemName }}</h1>
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
        @click.prevent="navigateNav(item)"
      >
        {{ item.label }}
      </a>
    </nav>

    <div class="topbar-actions" aria-label="全局工具">
      <div class="topbar-actions__weather" aria-label="当前时间和天气">
        <span>{{ currentTime }}</span>
        <strong>{{ weatherText }}</strong>
      </div>
      <button class="topbar-actions__button" type="button" title="刷新页面" @click="refreshPage">
        刷新
      </button>
      <button class="topbar-actions__button" type="button" title="个人主页暂未实现" disabled>
        个人主页
      </button>
    </div>
  </header>
</template>

<style scoped>
.topbar {
  position: relative;
  overflow: hidden;
  grid-template-columns: 290px minmax(0, 1fr) 330px;
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
  min-width: 0;
  align-items: center;
  overflow-x: auto;
  scrollbar-width: none;
}

.topbar-nav::-webkit-scrollbar {
  display: none;
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

.topbar-actions {
  display: grid;
  grid-template-columns: minmax(132px, 1fr) 72px 92px;
  gap: 10px;
  justify-content: end;
  min-width: 0;
}

.topbar-actions__weather,
.topbar-actions__button {
  min-height: 42px;
  border: 1px solid var(--color-line);
  background: rgba(255, 255, 255, 0.018);
}

.topbar-actions__weather {
  display: grid;
  align-content: center;
  padding: 8px 12px;
}

.topbar-actions__weather span,
.topbar-actions__weather strong {
  display: block;
  font-family: var(--font-mono);
}

.topbar-actions__weather span {
  color: var(--color-text);
  font-size: var(--text-body-sm);
}

.topbar-actions__weather strong {
  margin-top: 4px;
  color: var(--color-text-dim);
  font-size: var(--text-micro);
  font-weight: 600;
  letter-spacing: 0.12em;
}

.topbar-actions__button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  color: var(--color-text-muted);
  font-size: var(--text-meta);
  letter-spacing: var(--tracking-panel);
  transition:
    border-color 180ms ease,
    color 180ms ease,
    background-color 180ms ease;
}

.topbar-actions__button:hover:not(:disabled) {
  color: var(--color-text);
  border-color: var(--color-line-strong);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.035), rgba(255, 255, 255, 0.01));
}

.topbar-actions__button:disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

@media (max-width: 1179px) {
  .topbar {
    grid-template-columns: 1fr;
  }

  .topbar-actions {
    grid-template-columns: minmax(132px, max-content) 72px 92px;
    justify-content: start;
  }
}

@media (max-width: 719px) {
  .topbar-actions {
    grid-template-columns: 1fr 72px 92px;
  }
}
</style>
