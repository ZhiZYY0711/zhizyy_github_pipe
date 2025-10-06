<template>
  <div class="simulation-drill">
    <!-- 页面头部 - 发布模拟演练按钮 -->
    <div class="header-section">
      <button class="publish-btn" @click="showPublishModal = true">
        发布模拟演练
      </button>
    </div>

    <!-- 正在演习中的记录 -->
    <div class="ongoing-drills" v-if="ongoingDrills.length > 0">
      <h3>正在演习中</h3>
      <div class="drill-cards">
        <div 
          v-for="drill in ongoingDrills" 
          :key="drill.id"
          class="drill-card"
          @click="showDrillDetails(drill)"
        >
          <div class="card-header">
            <span class="area-name">{{ drill.area }}</span>
            <span class="status">{{ drill.status }}</span>
          </div>
          <div class="card-content">
            <p><strong>开始时间:</strong> {{ formatDateTime(drill.startTime) }}</p>
            <p><strong>预计结束:</strong> {{ formatDateTime(drill.estimatedEndTime) }}</p>
            <p><strong>参演人员:</strong> {{ drill.participants.join(', ') }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 历史记录表格 -->
    <div class="history-section">
      <h3>演习历史记录</h3>
      <table class="history-table">
        <thead>
          <tr>
            <th>编号</th>
            <th>区域</th>
            <th>开始时间</th>
            <th>结束时间</th>
            <th>演练结果</th>
            <th>参演人员</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="record in paginatedHistory" :key="record.id">
            <td>{{ record.id }}</td>
            <td>{{ record.area }}</td>
            <td>{{ formatDateTime(record.startTime) }}</td>
            <td>{{ formatDateTime(record.endTime) }}</td>
            <td>
              <span :class="['result-badge', record.result.toLowerCase()]">
                {{ record.result }}
              </span>
            </td>
            <td>{{ record.participants.join(', ') }}</td>
            <td>
              <button class="action-btn detail-btn" @click="showDetails(record)">详情</button>
              <button class="action-btn edit-btn" @click="editRecord(record)">编辑</button>
            </td>
          </tr>
        </tbody>
      </table>
      
      <!-- 分页 -->
      <div class="pagination">
        <button 
          @click="currentPage--" 
          :disabled="currentPage === 1"
          class="page-btn"
        >
          上一页
        </button>
        <span class="page-info">
          第 {{ currentPage }} 页，共 {{ totalPages }} 页
        </span>
        <button 
          @click="currentPage++" 
          :disabled="currentPage === totalPages"
          class="page-btn"
        >
          下一页
        </button>
      </div>
    </div>

    <!-- 发布演练模态框 -->
    <div v-if="showPublishModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>发布模拟演练</h3>
          <button class="close-btn" @click="closeModal">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>选择区域管道:</label>
            <select v-model="newDrill.area" class="form-control">
              <option value="">请选择区域</option>
              <option v-for="area in areas" :key="area" :value="area">{{ area }}</option>
            </select>
          </div>
          
          <div class="form-group">
            <label>演练类型:</label>
            <div class="radio-group">
              <label class="radio-label">
                <input type="radio" v-model="newDrill.type" value="状态模拟" />
                模拟状态
              </label>
              <label class="radio-label">
                <input type="radio" v-model="newDrill.type" value="事故模拟" />
                模拟事故
              </label>
            </div>
          </div>
          
          <div class="form-group">
            <label>选择参演检修员:</label>
            <div class="checkbox-group">
              <label v-for="worker in workers" :key="worker" class="checkbox-label">
                <input 
                  type="checkbox" 
                  :value="worker" 
                  v-model="newDrill.participants"
                />
                {{ worker }}
              </label>
            </div>
          </div>
          
          <div class="form-group">
            <label>模拟开始时间:</label>
            <input 
              type="datetime-local" 
              v-model="newDrill.startTime" 
              class="form-control"
            />
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeModal">取消</button>
          <button class="confirm-btn" @click="startDrill">模拟开始</button>
        </div>
      </div>
    </div>

    <!-- 演练详情模态框 -->
    <div v-if="showDetailsModal" class="modal-overlay" @click="closeDetailsModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>演练详情</h3>
          <button class="close-btn" @click="closeDetailsModal">&times;</button>
        </div>
        <div class="modal-body">
          <div class="detail-item">
            <strong>编号:</strong> {{ selectedDrill.id }}
          </div>
          <div class="detail-item">
            <strong>区域:</strong> {{ selectedDrill.area }}
          </div>
          <div class="detail-item">
            <strong>演练类型:</strong> {{ selectedDrill.type }}
          </div>
          <div class="detail-item">
            <strong>开始时间:</strong> {{ formatDateTime(selectedDrill.startTime) }}
          </div>
          <div class="detail-item">
            <strong>结束时间:</strong> {{ formatDateTime(selectedDrill.endTime) }}
          </div>
          <div class="detail-item">
            <strong>演练结果:</strong> 
            <span :class="['result-badge', selectedDrill.result?.toLowerCase()]">
              {{ selectedDrill.result }}
            </span>
          </div>
          <div class="detail-item">
            <strong>参演人员:</strong> {{ selectedDrill.participants?.join(', ') }}
          </div>
          <div class="detail-item" v-if="selectedDrill.description">
            <strong>演练描述:</strong> {{ selectedDrill.description }}
          </div>
        </div>
        <div class="modal-footer">
          <button class="confirm-btn" @click="closeDetailsModal">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'SimulationDrill',
  data() {
    return {
      showPublishModal: false,
      showDetailsModal: false,
      selectedDrill: {},
      currentPage: 1,
      pageSize: 10,
      
      // 新演练表单数据
      newDrill: {
        area: '',
        type: '',
        participants: [],
        startTime: ''
      },
      
      // 可选数据
      areas: ['北京区域', '上海区域', '广州区域', '深圳区域', '成都区域'],
      workers: ['张三', '李四', '王五', '赵六', '钱七', '孙八'],
      
      // 正在进行的演练
      ongoingDrills: [
        {
          id: 1,
          area: '北京区域',
          status: '状态模拟',
          startTime: new Date('2024-01-15 09:00:00'),
          estimatedEndTime: new Date('2024-01-15 12:00:00'),
          participants: ['张三', '李四']
        },
        {
          id: 2,
          area: '上海区域', 
          status: '事故模拟',
          startTime: new Date('2024-01-15 10:30:00'),
          estimatedEndTime: new Date('2024-01-15 14:30:00'),
          participants: ['王五', '赵六']
        }
      ],
      
      // 历史记录
      historyRecords: [
        {
          id: 'D001',
          area: '北京区域',
          startTime: new Date('2024-01-10 09:00:00'),
          endTime: new Date('2024-01-10 12:00:00'),
          result: '成功',
          participants: ['张三', '李四'],
          type: '状态模拟',
          description: '管道压力监测模拟演练'
        },
        {
          id: 'D002',
          area: '上海区域',
          startTime: new Date('2024-01-08 14:00:00'),
          endTime: new Date('2024-01-08 17:00:00'),
          result: '失败',
          participants: ['王五', '赵六'],
          type: '事故模拟',
          description: '管道泄漏应急处理演练'
        },
        {
          id: 'D003',
          area: '广州区域',
          startTime: new Date('2024-01-05 08:30:00'),
          endTime: new Date('2024-01-05 11:30:00'),
          result: '成功',
          participants: ['钱七', '孙八'],
          type: '状态模拟',
          description: '设备维护流程演练'
        }
      ]
    }
  },
  
  computed: {
    totalPages() {
      return Math.ceil(this.historyRecords.length / this.pageSize)
    },
    
    paginatedHistory() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.historyRecords.slice(start, end)
    }
  },
  
  methods: {
    // 显示发布演练模态框
    showPublishModal() {
      this.showPublishModal = true
    },
    
    // 关闭发布演练模态框
    closeModal() {
      this.showPublishModal = false
      this.resetForm()
    },
    
    // 重置表单
    resetForm() {
      this.newDrill = {
        area: '',
        type: '',
        participants: [],
        startTime: ''
      }
    },
    
    // 开始演练
    startDrill() {
      if (!this.newDrill.area || !this.newDrill.type || 
          this.newDrill.participants.length === 0 || !this.newDrill.startTime) {
        alert('请填写完整信息')
        return
      }
      
      // 添加到正在进行的演练
      const drill = {
        id: this.ongoingDrills.length + 1,
        area: this.newDrill.area,
        status: this.newDrill.type,
        startTime: new Date(this.newDrill.startTime),
        estimatedEndTime: new Date(new Date(this.newDrill.startTime).getTime() + 3 * 60 * 60 * 1000), // 预计3小时后结束
        participants: [...this.newDrill.participants]
      }
      
      this.ongoingDrills.push(drill)
      this.closeModal()
      alert('演练已成功发布！')
    },
    
    // 显示演练详情
    showDrillDetails(drill) {
      this.selectedDrill = drill
      this.showDetailsModal = true
    },
    
    // 显示历史记录详情
    showDetails(record) {
      this.selectedDrill = record
      this.showDetailsModal = true
    },
    
    // 关闭详情模态框
    closeDetailsModal() {
      this.showDetailsModal = false
      this.selectedDrill = {}
    },
    
    // 编辑记录
    editRecord(record) {
      alert(`编辑记录 ${record.id} 的功能待实现`)
    },
    
    // 格式化日期时间
    formatDateTime(date) {
      if (!date) return '-'
      return new Date(date).toLocaleString('zh-CN')
    }
  }
}
</script>

<style scoped>
.simulation-drill {
  padding: 20px;
  max-height: 100vh;
  overflow: hidden;
}

/* 头部区域 */
.header-section {
  margin-bottom: 20px;
}

.publish-btn {
  background-color: #007bff;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

.publish-btn:hover {
  background-color: #0056b3;
}

/* 正在演习区域 */
.ongoing-drills {
  margin-bottom: 30px;
}

.ongoing-drills h3 {
  margin-bottom: 15px;
  color: #333;
}

.drill-cards {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.drill-card {
  background: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  padding: 15px;
  min-width: 300px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.drill-card:hover {
  background: #e9ecef;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.area-name {
  font-weight: bold;
  color: #333;
}

.status {
  background: #28a745;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.card-content p {
  margin: 5px 0;
  font-size: 14px;
  color: #666;
}

/* 历史记录区域 */
.history-section h3 {
  margin-bottom: 15px;
  color: #333;
}

.history-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 20px;
}

.history-table th,
.history-table td {
  border: 1px solid #dee2e6;
  padding: 8px 12px;
  text-align: left;
}

.history-table th {
  background-color: #f8f9fa;
  font-weight: bold;
}

.history-table tbody tr:hover {
  background-color: #f5f5f5;
}

.result-badge {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  color: white;
}

.result-badge.成功 {
  background-color: #28a745;
}

.result-badge.失败 {
  background-color: #dc3545;
}

.action-btn {
  padding: 4px 8px;
  margin: 0 2px;
  border: none;
  border-radius: 3px;
  cursor: pointer;
  font-size: 12px;
}

.detail-btn {
  background-color: #17a2b8;
  color: white;
}

.edit-btn {
  background-color: #ffc107;
  color: #212529;
}

.action-btn:hover {
  opacity: 0.8;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid #dee2e6;
  background: white;
  cursor: pointer;
  border-radius: 4px;
}

.page-btn:hover:not(:disabled) {
  background-color: #f8f9fa;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: #666;
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
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
  max-width: 500px;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #dee2e6;
}

.modal-header h3 {
  margin: 0;
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

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}

.form-control {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  box-sizing: border-box;
}

.radio-group,
.checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.radio-label,
.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: normal;
}

.modal-footer {
  padding: 15px 20px;
  border-top: 1px solid #dee2e6;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.cancel-btn {
  padding: 8px 16px;
  border: 1px solid #dee2e6;
  background: white;
  cursor: pointer;
  border-radius: 4px;
}

.confirm-btn {
  padding: 8px 16px;
  background-color: #007bff;
  color: white;
  border: none;
  cursor: pointer;
  border-radius: 4px;
}

.confirm-btn:hover {
  background-color: #0056b3;
}

.cancel-btn:hover {
  background-color: #f8f9fa;
}

/* 详情项样式 */
.detail-item {
  margin-bottom: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-item:last-child {
  border-bottom: none;
}
</style>