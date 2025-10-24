<template>
  <view class="home-container">
    <!-- 自定义导航栏 -->
    <view class="custom-navbar">
      <!-- 消息通知中心 -->
      <view class="navbar-left" @click="goToNotifications">
        <text class="notification-icon">🔔</text>
        <view class="notification-badge" v-if="notificationCount > 0">
          <text class="badge-text">{{ notificationCount > 99 ? '99+' : notificationCount }}</text>
        </view>
      </view>
      
      <text class="navbar-title">主页</text>
      
      <!-- 今日待办任务 -->
      <view class="navbar-right" @click="goToTodayTasks">
        <text class="todo-icon">📋</text>
        <view class="todo-badge" v-if="todayTaskCount > 0">
          <text class="badge-text">{{ todayTaskCount > 99 ? '99+' : todayTaskCount }}</text>
        </view>
      </view>
    </view>
    
    <!-- 页面内容 -->
    <view class="content">
      <!-- 1. 顶部任务数据统计容器 -->
      <view class="task-stats-container">
        <text class="section-title">当前区域检修任务</text>
        <view class="stats-grid">
          <view class="stat-item">
            <text class="stat-number">{{ taskStats.published }}</text>
            <text class="stat-label">已发布</text>
          </view>
          <view class="stat-item">
            <text class="stat-number">{{ taskStats.accepted }}</text>
            <text class="stat-label">已接取</text>
          </view>
          <view class="stat-item">
            <text class="stat-number">{{ taskStats.pending }}</text>
            <text class="stat-label">等待接取</text>
          </view>
        </view>
      </view>

      <!-- 2. 分配任务容器 -->
      <view class="assigned-tasks-container">
        <text class="section-title">我的分配任务</text>
        <view class="task-list">
          <view class="task-item" v-for="task in assignedTasks" :key="task.id">
            <view class="task-header">
              <text class="task-name">{{ task.name }}</text>
              <view class="task-status" :class="'status-' + task.status">
                <text class="status-text">{{ getStatusText(task.status) }}</text>
              </view>
            </view>
            <view class="task-details">
              <text class="task-type">{{ task.type }}</text>
              <text class="task-time">{{ task.startTime }} - {{ task.endTime }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 3. 管道网络地图展示 -->
      <view class="map-container">
        <text class="section-title">管道网络地图</text>
        <view class="map-placeholder">
          <text class="placeholder-text">地图展示区域</text>
          <text class="placeholder-subtitle">ECharts地图将在此处显示</text>
        </view>
      </view>

      <!-- 4. 功能按钮区域 -->
      <view class="function-buttons">
        <view class="button-row">
          <view class="function-btn" @click="goToInspectionReport">
            <text class="btn-icon">📊</text>
            <text class="btn-text">巡检事件上报</text>
          </view>
          <view class="function-btn" @click="goToVirtualExpert">
            <text class="btn-icon">🤖</text>
            <text class="btn-text">虚拟专家</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'Home',
  data() {
    return {
      // 通知数量
      notificationCount: 5,
      // 今日待办任务数量
      todayTaskCount: 3,
      // 任务统计数据
      taskStats: {
        published: 12,
        accepted: 8,
        pending: 4
      },
      // 分配给当前检修员的任务
      assignedTasks: [
        {
          id: 1,
          name: '管道A段压力检测',
          type: '压力检测',
          status: 'in_progress',
          startTime: '2024-01-15 09:00',
          endTime: '2024-01-15 17:00'
        },
        {
          id: 2,
          name: '阀门B区域维护',
          type: '设备维护',
          status: 'pending',
          startTime: '2024-01-16 08:30',
          endTime: '2024-01-16 16:30'
        },
        {
          id: 3,
          name: '管道C段巡检',
          type: '日常巡检',
          status: 'completed',
          startTime: '2024-01-14 10:00',
          endTime: '2024-01-14 15:00'
        }
      ]
    }
  },
  methods: {
    // 获取状态文本
    getStatusText(status) {
      const statusMap = {
        'pending': '待开始',
        'in_progress': '进行中',
        'completed': '已完成',
        'overdue': '已逾期'
      }
      return statusMap[status] || '未知'
    },
    // 跳转到消息通知中心
    goToNotifications() {
      console.log('跳转到消息通知中心')
      // uni.navigateTo({
      //   url: '/pages/notifications/notifications'
      // })
    },
    // 跳转到今日待办任务
    goToTodayTasks() {
      console.log('跳转到今日待办任务')
      // uni.navigateTo({
      //   url: '/pages/task/task'
      // })
    },
    // 跳转到巡检事件上报
    goToInspectionReport() {
      console.log('跳转到巡检事件上报页面')
      // uni.navigateTo({
      //   url: '/pages/inspection/inspection'
      // })
    },
    // 跳转到虚拟专家
    goToVirtualExpert() {
      console.log('跳转到虚拟专家页面')
      // uni.navigateTo({
      //   url: '/pages/expert/expert'
      // })
    },
    // 加载任务统计数据
    loadTaskStats() {
      // 这里可以调用API获取真实数据
      console.log('加载任务统计数据')
    },
    // 加载分配任务数据
    loadAssignedTasks() {
      // 这里可以调用API获取真实数据
      console.log('加载分配任务数据')
    }
  },
  onLoad() {
    // 页面加载时获取数据
    this.loadTaskStats()
    this.loadAssignedTasks()
  }
}
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background-color: #F5F7FA;
}

.custom-navbar {
  height: 88rpx;
  background: linear-gradient(135deg, #1A3366 0%, #2B4A7A 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 44rpx 30rpx 0;
  box-shadow: 0 2rpx 8rpx rgba(26, 51, 102, 0.15);
  position: relative;
}

.navbar-left, .navbar-right {
  position: relative;
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.notification-icon, .todo-icon {
  font-size: 40rpx;
  color: #FFFFFF;
}

.notification-badge, .todo-badge {
  position: absolute;
  top: -10rpx;
  right: -10rpx;
  background: #FF4757;
  border-radius: 20rpx;
  min-width: 32rpx;
  height: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 8rpx;
}

.badge-text {
  color: #FFFFFF;
  font-size: 20rpx;
  font-weight: 600;
}

.navbar-title {
  color: #FFFFFF;
  font-size: 36rpx;
  font-weight: 600;
  font-family: PingFang SC, Microsoft YaHei, sans-serif;
}

.content {
  padding: 30rpx 30rpx 100rpx;
}

.section-title {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
  color: #1A3366;
  margin-bottom: 24rpx;
}

/* 任务统计容器 */
.task-stats-container {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(26, 51, 102, 0.08);
}

.stats-grid {
  display: flex;
  gap: 20rpx;
}

.stat-item {
  flex: 1;
  background: linear-gradient(135deg, #E3F2FD 0%, #F0F8FF 100%);
  border-radius: 16rpx;
  padding: 30rpx 20rpx;
  text-align: center;
  border: 2rpx solid #E1F5FE;
  transition: all 0.3s ease;
}

.stat-item:active {
  transform: scale(0.98);
  background: linear-gradient(135deg, #BBDEFB 0%, #E3F2FD 100%);
}

.stat-number {
  display: block;
  font-size: 44rpx;
  font-weight: 700;
  color: #1A3366;
  margin-bottom: 8rpx;
}

.stat-label {
  display: block;
  font-size: 24rpx;
  color: #546E7A;
}

/* 分配任务容器 */
.assigned-tasks-container {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(26, 51, 102, 0.08);
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.task-item {
  background: #F8FAFE;
  border-radius: 16rpx;
  padding: 24rpx;
  border-left: 6rpx solid #1A3366;
  transition: all 0.3s ease;
}

.task-item:active {
  transform: translateX(8rpx);
  background: #F0F8FF;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.task-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A3366;
  flex: 1;
}

.task-status {
  padding: 8rpx 16rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
}

.status-pending {
  background: #FFF3E0;
  color: #F57C00;
}

.status-in_progress {
  background: #E8F5E8;
  color: #2E7D32;
}

.status-completed {
  background: #E3F2FD;
  color: #1976D2;
}

.status-overdue {
  background: #FFEBEE;
  color: #D32F2F;
}

.status-text {
  font-weight: 600;
}

.task-details {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-type {
  font-size: 24rpx;
  color: #546E7A;
  background: #E1F5FE;
  padding: 6rpx 12rpx;
  border-radius: 12rpx;
}

.task-time {
  font-size: 24rpx;
  color: #7A7E83;
}

/* 地图容器 */
.map-container {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(26, 51, 102, 0.08);
}

.map-placeholder {
  height: 400rpx;
  background: linear-gradient(135deg, #F0F8FF 0%, #E3F2FD 100%);
  border-radius: 16rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2rpx dashed #90CAF9;
}

.placeholder-text {
  font-size: 32rpx;
  color: #1A3366;
  font-weight: 600;
  margin-bottom: 12rpx;
}

.placeholder-subtitle {
  font-size: 24rpx;
  color: #546E7A;
}

/* 功能按钮区域 */
.function-buttons {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(26, 51, 102, 0.08);
}

.button-row {
  display: flex;
  gap: 20rpx;
}

.function-btn {
  flex: 1;
  background: linear-gradient(135deg, #1A3366 0%, #2B4A7A 100%);
  border-radius: 16rpx;
  padding: 40rpx 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 20rpx rgba(26, 51, 102, 0.2);
  transition: all 0.3s ease;
}

.function-btn:active {
  transform: translateY(4rpx);
  box-shadow: 0 2rpx 8rpx rgba(26, 51, 102, 0.3);
}

.btn-icon {
  font-size: 48rpx;
  margin-bottom: 16rpx;
}

.btn-text {
  color: #FFFFFF;
  font-size: 26rpx;
  font-weight: 600;
  text-align: center;
}
</style>