<template>
  <div class="data-visualization">
    <div class="visualization-header">
      <h1>数据可视化总览</h1>
      <p>实时监控管道系统运行状态</p>
    </div>
    
    <!-- 主要内容区域 - 为后续功能预留充足空间 -->
    <div class="visualization-content">
      <!-- KPI指标卡区域 -->
      <div class="kpi-section">
        <div class="section-title">系统概览</div>
        <div class="kpi-placeholder">
          <div class="placeholder-card">全局KPI指标卡区域</div>
          <div class="placeholder-card">检修员KPI指标卡区域</div>
        </div>
      </div>
      
      <!-- 中央地图区域 -->
      <div class="map-section">
        <div class="section-title">区域监控地图</div>
        <div class="map-placeholder">
          <div class="map-container">
            <p>大地图显示区域</p>
            <p>深蓝色背景，显示所有区域</p>
            <p>颜色深浅代表异常率或报警等级</p>
          </div>
        </div>
      </div>
      
      <!-- 右侧信息面板 -->
      <div class="info-panel">
        <div class="section-title">实时信息</div>
        <div class="panel-placeholder">
          <!-- 实时报警流水区域 -->
          <div class="alert-stream">
            <h3>实时报警流水</h3>
            <div class="stream-placeholder">
              滚动显示高危/危险级报警信息
            </div>
          </div>
          
          <!-- 报警级别分布区域 -->
          <div class="alert-distribution">
            <h3>报警级别分布</h3>
            <div class="chart-placeholder">
              饼图/环形图显示区域
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DataVisualization',
  data() {
    return {
      // 数据将在后续任务中添加
    }
  },
  mounted() {
    // 组件挂载后的初始化逻辑
    console.log('数据可视化模块已加载');
  }
}
</script>

<style scoped>
.data-visualization {
  height: 100%;
  padding: 20px;
  background-color: #f8fafc;
  overflow-y: auto;
}

.visualization-header {
  margin-bottom: 30px;
  text-align: center;
}

.visualization-header h1 {
  color: #1e3a8a;
  font-size: 28px;
  margin-bottom: 10px;
}

.visualization-header p {
  color: #64748b;
  font-size: 16px;
}

.visualization-content {
  display: grid;
  grid-template-columns: 1fr 2fr 1fr;
  grid-template-rows: auto 1fr;
  gap: 20px;
  height: calc(100vh - 200px);
  min-height: 600px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e3a8a;
  margin-bottom: 15px;
  padding-bottom: 8px;
  border-bottom: 2px solid #e2e8f0;
}

/* KPI指标卡区域 */
.kpi-section {
  grid-column: 1 / -1;
  grid-row: 1;
}

.kpi-placeholder {
  display: flex;
  gap: 20px;
  justify-content: space-around;
}

.placeholder-card {
  flex: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 30px;
  border-radius: 12px;
  text-align: center;
  font-weight: 600;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 地图区域 */
.map-section {
  grid-column: 1 / 3;
  grid-row: 2;
}

.map-placeholder {
  height: 100%;
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.map-container {
  height: 100%;
  background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  text-align: center;
}

.map-container p {
  margin: 10px 0;
  opacity: 0.9;
}

/* 信息面板 */
.info-panel {
  grid-column: 3;
  grid-row: 2;
  display: flex;
  flex-direction: column;
}

.panel-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.alert-stream,
.alert-distribution {
  background-color: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  flex: 1;
}

.alert-stream h3,
.alert-distribution h3 {
  color: #1e3a8a;
  font-size: 16px;
  margin-bottom: 15px;
  font-weight: 600;
}

.stream-placeholder,
.chart-placeholder {
  height: 100%;
  background-color: #f1f5f9;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-style: italic;
  min-height: 150px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .visualization-content {
    grid-template-columns: 1fr;
    grid-template-rows: auto auto auto;
  }
  
  .kpi-section {
    grid-column: 1;
    grid-row: 1;
  }
  
  .map-section {
    grid-column: 1;
    grid-row: 2;
  }
  
  .info-panel {
    grid-column: 1;
    grid-row: 3;
  }
  
  .panel-placeholder {
    flex-direction: row;
  }
}

@media (max-width: 768px) {
  .kpi-placeholder {
    flex-direction: column;
  }
  
  .panel-placeholder {
    flex-direction: column;
  }
}
</style>