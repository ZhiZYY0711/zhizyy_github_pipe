# 布局设计规范

本文档定义了管道管理系统的布局系统，告别传统左右布局，采用现代化的多层次页面切换和抽屉式设计。

---

## 1. 整体布局架构

### 1.1 布局理念

**核心原则：**
- ❌ 拒绝传统左侧导航 + 右侧内容布局
- ✅ 顶部导航 + 全屏内容区
- ✅ 卡片式页面切换
- ✅ 抽屉式详情面板
- ✅ 标签页工作区

### 1.2 布局层级

```
┌─────────────────────────────────────────┐
│          顶部导航栏（固定）              │
├─────────────────────────────────────────┤
│                                         │
│                                         │
│           全屏内容区                     │
│        （可切换的页面卡片）              │
│                                         │
│                                         │
└─────────────────────────────────────────┘
         ↑ 抽屉式详情面板（从右侧滑出）
```

---

## 2. 顶部导航栏（Top Navigation）

### 2.1 设计规范

```vue
<template>
  <header class="top-nav">
    <div class="top-nav__left">
      <div class="top-nav__logo">
        <img src="@/assets/logo.png" alt="Logo" />
        <span class="top-nav__title">管道管理系统</span>
      </div>
      
      <nav class="top-nav__menu">
        <div
          v-for="item in menuItems"
          :key="item.key"
          class="top-nav__menu-item"
          :class="{ 'top-nav__menu-item--active': activeMenu === item.key }"
          @click="handleMenuClick(item)"
        >
          <i :class="item.icon"></i>
          <span>{{ item.label }}</span>
        </div>
      </nav>
    </div>
    
    <div class="top-nav__right">
      <div class="top-nav__search">
        <el-input
          v-model="searchQuery"
          placeholder="搜索..."
          prefix-icon="el-icon-search"
          size="small"
        />
      </div>
      
      <div class="top-nav__theme-toggle" @click="toggleTheme">
        <i :class="isDark ? 'el-icon-sunny' : 'el-icon-moon'"></i>
      </div>
      
      <div class="top-nav__notifications">
        <el-badge :value="notificationCount" :max="99">
          <i class="el-icon-bell"></i>
        </el-badge>
      </div>
      
      <el-dropdown @command="handleUserCommand">
        <div class="top-nav__user">
          <el-avatar :size="32" :src="userAvatar"></el-avatar>
          <span class="top-nav__username">{{ username }}</span>
        </div>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="profile">个人中心</el-dropdown-item>
          <el-dropdown-item command="settings">设置</el-dropdown-item>
          <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </header>
</template>

<script>
export default {
  name: 'TopNavigation',
  data() {
    return {
      searchQuery: '',
      activeMenu: 'dashboard',
      menuItems: [
        { key: 'dashboard', label: '数据总览', icon: 'el-icon-data-line' },
        { key: 'monitoring', label: '实时监控', icon: 'el-icon-monitor' },
        { key: 'tasks', label: '任务管理', icon: 'el-icon-document' },
        { key: 'equipment', label: '设备管理', icon: 'el-icon-setting' },
        { key: 'analysis', label: '数据分析', icon: 'el-icon-pie-chart' }
      ]
    };
  },
  computed: {
    isDark() {
      return this.$store.state.theme === 'dark';
    },
    username() {
      return this.$store.state.user.username;
    },
    userAvatar() {
      return this.$store.state.user.avatar;
    },
    notificationCount() {
      return this.$store.state.notifications.count;
    }
  },
  methods: {
    handleMenuClick(item) {
      this.activeMenu = item.key;
      this.$router.push({ name: item.key });
    },
    toggleTheme() {
      this.$store.dispatch('toggleTheme');
    },
    handleUserCommand(command) {
      if (command === 'logout') {
        this.$store.dispatch('logout');
        this.$router.push('/login');
      } else {
        this.$router.push({ name: command });
      }
    }
  }
};
</script>

<style scoped>
.top-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background-color: var(--color-bg-surface);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 var(--spacing-6);
  z-index: var(--z-index-fixed);
  box-shadow: var(--shadow-sm);
}

.top-nav__left {
  display: flex;
  align-items: center;
  gap: var(--spacing-8);
}

.top-nav__logo {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
}

.top-nav__logo img {
  height: 32px;
}

.top-nav__title {
  font-family: var(--font-display);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
  letter-spacing: 0.5px;
}

.top-nav__menu {
  display: flex;
  gap: var(--spacing-1);
}

.top-nav__menu-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-2) var(--spacing-3);
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: var(--transition-all);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.top-nav__menu-item:hover {
  background-color: var(--color-bg-elevated);
  color: var(--color-text-primary);
}

.top-nav__menu-item--active {
  background-color: var(--color-primary-light);
  color: var(--color-primary);
}

.top-nav__right {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
}

.top-nav__search {
  width: 240px;
}

.top-nav__theme-toggle {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition-all);
  font-size: 20px;
  color: var(--color-text-secondary);
}

.top-nav__theme-toggle:hover {
  background-color: var(--color-bg-elevated);
  color: var(--color-primary);
}

.top-nav__notifications {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition-all);
  font-size: 20px;
  color: var(--color-text-secondary);
}

.top-nav__notifications:hover {
  background-color: var(--color-bg-elevated);
  color: var(--color-primary);
}

.top-nav__user {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  cursor: pointer;
  padding: var(--spacing-1);
  border-radius: var(--radius-sm);
  transition: var(--transition-all);
}

.top-nav__user:hover {
  background-color: var(--color-bg-elevated);
}

.top-nav__username {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}
</style>
```

---

## 3. 全屏内容区（Content Area）

### 3.1 设计规范

```vue
<template>
  <main class="content-area">
    <transition name="page-fade" mode="out-in">
      <keep-alive :include="cachedPages">
        <router-view :key="$route.path" />
      </keep-alive>
    </transition>
  </main>
</template>

<script>
export default {
  name: 'ContentArea',
  computed: {
    cachedPages() {
      return this.$store.state.cachedPages;
    }
  }
};
</script>

<style scoped>
.content-area {
  margin-top: 60px;
  min-height: calc(100vh - 60px);
  padding: var(--spacing-6);
  background-color: var(--color-bg-page);
}

/* 页面切换动画 */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity var(--duration-base) var(--ease-in-out);
}

.page-fade-enter,
.page-fade-leave-to {
  opacity: 0;
}
</style>
```

---

## 4. 卡片式页面布局（Card-Based Layout）

### 4.1 数据仪表盘布局

```vue
<template>
  <div class="dashboard-page">
    <!-- 指标卡区域 -->
    <section class="dashboard-page__metrics">
      <metric-card
        v-for="metric in metrics"
        :key="metric.key"
        :label="metric.label"
        :value="metric.value"
        :icon="metric.icon"
        :type="metric.type"
        :trend="metric.trend"
        :suffix="metric.suffix"
        @click="handleMetricClick(metric)"
      />
    </section>
    
    <!-- 图表区域 -->
    <section class="dashboard-page__charts">
      <div class="dashboard-page__chart-row">
        <chart-container
          title="任务状态分布"
          :option="taskStatusOption"
          height="300px"
          class="dashboard-page__chart"
        />
        <chart-container
          title="任务数量趋势"
          :option="taskTrendOption"
          height="300px"
          class="dashboard-page__chart"
        />
      </div>
      
      <div class="dashboard-page__chart-row">
        <chart-container
          title="区域任务对比"
          :option="areaTaskOption"
          height="350px"
          class="dashboard-page__chart--full"
        />
      </div>
    </section>
    
    <!-- 数据表格区域 -->
    <section class="dashboard-page__table">
      <data-table
        title="最新任务"
        :data="recentTasks"
        :pagination="true"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="totalTasks"
        @row-click="handleTaskClick"
        @page-change="handlePageChange"
      >
        <el-table-column prop="title" label="任务标题" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <status-tag :text="row.status" :type="getStatusType(row.status)" />
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" />
        <el-table-column prop="assignee" label="负责人" />
        <el-table-column prop="publicTime" label="发布时间" />
      </data-table>
    </section>
  </div>
</template>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-6);
}

.dashboard-page__metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: var(--spacing-4);
}

.dashboard-page__charts {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.dashboard-page__chart-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-4);
}

.dashboard-page__chart--full {
  grid-column: 1 / -1;
}

@media (max-width: 1200px) {
  .dashboard-page__chart-row {
    grid-template-columns: 1fr;
  }
}
</style>
```

---

## 5. 抽屉式详情面板（Drawer Detail Panel）

### 5.1 使用场景

- 点击任务卡片查看详情
- 点击设备卡片查看详情
- 编辑表单
- 高级筛选

### 5.2 实现示例

```vue
<template>
  <div class="task-list-page">
    <data-table
      :data="tasks"
      @row-click="handleTaskClick"
    >
      <!-- 表格列定义 -->
    </data-table>
    
    <!-- 任务详情抽屉 -->
    <drawer-panel
      :visible.sync="detailVisible"
      title="任务详情"
      width="600px"
    >
      <task-detail :task-id="selectedTaskId" />
      
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleEdit">编辑</el-button>
      </template>
    </drawer-panel>
  </div>
</template>

<script>
export default {
  data() {
    return {
      detailVisible: false,
      selectedTaskId: null
    };
  },
  methods: {
    handleTaskClick(row) {
      this.selectedTaskId = row.id;
      this.detailVisible = true;
    }
  }
};
</script>
```

---

## 6. 标签页工作区（Tab Workspace）

### 6.1 设计规范

```vue
<template>
  <div class="tab-workspace">
    <div class="tab-workspace__tabs">
      <div
        v-for="tab in tabs"
        :key="tab.path"
        class="tab-workspace__tab"
        :class="{ 'tab-workspace__tab--active': activeTab === tab.path }"
        @click="handleTabClick(tab)"
      >
        <span class="tab-workspace__tab-label">{{ tab.title }}</span>
        <button
          v-if="tab.closable"
          class="tab-workspace__tab-close"
          @click.stop="handleTabClose(tab)"
        >
          <i class="el-icon-close"></i>
        </button>
      </div>
    </div>
    
    <div class="tab-workspace__content">
      <keep-alive>
        <router-view />
      </keep-alive>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TabWorkspace',
  computed: {
    tabs() {
      return this.$store.state.tabs;
    },
    activeTab() {
      return this.$route.path;
    }
  },
  methods: {
    handleTabClick(tab) {
      this.$router.push(tab.path);
    },
    handleTabClose(tab) {
      this.$store.dispatch('closeTab', tab.path);
    }
  }
};
</script>

<style scoped>
.tab-workspace {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.tab-workspace__tabs {
  display: flex;
  gap: var(--spacing-1);
  padding: var(--spacing-2) var(--spacing-4);
  background-color: var(--color-bg-surface);
  border-bottom: 1px solid var(--color-border);
  overflow-x: auto;
}

.tab-workspace__tab {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-2) var(--spacing-3);
  background-color: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition-all);
  white-space: nowrap;
}

.tab-workspace__tab:hover {
  border-color: var(--color-primary);
}

.tab-workspace__tab--active {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-text-inverse);
}

.tab-workspace__tab-label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.tab-workspace__tab-close {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: inherit;
  cursor: pointer;
  border-radius: 50%;
  transition: var(--transition-all);
  font-size: 12px;
}

.tab-workspace__tab-close:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.tab-workspace__content {
  flex: 1;
  overflow: auto;
}
</style>
```

---

## 7. 响应式布局

### 7.1 断点定义

```css
/* 移动端 */
@media (max-width: 576px) {
  .top-nav__menu {
    display: none;
  }
  
  .top-nav__search {
    display: none;
  }
  
  .dashboard-page__metrics {
    grid-template-columns: 1fr;
  }
  
  .dashboard-page__chart-row {
    grid-template-columns: 1fr;
  }
}

/* 平板 */
@media (min-width: 577px) and (max-width: 992px) {
  .dashboard-page__metrics {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .dashboard-page__chart-row {
    grid-template-columns: 1fr;
  }
}

/* 桌面 */
@media (min-width: 993px) {
  .dashboard-page__metrics {
    grid-template-columns: repeat(4, 1fr);
  }
  
  .dashboard-page__chart-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
```

---

## 8. 网格系统

### 8.1 12列网格

```css
.grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: var(--spacing-4);
}

.col-1 { grid-column: span 1; }
.col-2 { grid-column: span 2; }
.col-3 { grid-column: span 3; }
.col-4 { grid-column: span 4; }
.col-5 { grid-column: span 5; }
.col-6 { grid-column: span 6; }
.col-7 { grid-column: span 7; }
.col-8 { grid-column: span 8; }
.col-9 { grid-column: span 9; }
.col-10 { grid-column: span 10; }
.col-11 { grid-column: span 11; }
.col-12 { grid-column: span 12; }
```

### 8.2 使用示例

```vue
<div class="grid">
  <div class="col-8">
    <chart-container title="主图表" height="400px" />
  </div>
  <div class="col-4">
    <metric-card label="指标1" :value="100" />
    <metric-card label="指标2" :value="200" />
  </div>
</div>
```

---

## 9. 布局最佳实践

### 9.1 内容优先级

1. **关键指标** - 放在最显眼的位置（顶部）
2. **核心图表** - 放在次显眼的位置（中部）
3. **详细数据** - 放在底部或抽屉中

### 9.2 视觉层次

```
Level 1: 页面标题、关键指标
Level 2: 图表、卡片
Level 3: 表格、列表
Level 4: 详情、辅助信息
```

### 9.3 间距规范

```
页面边距: var(--spacing-6)
区块间距: var(--spacing-6)
卡片间距: var(--spacing-4)
元素间距: var(--spacing-2)
```
