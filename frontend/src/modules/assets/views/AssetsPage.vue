<script setup lang="ts">
import { onMounted, ref } from 'vue'
import ModuleShell from '../../../components/business/ModuleShell.vue'
import type { EquipmentIndicator, EquipmentItem, RepairmanIndicator, RepairmanItem } from '../types'
import { loadAssetWorkbench } from '../service'

const equipmentIndicators = ref<EquipmentIndicator>({})
const repairmanIndicators = ref<RepairmanIndicator>({})
const equipment = ref<EquipmentItem[]>([])
const repairmen = ref<RepairmanItem[]>([])
const selected = ref<EquipmentItem | RepairmanItem | null>(null)
const isFallback = ref(false)

function stateClass(status: string) {
  if (status.includes('故障')) return 'is-danger'
  if (status.includes('维护')) return 'is-warn'
  return 'is-good'
}

function sexText(sex: string) {
  if (sex === '0') return '男'
  if (sex === '1') return '女'
  return '未知'
}

onMounted(async () => {
  const model = await loadAssetWorkbench()
  equipmentIndicators.value = model.equipmentIndicators
  equipment.value = model.equipment
  repairmanIndicators.value = model.repairmanIndicators
  repairmen.value = model.repairmen
  isFallback.value = model.isFallback
})
</script>

<template>
  <ModuleShell active-path="/assets" eyebrow="Asset Console" title="资产信息工作台" ops-label="双区数据" :ops-value="isFallback ? 'Fallback' : 'API'">
    <section class="asset-stack">
      <article class="table-panel">
        <div class="panel-header">
          <div>
            <span class="eyebrow">Equipment</span>
            <h3 class="panel-title-text">设备信息区</h3>
          </div>
          <a class="control-button detail-link" href="/assets/equipment">进入详情</a>
        </div>
        <div class="metric-grid equipment-summary">
          <article class="metric-card"><span>设备总数</span><strong>{{ equipmentIndicators.total || 0 }}</strong></article>
          <article class="metric-card is-signal"><span>正常设备</span><strong>{{ equipmentIndicators.normal || 0 }}</strong></article>
          <article class="metric-card is-warn"><span>维护中</span><strong>{{ equipmentIndicators.maintenance || 0 }}</strong></article>
          <article class="metric-card is-danger"><span>故障设备</span><strong>{{ equipmentIndicators.fault || 0 }}</strong></article>
          <article class="metric-card is-danger"><span>离线设备</span><strong>{{ equipmentIndicators.offline || 0 }}</strong></article>
        </div>
        <div class="card-grid">
          <button v-for="item in equipment.slice(0, 16)" :key="item.id" class="asset-card" @click="selected = item">
            <strong>{{ item.type }} / ID {{ item.id }}</strong>
            <span class="state-pill" :class="stateClass(item.status)">{{ item.status }}</span>
            <p>{{ item.area_name }} / {{ item.pipe_name }}</p>
            <small>负责人 {{ item.responsible }}</small>
          </button>
        </div>
      </article>

      <article class="table-panel">
        <div class="panel-header">
          <div>
            <span class="eyebrow">Repairmen</span>
            <h3 class="panel-title-text">检修员信息区</h3>
          </div>
          <a class="control-button detail-link" href="/assets/repairmen">进入详情</a>
        </div>
        <div class="metric-grid repairman-summary">
          <article class="metric-card is-signal"><span>男性</span><strong>{{ repairmanIndicators.male || 0 }}</strong></article>
          <article class="metric-card is-warn"><span>女性</span><strong>{{ repairmanIndicators.female || 0 }}</strong></article>
          <article class="metric-card">
            <span>总人数</span>
            <strong>{{ repairmanIndicators.total || 0 }}<small>本月新增 {{ repairmanIndicators.new_this_month || 0 }}</small></strong>
          </article>
          <article class="metric-card"><span>平均年龄</span><strong>{{ repairmanIndicators.avg_age || 0 }}</strong></article>
        </div>
        <div class="card-grid">
          <button v-for="item in repairmen.slice(0, 16)" :key="item.id" class="asset-card" @click="selected = item">
            <strong>{{ item.name }} / {{ item.area_name }}</strong>
            <span class="state-pill is-good">{{ sexText(item.sex) }}</span>
            <p>{{ item.phone }} / 入职 {{ item.entry_time }}</p>
            <small>年龄 {{ item.age }}</small>
          </button>
        </div>
      </article>
    </section>

    <aside v-if="selected" class="detail-drawer">
      <div class="panel-header">
        <h3 class="section-title-text">资产详情抽屉</h3>
        <button class="text-button" @click="selected = null">关闭</button>
      </div>
      <div class="kv-list">
        <div v-for="value, key in selected" :key="key" class="kv-item">
          <span>{{ key }}</span>
          <strong>{{ value }}</strong>
        </div>
      </div>
    </aside>
  </ModuleShell>
</template>

<style scoped>
.asset-stack {
  display: grid;
  gap: var(--space-3);
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-3);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--space-3);
  margin-bottom: var(--space-3);
}

.repairman-summary {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.asset-card {
  min-width: 0;
  min-height: 148px;
  padding: var(--space-4);
  border: 1px solid var(--color-line);
  color: var(--color-text);
  background: rgba(255, 255, 255, 0.018);
  text-align: left;
  display: grid;
  gap: var(--space-3);
  align-content: space-between;
  transition: transform 180ms ease, border-color 180ms ease, background 180ms ease;
}

.asset-card:hover {
  transform: translateY(-2px);
  border-color: rgba(110, 202, 212, 0.28);
  background: rgba(110, 202, 212, 0.045);
}

.asset-card strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-card p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--text-meta);
  line-height: var(--leading-body);
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.asset-card small {
  color: var(--color-text-dim);
  font-size: var(--text-meta);
}

.detail-link {
  min-height: 32px;
}

.metric-card strong {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
}

.metric-card small {
  color: var(--color-text-muted);
  font-size: var(--text-meta);
  font-weight: 500;
}

@media (max-width: 960px) {
  .card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric-grid,
  .repairman-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .card-grid,
  .metric-grid,
  .repairman-summary {
    grid-template-columns: 1fr;
  }
}
</style>
