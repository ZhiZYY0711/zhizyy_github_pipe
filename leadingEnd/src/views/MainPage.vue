<template>
  <MainLayout @menu-change="handleMenuChange">
    <div class="main-page-content">
      <!-- 根据选中的菜单显示不同内容 -->
      <div v-if="currentMenu === 'visualization'" class="module-content">
        <DataVisualization />
      </div>

      <div v-else-if="currentMenu === 'analysis'" class="module-content">
        <h2>数据分析</h2>
        <div class="content-card">
          <p>数据分析模块，提供深度数据洞察</p>
          
          <!-- 图表和分析项目的布局容器 -->
          <div class="analysis-layout">
            <!-- 左上角的折线图 -->
            <div class="chart-section">
              <FlowTrendChart />
            </div>
            
            <!-- 右侧的分析项目 -->
            <div class="analysis-items-section">
              <div class="analysis-grid">
                <div class="analysis-item">异常检测分析</div>
                <div class="analysis-item">趋势预测分析</div>
                <div class="analysis-item">性能评估分析</div>
                <div class="analysis-item">风险评估分析</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="currentMenu === 'monitoring'" class="module-content">
        <MonitoringData />
      </div>

      <div v-else-if="currentMenu === 'equipment'" class="module-content">
        <DeviceInfo />
      </div>

      <div v-else-if="currentMenu === 'tasks'" class="module-content">
        <TaskManagement />
      </div>

      <div v-else-if="currentMenu === 'maintenance'" class="module-content">
        <RepairmanInfo />
      </div>

      <div v-else-if="currentMenu === 'virtualExpert'" class="module-content">
        <h2>虚拟专家</h2>
        <div class="content-card">
          <p>AI智能专家系统，提供专业的管道监测分析和建议</p>
          <div class="expert-grid">
            <div class="expert-card">
              <h4>智能诊断</h4>
              <p>基于AI算法的设备故障诊断</p>
              <button class="btn btn-primary">开始诊断</button>
            </div>
            <div class="expert-card">
              <h4>预测分析</h4>
              <p>管道运行状态预测和风险评估</p>
              <button class="btn btn-primary">查看预测</button>
            </div>
            <div class="expert-card">
              <h4>优化建议</h4>
              <p>运行参数优化和维护建议</p>
              <button class="btn btn-primary">获取建议</button>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="currentMenu === 'simulation'" class="module-content">
        <h2>模拟与演练</h2>
        <div class="content-card">
          <p>管道系统仿真模拟和应急演练平台</p>
          <div class="simulation-grid">
            <div class="simulation-card">
              <h4>压力仿真</h4>
              <p>管道压力变化仿真模拟</p>
              <button class="btn btn-success">启动仿真</button>
            </div>
            <div class="simulation-card">
              <h4>泄漏演练</h4>
              <p>管道泄漏应急处置演练</p>
              <button class="btn btn-success">开始演练</button>
            </div>
            <div class="simulation-card">
              <h4>维修演练</h4>
              <p>设备维修操作流程演练</p>
              <button class="btn btn-success">进入演练</button>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="currentMenu === 'emergency'" class="module-content">
        <h2>事故响应</h2>
        <div class="content-card">
          <p>应急事故处理和响应管理系统</p>
          <div class="emergency-dashboard">
            <div class="emergency-status">
              <h4>当前状态：<span class="status-normal">正常</span></h4>
              <p>系统运行正常，无紧急事件</p>
            </div>
            <div class="emergency-actions">
              <button class="btn btn-danger">启动应急预案</button>
              <button class="btn btn-warning">发布预警信息</button>
              <button class="btn btn-info">查看应急手册</button>
            </div>
            <div class="emergency-contacts">
              <h4>应急联系人</h4>
              <div class="contact-list">
                <div class="contact-item">
                  <span class="contact-name">应急指挥中心</span>
                  <span class="contact-phone">400-123-4567</span>
                </div>
                <div class="contact-item">
                  <span class="contact-name">技术支持</span>
                  <span class="contact-phone">400-123-4568</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="currentMenu === 'logs'" class="module-content">
        <LogRecord />
      </div>
    </div>
  </MainLayout>
</template>

<script>
import MainLayout from '@/components/MainLayout.vue'
import MonitoringData from '@/components/MonitoringData.vue'
import DeviceInfo from '@/components/DeviceInfo.vue'
import TaskManagement from '@/components/TaskManagement.vue'
import RepairmanInfo from '@/components/RepairmanInfo.vue'
import LogRecord from '@/components/LogRecord.vue'
import FlowTrendChart from '@/components/FlowTrendChart.vue'
import DataVisualization from '@/views/DataVisualization.vue'

export default {
  name: 'MainPage',
  components: {
    MainLayout,
    MonitoringData,
    DeviceInfo,
    TaskManagement,
    RepairmanInfo,
    LogRecord,
    FlowTrendChart,
    DataVisualization
  },
  data() {
    return {
      currentMenu: 'visualization'
    }
  },
  methods: {
    handleMenuChange(menu) {
      this.currentMenu = menu;
    }
  }
}
</script>

<style scoped>
.main-page-content {
  height: 100%;
  overflow: hidden;
}

.module-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.module-content h2 {
  color: #1E3A8A;
  margin-bottom: 20px;
  font-size: 24px;
  font-weight: 600;
}

.content-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  flex: 1;
  overflow-y: auto;
}

/* 数据可视化样式 */
.placeholder-chart {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 15px;
  margin-top: 20px;
}

.chart-item {
  background: #f8f9fa;
  border: 2px dashed #1E3A8A;
  border-radius: 6px;
  padding: 40px 20px;
  text-align: center;
  color: #1E3A8A;
  font-weight: 500;
}

/* 数据分析样式 */
.analysis-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-top: 20px;
  min-height: 400px;
}

.chart-section {
  grid-column: 1;
  grid-row: 1;
  min-height: 380px;
}

.analysis-items-section {
  grid-column: 2;
  grid-row: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}

.analysis-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 15px;
  height: fit-content;
}

.analysis-item {
  background: linear-gradient(135deg, #1E3A8A, #3B82F6);
  color: white;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
  font-weight: 500;
  cursor: pointer;
  transition: transform 0.3s ease;
  min-height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.analysis-item:hover {
  transform: translateY(-2px);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .analysis-layout {
    grid-template-columns: 1fr;
    grid-template-rows: auto auto;
    gap: 20px;
  }
  
  .chart-section {
    grid-column: 1;
    grid-row: 1;
  }
  
  .analysis-items-section {
    grid-column: 1;
    grid-row: 2;
  }
  
  .analysis-grid {
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  }
}

/* 监测数据表格样式 */
.monitoring-table {
  margin-top: 20px;
}

.table-header, .table-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  gap: 10px;
  padding: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.table-header {
  background: #f3f4f6;
  font-weight: 600;
  color: #374151;
}

.status-normal {
  color: #10b981;
  font-weight: 500;
}

/* 设备信息样式 */
.equipment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.equipment-card {
  background: #f8f9fa;
  border-left: 4px solid #1E3A8A;
  padding: 20px;
  border-radius: 6px;
}

.equipment-card h4 {
  color: #1E3A8A;
  margin-bottom: 10px;
}

/* 任务管理样式 */
.task-list {
  margin-top: 20px;
}

.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #e5e7eb;
}

.task-title {
  font-weight: 500;
}

.task-status {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.task-status.pending {
  background: #fef3c7;
  color: #d97706;
}

.task-status.in-progress {
  background: #dbeafe;
  color: #2563eb;
}

.task-status.completed {
  background: #d1fae5;
  color: #059669;
}

/* 检修员信息样式 */
.staff-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.staff-card {
  background: #f8f9fa;
  border: 1px solid #e5e7eb;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
}

.staff-card h4 {
  color: #1E3A8A;
  margin-bottom: 10px;
}

/* 虚拟专家样式 */
.expert-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.expert-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 25px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
}

.expert-card:hover {
  transform: translateY(-5px);
}

.expert-card h4 {
  margin-bottom: 10px;
  font-size: 18px;
}

.expert-card p {
  margin-bottom: 15px;
  opacity: 0.9;
}

/* 模拟与演练样式 */
.simulation-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.simulation-card {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: white;
  padding: 25px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
}

.simulation-card:hover {
  transform: translateY(-5px);
}

.simulation-card h4 {
  margin-bottom: 10px;
  font-size: 18px;
}

.simulation-card p {
  margin-bottom: 15px;
  opacity: 0.9;
}

/* 事故响应样式 */
.emergency-dashboard {
  margin-top: 20px;
}

.emergency-status {
  background: #f0f9ff;
  border-left: 4px solid #0ea5e9;
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 6px;
}

.emergency-status h4 {
  margin-bottom: 10px;
  color: #0369a1;
}

.emergency-actions {
  display: flex;
  gap: 15px;
  margin-bottom: 25px;
  flex-wrap: wrap;
}

.emergency-contacts {
  background: #fafafa;
  padding: 20px;
  border-radius: 8px;
}

.emergency-contacts h4 {
  margin-bottom: 15px;
  color: #1E3A8A;
}

.contact-list {
  display: grid;
  gap: 10px;
}

.contact-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: white;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}

.contact-name {
  font-weight: 500;
}

.contact-phone {
  color: #1E3A8A;
  font-weight: 600;
}

/* 按钮样式 */
.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  text-decoration: none;
  display: inline-block;
}

.btn-primary {
  background: #1E3A8A;
  color: white;
}

.btn-primary:hover {
  background: #1e40af;
  transform: translateY(-1px);
}

.btn-success {
  background: #059669;
  color: white;
}

.btn-success:hover {
  background: #047857;
  transform: translateY(-1px);
}

.btn-danger {
  background: #dc2626;
  color: white;
}

.btn-danger:hover {
  background: #b91c1c;
  transform: translateY(-1px);
}

.btn-warning {
  background: #d97706;
  color: white;
}

.btn-warning:hover {
  background: #b45309;
  transform: translateY(-1px);
}

.btn-info {
  background: #0891b2;
  color: white;
}

.btn-info:hover {
  background: #0e7490;
  transform: translateY(-1px);
}

/* 日志记录样式 */
.log-list {
  margin-top: 20px;
}

.log-item {
  display: grid;
  grid-template-columns: 150px 1fr 80px;
  gap: 15px;
  padding: 12px;
  border-bottom: 1px solid #e5e7eb;
  align-items: center;
}

.log-time {
  font-size: 12px;
  color: #6b7280;
}

.log-level.info {
  background: #dbeafe;
  color: #2563eb;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  text-align: center;
}

.log-level.warning {
  background: #fef3c7;
  color: #d97706;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  text-align: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .placeholder-chart {
    grid-template-columns: 1fr;
  }
  
  .analysis-grid {
    grid-template-columns: 1fr;
  }
  
  .equipment-grid {
    grid-template-columns: 1fr;
  }
  
  .staff-grid {
    grid-template-columns: 1fr;
  }
  
  .expert-grid {
    grid-template-columns: 1fr;
  }
  
  .simulation-grid {
    grid-template-columns: 1fr;
  }
  
  .emergency-actions {
    flex-direction: column;
  }
  
  .contact-item {
    flex-direction: column;
    gap: 5px;
    text-align: center;
  }
  
  .table-header, .table-row {
    grid-template-columns: 1fr;
    gap: 5px;
  }
  
  .log-item {
    grid-template-columns: 1fr;
    gap: 5px;
  }
}
</style>