<template>
  <view class="reports-page">
    <!-- 自定义导航栏 -->
    <view class="custom-navbar">
      <view class="navbar-back" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="navbar-title">我的报告</view>
      <view class="navbar-add" @click="addReport">
        <text class="add-icon">+</text>
      </view>
    </view>

    <!-- 筛选标签 -->
    <view class="filter-tabs">
      <view 
        class="filter-tab" 
        :class="{ active: activeTab === tab.key }"
        v-for="tab in filterTabs" 
        :key="tab.key"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </view>
    </view>

    <!-- 报告列表 -->
    <view class="reports-list">
      <view 
        class="report-item" 
        v-for="report in filteredReports" 
        :key="report.id"
        @click="viewReport(report)"
      >
        <view class="report-header">
          <view class="report-type" :class="getTypeClass(report.type)">
            {{ getTypeLabel(report.type) }}
          </view>
          <view class="report-status" :class="getStatusClass(report.status)">
            {{ getStatusLabel(report.status) }}
          </view>
        </view>
        <view class="report-title">{{ report.title }}</view>
        <view class="report-info">
          <view class="report-location">📍 {{ report.location }}</view>
          <view class="report-time">🕐 {{ report.createTime }}</view>
        </view>
        <view class="report-actions">
          <view class="action-btn edit" @click.stop="editReport(report)">编辑</view>
          <view class="action-btn delete" @click.stop="deleteReport(report)">删除</view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-if="filteredReports.length === 0">
      <view class="empty-icon">📋</view>
      <view class="empty-text">暂无{{ getTabLabel(activeTab) }}报告</view>
      <view class="empty-tip">点击右上角"+"添加新报告</view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'ReportsPage',
  data() {
    return {
      activeTab: 'all',
      filterTabs: [
        { key: 'all', label: '全部' },
        { key: 'inspection', label: '巡检' },
        { key: 'maintenance', label: '维修' },
        { key: 'emergency', label: '应急' }
      ],
      reports: [
        {
          id: 1,
          type: 'inspection',
          title: '管道A段日常巡检报告',
          location: '华北区域-管道A段',
          createTime: '2024-01-15 14:30',
          status: 'submitted'
        },
        {
          id: 2,
          type: 'maintenance',
          title: '阀门B12维修记录',
          location: '华北区域-阀门B12',
          createTime: '2024-01-14 09:15',
          status: 'approved'
        },
        {
          id: 3,
          type: 'inspection',
          title: '管道C段安全检查',
          location: '华北区域-管道C段',
          createTime: '2024-01-13 16:45',
          status: 'draft'
        },
        {
          id: 4,
          type: 'emergency',
          title: '管道泄漏应急处理报告',
          location: '华北区域-管道D段',
          createTime: '2024-01-12 11:20',
          status: 'submitted'
        }
      ]
    }
  },
  computed: {
    filteredReports() {
      if (this.activeTab === 'all') {
        return this.reports;
      }
      return this.reports.filter(report => report.type === this.activeTab);
    }
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    switchTab(tabKey) {
      this.activeTab = tabKey;
    },
    getTabLabel(tabKey) {
      const tab = this.filterTabs.find(t => t.key === tabKey);
      return tab ? tab.label : '';
    },
    getTypeClass(type) {
      const classMap = {
        inspection: 'type-inspection',
        maintenance: 'type-maintenance',
        emergency: 'type-emergency'
      };
      return classMap[type] || '';
    },
    getTypeLabel(type) {
      const labelMap = {
        inspection: '巡检',
        maintenance: '维修',
        emergency: '应急'
      };
      return labelMap[type] || type;
    },
    getStatusClass(status) {
      const classMap = {
        draft: 'status-draft',
        submitted: 'status-submitted',
        approved: 'status-approved',
        rejected: 'status-rejected'
      };
      return classMap[status] || '';
    },
    getStatusLabel(status) {
      const labelMap = {
        draft: '草稿',
        submitted: '已提交',
        approved: '已通过',
        rejected: '已驳回'
      };
      return labelMap[status] || status;
    },
    addReport() {
      uni.showActionSheet({
        itemList: ['巡检报告', '维修报告', '应急报告'],
        success: (res) => {
          const types = ['inspection', 'maintenance', 'emergency'];
          const type = types[res.tapIndex];
          uni.showToast({
            title: `创建${this.getTypeLabel(type)}报告功能开发中`,
            icon: 'none'
          });
        }
      });
    },
    viewReport(report) {
      uni.showToast({
        title: '查看报告详情功能开发中',
        icon: 'none'
      });
    },
    editReport(report) {
      if (report.status === 'approved') {
        uni.showToast({
          title: '已通过的报告无法编辑',
          icon: 'none'
        });
        return;
      }
      uni.showToast({
        title: '编辑报告功能开发中',
        icon: 'none'
      });
    },
    deleteReport(report) {
      uni.showModal({
        title: '删除报告',
        content: `确定要删除"${report.title}"吗？`,
        success: (res) => {
          if (res.confirm) {
            const index = this.reports.findIndex(r => r.id === report.id);
            if (index > -1) {
              this.reports.splice(index, 1);
              uni.showToast({
                title: '删除成功',
                icon: 'success'
              });
            }
          }
        }
      });
    }
  }
}
</script>

<style scoped>
.reports-page {
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

.navbar-back, .navbar-add {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-icon, .add-icon {
  font-size: 36rpx;
  color: white;
  font-weight: bold;
}

.navbar-title {
  color: white;
  font-size: 36rpx;
  font-weight: 600;
}

/* 筛选标签 */
.filter-tabs {
  display: flex;
  background: white;
  margin: 24rpx;
  border-radius: 16rpx;
  padding: 8rpx;
  box-shadow: 0 4rpx 20rpx rgba(26, 51, 102, 0.1);
}

.filter-tab {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  font-size: 28rpx;
  color: #666;
  border-radius: 12rpx;
  transition: all 0.3s;
}

.filter-tab.active {
  background: #1A3366;
  color: white;
  font-weight: 500;
}

/* 报告列表 */
.reports-list {
  padding: 0 24rpx;
}

.report-item {
  background: white;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(26, 51, 102, 0.1);
  transition: transform 0.3s;
}

.report-item:active {
  transform: scale(0.98);
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.report-type {
  padding: 8rpx 16rpx;
  border-radius: 12rpx;
  font-size: 24rpx;
  font-weight: 500;
}

.type-inspection {
  background: rgba(26, 51, 102, 0.1);
  color: #1A3366;
}

.type-maintenance {
  background: rgba(255, 149, 0, 0.1);
  color: #ff9500;
}

.type-emergency {
  background: rgba(255, 71, 87, 0.1);
  color: #ff4757;
}

.report-status {
  padding: 8rpx 16rpx;
  border-radius: 12rpx;
  font-size: 24rpx;
  font-weight: 500;
}

.status-draft {
  background: rgba(153, 153, 153, 0.1);
  color: #999;
}

.status-submitted {
  background: rgba(26, 51, 102, 0.1);
  color: #1A3366;
}

.status-approved {
  background: rgba(76, 217, 100, 0.1);
  color: #4cd964;
}

.status-rejected {
  background: rgba(255, 71, 87, 0.1);
  color: #ff4757;
}

.report-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 16rpx;
}

.report-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.report-location, .report-time {
  font-size: 24rpx;
  color: #999;
}

.report-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
}

.action-btn {
  padding: 12rpx 24rpx;
  border-radius: 12rpx;
  font-size: 24rpx;
  font-weight: 500;
  transition: all 0.3s;
}

.action-btn.edit {
  background: rgba(26, 51, 102, 0.1);
  color: #1A3366;
}

.action-btn.delete {
  background: rgba(255, 71, 87, 0.1);
  color: #ff4757;
}

.action-btn:active {
  transform: scale(0.95);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 120rpx 0;
}

.empty-icon {
  font-size: 120rpx;
  margin-bottom: 32rpx;
  opacity: 0.3;
}

.empty-text {
  font-size: 32rpx;
  color: #666;
  margin-bottom: 16rpx;
}

.empty-tip {
  font-size: 24rpx;
  color: #999;
}
</style>