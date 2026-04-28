<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import ModuleShell from '../../../components/business/ModuleShell.vue'
import type { LogIndicator, LogItem, PagedLogs } from '../types'
import { loadLogs } from '../service'

const indicators = ref<LogIndicator>({})
const page = ref<PagedLogs>({ records: [], total: 0, page: 1, pageSize: 50 })
const selected = ref<LogItem | null>(null)
const mode = ref<'page' | 'scroll'>('page')
const isFallback = ref(false)
const query = reactive({
  area_id: '',
  type: '',
  status: '',
  operation_time_min: '',
  operation_time_max: '',
})

function toTimestamp(value: string) {
  if (!value) return undefined
  const timestamp = new Date(value).getTime()
  return Number.isNaN(timestamp) ? undefined : timestamp
}

function statusClass(status: string | number | null | undefined) {
  const normalized = String(status ?? '')
  return normalized.includes('失败') ? 'is-danger' : 'is-good'
}

async function refreshLogs() {
  const model = await loadLogs({
    page: 1,
    page_size: 50,
    area_id: query.area_id ? Number(query.area_id) : undefined,
    type: query.type ? Number(query.type) : undefined,
    status: query.status ? Number(query.status) : undefined,
    operation_time_min: toTimestamp(query.operation_time_min),
    operation_time_max: toTimestamp(query.operation_time_max),
  })

  indicators.value = model.indicators
  page.value = model.page
  isFallback.value = model.isFallback
}

function resetFilters() {
  query.area_id = ''
  query.type = ''
  query.status = ''
  query.operation_time_min = ''
  query.operation_time_max = ''
  void refreshLogs()
}

onMounted(async () => {
  await refreshLogs()
})
</script>

<template>
  <ModuleShell active-path="/logs" eyebrow="System Logs" title="日志查询与详情" ops-label="日志数据" :ops-value="isFallback ? 'Fallback' : 'API'">
    <div class="page-headline">
      <div>
        <span class="eyebrow">High Density</span>
        <h2>系统日志列表</h2>
      </div>
      <div class="button-strip">
        <button class="control-button" :class="{ 'is-primary': mode === 'page' }" @click="mode = 'page'">分页模式</button>
        <button class="control-button" :class="{ 'is-primary': mode === 'scroll' }" @click="mode = 'scroll'">滚动模式</button>
      </div>
    </div>

    <section class="grid-auto">
      <article class="metric-card"><span>日志总数</span><strong>{{ indicators.total || indicators.all || page.total }}</strong></article>
      <article class="metric-card is-signal"><span>今日日志</span><strong>{{ indicators.today || 0 }}</strong></article>
      <article class="metric-card"><span>成功</span><strong>{{ indicators.success || 0 }}</strong></article>
      <article class="metric-card is-danger"><span>失败</span><strong>{{ indicators.failed || 0 }}</strong></article>
    </section>

    <section class="table-panel log-table-panel">
      <div class="panel-header">
        <div class="filter-grid">
          <label class="filter-field">
            <span>区域</span>
            <input v-model="query.area_id" type="number" placeholder="area_id" />
          </label>
          <label class="filter-field">
            <span>用户类型</span>
            <select v-model="query.type">
              <option value="">全部</option>
              <option value="0">管理员</option>
              <option value="1">检修员</option>
            </select>
          </label>
          <label class="filter-field">
            <span>操作状态</span>
            <select v-model="query.status">
              <option value="">全部</option>
              <option value="0">成功</option>
              <option value="1">失败</option>
              <option value="2">告警</option>
              <option value="3">调试</option>
            </select>
          </label>
          <label class="filter-field">
            <span>开始时间</span>
            <input v-model="query.operation_time_min" type="datetime-local" />
          </label>
          <label class="filter-field">
            <span>结束时间</span>
            <input v-model="query.operation_time_max" type="datetime-local" />
          </label>
          <div class="field-like">耗时均值：{{ indicators.avg_duration || 0 }}ms</div>
        </div>
        <div class="button-strip">
          <button class="control-button is-primary" @click="refreshLogs">筛选</button>
          <button class="control-button" @click="resetFilters">重置</button>
        </div>
        <span class="chip" :class="{ 'is-warn': isFallback }">{{ isFallback ? 'Fallback' : 'API' }}</span>
      </div>
      <div class="data-table-wrap" :class="{ 'is-scroll-mode': mode === 'scroll' }">
        <table class="data-table logs-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>时间</th>
              <th>用户</th>
              <th>用户类型</th>
              <th>操作</th>
              <th>IP</th>
              <th>耗时</th>
              <th>状态</th>
              <th>详情</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in page.records" :key="row.id">
              <td>{{ row.id }}</td>
              <td>{{ row.create_time }}</td>
              <td>{{ row.user_name }}</td>
              <td>{{ row.user_type }}</td>
              <td>{{ row.operation }}</td>
              <td>{{ row.ip }}</td>
              <td>{{ row.duration }}ms</td>
              <td><span class="state-pill" :class="statusClass(row.status)">{{ row.status }}</span></td>
              <td>{{ row.details }}</td>
              <td><button class="text-button" @click="selected = row">详情</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <aside v-if="selected" class="detail-drawer">
      <div class="panel-header">
        <h3 class="section-title-text">日志详情</h3>
        <button class="text-button" @click="selected = null">关闭</button>
      </div>
      <div class="kv-list">
        <div v-for="value, key in selected" :key="key" class="kv-item">
          <span>{{ key }}</span>
          <strong>{{ value || '-' }}</strong>
        </div>
      </div>
    </aside>
  </ModuleShell>
</template>

<style scoped>
.log-table-panel {
  margin-top: var(--space-3);
}

.logs-table {
  min-width: 1080px;
}

.is-scroll-mode {
  max-height: 440px;
}
</style>
