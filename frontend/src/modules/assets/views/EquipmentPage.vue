<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import ModuleShell from '../../../components/business/ModuleShell.vue'
import type { EquipmentIndicator, EquipmentItem, EquipmentQuery } from '../types'
import { loadEquipmentTable } from '../service'

type TableMode = 'paged' | 'long'

const indicators = ref<EquipmentIndicator>({})
const rows = ref<EquipmentItem[]>([])
const selected = ref<EquipmentItem | null>(null)
const isFallback = ref(false)
const showMoreFilters = ref(false)
const tableMode = ref<TableMode>('paged')
const currentPage = ref(1)
const pageSize = ref('50')
const query = reactive<EquipmentQuery>({
  id: '',
  type: '',
  status: '',
  area_id: '',
  pipe_id: '',
  pipe_segment_id: '',
  responsible: '',
  last_check_start: '',
  last_check_end: '',
})

const hasPrevPage = computed(() => currentPage.value > 1)
const hasNextPage = computed(() => rows.value.length >= Number(pageSize.value))

const typeOptions = [
  { label: '温度传感器', value: '0' },
  { label: '压力传感器', value: '1' },
  { label: '流量传感器', value: '2' },
  { label: '振动传感器', value: '3' },
  { label: '位移传感器', value: '4' },
]

const statusOptions = [
  { label: '故障', value: '0' },
  { label: '正常', value: '1' },
  { label: '维护中', value: '2' },
]

function stateClass(status: string) {
  if (status.includes('故障')) return 'is-danger'
  if (status.includes('维护')) return 'is-warn'
  return 'is-good'
}

function normalizeField(value: string | undefined) {
  const normalized = value?.trim()
  return normalized || undefined
}

function normalizeDateTime(value: string | undefined) {
  return normalizeField(value)?.replace('T', ' ')
}

function buildQuery(page: number): EquipmentQuery {
  return {
    page: String(page),
    page_size: tableMode.value === 'long' ? '500' : pageSize.value,
    id: normalizeField(query.id),
    type: normalizeField(query.type),
    status: normalizeField(query.status),
    area_id: normalizeField(query.area_id),
    pipe_id: normalizeField(query.pipe_id),
    pipe_segment_id: normalizeField(query.pipe_segment_id),
    responsible: normalizeField(query.responsible),
    last_check_start: normalizeDateTime(query.last_check_start),
    last_check_end: normalizeDateTime(query.last_check_end),
  }
}

onMounted(async () => {
  await refreshTable()
})

async function loadRows(page = 1) {
  const model = await loadEquipmentTable(buildQuery(page))
  indicators.value = model.indicators
  rows.value = model.rows
  isFallback.value = model.isFallback
  currentPage.value = page
}

async function refreshTable() {
  await loadRows(1)
}

async function applyFilter() {
  await loadRows(1)
}

async function resetFilter() {
  query.id = ''
  query.type = ''
  query.status = ''
  query.area_id = ''
  query.pipe_id = ''
  query.pipe_segment_id = ''
  query.responsible = ''
  query.last_check_start = ''
  query.last_check_end = ''
  await refreshTable()
}

async function switchMode(mode: TableMode) {
  tableMode.value = mode
  await loadRows(1)
}

async function goPage(page: number) {
  if (page < 1) return
  await loadRows(page)
}
</script>

<template>
  <ModuleShell active-path="/assets/equipment" eyebrow="Asset Console" title="设备详情二维表">
    <div class="page-headline">
      <div>
        <span class="eyebrow">Equipment Grid</span>
        <h2>设备全量数据</h2>
      </div>
      <div class="button-strip">
        <button class="control-button" :class="{ 'is-primary': tableMode === 'paged' }" @click="switchMode('paged')">分页</button>
        <button class="control-button" :class="{ 'is-primary': tableMode === 'long' }" @click="switchMode('long')">长屏</button>
        <a class="control-button" href="/assets">资产首页</a>
      </div>
    </div>

    <section class="equipment-metrics">
      <article class="metric-card"><span>设备总数</span><strong>{{ indicators.total || rows.length }}</strong></article>
      <article class="metric-card is-signal"><span>正常设备</span><strong>{{ indicators.normal || 0 }}</strong></article>
      <article class="metric-card is-warn"><span>维护中</span><strong>{{ indicators.maintenance || 0 }}</strong></article>
      <article class="metric-card is-danger"><span>故障设备</span><strong>{{ indicators.fault || 0 }}</strong></article>
      <article class="metric-card is-danger"><span>离线设备</span><strong>{{ indicators.offline || 0 }}</strong></article>
    </section>

    <section class="table-panel table-gap">
      <div class="equipment-filter">
        <div class="filter-grid primary-filters">
          <label class="filter-field">
            <span>设备ID</span>
            <input v-model="query.id" type="text" placeholder="id" />
          </label>
          <label class="filter-field">
            <span>状态</span>
            <select v-model="query.status">
              <option value="">全部状态</option>
              <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label class="filter-field">
            <span>管段ID</span>
            <input v-model="query.pipe_segment_id" type="text" placeholder="pipe_segment_id" />
          </label>
          <label class="filter-field page-size-field">
            <span>每页</span>
            <select v-model="pageSize" :disabled="tableMode === 'long'">
              <option value="20">20</option>
              <option value="50">50</option>
              <option value="100">100</option>
            </select>
          </label>
        </div>
        <div v-if="showMoreFilters" class="filter-grid secondary-filters">
          <label class="filter-field">
            <span>设备类型</span>
            <select v-model="query.type">
              <option value="">全部类型</option>
              <option v-for="item in typeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label class="filter-field">
            <span>区域ID</span>
            <input v-model="query.area_id" type="text" placeholder="area_id" />
          </label>
          <label class="filter-field">
            <span>管线ID</span>
            <input v-model="query.pipe_id" type="text" placeholder="pipe_id" />
          </label>
          <label class="filter-field">
            <span>负责人ID</span>
            <input v-model="query.responsible" type="text" placeholder="responsible" />
          </label>
          <label class="filter-field">
            <span>检查开始</span>
            <input v-model="query.last_check_start" type="datetime-local" />
          </label>
          <label class="filter-field">
            <span>检查结束</span>
            <input v-model="query.last_check_end" type="datetime-local" />
          </label>
        </div>
        <div class="filter-actions">
          <button class="control-button" @click="showMoreFilters = !showMoreFilters">{{ showMoreFilters ? '收起筛选' : '更多筛选' }}</button>
          <button class="control-button is-primary" @click="applyFilter">筛选</button>
          <button class="control-button" @click="resetFilter">重置</button>
          <span class="chip" :class="{ 'is-warn': isFallback }">{{ isFallback ? 'Fallback' : 'API' }}</span>
        </div>
      </div>
      <div class="data-table-wrap equipment-table-wrap" :class="{ 'is-long-mode': tableMode === 'long' }">
        <table class="data-table">
          <thead>
            <tr><th>ID</th><th>类型</th><th>区域</th><th>管线</th><th>管段</th><th>状态</th><th>最近巡检</th><th>负责人</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.id }}</td>
              <td>{{ row.type }}</td>
              <td>{{ row.area_name }}</td>
              <td>{{ row.pipe_name }}</td>
              <td>{{ row.pipe_segment_name || '-' }}</td>
              <td><span class="state-pill" :class="stateClass(row.status)">{{ row.status }}</span></td>
              <td>{{ row.last_check }}</td>
              <td>{{ row.responsible }}</td>
              <td><button class="text-button" @click="selected = row">详情</button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="tableMode === 'paged'" class="pagination-bar">
        <span class="muted-text">第 {{ currentPage }} 页，每页 {{ pageSize }} 条</span>
        <div class="button-strip">
          <button class="control-button" :disabled="!hasPrevPage" @click="goPage(currentPage - 1)">上一页</button>
          <button class="control-button" :disabled="!hasNextPage" @click="goPage(currentPage + 1)">下一页</button>
        </div>
      </div>
      <div v-else class="pagination-bar">
        <span class="muted-text">长屏模式：单次加载 500 条，表格区域内滚动</span>
      </div>
    </section>

    <aside v-if="selected" class="detail-drawer">
      <div class="panel-header"><h3 class="section-title-text">设备详情</h3><button class="text-button" @click="selected = null">关闭</button></div>
      <div class="kv-list">
        <div class="kv-item"><span>管段</span><strong>{{ selected.pipe_segment_name || '-' }}</strong></div>
        <div class="kv-item"><span>管段ID</span><strong>{{ selected.pipe_segment_id || '-' }}</strong></div>
        <div v-for="value, key in selected" :key="key" class="kv-item"><span>{{ key }}</span><strong>{{ value }}</strong></div>
      </div>
    </aside>
  </ModuleShell>
</template>

<style scoped>
.table-gap {
  margin-top: var(--space-3);
}

.equipment-metrics {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--space-3);
}

.equipment-filter {
  display: grid;
  gap: var(--space-3);
}

.primary-filters {
  grid-template-columns: 1.2fr 1fr 1.2fr 0.8fr;
}

.secondary-filters {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.filter-actions,
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.page-size-field select:disabled {
  opacity: 0.48;
}

.equipment-table-wrap {
  max-height: 560px;
}

.equipment-table-wrap.is-long-mode {
  max-height: 72vh;
}

@media (max-width: 1180px) {
  .equipment-metrics,
  .primary-filters,
  .secondary-filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .equipment-metrics,
  .primary-filters,
  .secondary-filters {
    grid-template-columns: 1fr;
  }

  .filter-actions,
  .pagination-bar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
