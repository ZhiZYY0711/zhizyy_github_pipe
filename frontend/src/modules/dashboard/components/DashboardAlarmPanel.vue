<script setup lang="ts">
import { ref } from 'vue'
import type { DashboardAlarm } from '../types'

defineProps<{
  alarms: DashboardAlarm[]
  loading: boolean
}>()

const selectedAlarm = ref<DashboardAlarm | null>(null)

const detailFields: Array<{ label: string; key: keyof DashboardAlarm }> = [
  { label: '报警级别', key: 'level' },
  { label: '报警时间', key: 'time' },
  { label: '报警消息', key: 'message' },
  { label: '传感器ID', key: 'sensor_id' },
  { label: '传感器名称', key: 'sensor_name' },
  { label: '区域ID', key: 'area_id' },
  { label: '区域名称', key: 'area_name' },
  { label: '流水ID', key: 'id' },
]
</script>

<template>
  <section class="panel side-panel alarm-panel">
    <div class="panel-head">
      <h2 class="panel-title">报警流水</h2>
      <span class="eyebrow">Running Alarm</span>
    </div>

    <div class="alarm-panel__body console-scrollbar">
      <div v-if="loading" class="alarm-empty">正在读取报警流水</div>
      <button
        v-for="alarm in alarms.slice(0, 8)"
        v-else
        :key="alarm.id || `${alarm.sensor_id}-${alarm.time}`"
        class="alarm-card"
        :class="{ 'is-critical': alarm.level === '高危' }"
        type="button"
        @click="selectedAlarm = alarm"
      >
        <span class="alarm-level">{{ alarm.level }}</span>
        <div class="alarm-card__main">
          <strong>{{ alarm.message }}</strong>
          <small>{{ alarm.area_name }} / {{ alarm.sensor_name || alarm.sensor_id || '未知传感器' }}</small>
        </div>
        <time>{{ alarm.time }}</time>
        <span class="alarm-more">详情</span>
      </button>
      <div v-if="!loading && alarms.length === 0" class="alarm-empty">暂无报警流水</div>
    </div>

    <Teleport to="body">
      <div v-if="selectedAlarm" class="alarm-modal" role="dialog" aria-modal="true" @click.self="selectedAlarm = null">
        <section class="alarm-modal__panel">
          <div class="alarm-modal__head">
            <div>
              <span class="eyebrow">Alarm Detail</span>
              <h3>报警流水详情</h3>
            </div>
            <button class="text-button" type="button" @click="selectedAlarm = null">关闭</button>
          </div>
          <div class="alarm-modal__body">
            <div v-for="field in detailFields" :key="field.key" class="alarm-detail-row">
              <span>{{ field.label }}</span>
              <strong>{{ selectedAlarm[field.key] || '-' }}</strong>
            </div>
          </div>
        </section>
      </div>
    </Teleport>
  </section>
</template>

<style scoped>
.alarm-panel__body {
  min-height: 0;
  padding: 12px;
  display: grid;
  gap: 8px;
  align-content: start;
  overflow: auto;
}

.alarm-card {
  position: relative;
  overflow: hidden;
  min-height: 72px;
  padding: 10px 12px 10px 16px;
  border: 1px solid var(--color-line);
  color: var(--color-text);
  background: rgba(255, 255, 255, 0.02);
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  grid-template-rows: auto auto;
  align-items: center;
  column-gap: 10px;
  row-gap: 4px;
  text-align: left;
  transition: transform 180ms ease, border-color 180ms ease, background 180ms ease;
}

.alarm-card::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  inline-size: 3px;
  background: rgba(221, 176, 84, 0.66);
}

.alarm-card.is-critical {
  border-color: rgba(231, 104, 45, 0.24);
  background: linear-gradient(180deg, rgba(231, 104, 45, 0.09), rgba(255, 255, 255, 0.01));
}

.alarm-card.is-critical::before {
  background: rgba(231, 104, 45, 0.74);
}

.alarm-card:hover {
  transform: translateY(-2px);
  border-color: rgba(110, 202, 212, 0.26);
  background: rgba(110, 202, 212, 0.045);
}

.alarm-level {
  grid-row: 1 / 3;
  color: var(--color-accent-orange);
  font-size: var(--text-body-sm);
  font-weight: 700;
}

.alarm-card__main {
  min-width: 0;
  display: grid;
  gap: 5px;
}

.alarm-card__main strong {
  min-width: 0;
  overflow: hidden;
  color: var(--color-text);
  font-size: var(--text-body-sm);
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alarm-card__main small,
.alarm-card time,
.alarm-more,
.alarm-empty {
  color: var(--color-text-muted);
  font-size: var(--text-meta);
}

.alarm-card__main small {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alarm-card time {
  justify-self: end;
  white-space: nowrap;
}

.alarm-more {
  justify-self: end;
  color: var(--color-accent-cyan);
  letter-spacing: var(--tracking-panel);
  text-transform: uppercase;
}

.alarm-empty {
  padding: 20px 14px;
  border: 1px dashed var(--color-line);
  text-align: center;
}

.alarm-modal {
  position: fixed;
  z-index: 80;
  inset: 0;
  display: grid;
  place-items: center;
  padding: var(--space-5);
  background: rgba(2, 7, 12, 0.68);
  backdrop-filter: blur(8px);
}

.alarm-modal__panel {
  width: min(620px, 100%);
  border: 1px solid rgba(110, 202, 212, 0.28);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.035), rgba(255, 255, 255, 0.012)),
    var(--color-bg-elevated);
  box-shadow: var(--shadow-card);
}

.alarm-modal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-4);
  border-bottom: 1px solid var(--color-line);
}

.alarm-modal__head h3 {
  margin: 4px 0 0;
  font-family: var(--font-serif);
  font-size: var(--text-heading-md);
}

.alarm-modal__body {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-4);
}

.alarm-detail-row {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  gap: var(--space-3);
  padding-block: var(--space-2);
  border-bottom: 1px solid var(--color-line);
}

.alarm-detail-row span {
  color: var(--color-text-muted);
  font-size: var(--text-meta);
}

.alarm-detail-row strong {
  min-width: 0;
  overflow-wrap: anywhere;
  color: var(--color-text);
  font-size: var(--text-body-sm);
}
</style>
