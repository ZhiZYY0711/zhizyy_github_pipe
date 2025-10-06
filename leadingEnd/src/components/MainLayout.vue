<template>
  <div class="main-layout">
    <!-- 顶部标题栏 -->
    <header class="header">
      <div class="header-left">
        <h1 class="system-title">油气管道监测管理系统</h1>
      </div>
      <div class="header-right">
        <div class="user-info">
          <span class="user-name">管理员</span>
          <span class="user-role">系统管理员</span>
        </div>
      </div>
    </header>

    <!-- 主体内容区域 -->
    <div class="main-content">
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
        @mouseleave="hideSidebar"
      >
        <ul class="nav-menu">
          <!-- 数据可视化模块 - 可展开 -->
          <li class="nav-item nav-parent" @click="setActiveMenu('visualization-overview')">
            <span class="nav-text">
              数据可视化
              <i class="expand-icon" :class="{ 'expanded': expandedMenus.includes('visualization') }">▼</i>
            </span>
          </li>
          <ul class="submenu" v-show="expandedMenus.includes('visualization')">
            <li class="nav-item nav-child" @click="setActiveMenu('area-details')">
              <span class="nav-text">区域详情</span>
            </li>
            <li class="nav-item nav-child" @click="setActiveMenu('pipeline-details')">
              <span class="nav-text">管道详情</span>
            </li>
            <li class="nav-item nav-child" @click="setActiveMenu('task-details')">
              <span class="nav-text">任务详情</span>
            </li>
          </ul>
          
          <!-- 数据管理模块 - 可展开 -->
          <li class="nav-item nav-parent" @click="toggleSubmenu('dataManagement')">
            <span class="nav-text">
              数据管理
              <i class="expand-icon" :class="{ 'expanded': expandedMenus.includes('dataManagement') }">▼</i>
            </span>
          </li>
          <ul class="submenu" v-show="expandedMenus.includes('dataManagement')">
            <li class="nav-item nav-child" @click="setActiveMenu('monitoring')">
              <span class="nav-text">监测数据</span>
            </li>
            <li class="nav-item nav-child" @click="setActiveMenu('equipment')">
              <span class="nav-text">设备信息</span>
            </li>
            <li class="nav-item nav-child" @click="setActiveMenu('tasks')">
              <span class="nav-text">任务管理</span>
            </li>
            <li class="nav-item nav-child" @click="setActiveMenu('maintenance')">
              <span class="nav-text">检修员信息</span>
            </li>
          </ul>
          
          <!-- 新增模块 -->
          <li class="nav-item" @click="setActiveMenu('virtualExpert')">
            <span class="nav-text">虚拟专家</span>
          </li>
          <li class="nav-item" @click="setActiveMenu('simulation')">
            <span class="nav-text">模拟与演练</span>
          </li>
          <li class="nav-item" @click="setActiveMenu('emergency')">
            <span class="nav-text">事故响应</span>
          </li>
          
          <li class="nav-item" @click="setActiveMenu('logs')">
            <span class="nav-text">日志记录</span>
          </li>
        </ul>
      </nav>

      <!-- 右侧内容区域 -->
      <main class="content-area">
        <div class="content-wrapper">
          <slot>
            <!-- 默认内容 -->
            <div class="welcome-content">
              <h2>欢迎使用油气管道监测管理系统</h2>
              <p>请从左侧导航栏选择功能模块</p>
            </div>
          </slot>
        </div>
      </main>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MainLayout',
  data() {
    return {
      activeMenu: 'visualization-overview',
      expandedMenus: ['visualization'],
      sidebarVisible: false
    }
  },
  mounted() {
    // 初始化时，如果是数据可视化模块，确保导航栏隐藏
    if (this.isVisualizationActive) {
      this.sidebarVisible = false;
    }
  },
  computed: {
    isVisualizationActive() {
      return this.activeMenu.startsWith('visualization') || 
             this.activeMenu === 'area-details' || 
             this.activeMenu === 'pipeline-details' || 
             this.activeMenu === 'task-details';
    }
  },
  methods: {
    setActiveMenu(menu) {
      this.activeMenu = menu;
      this.$emit('menu-change', menu);
      
      // 如果选择的是数据可视化相关菜单，自动隐藏侧边栏
      if (this.isVisualizationActive) {
        this.sidebarVisible = false;
        // 确保数据可视化子菜单展开
        if (!this.expandedMenus.includes('visualization')) {
          this.expandedMenus.push('visualization');
        }
      }
    },
    toggleSubmenu(menuName) {
      const index = this.expandedMenus.indexOf(menuName);
      if (index > -1) {
        this.expandedMenus.splice(index, 1);
      } else {
        this.expandedMenus.push(menuName);
      }
    },
    showSidebar() {
      if (this.isVisualizationActive) {
        this.sidebarVisible = true;
      }
    },
    hideSidebar() {
      if (this.isVisualizationActive) {
        this.sidebarVisible = false;
      }
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

.header-left .system-title {
  font-size: 20px;
  font-weight: bold;
  margin: 0;
}

.header-right .user-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
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
  transition: transform 0.3s ease, width 0.3s ease;
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

/* 展开图标样式 */
.expand-icon {
  font-size: 10px;
  transition: transform 0.3s ease;
  margin-left: 10px;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

/* 子菜单样式 */
.submenu {
  list-style: none;
  padding: 0;
  margin: 0;
  background-color: rgba(255, 255, 255, 0.05);
  border-left: 3px solid #3B82F6;
}

.nav-child {
  padding: 12px 20px 12px 40px;
  font-size: 13px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.nav-child:hover {
  background-color: rgba(255, 255, 255, 0.1);
  transform: translateX(5px);
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
}

.content-wrapper {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 欢迎内容样式 */
.welcome-content {
  text-align: center;
  padding: 50px 20px;
  color: #333;
}

.welcome-content h2 {
  font-size: 24px;
  margin-bottom: 10px;
  color: #1E3A8A;
}

.welcome-content p {
  font-size: 16px;
  color: #666;
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
</style>