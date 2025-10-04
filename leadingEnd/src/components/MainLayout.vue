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
      <!-- 左侧导航栏 -->
      <nav class="sidebar">
        <ul class="nav-menu">
          <li class="nav-item" @click="setActiveMenu('visualization')">
            <span class="nav-text">数据可视化</span>
          </li>
          <li class="nav-item" @click="setActiveMenu('analysis')">
            <span class="nav-text">数据分析</span>
          </li>
          <li class="nav-item" @click="setActiveMenu('monitoring')">
            <span class="nav-text">监测数据</span>
          </li>
          <li class="nav-item" @click="setActiveMenu('equipment')">
            <span class="nav-text">设备信息</span>
          </li>
          <li class="nav-item" @click="setActiveMenu('tasks')">
            <span class="nav-text">任务管理</span>
          </li>
          <li class="nav-item" @click="setActiveMenu('maintenance')">
            <span class="nav-text">检修员信息</span>
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
      activeMenu: 'visualization'
    }
  },
  methods: {
    setActiveMenu(menu) {
      this.activeMenu = menu;
      this.$emit('menu-change', menu);
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
}

/* 左侧导航栏样式 */
.sidebar {
  width: 200px;
  background-color: #1E3A8A;
  color: #FFFFFF;
  overflow-y: auto;
  flex-shrink: 0;
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
  display: block;
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