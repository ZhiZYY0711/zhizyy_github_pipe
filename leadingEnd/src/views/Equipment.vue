<template>
  <div class="equipment-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">设备信息管理</h2>
      <div class="header-actions">
        <button class="btn btn-primary" @click="showAddModal = true">
          <i class="icon">+</i> 添加设备
        </button>
        <button class="btn btn-secondary" @click="refreshData">
          <i class="icon">↻</i> 刷新
        </button>
      </div>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-item">
          <label>设备名称：</label>
          <input v-model="searchForm.name" placeholder="请输入设备名称" class="input-field">
        </div>
        <div class="filter-item">
          <label>设备类型：</label>
          <select v-model="searchForm.type" class="select-field">
            <option value="">全部类型</option>
            <option value="传感器">传感器</option>
            <option value="监控设备">监控设备</option>
            <option value="检测仪器">检测仪器</option>
            <option value="通信设备">通信设备</option>
          </select>
        </div>
        <div class="filter-item">
          <label>设备状态：</label>
          <select v-model="searchForm.status" class="select-field">
            <option value="">全部状态</option>
            <option value="正常">正常</option>
            <option value="故障">故障</option>
            <option value="维护中">维护中</option>
            <option value="离线">离线</option>
          </select>
        </div>
        <button class="btn btn-search" @click="searchEquipment">搜索</button>
      </div>
    </div>

    <!-- 设备列表 -->
    <div class="equipment-list">
      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>设备编号</th>
              <th>设备名称</th>
              <th>设备类型</th>
              <th>安装位置</th>
              <th>设备状态</th>
              <th>最后检查时间</th>
              <th>负责人</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="equipment in paginatedEquipment" :key="equipment.id">
              <td>{{ equipment.code }}</td>
              <td>{{ equipment.name }}</td>
              <td>{{ equipment.type }}</td>
              <td>{{ equipment.location }}</td>
              <td>
                <span class="status-badge" :class="getStatusClass(equipment.status)">
                  {{ equipment.status }}
                </span>
              </td>
              <td>{{ equipment.lastCheck }}</td>
              <td>{{ equipment.responsible }}</td>
              <td>
                <button class="btn-action btn-view" @click="viewEquipment(equipment)">查看</button>
                <button class="btn-action btn-edit" @click="editEquipment(equipment)">编辑</button>
                <button class="btn-action btn-delete" @click="deleteEquipment(equipment)">删除</button>
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
          第 {{ currentPage }} 页，共 {{ totalPages }} 页，总计 {{ filteredEquipment.length }} 条记录
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

    <!-- 添加/编辑设备模态框 -->
    <div v-if="showAddModal || showEditModal" class="modal-overlay" @click="closeModals">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ showAddModal ? '添加设备' : '编辑设备' }}</h3>
          <button class="close-btn" @click="closeModals">×</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveEquipment">
            <div class="form-row">
              <div class="form-group">
                <label>设备编号：</label>
                <input v-model="equipmentForm.code" required class="input-field">
              </div>
              <div class="form-group">
                <label>设备名称：</label>
                <input v-model="equipmentForm.name" required class="input-field">
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>设备类型：</label>
                <select v-model="equipmentForm.type" required class="select-field">
                  <option value="">请选择类型</option>
                  <option value="传感器">传感器</option>
                  <option value="监控设备">监控设备</option>
                  <option value="检测仪器">检测仪器</option>
                  <option value="通信设备">通信设备</option>
                </select>
              </div>
              <div class="form-group">
                <label>安装位置：</label>
                <input v-model="equipmentForm.location" required class="input-field">
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>设备状态：</label>
                <select v-model="equipmentForm.status" required class="select-field">
                  <option value="正常">正常</option>
                  <option value="故障">故障</option>
                  <option value="维护中">维护中</option>
                  <option value="离线">离线</option>
                </select>
              </div>
              <div class="form-group">
                <label>负责人：</label>
                <input v-model="equipmentForm.responsible" required class="input-field">
              </div>
            </div>
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="closeModals">取消</button>
              <button type="submit" class="btn btn-primary">保存</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- 查看设备详情模态框 -->
    <div v-if="showViewModal" class="modal-overlay" @click="showViewModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>设备详情</h3>
          <button class="close-btn" @click="showViewModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="detail-info" v-if="selectedEquipment">
            <div class="info-row">
              <span class="label">设备编号：</span>
              <span class="value">{{ selectedEquipment.code }}</span>
            </div>
            <div class="info-row">
              <span class="label">设备名称：</span>
              <span class="value">{{ selectedEquipment.name }}</span>
            </div>
            <div class="info-row">
              <span class="label">设备类型：</span>
              <span class="value">{{ selectedEquipment.type }}</span>
            </div>
            <div class="info-row">
              <span class="label">安装位置：</span>
              <span class="value">{{ selectedEquipment.location }}</span>
            </div>
            <div class="info-row">
              <span class="label">设备状态：</span>
              <span class="value status-badge" :class="getStatusClass(selectedEquipment.status)">
                {{ selectedEquipment.status }}
              </span>
            </div>
            <div class="info-row">
              <span class="label">最后检查时间：</span>
              <span class="value">{{ selectedEquipment.lastCheck }}</span>
            </div>
            <div class="info-row">
              <span class="label">负责人：</span>
              <span class="value">{{ selectedEquipment.responsible }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Equipment',
  data() {
    return {
      searchForm: {
        name: '',
        type: '',
        status: ''
      },
      equipmentList: [
        {
          id: 1,
          code: 'EQ001',
          name: '压力传感器A1',
          type: '传感器',
          location: '管道段A-001',
          status: '正常',
          lastCheck: '2024-01-15 14:30',
          responsible: '张工程师'
        },
        {
          id: 2,
          code: 'EQ002',
          name: '温度监控设备B2',
          type: '监控设备',
          location: '管道段B-002',
          status: '维护中',
          lastCheck: '2024-01-14 09:15',
          responsible: '李技术员'
        },
        {
          id: 3,
          code: 'EQ003',
          name: '流量检测仪C3',
          type: '检测仪器',
          location: '管道段C-003',
          status: '正常',
          lastCheck: '2024-01-16 11:20',
          responsible: '王工程师'
        },
        {
          id: 4,
          code: 'EQ004',
          name: '通信基站D4',
          type: '通信设备',
          location: '控制中心',
          status: '故障',
          lastCheck: '2024-01-13 16:45',
          responsible: '赵技术员'
        },
        {
          id: 5,
          code: 'EQ005',
          name: '振动传感器E5',
          type: '传感器',
          location: '管道段E-005',
          status: '离线',
          lastCheck: '2024-01-12 08:30',
          responsible: '陈工程师'
        }
      ],
      currentPage: 1,
      pageSize: 10,
      showAddModal: false,
      showEditModal: false,
      showViewModal: false,
      selectedEquipment: null,
      equipmentForm: {
        code: '',
        name: '',
        type: '',
        location: '',
        status: '正常',
        responsible: ''
      }
    }
  },
  computed: {
    filteredEquipment() {
      return this.equipmentList.filter(equipment => {
        return (!this.searchForm.name || equipment.name.includes(this.searchForm.name)) &&
               (!this.searchForm.type || equipment.type === this.searchForm.type) &&
               (!this.searchForm.status || equipment.status === this.searchForm.status)
      })
    },
    paginatedEquipment() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredEquipment.slice(start, end)
    },
    totalPages() {
      return Math.ceil(this.filteredEquipment.length / this.pageSize)
    }
  },
  methods: {
    searchEquipment() {
      this.currentPage = 1
    },
    refreshData() {
      // 模拟刷新数据
      console.log('刷新设备数据')
    },
    viewEquipment(equipment) {
      this.selectedEquipment = equipment
      this.showViewModal = true
    },
    editEquipment(equipment) {
      this.equipmentForm = { ...equipment }
      this.showEditModal = true
    },
    deleteEquipment(equipment) {
      if (confirm(`确定要删除设备 "${equipment.name}" 吗？`)) {
        const index = this.equipmentList.findIndex(item => item.id === equipment.id)
        if (index > -1) {
          this.equipmentList.splice(index, 1)
        }
      }
    },
    saveEquipment() {
      if (this.showAddModal) {
        // 添加新设备
        const newEquipment = {
          ...this.equipmentForm,
          id: Date.now(),
          lastCheck: new Date().toLocaleString()
        }
        this.equipmentList.push(newEquipment)
      } else if (this.showEditModal) {
        // 编辑设备
        const index = this.equipmentList.findIndex(item => item.id === this.equipmentForm.id)
        if (index > -1) {
          this.equipmentList.splice(index, 1, { ...this.equipmentForm })
        }
      }
      this.closeModals()
    },
    closeModals() {
      this.showAddModal = false
      this.showEditModal = false
      this.showViewModal = false
      this.equipmentForm = {
        code: '',
        name: '',
        type: '',
        location: '',
        status: '正常',
        responsible: ''
      }
    },
    getStatusClass(status) {
      const statusMap = {
        '正常': 'status-normal',
        '故障': 'status-error',
        '维护中': 'status-warning',
        '离线': 'status-offline'
      }
      return statusMap[status] || 'status-normal'
    }
  }
}
</script>

<style scoped>
.equipment-container {
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

.equipment-list {
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

.status-normal {
  background-color: #D1FAE5;
  color: #065F46;
}

.status-error {
  background-color: #FEE2E2;
  color: #991B1B;
}

.status-warning {
  background-color: #FEF3C7;
  color: #92400E;
}

.status-offline {
  background-color: #E5E7EB;
  color: #374151;
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
  max-width: 600px;
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
}
</style>