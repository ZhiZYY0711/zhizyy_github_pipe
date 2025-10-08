<template>
  <view class="home-container">
    <!-- 自定义导航栏 -->
    <view class="custom-navbar">
      <text class="navbar-title">主页</text>
    </view>
    
    <!-- 页面内容 -->
    <view class="content">
      <view class="welcome-card">
        <text class="welcome-title">欢迎使用油气管道监测系统</text>
        <text class="welcome-subtitle">实时监控，智能管理</text>
      </view>
      
      <view class="stats-grid">
        <view class="stat-item">
          <text class="stat-number">128</text>
          <text class="stat-label">监测点</text>
        </view>
        <view class="stat-item">
          <text class="stat-number">95%</text>
          <text class="stat-label">正常率</text>
        </view>
        <view class="stat-item">
          <text class="stat-number">24</text>
          <text class="stat-label">在线设备</text>
        </view>
        <view class="stat-item">
          <text class="stat-number">3</text>
          <text class="stat-label">待处理</text>
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
      
    }
  },
  onLoad() {
    
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

.welcome-card {
  background: linear-gradient(135deg, #1A3366 0%, #2B4A7A 100%);
  border-radius: 20rpx;
  padding: 60rpx 40rpx;
  margin-bottom: 40rpx;
  box-shadow: 0 8rpx 24rpx rgba(26, 51, 102, 0.15);
}

.welcome-title {
  color: #FFFFFF;
  font-size: 42rpx;
  font-weight: 600;
  display: block;
  margin-bottom: 20rpx;
}

.welcome-subtitle {
  color: rgba(255, 255, 255, 0.8);
  font-size: 28rpx;
  display: block;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
}

.stat-item {
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 40rpx 20rpx;
  text-align: center;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease;
}

.stat-item:active {
  transform: scale(0.98);
}

.stat-number {
  display: block;
  font-size: 48rpx;
  font-weight: 700;
  color: #1A3366;
  margin-bottom: 10rpx;
}

.stat-label {
  display: block;
  font-size: 24rpx;
  color: #7A7E83;
}
</style>