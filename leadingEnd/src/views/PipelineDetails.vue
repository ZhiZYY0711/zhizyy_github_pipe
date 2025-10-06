<template>
  <div class="pipeline-details">
    <div class="page-header">
      <h1>管道详情</h1>
      <p>查看特定管道的详细监控信息和运行状态</p>
    </div>
    
    <div class="details-content">
      <!-- 管道选择器 -->
      <div class="pipeline-selector">
        <label for="pipeline-select">选择管道：</label>
        <select id="pipeline-select" v-model="selectedPipeline" @change="loadPipelineData">
          <option value="">请选择管道</option>
          <option value="pipe1">主干线A</option>
          <option value="pipe2">主干线B</option>
          <option value="pipe3">支线C</option>
          <option value="pipe4">支线D</option>
          <option value="pipe5">连接线E</option>
        </select>
      </div>
      
      <!-- 管道基本信息 -->
      <div class="pipeline-overview" v-if="selectedPipeline">
        <div class="overview-cards">
          <div class="overview-card">
            <h3>基本信息</h3>
            <div class="card-content">
              <p><strong>管道名称：</strong>{{ pipelineInfo.name }}</p>
              <p><strong>管道编号：</strong>{{ pipelineInfo.code }}</p>
              <p><strong>总长度：</strong>{{ pipelineInfo.length }} km</p>
              <p><strong>管径：</strong>{{ pipelineInfo.diameter }} mm</p>
              <p><strong>材质：</strong>{{ pipelineInfo.material }}</p>
              <p><strong>投运时间：</strong>{{ pipelineInfo.operationDate }}</p>
            </div>
          </div>
          
          <div class="overview-card">
            <h3>运行参数</h3>
            <div class="card-content">
              <p><strong>当前压力：</strong>{{ pipelineInfo.currentPressure }} MPa</p>
              <p><strong>设计压力：</strong>{{ pipelineInfo.designPressure }} MPa</p>
              <p><strong>当前流量：</strong>{{ pipelineInfo.currentFlow }} m³/h</p>
              <p><strong>设计流量：</strong>{{ pipelineInfo.designFlow }} m³/h</p>
              <p><strong>介质温度：</strong>{{ pipelineInfo.temperature }} °C</p>
              <p><strong>运行状态：</strong>
                <span :class="getStatusClass(pipelineInfo.status)">
                  {{ pipelineInfo.status }}
                </span>
              </p>
            </div>
          </div>
          
          <div class="overview-card">
            <h3>安全状况</h3>
            <div class="card-content">
              <p><strong>安全等级：</strong>
                <span :class="getAlertClass(pipelineInfo.safetyLevel)">
                  {{ getAlertText(pipelineInfo.safetyLevel) }}
                </span>
              </p>
              <p><strong>最近检测：</strong>{{ pipelineInfo.lastInspection }}</p>
              <p><strong>下次检测：</strong>{{ pipelineInfo.nextInspection }}</p>
              <p><strong>风险评估：</strong>{{ pipelineInfo.riskAssessment }}</p>
              <p><strong>维护状态：</strong>{{ pipelineInfo.maintenanceStatus }}</p>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 传感器分布图 -->
      <div class="sensor-distribution" v-if="selectedPipeline">
        <h2>传感器分布</h2>
        <div class="distribution-container">
          <div class="pipeline-diagram">
            <div class="pipeline-line">
              <div 
                v-for="(sensor, index) in sensorDistribution" 
                :key="sensor.id"
                class="sensor-point"
                :class="getSensorClass(sensor.status)"
                :style="{ left: sensor.position + '%' }"
                @click="selectSensor(sensor)"
                :title="`${sensor.id} - ${sensor.type} - ${sensor.status}`"
              >
                <span class="sensor-label">{{ sensor.id }}</span>
              </div>
            </div>
            <div class="distance-markers">
              <span v-for="marker in distanceMarkers" :key="marker" class="marker">
                {{ marker }}km
              </span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 选中传感器详情 -->
      <div class="selected-sensor-info" v-if="selectedSensor">
        <h3>传感器详情 - {{ selectedSensor.id }}</h3>
        <div class="sensor-details">
          <div class="detail-item">
            <strong>类型：</strong>{{ selectedSensor.type }}
          </div>
          <div class="detail-item">
            <strong>位置：</strong>{{ selectedSensor.location }}
          </div>
          <div class="detail-item">
            <strong>状态：</strong>
            <span :class="getSensorClass(selectedSensor.status)">
              {{ selectedSensor.status }}
            </span>
          </div>
          <div class="detail-item">
            <strong>当前读数：</strong>{{ selectedSensor.reading }}
          </div>
          <div class="detail-item">
            <strong>报警等级：</strong>
            <span :class="getAlertClass(selectedSensor.alertLevel)">
              {{ getAlertText(selectedSensor.alertLevel) }}
            </span>
          </div>
          <div class="detail-item">
            <strong>最后更新：</strong>{{ selectedSensor.lastUpdate }}
          </div>
        </div>
      </div>
      
      <!-- 历史数据表格 -->
      <div class="history-data" v-if="selectedPipeline">
        <h2>历史检测记录</h2>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>检测时间</th>
                <th>检测类型</th>
                <th>检测结果</th>
                <th>发现问题</th>
                <th>处理状态</th>
                <th>检测员</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="record in historyData" :key="record.id">
                <td>{{ record.date }}</td>
                <td>{{ record.type }}</td>
                <td>
                  <span :class="getResultClass(record.result)">
                    {{ record.result }}
                  </span>
                </td>
                <td>{{ record.issues }}</td>
                <td>
                  <span :class="getProcessClass(record.processStatus)">
                    {{ record.processStatus }}
                  </span>
                </td>
                <td>{{ record.inspector }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      
      <!-- 空状态 -->
      <div class="empty-state" v-if="!selectedPipeline">
        <p>请选择一条管道查看详细信息</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PipelineDetails',
  data() {
    return {
      selectedPipeline: '',
      selectedSensor: null,
      pipelineInfo: {},
      sensorDistribution: [],
      historyData: [],
      distanceMarkers: []
    }
  },
  methods: {
    loadPipelineData() {
      if (!this.selectedPipeline) {
        this.pipelineInfo = {};
        this.sensorDistribution = [];
        this.historyData = [];
        this.selectedSensor = null;
        return;
      }
      
      // 模拟管道基本信息
      this.pipelineInfo = {
        name: this.getPipelineName(this.selectedPipeline),
        code: this.selectedPipeline.toUpperCase() + '-001',
        length: Math.floor(Math.random() * 200) + 50,
        diameter: [500, 600, 800, 1000][Math.floor(Math.random() * 4)],
        material: '20号钢',
        operationDate: '2018-06-15',
        currentPressure: (Math.random() * 2 + 6).toFixed(2),
        designPressure: '8.0',
        currentFlow: Math.floor(Math.random() * 1000) + 500,
        designFlow: '1500',
        temperature: Math.floor(Math.random() * 20) + 15,
        status: ['正常运行', '维护中', '停运'][Math.floor(Math.random() * 3)],
        safetyLevel: Math.floor(Math.random() * 4),
        lastInspection: '2024-01-15',
        nextInspection: '2024-07-15',
        riskAssessment: '低风险',
        maintenanceStatus: '良好'
      };
      
      // 生成传感器分布
      const sensorCount = Math.floor(Math.random() * 8) + 5;
      this.sensorDistribution = Array.from({ length: sensorCount }, (_, index) => ({
        id: `S${index + 1}`,
        type: ['压力传感器', '温度传感器', '流量传感器'][Math.floor(Math.random() * 3)],
        position: (index + 1) * (100 / (sensorCount + 1)),
        location: `${((index + 1) * (this.pipelineInfo.length / (sensorCount + 1))).toFixed(1)}km`,
        status: ['正常', '异常', '离线'][Math.floor(Math.random() * 3)],
        reading: `${(Math.random() * 100).toFixed(2)} ${['MPa', '°C', 'm³/h'][Math.floor(Math.random() * 3)]}`,
        alertLevel: Math.floor(Math.random() * 4),
        lastUpdate: new Date(Date.now() - Math.random() * 3600000).toLocaleString()
      }));
      
      // 生成距离标记
      this.distanceMarkers = Array.from({ length: 6 }, (_, index) => 
        Math.floor(index * (this.pipelineInfo.length / 5))
      );
      
      // 生成历史数据
      this.historyData = Array.from({ length: 10 }, (_, index) => ({
        id: index + 1,
        date: new Date(Date.now() - Math.random() * 30 * 24 * 3600000).toLocaleDateString(),
        type: ['定期检测', '专项检测', '应急检测'][Math.floor(Math.random() * 3)],
        result: ['合格', '不合格', '需关注'][Math.floor(Math.random() * 3)],
        issues: ['无', '轻微腐蚀', '压力异常', '温度偏高'][Math.floor(Math.random() * 4)],
        processStatus: ['已处理', '处理中', '待处理'][Math.floor(Math.random() * 3)],
        inspector: ['张三', '李四', '王五'][Math.floor(Math.random() * 3)]
      }));
    },
    
    getPipelineName(pipelineCode) {
      const pipelineNames = {
        pipe1: '主干线A',
        pipe2: '主干线B',
        pipe3: '支线C',
        pipe4: '支线D',
        pipe5: '连接线E'
      };
      return pipelineNames[pipelineCode] || '未知管道';
    },
    
    selectSensor(sensor) {
      this.selectedSensor = sensor;
    },
    
    getStatusClass(status) {
      const classMap = {
        '正常运行': 'status-normal',
        '维护中': 'status-maintenance',
        '停运': 'status-stopped'
      };
      return classMap[status] || 'status-normal';
    },
    
    getSensorClass(status) {
      const classMap = {
        '正常': 'sensor-normal',
        '异常': 'sensor-abnormal',
        '离线': 'sensor-offline'
      };
      return classMap[status] || 'sensor-normal';
    },
    
    getAlertClass(level) {
      const classes = ['alert-safe', 'alert-good', 'alert-danger', 'alert-critical'];
      return classes[level] || 'alert-safe';
    },
    
    getAlertText(level) {
      const texts = ['安全', '良好', '危险', '高危'];
      return texts[level] || '安全';
    },
    
    getResultClass(result) {
      const classMap = {
        '合格': 'result-pass',
        '不合格': 'result-fail',
        '需关注': 'result-attention'
      };
      return classMap[result] || 'result-pass';
    },
    
    getProcessClass(status) {
      const classMap = {
        '已处理': 'process-completed',
        '处理中': 'process-ongoing',
        '待处理': 'process-pending'
      };
      return classMap[status] || 'process-pending';
    }
  }
}
</script>

<style scoped>
.pipeline-details {
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

.pipeline-selector {
  margin-bottom: 30px;
  text-align: center;
}

.pipeline-selector label {
  font-weight: 600;
  color: #374151;
  margin-right: 10px;
}

.pipeline-selector select {
  padding: 8px 15px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 16px;
  background-color: white;
  color: #374151;
}

.pipeline-overview {
  margin-bottom: 40px;
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 20px;
}

.overview-card {
  background-color: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.overview-card h3 {
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

.sensor-distribution {
  background-color: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.sensor-distribution h2 {
  color: #1e3a8a;
  font-size: 20px;
  margin-bottom: 20px;
  font-weight: 600;
}

.distribution-container {
  padding: 20px 0;
}

.pipeline-diagram {
  position: relative;
  height: 80px;
}

.pipeline-line {
  position: relative;
  height: 8px;
  background: linear-gradient(90deg, #3b82f6, #1e40af);
  border-radius: 4px;
  margin: 20px 0;
}

.sensor-point {
  position: absolute;
  top: -15px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  cursor: pointer;
  transform: translateX(-50%);
  transition: all 0.3s ease;
}

.sensor-point:hover {
  transform: translateX(-50%) scale(1.5);
}

.sensor-normal {
  background-color: #10b981;
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.5);
}

.sensor-abnormal {
  background-color: #f59e0b;
  box-shadow: 0 0 10px rgba(245, 158, 11, 0.5);
}

.sensor-offline {
  background-color: #6b7280;
  box-shadow: 0 0 10px rgba(107, 114, 128, 0.5);
}

.sensor-label {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  color: #374151;
  font-weight: 600;
  white-space: nowrap;
}

.distance-markers {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
}

.marker {
  font-size: 12px;
  color: #6b7280;
}

.selected-sensor-info {
  background-color: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.selected-sensor-info h3 {
  color: #1e3a8a;
  font-size: 18px;
  margin-bottom: 15px;
  font-weight: 600;
}

.sensor-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 15px;
}

.detail-item {
  color: #374151;
  line-height: 1.6;
}

.history-data {
  background-color: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.history-data h2 {
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

.status-maintenance {
  color: #0891b2;
  font-weight: 600;
}

.status-stopped {
  color: #dc2626;
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

/* 检测结果样式 */
.result-pass {
  color: #059669;
  font-weight: 600;
}

.result-fail {
  color: #dc2626;
  font-weight: 600;
}

.result-attention {
  color: #ea580c;
  font-weight: 600;
}

/* 处理状态样式 */
.process-completed {
  color: #059669;
  font-weight: 600;
}

.process-ongoing {
  color: #0891b2;
  font-weight: 600;
}

.process-pending {
  color: #ea580c;
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
  .overview-cards {
    grid-template-columns: 1fr;
  }
  
  .sensor-details {
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