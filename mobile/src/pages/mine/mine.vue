<template>
  <view class="page">
    <view class="header">我的</view>
    <view class="content">这里是“我的”页，占位内容。</view>
  </view>
  
</template>

<script>
export default {
  name: 'MinePage',
  data() {
    return {
      // 在此添加个人信息相关数据
    }
  }
}
</script>

<style scoped>
.page {
  padding: 24rpx;
}
.header {
  font-size: 32rpx;
  font-weight: 600;
  margin-bottom: 24rpx;
}
.content {
  color: #666;
}
</style>