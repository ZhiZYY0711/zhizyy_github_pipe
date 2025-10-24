<template>
  <view class="performance-container">
    <!-- 自定义导航栏 -->
    <view class="custom-navbar">
      <text class="navbar-title">绩效</text>
      <view class="navbar-actions">
        <text class="detail-btn" @click="toggleDetailView">{{ showDetail ? '返回' : '详情' }}</text>
      </view>
    </view>
    
    <!-- 页面内容 -->
    <view class="content">
      <!-- 原有概览视图 -->
      <view v-if="!showDetail">
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

        <!-- 团队绩效对比 -->
        <view class="team-performance">
          <text class="section-title">团队绩效对比</text>
          <view class="metric-tabs">
            <view 
              class="tab-item" 
              :class="{ active: activeTab === tab.key }"
              v-for="tab in performanceTabs" 
              :key="tab.key"
              @click="switchTab(tab.key)"
            >
              <text class="tab-text">{{ tab.label }}</text>
            </view>
          </view>
          <view class="ranking-list">
            <view class="ranking-item" v-for="(item, index) in currentRanking" :key="index">
              <view class="rank-info">
                <text class="rank-number">{{ index + 1 }}</text>
                <text class="member-name">{{ item.name }}</text>
              </view>
              <view class="rank-value">
                <text class="value-text">{{ item.value }}</text>
                <text class="value-unit">{{ item.unit }}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 操作按钮 -->
        <view class="action-buttons">
          <view class="action-btn primary" @click="goToAssessmentDetail">
            <text class="btn-text">考核明细</text>
          </view>
          <view class="action-btn secondary" @click="goToAppeal">
            <text class="btn-text">申诉入口</text>
          </view>
        </view>
      </view>

      <!-- 详细绩效数据视图 -->
      <view v-else class="detail-view">
        <view class="detail-container">
          <text class="section-title">详细绩效数据</text>
          <view class="detail-metrics">
            <view class="detail-metric-item" v-for="(item, index) in detailMetrics" :key="index">
              <view class="metric-header">
                <text class="metric-name">{{ item.name }}</text>
                <text class="metric-main-value">{{ item.value }}{{ item.unit }}</text>
              </view>
              <view class="metric-details">
                <view class="detail-row" v-for="(detail, idx) in item.details" :key="idx">
                  <text class="detail-label">{{ detail.label }}</text>
                  <text class="detail-value">{{ detail.value }}</text>
                </view>
              </view>
            </view>
          </view>
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
      showDetail: false,
      activeTab: 'score',
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
      ],
      performanceTabs: [
        { key: 'score', label: '综合评分' },
        { key: 'completed', label: '已完成数量' },
        { key: 'rate', label: '完成率' },
        { key: 'avgTime', label: '平均完成时间' },
        { key: 'safety', label: '安全指标' }
      ],
      rankingData: {
        score: [
          { name: '张三', value: '92', unit: '分' },
          { name: '李四', value: '89', unit: '分' },
          { name: '王五', value: '87', unit: '分' },
          { name: '赵六', value: '85', unit: '分' },
          { name: '钱七', value: '83', unit: '分' }
        ],
        completed: [
          { name: '张三', value: '28', unit: '个' },
          { name: '李四', value: '25', unit: '个' },
          { name: '王五', value: '23', unit: '个' },
          { name: '赵六', value: '21', unit: '个' },
          { name: '钱七', value: '19', unit: '个' }
        ],
        rate: [
          { name: '张三', value: '95', unit: '%' },
          { name: '李四', value: '92', unit: '%' },
          { name: '王五', value: '89', unit: '%' },
          { name: '赵六', value: '87', unit: '%' },
          { name: '钱七', value: '85', unit: '%' }
        ],
        avgTime: [
          { name: '张三', value: '2.3', unit: '小时' },
          { name: '李四', value: '2.8', unit: '小时' },
          { name: '王五', value: '3.1', unit: '小时' },
          { name: '赵六', value: '3.5', unit: '小时' },
          { name: '钱七', value: '3.8', unit: '小时' }
        ],
        safety: [
          { name: '张三', value: '100', unit: '%' },
          { name: '李四', value: '98', unit: '%' },
          { name: '王五', value: '96', unit: '%' },
          { name: '赵六', value: '94', unit: '%' },
          { name: '钱七', value: '92', unit: '%' }
        ]
      },
      detailMetrics: [
        {
          name: '综合评分',
          value: '92',
          unit: '分',
          details: [
            { label: '任务完成质量', value: '95分' },
            { label: '工作效率', value: '88分' },
            { label: '团队协作', value: '90分' },
            { label: '创新能力', value: '85分' }
          ]
        },
        {
          name: '已完成数量',
          value: '28',
          unit: '个',
          details: [
            { label: '本月完成', value: '28个' },
            { label: '上月完成', value: '25个' },
            { label: '环比增长', value: '12%' },
            { label: '年度累计', value: '312个' }
          ]
        },
        {
          name: '完成率',
          value: '95',
          unit: '%',
          details: [
            { label: '按时完成率', value: '95%' },
            { label: '提前完成率', value: '35%' },
            { label: '延期完成率', value: '5%' },
            { label: '平均提前时间', value: '0.5天' }
          ]
        },
        {
          name: '任务超期率',
          value: '5',
          unit: '%',
          details: [
            { label: '轻微超期', value: '3%' },
            { label: '严重超期', value: '2%' },
            { label: '平均超期时间', value: '1.2天' },
            { label: '超期原因分析', value: '设备故障' }
          ]
        },
        {
          name: '平均完成时间',
          value: '2.3',
          unit: '小时',
          details: [
            { label: '简单任务', value: '1.5小时' },
            { label: '中等任务', value: '2.8小时' },
            { label: '复杂任务', value: '4.2小时' },
            { label: '效率提升', value: '15%' }
          ]
        },
        {
          name: '设备正常率',
          value: '97',
          unit: '%',
          details: [
            { label: '设备完好率', value: '97%' },
            { label: '故障处理及时率', value: '100%' },
            { label: '预防性维护', value: '95%' },
            { label: '设备利用率', value: '88%' }
          ]
        },
        {
          name: '安全指标',
          value: '100',
          unit: '%',
          details: [
            { label: '安全操作规范', value: '100%' },
            { label: '安全培训参与', value: '100%' },
            { label: '事故发生次数', value: '0次' },
            { label: '安全隐患上报', value: '5次' }
          ]
        }
      ]
    }
  },
  computed: {
    currentRanking() {
      return this.rankingData[this.activeTab] || []
    }
  },
  methods: {
    toggleDetailView() {
      this.showDetail = !this.showDetail
    },
    switchTab(tabKey) {
      this.activeTab = tabKey
    },
    goToAssessmentDetail() {
      uni.showToast({
        title: '跳转到考核明细页面',
        icon: 'none'
      })
      // TODO: 实现页面跳转
      // uni.navigateTo({
      //   url: '/pages/assessment/detail'
      // })
    },
    goToAppeal() {
      uni.showToast({
        title: '跳转到申诉入口页面',
        icon: 'none'
      })
      // TODO: 实现页面跳转
      // uni.navigateTo({
      //   url: '/pages/appeal/index'
      // })
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
  justify-content: space-between;
  padding: 44rpx 30rpx 0;
  box-shadow: 0 2rpx 8rpx rgba(26, 51, 102, 0.15);
}

.navbar-title {
  color: #FFFFFF;
  font-size: 36rpx;
  font-weight: 600;
  font-family: PingFang SC, Microsoft YaHei, sans-serif;
}

.navbar-actions {
  display: flex;
  align-items: center;
}

.detail-btn {
  color: #FFFFFF;
  font-size: 28rpx;
  padding: 10rpx 20rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.3);
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

/* 团队绩效对比样式 */
.team-performance {
  margin-bottom: 40rpx;
}

.metric-tabs {
  display: flex;
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 8rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
  overflow-x: auto;
}

.tab-item {
  flex: 1;
  min-width: 120rpx;
  padding: 20rpx 15rpx;
  text-align: center;
  border-radius: 12rpx;
  transition: all 0.3s ease;
}

.tab-item.active {
  background: linear-gradient(135deg, #1A3366 0%, #2B4A7A 100%);
}

.tab-text {
  font-size: 24rpx;
  color: #7A7E83;
  white-space: nowrap;
}

.tab-item.active .tab-text {
  color: #FFFFFF;
  font-weight: 600;
}

.ranking-list {
  background: #FFFFFF;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.ranking-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #F0F0F0;
}

.ranking-item:last-child {
  border-bottom: none;
}

.rank-info {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.rank-number {
  width: 40rpx;
  height: 40rpx;
  background: linear-gradient(135deg, #1A3366 0%, #2B4A7A 100%);
  color: #FFFFFF;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 600;
}

.member-name {
  font-size: 28rpx;
  color: #1A3366;
  font-weight: 500;
}

.rank-value {
  display: flex;
  align-items: baseline;
  gap: 5rpx;
}

.value-text {
  font-size: 32rpx;
  font-weight: 700;
  color: #1A3366;
}

.value-unit {
  font-size: 24rpx;
  color: #7A7E83;
}

/* 操作按钮样式 */
.action-buttons {
  display: flex;
  gap: 20rpx;
  margin-top: 40rpx;
}

.action-btn {
  flex: 1;
  padding: 30rpx;
  border-radius: 16rpx;
  text-align: center;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.action-btn.primary {
  background: linear-gradient(135deg, #1A3366 0%, #2B4A7A 100%);
}

.action-btn.secondary {
  background: #FFFFFF;
  border: 2rpx solid #1A3366;
}

.action-btn.primary .btn-text {
  color: #FFFFFF;
  font-size: 28rpx;
  font-weight: 600;
}

.action-btn.secondary .btn-text {
  color: #1A3366;
  font-size: 28rpx;
  font-weight: 600;
}

/* 详细视图样式 */
.detail-view {
  min-height: calc(100vh - 132rpx);
}

.detail-container {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 40rpx;
  box-shadow: 0 8rpx 24rpx rgba(26, 51, 102, 0.15);
}

.detail-metrics {
  display: flex;
  flex-direction: column;
  gap: 30rpx;
}

.detail-metric-item {
  background: #F8F9FA;
  border-radius: 16rpx;
  padding: 30rpx;
  border-left: 6rpx solid #1A3366;
}

.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #E8E8E8;
}

.metric-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #1A3366;
}

.metric-main-value {
  font-size: 36rpx;
  font-weight: 700;
  color: #1A3366;
}

.metric-details {
  display: flex;
  flex-direction: column;
  gap: 15rpx;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15rpx 0;
}

.detail-label {
  font-size: 26rpx;
  color: #7A7E83;
}

.detail-value {
  font-size: 26rpx;
  font-weight: 600;
  color: #1A3366;
}
</style>