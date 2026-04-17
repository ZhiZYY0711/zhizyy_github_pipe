# 组件设计规范

本文档定义了管道管理系统的核心组件设计规范，确保UI一致性和可复用性。

---

## 1. 指标卡（Metric Card）

### 1.1 组件说明

用于展示关键业务指标，是数据可视化页面的核心组件。

### 1.2 设计规范

```vue
<template>
  <div class="metric-card" :class="`metric-card--${type}`">
    <div class="metric-card__icon">
      <i :class="icon"></i>
    </div>
    <div class="metric-card__content">
      <div class="metric-card__label">{{ label }}</div>
      <div class="metric-card__value">{{ formattedValue }}</div>
      <div v-if="trend" class="metric-card__trend" :class="trendClass">
        <i :class="trendIcon"></i>
        <span>{{ trendText }}</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MetricCard',
  props: {
    label: String,
    value: [Number, String],
    icon: String,
    type: {
      type: String,
      default: 'default',
      validator: (val) => ['default', 'primary', 'success', 'warning', 'danger'].includes(val)
    },
    trend: Number,
    suffix: String,
    prefix: String
  },
  computed: {
    formattedValue() {
      let val = this.value;
      if (this.prefix) val = this.prefix + val;
      if (this.suffix) val = val + this.suffix;
      return val;
    },
    trendClass() {
      return this.trend > 0 ? 'metric-card__trend--up' : 'metric-card__trend--down';
    },
    trendIcon() {
      return this.trend > 0 ? 'el-icon-top' : 'el-icon-bottom';
    },
    trendText() {
      return Math.abs(this.trend) + '%';
    }
  }
};
</script>

<style scoped>
.metric-card {
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-base);
  padding: var(--spacing-4);
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-3);
  transition: var(--transition-all);
  cursor: pointer;
}

.metric-card:hover {
  border-color: var(--color-primary);
  box-shadow: var(--glow-primary);
  transform: translateY(-2px);
}

.metric-card--primary .metric-card__icon {
  background-color: rgba(0, 212, 255, 0.1);
  color: var(--color-primary);
}

.metric-card--success .metric-card__icon {
  background-color: rgba(0, 255, 136, 0.1);
  color: var(--color-success);
}

.metric-card--warning .metric-card__icon {
  background-color: rgba(255, 184, 0, 0.1);
  color: var(--color-warning);
}

.metric-card--danger .metric-card__icon {
  background-color: rgba(255, 107, 107, 0.1);
  color: var(--color-danger);
}

.metric-card__icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-base);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background-color: var(--color-bg-elevated);
  color: var(--color-text-secondary);
}

.metric-card__label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-1);
}

.metric-card__value {
  font-family: var(--font-mono);
  font-size: var(--font-size-data);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  line-height: 1;
  margin-bottom: var(--spacing-1);
}

.metric-card__trend {
  display: flex;
  align-items: center;
  gap: var(--spacing-1);
  font-size: var(--font-size-xs);
}

.metric-card__trend--up {
  color: var(--color-success);
}

.metric-card__trend--down {
  color: var(--color-danger);
}
</style>
```

### 1.3 使用示例

```vue
<metric-card
  label="传感器总数"
  :value="150"
  icon="el-icon-cpu"
  type="primary"
  :trend="12"
  suffix="个"
/>
```

---

## 2. 数据表格（Data Table）

### 2.1 组件说明

用于展示结构化数据，支持排序、筛选、分页等功能。

### 2.2 设计规范

```vue
<template>
  <div class="data-table">
    <div v-if="title || $slots.toolbar" class="data-table__header">
      <h3 v-if="title" class="data-table__title">{{ title }}</h3>
      <div class="data-table__toolbar">
        <slot name="toolbar"></slot>
      </div>
    </div>
    
    <el-table
      :data="data"
      :border="false"
      :stripe="true"
      @row-click="handleRowClick"
      class="data-table__body"
    >
      <slot></slot>
    </el-table>
    
    <div v-if="pagination" class="data-table__footer">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script>
export default {
  name: 'DataTable',
  props: {
    data: Array,
    title: String,
    pagination: Boolean,
    currentPage: Number,
    pageSize: Number,
    total: Number
  },
  methods: {
    handleRowClick(row) {
      this.$emit('row-click', row);
    },
    handlePageChange(page) {
      this.$emit('page-change', page);
    },
    handleSizeChange(size) {
      this.$emit('size-change', size);
    }
  }
};
</script>

<style scoped>
.data-table {
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-base);
  overflow: hidden;
}

.data-table__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-4);
  border-bottom: 1px solid var(--color-border);
}

.data-table__title {
  font-family: var(--font-display);
  font-size: var(--font-size-h4);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

.data-table__body {
  background-color: transparent;
}

.data-table__body >>> .el-table__row {
  cursor: pointer;
  transition: var(--transition-all);
}

.data-table__body >>> .el-table__row:hover {
  background-color: var(--color-bg-elevated) !important;
}

.data-table__footer {
  display: flex;
  justify-content: flex-end;
  padding: var(--spacing-3) var(--spacing-4);
  border-top: 1px solid var(--color-border);
}
</style>
```

---

## 3. 抽屉面板（Drawer Panel）

### 3.1 组件说明

从屏幕边缘滑出的面板，用于展示详情、编辑表单等，不离开当前页面。

### 3.2 设计规范

```vue
<template>
  <transition name="drawer-fade">
    <div v-if="visible" class="drawer-overlay" @click="handleOverlayClick">
      <transition name="drawer-slide">
        <div
          v-if="visible"
          class="drawer"
          :class="`drawer--${placement}`"
          :style="drawerStyle"
          @click.stop
        >
          <div class="drawer__header">
            <h3 class="drawer__title">{{ title }}</h3>
            <button class="drawer__close" @click="handleClose">
              <i class="el-icon-close"></i>
            </button>
          </div>
          
          <div class="drawer__body">
            <slot></slot>
          </div>
          
          <div v-if="$slots.footer" class="drawer__footer">
            <slot name="footer"></slot>
          </div>
        </div>
      </transition>
    </div>
  </transition>
</template>

<script>
export default {
  name: 'DrawerPanel',
  props: {
    visible: Boolean,
    title: String,
    width: {
      type: String,
      default: '600px'
    },
    placement: {
      type: String,
      default: 'right',
      validator: (val) => ['left', 'right', 'bottom'].includes(val)
    },
    closable: {
      type: Boolean,
      default: true
    }
  },
  computed: {
    drawerStyle() {
      if (this.placement === 'bottom') {
        return { height: this.width };
      }
      return { width: this.width };
    }
  },
  methods: {
    handleClose() {
      this.$emit('update:visible', false);
      this.$emit('close');
    },
    handleOverlayClick() {
      if (this.closable) {
        this.handleClose();
      }
    }
  }
};
</script>

<style scoped>
.drawer-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: var(--color-bg-overlay);
  z-index: var(--z-index-drawer);
  display: flex;
}

.drawer {
  background-color: var(--color-bg-surface);
  box-shadow: var(--shadow-xl);
  display: flex;
  flex-direction: column;
}

.drawer--right {
  margin-left: auto;
  height: 100%;
}

.drawer--left {
  margin-right: auto;
  height: 100%;
}

.drawer--bottom {
  width: 100%;
  margin-top: auto;
}

.drawer__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-4) var(--spacing-6);
  border-bottom: 1px solid var(--color-border);
}

.drawer__title {
  font-family: var(--font-display);
  font-size: var(--font-size-h4);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

.drawer__close {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: var(--transition-all);
  display: flex;
  align-items: center;
  justify-content: center;
}

.drawer__close:hover {
  background-color: var(--color-bg-elevated);
  color: var(--color-text-primary);
}

.drawer__body {
  flex: 1;
  padding: var(--spacing-6);
  overflow-y: auto;
}

.drawer__footer {
  padding: var(--spacing-4) var(--spacing-6);
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-2);
}

/* 动画 */
.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity var(--duration-base);
}

.drawer-fade-enter,
.drawer-fade-leave-to {
  opacity: 0;
}

.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: transform var(--duration-slow) var(--ease-out);
}

.drawer--right.drawer-slide-enter,
.drawer--right.drawer-slide-leave-to {
  transform: translateX(100%);
}

.drawer--left.drawer-slide-enter,
.drawer--left.drawer-slide-leave-to {
  transform: translateX(-100%);
}

.drawer--bottom.drawer-slide-enter,
.drawer--bottom.drawer-slide-leave-to {
  transform: translateY(100%);
}
</style>
```

---

## 4. 状态标签（Status Tag）

### 4.1 组件说明

用于展示任务状态、设备状态等信息。

### 4.2 设计规范

```vue
<template>
  <span class="status-tag" :class="`status-tag--${type}`">
    <span class="status-tag__dot"></span>
    <span class="status-tag__text">{{ text }}</span>
  </span>
</template>

<script>
export default {
  name: 'StatusTag',
  props: {
    text: String,
    type: {
      type: String,
      default: 'default',
      validator: (val) => ['default', 'primary', 'success', 'warning', 'danger', 'info'].includes(val)
    }
  }
};
</script>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-1);
  padding: var(--spacing-1) var(--spacing-2);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
}

.status-tag__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.status-tag--default {
  background-color: var(--color-bg-elevated);
  color: var(--color-text-secondary);
}

.status-tag--default .status-tag__dot {
  background-color: var(--color-text-secondary);
}

.status-tag--primary {
  background-color: rgba(0, 212, 255, 0.1);
  color: var(--color-primary);
}

.status-tag--primary .status-tag__dot {
  background-color: var(--color-primary);
}

.status-tag--success {
  background-color: rgba(0, 255, 136, 0.1);
  color: var(--color-success);
}

.status-tag--success .status-tag__dot {
  background-color: var(--color-success);
}

.status-tag--warning {
  background-color: rgba(255, 184, 0, 0.1);
  color: var(--color-warning);
}

.status-tag--warning .status-tag__dot {
  background-color: var(--color-warning);
}

.status-tag--danger {
  background-color: rgba(255, 107, 107, 0.1);
  color: var(--color-danger);
}

.status-tag--danger .status-tag__dot {
  background-color: var(--color-danger);
}

.status-tag--info {
  background-color: rgba(148, 163, 184, 0.1);
  color: var(--color-text-secondary);
}

.status-tag--info .status-tag__dot {
  background-color: var(--color-text-secondary);
}
</style>
```

---

## 5. 图表容器（Chart Container）

### 5.1 组件说明

用于包装 ECharts 图表，提供统一的样式和交互。

### 5.2 设计规范

```vue
<template>
  <div class="chart-container">
    <div v-if="title" class="chart-container__header">
      <h4 class="chart-container__title">{{ title }}</h4>
      <div v-if="$slots.actions" class="chart-container__actions">
        <slot name="actions"></slot>
      </div>
    </div>
    <div class="chart-container__body" :style="{ height: height }">
      <div ref="chart" class="chart-container__chart"></div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: 'ChartContainer',
  props: {
    title: String,
    height: {
      type: String,
      default: '300px'
    },
    option: Object
  },
  data() {
    return {
      chart: null
    };
  },
  watch: {
    option: {
      deep: true,
      handler() {
        this.updateChart();
      }
    }
  },
  mounted() {
    this.initChart();
    window.addEventListener('resize', this.handleResize);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize);
    if (this.chart) {
      this.chart.dispose();
    }
  },
  methods: {
    initChart() {
      this.chart = echarts.init(this.$refs.chart);
      this.updateChart();
    },
    updateChart() {
      if (this.chart && this.option) {
        this.chart.setOption(this.option);
      }
    },
    handleResize() {
      if (this.chart) {
        this.chart.resize();
      }
    }
  }
};
</script>

<style scoped>
.chart-container {
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-base);
  overflow: hidden;
}

.chart-container__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-3) var(--spacing-4);
  border-bottom: 1px solid var(--color-border);
}

.chart-container__title {
  font-family: var(--font-display);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

.chart-container__body {
  position: relative;
  padding: var(--spacing-4);
}

.chart-container__chart {
  width: 100%;
  height: 100%;
}
</style>
```

---

## 6. 按钮样式

### 6.1 按钮类型

```css
/* 主要按钮 */
.btn-primary {
  background-color: var(--color-primary);
  color: var(--color-text-inverse);
  border: none;
  padding: var(--spacing-2) var(--spacing-4);
  border-radius: var(--radius-sm);
  font-weight: var(--font-weight-medium);
  transition: var(--transition-all);
  cursor: pointer;
}

.btn-primary:hover {
  background-color: var(--color-primary-hover);
  box-shadow: var(--glow-primary);
}

.btn-primary:active {
  background-color: var(--color-primary-active);
}

/* 次要按钮 */
.btn-secondary {
  background-color: var(--color-bg-surface);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  padding: var(--spacing-2) var(--spacing-4);
  border-radius: var(--radius-sm);
  font-weight: var(--font-weight-medium);
  transition: var(--transition-all);
  cursor: pointer;
}

.btn-secondary:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

/* 文字按钮 */
.btn-text {
  background: transparent;
  color: var(--color-primary);
  border: none;
  padding: var(--spacing-2);
  font-weight: var(--font-weight-medium);
  transition: var(--transition-all);
  cursor: pointer;
}

.btn-text:hover {
  background-color: var(--color-primary-light);
}

/* 危险按钮 */
.btn-danger {
  background-color: var(--color-danger);
  color: var(--color-text-inverse);
  border: none;
  padding: var(--spacing-2) var(--spacing-4);
  border-radius: var(--radius-sm);
  font-weight: var(--font-weight-medium);
  transition: var(--transition-all);
  cursor: pointer;
}

.btn-danger:hover {
  box-shadow: var(--glow-danger);
}
```

---

## 7. 表单样式

### 7.1 输入框

```css
.input {
  width: 100%;
  background-color: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: var(--spacing-2) var(--spacing-3);
  color: var(--color-text-primary);
  font-size: var(--font-size-base);
  transition: var(--transition-all);
}

.input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-light);
}

.input::placeholder {
  color: var(--color-text-disabled);
}

.input:disabled {
  background-color: var(--color-bg-surface);
  color: var(--color-text-disabled);
  cursor: not-allowed;
}
```

---

## 8. Element UI 主题覆盖

### 8.1 全局覆盖

```css
/* 覆盖 Element UI 默认主题 */
:root {
  --el-color-primary: var(--color-primary);
  --el-color-success: var(--color-success);
  --el-color-warning: var(--color-warning);
  --el-color-danger: var(--color-danger);
  --el-color-info: var(--color-info);
  
  --el-border-radius-base: var(--radius-sm);
  --el-border-radius-small: var(--radius-sm);
  
  --el-font-family: var(--font-body);
}

/* 表格样式覆盖 */
.el-table {
  background-color: transparent !important;
}

.el-table th {
  background-color: var(--color-bg-elevated) !important;
  color: var(--color-text-secondary) !important;
  font-weight: var(--font-weight-semibold) !important;
}

.el-table td {
  border-bottom: 1px solid var(--color-border-light) !important;
}

/* 分页器样式覆盖 */
.el-pagination button {
  background-color: var(--color-bg-surface) !important;
  color: var(--color-text-primary) !important;
}

.el-pagination .el-pager li {
  background-color: var(--color-bg-surface) !important;
  color: var(--color-text-primary) !important;
}

.el-pagination .el-pager li.active {
  background-color: var(--color-primary) !important;
  color: var(--color-text-inverse) !important;
}
```
