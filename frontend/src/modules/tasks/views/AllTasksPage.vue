<script setup lang="ts">
import { onMounted, ref } from 'vue'
import ModuleShell from '../../../components/business/ModuleShell.vue'
import type { PagedTasks, TaskItem, TaskQuery } from '../types'
import { loadTaskTable } from '../service'

const page = ref<PagedTasks>({ records: [], total: 0, page: 1, pageSize: 20 })
const selected = ref<TaskItem | null>(null)
const isFallback = ref(false)
const query = ref<{ pipe_segment_id: string }>({ pipe_segment_id: '' })

function priorityClass(priority: string) {
  if (priority.includes('紧急')) return 'is-danger'
  if (priority.includes('高') || priority.includes('中')) return 'is-warn'
  return 'is-good'
}

onMounted(async () => {
  await refreshTable()
})

async function refreshTable() {
  const model = await loadTaskTable()
  page.value = model.page
  isFallback.value = model.isFallback
}

async function applyFilter() {
  const nextQuery: TaskQuery = { page: 1, page_size: 20 }
  const segmentId = Number(query.value.pipe_segment_id)

  if (query.value.pipe_segment_id.trim() && !Number.isNaN(segmentId)) {
    nextQuery.pipe_segment_id = segmentId
  }

  const model = await loadTaskTable(nextQuery)
  page.value = model.page
  isFallback.value = model.isFallback
}

async function resetFilter() {
  query.value.pipe_segment_id = ''
  await refreshTable()
}
</script>

<template>
  <ModuleShell active-path="/tasks/all" eyebrow="Task Data Grid" title="所有任务二维表">
    <div class="page-headline">
      <div>
        <span class="eyebrow">Dense Table</span>
        <h2>任务全量列表</h2>
      </div>
      <a class="control-button" href="/tasks">任务工作台</a>
    </div>

    <section class="table-panel">
      <div class="panel-header">
        <div class="filter-grid">
          <div class="field-like">任务标题：异常复核</div>
          <label class="filter-field">
            <span>管段ID</span>
            <input v-model="query.pipe_segment_id" type="number" placeholder="pipe_segment_id" />
          </label>
          <button class="control-button is-primary" @click="applyFilter">筛选</button>
          <button class="control-button" @click="resetFilter">重置</button>
        </div>
        <span class="chip" :class="{ 'is-warn': isFallback }">{{ isFallback ? 'Fallback' : 'API' }}</span>
      </div>
      <div class="data-table-wrap">
        <table class="data-table">
          <thead>
            <tr><th>ID</th><th>标题</th><th>类型</th><th>优先级</th><th>状态</th><th>区域</th><th>管线</th><th>管段</th><th>负责人</th><th>发布时间</th><th>响应时间</th><th>完成时间</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="row in page.records" :key="row.id">
              <td>{{ row.id }}</td>
              <td>{{ row.title }}</td>
              <td>{{ row.type }}</td>
              <td><span class="state-pill" :class="priorityClass(row.priority)">{{ row.priority }}</span></td>
              <td>{{ row.status }}</td>
              <td>{{ row.area_name }}</td>
              <td>{{ row.pipe_name }}</td>
              <td>{{ row.pipe_segment_name || '-' }}</td>
              <td>{{ row.assignee }}</td>
              <td>{{ row.public_time }}</td>
              <td>{{ row.response_time || '-' }}</td>
              <td>{{ row.accomplish_time || '-' }}</td>
              <td><button class="text-button" @click="selected = row">详情</button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="muted-text">显示 {{ page.records.length }} / {{ page.total }}</p>
    </section>

    <aside v-if="selected" class="detail-drawer">
      <div class="panel-header"><h3 class="section-title-text">任务详情/编辑</h3><button class="text-button" @click="selected = null">关闭</button></div>
      <div class="kv-list">
        <div class="kv-item"><span>管段</span><strong>{{ selected.pipe_segment_name || '-' }}</strong></div>
        <div class="kv-item"><span>管段ID</span><strong>{{ selected.pipe_segment_id || '-' }}</strong></div>
        <div v-for="value, key in selected" :key="key" class="kv-item"><span>{{ key }}</span><strong>{{ value || '-' }}</strong></div>
      </div>
    </aside>
  </ModuleShell>
</template>
