<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import ModuleShell from '../../../components/business/ModuleShell.vue'
import type { AreaOption, ManoeuvreItem } from '../types'
import {
  loadEmergencyCities,
  loadEmergencyDetail,
  loadEmergencyDistricts,
  loadEmergencyProvinces,
  loadEmergencyTimeline,
} from '../service'

const rows = ref<ManoeuvreItem[]>([])
const selected = ref<ManoeuvreItem | null>(null)
const isFallback = ref(false)
const detailLoading = ref(false)
const provinces = ref<AreaOption[]>([])
const cities = ref<AreaOption[]>([])
const districts = ref<AreaOption[]>([])
const selectedProvince = ref('')
const selectedCity = ref('')
const selectedDistrict = ref('')
const query = reactive({
  type: '',
  status: '',
  start_time: '',
  end_time: '',
})

const typeOptions = [
  { label: '全部类型', value: '' },
  { label: '状态模拟', value: '0' },
  { label: '事故模拟', value: '1' },
  { label: '紧急事件', value: '2' },
  { label: '日常演练', value: '3' },
]

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '成功', value: '0' },
  { label: '失败', value: '1' },
  { label: '进行中', value: '2' },
]

const selectedAreaId = computed(() => selectedDistrict.value || selectedCity.value || selectedProvince.value || '')
const selectedAreaName = computed(() => {
  const district = districts.value.find((item) => item.code === selectedDistrict.value)
  const city = cities.value.find((item) => item.code === selectedCity.value)
  const province = provinces.value.find((item) => item.code === selectedProvince.value)
  return district?.name || city?.name || province?.name || '全国'
})

const completionText = computed(() => {
  if (!selected.value) return '-'
  if (selected.value.result) return selected.value.result
  if (selected.value.status.includes('成功') || selected.value.status.includes('完成')) return '已归档'
  if (selected.value.status.includes('进行')) return '处置中'
  if (selected.value.status.includes('失败')) return '待复盘'
  return '未填报'
})

function stateClass(status: string) {
  if (status.includes('进行')) return 'is-warn'
  if (status.includes('成功')) return 'is-good'
  if (status.includes('失败')) return 'is-danger'
  return ''
}

function toTimestamp(value: string) {
  if (!value) return undefined
  const timestamp = new Date(value).getTime()
  return Number.isNaN(timestamp) ? undefined : timestamp
}

async function refreshTimeline() {
  const previousId = selected.value?.id
  const model = await loadEmergencyTimeline({
    type: query.type ? Number(query.type) : undefined,
    status: query.status ? Number(query.status) : undefined,
    area_id: selectedAreaId.value ? Number(selectedAreaId.value) : undefined,
    start_time_min: toTimestamp(query.start_time),
    end_time_max: toTimestamp(query.end_time),
  })

  rows.value = model.rows
  selected.value = model.rows.find((item) => item.id === previousId) || model.rows[0] || null
  isFallback.value = model.isFallback
}

async function selectManoeuvre(item: ManoeuvreItem) {
  selected.value = item
  detailLoading.value = true
  selected.value = await loadEmergencyDetail(item)
  detailLoading.value = false
}

async function selectProvince(code: string) {
  selectedProvince.value = code
  selectedCity.value = ''
  selectedDistrict.value = ''
  cities.value = code ? await loadEmergencyCities(code).catch(() => []) : []
  districts.value = []
  await refreshTimeline()
}

async function selectCity(code: string) {
  selectedCity.value = code
  selectedDistrict.value = ''
  districts.value = code ? await loadEmergencyDistricts(code).catch(() => []) : []
  await refreshTimeline()
}

async function selectDistrict(code: string) {
  selectedDistrict.value = code
  await refreshTimeline()
}

function resetFilters() {
  query.type = ''
  query.status = ''
  query.start_time = ''
  query.end_time = ''
  selectedProvince.value = ''
  selectedCity.value = ''
  selectedDistrict.value = ''
  cities.value = []
  districts.value = []
  void refreshTimeline()
}

onMounted(async () => {
  provinces.value = await loadEmergencyProvinces().catch(() => [])
  await refreshTimeline()
})
</script>

<template>
  <ModuleShell
    active-path="/emergency"
    eyebrow="Emergency Drill"
    title="应急演练管理"
    ops-label="演练数据"
    :ops-value="isFallback ? 'Fallback' : 'API'"
    expert-mode
  >
    <section class="emergency-page">
      <section class="emergency-filter">
        <div class="filter-meta">
          <span class="chip">{{ selectedAreaName }}</span>
          <span class="chip" :class="{ 'is-warn': isFallback }">{{ isFallback ? 'Fallback' : 'API' }}</span>
        </div>

        <label class="select-field">
          <span>省份</span>
          <select :value="selectedProvince" @change="selectProvince(($event.target as HTMLSelectElement).value)">
            <option value="">全国</option>
            <option v-for="province in provinces" :key="province.code" :value="province.code">{{ province.name }}</option>
          </select>
        </label>

        <label class="select-field">
          <span>城市</span>
          <select :value="selectedCity" :disabled="!selectedProvince" @change="selectCity(($event.target as HTMLSelectElement).value)">
            <option value="">全部城市</option>
            <option v-for="city in cities" :key="city.code" :value="city.code">{{ city.name }}</option>
          </select>
        </label>

        <label class="select-field">
          <span>区县</span>
          <select :value="selectedDistrict" :disabled="!selectedCity" @change="selectDistrict(($event.target as HTMLSelectElement).value)">
            <option value="">全部区县</option>
            <option v-for="district in districts" :key="district.code" :value="district.code">{{ district.name }}</option>
          </select>
        </label>

        <label class="select-field">
          <span>演练类型</span>
          <select v-model="query.type" @change="refreshTimeline">
            <option v-for="option in typeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>

        <label class="select-field">
          <span>状态</span>
          <select v-model="query.status" @change="refreshTimeline">
            <option v-for="option in statusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>

        <label class="select-field time-field">
          <span>开始时间</span>
          <input v-model="query.start_time" type="datetime-local" @change="refreshTimeline" />
        </label>

        <label class="select-field time-field">
          <span>结束时间</span>
          <input v-model="query.end_time" type="datetime-local" @change="refreshTimeline" />
        </label>

        <div class="filter-actions">
          <button class="control-button is-primary" type="button" @click="refreshTimeline">查询</button>
          <button class="control-button" type="button" @click="resetFilters">重置</button>
        </div>
      </section>

      <section class="emergency-layout">
        <aside class="emergency-panel drill-list-panel">
          <div class="panel-header compact">
            <div>
              <span class="eyebrow">Drill Queue</span>
              <h3 class="panel-title-text">演练列表</h3>
            </div>
            <span class="chip">{{ rows.length }} 项</span>
          </div>

          <div class="timeline drill-scrollbar">
            <article v-for="item in rows" :key="item.id" class="timeline-item">
              <button class="exercise-card" :class="{ 'is-active': selected?.id === item.id }" @click="selectManoeuvre(item)">
                <span class="state-pill" :class="stateClass(item.status)">{{ item.status }}</span>
                <strong>{{ item.name }}</strong>
                <span>{{ item.type }} / {{ item.area_name }}</span>
                <p>{{ item.location }} / {{ item.start_time }}</p>
                <small>参与 {{ item.participants }} 人 · {{ item.organizer }}</small>
              </button>
            </article>
            <div v-if="rows.length === 0" class="empty-state">当前筛选条件下暂无演练数据。</div>
          </div>
        </aside>

        <main class="emergency-panel drill-command-panel">
          <div class="panel-header compact">
            <div>
              <span class="eyebrow">Scenario Board</span>
              <h3 class="panel-title-text">{{ selected?.name || '未选择演练' }}</h3>
            </div>
            <span v-if="selected" class="state-pill" :class="stateClass(selected.status)">{{ selected.status }}</span>
          </div>

          <template v-if="selected">
            <div class="drill-kpis">
              <article>
                <span>类型</span>
                <strong>{{ selected.type }}</strong>
              </article>
              <article>
                <span>区域</span>
                <strong>{{ selected.area_name }}</strong>
              </article>
              <article>
                <span>参与人数</span>
                <strong>{{ selected.participants }}</strong>
              </article>
              <article>
                <span>结论</span>
                <strong>{{ completionText }}</strong>
              </article>
            </div>

            <div class="scenario-grid">
              <article class="scenario-card is-wide">
                <span>演练目标</span>
                <p>{{ selected.description }}</p>
              </article>
              <article class="scenario-card">
                <span>时间窗口</span>
                <strong>{{ selected.start_time }}</strong>
                <p>至 {{ selected.end_time }}</p>
              </article>
              <article class="scenario-card">
                <span>现场位置</span>
                <strong>{{ selected.location }}</strong>
                <p>{{ selected.area_name }}</p>
              </article>
              <article class="scenario-card">
                <span>组织单位</span>
                <strong>{{ selected.organizer }}</strong>
                <p>调度、抢修、现场联络协同</p>
              </article>
              <article class="scenario-card">
                <span>复盘问题</span>
                <strong>{{ selected.issues || '暂无问题记录' }}</strong>
                <p>用于后续整改闭环</p>
              </article>
            </div>
          </template>

          <div v-else class="empty-state">请从左侧选择一项演练。</div>
        </main>

        <aside class="emergency-panel detail-panel">
          <div class="panel-header compact">
            <div>
              <span class="eyebrow">Drill Detail</span>
              <h3 class="panel-title-text">联动详情</h3>
            </div>
            <span v-if="detailLoading" class="chip is-warn">加载中</span>
          </div>

          <div v-if="selected" class="detail-stack">
            <div class="detail-callout">
              <span>当前选中</span>
              <strong>{{ selected.name }}</strong>
              <p>{{ selected.location }} / {{ selected.organizer }}</p>
            </div>

            <div class="kv-list">
              <div class="kv-item"><span>ID</span><strong>{{ selected.id }}</strong></div>
              <div class="kv-item"><span>状态</span><strong>{{ selected.status }}</strong></div>
              <div class="kv-item"><span>区域</span><strong>{{ selected.area_name }}</strong></div>
              <div class="kv-item"><span>开始</span><strong>{{ selected.start_time }}</strong></div>
              <div class="kv-item"><span>结束</span><strong>{{ selected.end_time }}</strong></div>
              <div class="kv-item"><span>参与</span><strong>{{ selected.participants }} 人</strong></div>
            </div>

          </div>
          <div v-else class="empty-state">暂无详情。</div>
        </aside>
      </section>
    </section>
  </ModuleShell>
</template>

<style scoped>
.emergency-page {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
  block-size: 100%;
  min-block-size: 0;
  overflow: hidden;
}

.emergency-filter {
  display: grid;
  grid-template-columns: auto repeat(5, minmax(116px, 1fr)) minmax(176px, 1.24fr) minmax(176px, 1.24fr) auto;
  gap: 8px;
  align-items: end;
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--color-line);
  background:
    linear-gradient(90deg, rgba(110, 202, 212, 0.045), transparent 52%),
    rgba(12, 23, 32, 0.78);
}

.filter-meta {
  display: grid;
  gap: 6px;
  align-self: stretch;
  align-content: end;
  min-width: 92px;
}

.select-field {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.select-field span {
  overflow: hidden;
  color: var(--color-text-muted);
  font-size: var(--text-meta);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.select-field select,
.select-field input {
  width: 100%;
  min-width: 0;
  block-size: 34px;
  border: 1px solid var(--color-line);
  color: var(--color-text);
  background: rgba(2, 8, 13, 0.82);
  padding: 0 10px;
  color-scheme: dark;
}

.select-field select:disabled {
  opacity: 0.55;
}

.filter-actions {
  display: flex;
  gap: 8px;
  align-items: end;
}

.filter-actions .control-button {
  min-height: 34px;
  padding-inline: 12px;
  white-space: nowrap;
}

.emergency-layout {
  display: grid;
  grid-template-columns: minmax(300px, 0.8fr) minmax(0, 1.52fr) minmax(300px, 0.72fr);
  gap: 12px;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.emergency-panel {
  min-width: 0;
  min-height: 0;
  border: 1px solid var(--color-line);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.024), transparent 44%),
    rgba(12, 23, 32, 0.82);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.025);
}

.panel-header.compact {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px 8px;
}

.panel-header.compact .panel-title-text {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.drill-list-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
}

.timeline {
  display: grid;
  align-content: start;
  gap: 8px;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 0 12px 12px;
}

.timeline-item {
  min-width: 0;
}

.exercise-card {
  width: 100%;
  min-width: 0;
  min-height: 124px;
  padding: 12px;
  border: 1px solid var(--color-line);
  color: var(--color-text);
  background:
    linear-gradient(90deg, rgba(110, 202, 212, 0.04), transparent 52%),
    rgba(255, 255, 255, 0.014);
  text-align: left;
  display: grid;
  gap: 7px;
  overflow: hidden;
}

.exercise-card:hover,
.exercise-card.is-active {
  border-color: var(--color-line-strong);
  background:
    linear-gradient(90deg, rgba(110, 202, 212, 0.09), transparent 58%),
    rgba(255, 255, 255, 0.02);
}

.exercise-card strong,
.exercise-card span,
.exercise-card p,
.exercise-card small {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.exercise-card span,
.exercise-card p,
.exercise-card small {
  color: var(--color-text-muted);
  font-size: var(--text-meta);
}

.exercise-card .state-pill {
  justify-self: start;
  color: var(--color-text);
}

.drill-command-panel {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  overflow: hidden;
}

.drill-kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  padding: 0 14px 10px;
}

.drill-kpis article,
.scenario-card,
.detail-callout,
.api-note {
  min-width: 0;
  border: 1px solid var(--color-line);
  background: rgba(255, 255, 255, 0.016);
}

.drill-kpis article {
  display: grid;
  gap: 4px;
  min-height: 68px;
  padding: 10px;
}

.drill-kpis span,
.scenario-card span,
.detail-callout span,
.api-note span {
  color: var(--color-text-muted);
  font-size: var(--text-meta);
}

.drill-kpis strong,
.scenario-card strong,
.detail-callout strong {
  min-width: 0;
  overflow: hidden;
  color: var(--color-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scenario-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-auto-rows: minmax(112px, auto);
  gap: 10px;
  min-height: 0;
  overflow: hidden;
  padding: 0 14px 14px;
}

.scenario-card {
  display: grid;
  align-content: start;
  gap: 8px;
  padding: 12px;
  overflow: hidden;
}

.scenario-card.is-wide {
  grid-column: span 2;
}

.scenario-card p {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--color-text-muted);
  font-size: var(--text-meta);
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.detail-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
}

.detail-stack {
  display: grid;
  align-content: start;
  gap: 10px;
  min-height: 0;
  padding: 0 14px 14px;
  overflow: hidden;
}

.detail-callout {
  display: grid;
  gap: 8px;
  padding: 12px;
  background:
    linear-gradient(135deg, rgba(231, 104, 45, 0.08), transparent 58%),
    rgba(255, 255, 255, 0.018);
}

.detail-callout p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--text-meta);
}

.kv-list {
  display: grid;
  gap: 8px;
}

.kv-item {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 34px;
  padding: 7px 9px;
  border: 1px solid var(--color-line);
  background: rgba(255, 255, 255, 0.012);
}

.kv-item span {
  color: var(--color-text-muted);
  font-size: var(--text-meta);
}

.kv-item strong {
  min-width: 0;
  overflow: hidden;
  color: var(--color-text);
  font-size: var(--text-meta);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.api-note {
  display: grid;
  gap: 8px;
  padding: 12px;
}

.api-note code {
  min-width: 0;
  overflow: hidden;
  color: var(--color-accent-cyan);
  font-size: var(--text-micro);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-state {
  margin: 0 14px 14px;
  padding: 14px;
  border: 1px dashed var(--color-line);
  color: var(--color-text-muted);
}

.drill-scrollbar {
  scrollbar-color: rgba(110, 202, 212, 0.34) rgba(255, 255, 255, 0.04);
  scrollbar-width: thin;
}

.drill-scrollbar::-webkit-scrollbar {
  width: 8px;
}

.drill-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(110, 202, 212, 0.32);
}

@media (max-width: 1439px) {
  .emergency-filter {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .filter-meta,
  .filter-actions {
    grid-column: span 2;
  }
}

@media (max-width: 1179px) {
  .emergency-layout {
    grid-template-columns: minmax(280px, 0.8fr) minmax(0, 1.2fr);
  }

  .detail-panel {
    display: none;
  }
}

@media (max-width: 719px) {
  .emergency-filter,
  .emergency-layout {
    grid-template-columns: 1fr;
  }

  .filter-meta,
  .filter-actions,
  .scenario-card.is-wide {
    grid-column: auto;
  }

  .scenario-grid,
  .drill-kpis {
    grid-template-columns: 1fr;
  }
}
</style>
