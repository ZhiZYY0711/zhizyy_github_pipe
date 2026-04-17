# 主题切换系统

本文档定义了管道管理系统的深浅双主题切换系统，实现科技感的视觉体验。

---

## 1. 主题架构

### 1.1 主题类型

- **深色主题（Dark Theme）** - 默认主题，科技感强
- **浅色主题（Light Theme）** - 清爽专业

### 1.2 主题切换方式

1. 手动切换 - 用户点击主题切换按钮
2. 自动切换 - 跟随系统主题
3. 记忆功能 - 保存用户偏好到 localStorage

---

## 2. 主题实现方案

### 2.1 CSS 变量方案

```css
/* themes.css */

/* 深色主题（默认） */
:root,
:root[data-theme="dark"] {
  /* 主色调 */
  --color-primary: #00D4FF;
  --color-primary-hover: #33DDFF;
  --color-primary-active: #00BBDD;
  --color-primary-light: rgba(0, 212, 255, 0.1);
  
  /* 辅助色 */
  --color-secondary: #00FF88;
  --color-success: #00FF88;
  --color-warning: #FFB800;
  --color-danger: #FF6B6B;
  --color-info: #00D4FF;
  
  /* 背景色 */
  --color-bg-page: #0A0E27;
  --color-bg-surface: #1A1F3A;
  --color-bg-elevated: #252B4A;
  --color-bg-overlay: rgba(10, 14, 39, 0.8);
  
  /* 文字色 */
  --color-text-primary: #E2E8F0;
  --color-text-secondary: #94A3B8;
  --color-text-disabled: #64748B;
  --color-text-inverse: #0A0E27;
  
  /* 边框色 */
  --color-border: #334155;
  --color-border-light: #1E293B;
  --color-border-focus: #00D4FF;
  
  /* 阴影 */
  --shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.3);
  --shadow-base: 0 4px 16px rgba(0, 0, 0, 0.4);
  --shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.5);
  --shadow-xl: 0 16px 48px rgba(0, 0, 0, 0.6);
  
  /* 发光效果 */
  --glow-primary: 0 0 20px rgba(0, 212, 255, 0.3);
  --glow-success: 0 0 20px rgba(0, 255, 136, 0.3);
  --glow-danger: 0 0 20px rgba(255, 107, 107, 0.3);
}

/* 浅色主题 */
:root[data-theme="light"] {
  /* 主色调 */
  --color-primary: #1890FF;
  --color-primary-hover: #40A9FF;
  --color-primary-active: #096DD9;
  --color-primary-light: rgba(24, 144, 255, 0.1);
  
  /* 辅助色 */
  --color-secondary: #52C41A;
  --color-success: #52C41A;
  --color-warning: #FAAD14;
  --color-danger: #FF4D4F;
  --color-info: #1890FF;
  
  /* 背景色 */
  --color-bg-page: #F0F2F5;
  --color-bg-surface: #FFFFFF;
  --color-bg-elevated: #FAFAFA;
  --color-bg-overlay: rgba(0, 0, 0, 0.45);
  
  /* 文字色 */
  --color-text-primary: #1E293B;
  --color-text-secondary: #64748B;
  --color-text-disabled: #94A3B8;
  --color-text-inverse: #FFFFFF;
  
  /* 边框色 */
  --color-border: #E2E8F0;
  --color-border-light: #F1F5F9;
  --color-border-focus: #1890FF;
  
  /* 阴影 */
  --shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.08);
  --shadow-base: 0 4px 16px rgba(0, 0, 0, 0.12);
  --shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.16);
  --shadow-xl: 0 16px 48px rgba(0, 0, 0, 0.2);
  
  /* 发光效果 */
  --glow-primary: 0 0 20px rgba(24, 144, 255, 0.2);
  --glow-success: 0 0 20px rgba(82, 196, 26, 0.2);
  --glow-danger: 0 0 20px rgba(255, 77, 79, 0.2);
}
```

---

## 3. Vuex 状态管理

### 3.1 Theme Store

```javascript
// src/store/modules/theme.js

const state = {
  theme: localStorage.getItem('theme') || 'dark',
  followSystem: localStorage.getItem('followSystem') === 'true'
};

const mutations = {
  SET_THEME(state, theme) {
    state.theme = theme;
    localStorage.setItem('theme', theme);
    document.documentElement.setAttribute('data-theme', theme);
  },
  
  SET_FOLLOW_SYSTEM(state, follow) {
    state.followSystem = follow;
    localStorage.setItem('followSystem', follow);
  }
};

const actions = {
  initTheme({ commit, state }) {
    if (state.followSystem) {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      commit('SET_THEME', prefersDark ? 'dark' : 'light');
    } else {
      commit('SET_THEME', state.theme);
    }
    
    // 监听系统主题变化
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      if (state.followSystem) {
        commit('SET_THEME', e.matches ? 'dark' : 'light');
      }
    });
  },
  
  toggleTheme({ commit, state }) {
    const newTheme = state.theme === 'dark' ? 'light' : 'dark';
    commit('SET_THEME', newTheme);
  },
  
  setTheme({ commit }, theme) {
    commit('SET_THEME', theme);
  },
  
  setFollowSystem({ commit, dispatch }, follow) {
    commit('SET_FOLLOW_SYSTEM', follow);
    if (follow) {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      commit('SET_THEME', prefersDark ? 'dark' : 'light');
    }
  }
};

const getters = {
  isDark: state => state.theme === 'dark',
  currentTheme: state => state.theme
};

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
};
```

---

## 4. 主题切换组件

### 4.1 ThemeToggle 组件

```vue
<template>
  <div class="theme-toggle">
    <el-tooltip :content="tooltipText" placement="bottom">
      <div class="theme-toggle__button" @click="handleToggle">
        <transition name="theme-icon" mode="out-in">
          <i :key="currentTheme" :class="iconClass" class="theme-toggle__icon"></i>
        </transition>
      </div>
    </el-tooltip>
  </div>
</template>

<script>
import { mapGetters, mapActions } from 'vuex';

export default {
  name: 'ThemeToggle',
  computed: {
    ...mapGetters('theme', ['isDark', 'currentTheme']),
    iconClass() {
      return this.isDark ? 'el-icon-sunny' : 'el-icon-moon';
    },
    tooltipText() {
      return this.isDark ? '切换到浅色模式' : '切换到深色模式';
    }
  },
  methods: {
    ...mapActions('theme', ['toggleTheme']),
    handleToggle() {
      this.toggleTheme();
      this.$message.success(`已切换到${this.isDark ? '深色' : '浅色'}模式`);
    }
  }
};
</script>

<style scoped>
.theme-toggle__button {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition-all);
  background-color: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
}

.theme-toggle__button:hover {
  background-color: var(--color-primary-light);
  border-color: var(--color-primary);
}

.theme-toggle__icon {
  font-size: 20px;
  color: var(--color-text-secondary);
  transition: var(--transition-all);
}

.theme-toggle__button:hover .theme-toggle__icon {
  color: var(--color-primary);
}

/* 图标切换动画 */
.theme-icon-enter-active,
.theme-icon-leave-active {
  transition: all var(--duration-base) var(--ease-in-out);
}

.theme-icon-enter {
  opacity: 0;
  transform: rotate(-90deg) scale(0.5);
}

.theme-icon-leave-to {
  opacity: 0;
  transform: rotate(90deg) scale(0.5);
}
</style>
```

---

## 5. Element UI 主题适配

### 5.1 覆盖 Element UI 样式

```css
/* element-theme-override.css */

/* 深色主题 */
:root[data-theme="dark"] {
  /* Element UI 主色 */
  --el-color-primary: #00D4FF;
  --el-color-success: #00FF88;
  --el-color-warning: #FFB800;
  --el-color-danger: #FF6B6B;
  --el-color-info: #94A3B8;
  
  /* Element UI 背景色 */
  --el-bg-color: #1A1F3A;
  --el-bg-color-page: #0A0E27;
  --el-bg-color-overlay: #252B4A;
  
  /* Element UI 文字色 */
  --el-text-color-primary: #E2E8F0;
  --el-text-color-regular: #94A3B8;
  --el-text-color-secondary: #64748B;
  --el-text-color-placeholder: #64748B;
  
  /* Element UI 边框色 */
  --el-border-color: #334155;
  --el-border-color-light: #1E293B;
  --el-border-color-lighter: #1E293B;
  --el-border-color-extra-light: #1E293B;
  
  /* Element UI 填充色 */
  --el-fill-color: #252B4A;
  --el-fill-color-light: #1A1F3A;
  --el-fill-color-lighter: #1A1F3A;
  --el-fill-color-extra-light: #1A1F3A;
}

/* 浅色主题 */
:root[data-theme="light"] {
  /* Element UI 主色 */
  --el-color-primary: #1890FF;
  --el-color-success: #52C41A;
  --el-color-warning: #FAAD14;
  --el-color-danger: #FF4D4F;
  --el-color-info: #64748B;
  
  /* Element UI 背景色 */
  --el-bg-color: #FFFFFF;
  --el-bg-color-page: #F0F2F5;
  --el-bg-color-overlay: #FFFFFF;
  
  /* Element UI 文字色 */
  --el-text-color-primary: #1E293B;
  --el-text-color-regular: #64748B;
  --el-text-color-secondary: #94A3B8;
  --el-text-color-placeholder: #94A3B8;
  
  /* Element UI 边框色 */
  --el-border-color: #E2E8F0;
  --el-border-color-light: #F1F5F9;
  --el-border-color-lighter: #F1F5F9;
  --el-border-color-extra-light: #F1F5F9;
  
  /* Element UI 填充色 */
  --el-fill-color: #F1F5F9;
  --el-fill-color-light: #F8FAFC;
  --el-fill-color-lighter: #F8FAFC;
  --el-fill-color-extra-light: #F8FAFC;
}

/* 全局覆盖 */
.el-button--primary {
  background-color: var(--el-color-primary);
  border-color: var(--el-color-primary);
}

.el-input__inner {
  background-color: var(--el-bg-color);
  border-color: var(--el-border-color);
  color: var(--el-text-color-primary);
}

.el-table {
  background-color: var(--el-bg-color);
  color: var(--el-text-color-primary);
}

.el-table th {
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
}

.el-table tr {
  background-color: var(--el-bg-color);
}

.el-table--striped .el-table__body tr.el-table__row--striped td {
  background-color: var(--el-fill-color-lighter);
}

.el-dialog {
  background-color: var(--el-bg-color);
}

.el-message-box {
  background-color: var(--el-bg-color);
}
```

---

## 6. 图表主题适配

### 6.1 ECharts 主题配置

```javascript
// src/utils/chartTheme.js

export const darkTheme = {
  backgroundColor: 'transparent',
  textStyle: {
    color: '#94A3B8'
  },
  title: {
    textStyle: {
      color: '#E2E8F0'
    }
  },
  legend: {
    textStyle: {
      color: '#94A3B8'
    }
  },
  xAxis: {
    axisLine: {
      lineStyle: {
        color: '#334155'
      }
    },
    axisLabel: {
      color: '#94A3B8'
    },
    splitLine: {
      lineStyle: {
        color: '#1E293B'
      }
    }
  },
  yAxis: {
    axisLine: {
      lineStyle: {
        color: '#334155'
      }
    },
    axisLabel: {
      color: '#94A3B8'
    },
    splitLine: {
      lineStyle: {
        color: '#1E293B'
      }
    }
  },
  color: ['#00D4FF', '#00FF88', '#FFB800', '#FF6B6B', '#A78BFA', '#F472B6']
};

export const lightTheme = {
  backgroundColor: 'transparent',
  textStyle: {
    color: '#64748B'
  },
  title: {
    textStyle: {
      color: '#1E293B'
    }
  },
  legend: {
    textStyle: {
      color: '#64748B'
    }
  },
  xAxis: {
    axisLine: {
      lineStyle: {
        color: '#E2E8F0'
      }
    },
    axisLabel: {
      color: '#64748B'
    },
    splitLine: {
      lineStyle: {
        color: '#F1F5F9'
      }
    }
  },
  yAxis: {
    axisLine: {
      lineStyle: {
        color: '#E2E8F0'
      }
    },
    axisLabel: {
      color: '#64748B'
    },
    splitLine: {
      lineStyle: {
        color: '#F1F5F9'
      }
    }
  },
  color: ['#1890FF', '#52C41A', '#FAAD14', '#FF4D4F', '#722ED1', '#EB2F96']
};
```

### 6.2 图表组件中使用

```javascript
import { darkTheme, lightTheme } from '@/utils/chartTheme';
import { mapGetters } from 'vuex';

export default {
  computed: {
    ...mapGetters('theme', ['isDark']),
    chartTheme() {
      return this.isDark ? darkTheme : lightTheme;
    }
  },
  methods: {
    initChart() {
      const chart = echarts.init(this.$refs.chart);
      chart.setOption({
        ...this.chartTheme,
        ...this.option
      });
    }
  }
};
```

---

## 7. 主题切换动画

### 7.1 全局过渡动画

```css
/* theme-transition.css */

/* 全局主题切换过渡 */
* {
  transition: background-color var(--duration-slow) var(--ease-in-out),
              border-color var(--duration-slow) var(--ease-in-out),
              color var(--duration-slow) var(--ease-in-out);
}

/* 排除某些元素 */
.no-transition,
.no-transition * {
  transition: none !important;
}
```

---

## 8. 主题初始化

### 8.1 在 main.js 中初始化

```javascript
// src/main.js

import Vue from 'vue';
import App from './App.vue';
import store from './store';
import router from './router';

// 初始化主题
store.dispatch('theme/initTheme');

new Vue({
  store,
  router,
  render: h => h(App)
}).$mount('#app');
```

### 8.2 在 App.vue 中应用主题

```vue
<template>
  <div id="app" :data-theme="currentTheme">
    <router-view />
  </div>
</template>

<script>
import { mapGetters } from 'vuex';

export default {
  computed: {
    ...mapGetters('theme', ['currentTheme'])
  }
};
</script>
```

---

## 9. 主题设置页面

### 9.1 设置组件

```vue
<template>
  <div class="theme-settings">
    <h3 class="theme-settings__title">主题设置</h3>
    
    <div class="theme-settings__option">
      <span class="theme-settings__label">跟随系统</span>
      <el-switch
        :value="followSystem"
        @change="handleFollowSystemChange"
      />
    </div>
    
    <div v-if="!followSystem" class="theme-settings__option">
      <span class="theme-settings__label">主题模式</span>
      <el-radio-group :value="currentTheme" @change="handleThemeChange">
        <el-radio-button label="light">
          <i class="el-icon-sunny"></i> 浅色
        </el-radio-button>
        <el-radio-button label="dark">
          <i class="el-icon-moon"></i> 深色
        </el-radio-button>
      </el-radio-group>
    </div>
    
    <div class="theme-settings__preview">
      <div class="theme-settings__preview-card" :data-theme="currentTheme">
        <div class="preview-header">预览效果</div>
        <div class="preview-content">
          <div class="preview-text">这是一段文字</div>
          <div class="preview-button">按钮</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters, mapActions } from 'vuex';

export default {
  computed: {
    ...mapGetters('theme', ['currentTheme', 'followSystem'])
  },
  methods: {
    ...mapActions('theme', ['setTheme', 'setFollowSystem']),
    handleThemeChange(theme) {
      this.setTheme(theme);
    },
    handleFollowSystemChange(follow) {
      this.setFollowSystem(follow);
    }
  }
};
</script>

<style scoped>
.theme-settings {
  padding: var(--spacing-6);
}

.theme-settings__title {
  font-size: var(--font-size-h4);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-4);
}

.theme-settings__option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-3) 0;
  border-bottom: 1px solid var(--color-border-light);
}

.theme-settings__label {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
}

.theme-settings__preview {
  margin-top: var(--spacing-6);
}

.theme-settings__preview-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-base);
  overflow: hidden;
}

.preview-header {
  padding: var(--spacing-3) var(--spacing-4);
  background-color: var(--color-bg-elevated);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.preview-content {
  padding: var(--spacing-4);
}

.preview-text {
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-3);
}

.preview-button {
  display: inline-block;
  padding: var(--spacing-2) var(--spacing-4);
  background-color: var(--color-primary);
  color: var(--color-text-inverse);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
}
</style>
```
