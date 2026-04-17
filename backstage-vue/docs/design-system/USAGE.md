# 设计系统使用指南

本文档说明如何在项目中使用设计系统。

---

## 1. 快速开始

### 1.1 引入样式文件

在 `main.js` 或 `App.vue` 中引入设计系统样式：

```javascript
// main.js
import Vue from 'vue';
import App from './App.vue';
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';

// 引入设计系统样式
import '@/styles/design-tokens.css';
import '@/styles/element-override.css';
import '@/styles/components.css';

Vue.use(ElementUI);
Vue.config.productionTip = false;

new Vue({
  render: h => h(App),
}).$mount('#app');
```

### 1.2 初始化主题

在 `App.vue` 中初始化主题：

```vue
<script>
import { initTheme } from '@/utils/theme';

export default {
  name: 'App',
  mounted() {
    initTheme();
  }
};
</script>
```

---

## 2. 使用布局组件

### 2.1 使用 AppLayout

```vue
<template>
  <app-layout>
    <router-view />
  </app-layout>
</template>

<script>
import AppLayout from '@/layouts/AppLayout.vue';

export default {
  components: {
    AppLayout
  }
};
</script>
```

---

## 3. 使用核心组件

### 3.1 指标卡（MetricCard）

```vue
<template>
  <metric-card
    label="传感器总数"
    :value="150"
    icon="el-icon-cpu"
    type="primary"
    :trend="12"
  />
</template>

<script>
import MetricCard from '@/components/MetricCard.vue';

export default {
  components: {
    MetricCard
  }
};
</script>
```

**Props:**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| label | String | - | 指标标签 |
| value | Number/String | - | 指标值 |
| icon | String | - | 图标类名 |
| type | String | 'default' | 类型：default/primary/success/warning/danger |
| trend | Number | - | 趋势百分比 |
| suffix | String | - | 值后缀 |
| prefix | String | - | 值前缀 |

### 3.2 抽屉面板（DrawerPanel）

```vue
<template>
  <div>
    <button @click="drawerVisible = true">打开抽屉</button>
    
    <drawer-panel
      :visible.sync="drawerVisible"
      title="任务详情"
      width="600px"
      placement="right"
    >
      <div>抽屉内容</div>
      
      <template #footer>
        <button class="btn btn-secondary" @click="drawerVisible = false">关闭</button>
        <button class="btn btn-primary">保存</button>
      </template>
    </drawer-panel>
  </div>
</template>

<script>
import DrawerPanel from '@/components/DrawerPanel.vue';

export default {
  components: {
    DrawerPanel
  },
  data() {
    return {
      drawerVisible: false
    };
  }
};
</script>
```

**Props:**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| visible | Boolean | false | 是否显示（支持.sync） |
| title | String | - | 标题 |
| width | String | '600px' | 宽度（bottom时为高度） |
| placement | String | 'right' | 位置：left/right/bottom |
| closable | Boolean | true | 是否可点击遮罩关闭 |

---

## 4. 使用 CSS 类

### 4.1 按钮类

```html
<!-- 主要按钮 -->
<button class="btn btn-primary">主要按钮</button>

<!-- 次要按钮 -->
<button class="btn btn-secondary">次要按钮</button>

<!-- 文字按钮 -->
<button class="btn btn-text">文字按钮</button>

<!-- 危险按钮 -->
<button class="btn btn-danger">危险按钮</button>

<!-- 尺寸 -->
<button class="btn btn-primary btn-sm">小按钮</button>
<button class="btn btn-primary btn-lg">大按钮</button>
<button class="btn btn-primary btn-block">块级按钮</button>
```

### 4.2 卡片类

```html
<!-- 基础卡片 -->
<div class="card">
  <div class="card__header">
    <h4 class="card__title">卡片标题</h4>
  </div>
  <div class="card__body">
    卡片内容
  </div>
  <div class="card__footer">
    <button class="btn btn-primary">确定</button>
  </div>
</div>
```

### 4.3 状态标签

```html
<span class="status-tag status-tag--primary">
  <span class="status-tag__dot"></span>
  进行中
</span>

<span class="status-tag status-tag--success">
  <span class="status-tag__dot"></span>
  已完成
</span>

<span class="status-tag status-tag--warning">
  <span class="status-tag__dot"></span>
  待处理
</span>

<span class="status-tag status-tag--danger">
  <span class="status-tag__dot"></span>
  紧急
</span>
```

### 4.4 输入框

```html
<input type="text" class="input" placeholder="请输入..." />

<!-- 错误状态 -->
<input type="text" class="input input--error" />
```

---

## 5. 使用 CSS 变量

### 5.1 在样式中使用

```css
.my-component {
  background-color: var(--color-bg-surface);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-base);
  padding: var(--spacing-4);
  transition: var(--transition-all);
}

.my-component:hover {
  border-color: var(--color-primary);
  box-shadow: var(--glow-primary);
}
```

### 5.2 在 JavaScript 中使用

```javascript
// 获取 CSS 变量
const primaryColor = getComputedStyle(document.documentElement)
  .getPropertyValue('--color-primary');

// 设置 CSS 变量
document.documentElement.style.setProperty('--color-primary', '#00D4FF');
```

---

## 6. 主题切换

### 6.1 手动切换

```vue
<template>
  <button @click="toggleTheme">
    {{ isDark ? '切换到浅色' : '切换到深色' }}
  </button>
</template>

<script>
import { getTheme, toggleTheme } from '@/utils/theme';

export default {
  data() {
    return {
      isDark: true
    };
  },
  mounted() {
    this.isDark = getTheme() === 'dark';
  },
  methods: {
    toggleTheme() {
      const theme = toggleTheme();
      this.isDark = theme === 'dark';
    }
  }
};
</script>
```

### 6.2 获取当前主题

```javascript
import { getTheme } from '@/utils/theme';

const currentTheme = getTheme(); // 'dark' 或 'light'
```

---

## 7. 图表集成

### 7.1 使用 ECharts

```vue
<template>
  <div class="chart-container">
    <div class="chart-container__header">
      <h4 class="chart-container__title">图表标题</h4>
    </div>
    <div class="chart-container__body" style="height: 300px;">
      <div ref="chart" style="width: 100%; height: 100%;"></div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  data() {
    return {
      chart: null
    };
  },
  mounted() {
    this.initChart();
  },
  methods: {
    initChart() {
      this.chart = echarts.init(this.$refs.chart);
      
      const option = {
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          type: 'category',
          data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
          axisLine: {
            lineStyle: {
              color: 'var(--color-border)'
            }
          },
          axisLabel: {
            color: 'var(--color-text-secondary)'
          }
        },
        yAxis: {
          type: 'value',
          axisLine: {
            lineStyle: {
              color: 'var(--color-border)'
            }
          },
          axisLabel: {
            color: 'var(--color-text-secondary)'
          },
          splitLine: {
            lineStyle: {
              color: 'var(--color-border-light)'
            }
          }
        },
        series: [
          {
            data: [120, 200, 150, 80, 70, 110, 130],
            type: 'line',
            smooth: true,
            itemStyle: {
              color: 'var(--chart-1)'
            }
          }
        ]
      };
      
      this.chart.setOption(option);
    }
  }
};
</script>
```

---

## 8. 最佳实践

### 8.1 始终使用设计令牌

❌ **错误：**

```css
.my-component {
  color: #E2E8F0;
  background-color: #1A1F3A;
  border-radius: 8px;
}
```

✅ **正确：**

```css
.my-component {
  color: var(--color-text-primary);
  background-color: var(--color-bg-surface);
  border-radius: var(--radius-base);
}
```

### 8.2 避免硬编码颜色

❌ **错误：**

```javascript
const color = '#00D4FF';
```

✅ **正确：**

```javascript
const color = getComputedStyle(document.documentElement)
  .getPropertyValue('--color-primary').trim();
```

### 8.3 响应式设计

```css
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--spacing-4);
}

@media (max-width: 768px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
}
```

---

## 9. 示例页面

查看完整示例：[DashboardExample.vue](../src/views/DashboardExample.vue)

---

## 10. 相关文档

- [设计令牌](./design-tokens.md)
- [组件规范](./components.md)
- [布局规范](./layouts.md)
- [主题系统](./themes.md)
- [动画规范](./animations.md)
