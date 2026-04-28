<template>
  <view class="favorites-page">
    <!-- 自定义导航栏 -->
    <view class="custom-navbar">
      <view class="navbar-back" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="navbar-title">我的收藏</view>
      <view class="navbar-search" @click="searchFavorites">
        <text class="search-icon">🔍</text>
      </view>
    </view>

    <!-- 分类标签 -->
    <view class="category-tabs">
      <view 
        class="category-tab" 
        :class="{ active: activeCategory === category.key }"
        v-for="category in categories" 
        :key="category.key"
        @click="switchCategory(category.key)"
      >
        {{ category.label }}
      </view>
    </view>

    <!-- 收藏列表 -->
    <view class="favorites-list">
      <view 
        class="favorite-item" 
        v-for="item in filteredFavorites" 
        :key="item.id"
        @click="openFavorite(item)"
      >
        <view class="favorite-icon">{{ getCategoryIcon(item.category) }}</view>
        <view class="favorite-content">
          <view class="favorite-title">{{ item.title }}</view>
          <view class="favorite-description">{{ item.description }}</view>
          <view class="favorite-meta">
            <text class="favorite-time">{{ item.addTime }}</text>
            <text class="favorite-category">{{ getCategoryLabel(item.category) }}</text>
          </view>
        </view>
        <view class="favorite-actions">
          <view class="action-btn" @click.stop="removeFavorite(item)">
            <text class="remove-icon">❌</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-if="filteredFavorites.length === 0">
      <view class="empty-icon">⭐</view>
      <view class="empty-text">暂无{{ getCategoryLabel(activeCategory) }}收藏</view>
      <view class="empty-tip">在相关页面点击收藏按钮添加收藏</view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'FavoritesPage',
  data() {
    return {
      activeCategory: 'all',
      categories: [
        { key: 'all', label: '全部' },
        { key: 'monitoring', label: '监测点' },
        { key: 'document', label: '文档' },
        { key: 'process', label: '流程' }
      ],
      favorites: [
        {
          id: 1,
          category: 'monitoring',
          title: '管道A段监测点MP001',
          description: '华北区域主要监测点，压力传感器正常',
          addTime: '2024-01-15',
          location: '华北区域-管道A段',
          status: 'normal'
        },
        {
          id: 2,
          category: 'document',
          title: '管道安全操作手册',
          description: '最新版本的管道安全操作规范文档',
          addTime: '2024-01-14',
          fileType: 'PDF',
          size: '2.5MB'
        },
        {
          id: 3,
          category: 'process',
          title: '应急处理流程',
          description: '管道泄漏应急处理标准操作流程',
          addTime: '2024-01-13',
          steps: 8
        },
        {
          id: 4,
          category: 'monitoring',
          title: '阀门B12监测点',
          description: '关键阀门监测点，温度压力双重监控',
          addTime: '2024-01-12',
          location: '华北区域-阀门B12',
          status: 'warning'
        },
        {
          id: 5,
          category: 'document',
          title: '巡检记录表模板',
          description: '标准化巡检记录表格模板',
          addTime: '2024-01-11',
          fileType: 'Excel',
          size: '156KB'
        }
      ]
    }
  },
  computed: {
    filteredFavorites() {
      if (this.activeCategory === 'all') {
        return this.favorites;
      }
      return this.favorites.filter(item => item.category === this.activeCategory);
    }
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    switchCategory(categoryKey) {
      this.activeCategory = categoryKey;
    },
    getCategoryLabel(categoryKey) {
      const category = this.categories.find(c => c.key === categoryKey);
      return category ? category.label : '';
    },
    getCategoryIcon(category) {
      const iconMap = {
        monitoring: '📊',
        document: '📄',
        process: '🔄'
      };
      return iconMap[category] || '⭐';
    },
    searchFavorites() {
      uni.showToast({
        title: '搜索功能开发中',
        icon: 'none'
      });
    },
    openFavorite(item) {
      let message = '';
      switch (item.category) {
        case 'monitoring':
          message = `打开监测点：${item.title}`;
          break;
        case 'document':
          message = `打开文档：${item.title}`;
          break;
        case 'process':
          message = `查看流程：${item.title}`;
          break;
        default:
          message = `打开收藏：${item.title}`;
      }
      
      uni.showToast({
        title: message,
        icon: 'none'
      });
    },
    removeFavorite(item) {
      uni.showModal({
        title: '取消收藏',
        content: `确定要取消收藏"${item.title}"吗？`,
        success: (res) => {
          if (res.confirm) {
            const index = this.favorites.findIndex(f => f.id === item.id);
            if (index > -1) {
              this.favorites.splice(index, 1);
              uni.showToast({
                title: '已取消收藏',
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
.favorites-page {
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

.navbar-back, .navbar-search {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-icon, .search-icon {
  font-size: 36rpx;
  color: white;
  font-weight: bold;
}

.navbar-title {
  color: white;
  font-size: 36rpx;
  font-weight: 600;
}

/* 分类标签 */
.category-tabs {
  display: flex;
  background: white;
  margin: 24rpx;
  border-radius: 16rpx;
  padding: 8rpx;
  box-shadow: 0 4rpx 20rpx rgba(26, 51, 102, 0.1);
}

.category-tab {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  font-size: 28rpx;
  color: #666;
  border-radius: 12rpx;
  transition: all 0.3s;
}

.category-tab.active {
  background: #1A3366;
  color: white;
  font-weight: 500;
}

/* 收藏列表 */
.favorites-list {
  padding: 0 24rpx;
}

.favorite-item {
  background: white;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  display: flex;
  align-items: flex-start;
  box-shadow: 0 4rpx 20rpx rgba(26, 51, 102, 0.1);
  transition: transform 0.3s;
}

.favorite-item:active {
  transform: scale(0.98);
}

.favorite-icon {
  font-size: 48rpx;
  margin-right: 24rpx;
  margin-top: 8rpx;
}

.favorite-content {
  flex: 1;
}

.favorite-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 12rpx;
  line-height: 1.4;
}

.favorite-description {
  font-size: 28rpx;
  color: #666;
  margin-bottom: 16rpx;
  line-height: 1.5;
}

.favorite-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.favorite-time {
  font-size: 24rpx;
  color: #999;
}

.favorite-category {
  font-size: 24rpx;
  color: #1A3366;
  background: rgba(26, 51, 102, 0.1);
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}

.favorite-actions {
  margin-left: 16rpx;
}

.action-btn {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx;
  transition: all 0.3s;
}

.action-btn:active {
  background: #F5F7FA;
  transform: scale(0.9);
}

.remove-icon {
  font-size: 24rpx;
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
  line-height: 1.5;
  padding: 0 60rpx;
}
</style>