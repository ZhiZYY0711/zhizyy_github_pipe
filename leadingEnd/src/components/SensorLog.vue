<template>
  <div class="sensor-log">
    <!-- 搜索区域 -->
    <div class="search-section">
      <div class="search-row">
        <div class="search-item">
          <label>日志ID:</label>
          <input v-model="searchForm.id" placeholder="请输入日志ID" />
        </div>
        <div class="search-item">
          <label>传感器ID:</label>
          <input v-model="searchForm.sensorId" placeholder="请输入传感器ID" />
        </div>
        <div class="search-item">
          <label>传感器状态:</label>
          <select v-model="searchForm.status">
            <option value="">全部状态</option>
            <option value="0">正常</option>
            <option value="1">异常</option>
            <option value="2">离线</option>
          </select>
        </div>
        <div class="search-item">
          <label>检修员ID:</label>
          <input v-model="searchForm.repairmanId" placeholder="请输入检修员ID" />
        </div>
      </div>
      <div class="search-row">
        <div class="search-item">
          <label>检修时间:</label>
          <input v-model="searchForm.overhaulStartDate" type="date" />
          <span>-</span>
          <input v-model="searchForm.overhaulEndDate" type="date" />
        </div>
        <div class="search-item">
          <label>创建时间:</label>
          <input v-model="searchForm.createStartDate" type="date" />
          <span>-</span>
          <input v-model="searchForm.createEndDate" type="date" />
        </div>
        <div class="search-item">
          <label>模糊搜索:</label>
          <input v-model="searchForm.keyword" placeholder="传感器ID、检修员ID等" />
        </div>
      </div>
      <div class="search-actions">
        <button class="search-btn" @click="searchData">🔍 搜索</button>
        <button class="reset-btn" @click="resetSearch">🔄 重置</button>
        <button class="add-btn" @click="showAddModal">➕ 新增日志</button>
        <button class="export-btn" @click="exportData">📊 导出Excel</button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-section">
      <table class="data-table">
        <thead>
          <tr>
            <th @click="sortBy('id')" class="sortable">
              日志ID <span class="sort-icon">{{ getSortIcon('id') }}</span>
            </th>
            <th @click="sortBy('sensorId')" class="sortable">
              传感器ID <span class="sort-icon">{{ getSortIcon('sensorId') }}</span>
            </th>
            <th @click="sortBy('status')" class="sortable">
              传感器状态 <span class="sort-icon">{{ getSortIcon('status') }}</span>
            </th>
            <th @click="sortBy('overhaulTime')" class="sortable">
              检修时间 <span class="sort-icon">{{ getSortIcon('overhaulTime') }}</span>
            </th>
            <th @click="sortBy('repairmanId')" class="sortable">
              检修员ID <span class="sort-icon">{{ getSortIcon('repairmanId') }}</span>
            </th>
            <th @click="sortBy('createTime')" class="sortable">
              创建时间 <span class="sort-icon">{{ getSortIcon('createTime') }}</span>
            </th>
            <th @click="sortBy('updateTime')" class="sortable">
              更新时间 <span class="sort-icon">{{ getSortIcon('updateTime') }}</span>
            </th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, index) in paginatedData" :key="item.id" :class="{ 'even-row': index % 2 === 1 }">
            <td>{{ item.id }}</td>
            <td>{{ item.sensorId }}</td>
            <td>
              <span :class="'status-' + item.status">
                {{ getStatusText(item.status) }}
              </span>
            </td>
            <td>{{ formatDateTime(item.overhaulTime) || '未检修' }}</td>
            <td>{{ item.repairmanId || '无' }}</td>
            <td>{{ formatDateTime(item.createTime) }}</td>
            <td>{{ formatDateTime(item.updateTime) }}</td>
            <td class="actions">
              <button class="action-btn view-btn" @click="viewDetails(item)">📋 详情</button>
              <button class="action-btn edit-btn" @click="editItem(item)">✏️ 修改</button>
              <button class="action-btn maintenance-btn" @click="scheduleMaintenance(item)">🔧 安排检修</button>
              <button class="action-btn delete-btn" @click="deleteItem(item)">🗑️ 删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div class="pagination">
      <div class="pagination-info">
        共 {{ filteredData.length }} 条记录，每页显示 {{ pageSize }} 条
      </div>
      <div class="pagination-controls">
        <button @click="prevPage" :disabled="currentPage === 1">上一页</button>
        <span class="page-numbers">
          <button 
            v-for="page in visiblePages" 
            :key="page"
            @click="goToPage(page)"
            :class="{ 'active': page === currentPage }"
          >
            {{ page }}
          </button>
        </span>
        <button @click="nextPage" :disabled="currentPage === totalPages">下一页</button>
      </div>
    </div>

    <!-- 新增/编辑模态框 -->
    <div v-if="showModal" class="modal-overlay" @click="closeModal">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h3>{{ isEdit ? '编辑' : '新增' }}传感器日志</h3>
          <button class="close-btn" @click="closeModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-item">
              <label>传感器ID *:</label>
              <input v-model="formData.sensorId" placeholder="请输入传感器ID" type="number" />
            </div>
            <div class="form-item">
              <label>传感器状态 *:</label>
              <select v-model="formData.status">
                <option value="">请选择状态</option>
                <option value="0">正常</option>
                <option value="1">异常</option>
                <option value="2">离线</option>
              </select>
            </div>
          </div>
          <div class="form-row">
            <div class="form-item">
              <label>检修时间:</label>
              <input v-model="formData.overhaulTime" type="datetime-local" />
            </div>
            <div class="form-item">
              <label>检修员ID:</label>
              <input v-model="formData.repairmanId" placeholder="请输入检修员ID" type="number" />
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeModal">取消</button>
          <button class="confirm-btn" @click="saveData">{{ isEdit ? '更新' : '保存' }}</button>
        </div>
      </div>
    </div>

    <!-- 详情模态框 -->
    <div v-if="showDetailModal" class="modal-overlay" @click="closeDetailModal">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h3>传感器日志详情</h3>
          <button class="close-btn" @click="closeDetailModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="detail-item">
            <label>日志ID:</label>
            <span>{{ selectedItem.id }}</span>
          </div>
          <div class="detail-item">
            <label>传感器ID:</label>
            <span>{{ selectedItem.sensorId }}</span>
          </div>
          <div class="detail-item">
            <label>传感器状态:</label>
            <span :class="'status-' + selectedItem.status">
              {{ getStatusText(selectedItem.status) }}
            </span>
          </div>
          <div class="detail-item">
            <label>检修时间:</label>
            <span>{{ formatDateTime(selectedItem.overhaulTime) || '未检修' }}</span>
          </div>
          <div class="detail-item">
            <label>检修员ID:</label>
            <span>{{ selectedItem.repairmanId || '无' }}</span>
          </div>
          <div class="detail-item">
            <label>创建时间:</label>
            <span>{{ formatDateTime(selectedItem.createTime) }}</span>
          </div>
          <div class="detail-item">
            <label>更新时间:</label>
            <span>{{ formatDateTime(selectedItem.updateTime) }}</span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="confirm-btn" @click="closeDetailModal">关闭</button>
        </div>
      </div>
    </div>

    <!-- 安排检修模态框 -->
    <div v-if="showMaintenanceModal" class="modal-overlay" @click="closeMaintenanceModal">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h3>安排传感器检修</h3>
          <button class="close-btn" @click="closeMaintenanceModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-item">
              <label>传感器ID:</label>
              <input v-model="maintenanceForm.sensorId" readonly />
            </div>
            <div class="form-item">
              <label>当前状态:</label>
              <span :class="'status-' + maintenanceForm.currentStatus">
                {{ getStatusText(maintenanceForm.currentStatus) }}
              </span>
            </div>
          </div>
          <div class="form-row">
            <div class="form-item">
              <label>检修时间 *:</label>
              <input v-model="maintenanceForm.overhaulTime" type="datetime-local" />
            </div>
            <div class="form-item">
              <label>检修员ID *:</label>
              <input v-model="maintenanceForm.repairmanId" placeholder="请输入检修员ID" type="number" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-item">
              <label>检修后状态 *:</label>
              <select v-model="maintenanceForm.newStatus">
                <option value="">请选择状态</option>
                <option value="0">正常</option>
                <option value="1">异常</option>
                <option value="2">离线</option>
              </select>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeMaintenanceModal">取消</button>
          <button class="confirm-btn" @click="saveMaintenance">安排检修</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'SensorLog',
  data() {
    return {
      // 搜索表单
      searchForm: {
        id: '',
        sensorId: '',
        status: '',
        repairmanId: '',
        overhaulStartDate: '',
        overhaulEndDate: '',
        createStartDate: '',
        createEndDate: '',
        keyword: ''
      },
      // 表格数据
      tableData: [
        {
          id: 1,
          sensorId: 2001,
          status: 0,
          overhaulTime: '2024-01-10 14:30:00',
          repairmanId: 101,
          createTime: '2024-01-15 10:30:00',
          updateTime: '2024-01-15 10:30:00'
        },
        {
          id: 2,
          sensorId: 2002,
          status: 1,
          overhaulTime: null,
          repairmanId: null,
          createTime: '2024-01-15 11:15:00',
          updateTime: '2024-01-15 11:15:00'
        },
        {
          id: 3,
          sensorId: 2003,
          status: 2,
          overhaulTime: '2024-01-12 09:00:00',
          repairmanId: 102,
          createTime: '2024-01-15 12:00:00',
          updateTime: '2024-01-15 12:00:00'
        },
        {
          id: 4,
          sensorId: 2004,
          status: 0,
          overhaulTime: '2024-01-14 16:45:00',
          repairmanId: 103,
          createTime: '2024-01-15 13:45:00',
          updateTime: '2024-01-15 13:45:00'
        },
        {
          id: 5,
          sensorId: 2005,
          status: 1,
          overhaulTime: null,
          repairmanId: null,
          createTime: '2024-01-15 14:20:00',
          updateTime: '2024-01-15 14:20:00'
        }
      ],
      // 排序
      sortField: '',
      sortOrder: 'asc',
      // 分页
      currentPage: 1,
      pageSize: 20,
      // 模态框
      showModal: false,
      showDetailModal: false,
      showMaintenanceModal: false,
      isEdit: false,
      selectedItem: {},
      formData: {
        sensorId: '',
        status: '',
        overhaulTime: '',
        repairmanId: ''
      },
      maintenanceForm: {
        sensorId: '',
        currentStatus: '',
        overhaulTime: '',
        repairmanId: '',
        newStatus: ''
      }
    }
  },
  computed: {
    filteredData() {
      let data = [...this.tableData]
      
      // 应用搜索过滤
      if (this.searchForm.id) {
        data = data.filter(item => item.id.toString().includes(this.searchForm.id))
      }
      if (this.searchForm.sensorId) {
        data = data.filter(item => item.sensorId.toString().includes(this.searchForm.sensorId))
      }
      if (this.searchForm.status !== '') {
        data = data.filter(item => item.status.toString() === this.searchForm.status)
      }
      if (this.searchForm.repairmanId) {
        data = data.filter(item => item.repairmanId && item.repairmanId.toString().includes(this.searchForm.repairmanId))
      }
      if (this.searchForm.overhaulStartDate) {
        data = data.filter(item => item.overhaulTime && item.overhaulTime >= this.searchForm.overhaulStartDate)
      }
      if (this.searchForm.overhaulEndDate) {
        data = data.filter(item => item.overhaulTime && item.overhaulTime <= this.searchForm.overhaulEndDate + ' 23:59:59')
      }
      if (this.searchForm.createStartDate) {
        data = data.filter(item => item.createTime >= this.searchForm.createStartDate)
      }
      if (this.searchForm.createEndDate) {
        data = data.filter(item => item.createTime <= this.searchForm.createEndDate + ' 23:59:59')
      }
      if (this.searchForm.keyword) {
        const keyword = this.searchForm.keyword.toLowerCase()
        data = data.filter(item => 
          item.sensorId.toString().includes(keyword) ||
          (item.repairmanId && item.repairmanId.toString().includes(keyword))
        )
      }
      
      // 应用排序
      if (this.sortField) {
        data.sort((a, b) => {
          let aVal = a[this.sortField]
          let bVal = b[this.sortField]
          
          if (typeof aVal === 'string') {
            aVal = aVal.toLowerCase()
            bVal = bVal.toLowerCase()
          }
          
          if (this.sortOrder === 'asc') {
            return aVal > bVal ? 1 : -1
          } else {
            return aVal < bVal ? 1 : -1
          }
        })
      }
      
      return data
    },
    paginatedData() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredData.slice(start, end)
    },
    totalPages() {
      return Math.ceil(this.filteredData.length / this.pageSize)
    },
    visiblePages() {
      const pages = []
      const total = this.totalPages
      const current = this.currentPage
      
      if (total <= 7) {
        for (let i = 1; i <= total; i++) {
          pages.push(i)
        }
      } else {
        if (current <= 4) {
          for (let i = 1; i <= 5; i++) {
            pages.push(i)
          }
          pages.push('...')
          pages.push(total)
        } else if (current >= total - 3) {
          pages.push(1)
          pages.push('...')
          for (let i = total - 4; i <= total; i++) {
            pages.push(i)
          }
        } else {
          pages.push(1)
          pages.push('...')
          for (let i = current - 1; i <= current + 1; i++) {
            pages.push(i)
          }
          pages.push('...')
          pages.push(total)
        }
      }
      
      return pages
    }
  },
  methods: {
    // 搜索
    searchData() {
      this.currentPage = 1
    },
    resetSearch() {
      this.searchForm = {
        id: '',
        sensorId: '',
        status: '',
        repairmanId: '',
        overhaulStartDate: '',
        overhaulEndDate: '',
        createStartDate: '',
        createEndDate: '',
        keyword: ''
      }
      this.currentPage = 1
    },
    // 排序
    sortBy(field) {
      if (this.sortField === field) {
        this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc'
      } else {
        this.sortField = field
        this.sortOrder = 'asc'
      }
    },
    getSortIcon(field) {
      if (this.sortField !== field) return '↕️'
      return this.sortOrder === 'asc' ? '↑' : '↓'
    },
    // 分页
    prevPage() {
      if (this.currentPage > 1) {
        this.currentPage--
      }
    },
    nextPage() {
      if (this.currentPage < this.totalPages) {
        this.currentPage++
      }
    },
    goToPage(page) {
      if (page !== '...' && page !== this.currentPage) {
        this.currentPage = page
      }
    },
    // 模态框
    showAddModal() {
      this.isEdit = false
      this.formData = {
        sensorId: '',
        status: '',
        overhaulTime: '',
        repairmanId: ''
      }
      this.showModal = true
    },
    editItem(item) {
      this.isEdit = true
      this.formData = { 
        ...item,
        overhaulTime: item.overhaulTime ? this.formatDateTimeForInput(item.overhaulTime) : ''
      }
      this.showModal = true
    },
    closeModal() {
      this.showModal = false
    },
    saveData() {
      // 验证必填字段
      if (!this.formData.sensorId || this.formData.status === '') {
        alert('请填写所有必填字段')
        return
      }
      
      if (this.isEdit) {
        // 更新数据
        const index = this.tableData.findIndex(item => item.id === this.formData.id)
        if (index !== -1) {
          this.tableData[index] = {
            ...this.formData,
            updateTime: new Date().toLocaleString()
          }
        }
      } else {
        // 新增数据
        const newItem = {
          ...this.formData,
          id: Math.max(...this.tableData.map(item => item.id)) + 1,
          createTime: new Date().toLocaleString(),
          updateTime: new Date().toLocaleString()
        }
        this.tableData.push(newItem)
      }
      
      this.closeModal()
    },
    deleteItem(item) {
      if (confirm('确定要删除这条传感器日志记录吗？')) {
        const index = this.tableData.findIndex(data => data.id === item.id)
        if (index !== -1) {
          this.tableData.splice(index, 1)
        }
      }
    },
    viewDetails(item) {
      this.selectedItem = item
      this.showDetailModal = true
    },
    closeDetailModal() {
      this.showDetailModal = false
    },
    // 安排检修
    scheduleMaintenance(item) {
      this.maintenanceForm = {
        sensorId: item.sensorId,
        currentStatus: item.status,
        overhaulTime: '',
        repairmanId: '',
        newStatus: ''
      }
      this.showMaintenanceModal = true
    },
    closeMaintenanceModal() {
      this.showMaintenanceModal = false
    },
    saveMaintenance() {
      // 验证必填字段
      if (!this.maintenanceForm.overhaulTime || !this.maintenanceForm.repairmanId || this.maintenanceForm.newStatus === '') {
        alert('请填写所有必填字段')
        return
      }
      
      // 创建新的日志记录
      const newItem = {
        id: Math.max(...this.tableData.map(item => item.id)) + 1,
        sensorId: this.maintenanceForm.sensorId,
        status: parseInt(this.maintenanceForm.newStatus),
        overhaulTime: this.maintenanceForm.overhaulTime,
        repairmanId: parseInt(this.maintenanceForm.repairmanId),
        createTime: new Date().toLocaleString(),
        updateTime: new Date().toLocaleString()
      }
      
      this.tableData.push(newItem)
      this.closeMaintenanceModal()
      alert('检修安排已保存')
    },
    // 导出数据
    exportData() {
      alert('导出Excel功能开发中...')
    },
    // 工具方法
    getStatusText(status) {
      const statusMap = {
        0: '正常',
        1: '异常',
        2: '离线'
      }
      return statusMap[status] || '未知'
    },
    formatDateTime(dateTime) {
      if (!dateTime) return ''
      return new Date(dateTime).toLocaleString()
    },
    formatDateTimeForInput(dateTime) {
      if (!dateTime) return ''
      const date = new Date(dateTime)
      return date.toISOString().slice(0, 16)
    }
  }
}
</script>

<style scoped>
.sensor-log {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
  padding: 20px;
}

/* 搜索区域样式 */
.search-section {
  background: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.search-row {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
  flex-wrap: wrap;
}

.search-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 200px;
}

.search-item label {
  font-weight: 500;
  color: #374151;
  white-space: nowrap;
  min-width: 80px;
}

.search-item input,
.search-item select {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 14px;
  flex: 1;
}

.search-item span {
  color: #6b7280;
}

.search-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.search-btn, .reset-btn, .add-btn, .export-btn {
  padding: 10px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.search-btn {
  background: #1E3A8A;
  color: white;
}

.search-btn:hover {
  background: #1E40AF;
}

.reset-btn {
  background: #6B7280;
  color: white;
}

.reset-btn:hover {
  background: #4B5563;
}

.add-btn {
  background: #059669;
  color: white;
}

.add-btn:hover {
  background: #047857;
}

.export-btn {
  background: #DC2626;
  color: white;
}

.export-btn:hover {
  background: #B91C1C;
}

/* 表格样式 */
.table-section {
  flex: 1;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  background: #f8f9fa;
  padding: 12px;
  text-align: left;
  font-weight: 600;
  color: #374151;
  border-bottom: 2px solid #e5e7eb;
  position: sticky;
  top: 0;
  z-index: 10;
}

.data-table th.sortable {
  cursor: pointer;
  user-select: none;
}

.data-table th.sortable:hover {
  background: #e5e7eb;
}

.sort-icon {
  margin-left: 5px;
  font-size: 12px;
}

.data-table td {
  padding: 12px;
  border-bottom: 1px solid #e5e7eb;
  color: #374151;
}

.data-table tr:hover {
  background: #f3f4f6;
}

.even-row {
  background: #f9fafb;
}

.even-row:hover {
  background: #f3f4f6;
}

/* 状态样式 */
.status-0 {
  background: #D1FAE5;
  color: #065F46;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-1 {
  background: #FEF3C7;
  color: #92400E;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-2 {
  background: #FEE2E2;
  color: #991B1B;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

/* 操作按钮 */
.actions {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}

.action-btn {
  padding: 6px 10px;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.view-btn {
  background: #3B82F6;
  color: white;
}

.view-btn:hover {
  background: #2563EB;
}

.edit-btn {
  background: #F59E0B;
  color: white;
}

.edit-btn:hover {
  background: #D97706;
}

.maintenance-btn {
  background: #8B5CF6;
  color: white;
}

.maintenance-btn:hover {
  background: #7C3AED;
}

.delete-btn {
  background: #EF4444;
  color: white;
}

.delete-btn:hover {
  background: #DC2626;
}

/* 分页样式 */
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: white;
  border-top: 1px solid #e5e7eb;
}

.pagination-info {
  color: #6b7280;
  font-size: 14px;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination-controls button {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  background: white;
  color: #374151;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.pagination-controls button:hover:not(:disabled) {
  background: #f3f4f6;
}

.pagination-controls button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-controls button.active {
  background: #1E3A8A;
  color: white;
  border-color: #1E3A8A;
}

.page-numbers {
  display: flex;
  gap: 5px;
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h3 {
  margin: 0;
  color: #1E3A8A;
  font-size: 18px;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #6b7280;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.close-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

.modal-body {
  padding: 20px;
}

.form-row {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
}

.form-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.form-item label {
  font-weight: 500;
  color: #374151;
}

.form-item input,
.form-item select {
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 14px;
}

.form-item input:focus,
.form-item select:focus {
  outline: none;
  border-color: #1E3A8A;
  box-shadow: 0 0 0 3px rgba(30, 58, 138, 0.1);
}

.detail-item {
  display: flex;
  margin-bottom: 15px;
  align-items: center;
}

.detail-item label {
  font-weight: 500;
  color: #374151;
  min-width: 120px;
}

.detail-item span {
  color: #6b7280;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 20px;
  border-top: 1px solid #e5e7eb;
}

.cancel-btn, .confirm-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cancel-btn {
  background: #6B7280;
  color: white;
}

.cancel-btn:hover {
  background: #4B5563;
}

.confirm-btn {
  background: #1E3A8A;
  color: white;
}

.confirm-btn:hover {
  background: #1E40AF;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sensor-log {
    padding: 10px;
  }
  
  .search-row {
    flex-direction: column;
    gap: 10px;
  }
  
  .search-item {
    min-width: auto;
  }
  
  .search-actions {
    justify-content: center;
  }
  
  .data-table {
    font-size: 12px;
  }
  
  .data-table th,
  .data-table td {
    padding: 8px;
  }
  
  .actions {
    flex-direction: column;
    gap: 3px;
  }
  
  .action-btn {
    font-size: 11px;
    padding: 4px 8px;
  }
  
  .pagination {
    flex-direction: column;
    gap: 10px;
  }
  
  .form-row {
    flex-direction: column;
  }
  
  .modal {
    width: 95%;
    margin: 10px;
  }
}

@media (max-width: 480px) {
  .search-section {
    padding: 15px;
  }
  
  .search-btn, .reset-btn, .add-btn, .export-btn {
    padding: 8px 12px;
    font-size: 12px;
  }
  
  .data-table {
    font-size: 11px;
  }
  
  .data-table th,
  .data-table td {
    padding: 6px;
  }
  
  .modal-header,
  .modal-body,
  .modal-footer {
    padding: 15px;
  }
  
  .pagination-controls button {
    padding: 6px 10px;
    font-size: 12px;
  }
}
</style>