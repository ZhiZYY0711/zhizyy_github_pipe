<template>
  <view class="settings-page">
    <!-- 自定义导航栏 -->
    <view class="custom-navbar">
      <view class="navbar-back" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="navbar-title">设置</view>
      <view class="navbar-placeholder"></view>
    </view>

    <!-- 账户与安全 -->
    <view class="section">
      <view class="section-title">账户与安全</view>
      <view class="menu-list">
        <view class="menu-item" @click="editProfile">
          <view class="menu-icon">👤</view>
          <view class="menu-text">个人信息维护</view>
          <view class="menu-arrow">></view>
        </view>
        <view class="menu-item" @click="changePassword">
          <view class="menu-icon">🔒</view>
          <view class="menu-text">修改密码</view>
          <view class="menu-arrow">></view>
        </view>
        <view class="menu-item" @click="viewPermissions">
          <view class="menu-icon">🛡️</view>
          <view class="menu-text">权限说明</view>
          <view class="menu-arrow">></view>
        </view>
        <view class="menu-item" @click="switchAccount">
          <view class="menu-icon">🔄</view>
          <view class="menu-text">切换账号</view>
          <view class="menu-arrow">></view>
        </view>
      </view>
    </view>

    <!-- 系统与设置 -->
    <view class="section">
      <view class="section-title">系统与设置</view>
      <view class="menu-list">
        <view class="menu-item" @click="notificationSettings">
          <view class="menu-icon">🔔</view>
          <view class="menu-text">消息通知设置</view>
          <view class="menu-arrow">></view>
        </view>
        <view class="menu-item" @click="offlineMapDownload">
          <view class="menu-icon">🗺️</view>
          <view class="menu-text">离线地图下载</view>
          <view class="menu-arrow">></view>
        </view>
        <view class="menu-item" @click="dataSyncSettings">
          <view class="menu-icon">☁️</view>
          <view class="menu-text">数据同步设置</view>
          <view class="menu-arrow">></view>
        </view>
        <view class="menu-item" @click="clearCache">
          <view class="menu-icon">🗑️</view>
          <view class="menu-text">清除缓存</view>
          <view class="menu-value">{{ cacheSize }}</view>
          <view class="menu-arrow">></view>
        </view>
        <view class="menu-item" @click="languageSettings">
          <view class="menu-icon">🌐</view>
          <view class="menu-text">语言设置</view>
          <view class="menu-value">中文</view>
          <view class="menu-arrow">></view>
        </view>
      </view>
    </view>

    <!-- 支持与反馈 -->
    <view class="section">
      <view class="section-title">支持与反馈</view>
      <view class="menu-list">
        <view class="menu-item" @click="helpCenter">
          <view class="menu-icon">❓</view>
          <view class="menu-text">帮助中心/操作指南</view>
          <view class="menu-arrow">></view>
        </view>
        <view class="menu-item" @click="feedback">
          <view class="menu-icon">💬</view>
          <view class="menu-text">意见反馈</view>
          <view class="menu-arrow">></view>
        </view>
        <view class="menu-item" @click="contactSupport">
          <view class="menu-icon">📞</view>
          <view class="menu-text">联系客服/紧急联系人</view>
          <view class="menu-arrow">></view>
        </view>
        <view class="menu-item" @click="aboutUs">
          <view class="menu-icon">ℹ️</view>
          <view class="menu-text">关于我们</view>
          <view class="menu-value">v1.0.0</view>
          <view class="menu-arrow">></view>
        </view>
      </view>
    </view>

    <!-- 安全退出 -->
    <view class="section">
      <view class="logout-button" @click="logout">
        <text class="logout-text">退出登录</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'SettingsPage',
  data() {
    return {
      cacheSize: '125MB'
    }
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    editProfile() {
      uni.showToast({
        title: '个人信息维护功能开发中',
        icon: 'none'
      });
    },
    changePassword() {
      uni.showToast({
        title: '修改密码功能开发中',
        icon: 'none'
      });
    },
    viewPermissions() {
      uni.showModal({
        title: '权限说明',
        content: '当前账户权限：\n• 查看管道监测数据\n• 提交巡检报告\n• 接收报警通知\n• 查看任务分配',
        showCancel: false
      });
    },
    switchAccount() {
      uni.showModal({
        title: '切换账号',
        content: '确定要切换到其他账号吗？',
        success: (res) => {
          if (res.confirm) {
            uni.reLaunch({
              url: '/pages/login/login'
            });
          }
        }
      });
    },
    notificationSettings() {
      uni.showToast({
        title: '消息通知设置功能开发中',
        icon: 'none'
      });
    },
    offlineMapDownload() {
      uni.showToast({
        title: '离线地图下载功能开发中',
        icon: 'none'
      });
    },
    dataSyncSettings() {
      uni.showToast({
        title: '数据同步设置功能开发中',
        icon: 'none'
      });
    },
    clearCache() {
      uni.showModal({
        title: '清除缓存',
        content: `当前缓存大小：${this.cacheSize}\n确定要清除所有缓存吗？`,
        success: (res) => {
          if (res.confirm) {
            uni.showLoading({
              title: '清除中...'
            });
            setTimeout(() => {
              uni.hideLoading();
              this.cacheSize = '0MB';
              uni.showToast({
                title: '缓存清除成功',
                icon: 'success'
              });
            }, 2000);
          }
        }
      });
    },
    languageSettings() {
      uni.showActionSheet({
        itemList: ['中文', 'English'],
        success: (res) => {
          if (res.tapIndex === 0) {
            uni.showToast({
              title: '已切换到中文',
              icon: 'success'
            });
          } else {
            uni.showToast({
              title: 'English is not supported yet',
              icon: 'none'
            });
          }
        }
      });
    },
    helpCenter() {
      uni.showToast({
        title: '帮助中心功能开发中',
        icon: 'none'
      });
    },
    feedback() {
      uni.showToast({
        title: '意见反馈功能开发中',
        icon: 'none'
      });
    },
    contactSupport() {
      uni.showActionSheet({
        itemList: ['拨打客服电话', '在线客服', '紧急联系人'],
        success: (res) => {
          const actions = ['400-123-4567', '在线客服功能开发中', '紧急联系：110'];
          if (res.tapIndex === 0) {
            uni.makePhoneCall({
              phoneNumber: '400-123-4567'
            });
          } else {
            uni.showToast({
              title: actions[res.tapIndex],
              icon: 'none'
            });
          }
        }
      });
    },
    aboutUs() {
      uni.showModal({
        title: '关于我们',
        content: '油气管道监测系统\n版本：v1.0.0\n开发商：管道科技有限公司\n技术支持：400-123-4567',
        showCancel: false
      });
    },
    logout() {
      uni.showModal({
        title: '退出登录',
        content: '确定要退出当前账号吗？',
        success: (res) => {
          if (res.confirm) {
            uni.reLaunch({
              url: '/pages/login/login'
            });
          }
        }
      });
    }
  }
}
</script>

<style scoped>
.settings-page {
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

.navbar-back {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-icon {
  font-size: 36rpx;
  color: white;
  font-weight: bold;
}

.navbar-title {
  color: white;
  font-size: 36rpx;
  font-weight: 600;
}

.navbar-placeholder {
  width: 60rpx;
}

/* 设置区块 */
.section {
  margin-bottom: 32rpx;
}

.section-title {
  font-size: 28rpx;
  color: #999;
  padding: 24rpx 32rpx 16rpx;
  font-weight: 500;
}

.menu-list {
  background: white;
  border-radius: 24rpx;
  margin: 0 24rpx;
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

.menu-value {
  font-size: 28rpx;
  color: #999;
  margin-right: 16rpx;
}

.menu-arrow {
  font-size: 32rpx;
  color: #999;
}

/* 退出登录按钮 */
.logout-button {
  background: white;
  margin: 0 24rpx;
  border-radius: 24rpx;
  padding: 32rpx;
  text-align: center;
  box-shadow: 0 4rpx 20rpx rgba(26, 51, 102, 0.1);
}

.logout-text {
  font-size: 32rpx;
  color: #ff4757;
  font-weight: 500;
}
</style>