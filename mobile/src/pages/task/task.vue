<template>
  <view class="task-container">
    <!-- 自定义导航栏 -->
    <view class="custom-navbar">
      <text class="navbar-title">任务</text>
    </view>
    
    <!-- 页面内容 -->
    <view class="content">
      <view class="task-stats">
        <view class="stat-card">
          <text class="stat-number">12</text>
          <text class="stat-label">待处理</text>
        </view>
        <view class="stat-card">
          <text class="stat-number">8</text>
          <text class="stat-label">进行中</text>
        </view>
        <view class="stat-card">
          <text class="stat-number">25</text>
          <text class="stat-label">已完成</text>
        </view>
      </view>
      
      <view class="task-list">
        <text class="section-title">任务列表</text>
        <view class="task-item" v-for="(task, index) in taskList" :key="index">
          <view class="task-header">
            <text class="task-title">{{ task.title }}</text>
            <view class="task-status" :class="task.status">
              <text class="status-text">{{ task.statusText }}</text>
            </view>
          </view>
          <text class="task-desc">{{ task.description }}</text>
          <view class="task-footer">
            <text class="task-time">{{ task.time }}</text>
            <text class="task-priority" :class="task.priority">{{ task.priorityText }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'Task',
  data() {
    return {
      taskList: [
        {
          title: '管道巡检任务',
          description: '对A区域管道进行例行巡检，检查管道完整性',
          time: '2024-01-15 09:00',
          status: 'pending',
          statusText: '待处理',
          priority: 'high',
          priorityText: '高优先级'
        },
        {
          title: '压力监测校准',
          description: '对压力传感器进行校准和维护',
          time: '2024-01-15 14:00',
          status: 'processing',
          statusText: '进行中',
          priority: 'medium',
          priorityText: '中优先级'
        },
        {
          title: '数据备份任务',
          description: '执行系统数据的定期备份操作',
          time: '2024-01-14 18:00',
          status: 'completed',
          statusText: '已完成',
          priority: 'low',
          priorityText: '低优先级'
        }
      ]
    }
  },
  onLoad() {
    
  }
}
</script>

<style scoped>
.task-container {
  min-height: 100vh;
  background-color: #F5F7FA;
}

.custom-navbar {
  height: 88rpx;
  background: linear-gradient(135deg, #1A3366 0%, #2B4A7A 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: 44rpx;
  box-shadow: 0 2rpx 8rpx rgba(26, 51, 102, 0.15);
}

.navbar-title {
  color: #FFFFFF;
  font-size: 36rpx;
  font-weight: 600;
  font-family: PingFang SC, Microsoft YaHei, sans-serif;
}

.content {
  padding: 40rpx 30rpx;
}

.task-stats {
  display: flex;
  gap: 20rpx;
  margin-bottom: 40rpx;
}

.stat-card {
  flex: 1;
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 30rpx 20rpx;
  text-align: center;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.stat-number {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  color: #1A3366;
  margin-bottom: 10rpx;
}

.stat-label {
  display: block;
  font-size: 22rpx;
  color: #7A7E83;
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1A3366;
  margin-bottom: 30rpx;
  display: block;
}

.task-item {
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease;
}

.task-item:active {
  transform: scale(0.98);
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15rpx;
}

.task-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A3366;
  flex: 1;
}

.task-status {
  padding: 8rpx 16rpx;
  border-radius: 20rpx;
  font-size: 20rpx;
}

.task-status.pending {
  background: #FFF7E6;
  color: #FA8C16;
}

.task-status.processing {
  background: #E6F7FF;
  color: #1890FF;
}

.task-status.completed {
  background: #F6FFED;
  color: #52C41A;
}

.status-text {
  font-size: 20rpx;
}

.task-desc {
  font-size: 24rpx;
  color: #7A7E83;
  line-height: 1.5;
  margin-bottom: 20rpx;
  display: block;
}

.task-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-time {
  font-size: 22rpx;
  color: #7A7E83;
}

.task-priority {
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  border-radius: 12rpx;
}

.task-priority.high {
  background: #FFEBEE;
  color: #F44336;
}

.task-priority.medium {
  background: #FFF3E0;
  color: #FF9800;
}

.task-priority.low {
  background: #E8F5E8;
  color: #4CAF50;
}
</style>