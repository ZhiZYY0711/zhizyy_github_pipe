<script setup lang="ts">
import type { AreaOption, MapTimePreset, MapTimeRange } from '../types'

defineProps<{
  provinces: AreaOption[]
  cities: AreaOption[]
  districts: AreaOption[]
  selectedProvince: string
  selectedCity: string
  selectedDistrict: string
  hotRegions: AreaOption[]
  activeAreaName: string
  timeRange: MapTimeRange
  activeTimePreset: MapTimePreset
}>()

const emit = defineEmits<{
  selectProvince: [code: string]
  selectCity: [code: string]
  selectDistrict: [code: string]
  selectHotRegion: [code: string]
  updateTimeRange: [range: MapTimeRange]
  selectTimePreset: [preset: MapTimePreset]
}>()

const timePresets: Array<{ label: string; value: MapTimePreset }> = [
  { label: '全部', value: 'all' },
  { label: '近一周', value: 'week' },
  { label: '近一月', value: 'month' },
  { label: '近三月', value: 'quarter' },
]
</script>

<template>
  <aside class="area-filter">
    <section class="area-filter__section">
      <div class="area-filter__head">
        <span>热门地区</span>
      </div>
      <div class="hot-tabs">
        <button
          v-for="region in hotRegions"
          :key="region.code"
          type="button"
          :class="{ 'is-active': [selectedProvince, selectedCity, selectedDistrict].includes(region.code) }"
          @click="emit('selectHotRegion', region.code)"
        >
          {{ region.name }}
        </button>
      </div>
    </section>

    <section class="area-filter__section">
      <div class="area-filter__head">
        <span>行政区筛选</span>
      </div>

      <label class="select-field">
        <span>省级</span>
        <select :value="selectedProvince" @change="emit('selectProvince', ($event.target as HTMLSelectElement).value)">
          <option value="">全国</option>
          <option v-for="province in provinces" :key="province.code" :value="province.code">
            {{ province.name }}
          </option>
        </select>
      </label>

      <label class="select-field">
        <span>市级</span>
        <select
          :value="selectedCity"
          :disabled="!selectedProvince"
          @change="emit('selectCity', ($event.target as HTMLSelectElement).value)"
        >
          <option value="">全部城市</option>
          <option v-for="city in cities" :key="city.code" :value="city.code">
            {{ city.name }}
          </option>
        </select>
      </label>

      <label class="select-field">
        <span>区县</span>
        <select
          :value="selectedDistrict"
          :disabled="!selectedCity"
          @change="emit('selectDistrict', ($event.target as HTMLSelectElement).value)"
        >
          <option value="">全部区县</option>
          <option v-for="district in districts" :key="district.code" :value="district.code">
            {{ district.name }}
          </option>
        </select>
      </label>
    </section>

    <section class="area-filter__section">
      <div class="area-filter__head">
        <span>时间区间</span>
      </div>

      <div class="range-tabs">
        <button
          v-for="preset in timePresets"
          :key="preset.value"
          type="button"
          :class="{ 'is-active': activeTimePreset === preset.value }"
          @click="emit('selectTimePreset', preset.value)"
        >
          {{ preset.label }}
        </button>
      </div>

      <label class="select-field">
        <span>开始日期</span>
        <input
          type="date"
          :value="timeRange.startDate"
          @change="emit('updateTimeRange', { ...timeRange, startDate: ($event.target as HTMLInputElement).value })"
        />
      </label>

      <label class="select-field">
        <span>结束日期</span>
        <input
          type="date"
          :value="timeRange.endDate"
          @change="emit('updateTimeRange', { ...timeRange, endDate: ($event.target as HTMLInputElement).value })"
        />
      </label>
    </section>
  </aside>
</template>

<style scoped>
.area-filter {
  position: relative;
  border-right: 1px solid var(--color-line);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.012), transparent 16%), var(--color-panel);
  padding: var(--space-4);
  display: grid;
  gap: var(--space-3);
  align-content: start;
  overflow: hidden;
}

.area-filter::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  inline-size: 1px;
  background: linear-gradient(180deg, rgba(110, 202, 212, 0.14), transparent 30%, rgba(231, 104, 45, 0.1) 72%, transparent);
}

.area-filter__section {
  border: 1px solid var(--color-line);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.02), transparent), var(--color-panel-2);
  padding: 12px;
}

.hot-tabs button,
.range-tabs button {
  min-height: 34px;
  border: 1px solid var(--color-line);
  color: var(--color-text-muted);
  background: rgba(255, 255, 255, 0.02);
}

.hot-tabs button:hover,
.hot-tabs button.is-active,
.range-tabs button:hover,
.range-tabs button.is-active {
  color: var(--color-text);
  border-color: var(--color-line-strong);
  background: linear-gradient(180deg, rgba(110, 202, 212, 0.08), rgba(255, 255, 255, 0.01));
}

.area-filter__section {
  display: grid;
  gap: var(--space-3);
}

.area-filter__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--color-accent-cyan);
  font-size: var(--text-micro);
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
}

.hot-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.range-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
}

.hot-tabs button,
.range-tabs button {
  padding: 0 8px;
  font-size: var(--text-meta);
}

.select-field {
  display: grid;
  gap: 8px;
}

.select-field span {
  color: var(--color-text-muted);
  font-size: var(--text-meta);
}

.select-field select,
.select-field input {
  width: 100%;
  min-height: 38px;
  border: 1px solid var(--color-line);
  color: var(--color-text);
  background: #091018;
  padding: 0 10px;
  color-scheme: dark;
}

.select-field select:disabled,
.select-field input:disabled {
  color: var(--color-text-dim);
  opacity: 0.58;
}
</style>
