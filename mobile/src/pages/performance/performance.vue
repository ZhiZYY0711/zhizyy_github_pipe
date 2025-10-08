<template>
  <view class="performance-container">
    <!-- 自定义导航栏 -->
    <view class="custom-navbar">
      <text class="navbar-title">绩效</text>
    </view>
    
    <!-- 页面内容 -->
    <view class="content">
      <view class="performance-overview">
        <text class="section-title">绩效概览</text>
        <view class="overview-card">
          <view class="score-section">
            <text class="score-number">92</text>
            <text class="score-label">综合评分</text>
          </view>
          <view class="trend-section">
            <text class="trend-text">较上月提升 5%</text>
            <text class="trend-icon">↗</text>
          </view>
        </view>
      </view>
      
      <view class="metrics-grid">
        <view class="metric-item" v-for="(metric, index) in metrics" :key="index">
          <text class="metric-title">{{ metric.title }}</text>
          <view class="metric-content">
            <text class="metric-value">{{ metric.value }}</text>
            <text class="metric-unit">{{ metric.unit }}</text>
          </view>
          <view class="metric-progress">
            <view class="progress-bar">
              <view class="progress-fill" :style="{ width: metric.progress + '%' }"></view>
            </view>
            <text class="progress-text">{{ metric.progress }}%</text>
          </view>
        </view>
      </view>
      
      <view class="performance-chart">
        <text class="section-title">月度趋势</text>
        <view class="chart-placeholder">
          <text class="placeholder-text">绩效趋势图</text>
          <text class="placeholder-desc">月度绩效变化趋势将在此显示</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'Performance',
  data() {
    return {
      metrics: [
        {
          title: '任务完成率',
          value: '95',
          unit: '%',
          progress: 95
        },
        {
          title: '响应及时性',
          value: '88',
          unit: '%',
          progress: 88
        },
        {
          title: '设备正常率',
          value: '97',
          unit: '%',
          progress: 97
        },
        {
          title: '安全指标',
          value: '100',
          unit: '%',
          progress: 100
        }
      ]
    }
  },
  onLoad() {
    
  }
}
</script>

<style scoped>
.performance-container {
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

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1A3366;
  margin-bottom: 30rpx;
  display: block;
}

.overview-card {
  background: linear-gradient(135deg, #1A3366 0%, #2B4A7A 100%);
  border-radius: 20rpx;
  padding: 40rpx;
  margin-bottom: 40rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 8rpx 24rpx rgba(26, 51, 102, 0.15);
}

.score-section {
  text-align: center;
}

.score-number {
  display: block;
  font-size: 72rpx;
  font-weight: 700;
  color: #FFFFFF;
  margin-bottom: 10rpx;
}

.score-label {
  display: block;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
}

.trend-section {
  text-align: right;
}

.trend-text {
  display: block;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 10rpx;
}

.trend-icon {
  font-size: 36rpx;
  color: #52C41A;
}

.metrics-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
  margin-bottom: 40rpx;
}

.metric-item {
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 30rpx 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.metric-title {
  font-size: 24rpx;
  color: #7A7E83;
  display: block;
  margin-bottom: 20rpx;
}

.metric-content {
  display: flex;
  align-items: baseline;
  margin-bottom: 20rpx;
}

.metric-value {
  font-size: 48rpx;
  font-weight: 700;
  color: #1A3366;
}

.metric-unit {
  font-size: 24rpx;
  color: #7A7E83;
  margin-left: 5rpx;
}

.metric-progress {
  display: flex;
  align-items: center;
  gap: 15rpx;
}

.progress-bar {
  flex: 1;
  height: 8rpx;
  background: #F0F0F0;
  border-radius: 4rpx;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #1A3366 0%, #2B4A7A 100%);
  border-radius: 4rpx;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 20rpx;
  color: #7A7E83;
  min-width: 60rpx;
}

.chart-placeholder {
  background: #FFFFFF;
  border-radius: 16rpx;
  height: 400rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.placeholder-text {
  font-size: 32rpx;
  color: #1A3366;
  font-weight: 600;
  margin-bottom: 20rpx;
}

.placeholder-desc {
  font-size: 24rpx;
  color: #7A7E83;
}
</style>