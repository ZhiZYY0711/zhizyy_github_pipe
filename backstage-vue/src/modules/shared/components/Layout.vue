<template>
  <div class="main-layout">
    <!-- 顶部标题栏 -->
    <header class="header">
      <div class="header-left">
        <h1 class="system-title zhi-mang-xing-regular">油气管道监测管理系统</h1>
      </div>
      
      <!-- 中间区域：三级行政区下拉菜单 -->
      <div class="header-center">
        <div class="area-selector">
          <!-- 省份下拉菜单 -->
          <div class="select-wrapper">
            <select v-model="selectedProvince" @change="onProvinceChange" class="area-select">
              <option value="">省份不限</option>
              <option v-for="province in provinces" :key="province.code" :value="province.code">
                {{ province.name }}
              </option>
            </select>
          </div>
          
          <!-- 城市下拉菜单 -->
          <div class="select-wrapper">
            <select v-model="selectedCity" @change="onCityChange" class="area-select" :disabled="!selectedProvince">
              <option value="">城市不限</option>
              <option v-for="city in cities" :key="city.code" :value="city.code">
                {{ city.name }}
              </option>
            </select>
          </div>
          
          <!-- 区县下拉菜单 -->
          <div class="select-wrapper">
            <select v-model="selectedDistrict" @change="onDistrictChange" class="area-select" :disabled="!selectedCity">
              <option value="">区县不限</option>
              <option v-for="district in districts" :key="district.code" :value="district.code">
                {{ district.name }}
              </option>
            </select>
          </div>
        </div>
      </div>
      
      <div class="header-right">
        <WeatherDate />
        <div class="user-info">
          <span class="user-name">管理员</span>
          <span class="user-role">系统管理员</span>
        </div>
      </div>
    </header>

    <!-- 主体内容区域 -->
    <div class="main-content" :class="{ 'content-shifted': isVisualizationActive && sidebarVisible }">
      <!-- 半圆按钮 - 仅在数据可视化模块显示 -->
      <div 
        v-if="isVisualizationActive && !sidebarVisible" 
        class="sidebar-toggle-btn"
        @click="showSidebar"
      >
        <i class="toggle-icon">▶</i>
      </div>
      
      <!-- 左侧导航栏 -->
      <nav 
        class="sidebar" 
        :class="{ 
          'sidebar-hidden': isVisualizationActive && !sidebarVisible,
          'sidebar-auto-hide': isVisualizationActive
        }"
        @mouseleave="handleSidebarMouseLeave"
        @mouseenter="showSidebar"
      >
        <ul class="nav-menu">
          <!-- 数据可视化模块 - 可展开 -->
          <li class="nav-item nav-parent" :class="{ active: isActiveVisualization }" @click="navigateToVisualization">
            <span class="nav-text">
              数据可视化
              <i class="expand-icon" :class="{ 'expanded': expandedMenus.includes('visualization') }">▼</i>
            </span>
          </li>
          <transition name="submenu-slide">
            <ul class="submenu" v-show="expandedMenus.includes('visualization')">
              <li class="nav-item nav-child" :class="{ active: isActivePath('/main/data-monitoring') }" @click="navigateTo('/main/data-monitoring')">
                <span class="nav-text">数据监控</span>
              </li>
              <li class="nav-item nav-child" :class="{ active: isActivePath('/main/task-details') }" @click="navigateTo('/main/task-details')">
                <span class="nav-text">任务详情</span>
              </li>
            </ul>
          </transition>
          
          <!-- 数据管理模块 - 可展开 -->
          <li class="nav-item nav-parent" :class="{ active: isActiveDataManagement }" @click="toggleSubmenu('dataManagement')">
            <span class="nav-text">
              数据管理
              <i class="expand-icon" :class="{ 'expanded': expandedMenus.includes('dataManagement') }">▼</i>
            </span>
          </li>
          <transition name="submenu-slide">
            <ul class="submenu" v-show="expandedMenus.includes('dataManagement')">
              <li class="nav-item nav-child" :class="{ active: isActivePath('/main/monitoring') }" @click="navigateTo('/main/monitoring')">
                <span class="nav-text">监测数据</span>
              </li>
              <li class="nav-item nav-child" :class="{ active: isActivePath('/main/equipment') }" @click="navigateTo('/main/equipment')">
                <span class="nav-text">设备信息</span>
              </li>
              <li class="nav-item nav-child" :class="{ active: isActivePath('/main/tasks') }" @click="navigateTo('/main/tasks')">
                <span class="nav-text">任务管理</span>
              </li>
              <li class="nav-item nav-child" :class="{ active: isActivePath('/main/repairman') }" @click="navigateTo('/main/repairman')">
                <span class="nav-text">检修员信息</span>
              </li>
            </ul>
          </transition>
          
          <!-- 新增模块 -->
          <li class="nav-item" :class="{ active: isActivePath('/main/virtual-expert') }" @click="navigateTo('/main/virtual-expert')">
            <span class="nav-text">虚拟专家</span>
          </li>
          <li class="nav-item" :class="{ active: isActivePath('/main/simulation-drill') }" @click="navigateTo('/main/simulation-drill')">
            <span class="nav-text">模拟与演练</span>
          </li>
          <li class="nav-item" :class="{ active: isActivePath('/main/emergency') }" @click="navigateTo('/main/emergency')">
            <span class="nav-text">事故响应</span>
          </li>
          <li class="nav-item" :class="{ active: isActivePath('/main/logs') }" @click="navigateTo('/main/logs')">
            <span class="nav-text">日志记录</span>
          </li>
        </ul>
      </nav>

      <!-- 右侧内容区域 -->
      <main class="content-area">
        <div class="content-wrapper">
          <!-- 使用router-view显示子路由内容 -->
          <transition name="page-fade" mode="out-in">
            <router-view :key="$route.fullPath"></router-view>
          </transition>
        </div>
      </main>
    </div>
  </div>
</template>

<script>
import WeatherDate from '@monitoring/components/WeatherDate.vue';
import { AreaManager } from '../utils/areaUtils.js';
import { NavigationManager, SidebarManager, PathUtils } from '../utils/navigationUtils.js';
import { DEFAULT_VALUES } from '../utils/constants.js';

export default {
  name: 'Layout',
  components: {
    WeatherDate
  },
  data() {
    return {
      // 管理器实例
      areaManager: null,
      navigationManager: null,
      sidebarManager: null
    }
  },
  computed: {
    isVisualizationActive() {
      const route = this.$route.path;
      return PathUtils.isPathInVisualization(route);
    },
    // 父级菜单选中状态：用于高亮父级"数据可视化/数据管理"
    isActiveVisualization() {
      return PathUtils.isPathInVisualization(this.$route.path);
    },
    isActiveDataManagement() {
      return PathUtils.isPathInDataManagement(this.$route.path);
    },
    // 从管理器获取状态
    expandedMenus() {
      return this.navigationManager ? this.navigationManager.getState().expandedMenus : [];
    },
    sidebarVisible() {
      return this.sidebarManager ? this.sidebarManager.getState().sidebarVisible : false;
    },
    // 区域相关状态
    provinces() {
      return this.areaManager ? this.areaManager.getState().provinces : [];
    },
    cities() {
      return this.areaManager ? this.areaManager.getState().cities : [];
    },
    districts() {
      return this.areaManager ? this.areaManager.getState().districts : [];
    },
    selectedProvince: {
      get() {
        return this.areaManager ? this.areaManager.getState().selectedProvince : DEFAULT_VALUES.EMPTY_STRING;
      },
      set(value) {
        if (this.areaManager) {
          this.areaManager.onProvinceChange(value);
        }
      }
    },
    selectedCity: {
      get() {
        return this.areaManager ? this.areaManager.getState().selectedCity : DEFAULT_VALUES.EMPTY_STRING;
      },
      set(value) {
        if (this.areaManager) {
          this.areaManager.onCityChange(value);
        }
      }
    },
    selectedDistrict: {
      get() {
        return this.areaManager ? this.areaManager.getState().selectedDistrict : DEFAULT_VALUES.EMPTY_STRING;
      },
      set(value) {
        if (this.areaManager) {
          this.areaManager.onDistrictChange(value);
        }
      }
    }
  },
  async mounted() {
    // 初始化管理器
    this.sidebarManager = new SidebarManager();
    this.navigationManager = new NavigationManager(this.$router, this.sidebarManager);
    this.areaManager = new AreaManager(this);
    
    // 初始化时，如果是数据可视化模块，确保导航栏隐藏
    if (this.isVisualizationActive) {
      this.sidebarManager.hideSidebar(true);
    }
    
    // 自动加载省份数据
    await this.areaManager.init();
  },
  beforeDestroy() {
    // 清理管理器
    if (this.navigationManager) {
      this.navigationManager.destroy();
    }
    if (this.sidebarManager) {
      this.sidebarManager.destroy();
    }
  },
  methods: {
    // 判断具体路径是否为当前选中，用于子菜单与普通菜单项高亮
    isActivePath(path) {
      return PathUtils.isActivePath(this.$route.path, path);
    },
    
    // 导航相关方法
    navigateTo(path) {
      this.navigationManager.navigateTo(path, this.$route.path);
    },

    navigateToVisualization() {
      this.navigationManager.navigateToVisualization(this.$route.path);
    },

    toggleSubmenu(menuName) {
      this.navigationManager.toggleSubmenu(menuName);
    },

    // 侧边栏相关方法
    showSidebar() {
      this.sidebarManager.showSidebar(this.isVisualizationActive);
    },

    hideSidebar() {
      this.sidebarManager.hideSidebar(this.isVisualizationActive);
    },

    handleSidebarMouseLeave() {
      this.sidebarManager.handleSidebarMouseLeave(this.isVisualizationActive, this.$el);
    },

    // 区域选择变化事件（通过computed的setter处理）
    onProvinceChange() {
      // 通过computed的setter自动处理
    },

    onCityChange() {
      // 通过computed的setter自动处理
    },

    onDistrictChange() {
      // 通过computed的setter自动处理
    }
  }
}
</script>

<style scoped>
/* 全局布局样式 */
.main-layout {
  height: 100vh;
  width: 100vw;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 顶部标题栏样式 */
.header {
  height: 60px;
  background-color: #1E3A8A;
  color: #FFFFFF;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

/* 头部中间区域样式 */
.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0 20px;
}

.area-selector {
  display: flex;
  gap: 15px;
  align-items: center;
}

.select-wrapper {
  position: relative;
}

.area-select {
  background-color: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 6px;
  color: #FFFFFF;
  padding: 8px 12px;
  font-size: 14px;
  min-width: 120px;
  cursor: pointer;
  transition: all 0.3s ease;
  outline: none;
  appearance: none;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='white' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6,9 12,15 18,9'%3e%3c/polyline%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 8px center;
  background-size: 16px;
  padding-right: 32px;
}

.area-select:hover {
  background-color: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.5);
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.area-select:focus {
  background-color: rgba(255, 255, 255, 0.2);
  border-color: #60A5FA;
  box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.3);
}

.area-select:disabled {
  background-color: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.5);
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.area-select option {
  background-color: #1E3A8A;
  color: #FFFFFF;
  padding: 8px 12px;
}

.area-select option:hover {
  background-color: #3B82F6;
}

.zhi-mang-xing-regular {
  font-family: "Zhi Mang Xing", cursive;
  font-weight: 400;
  font-style: normal;
}

.header-left .system-title {
  font-size: 28px; /* 增大字号 */
  font-weight: bold;
  margin: 0;
  margin-left: 20px; /* 向右移动 */
}

.header-right {
  display: flex;
  justify-content: flex-end; /* 整体靠右对齐 */
  align-items: center;
  gap: 30px; /* 调整天气日期和用户信息之间的间距，增大 */
}

.header-right .user-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  padding: 6px 0; /* 添加垂直内边距 */
  margin-right: 20px; /* 向左移动 */
}

.user-name {
  font-size: 14px;
  font-weight: 500;
}

.user-role {
  font-size: 12px;
  opacity: 0.8;
}

/* 主体内容区域 */
.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

/* 半圆按钮样式 */
.sidebar-toggle-btn {
  position: fixed;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 30px;
  height: 60px;
  background: linear-gradient(135deg, #1E3A8A, #3B82F6);
  border-radius: 0 30px 30px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 1000;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

.sidebar-toggle-btn:hover {
  width: 35px;
  background: linear-gradient(135deg, #0F2C6B, #1E3A8A);
  box-shadow: 3px 0 15px rgba(0, 0, 0, 0.3);
}

.toggle-icon {
  color: white;
  font-size: 14px;
  font-weight: bold;
  margin-left: 3px;
  transition: transform 0.3s ease;
}

.sidebar-toggle-btn:hover .toggle-icon {
  transform: scale(1.2);
}

/* 左侧导航栏样式 */
.sidebar {
  width: 200px;
  background-color: #1E3A8A;
  color: #FFFFFF;
  overflow-y: auto;
  flex-shrink: 0;
  transition: transform 0.45s cubic-bezier(0.25, 0.46, 0.45, 0.94), width 0.45s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  will-change: transform, width;
  position: relative;
  z-index: 100;
}

/* 数据可视化模块的自动隐藏样式 */
.sidebar-auto-hide {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  z-index: 200;
}

.sidebar-hidden {
  transform: translateX(-180px);
  width: 20px;
}

.sidebar-auto-hide.sidebar-hidden {
  transform: translateX(-180px);
  width: 20px;
}

.sidebar-auto-hide:not(.sidebar-hidden) {
  transform: translateX(0);
  width: 200px;
}

.nav-menu {
  list-style: none;
  padding: 0;
  margin: 0;
}

.nav-item {
  padding: 15px 20px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
  position: relative;
}

.nav-item:hover {
  background-color: #0F2C6B;
  transform: scale(1.05);
}

.nav-item:hover .nav-text {
  transform: scale(1.1);
}

.nav-text {
  font-size: 14px;
  font-weight: 500;
  transition: transform 0.3s ease;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 父级菜单样式 */
.nav-parent {
  cursor: pointer;
}

.nav-parent .nav-text {
  font-weight: 600;
}

/* 展开图标样式 - 优化版本 */
.expand-icon {
  font-size: 10px;
  transition: transform 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  margin-left: 10px;
  transform-origin: center;
  will-change: transform;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

/* 子菜单样式 - 优化版本 */
.submenu {
  list-style: none;
  padding: 0;
  margin: 0;
  /* background: linear-gradient(135deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.03)); */
  border-left: 3px solid #3B82F6;
  border-radius: 0 8px 8px 0;
  box-shadow: 
    inset 0 1px 0 rgba(255, 255, 255, 0.1),
    0 2px 8px rgba(0, 0, 0, 0.1);
  max-height: 200px;
  overflow: hidden;
  backdrop-filter: blur(10px);
  margin: 2px 0;
}

.submenu[style*="display: none"] {
  max-height: 0;
}

.nav-child {
  padding: 12px 20px 12px 40px;
  font-size: 13px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  position: relative;
}

.nav-child:last-child {
  border-bottom: none;
}

.nav-child:hover {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.15), rgba(255, 255, 255, 0.08));
  transform: translateX(8px);
  box-shadow: 0 2px 12px rgba(59, 130, 246, 0.2);
}

.nav-child:hover::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(to bottom, #60A5FA, #3B82F6);
  border-radius: 0;
}

.nav-child .nav-text {
  font-weight: 400;
  font-size: 13px;
}

/* 右侧内容区域样式 */
.content-area {
  flex: 1;
  background-color: #f5f5f5;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  /* 平滑过渡内容区域的左右间距变化 */
  transition: margin-left 0.45s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  will-change: margin-left;
}

.content-wrapper {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 当数据可视化模块的侧边栏显示时，让出对应宽度 */
.main-content.content-shifted .content-area {
  margin-left: 200px;
}

@media (max-width: 768px) {
  .main-content.content-shifted .content-area {
    margin-left: 180px;
  }
}

@media (max-width: 480px) {
  .main-content.content-shifted .content-area {
    margin-left: 160px;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    width: 180px;
  }
  
  .header-left .system-title {
    font-size: 18px;
  }
  
  .nav-text {
    font-size: 13px;
  }
  
  .header-center {
    margin: 0 10px;
  }
  
  .area-selector {
    gap: 10px;
  }
  
  .area-select {
    min-width: 100px;
    font-size: 13px;
    padding: 6px 10px;
    padding-right: 28px;
  }
}

@media (max-width: 480px) {
  .sidebar {
    width: 160px;
  }
  
  .header {
    padding: 0 15px;
  }
  
  .header-left .system-title {
    font-size: 16px;
  }
  
  .content-wrapper {
    padding: 15px;
  }
  
  .header-center {
    margin: 0 5px;
  }
  
  .area-selector {
    gap: 8px;
    flex-direction: column;
    align-items: stretch;
  }
  
  .area-select {
    min-width: 80px;
    font-size: 12px;
    padding: 5px 8px;
    padding-right: 24px;
  }
}

/* 确保无滚动条的单页设计 */
::-webkit-scrollbar {
  width: 0px;
  background: transparent;
}

/* 为火狐浏览器隐藏滚动条 */
.content-wrapper {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

/* 子菜单动画效果 - 优化版本 */
.submenu-slide-enter-active {
  transition: all 0.45s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  overflow: hidden;
  will-change: max-height, opacity, transform;
}

.submenu-slide-leave-active {
  transition: all 0.35s cubic-bezier(0.55, 0.06, 0.68, 0.19);
  overflow: hidden;
  will-change: max-height, opacity, transform;
}

.submenu-slide-enter-from {
  max-height: 0;
  opacity: 0;
  transform: translateY(-12px) scale(0.98);
  filter: blur(1px);
}

.submenu-slide-enter-to {
  max-height: 200px;
  opacity: 1;
  transform: translateY(0) scale(1);
  filter: blur(0);
}

.submenu-slide-leave-from {
  max-height: 200px;
  opacity: 1;
  transform: translateY(0) scale(1);
  filter: blur(0);
}

.submenu-slide-leave-to {
  max-height: 0;
  opacity: 0;
  transform: translateY(-8px) scale(0.95);
  filter: blur(1px);
}

/* 子菜单项的交错动画效果 */
.submenu-slide-enter-active .nav-child {
  animation: slideInStagger 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
  opacity: 0;
  transform: translateX(-20px);
}

.submenu-slide-enter-active .nav-child:nth-child(1) { animation-delay: 0.1s; }
.submenu-slide-enter-active .nav-child:nth-child(2) { animation-delay: 0.15s; }
.submenu-slide-enter-active .nav-child:nth-child(3) { animation-delay: 0.2s; }
.submenu-slide-enter-active .nav-child:nth-child(4) { animation-delay: 0.25s; }

@keyframes slideInStagger {
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 页面切换动画（右侧内容区域） */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
  will-change: opacity, transform;
}
.page-fade-enter {
  opacity: 0;
  transform: translateY(8px);
  filter: blur(1px);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
  filter: blur(1px);
}

/* 选中态高亮与圆角覆盖样式（追加在末尾提高优先级） */
.nav-item.active {
  background: linear-gradient(135deg, #3B82F6, #1E3A8A);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25);
}
.nav-item.active .nav-text { color: #E6F0FF; }
.nav-child.active {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0.12));
}
.nav-child.active::before {
  content: '';
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 3px;
  background: linear-gradient(to bottom, #93C5FD, #3B82F6);
  border-radius: 0 2px 2px 0;
}

/* 圆角增强（覆盖原样式） */
.sidebar { border-radius: 0; }
.submenu { border-radius: 0; }
.nav-item { border-radius: 0; }
.nav-child { border-radius: 0; }
</style>