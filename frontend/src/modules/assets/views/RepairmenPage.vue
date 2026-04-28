<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import ModuleShell from '../../../components/business/ModuleShell.vue'
import type { RepairmanIndicator, RepairmanItem, RepairmanQuery } from '../types'
import { loadRepairmanTable } from '../service'

type TableMode = 'paged' | 'long'

const indicators = ref<RepairmanIndicator>({})
const rows = ref<RepairmanItem[]>([])
const selected = ref<RepairmanItem | null>(null)
const isFallback = ref(false)
const showMoreFilters = ref(false)
const tableMode = ref<TableMode>('paged')
const currentPage = ref(1)
const pageSize = ref(50)
const query = reactive({
  name: '',
  sex: '',
  phone: '',
  min_age: '',
  max_age: '',
  area_id: '',
  entry_start_time: '',
  entry_end_time: '',
})

const hasPrevPage = computed(() => currentPage.value > 1)
const hasNextPage = computed(() => rows.value.length >= pageSize.value)

function sexText(sex: string) {
  if (sex === '0') return '男'
  if (sex === '1') return '女'
  return '未知'
}

function normalizeField(value: string) {
  const normalized = value.trim()
  return normalized || undefined
}

function normalizeDateTime(value: string) {
  return normalizeField(value)?.replace('T', ' ')
}

function buildQuery(page: number): RepairmanQuery {
  return {
    page,
    page_size: tableMode.value === 'long' ? 500 : pageSize.value,
    name: normalizeField(query.name),
    sex: normalizeField(query.sex),
    phone: normalizeField(query.phone),
    min_age: normalizeField(query.min_age),
    max_age: normalizeField(query.max_age),
    area_id: normalizeField(query.area_id),
    entry_start_time: normalizeDateTime(query.entry_start_time),
    entry_end_time: normalizeDateTime(query.entry_end_time),
  }
}

onMounted(async () => {
  await loadRows(1)
})

async function loadRows(page = 1) {
  const model = await loadRepairmanTable(buildQuery(page))
  indicators.value = model.indicators
  rows.value = model.rows
  isFallback.value = model.isFallback
  currentPage.value = page
}

async function applyFilter() {
  await loadRows(1)
}

async function resetFilter() {
  query.name = ''
  query.sex = ''
  query.phone = ''
  query.min_age = ''
  query.max_age = ''
  query.area_id = ''
  query.entry_start_time = ''
  query.entry_end_time = ''
  await loadRows(1)
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
  <ModuleShell active-path="/assets/repairmen" eyebrow="Asset Console" title="检修员详情二维表">
    <div class="page-headline">
      <div>
        <span class="eyebrow">Repairmen Grid</span>
        <h2>检修员全量数据</h2>
      </div>
      <div class="button-strip">
        <button class="control-button" :class="{ 'is-primary': tableMode === 'paged' }" @click="switchMode('paged')">分页</button>
        <button class="control-button" :class="{ 'is-primary': tableMode === 'long' }" @click="switchMode('long')">长屏</button>
        <a class="control-button" href="/assets">资产首页</a>
      </div>
    </div>

    <section class="repairman-metrics">
      <article class="metric-card">
        <span>总人数</span>
        <strong>{{ indicators.total || rows.length }}<small>本月新增 {{ indicators.new_this_month || 0 }}</small></strong>
      </article>
      <article class="metric-card is-signal"><span>男性</span><strong>{{ indicators.male || 0 }}</strong></article>
      <article class="metric-card is-warn"><span>女性</span><strong>{{ indicators.female || 0 }}</strong></article>
      <article class="metric-card"><span>平均年龄</span><strong>{{ indicators.avg_age || 0 }}</strong></article>
    </section>

    <section class="table-panel table-gap">
      <div class="repairman-filter">
        <div class="filter-grid primary-filters">
          <label class="filter-field">
            <span>姓名</span>
            <input v-model="query.name" type="text" placeholder="name" />
          </label>
          <label class="filter-field">
            <span>性别</span>
            <select v-model="query.sex">
              <option value="">全部性别</option>
              <option value="0">男</option>
              <option value="1">女</option>
            </select>
          </label>
          <label class="filter-field">
            <span>手机号</span>
            <input v-model="query.phone" type="text" placeholder="phone" />
          </label>
          <label class="filter-field page-size-field">
            <span>每页</span>
            <select v-model="pageSize" :disabled="tableMode === 'long'">
              <option :value="20">20</option>
              <option :value="50">50</option>
              <option :value="100">100</option>
            </select>
          </label>
        </div>
        <div v-if="showMoreFilters" class="filter-grid secondary-filters">
          <label class="filter-field">
            <span>最小年龄</span>
            <input v-model="query.min_age" type="number" min="0" placeholder="min_age" />
          </label>
          <label class="filter-field">
            <span>最大年龄</span>
            <input v-model="query.max_age" type="number" min="0" placeholder="max_age" />
          </label>
          <label class="filter-field">
            <span>区域ID</span>
            <input v-model="query.area_id" type="text" placeholder="area_id" />
          </label>
          <label class="filter-field">
            <span>入职开始</span>
            <input v-model="query.entry_start_time" type="datetime-local" />
          </label>
          <label class="filter-field">
            <span>入职结束</span>
            <input v-model="query.entry_end_time" type="datetime-local" />
          </label>
        </div>
        <div class="filter-actions">
          <button class="control-button" @click="showMoreFilters = !showMoreFilters">{{ showMoreFilters ? '收起筛选' : '更多筛选' }}</button>
          <button class="control-button is-primary" @click="applyFilter">筛选</button>
          <button class="control-button" @click="resetFilter">重置</button>
          <span class="chip" :class="{ 'is-warn': isFallback }">{{ isFallback ? 'Fallback' : 'API' }}</span>
        </div>
      </div>
      <div class="data-table-wrap repairman-table-wrap" :class="{ 'is-long-mode': tableMode === 'long' }">
        <table class="data-table">
          <thead>
            <tr><th>ID</th><th>姓名</th><th>年龄</th><th>性别</th><th>负责区域</th><th>手机号</th><th>入职时间</th><th>状态</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.id }}</td>
              <td>{{ row.name }}</td>
              <td>{{ row.age }}</td>
              <td>{{ sexText(row.sex) }}</td>
              <td>{{ row.area_name }}</td>
              <td>{{ row.phone }}</td>
              <td>{{ row.entry_time }}</td>
              <td><span class="state-pill is-good">在册</span></td>
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
      <div class="panel-header"><h3 class="section-title-text">检修员详情</h3><button class="text-button" @click="selected = null">关闭</button></div>
      <div class="kv-list">
        <div v-for="value, key in selected" :key="key" class="kv-item"><span>{{ key }}</span><strong>{{ value }}</strong></div>
      </div>
    </aside>
  </ModuleShell>
</template>

<style scoped>
.table-gap {
  margin-top: var(--space-3);
}

.repairman-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-3);
}

.repairman-filter {
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

.repairman-table-wrap {
  max-height: 560px;
}

.repairman-table-wrap.is-long-mode {
  max-height: 72vh;
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

@media (max-width: 1180px) {
  .repairman-metrics,
  .primary-filters,
  .secondary-filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .repairman-metrics,
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
