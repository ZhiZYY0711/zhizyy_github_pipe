<template>
  <view class="mine-page">
    <!-- 自定义导航栏 -->
    <view class="custom-navbar">
      <view class="navbar-title">我的</view>
      <view class="navbar-setting" @click="goToSettings">
        <text class="setting-icon">⚙️</text>
      </view>
    </view>

    <!-- 个人信息卡片 -->
    <view class="profile-card">
      <view class="profile-header">
        <view class="avatar-container" @click="changeAvatar">
          <image class="avatar" :src="userInfo.avatar" mode="aspectFill"></image>
          <view class="avatar-edit">📷</view>
        </view>
        <view class="profile-info">
          <view class="name-id">
            <text class="name">{{ userInfo.name }}</text>
            <text class="employee-id">ID: {{ userInfo.employeeId }}</text>
          </view>
          <view class="area">{{ userInfo.area }}</view>
        </view>
      </view>
      
      <!-- 实时状态 -->
      <view class="status-container">
        <view class="status-badge" :class="statusClass">
          <text class="status-dot"></text>
          <text class="status-text">{{ statusText }}</text>
        </view>
      </view>
    </view>

    <!-- 本月工作概览 -->
    <view class="work-overview">
      <view class="overview-title">本月工作概览</view>
      <view class="overview-grid">
        <view class="overview-item">
          <view class="overview-number">{{ workData.completedTasks }}/{{ workData.totalTasks }}</view>
          <view class="overview-label">任务数</view>
        </view>
        <view class="overview-item">
          <view class="overview-number">{{ workData.performanceScore }}</view>
          <view class="overview-label">绩效分</view>
        </view>
        <view class="overview-item">
          <view class="overview-number">{{ workData.inspectionDistance }}km</view>
          <view class="overview-label">巡检里程</view>
        </view>
      </view>
    </view>

    <!-- 功能菜单 -->
    <view class="menu-section">
      <view class="menu-item" @click="goToReports">
        <view class="menu-icon">📋</view>
        <view class="menu-text">我的报告</view>
        <view class="menu-arrow">></view>
      </view>
      <view class="menu-item" @click="goToFavorites">
        <view class="menu-icon">⭐</view>
        <view class="menu-text">我的收藏</view>
        <view class="menu-arrow">></view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'MinePage',
  data() {
    return {
      userInfo: {
        name: '张三',
        employeeId: 'EMP001',
        area: '华北区域',
        avatar: '/static/logo.png'
      },
      status: 'working', // working, resting, onDuty
      workData: {
        completedTasks: 15,
        totalTasks: 20,
        performanceScore: 85,
        inspectionDistance: 120
      }
    }
  },
  computed: {
    statusClass() {
      return `status-${this.status}`;
    },
    statusText() {
      const statusMap = {
        working: '在岗',
        resting: '休息',
        onDuty: '值班'
      };
      return statusMap[this.status] || '未知';
    }
  },
  methods: {
    changeAvatar() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          this.userInfo.avatar = res.tempFilePaths[0];
          uni.showToast({
            title: '头像更新成功',
            icon: 'success'
          });
        }
      });
    },
    goToSettings() {
      uni.navigateTo({
        url: '/pages/mine/settings'
      });
    },
    goToReports() {
      uni.navigateTo({
        url: '/pages/mine/reports'
      });
    },
    goToFavorites() {
      uni.navigateTo({
        url: '/pages/mine/favorites'
      });
    }
  }
}
</script>

<style scoped>
.mine-page {
  background-color: #F5F7FA;
  min-height: 100vh;
}

/* 自定义导航栏 */
.custom-navbar {
  background: linear-gradient(135deg, #1A3366 0%, #2B4A7A 100%);
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32rpx;
  padding-top: var(--status-bar-height);
}

.navbar-title {
  color: white;
  font-size: 36rpx;
  font-weight: 600;
}

.navbar-setting {
  padding: 8rpx;
}

.setting-icon {
  font-size: 40rpx;
  color: white;
}

/* 个人信息卡片 */
.profile-card {
  background: white;
  margin: 24rpx;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 4rpx 20rpx rgba(26, 51, 102, 0.1);
}

.profile-header {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
}

.avatar-container {
  position: relative;
  margin-right: 24rpx;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  border: 4rpx solid #1A3366;
}

.avatar-edit {
  position: absolute;
  bottom: -8rpx;
  right: -8rpx;
  background: #1A3366;
  width: 48rpx;
  height: 48rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
}

.profile-info {
  flex: 1;
}

.name-id {
  display: flex;
  align-items: center;
  margin-bottom: 8rpx;
}

.name {
  font-size: 36rpx;
  font-weight: 600;
  color: #333;
  margin-right: 16rpx;
}

.employee-id {
  font-size: 24rpx;
  color: #999;
  background: #F5F7FA;
  padding: 4rpx 12rpx;
  border-radius: 12rpx;
}

.area {
  font-size: 28rpx;
  color: #666;
}

.status-container {
  display: flex;
  justify-content: flex-end;
}

.status-badge {
  display: flex;
  align-items: center;
  padding: 8rpx 16rpx;
  border-radius: 20rpx;
  font-size: 24rpx;
}

.status-working {
  background: rgba(76, 217, 100, 0.1);
  color: #4cd964;
}

.status-resting {
  background: rgba(255, 149, 0, 0.1);
  color: #ff9500;
}

.status-onDuty {
  background: rgba(26, 51, 102, 0.1);
  color: #1A3366;
}

.status-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 6rpx;
  background: currentColor;
  margin-right: 8rpx;
}

/* 工作概览 */
.work-overview {
  background: white;
  margin: 0 24rpx 24rpx;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 4rpx 20rpx rgba(26, 51, 102, 0.1);
}

.overview-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 24rpx;
}

.overview-grid {
  display: flex;
  justify-content: space-between;
}

.overview-item {
  text-align: center;
  flex: 1;
}

.overview-number {
  font-size: 48rpx;
  font-weight: 700;
  color: #1A3366;
  margin-bottom: 8rpx;
}

.overview-label {
  font-size: 24rpx;
  color: #999;
}

/* 功能菜单 */
.menu-section {
  background: white;
  margin: 0 24rpx;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 20rpx rgba(26, 51, 102, 0.1);
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 32rpx;
  border-bottom: 1rpx solid #F5F7FA;
  transition: background-color 0.3s;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item:active {
  background-color: #F5F7FA;
}

.menu-icon {
  font-size: 40rpx;
  margin-right: 24rpx;
  width: 60rpx;
  text-align: center;
}

.menu-text {
  flex: 1;
  font-size: 32rpx;
  color: #333;
}

.menu-arrow {
  font-size: 32rpx;
  color: #999;
}
</style>