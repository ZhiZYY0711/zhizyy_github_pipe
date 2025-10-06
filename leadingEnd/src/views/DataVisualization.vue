<template>
  <div class="data-visualization">
    <!-- 主要内容区域 - 紧凑布局 -->
    <div class="visualization-content">
      <!-- KPI指标卡区域 -->
      <div class="kpi-section">
        <div class="kpi-cards">
          <!-- 全局KPI指标卡 -->
          <div class="kpi-card global-kpi">
            <div class="kpi-header">
              <h3>全局KPI指标</h3>
              <i class="icon-global">🌐</i>
            </div>
            <div class="kpi-metrics">
              <div class="metric-item">
                <div class="metric-label">传感器总数</div>
                <div class="metric-value">{{ globalKPI.totalSensors }}</div>
                <div class="metric-sub">
                  <span class="sub-label">异常/离线:</span>
                  <span class="sub-value error">{{ globalKPI.abnormalSensors }}</span>
                </div>
              </div>
              <div class="metric-item">
                <div class="metric-label">今日高危/危险报警</div>
                <div class="metric-value danger">{{ globalKPI.todayAlerts }}</div>
                <div class="metric-sub">
                  <span class="sub-label">次数</span>
                </div>
              </div>
              <div class="metric-item">
                <div class="metric-label">进行中任务数</div>
                <div class="metric-value">{{ globalKPI.activeTasks }}</div>
                <div class="metric-sub">
                  <span class="sub-label">超时未处理:</span>
                  <span class="sub-value warning">{{ globalKPI.overdueTasks }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 检修员KPI指标卡 -->
          <div class="kpi-card maintenance-kpi">
            <div class="kpi-header">
              <h3>检修员KPI指标</h3>
              <i class="icon-maintenance">👷</i>
            </div>
            <div class="kpi-metrics">
              <div class="metric-item">
                <div class="metric-label">检修员总数</div>
                <div class="metric-value">{{ maintenanceKPI.totalStaff }}</div>
                <div class="metric-sub">
                  <span class="sub-label">在线人员</span>
                </div>
              </div>
              <div class="metric-item">
                <div class="metric-label">今日完成任务</div>
                <div class="metric-value success">{{ maintenanceKPI.todayCompleted }}</div>
                <div class="metric-sub">
                  <span class="sub-label">个</span>
                </div>
              </div>
              <div class="metric-item">
                <div class="metric-label">任务完成率</div>
                <div class="metric-value">{{ maintenanceKPI.completionRate }}%</div>
                <div class="metric-sub">
                  <span class="sub-label">平均处理时间:</span>
                  <span class="sub-value">{{ maintenanceKPI.avgProcessTime }}分钟</span>
                </div>
              </div>
            </div>
          </div>
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
            <div class="alert-list" ref="alertList">
              <div class="alert-list-container">
                <div 
                  v-for="alert in alertStream" 
                  :key="alert.id"
                  class="alert-item"
                  :class="alert.level"
                >
                  <div class="alert-time">{{ formatTime(alert.time) }}</div>
                  <div class="alert-content">
                    <div class="alert-sensor">{{ alert.sensorId }}</div>
                    <div class="alert-location">{{ alert.location }}</div>
                    <div class="alert-message">{{ alert.message }}</div>
                    <div class="alert-area">{{ alert.area }}</div>
                  </div>
                  <div class="alert-level-badge" :class="alert.level">
                    {{ getLevelText(alert.level) }}
                  </div>
                </div>
                <!-- 复制一份内容用于无缝滚动 -->
                <div 
                  v-for="alert in alertStream" 
                  :key="'copy-' + alert.id"
                  class="alert-item"
                  :class="alert.level"
                >
                  <div class="alert-time">{{ formatTime(alert.time) }}</div>
                  <div class="alert-content">
                    <div class="alert-sensor">{{ alert.sensorId }}</div>
                    <div class="alert-location">{{ alert.location }}</div>
                    <div class="alert-message">{{ alert.message }}</div>
                    <div class="alert-area">{{ alert.area }}</div>
                  </div>
                  <div class="alert-level-badge" :class="alert.level">
                    {{ getLevelText(alert.level) }}
                  </div>
                </div>
                <div v-if="alertStream.length === 0" class="no-alerts">
                  暂无高危/危险级报警
                </div>
              </div>
            </div>
          </div>
          
          <!-- 报警级别分布区域 -->
          <div class="alert-distribution">
            <h3>报警级别分布</h3>
            <div class="chart-container">
              <div class="pie-chart" ref="pieChart">
                <svg viewBox="0 0 200 200" class="pie-svg">
                  <circle
                    v-for="(segment, index) in pieSegments"
                    :key="index"
                    :cx="100"
                    :cy="100"
                    :r="80"
                    :stroke="segment.color"
                    :stroke-width="20"
                    :stroke-dasharray="segment.dashArray"
                    :stroke-dashoffset="segment.dashOffset"
                    :transform="segment.transform"
                    fill="transparent"
                    class="pie-segment"
                  />
                  <text x="100" y="100" text-anchor="middle" dy="0.3em" class="pie-center-text">
                    传感器状态
                  </text>
                </svg>
              </div>
              <div class="chart-legend">
                <div 
                  v-for="item in alertDistribution" 
                  :key="item.level"
                  class="legend-item"
                >
                  <div class="legend-color" :style="{ backgroundColor: item.color }"></div>
                  <div class="legend-info">
                    <div class="legend-label">{{ item.label }}</div>
                    <div class="legend-value">{{ item.count }} ({{ item.percentage }}%)</div>
                  </div>
                </div>
              </div>
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
      // 全局KPI数据
      globalKPI: {
        totalSensors: 0,
        abnormalSensors: 0,
        todayAlerts: 0,
        activeTasks: 0,
        overdueTasks: 0
      },
      // 检修员KPI数据
      maintenanceKPI: {
        totalStaff: 0,
        todayCompleted: 0,
        completionRate: 0,
        avgProcessTime: 0
      },
      // 实时报警流水数据
      alertStream: [],
      // 报警级别分布数据
      alertDistribution: [
        { level: 0, label: '安全', count: 0, color: '#10b981', percentage: 0 },
        { level: 1, label: '良好', count: 0, color: '#3b82f6', percentage: 0 },
        { level: 2, label: '危险', count: 0, color: '#f59e0b', percentage: 0 },
        { level: 3, label: '高危', count: 0, color: '#dc2626', percentage: 0 }
      ],
      // 定时器
      refreshTimer: null,
      alertTimer: null
    }
  },
  computed: {
    // 计算饼图段
    pieSegments() {
      const total = this.alertDistribution.reduce((sum, item) => sum + item.count, 0);
      if (total === 0) return [];
      
      const circumference = 2 * Math.PI * 80; // 半径为80的圆周长
      let currentOffset = 0;
      
      return this.alertDistribution.map(item => {
        const percentage = item.count / total;
        const dashLength = circumference * percentage;
        const dashArray = `${dashLength} ${circumference - dashLength}`;
        const dashOffset = -currentOffset;
        const transform = `rotate(-90 100 100)`; // 从顶部开始
        
        currentOffset += dashLength;
        
        return {
          color: item.color,
          dashArray,
          dashOffset,
          transform
        };
      }).filter(segment => parseFloat(segment.dashArray.split(' ')[0]) > 0);
    }
  },
  mounted() {
    console.log('数据可视化模块已加载');
    this.loadKPIData();
    this.loadAlertStream();
    this.loadAlertDistribution();
    // 每30秒刷新一次KPI数据
    this.refreshTimer = setInterval(() => {
      this.loadKPIData();
      this.loadAlertDistribution();
    }, 30000);
    // 每5秒刷新一次报警流水
    this.alertTimer = setInterval(() => {
      this.loadAlertStream();
    }, 5000);
  },
  beforeDestroy() {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
    }
    if (this.alertTimer) {
      clearInterval(this.alertTimer);
    }
  },
  methods: {
    // 加载KPI数据
    async loadKPIData() {
      try {
        // 模拟API调用获取实时数据
        await this.loadGlobalKPI();
        await this.loadMaintenanceKPI();
      } catch (error) {
        console.error('加载KPI数据失败:', error);
      }
    },
    
    // 加载全局KPI数据
    async loadGlobalKPI() {
      // 模拟API调用
      // 实际项目中这里应该调用真实的API
      this.globalKPI = {
        totalSensors: 156,
        abnormalSensors: 8,
        todayAlerts: 12,
        activeTasks: 23,
        overdueTasks: 3
      };
    },
    
    // 加载检修员KPI数据
    async loadMaintenanceKPI() {
      // 模拟API调用
      // 实际项目中这里应该调用真实的API
      const totalTasks = 45;
      const completedTasks = 38;
      this.maintenanceKPI = {
        totalStaff: 15,
        todayCompleted: completedTasks,
        completionRate: Math.round((completedTasks / totalTasks) * 100),
        avgProcessTime: 45
      };
    },
    
    // 加载实时报警流水
    async loadAlertStream() {
      // 模拟API调用获取最新的高危/危险级报警
      const mockAlerts = [
        {
          id: Date.now() + Math.random(),
          time: new Date(),
          sensorId: 'S001',
          location: '管道A-001',
          message: '压力过高',
          area: '东区',
          level: 'critical'
        },
        {
          id: Date.now() + Math.random() + 1,
          time: new Date(Date.now() - 2 * 60 * 1000),
          sensorId: 'S045',
          location: '管道B-023',
          message: '温度异常',
          area: '西区',
          level: 'danger'
        },
        {
          id: Date.now() + Math.random() + 2,
          time: new Date(Date.now() - 5 * 60 * 1000),
          sensorId: 'S078',
          location: '管道C-012',
          message: '流量异常',
          area: '南区',
          level: 'critical'
        }
      ];
      
      // 添加新报警到流水中，保持最新的20条
      this.alertStream = [...mockAlerts, ...this.alertStream.slice(0, 17)];
      
      // 自动滚动到顶部显示最新报警
      this.$nextTick(() => {
        if (this.$refs.alertList) {
          this.$refs.alertList.scrollTop = 0;
        }
      });
    },
    
    // 格式化时间显示
    formatTime(time) {
      const now = new Date();
      const diff = Math.floor((now - time) / 1000);
      
      if (diff < 60) {
        return `${diff}秒前`;
      } else if (diff < 3600) {
        return `${Math.floor(diff / 60)}分钟前`;
      } else if (diff < 86400) {
        return `${Math.floor(diff / 3600)}小时前`;
      } else {
        return time.toLocaleDateString() + ' ' + time.toLocaleTimeString();
      }
    },
    
    // 获取报警级别文本
    getLevelText(level) {
      const levelMap = {
        'critical': '高危',
        'danger': '危险',
        'warning': '警告',
        'normal': '正常'
      };
      return levelMap[level] || '未知';
    },
    
    // 加载报警级别分布数据
    async loadAlertDistribution() {
      // 模拟API调用获取传感器状态分布
      const mockData = {
        safe: 120,      // 安全 (0)
        good: 28,       // 良好 (1)
        danger: 6,      // 危险 (2)
        critical: 2     // 高危 (3)
      };
      
      const total = mockData.safe + mockData.good + mockData.danger + mockData.critical;
      
      this.alertDistribution = [
        {
          level: 0,
          label: '安全',
          count: mockData.safe,
          color: '#10b981',
          percentage: Math.round((mockData.safe / total) * 100)
        },
        {
          level: 1,
          label: '良好',
          count: mockData.good,
          color: '#3b82f6',
          percentage: Math.round((mockData.good / total) * 100)
        },
        {
          level: 2,
          label: '危险',
          count: mockData.danger,
          color: '#f59e0b',
          percentage: Math.round((mockData.danger / total) * 100)
        },
        {
          level: 3,
          label: '高危',
          count: mockData.critical,
          color: '#dc2626',
          percentage: Math.round((mockData.critical / total) * 100)
        }
      ];
    }
  }
}
</script>

<style scoped>
.data-visualization {
  height: 100vh;
  padding: 12px;
  background-color: #f8fafc;
  overflow: hidden;
  box-sizing: border-box;
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
  gap: 12px;
  height: calc(100vh - 80px);
  min-height: 500px;
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

.kpi-cards {
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.kpi-card {
  flex: 1;
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.kpi-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.kpi-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
}

.global-kpi::before {
  background: linear-gradient(90deg, #1e3a8a 0%, #3b82f6 100%);
}

.maintenance-kpi::before {
  background: linear-gradient(90deg, #059669 0%, #10b981 100%);
}

.kpi-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
}

.kpi-header h3 {
  color: #1e293b;
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}

.icon-global,
.icon-maintenance {
  font-size: 24px;
  opacity: 0.8;
}

.kpi-metrics {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.metric-item {
  flex: 1;
  text-align: center;
}

.metric-label {
  color: #64748b;
  font-size: 11px;
  font-weight: 500;
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.metric-value {
  color: #1e293b;
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 3px;
  line-height: 1;
}

.metric-value.danger {
  color: #dc2626;
}

.metric-value.success {
  color: #059669;
}

.metric-value.warning {
  color: #d97706;
}

.metric-sub {
  font-size: 11px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.sub-label {
  color: #64748b;
}

.sub-value {
  font-weight: 600;
}

.sub-value.error {
  color: #dc2626;
}

.sub-value.warning {
  color: #d97706;
}

.sub-value.success {
  color: #059669;
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
  gap: 12px;
}

.alert-stream,
.alert-distribution {
  background-color: white;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  flex: 1;
}

.alert-stream h3,
.alert-distribution h3 {
  color: #1e3a8a;
  font-size: 14px;
  margin-bottom: 8px;
  font-weight: 600;
}

/* 实时报警流水样式 */
.alert-list {
  height: 100%;
  overflow: hidden;
  padding-right: 8px;
  position: relative;
}

.alert-list-container {
  animation: autoScroll 20s linear infinite;
  transition: animation-play-state 0.3s ease;
}

.alert-list:hover .alert-list-container {
  animation-play-state: paused;
}

@keyframes autoScroll {
  0% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-50%);
  }
  100% {
    transform: translateY(0);
  }
}

.alert-list::-webkit-scrollbar {
  width: 4px;
}

.alert-list::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 2px;
}

.alert-list::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 2px;
}

.alert-list::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

.alert-item {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 8px;
  border-left: 4px solid #e2e8f0;
  transition: all 0.3s ease;
  cursor: pointer;
}

.alert-item:hover {
  background: #f1f5f9;
  transform: translateX(2px);
}

.alert-item.critical {
  border-left-color: #dc2626;
  background: #fef2f2;
}

.alert-item.critical:hover {
  background: #fee2e2;
}

.alert-item.danger {
  border-left-color: #ea580c;
  background: #fff7ed;
}

.alert-item.danger:hover {
  background: #fed7aa;
}

.alert-time {
  font-size: 10px;
  color: #64748b;
  margin-bottom: 6px;
  font-weight: 500;
}

.alert-content {
  margin-bottom: 8px;
}

.alert-sensor {
  font-size: 12px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 2px;
}

.alert-location {
  font-size: 11px;
  color: #475569;
  margin-bottom: 2px;
}

.alert-message {
  font-size: 12px;
  color: #dc2626;
  font-weight: 500;
  margin-bottom: 2px;
}

.alert-area {
  font-size: 10px;
  color: #64748b;
}

.alert-level-badge {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 9px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.alert-level-badge.critical {
  background: #dc2626;
  color: white;
}

.alert-level-badge.danger {
  background: #ea580c;
  color: white;
}

.alert-level-badge.warning {
  background: #d97706;
  color: white;
}

.no-alerts {
  text-align: center;
  color: #64748b;
  font-style: italic;
  padding: 40px 20px;
  font-size: 14px;
}

/* 报警级别分布样式 */
.alert-distribution {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  height: 100%;
}

.alert-distribution h3 {
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.chart-container {
  display: flex;
  align-items: center;
  gap: 20px;
  height: calc(100% - 60px);
}

.pie-chart {
  flex-shrink: 0;
  width: 200px;
  height: 200px;
}

.pie-svg {
  width: 100%;
  height: 100%;
}

.pie-segment {
  transition: stroke-width 0.3s ease;
}

.pie-segment:hover {
  stroke-width: 25;
}

.pie-center-text {
  font-size: 12px;
  font-weight: 600;
  fill: #64748b;
}

.chart-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #f8fafc;
  transition: background-color 0.2s ease;
}

.legend-item:hover {
  background: #e2e8f0;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-info {
  flex: 1;
}

.legend-label {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  margin-bottom: 2px;
}

.legend-value {
  font-size: 12px;
  color: #64748b;
}

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
  .kpi-cards {
    flex-direction: column;
  }
  
  .kpi-metrics {
    flex-direction: column;
    gap: 12px;
  }
  
  .metric-item {
    padding: 8px 0;
    border-bottom: 1px solid #f1f5f9;
  }
  
  .metric-item:last-child {
    border-bottom: none;
  }
  
  .panel-placeholder {
    flex-direction: column;
  }
}
</style>