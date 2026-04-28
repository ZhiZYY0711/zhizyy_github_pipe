<template>
  <view class="task-container">
    <!-- 自定义导航栏 -->
    <view class="custom-navbar">
      <text class="navbar-title">任务管理</text>
    </view>
    
    <!-- 页面内容 -->
    <view class="content">
      <!-- KPI状态卡片 -->
      <view class="kpi-section">
        <view class="kpi-container">
          <view class="kpi-header">
            <text class="kpi-title">{{ currentUser.name }} 的任务概览</text>
            <text class="kpi-subtitle">今日工作状态</text>
          </view>
          
          <view class="kpi-stats">
            <view class="kpi-item published">
              <text class="kpi-number">{{ taskStats.published }}</text>
              <text class="kpi-label">已发布</text>
            </view>
            
            <view class="kpi-item accepted">
              <text class="kpi-number">{{ taskStats.accepted }}</text>
              <text class="kpi-label">已接取</text>
            </view>
            
            <view class="kpi-item waiting">
              <text class="kpi-number">{{ taskStats.waiting }}</text>
              <text class="kpi-label">等待接取</text>
            </view>
            
            <view class="kpi-item completed">
              <text class="kpi-number">{{ taskStats.completed }}</text>
              <text class="kpi-label">已完成</text>
            </view>
            
            <view class="kpi-item overdue">
              <text class="kpi-number">{{ taskStats.overdue }}</text>
              <text class="kpi-label">已超期</text>
            </view>
          </view>
        </view>
      </view>
      
      <!-- 任务列表容器 -->
      <view class="task-list-container">
        <!-- 查询条件 -->
        <view class="search-header">
          <view class="search-row">
            <picker mode="selector" :value="filterStatus" :range="statusOptions" @change="onStatusChange">
              <view class="filter-item">
                <text class="filter-label">状态：{{ statusOptions[filterStatus] }}</text>
                <text class="filter-arrow">▼</text>
              </view>
            </picker>
            <picker mode="selector" :value="filterType" :range="typeOptions" @change="onTypeChange">
              <view class="filter-item">
                <text class="filter-label">类型：{{ typeOptions[filterType] }}</text>
                <text class="filter-arrow">▼</text>
              </view>
            </picker>
          </view>
          <view class="search-input-row">
            <input class="search-input" placeholder="搜索任务名称..." v-model="searchKeyword" @input="onSearch" />
            <view class="search-btn" @click="resetFilters">
              <text class="search-btn-text">重置</text>
            </view>
          </view>
        </view>
        
        <!-- 任务列表 -->
        <view class="task-list">
          <view class="list-header">
            <text class="list-title">任务列表 ({{ filteredTaskList.length }})</text>
          </view>
          <scroll-view scroll-y="true" class="task-scroll">
            <view class="task-item" v-for="(task, index) in filteredTaskList" :key="task.id" @click="goToTaskDetail(task)">
              <view class="task-card">
                <view class="task-header">
                  <view class="task-title-row">
                    <text class="task-name">{{ task.name }}</text>
                    <view class="task-status" :class="task.status">
                      <text class="status-text">{{ getStatusText(task.status) }}</text>
                    </view>
                  </view>
                  <text class="task-type">{{ task.type }}</text>
                </view>
                <view class="task-body">
                  <view class="task-time-info">
                    <view class="time-item">
                      <text class="time-label">开始时间：</text>
                      <text class="time-value">{{ formatTime(task.startTime) }}</text>
                    </view>
                    <view class="time-item">
                      <text class="time-label">结束时间：</text>
                      <text class="time-value">{{ formatTime(task.endTime) }}</text>
                    </view>
                  </view>
                  <view class="task-location" v-if="task.location">
                    <text class="location-label">📍 {{ task.location }}</text>
                  </view>
                </view>
                <view class="task-footer">
                  <view class="task-priority" :class="task.priority">
                    <text class="priority-text">{{ getPriorityText(task.priority) }}</text>
                  </view>
                  <view class="task-action">
                    <text class="action-text">查看详情 ></text>
                  </view>
                </view>
              </view>
            </view>
            <view class="list-footer" v-if="filteredTaskList.length === 0">
              <text class="empty-text">暂无任务数据</text>
            </view>
          </scroll-view>
        </view>
      </view>
    </view>
    
    <!-- 右下角浮动按钮 -->
    <view class="float-buttons">
      <view class="float-btn back-to-top" @click="backToTop">
        <text class="float-btn-text">↑</text>
      </view>
      <view class="float-btn feedback" @click="showFeedback">
        <text class="float-btn-text">💬</text>
      </view>
    </view>
    
    <!-- 反馈弹窗 -->
    <view class="feedback-modal" v-if="showFeedbackModal" @click="hideFeedback">
      <view class="feedback-content" @click.stop>
        <view class="feedback-header">
          <text class="feedback-title">意见反馈</text>
          <text class="feedback-close" @click="hideFeedback">×</text>
        </view>
        <textarea class="feedback-textarea" placeholder="请输入您的意见或建议..." v-model="feedbackText"></textarea>
        <view class="feedback-actions">
          <view class="feedback-btn cancel" @click="hideFeedback">
            <text class="btn-text">取消</text>
          </view>
          <view class="feedback-btn submit" @click="submitFeedback">
            <text class="btn-text">提交</text>
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
      // 当前用户信息
      currentUser: {
        name: '张三',
        id: 'U001'
      },
      
      // KPI统计数据
      taskStats: {
        published: 15,
        accepted: 12,
        waiting: 3,
        completed: 28,
        overdue: 2
      },
      
      // 筛选条件
      filterStatus: 0,
      filterType: 0,
      searchKeyword: '',
      
      // 筛选选项
      statusOptions: ['全部状态', '已发布', '已接取', '等待接取', '已完成', '已超期'],
      typeOptions: ['全部类型', '管道巡检', '设备维护', '数据采集', '安全检查', '应急处理'],
      
      // 反馈相关
      showFeedbackModal: false,
      feedbackText: '',
      
      // 任务列表数据
      taskList: [
        {
          id: 'T001',
          name: '西部管道A段巡检任务',
          type: '管道巡检',
          status: 'published',
          startTime: '2024-01-15 09:00:00',
          endTime: '2024-01-15 17:00:00',
          location: '西部管道A段（桩号K10+500-K15+200）',
          priority: 'high',
          description: '对西部管道A段进行例行巡检，重点检查管道完整性、防腐层状况及周边环境安全'
        },
        {
          id: 'T002',
          name: '压力监测设备校准',
          type: '设备维护',
          status: 'accepted',
          startTime: '2024-01-15 14:00:00',
          endTime: '2024-01-15 18:00:00',
          location: '中央控制站',
          priority: 'medium',
          description: '对压力传感器进行定期校准和维护，确保监测数据准确性'
        },
        {
          id: 'T003',
          name: '东部管道数据采集',
          type: '数据采集',
          status: 'waiting',
          startTime: '2024-01-16 08:00:00',
          endTime: '2024-01-16 12:00:00',
          location: '东部管道B段',
          priority: 'medium',
          description: '采集东部管道B段的流量、压力、温度等运行数据'
        },
        {
          id: 'T004',
          name: '安全阀检查维护',
          type: '安全检查',
          status: 'completed',
          startTime: '2024-01-14 10:00:00',
          endTime: '2024-01-14 16:00:00',
          location: '南部分输站',
          priority: 'high',
          description: '对南部分输站安全阀进行检查和维护，确保安全运行'
        },
        {
          id: 'T005',
          name: '管道泄漏应急演练',
          type: '应急处理',
          status: 'overdue',
          startTime: '2024-01-13 09:00:00',
          endTime: '2024-01-13 15:00:00',
          location: '北部管道C段',
          priority: 'high',
          description: '进行管道泄漏应急处理演练，提高应急响应能力'
        },
        {
          id: 'T006',
          name: '阴极保护系统检测',
          type: '设备维护',
          status: 'published',
          startTime: '2024-01-17 09:00:00',
          endTime: '2024-01-17 17:00:00',
          location: '全线阴极保护站',
          priority: 'medium',
          description: '检测阴极保护系统运行状态，确保管道防腐效果'
        }
      ]
    }
  },
  
  computed: {
    // 过滤后的任务列表
    filteredTaskList() {
      let filtered = this.taskList;
      
      // 按状态筛选
      if (this.filterStatus > 0) {
        const statusMap = {
          1: 'published',
          2: 'accepted', 
          3: 'waiting',
          4: 'completed',
          5: 'overdue'
        };
        filtered = filtered.filter(task => task.status === statusMap[this.filterStatus]);
      }
      
      // 按类型筛选
      if (this.filterType > 0) {
        const selectedType = this.typeOptions[this.filterType];
        filtered = filtered.filter(task => task.type === selectedType);
      }
      
      // 按关键词搜索
      if (this.searchKeyword.trim()) {
        const keyword = this.searchKeyword.trim().toLowerCase();
        filtered = filtered.filter(task => 
          task.name.toLowerCase().includes(keyword) ||
          task.description.toLowerCase().includes(keyword)
        );
      }
      
      return filtered;
    }
  },
  
  methods: {
    // 状态筛选变化
    onStatusChange(e) {
      this.filterStatus = e.detail.value;
    },
    
    // 类型筛选变化
    onTypeChange(e) {
      this.filterType = e.detail.value;
    },
    
    // 搜索输入
    onSearch() {
      // 实时搜索，computed会自动更新
    },
    
    // 重置筛选条件
    resetFilters() {
      this.filterStatus = 0;
      this.filterType = 0;
      this.searchKeyword = '';
    },
    
    // 获取状态文本
    getStatusText(status) {
      const statusMap = {
        'published': '已发布',
        'accepted': '已接取',
        'waiting': '等待接取',
        'completed': '已完成',
        'overdue': '已超期'
      };
      return statusMap[status] || '未知';
    },
    
    // 获取优先级文本
    getPriorityText(priority) {
      const priorityMap = {
        'high': '高优先级',
        'medium': '中优先级',
        'low': '低优先级'
      };
      return priorityMap[priority] || '普通';
    },
    
    // 格式化时间
    formatTime(timeStr) {
      if (!timeStr) return '';
      const date = new Date(timeStr);
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      const hours = date.getHours().toString().padStart(2, '0');
      const minutes = date.getMinutes().toString().padStart(2, '0');
      return `${month}-${day} ${hours}:${minutes}`;
    },
    
    // 跳转到任务详情
    goToTaskDetail(task) {
      console.log('查看任务详情:', task);
      uni.navigateTo({
        url: `/pages/task/detail?id=${task.id}`
      });
    },
    
    // 回到顶部
    backToTop() {
      uni.pageScrollTo({
        scrollTop: 0,
        duration: 300
      });
    },
    
    // 显示反馈弹窗
    showFeedback() {
      this.showFeedbackModal = true;
    },
    
    // 隐藏反馈弹窗
    hideFeedback() {
      this.showFeedbackModal = false;
      this.feedbackText = '';
    },
    
    // 提交反馈
    submitFeedback() {
      if (!this.feedbackText.trim()) {
        uni.showToast({
          title: '请输入反馈内容',
          icon: 'none'
        });
        return;
      }
      
      // 这里可以调用API提交反馈
      console.log('提交反馈:', this.feedbackText);
      
      uni.showToast({
        title: '反馈提交成功',
        icon: 'success'
      });
      
      this.hideFeedback();
    },
    
    // 加载任务数据
    loadTaskData() {
      // 这里可以调用API加载真实数据
      // 更新taskStats和taskList
    }
  },
  
  onLoad() {
    this.loadTaskData();
  },
  
  onShow() {
    // 页面显示时刷新数据
    this.loadTaskData();
  }
}
</script>

<style scoped>
.task-container {
  height: 100vh;
  background: linear-gradient(180deg, #F5F8FF 0%, #E8F2FF 100%);
  display: flex;
  flex-direction: column;
}

.custom-navbar {
  height: 88rpx;
  background: linear-gradient(135deg, #1A3366 0%, #2B4A7A 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: 44rpx;
  box-shadow: 0 4rpx 20rpx rgba(26, 51, 102, 0.2);
  position: relative;
  z-index: 100;
}

.navbar-title {
  color: #FFFFFF;
  font-size: 36rpx;
  font-weight: 600;
  font-family: PingFang SC, Microsoft YaHei, sans-serif;
  letter-spacing: 1rpx;
}

.content {
    flex: 1;
    padding: 20rpx 24rpx 0;
  }

/* KPI状态卡片样式 */
.kpi-section {
  margin-bottom: 20rpx;
}

.kpi-container {
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(26, 51, 102, 0.08);
}

.kpi-header {
  margin-bottom: 20rpx;
  text-align: center;
}

.kpi-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A3366;
  display: block;
  margin-bottom: 6rpx;
}

.kpi-subtitle {
  font-size: 22rpx;
  color: #666;
  display: block;
}

.kpi-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kpi-item {
  flex: 1;
  text-align: center;
  padding: 16rpx 8rpx;
  border-radius: 12rpx;
  margin: 0 4rpx;
  transition: all 0.3s ease;
}

.kpi-item.published {
  background: linear-gradient(135deg, #E6F7FF 0%, #F0F7FF 100%);
}

.kpi-item.accepted {
  background: linear-gradient(135deg, #F6FFED 0%, #F0FFF0 100%);
}

.kpi-item.waiting {
  background: linear-gradient(135deg, #FFFBE6 0%, #FFFFF0 100%);
}

.kpi-item.completed {
  background: linear-gradient(135deg, #E6FFFB 0%, #F0FFFF 100%);
}

.kpi-item.overdue {
  background: linear-gradient(135deg, #FFF1F0 0%, #FFF5F5 100%);
}

.kpi-number {
  font-size: 32rpx;
  font-weight: 700;
  color: #1A3366;
  display: block;
  margin-bottom: 6rpx;
}

.kpi-label {
  font-size: 20rpx;
  color: #666;
  display: block;
}

/* 任务列表容器样式 */
.task-list-container {
  background: #FFFFFF;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 6rpx 24rpx rgba(26, 51, 102, 0.1);
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 搜索头部样式 */
.search-header {
  padding: 24rpx;
  background: linear-gradient(135deg, #F8FAFF 0%, #EDF4FF 100%);
  border-bottom: 2rpx solid #E8F2FF;
}

.search-row {
  display: flex;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.filter-item {
  flex: 1;
  background: #FFFFFF;
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 2rpx solid #E8F2FF;
  transition: all 0.3s ease;
}

.filter-item:active {
  border-color: #4A90E2;
  background: #F0F7FF;
}

.filter-label {
  font-size: 26rpx;
  color: #333;
}

.filter-arrow {
  font-size: 20rpx;
  color: #999;
}

.search-input-row {
  display: flex;
  gap: 16rpx;
  align-items: center;
}

.search-input {
  flex: 1;
  background: #FFFFFF;
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  font-size: 26rpx;
  border: 2rpx solid #E8F2FF;
  transition: all 0.3s ease;
}

.search-input:focus {
  border-color: #4A90E2;
  background: #F0F7FF;
}

.search-btn {
  background: linear-gradient(135deg, #4A90E2 0%, #357ABD 100%);
  border-radius: 12rpx;
  padding: 16rpx 24rpx;
  transition: all 0.3s ease;
}

.search-btn:active {
  transform: scale(0.95);
}

.search-btn-text {
  color: #FFFFFF;
  font-size: 26rpx;
  font-weight: 500;
}

/* 任务列表样式 */
.task-list {
  background: #FFFFFF;
}

.list-header {
  padding: 24rpx;
  border-bottom: 2rpx solid #F0F0F0;
}

.list-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A3366;
}

.task-scroll {
  max-height: 800rpx;
}

.task-item {
  padding: 20rpx 24rpx;
  border-bottom: 2rpx solid #F8FAFF;
  transition: all 0.3s ease;
}

.task-item:active {
  background: #F8FAFF;
}

.task-item:last-child {
  border-bottom: none;
}

.task-card {
  width: 100%;
}

.task-header {
  margin-bottom: 16rpx;
}

.task-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8rpx;
}

.task-name {
  flex: 1;
  font-size: 28rpx;
  font-weight: 600;
  color: #1A3366;
  line-height: 1.4;
  margin-right: 16rpx;
}

.task-status {
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  font-weight: 500;
  white-space: nowrap;
}

.task-status.published {
  background: #E6F7FF;
  color: #1890FF;
  border: 1rpx solid #91D5FF;
}

.task-status.accepted {
  background: #F6FFED;
  color: #52C41A;
  border: 1rpx solid #B7EB8F;
}

.task-status.waiting {
  background: #FFFBE6;
  color: #FAAD14;
  border: 1rpx solid #FFE58F;
}

.task-status.completed {
  background: #E6FFFB;
  color: #13C2C2;
  border: 1rpx solid #87E8DE;
}

.task-status.overdue {
  background: #FFF1F0;
  color: #FF4D4F;
  border: 1rpx solid #FFCCC7;
}

.status-text {
  font-size: 22rpx;
}

.task-type {
  font-size: 24rpx;
  color: #666;
  display: block;
}

.task-body {
  margin-bottom: 16rpx;
}

.task-time-info {
  margin-bottom: 12rpx;
}

.time-item {
  display: flex;
  align-items: center;
  margin-bottom: 6rpx;
}

.time-label {
  font-size: 24rpx;
  color: #999;
  width: 140rpx;
}

.time-value {
  font-size: 24rpx;
  color: #333;
}

.task-location {
  margin-bottom: 8rpx;
}

.location-label {
  font-size: 24rpx;
  color: #666;
}

.task-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-priority {
  padding: 4rpx 12rpx;
  border-radius: 12rpx;
  font-size: 20rpx;
}

.task-priority.high {
  background: #FFF1F0;
  color: #FF4D4F;
}

.task-priority.medium {
  background: #FFFBE6;
  color: #FAAD14;
}

.task-priority.low {
  background: #F6FFED;
  color: #52C41A;
}

.priority-text {
  font-size: 20rpx;
}

.task-action {
  padding: 8rpx 16rpx;
  background: linear-gradient(135deg, #4A90E2 0%, #357ABD 100%);
  border-radius: 20rpx;
}

.action-text {
  color: #FFFFFF;
  font-size: 22rpx;
  font-weight: 500;
}

.list-footer {
  padding: 60rpx 24rpx;
  text-align: center;
}

.empty-text {
  font-size: 26rpx;
  color: #999;
}

/* 浮动按钮样式 */
.float-buttons {
  position: fixed;
  right: 30rpx;
  bottom: 120rpx;
  z-index: 999;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.float-btn {
  width: 96rpx;
  height: 96rpx;
  border-radius: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 20rpx rgba(26, 51, 102, 0.2);
  transition: all 0.3s ease;
}

.float-btn:active {
  transform: scale(0.9);
}

.float-btn.back-to-top {
  background: linear-gradient(135deg, #4A90E2 0%, #357ABD 100%);
}

.float-btn.feedback {
  background: linear-gradient(135deg, #52C41A 0%, #389E0D 100%);
}

.float-btn-text {
  color: #FFFFFF;
  font-size: 32rpx;
  font-weight: 600;
}

/* 反馈弹窗样式 */
.feedback-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 40rpx;
}

.feedback-content {
  background: #FFFFFF;
  border-radius: 20rpx;
  width: 100%;
  max-width: 600rpx;
  overflow: hidden;
  box-shadow: 0 10rpx 40rpx rgba(0, 0, 0, 0.2);
}

.feedback-header {
  padding: 32rpx 32rpx 24rpx;
  border-bottom: 2rpx solid #F0F0F0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.feedback-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1A3366;
}

.feedback-close {
  font-size: 40rpx;
  color: #999;
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 30rpx;
  transition: all 0.3s ease;
}

.feedback-close:active {
  background: #F0F0F0;
}

.feedback-textarea {
  width: 100%;
  min-height: 200rpx;
  padding: 32rpx;
  font-size: 28rpx;
  line-height: 1.5;
  border: none;
  resize: none;
  background: #FAFAFA;
}

.feedback-actions {
  padding: 24rpx 32rpx 32rpx;
  display: flex;
  gap: 20rpx;
}

.feedback-btn {
  flex: 1;
  padding: 24rpx;
  border-radius: 12rpx;
  text-align: center;
  transition: all 0.3s ease;
}

.feedback-btn:active {
  transform: scale(0.95);
}

.feedback-btn.cancel {
  background: #F5F5F5;
  border: 2rpx solid #D9D9D9;
}

.feedback-btn.submit {
  background: linear-gradient(135deg, #4A90E2 0%, #357ABD 100%);
}

.feedback-btn.cancel .btn-text {
  color: #666;
}

.feedback-btn.submit .btn-text {
  color: #FFFFFF;
}

.btn-text {
  font-size: 28rpx;
  font-weight: 500;
}

/* 响应式设计 */
@media screen and (max-width: 750rpx) {
  .kpi-cards {
    flex-wrap: wrap;
  }
  
  .kpi-card {
    flex: 0 0 calc(50% - 8rpx);
    margin-bottom: 16rpx;
  }
  
  .search-row {
    flex-direction: column;
    gap: 12rpx;
  }
  
  .filter-item {
    width: 100%;
  }
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