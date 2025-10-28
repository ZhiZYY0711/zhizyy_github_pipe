<template>
  <div class="monitoring-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">监测数据管理</h2>
      <div class="header-actions">
        <button class="btn btn-primary" @click="showAddModal = true">
          <i class="icon">+</i> 添加监测数据
        </button>
        <button class="btn btn-secondary" @click="refreshData">
          <i class="icon">↻</i> 刷新
        </button>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-section">
      <div class="stat-card">
        <div class="stat-number">{{ totalMonitoringData }}</div>
        <div class="stat-label">总监测记录</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ normalDataCount }}</div>
        <div class="stat-label">正常数据</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ abnormalDataCount }}</div>
        <div class="stat-label">异常数据</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ todayDataCount }}</div>
        <div class="stat-label">今日新增</div>
      </div>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-item">
          <label>传感器ID：</label>
          <input v-model="searchForm.sensorId" placeholder="请输入传感器ID" class="input-field">
        </div>
        <div class="filter-item">
          <label>区域：</label>
          <select v-model="searchForm.area" class="select-field">
            <option value="">全部区域</option>
            <option value="华北区域">华北区域</option>
            <option value="华东区域">华东区域</option>
            <option value="华南区域">华南区域</option>
            <option value="西北区域">西北区域</option>
            <option value="西南区域">西南区域</option>
          </select>
        </div>
        <div class="filter-item">
          <label>数据状态：</label>
          <select v-model="searchForm.status" class="select-field">
            <option value="">全部状态</option>
            <option value="0">安全</option>
            <option value="1">良好</option>
            <option value="2">危险</option>
            <option value="3">高危</option>
          </select>
        </div>
        <div class="filter-item">
          <label>管道名称：</label>
          <input v-model="searchForm.pipelineName" placeholder="请输入管道名称" class="input-field">
        </div>
        <button class="btn btn-search" @click="searchMonitoringData">搜索</button>
      </div>
    </div>

    <!-- 监测数据列表 -->
    <div class="monitoring-list">
      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>监测ID</th>
              <th>传感器ID</th>
              <th>压力值(MPa)</th>
              <th>温度(°C)</th>
              <th>流量(m³/h)</th>
              <th>震动值</th>
              <th>数据状态</th>
              <th>区域</th>
              <th>所属管道</th>
              <th>监测时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="data in paginatedMonitoringData" :key="data.id">
              <td>{{ data.id }}</td>
              <td>{{ data.sensorId }}</td>
              <td>{{ data.pressure }}</td>
              <td>{{ data.temperature }}</td>
              <td>{{ data.traffic }}</td>
              <td>{{ data.shake }}</td>
              <td>
                <span class="status-badge" :class="getStatusClass(data.dataStatus)">
                  {{ getStatusText(data.dataStatus) }}
                </span>
              </td>
              <td>{{ data.area }}</td>
              <td>{{ data.pipelineName }}</td>
              <td>{{ data.createTime }}</td>
              <td>
                <button class="btn-action btn-view" @click="viewMonitoringData(data)">查看</button>
                <button class="btn-action btn-edit" @click="editMonitoringData(data)">编辑</button>
                <button class="btn-action btn-delete" @click="deleteMonitoringData(data)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="pagination">
        <button 
          class="page-btn" 
          :disabled="currentPage === 1" 
          @click="currentPage--"
        >
          上一页
        </button>
        <span class="page-info">
          第 {{ currentPage }} 页，共 {{ totalPages }} 页，总计 {{ filteredMonitoringData.length }} 条记录
        </span>
        <button 
          class="page-btn" 
          :disabled="currentPage === totalPages" 
          @click="currentPage++"
        >
          下一页
        </button>
      </div>
    </div>

    <!-- 添加/编辑监测数据模态框 -->
    <div v-if="showAddModal || showEditModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ showAddModal ? '添加监测数据' : '编辑监测数据' }}</h3>
          <button class="close-btn" @click="closeModal">×</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveMonitoringData">
            <div class="form-row">
              <div class="form-group">
                <label>传感器ID：</label>
                <input v-model="formData.sensorId" type="text" required class="form-input">
              </div>
              <div class="form-group">
                <label>压力值(MPa)：</label>
                <input v-model="formData.pressure" type="number" step="0.01" required class="form-input">
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>温度(°C)：</label>
                <input v-model="formData.temperature" type="number" step="0.1" required class="form-input">
              </div>
              <div class="form-group">
                <label>流量(m³/h)：</label>
                <input v-model="formData.traffic" type="number" step="0.01" required class="form-input">
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>震动值：</label>
                <input v-model="formData.shake" type="number" step="0.01" required class="form-input">
              </div>
              <div class="form-group">
                <label>数据状态：</label>
                <select v-model="formData.dataStatus" required class="form-input">
                  <option value="0">安全</option>
                  <option value="1">良好</option>
                  <option value="2">危险</option>
                  <option value="3">高危</option>
                </select>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>区域：</label>
                <select v-model="formData.area" required class="form-input">
                  <option value="华北区域">华北区域</option>
                  <option value="华东区域">华东区域</option>
                  <option value="华南区域">华南区域</option>
                  <option value="西北区域">西北区域</option>
                  <option value="西南区域">西南区域</option>
                </select>
              </div>
              <div class="form-group">
                <label>所属管道：</label>
                <input v-model="formData.pipelineName" type="text" required class="form-input">
              </div>
            </div>
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="closeModal">取消</button>
              <button type="submit" class="btn btn-primary">保存</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- 查看监测数据模态框 -->
    <div v-if="showViewModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>监测数据详情</h3>
          <button class="close-btn" @click="closeModal">×</button>
        </div>
        <div class="modal-body">
          <div class="detail-grid">
            <div class="detail-item">
              <label>监测ID：</label>
              <span>{{ viewData.id }}</span>
            </div>
            <div class="detail-item">
              <label>传感器ID：</label>
              <span>{{ viewData.sensorId }}</span>
            </div>
            <div class="detail-item">
              <label>压力值：</label>
              <span>{{ viewData.pressure }} MPa</span>
            </div>
            <div class="detail-item">
              <label>温度：</label>
              <span>{{ viewData.temperature }} °C</span>
            </div>
            <div class="detail-item">
              <label>流量：</label>
              <span>{{ viewData.traffic }} m³/h</span>
            </div>
            <div class="detail-item">
              <label>震动值：</label>
              <span>{{ viewData.shake }}</span>
            </div>
            <div class="detail-item">
              <label>数据状态：</label>
              <span class="status-badge" :class="getStatusClass(viewData.dataStatus)">
                {{ getStatusText(viewData.dataStatus) }}
              </span>
            </div>
            <div class="detail-item">
              <label>区域：</label>
              <span>{{ viewData.area }}</span>
            </div>
            <div class="detail-item">
              <label>所属管道：</label>
              <span>{{ viewData.pipelineName }}</span>
            </div>
            <div class="detail-item">
              <label>监测时间：</label>
              <span>{{ viewData.createTime }}</span>
            </div>
            <div class="detail-item">
              <label>更新时间：</label>
              <span>{{ viewData.updateTime }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Monitoring',
  data() {
    return {
      // 搜索表单
      searchForm: {
        sensorId: '',
        area: '',
        status: '',
        pipelineName: ''
      },
      
      // 监测数据列表
      monitoringDataList: [
        {
          id: 'MD001',
          sensorId: 'S001',
          pressure: 2.5,
          temperature: 25.3,
          traffic: 150.2,
          shake: 0.02,
          dataStatus: '0',
          area: '华北区域',
          pipelineName: '西气东输一线',
          createTime: '2024-01-15 10:30:00',
          updateTime: '2024-01-15 10:30:00'
        },
        {
          id: 'MD002',
          sensorId: 'S002',
          pressure: 3.1,
          temperature: 28.7,
          traffic: 180.5,
          shake: 0.05,
          dataStatus: '1',
          area: '华东区域',
          pipelineName: '川气东送',
          createTime: '2024-01-15 11:15:00',
          updateTime: '2024-01-15 11:15:00'
        },
        {
          id: 'MD003',
          sensorId: 'S003',
          pressure: 4.2,
          temperature: 32.1,
          traffic: 220.8,
          shake: 0.08,
          dataStatus: '2',
          area: '华南区域',
          pipelineName: '中缅天然气管道',
          createTime: '2024-01-15 12:00:00',
          updateTime: '2024-01-15 12:00:00'
        },
        {
          id: 'MD004',
          sensorId: 'S004',
          pressure: 5.5,
          temperature: 35.8,
          traffic: 280.3,
          shake: 0.12,
          dataStatus: '3',
          area: '西北区域',
          pipelineName: '西气东输二线',
          createTime: '2024-01-15 13:45:00',
          updateTime: '2024-01-15 13:45:00'
        },
        {
          id: 'MD005',
          sensorId: 'S005',
          pressure: 2.8,
          temperature: 26.5,
          traffic: 165.7,
          shake: 0.03,
          dataStatus: '0',
          area: '西南区域',
          pipelineName: '中亚天然气管道',
          createTime: '2024-01-15 14:20:00',
          updateTime: '2024-01-15 14:20:00'
        }
      ],
      
      // 分页
      currentPage: 1,
      pageSize: 10,
      
      // 模态框状态
      showAddModal: false,
      showEditModal: false,
      showViewModal: false,
      
      // 表单数据
      formData: {
        id: '',
        sensorId: '',
        pressure: '',
        temperature: '',
        traffic: '',
        shake: '',
        dataStatus: '0',
        area: '',
        pipelineName: ''
      },
      
      // 查看数据
      viewData: {}
    }
  },
  
  computed: {
    // 筛选后的监测数据
    filteredMonitoringData() {
      let filtered = this.monitoringDataList;
      
      if (this.searchForm.sensorId) {
        filtered = filtered.filter(item => 
          item.sensorId.toLowerCase().includes(this.searchForm.sensorId.toLowerCase())
        );
      }
      
      if (this.searchForm.area) {
        filtered = filtered.filter(item => item.area === this.searchForm.area);
      }
      
      if (this.searchForm.status) {
        filtered = filtered.filter(item => item.dataStatus === this.searchForm.status);
      }
      
      if (this.searchForm.pipelineName) {
        filtered = filtered.filter(item => 
          item.pipelineName.toLowerCase().includes(this.searchForm.pipelineName.toLowerCase())
        );
      }
      
      return filtered;
    },
    
    // 分页后的监测数据
    paginatedMonitoringData() {
      const start = (this.currentPage - 1) * this.pageSize;
      const end = start + this.pageSize;
      return this.filteredMonitoringData.slice(start, end);
    },
    
    // 总页数
    totalPages() {
      return Math.ceil(this.filteredMonitoringData.length / this.pageSize);
    },
    
    // 统计数据
    totalMonitoringData() {
      return this.monitoringDataList.length;
    },
    
    normalDataCount() {
      return this.monitoringDataList.filter(item => item.dataStatus === '0' || item.dataStatus === '1').length;
    },
    
    abnormalDataCount() {
      return this.monitoringDataList.filter(item => item.dataStatus === '2' || item.dataStatus === '3').length;
    },
    
    todayDataCount() {
      const today = new Date().toISOString().split('T')[0];
      return this.monitoringDataList.filter(item => 
        item.createTime.startsWith(today.replace(/-/g, '-'))
      ).length;
    }
  },
  
  methods: {
    // 搜索监测数据
    searchMonitoringData() {
      this.currentPage = 1;
    },
    
    // 刷新数据
    refreshData() {
      // 模拟刷新数据
      console.log('刷新监测数据');
    },
    
    // 查看监测数据
    viewMonitoringData(data) {
      this.viewData = { ...data };
      this.showViewModal = true;
    },
    
    // 编辑监测数据
    editMonitoringData(data) {
      this.formData = { ...data };
      this.showEditModal = true;
    },
    
    // 删除监测数据
    deleteMonitoringData(data) {
      if (confirm(`确定要删除监测数据 ${data.id} 吗？`)) {
        const index = this.monitoringDataList.findIndex(item => item.id === data.id);
        if (index > -1) {
          this.monitoringDataList.splice(index, 1);
        }
      }
    },
    
    // 保存监测数据
    saveMonitoringData() {
      if (this.showAddModal) {
        // 添加新监测数据
        const newData = {
          ...this.formData,
          id: 'MD' + String(Date.now()).slice(-3),
          createTime: new Date().toLocaleString(),
          updateTime: new Date().toLocaleString()
        };
        this.monitoringDataList.unshift(newData);
      } else {
        // 更新监测数据
        const index = this.monitoringDataList.findIndex(item => item.id === this.formData.id);
        if (index > -1) {
          this.monitoringDataList[index] = {
            ...this.formData,
            updateTime: new Date().toLocaleString()
          };
        }
      }
      this.closeModal();
    },
    
    // 关闭模态框
    closeModal() {
      this.showAddModal = false;
      this.showEditModal = false;
      this.showViewModal = false;
      this.formData = {
        id: '',
        sensorId: '',
        pressure: '',
        temperature: '',
        traffic: '',
        shake: '',
        dataStatus: '0',
        area: '',
        pipelineName: ''
      };
    },
    
    // 获取状态样式类
    getStatusClass(status) {
      const statusMap = {
        '0': 'status-safe',
        '1': 'status-good',
        '2': 'status-danger',
        '3': 'status-critical'
      };
      return statusMap[status] || 'status-safe';
    },
    
    // 获取状态文本
    getStatusText(status) {
      const statusMap = {
        '0': '安全',
        '1': '良好',
        '2': '危险',
        '3': '高危'
      };
      return statusMap[status] || '未知';
    }
  }
}
</script>

<style scoped>
.monitoring-container {
  padding: 20px;
  background-color: #f5f5f5;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.page-title {
  margin: 0;
  color: #333;
  font-size: 24px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  text-align: center;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #2563eb;
  margin-bottom: 5px;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.filter-section {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  margin-bottom: 20px;
}

.filter-row {
  display: flex;
  gap: 20px;
  align-items: end;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  flex-direction: column;
  min-width: 150px;
}

.filter-item label {
  margin-bottom: 5px;
  font-weight: 500;
  color: #333;
}

.input-field, .select-field {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.monitoring-list {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  overflow: hidden;
}

.table-container {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.data-table th {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #333;
}

.data-table tbody tr:hover {
  background-color: #f8f9fa;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-safe {
  background-color: #dcfce7;
  color: #166534;
}

.status-good {
  background-color: #dbeafe;
  color: #1e40af;
}

.status-danger {
  background-color: #fef3c7;
  color: #92400e;
}

.status-critical {
  background-color: #fecaca;
  color: #991b1b;
}

.btn-action {
  padding: 4px 8px;
  margin: 0 2px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.btn-view {
  background-color: #3b82f6;
  color: white;
}

.btn-edit {
  background-color: #10b981;
  color: white;
}

.btn-delete {
  background-color: #ef4444;
  color: white;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background-color: #f8f9fa;
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: #666;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.btn-primary {
  background-color: #2563eb;
  color: white;
}

.btn-secondary {
  background-color: #6b7280;
  color: white;
}

.btn-search {
  background-color: #059669;
  color: white;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
}

.modal-body {
  padding: 20px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group label {
  margin-bottom: 5px;
  font-weight: 500;
  color: #333;
}

.form-input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.detail-item {
  display: flex;
  flex-direction: column;
}

.detail-item label {
  font-weight: 500;
  color: #666;
  margin-bottom: 5px;
}

.detail-item span {
  color: #333;
}

@media (max-width: 768px) {
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-item {
    min-width: auto;
  }
  
  .form-row {
    grid-template-columns: 1fr;
  }
  
  .detail-grid {
    grid-template-columns: 1fr;
  }
  
  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>