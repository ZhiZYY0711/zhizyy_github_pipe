<template>
  <MainLayout @menu-change="handleMenuChange">
    <div class="main-page-content">
      <!-- 根据选中的菜单显示不同内容 -->
      <div v-if="currentMenu === 'visualization'" class="module-content">
        <h2>数据可视化</h2>
        <div class="content-card">
          <p>这里将显示各种数据图表和可视化内容</p>
          <div class="placeholder-chart">
            <div class="chart-item">管道压力趋势图</div>
            <div class="chart-item">流量监测图表</div>
            <div class="chart-item">温度分布图</div>
          </div>
        </div>
      </div>

      <div v-else-if="currentMenu === 'analysis'" class="module-content">
        <h2>数据分析</h2>
        <div class="content-card">
          <p>数据分析模块，提供深度数据洞察</p>
          <div class="analysis-grid">
            <div class="analysis-item">异常检测分析</div>
            <div class="analysis-item">趋势预测分析</div>
            <div class="analysis-item">性能评估分析</div>
            <div class="analysis-item">风险评估分析</div>
          </div>
        </div>
      </div>

      <div v-else-if="currentMenu === 'monitoring'" class="module-content">
        <MonitoringData />
      </div>

      <div v-else-if="currentMenu === 'equipment'" class="module-content">
        <h2>设备信息</h2>
        <div class="content-card">
          <p>设备管理和信息查看</p>
          <div class="equipment-grid">
            <div class="equipment-card">
              <h4>压缩机001</h4>
              <p>状态：运行中</p>
              <p>功率：85%</p>
            </div>
            <div class="equipment-card">
              <h4>阀门002</h4>
              <p>状态：开启</p>
              <p>开度：75%</p>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="currentMenu === 'tasks'" class="module-content">
        <h2>任务管理</h2>
        <div class="content-card">
          <p>任务分配和进度跟踪</p>
          <div class="task-list">
            <div class="task-item">
              <span class="task-title">管道巡检任务</span>
              <span class="task-status pending">待执行</span>
            </div>
            <div class="task-item">
              <span class="task-title">设备维护任务</span>
              <span class="task-status in-progress">进行中</span>
            </div>
            <div class="task-item">
              <span class="task-title">数据备份任务</span>
              <span class="task-status completed">已完成</span>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="currentMenu === 'maintenance'" class="module-content">
        <h2>检修员信息</h2>
        <div class="content-card">
          <p>检修人员管理和信息维护</p>
          <div class="staff-grid">
            <div class="staff-card">
              <h4>张三</h4>
              <p>工号：M001</p>
              <p>专业：管道检修</p>
              <p>状态：在岗</p>
            </div>
            <div class="staff-card">
              <h4>李四</h4>
              <p>工号：M002</p>
              <p>专业：设备维护</p>
              <p>状态：休假</p>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="currentMenu === 'logs'" class="module-content">
        <h2>日志记录</h2>
        <div class="content-card">
          <p>系统操作日志和事件记录</p>
          <div class="log-list">
            <div class="log-item">
              <span class="log-time">2024-01-15 10:30:25</span>
              <span class="log-content">用户登录系统</span>
              <span class="log-level info">信息</span>
            </div>
            <div class="log-item">
              <span class="log-time">2024-01-15 10:25:12</span>
              <span class="log-content">监测数据更新</span>
              <span class="log-level info">信息</span>
            </div>
            <div class="log-item">
              <span class="log-time">2024-01-15 09:45:33</span>
              <span class="log-content">设备状态异常</span>
              <span class="log-level warning">警告</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script>
import MainLayout from '@/components/MainLayout.vue'
import MonitoringData from '@/components/MonitoringData.vue'

export default {
  name: 'MainPage',
  components: {
    MainLayout,
    MonitoringData
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
.analysis-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
  margin-top: 20px;
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
}

.analysis-item:hover {
  transform: translateY(-2px);
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