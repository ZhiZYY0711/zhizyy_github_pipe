<template>
  <div class="pipeline-details">
    <!-- 顶部导航栏 -->
    <div class="top-navigation">
      <div class="page-title">
        <h2>管道详情</h2>
        <p>管道监测数据详细信息</p>
      </div>
      <!-- 切换按钮 -->
      <div class="switch-button">
        <button class="switch-btn" @click="switchToPage('area')">
          <i class="switch-icon">🔄</i>
          <span>区域详情</span>
        </button>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="content-area">
      <div class="placeholder-content">
        <div class="placeholder-icon">🚧</div>
        <h3>管道详情页面</h3>
        <p>此页面内容正在开发中...</p>
        <p>请使用右上角的切换按钮返回区域详情页面</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PipelineDetails',
  methods: {
    // 切换页面
    switchToPage(pageType) {
      this.$emit('switch-page', pageType)
    }
  }
}
</script>

<style scoped>
.pipeline-details {
  padding: 20px;
  background-color: #f5f5f5;
  min-height: 100vh;
}

/* 顶部导航栏 */
.top-navigation {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title h2 {
  margin: 0;
  color: #1890ff;
  font-size: 24px;
  font-weight: 600;
}

.page-title p {
  margin: 5px 0 0 0;
  color: #666;
  font-size: 14px;
}

/* 切换按钮 */
.switch-button {
  margin-left: 20px;
}

.switch-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #52c41a, #73d13d);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
  font-weight: 500;
}

.switch-btn:hover {
  background: linear-gradient(135deg, #73d13d, #95de64);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.3);
}

.switch-icon {
  font-size: 16px;
}

/* 内容区域 */
.content-area {
  background: white;
  border-radius: 8px;
  padding: 40px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  min-height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-content {
  text-align: center;
  color: #666;
}

.placeholder-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.placeholder-content h3 {
  color: #1890ff;
  margin-bottom: 15px;
  font-size: 24px;
}

.placeholder-content p {
  margin: 10px 0;
  font-size: 16px;
  line-height: 1.6;
}
</style>