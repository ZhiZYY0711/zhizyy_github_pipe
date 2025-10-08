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
        @mouseleave="handleSidebarMouseLeave"
        @mouseenter="showSidebar"
      >
        <ul class="nav-menu">
          <!-- 数据可视化模块 - 可展开 -->
          <li class="nav-item nav-parent" @click="navigateToVisualization">
            <span class="nav-text">
              数据可视化
              <i class="expand-icon" :class="{ 'expanded': expandedMenus.includes('visualization') }">▼</i>
            </span>
          </li>
          <transition name="submenu-slide">
            <ul class="submenu" v-show="expandedMenus.includes('visualization')">
              <li class="nav-item nav-child" @click="navigateTo('/main/data-monitoring')">
                <span class="nav-text">数据监控</span>
              </li>
              <li class="nav-item nav-child" @click="navigateTo('/main/task-details')">
                <span class="nav-text">任务详情</span>
              </li>
            </ul>
          </transition>
          
          <!-- 数据管理模块 - 可展开 -->
          <li class="nav-item nav-parent" @click="toggleSubmenu('dataManagement')">
            <span class="nav-text">
              数据管理
              <i class="expand-icon" :class="{ 'expanded': expandedMenus.includes('dataManagement') }">▼</i>
            </span>
          </li>
          <transition name="submenu-slide">
            <ul class="submenu" v-show="expandedMenus.includes('dataManagement')">
              <li class="nav-item nav-child" @click="navigateTo('/main/monitoring')">
                <span class="nav-text">监测数据</span>
              </li>
              <li class="nav-item nav-child" @click="navigateTo('/main/equipment')">
                <span class="nav-text">设备信息</span>
              </li>
              <li class="nav-item nav-child" @click="navigateTo('/main/tasks')">
                <span class="nav-text">任务管理</span>
              </li>
              <li class="nav-item nav-child" @click="navigateTo('/main/repairman')">
                <span class="nav-text">检修员信息</span>
              </li>
            </ul>
          </transition>
          
          <!-- 新增模块 -->
          <li class="nav-item" @click="navigateTo('/main/virtual-expert')">
            <span class="nav-text">虚拟专家</span>
          </li>
          <li class="nav-item" @click="navigateTo('/main/simulation-drill')">
            <span class="nav-text">模拟与演练</span>
          </li>
          <li class="nav-item" @click="navigateTo('/main/emergency')">
            <span class="nav-text">事故响应</span>
          </li>
          <li class="nav-item" @click="navigateTo('/main/logs')">
            <span class="nav-text">日志记录</span>
          </li>
        </ul>
      </nav>

      <!-- 右侧内容区域 -->
      <main class="content-area">
        <div class="content-wrapper">
          <!-- 使用router-view显示子路由内容 -->
          <router-view></router-view>
        </div>
      </main>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Layout',
  data() {
    return {
      expandedMenus: ['visualization'],
      sidebarVisible: false,
      waitingForClickToHide: false, // 新增：标记是否处于等待点击隐藏的状态
      documentClickListener: null // 新增：存储全局点击事件的引用
    }
  },
  computed: {
    isVisualizationActive() {
      const route = this.$route.path;
      return this.isPathInVisualization(route);
    }
  },
  mounted() {
    // 初始化时，如果是数据可视化模块，确保导航栏隐藏
    if (this.isVisualizationActive) {
      this.sidebarVisible = false;
    }
  },
  methods: {
    isPathInVisualization(path) {
      return path.includes('visualization') || 
             path.includes('area-details') || 
             path.includes('pipeline-details') || 
             path.includes('task-details') ||
             path.includes('data-monitoring');
    },
    isPathInDataManagement(path) {
      return path.includes('monitoring') ||
             path.includes('equipment') ||
             path.includes('tasks') ||
             path.includes('maintenance');
    },
    navigateTo(path) {
      if (this.$route.path !== path) {
        this.$router.push(path);
      }
  
      // 根据导航路径自动展开或收起菜单
      if (this.isPathInVisualization(path)) {
        if (!this.expandedMenus.includes('visualization')) {
          this.expandedMenus.push('visualization');
        }
        const dataManagementIndex = this.expandedMenus.indexOf('dataManagement');
        if (dataManagementIndex > -1) {
          this.expandedMenus.splice(dataManagementIndex, 1);
        }
        this.showSidebar();
      } else if (this.isPathInDataManagement(path)) {
        if (!this.expandedMenus.includes('dataManagement')) {
          this.expandedMenus.push('dataManagement');
        }
        const visualizationIndex = this.expandedMenus.indexOf('visualization');
        if (visualizationIndex > -1) {
          this.expandedMenus.splice(visualizationIndex, 1);
        }
        this.showSidebar();
      } else {
        // 如果导航到非数据可视化和非数据管理相关菜单，收起所有父级菜单
        this.expandedMenus = [];
        this.hideSidebar();
      }
    },

    navigateToVisualization() {
      // 确保数据可视化菜单始终展开
      if (!this.expandedMenus.includes('visualization')) {
        this.expandedMenus.push('visualization');
      }
      // 收起数据管理菜单
      const dataManagementIndex = this.expandedMenus.indexOf('dataManagement');
      if (dataManagementIndex > -1) {
        this.expandedMenus.splice(dataManagementIndex, 1);
      }
  
      if (this.$route.path !== '/main/visualization') {
        this.$router.push('/main/visualization');
      }
      this.showSidebar();
    },

    toggleSubmenu(menuName) {
      // 如果点击的菜单已经展开，则收起；否则展开当前菜单并收起其他菜单
      if (this.expandedMenus.includes(menuName)) {
        const index = this.expandedMenus.indexOf(menuName);
        this.expandedMenus.splice(index, 1);
      } else {
        this.expandedMenus = [menuName];
      }
    },

    showSidebar() {
      if (this.isVisualizationActive) {
        this.sidebarVisible = true;
        this.waitingForClickToHide = false; // 侧边栏显示时，取消等待点击隐藏的状态
        // 移除可能存在的全局点击监听器
        if (this.documentClickListener) {
          document.body.removeEventListener('click', this.documentClickListener);
          this.documentClickListener = null;
        }
      }
    },

    hideSidebar() {
      if (this.isVisualizationActive) {
        this.sidebarVisible = false;
        this.waitingForClickToHide = false; // 侧边栏隐藏时，取消等待点击隐藏的状态
        // 移除可能存在的全局点击监听器
        if (this.documentClickListener) {
          document.body.removeEventListener('click', this.documentClickListener);
          this.documentClickListener = null;
        }
      }
    },

    // 新增方法：处理鼠标移出侧边栏事件
    handleSidebarMouseLeave() {
      if (this.isVisualizationActive) {
        this.waitingForClickToHide = true;
        // 只有在没有监听器的情况下才添加
        if (!this.documentClickListener) {
          this.documentClickListener = (event) => {
            const sidebarElement = this.$el.querySelector('.sidebar');
            const toggleButtonElement = this.$el.querySelector('.sidebar-toggle-btn');

            // 如果处于等待隐藏状态，且点击目标不在侧边栏和切换按钮内部，则隐藏侧边栏
            if (this.waitingForClickToHide &&
                sidebarElement && !sidebarElement.contains(event.target) &&
                (!toggleButtonElement || !toggleButtonElement.contains(event.target))) {
              this.hideSidebar();
            }
          };
          document.body.addEventListener('click', this.documentClickListener);
        }
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
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.03));
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
  border-radius: 0 2px 2px 0;
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
</style>