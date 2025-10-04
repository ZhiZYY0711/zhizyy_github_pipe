<template>
  <div class="device-info">
    <div class="module-header">
      <h2>设备信息管理</h2>
      <div class="header-actions">
        <button class="btn btn-primary" @click="showAddDialog = true">
          <i class="icon-plus"></i>添加设备
        </button>
        <button class="btn btn-success" @click="exportToExcel">
          <i class="icon-export"></i>导出Excel
        </button>
      </div>
    </div>

    <!-- 搜索区域 -->
    <div class="search-section">
      <div class="search-row">
        <div class="search-item">
          <label>搜索字段：</label>
          <select v-model="searchField" class="search-select">
            <option value="">请选择搜索字段</option>
            <option value="id">设备ID</option>
            <option value="area">所属区域</option>
            <option value="pipeline">所属管道</option>
            <option value="status">设备状态</option>
            <option value="position">设备位置</option>
          </select>
        </div>
        <div class="search-item">
          <label>关键词：</label>
          <input 
            v-model="searchKeyword" 
            type="text" 
            placeholder="请输入搜索关键词"
            class="search-input"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="search-item">
          <label>设备状态：</label>
          <select v-model="statusFilter" class="search-select">
            <option value="">全部状态</option>
            <option value="0">正常</option>
            <option value="1">异常</option>
            <option value="2">离线</option>
          </select>
        </div>
        <button class="btn btn-primary" @click="handleSearch">搜索</button>
        <button class="btn btn-secondary" @click="resetSearch">重置</button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th @click="handleSort('id')" class="sortable">
              设备ID
              <span class="sort-indicator" v-if="sortField === 'id'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('area_name')" class="sortable">
              所属区域
              <span class="sort-indicator" v-if="sortField === 'area_name'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('pipeline_name')" class="sortable">
              所属管道
              <span class="sort-indicator" v-if="sortField === 'pipeline_name'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('status')" class="sortable">
              设备状态
              <span class="sort-indicator" v-if="sortField === 'status'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('position')" class="sortable">
              设备位置
              <span class="sort-indicator" v-if="sortField === 'position'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('create_time')" class="sortable">
              创建时间
              <span class="sort-indicator" v-if="sortField === 'create_time'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('update_time')" class="sortable">
              更新时间
              <span class="sort-indicator" v-if="sortField === 'update_time'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, index) in paginatedData" :key="item.id" 
              :class="{ 'even-row': index % 2 === 1 }">
            <td>{{ item.id }}</td>
            <td>{{ item.area_name }}</td>
            <td>{{ item.pipeline_name }}</td>
            <td>
              <span :class="getStatusClass(item.status)">
                {{ getStatusText(item.status) }}
              </span>
            </td>
            <td>{{ item.position }}</td>
            <td>{{ formatDateTime(item.create_time) }}</td>
            <td>{{ formatDateTime(item.update_time) }}</td>
            <td class="action-cell">
              <button class="btn btn-info btn-sm" @click="viewLogs(item)">日志</button>
              <div class="dropdown">
                <button class="btn btn-secondary btn-sm dropdown-toggle">更多</button>
                <div class="dropdown-menu">
                  <a href="#" @click="editDevice(item)">修改</a>
                  <a href="#" @click="deleteDevice(item)" class="text-danger">删除</a>
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页器 -->
    <div class="pagination-container">
      <div class="pagination-info">
        共 {{ totalItems }} 条记录，每页显示 {{ pageSize }} 条
      </div>
      <div class="pagination">
        <button 
          class="btn btn-sm" 
          :disabled="currentPage === 1"
          @click="changePage(currentPage - 1)"
        >
          上一页
        </button>
        <span class="page-numbers">
          <button 
            v-for="page in visiblePages" 
            :key="page"
            class="btn btn-sm"
            :class="{ 'active': page === currentPage }"
            @click="changePage(page)"
          >
            {{ page }}
          </button>
        </span>
        <button 
          class="btn btn-sm"
          :disabled="currentPage === totalPages"
          @click="changePage(currentPage + 1)"
        >
          下一页
        </button>
      </div>
    </div>

    <!-- 添加/编辑设备对话框 -->
    <div v-if="showAddDialog || showEditDialog" class="modal-overlay" @click="closeDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ showAddDialog ? '添加设备' : '编辑设备' }}</h3>
          <button class="close-btn" @click="closeDialog">&times;</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveDevice">
            <div class="form-group">
              <label>所属区域：</label>
              <select v-model="deviceForm.area_id" required class="form-control">
                <option value="">请选择区域</option>
                <option v-for="area in areas" :key="area.id" :value="area.id">
                  {{ area.province }} - {{ area.city }} - {{ area.district }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>所属管道：</label>
              <select v-model="deviceForm.pipeline_id" required class="form-control">
                <option value="">请选择管道</option>
                <option v-for="pipeline in pipelines" :key="pipeline.id" :value="pipeline.id">
                  {{ pipeline.name }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>设备状态：</label>
              <select v-model="deviceForm.status" required class="form-control">
                <option value="0">正常</option>
                <option value="1">异常</option>
                <option value="2">离线</option>
              </select>
            </div>
            <div class="form-group">
              <label>设备位置：</label>
              <input 
                v-model="deviceForm.position" 
                type="text" 
                required 
                class="form-control"
                placeholder="请输入设备经纬度位置"
              />
            </div>
            <div class="form-actions">
              <button type="submit" class="btn btn-primary">保存</button>
              <button type="button" class="btn btn-secondary" @click="closeDialog">取消</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DeviceInfo',
  data() {
    return {
      // 搜索相关
      searchField: '',
      searchKeyword: '',
      statusFilter: '',
      
      // 排序相关
      sortField: '',
      sortOrder: 'asc',
      
      // 分页相关
      currentPage: 1,
      pageSize: 20,
      totalItems: 0,
      
      // 对话框相关
      showAddDialog: false,
      showEditDialog: false,
      deviceForm: {
        id: null,
        area_id: '',
        pipeline_id: '',
        status: '0',
        position: ''
      },
      
      // 数据
      deviceData: [],
      filteredData: [],
      areas: [],
      pipelines: []
    }
  },
  computed: {
    totalPages() {
      return Math.ceil(this.totalItems / this.pageSize)
    },
    paginatedData() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredData.slice(start, end)
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
  mounted() {
    this.loadData()
    this.loadAreas()
    this.loadPipelines()
  },
  methods: {
    loadData() {
      // 模拟数据加载
      this.deviceData = [
        {
          id: 1,
          area_id: 1,
          area_name: '北京市 - 朝阳区 - 望京街道',
          pipeline_id: 1,
          pipeline_name: '京津管道',
          status: 0,
          position: '116.4074,39.9042',
          create_time: '2024-01-15 10:30:00',
          update_time: '2024-01-20 14:20:00'
        },
        {
          id: 2,
          area_id: 2,
          area_name: '上海市 - 浦东新区 - 陆家嘴街道',
          pipeline_id: 2,
          pipeline_name: '沪宁管道',
          status: 1,
          position: '121.4737,31.2304',
          create_time: '2024-01-16 09:15:00',
          update_time: '2024-01-21 16:45:00'
        },
        {
          id: 3,
          area_id: 3,
          area_name: '广州市 - 天河区 - 珠江新城',
          pipeline_id: 3,
          pipeline_name: '粤港管道',
          status: 2,
          position: '113.2644,23.1291',
          create_time: '2024-01-17 11:20:00',
          update_time: '2024-01-22 08:30:00'
        }
      ]
      
      this.filteredData = [...this.deviceData]
      this.totalItems = this.filteredData.length
    },
    
    loadAreas() {
      // 模拟区域数据
      this.areas = [
        { id: 1, province: '北京市', city: '朝阳区', district: '望京街道' },
        { id: 2, province: '上海市', city: '浦东新区', district: '陆家嘴街道' },
        { id: 3, province: '广州市', city: '天河区', district: '珠江新城' }
      ]
    },
    
    loadPipelines() {
      // 模拟管道数据
      this.pipelines = [
        { id: 1, name: '京津管道' },
        { id: 2, name: '沪宁管道' },
        { id: 3, name: '粤港管道' }
      ]
    },
    
    handleSearch() {
      let filtered = [...this.deviceData]
      
      if (this.searchField && this.searchKeyword) {
        filtered = filtered.filter(item => {
          const value = String(item[this.searchField] || '').toLowerCase()
          return value.includes(this.searchKeyword.toLowerCase())
        })
      }
      
      if (this.statusFilter !== '') {
        filtered = filtered.filter(item => item.status == this.statusFilter)
      }
      
      this.filteredData = filtered
      this.totalItems = filtered.length
      this.currentPage = 1
    },
    
    resetSearch() {
      this.searchField = ''
      this.searchKeyword = ''
      this.statusFilter = ''
      this.filteredData = [...this.deviceData]
      this.totalItems = this.filteredData.length
      this.currentPage = 1
    },
    
    handleSort(field) {
      if (this.sortField === field) {
        this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc'
      } else {
        this.sortField = field
        this.sortOrder = 'asc'
      }
      
      this.filteredData.sort((a, b) => {
        let aVal = a[field]
        let bVal = b[field]
        
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
    },
    
    changePage(page) {
      if (page >= 1 && page <= this.totalPages) {
        this.currentPage = page
      }
    },
    
    getStatusClass(status) {
      const classes = {
        0: 'status-normal',
        1: 'status-warning',
        2: 'status-danger'
      }
      return classes[status] || 'status-normal'
    },
    
    getStatusText(status) {
      const texts = {
        0: '正常',
        1: '异常',
        2: '离线'
      }
      return texts[status] || '未知'
    },
    
    formatDateTime(dateTime) {
      if (!dateTime) return '-'
      return dateTime.replace('T', ' ').substring(0, 19)
    },
    
    viewLogs(device) {
      alert(`查看设备 ${device.id} 的日志记录`)
    },
    
    editDevice(device) {
      this.deviceForm = { ...device }
      this.showEditDialog = true
    },
    
    deleteDevice(device) {
      if (confirm(`确定要删除设备 ${device.id} 吗？`)) {
        const index = this.deviceData.findIndex(item => item.id === device.id)
        if (index > -1) {
          this.deviceData.splice(index, 1)
          this.handleSearch()
          alert('删除成功')
        }
      }
    },
    
    saveDevice() {
      if (this.showAddDialog) {
        // 添加新设备
        const newDevice = {
          ...this.deviceForm,
          id: Date.now(),
          area_name: this.areas.find(a => a.id == this.deviceForm.area_id)?.province + ' - ' + 
                     this.areas.find(a => a.id == this.deviceForm.area_id)?.city + ' - ' +
                     this.areas.find(a => a.id == this.deviceForm.area_id)?.district,
          pipeline_name: this.pipelines.find(p => p.id == this.deviceForm.pipeline_id)?.name,
          create_time: new Date().toISOString().substring(0, 19).replace('T', ' '),
          update_time: new Date().toISOString().substring(0, 19).replace('T', ' ')
        }
        this.deviceData.push(newDevice)
        alert('添加成功')
      } else {
        // 编辑设备
        const index = this.deviceData.findIndex(item => item.id === this.deviceForm.id)
        if (index > -1) {
          this.deviceData[index] = {
            ...this.deviceForm,
            area_name: this.areas.find(a => a.id == this.deviceForm.area_id)?.province + ' - ' + 
                       this.areas.find(a => a.id == this.deviceForm.area_id)?.city + ' - ' +
                       this.areas.find(a => a.id == this.deviceForm.area_id)?.district,
            pipeline_name: this.pipelines.find(p => p.id == this.deviceForm.pipeline_id)?.name,
            update_time: new Date().toISOString().substring(0, 19).replace('T', ' ')
          }
          alert('修改成功')
        }
      }
      
      this.closeDialog()
      this.handleSearch()
    },
    
    closeDialog() {
      this.showAddDialog = false
      this.showEditDialog = false
      this.deviceForm = {
        id: null,
        area_id: '',
        pipeline_id: '',
        status: '0',
        position: ''
      }
    },
    
    exportToExcel() {
      alert('导出Excel功能')
    }
  }
}
</script>

<style scoped>
.device-info {
  padding: 20px;
  background: #f8f9fa;
  min-height: 100vh;
}

.module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 15px 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.module-header h2 {
  margin: 0;
  color: #1E3A8A;
  font-size: 24px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.search-section {
  background: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.search-row {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.search-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-item label {
  font-weight: 500;
  color: #333;
  white-space: nowrap;
}

.search-select, .search-input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.search-select {
  min-width: 150px;
}

.search-input {
  min-width: 200px;
}

.table-container {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  margin-bottom: 20px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  background: #1E3A8A;
  color: white;
  padding: 12px;
  text-align: left;
  font-weight: 500;
  border-bottom: 2px solid #0F2C6B;
}

.data-table th.sortable {
  cursor: pointer;
  user-select: none;
  position: relative;
  transition: background-color 0.3s;
}

.data-table th.sortable:hover {
  background: #0F2C6B;
}

.sort-indicator {
  margin-left: 5px;
  font-size: 12px;
}

.data-table td {
  padding: 12px;
  border-bottom: 1px solid #eee;
  transition: background-color 0.3s;
}

.data-table tr:hover td {
  background: #f0f8ff;
}

.data-table tr.even-row td {
  background: #f0f8ff;
}

.data-table tr.even-row:hover td {
  background: #e6f3ff;
}

.action-cell {
  display: flex;
  gap: 8px;
  align-items: center;
}

.dropdown {
  position: relative;
  display: inline-block;
}

.dropdown-toggle {
  cursor: pointer;
}

.dropdown-menu {
  display: none;
  position: absolute;
  top: 100%;
  right: 0;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  z-index: 1000;
  min-width: 100px;
}

.dropdown:hover .dropdown-menu {
  display: block;
}

.dropdown-menu a {
  display: block;
  padding: 8px 12px;
  text-decoration: none;
  color: #333;
  transition: background-color 0.3s;
}

.dropdown-menu a:hover {
  background: #f5f5f5;
}

.dropdown-menu a.text-danger {
  color: #dc3545;
}

.status-normal {
  color: #28a745;
  font-weight: 500;
}

.status-warning {
  color: #ffc107;
  font-weight: 500;
}

.status-danger {
  color: #dc3545;
  font-weight: 500;
}

.pagination-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 15px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.pagination-info {
  color: #666;
  font-size: 14px;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 5px;
}

.page-numbers {
  display: flex;
  gap: 2px;
}

.btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  color: #333;
  cursor: pointer;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.3s;
}

.btn:hover {
  background: #f5f5f5;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn.active {
  background: #1E3A8A;
  color: white;
  border-color: #1E3A8A;
}

.btn-primary {
  background: #1E3A8A;
  color: white;
  border-color: #1E3A8A;
}

.btn-primary:hover {
  background: #0F2C6B;
  border-color: #0F2C6B;
}

.btn-success {
  background: #28a745;
  color: white;
  border-color: #28a745;
}

.btn-success:hover {
  background: #218838;
  border-color: #218838;
}

.btn-info {
  background: #17a2b8;
  color: white;
  border-color: #17a2b8;
}

.btn-info:hover {
  background: #138496;
  border-color: #138496;
}

.btn-secondary {
  background: #6c757d;
  color: white;
  border-color: #6c757d;
}

.btn-secondary:hover {
  background: #5a6268;
  border-color: #5a6268;
}

.btn-sm {
  padding: 4px 8px;
  font-size: 12px;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
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
  color: #1E3A8A;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.close-btn:hover {
  color: #333;
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
  font-weight: 500;
  color: #333;
}

.form-control {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-control:focus {
  outline: none;
  border-color: #1E3A8A;
  box-shadow: 0 0 0 2px rgba(30, 58, 138, 0.2);
}

.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
}

.icon-plus::before {
  content: '+';
  margin-right: 5px;
}

.icon-export::before {
  content: '↓';
  margin-right: 5px;
}

@media (max-width: 768px) {
  .search-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-item {
    flex-direction: column;
    align-items: stretch;
  }
  
  .header-actions {
    flex-direction: column;
  }
  
  .pagination-container {
    flex-direction: column;
    gap: 10px;
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