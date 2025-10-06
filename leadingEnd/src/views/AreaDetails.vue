<template>
  <div class="area-details">
    <div class="page-header">
      <h1>区域详情</h1>
      <p>查看特定区域的详细监控信息</p>
    </div>
    
    <div class="details-content">
      <!-- 区域选择器 -->
      <div class="area-selector">
        <label for="area-select">选择区域：</label>
        <select id="area-select" v-model="selectedArea" @change="loadAreaData">
          <option value="">请选择区域</option>
          <option value="area1">华北区域</option>
          <option value="area2">华东区域</option>
          <option value="area3">华南区域</option>
          <option value="area4">西北区域</option>
        </select>
      </div>
      
      <!-- 区域信息卡片 -->
      <div class="area-info-cards" v-if="selectedArea">
        <div class="info-card">
          <h3>基本信息</h3>
          <div class="card-content">
            <p><strong>区域名称：</strong>{{ areaInfo.name }}</p>
            <p><strong>管道总长：</strong>{{ areaInfo.totalLength }} km</p>
            <p><strong>传感器数量：</strong>{{ areaInfo.sensorCount }} 个</p>
            <p><strong>管理员：</strong>{{ areaInfo.manager }}</p>
          </div>
        </div>
        
        <div class="info-card">
          <h3>运行状态</h3>
          <div class="card-content">
            <p><strong>正常传感器：</strong>{{ areaInfo.normalSensors }} 个</p>
            <p><strong>异常传感器：</strong>{{ areaInfo.abnormalSensors }} 个</p>
            <p><strong>离线传感器：</strong>{{ areaInfo.offlineSensors }} 个</p>
            <p><strong>最高报警等级：</strong>
              <span :class="getAlertClass(areaInfo.maxAlertLevel)">
                {{ getAlertText(areaInfo.maxAlertLevel) }}
              </span>
            </p>
          </div>
        </div>
        
        <div class="info-card">
          <h3>任务统计</h3>
          <div class="card-content">
            <p><strong>进行中任务：</strong>{{ areaInfo.activeTasks }} 个</p>
            <p><strong>已完成任务：</strong>{{ areaInfo.completedTasks }} 个</p>
            <p><strong>超时任务：</strong>{{ areaInfo.overdueTasks }} 个</p>
            <p><strong>任务完成率：</strong>{{ areaInfo.taskCompletionRate }}%</p>
          </div>
        </div>
      </div>
      
      <!-- 详细数据表格 -->
      <div class="data-table-section" v-if="selectedArea">
        <h2>传感器详细信息</h2>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>传感器ID</th>
                <th>位置</th>
                <th>类型</th>
                <th>状态</th>
                <th>最新读数</th>
                <th>报警等级</th>
                <th>最后更新</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="sensor in sensorData" :key="sensor.id">
                <td>{{ sensor.id }}</td>
                <td>{{ sensor.location }}</td>
                <td>{{ sensor.type }}</td>
                <td>
                  <span :class="getStatusClass(sensor.status)">
                    {{ sensor.status }}
                  </span>
                </td>
                <td>{{ sensor.reading }}</td>
                <td>
                  <span :class="getAlertClass(sensor.alertLevel)">
                    {{ getAlertText(sensor.alertLevel) }}
                  </span>
                </td>
                <td>{{ sensor.lastUpdate }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      
      <!-- 空状态 -->
      <div class="empty-state" v-if="!selectedArea">
        <p>请选择一个区域查看详细信息</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AreaDetails',
  data() {
    return {
      selectedArea: '',
      areaInfo: {},
      sensorData: []
    }
  },
  methods: {
    loadAreaData() {
      if (!this.selectedArea) {
        this.areaInfo = {};
        this.sensorData = [];
        return;
      }
      
      // 模拟数据加载
      this.areaInfo = {
        name: this.getAreaName(this.selectedArea),
        totalLength: Math.floor(Math.random() * 500) + 100,
        sensorCount: Math.floor(Math.random() * 50) + 20,
        manager: '张三',
        normalSensors: Math.floor(Math.random() * 40) + 15,
        abnormalSensors: Math.floor(Math.random() * 8) + 2,
        offlineSensors: Math.floor(Math.random() * 3) + 1,
        maxAlertLevel: Math.floor(Math.random() * 4),
        activeTasks: Math.floor(Math.random() * 10) + 2,
        completedTasks: Math.floor(Math.random() * 50) + 20,
        overdueTasks: Math.floor(Math.random() * 3),
        taskCompletionRate: Math.floor(Math.random() * 20) + 80
      };
      
      // 生成模拟传感器数据
      this.sensorData = Array.from({ length: 10 }, (_, index) => ({
        id: `S${this.selectedArea.toUpperCase()}-${String(index + 1).padStart(3, '0')}`,
        location: `位置点 ${index + 1}`,
        type: ['压力传感器', '温度传感器', '流量传感器'][Math.floor(Math.random() * 3)],
        status: ['正常', '异常', '离线'][Math.floor(Math.random() * 3)],
        reading: `${(Math.random() * 100).toFixed(2)} ${['MPa', '°C', 'm³/h'][Math.floor(Math.random() * 3)]}`,
        alertLevel: Math.floor(Math.random() * 4),
        lastUpdate: new Date(Date.now() - Math.random() * 3600000).toLocaleString()
      }));
    },
    
    getAreaName(areaCode) {
      const areaNames = {
        area1: '华北区域',
        area2: '华东区域',
        area3: '华南区域',
        area4: '西北区域'
      };
      return areaNames[areaCode] || '未知区域';
    },
    
    getAlertClass(level) {
      const classes = ['alert-safe', 'alert-good', 'alert-danger', 'alert-critical'];
      return classes[level] || 'alert-safe';
    },
    
    getAlertText(level) {
      const texts = ['安全', '良好', '危险', '高危'];
      return texts[level] || '安全';
    },
    
    getStatusClass(status) {
      const classMap = {
        '正常': 'status-normal',
        '异常': 'status-abnormal',
        '离线': 'status-offline'
      };
      return classMap[status] || 'status-normal';
    }
  }
}
</script>

<style scoped>
.area-details {
  padding: 20px;
  background-color: #f8fafc;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 30px;
  text-align: center;
}

.page-header h1 {
  color: #1e3a8a;
  font-size: 28px;
  margin-bottom: 10px;
}

.page-header p {
  color: #64748b;
  font-size: 16px;
}

.details-content {
  max-width: 1200px;
  margin: 0 auto;
}

.area-selector {
  margin-bottom: 30px;
  text-align: center;
}

.area-selector label {
  font-weight: 600;
  color: #374151;
  margin-right: 10px;
}

.area-selector select {
  padding: 8px 15px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 16px;
  background-color: white;
  color: #374151;
}

.area-info-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 40px;
}

.info-card {
  background-color: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.info-card h3 {
  color: #1e3a8a;
  font-size: 18px;
  margin-bottom: 15px;
  font-weight: 600;
  border-bottom: 2px solid #e2e8f0;
  padding-bottom: 8px;
}

.card-content p {
  margin: 10px 0;
  color: #374151;
  line-height: 1.6;
}

.data-table-section {
  background-color: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.data-table-section h2 {
  color: #1e3a8a;
  font-size: 20px;
  margin-bottom: 20px;
  font-weight: 600;
}

.table-container {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th,
.data-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.data-table th {
  background-color: #f8fafc;
  font-weight: 600;
  color: #374151;
}

.data-table tr:hover {
  background-color: #f8fafc;
}

/* 状态样式 */
.status-normal {
  color: #059669;
  font-weight: 600;
}

.status-abnormal {
  color: #dc2626;
  font-weight: 600;
}

.status-offline {
  color: #6b7280;
  font-weight: 600;
}

/* 报警等级样式 */
.alert-safe {
  color: #059669;
  font-weight: 600;
}

.alert-good {
  color: #0891b2;
  font-weight: 600;
}

.alert-danger {
  color: #ea580c;
  font-weight: 600;
}

.alert-critical {
  color: #dc2626;
  font-weight: 600;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #6b7280;
  font-size: 18px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .area-info-cards {
    grid-template-columns: 1fr;
  }
  
  .data-table {
    font-size: 12px;
  }
  
  .data-table th,
  .data-table td {
    padding: 8px;
  }
}
</style>