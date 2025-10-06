<template>
  <div class="maintenance-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">维护管理</h2>
      <div class="header-actions">
        <button class="btn btn-primary" @click="showAddModal = true">
          <i class="icon">+</i> 新增维护计划
        </button>
        <button class="btn btn-secondary" @click="refreshData">
          <i class="icon">↻</i> 刷新
        </button>
      </div>
    </div>

    <!-- 维护统计卡片 -->
    <div class="stats-section">
      <div class="stat-card">
        <div class="stat-number">{{ maintenanceStats.total }}</div>
        <div class="stat-label">总维护计划</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ maintenanceStats.scheduled }}</div>
        <div class="stat-label">计划中</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ maintenanceStats.inProgress }}</div>
        <div class="stat-label">执行中</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ maintenanceStats.completed }}</div>
        <div class="stat-label">已完成</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ maintenanceStats.overdue }}</div>
        <div class="stat-label">已逾期</div>
      </div>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-item">
          <label>维护项目：</label>
          <input v-model="searchForm.project" placeholder="请输入维护项目" class="input-field">
        </div>
        <div class="filter-item">
          <label>维护类型：</label>
          <select v-model="searchForm.type" class="select-field">
            <option value="">全部类型</option>
            <option value="预防性维护">预防性维护</option>
            <option value="纠正性维护">纠正性维护</option>
            <option value="改进性维护">改进性维护</option>
            <option value="应急维护">应急维护</option>
          </select>
        </div>
        <div class="filter-item">
          <label>状态：</label>
          <select v-model="searchForm.status" class="select-field">
            <option value="">全部状态</option>
            <option value="计划中">计划中</option>
            <option value="执行中">执行中</option>
            <option value="已完成">已完成</option>
            <option value="已逾期">已逾期</option>
            <option value="已取消">已取消</option>
          </select>
        </div>
        <div class="filter-item">
          <label>负责人：</label>
          <input v-model="searchForm.responsible" placeholder="请输入负责人" class="input-field">
        </div>
        <button class="btn btn-search" @click="searchMaintenance">搜索</button>
      </div>
    </div>

    <!-- 维护计划列表 -->
    <div class="maintenance-list">
      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>维护编号</th>
              <th>维护项目</th>
              <th>维护类型</th>
              <th>设备/管段</th>
              <th>状态</th>
              <th>负责人</th>
              <th>计划开始时间</th>
              <th>计划结束时间</th>
              <th>实际完成时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="maintenance in paginatedMaintenance" :key="maintenance.id">
              <td>{{ maintenance.code }}</td>
              <td>{{ maintenance.project }}</td>
              <td>{{ maintenance.type }}</td>
              <td>{{ maintenance.equipment }}</td>
              <td>
                <span class="status-badge" :class="getStatusClass(maintenance.status)">
                  {{ maintenance.status }}
                </span>
              </td>
              <td>{{ maintenance.responsible }}</td>
              <td>{{ maintenance.plannedStartTime }}</td>
              <td>{{ maintenance.plannedEndTime }}</td>
              <td>{{ maintenance.actualEndTime || '-' }}</td>
              <td>
                <button class="btn-action btn-view" @click="viewMaintenance(maintenance)">查看</button>
                <button class="btn-action btn-edit" @click="editMaintenance(maintenance)">编辑</button>
                <button class="btn-action btn-delete" @click="deleteMaintenance(maintenance)">删除</button>
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
          第 {{ currentPage }} 页，共 {{ totalPages }} 页，总计 {{ filteredMaintenance.length }} 条记录
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

    <!-- 添加/编辑维护计划模态框 -->
    <div v-if="showAddModal || showEditModal" class="modal-overlay" @click="closeModals">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ showAddModal ? '新增维护计划' : '编辑维护计划' }}</h3>
          <button class="close-btn" @click="closeModals">×</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveMaintenance">
            <div class="form-row">
              <div class="form-group">
                <label>维护编号：</label>
                <input v-model="maintenanceForm.code" required class="input-field">
              </div>
              <div class="form-group">
                <label>维护项目：</label>
                <input v-model="maintenanceForm.project" required class="input-field">
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>维护类型：</label>
                <select v-model="maintenanceForm.type" required class="select-field">
                  <option value="">请选择类型</option>
                  <option value="预防性维护">预防性维护</option>
                  <option value="纠正性维护">纠正性维护</option>
                  <option value="改进性维护">改进性维护</option>
                  <option value="应急维护">应急维护</option>
                </select>
              </div>
              <div class="form-group">
                <label>设备/管段：</label>
                <input v-model="maintenanceForm.equipment" required class="input-field">
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>负责人：</label>
                <input v-model="maintenanceForm.responsible" required class="input-field">
              </div>
              <div class="form-group">
                <label>联系电话：</label>
                <input v-model="maintenanceForm.phone" class="input-field">
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>计划开始时间：</label>
                <input v-model="maintenanceForm.plannedStartTime" type="datetime-local" required class="input-field">
              </div>
              <div class="form-group">
                <label>计划结束时间：</label>
                <input v-model="maintenanceForm.plannedEndTime" type="datetime-local" required class="input-field">
              </div>
            </div>
            <div class="form-group">
              <label>维护内容：</label>
              <textarea v-model="maintenanceForm.content" rows="4" class="input-field"></textarea>
            </div>
            <div class="form-group">
              <label>备注：</label>
              <textarea v-model="maintenanceForm.remarks" rows="3" class="input-field"></textarea>
            </div>
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="closeModals">取消</button>
              <button type="submit" class="btn btn-primary">保存</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- 查看维护详情模态框 -->
    <div v-if="showViewModal" class="modal-overlay" @click="showViewModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>维护计划详情</h3>
          <button class="close-btn" @click="showViewModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="detail-info" v-if="selectedMaintenance">
            <div class="info-row">
              <span class="label">维护编号：</span>
              <span class="value">{{ selectedMaintenance.code }}</span>
            </div>
            <div class="info-row">
              <span class="label">维护项目：</span>
              <span class="value">{{ selectedMaintenance.project }}</span>
            </div>
            <div class="info-row">
              <span class="label">维护类型：</span>
              <span class="value">{{ selectedMaintenance.type }}</span>
            </div>
            <div class="info-row">
              <span class="label">设备/管段：</span>
              <span class="value">{{ selectedMaintenance.equipment }}</span>
            </div>
            <div class="info-row">
              <span class="label">状态：</span>
              <span class="value status-badge" :class="getStatusClass(selectedMaintenance.status)">
                {{ selectedMaintenance.status }}
              </span>
            </div>
            <div class="info-row">
              <span class="label">负责人：</span>
              <span class="value">{{ selectedMaintenance.responsible }}</span>
            </div>
            <div class="info-row">
              <span class="label">联系电话：</span>
              <span class="value">{{ selectedMaintenance.phone }}</span>
            </div>
            <div class="info-row">
              <span class="label">计划开始时间：</span>
              <span class="value">{{ selectedMaintenance.plannedStartTime }}</span>
            </div>
            <div class="info-row">
              <span class="label">计划结束时间：</span>
              <span class="value">{{ selectedMaintenance.plannedEndTime }}</span>
            </div>
            <div class="info-row">
              <span class="label">实际完成时间：</span>
              <span class="value">{{ selectedMaintenance.actualEndTime || '未完成' }}</span>
            </div>
            <div class="info-row">
              <span class="label">维护内容：</span>
              <span class="value">{{ selectedMaintenance.content }}</span>
            </div>
            <div class="info-row">
              <span class="label">备注：</span>
              <span class="value">{{ selectedMaintenance.remarks || '无' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Maintenance',
  data() {
    return {
      searchForm: {
        project: '',
        type: '',
        status: '',
        responsible: ''
      },
      maintenanceList: [
        {
          id: 1,
          code: 'MAINT001',
          project: '管道段A-001定期检修',
          type: '预防性维护',
          equipment: '管道段A-001',
          status: '计划中',
          responsible: '张工程师',
          phone: '13800138001',
          plannedStartTime: '2024-01-20 08:00',
          plannedEndTime: '2024-01-20 18:00',
          actualEndTime: '',
          content: '对管道段A-001进行全面检修，包括压力测试、腐蚀检查、阀门维护等。',
          remarks: '需要停气4小时'
        },
        {
          id: 2,
          code: 'MAINT002',
          project: '压力传感器校准',
          type: '预防性维护',
          equipment: '压力传感器PS-001',
          status: '执行中',
          responsible: '李技术员',
          phone: '13800138002',
          plannedStartTime: '2024-01-18 09:00',
          plannedEndTime: '2024-01-18 17:00',
          actualEndTime: '',
          content: '对所有压力传感器进行校准和精度检查。',
          remarks: ''
        },
        {
          id: 3,
          code: 'MAINT003',
          project: '阀门更换维修',
          type: '纠正性维护',
          equipment: '调节阀V-003',
          status: '已完成',
          responsible: '王师傅',
          phone: '13800138003',
          plannedStartTime: '2024-01-15 10:00',
          plannedEndTime: '2024-01-15 16:00',
          actualEndTime: '2024-01-15 15:30',
          content: '更换损坏的调节阀V-003，并进行功能测试。',
          remarks: '已更换为新型号阀门'
        },
        {
          id: 4,
          code: 'MAINT004',
          project: '紧急泄漏修复',
          type: '应急维护',
          equipment: '管道段B-005',
          status: '已逾期',
          responsible: '赵队长',
          phone: '13800138004',
          plannedStartTime: '2024-01-10 14:00',
          plannedEndTime: '2024-01-10 20:00',
          actualEndTime: '',
          content: '修复管道段B-005的紧急泄漏问题。',
          remarks: '需要紧急处理'
        },
        {
          id: 5,
          code: 'MAINT005',
          project: '监控系统升级',
          type: '改进性维护',
          equipment: '监控系统',
          status: '计划中',
          responsible: '陈工程师',
          phone: '13800138005',
          plannedStartTime: '2024-01-25 09:00',
          plannedEndTime: '2024-01-26 18:00',
          actualEndTime: '',
          content: '升级监控系统软件和硬件，提高监控精度。',
          remarks: '需要停机维护'
        }
      ],
      currentPage: 1,
      pageSize: 10,
      showAddModal: false,
      showEditModal: false,
      showViewModal: false,
      selectedMaintenance: null,
      maintenanceForm: {
        code: '',
        project: '',
        type: '',
        equipment: '',
        responsible: '',
        phone: '',
        plannedStartTime: '',
        plannedEndTime: '',
        content: '',
        remarks: ''
      }
    }
  },
  computed: {
    maintenanceStats() {
      const total = this.maintenanceList.length
      const scheduled = this.maintenanceList.filter(item => item.status === '计划中').length
      const inProgress = this.maintenanceList.filter(item => item.status === '执行中').length
      const completed = this.maintenanceList.filter(item => item.status === '已完成').length
      const overdue = this.maintenanceList.filter(item => item.status === '已逾期').length
      return { total, scheduled, inProgress, completed, overdue }
    },
    filteredMaintenance() {
      return this.maintenanceList.filter(item => {
        return (!this.searchForm.project || item.project.includes(this.searchForm.project)) &&
               (!this.searchForm.type || item.type === this.searchForm.type) &&
               (!this.searchForm.status || item.status === this.searchForm.status) &&
               (!this.searchForm.responsible || item.responsible.includes(this.searchForm.responsible))
      })
    },
    paginatedMaintenance() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredMaintenance.slice(start, end)
    },
    totalPages() {
      return Math.ceil(this.filteredMaintenance.length / this.pageSize)
    }
  },
  methods: {
    searchMaintenance() {
      this.currentPage = 1
    },
    refreshData() {
      console.log('刷新维护数据')
    },
    viewMaintenance(maintenance) {
      this.selectedMaintenance = maintenance
      this.showViewModal = true
    },
    editMaintenance(maintenance) {
      this.maintenanceForm = { ...maintenance }
      this.showEditModal = true
    },
    deleteMaintenance(maintenance) {
      if (confirm(`确定要删除维护计划 "${maintenance.project}" 吗？`)) {
        const index = this.maintenanceList.findIndex(item => item.id === maintenance.id)
        if (index > -1) {
          this.maintenanceList.splice(index, 1)
        }
      }
    },
    saveMaintenance() {
      if (this.showAddModal) {
        const newMaintenance = {
          ...this.maintenanceForm,
          id: Date.now(),
          status: '计划中',
          actualEndTime: ''
        }
        this.maintenanceList.push(newMaintenance)
      } else if (this.showEditModal) {
        const index = this.maintenanceList.findIndex(item => item.id === this.maintenanceForm.id)
        if (index > -1) {
          this.maintenanceList.splice(index, 1, { ...this.maintenanceForm })
        }
      }
      this.closeModals()
    },
    closeModals() {
      this.showAddModal = false
      this.showEditModal = false
      this.showViewModal = false
      this.maintenanceForm = {
        code: '',
        project: '',
        type: '',
        equipment: '',
        responsible: '',
        phone: '',
        plannedStartTime: '',
        plannedEndTime: '',
        content: '',
        remarks: ''
      }
    },
    getStatusClass(status) {
      const statusMap = {
        '计划中': 'status-scheduled',
        '执行中': 'status-progress',
        '已完成': 'status-completed',
        '已逾期': 'status-overdue',
        '已取消': 'status-cancelled'
      }
      return statusMap[status] || 'status-scheduled'
    }
  }
}
</script>

<style scoped>
.maintenance-container {
  padding: 20px;
  background-color: #f5f5f5;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
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
  color: #1E3A8A;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #6B7280;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 5px;
  transition: all 0.3s ease;
}

.btn-primary {
  background-color: #1E3A8A;
  color: white;
}

.btn-primary:hover {
  background-color: #0F2C6B;
}

.btn-secondary {
  background-color: #6B7280;
  color: white;
}

.btn-secondary:hover {
  background-color: #4B5563;
}

.btn-search {
  background-color: #059669;
  color: white;
}

.btn-search:hover {
  background-color: #047857;
}

.filter-section {
  background: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
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
  gap: 5px;
}

.filter-item label {
  font-weight: 500;
  color: #374151;
}

.input-field, .select-field {
  padding: 8px 12px;
  border: 1px solid #D1D5DB;
  border-radius: 4px;
  font-size: 14px;
  min-width: 150px;
}

.input-field:focus, .select-field:focus {
  outline: none;
  border-color: #1E3A8A;
  box-shadow: 0 0 0 2px rgba(30, 58, 138, 0.1);
}

textarea.input-field {
  resize: vertical;
  min-height: 80px;
}

.maintenance-list {
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
  border-bottom: 1px solid #E5E7EB;
}

.data-table th {
  background-color: #F9FAFB;
  font-weight: 600;
  color: #374151;
}

.data-table tr:hover {
  background-color: #F9FAFB;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-scheduled {
  background-color: #E0E7FF;
  color: #3730A3;
}

.status-progress {
  background-color: #DBEAFE;
  color: #1E40AF;
}

.status-completed {
  background-color: #D1FAE5;
  color: #065F46;
}

.status-overdue {
  background-color: #FEE2E2;
  color: #991B1B;
}

.status-cancelled {
  background-color: #F3F4F6;
  color: #6B7280;
}

.btn-action {
  padding: 4px 8px;
  margin: 0 2px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s ease;
}

.btn-view {
  background-color: #3B82F6;
  color: white;
}

.btn-edit {
  background-color: #F59E0B;
  color: white;
}

.btn-delete {
  background-color: #EF4444;
  color: white;
}

.btn-action:hover {
  opacity: 0.8;
  transform: scale(1.05);
}

.pagination {
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #E5E7EB;
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid #D1D5DB;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.page-btn:hover:not(:disabled) {
  background-color: #F3F4F6;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: #6B7280;
  font-size: 14px;
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
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  padding: 20px;
  border-bottom: 1px solid #E5E7EB;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  color: #374151;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #6B7280;
}

.modal-body {
  padding: 20px;
}

.form-row {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
}

.form-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.form-group label {
  font-weight: 500;
  color: #374151;
}

.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #E5E7EB;
}

.detail-info {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.info-row {
  display: flex;
  align-items: center;
}

.info-row .label {
  font-weight: 500;
  color: #374151;
  min-width: 120px;
}

.info-row .value {
  color: #6B7280;
}

.icon {
  font-style: normal;
}

@media (max-width: 768px) {
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .form-row {
    flex-direction: column;
  }
  
  .header-actions {
    flex-direction: column;
  }
  
  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>