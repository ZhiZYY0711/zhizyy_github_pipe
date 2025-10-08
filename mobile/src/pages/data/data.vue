<template>
  <view class="data-container">
    <!-- 自定义导航栏 -->
    <view class="custom-navbar">
      <text class="navbar-title">数据</text>
    </view>
    
    <!-- 页面内容 -->
    <view class="content">
      <view class="data-overview">
        <text class="section-title">数据概览</text>
        <view class="chart-placeholder">
          <text class="placeholder-text">数据图表区域</text>
          <text class="placeholder-desc">实时监测数据将在此显示</text>
        </view>
      </view>
      
      <view class="data-list">
        <text class="section-title">最新数据</text>
        <view class="data-item" v-for="(item, index) in dataList" :key="index">
          <view class="data-info">
            <text class="data-name">{{ item.name }}</text>
            <text class="data-time">{{ item.time }}</text>
          </view>
          <view class="data-value">
            <text class="value-number" :class="item.status">{{ item.value }}</text>
            <text class="value-unit">{{ item.unit }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'Data',
  data() {
    return {
      dataList: [
        { name: '管道压力', value: '2.5', unit: 'MPa', time: '2分钟前', status: 'normal' },
        { name: '流量监测', value: '156.8', unit: 'm³/h', time: '3分钟前', status: 'normal' },
        { name: '温度监测', value: '45.2', unit: '°C', time: '5分钟前', status: 'warning' },
        { name: '泄漏检测', value: '正常', unit: '', time: '1分钟前', status: 'normal' }
      ]
    }
  },
  onLoad() {
    
  }
}
</script>

<style scoped>
.data-container {
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

.data-overview {
  margin-bottom: 50rpx;
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

.data-item {
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease;
}

.data-item:active {
  transform: scale(0.98);
}

.data-info {
  flex: 1;
}

.data-name {
  font-size: 28rpx;
  color: #1A3366;
  font-weight: 600;
  display: block;
  margin-bottom: 10rpx;
}

.data-time {
  font-size: 22rpx;
  color: #7A7E83;
  display: block;
}

.data-value {
  text-align: right;
}

.value-number {
  font-size: 32rpx;
  font-weight: 700;
  display: block;
  margin-bottom: 5rpx;
}

.value-number.normal {
  color: #52C41A;
}

.value-number.warning {
  color: #FA8C16;
}

.value-unit {
  font-size: 20rpx;
  color: #7A7E83;
  display: block;
}
</style>