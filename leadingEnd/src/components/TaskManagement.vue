<template>
  <div class="task-management">
    <div class="module-header">
      <h2>任务管理</h2>
      <div class="header-actions">
        <button class="btn btn-primary" @click="showAddDialog = true">
          <i class="icon-plus"></i>发布任务
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
            <option value="id">任务ID</option>
            <option value="inspection_id">监测记录ID</option>
            <option value="repairman_name">检修员姓名</option>
            <option value="result">任务状态</option>
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
          <label>任务状态：</label>
          <select v-model="statusFilter" class="search-select">
            <option value="">全部状态</option>
            <option value="0">已发布</option>
            <option value="1">已接取</option>
            <option value="2">已完成</option>
            <option value="3">异常</option>
          </select>
        </div>
        <div class="search-item">
          <label>时间范围：</label>
          <input v-model="startDate" type="date" class="search-input" />
          <span>至</span>
          <input v-model="endDate" type="date" class="search-input" />
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
              任务ID
              <span class="sort-indicator" v-if="sortField === 'id'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('inspection_id')" class="sortable">
              监测记录ID
              <span class="sort-indicator" v-if="sortField === 'inspection_id'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('repairman_name')" class="sortable">
              检修员
              <span class="sort-indicator" v-if="sortField === 'repairman_name'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('result')" class="sortable">
              任务状态
              <span class="sort-indicator" v-if="sortField === 'result'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('public_time')" class="sortable">
              发布时间
              <span class="sort-indicator" v-if="sortField === 'public_time'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('response_time')" class="sortable">
              响应时间
              <span class="sort-indicator" v-if="sortField === 'response_time'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="handleSort('accomplish_time')" class="sortable">
              完成时间
              <span class="sort-indicator" v-if="sortField === 'accomplish_time'">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th>反馈信息</th>
            <th @click="handleSort('create_time')" class="sortable">
              创建时间
              <span class="sort-indicator" v-if="sortField === 'create_time'">
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
            <td>{{ item.inspection_id }}</td>
            <td>{{ item.repairman_name }}</td>
            <td>
              <span :class="getStatusClass(item.result)">
                {{ getStatusText(item.result) }}
              </span>
            </td>
            <td>{{ formatDateTime(item.public_time) }}</td>
            <td>{{ formatDateTime(item.response_time) }}</td>
            <td>{{ formatDateTime(item.accomplish_time) }}</td>
            <td>
              <div class="feedback-cell" :title="item.feedback_information">
                {{ truncateText(item.feedback_information, 20) }}
              </div>
            </td>
            <td>{{ formatDateTime(item.create_time) }}</td>
            <td class="action-cell">
              <button class="btn btn-info btn-sm" @click="viewDetails(item)">详情</button>
              <div class="dropdown">
                <button class="btn btn-secondary btn-sm dropdown-toggle">更多</button>
                <div class="dropdown-menu">
                  <a href="#" @click="editTask(item)">修改</a>
                  <a href="#" @click="assignTask(item)" v-if="item.result === 0">分配</a>
                  <a href="#" @click="completeTask(item)" v-if="item.result === 1">完成</a>
                  <a href="#" @click="deleteTask(item)" class="text-danger">删除</a>
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

    <!-- 添加/编辑任务对话框 -->
    <div v-if="showAddDialog || showEditDialog" class="modal-overlay" @click="closeDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ showAddDialog ? '发布任务' : '编辑任务' }}</h3>
          <button class="close-btn" @click="closeDialog">&times;</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveTask">
            <div class="form-group">
              <label>监测记录ID：</label>
              <select v-model="taskForm.inspection_id" required class="form-control">
                <option value="">请选择监测记录</option>
                <option v-for="inspection in inspections" :key="inspection.id" :value="inspection.id">
                  {{ inspection.id }} - {{ inspection.sensor_name }} ({{ getDataStatusText(inspection.data_status) }})
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>分配检修员：</label>
              <select v-model="taskForm.repairman_id" required class="form-control">
                <option value="">请选择检修员</option>
                <option v-for="repairman in repairmen" :key="repairman.id" :value="repairman.id">
                  {{ repairman.name }} - {{ repairman.phone }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>任务状态：</label>
              <select v-model="taskForm.result" required class="form-control">
                <option value="0">已发布</option>
                <option value="1">已接取</option>
                <option value="2">已完成</option>
                <option value="3">异常</option>
              </select>
            </div>
            <div class="form-group">
              <label>发布时间：</label>
              <input 
                v-model="taskForm.public_time" 
                type="datetime-local" 
                required 
                class="form-control"
              />
            </div>
            <div class="form-group" v-if="taskForm.result >= 1">
              <label>响应时间：</label>
              <input 
                v-model="taskForm.response_time" 
                type="datetime-local" 
                class="form-control"
              />
            </div>
            <div class="form-group" v-if="taskForm.result >= 2">
              <label>完成时间：</label>
              <input 
                v-model="taskForm.accomplish_time" 
                type="datetime-local" 
                class="form-control"
              />
            </div>
            <div class="form-group">
              <label>反馈信息：</label>
              <textarea 
                v-model="taskForm.feedback_information" 
                class="form-control"
                rows="4"
                placeholder="请输入反馈信息"
              ></textarea>
            </div>
            <div class="form-actions">
              <button type="submit" class="btn btn-primary">保存</button>
              <button type="button" class="btn btn-secondary" @click="closeDialog">取消</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- 任务详情对话框 -->
    <div v-if="showDetailsDialog" class="modal-overlay" @click="closeDetailsDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>任务详情</h3>
          <button class="close-btn" @click="closeDetailsDialog">&times;</button>
        </div>
        <div class="modal-body">
          <div class="details-grid">
            <div class="detail-item">
              <label>任务ID：</label>
              <span>{{ selectedTask.id }}</span>
            </div>
            <div class="detail-item">
              <label>监测记录ID：</label>
              <span>{{ selectedTask.inspection_id }}</span>
            </div>
            <div class="detail-item">
              <label>检修员：</label>
              <span>{{ selectedTask.repairman_name }}</span>
            </div>
            <div class="detail-item">
              <label>任务状态：</label>
              <span :class="getStatusClass(selectedTask.result)">
                {{ getStatusText(selectedTask.result) }}
              </span>
            </div>
            <div class="detail-item">
              <label>发布时间：</label>
              <span>{{ formatDateTime(selectedTask.public_time) }}</span>
            </div>
            <div class="detail-item">
              <label>响应时间：</label>
              <span>{{ formatDateTime(selectedTask.response_time) }}</span>
            </div>
            <div class="detail-item">
              <label>完成时间：</label>
              <span>{{ formatDateTime(selectedTask.accomplish_time) }}</span>
            </div>
            <div class="detail-item full-width">
              <label>反馈信息：</label>
              <div class="feedback-content">{{ selectedTask.feedback_information || '暂无反馈' }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TaskManagement',
  data() {
    return {
      // 搜索相关
      searchField: '',
      searchKeyword: '',
      statusFilter: '',
      startDate: '',
      endDate: '',
      
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
      showDetailsDialog: false,
      taskForm: {
        id: null,
        inspection_id: '',
        repairman_id: '',
        result: '0',
        public_time: '',
        response_time: '',
        accomplish_time: '',
        feedback_information: ''
      },
      selectedTask: {},
      
      // 数据
      taskData: [],
      filteredData: [],
      inspections: [],
      repairmen: []
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
    this.loadInspections()
    this.loadRepairmen()
  },
  methods: {
    loadData() {
      // 模拟数据加载
      this.taskData = [
        {
          id: 1,
          inspection_id: 101,
          repairman_id: 1,
          repairman_name: '张三',
          result: 2,
          public_time: '2024-01-15 09:00:00',
          response_time: '2024-01-15 09:30:00',
          accomplish_time: '2024-01-15 15:00:00',
          feedback_information: '管道压力异常已修复，更换了压力传感器，现在运行正常。',
          create_time: '2024-01-15 09:00:00',
          update_time: '2024-01-15 15:00:00'
        },
        {
          id: 2,
          inspection_id: 102,
          repairman_id: 2,
          repairman_name: '李四',
          result: 1,
          public_time: '2024-01-16 10:00:00',
          response_time: '2024-01-16 10:15:00',
          accomplish_time: null,
          feedback_information: '已到达现场，正在检查设备状态。',
          create_time: '2024-01-16 10:00:00',
          update_time: '2024-01-16 10:15:00'
        },
        {
          id: 3,
          inspection_id: 103,
          repairman_id: 3,
          repairman_name: '王五',
          result: 0,
          public_time: '2024-01-17 08:00:00',
          response_time: null,
          accomplish_time: null,
          feedback_information: null,
          create_time: '2024-01-17 08:00:00',
          update_time: '2024-01-17 08:00:00'
        }
      ]
      
      this.filteredData = [...this.taskData]
      this.totalItems = this.filteredData.length
    },
    
    loadInspections() {
      // 模拟监测记录数据
      this.inspections = [
        { id: 101, sensor_name: '传感器001', data_status: 2 },
        { id: 102, sensor_name: '传感器002', data_status: 3 },
        { id: 103, sensor_name: '传感器003', data_status: 2 }
      ]
    },
    
    loadRepairmen() {
      // 模拟检修员数据
      this.repairmen = [
        { id: 1, name: '张三', phone: '13800138001' },
        { id: 2, name: '李四', phone: '13800138002' },
        { id: 3, name: '王五', phone: '13800138003' }
      ]
    },
    
    handleSearch() {
      let filtered = [...this.taskData]
      
      if (this.searchField && this.searchKeyword) {
        filtered = filtered.filter(item => {
          const value = String(item[this.searchField] || '').toLowerCase()
          return value.includes(this.searchKeyword.toLowerCase())
        })
      }
      
      if (this.statusFilter !== '') {
        filtered = filtered.filter(item => item.result == this.statusFilter)
      }
      
      if (this.startDate) {
        filtered = filtered.filter(item => item.public_time >= this.startDate)
      }
      
      if (this.endDate) {
        filtered = filtered.filter(item => item.public_time <= this.endDate + ' 23:59:59')
      }
      
      this.filteredData = filtered
      this.totalItems = filtered.length
      this.currentPage = 1
    },
    
    resetSearch() {
      this.searchField = ''
      this.searchKeyword = ''
      this.statusFilter = ''
      this.startDate = ''
      this.endDate = ''
      this.filteredData = [...this.taskData]
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
        0: 'status-published',
        1: 'status-accepted',
        2: 'status-completed',
        3: 'status-error'
      }
      return classes[status] || 'status-published'
    },
    
    getStatusText(status) {
      const texts = {
        0: '已发布',
        1: '已接取',
        2: '已完成',
        3: '异常'
      }
      return texts[status] || '未知'
    },
    
    getDataStatusText(status) {
      const texts = {
        0: '安全',
        1: '良好',
        2: '危险',
        3: '高危'
      }
      return texts[status] || '未知'
    },
    
    formatDateTime(dateTime) {
      if (!dateTime) return '-'
      return dateTime.replace('T', ' ').substring(0, 19)
    },
    
    truncateText(text, length) {
      if (!text) return '-'
      return text.length > length ? text.substring(0, length) + '...' : text
    },
    
    viewDetails(task) {
      this.selectedTask = { ...task }
      this.showDetailsDialog = true
    },
    
    editTask(task) {
      this.taskForm = { ...task }
      this.taskForm.public_time = this.formatDateTimeForInput(task.public_time)
      this.taskForm.response_time = this.formatDateTimeForInput(task.response_time)
      this.taskForm.accomplish_time = this.formatDateTimeForInput(task.accomplish_time)
      this.showEditDialog = true
    },
    
    assignTask(task) {
      if (confirm(`确定要分配任务 ${task.id} 吗？`)) {
        const index = this.taskData.findIndex(item => item.id === task.id)
        if (index > -1) {
          this.taskData[index].result = 1
          this.taskData[index].response_time = new Date().toISOString().substring(0, 19).replace('T', ' ')
          this.handleSearch()
          alert('任务分配成功')
        }
      }
    },
    
    completeTask(task) {
      if (confirm(`确定要完成任务 ${task.id} 吗？`)) {
        const index = this.taskData.findIndex(item => item.id === task.id)
        if (index > -1) {
          this.taskData[index].result = 2
          this.taskData[index].accomplish_time = new Date().toISOString().substring(0, 19).replace('T', ' ')
          this.handleSearch()
          alert('任务完成')
        }
      }
    },
    
    deleteTask(task) {
      if (confirm(`确定要删除任务 ${task.id} 吗？`)) {
        const index = this.taskData.findIndex(item => item.id === task.id)
        if (index > -1) {
          this.taskData.splice(index, 1)
          this.handleSearch()
          alert('删除成功')
        }
      }
    },
    
    saveTask() {
      if (this.showAddDialog) {
        // 添加新任务
        const newTask = {
          ...this.taskForm,
          id: Date.now(),
          repairman_name: this.repairmen.find(r => r.id == this.taskForm.repairman_id)?.name,
          create_time: new Date().toISOString().substring(0, 19).replace('T', ' '),
          update_time: new Date().toISOString().substring(0, 19).replace('T', ' ')
        }
        this.taskData.push(newTask)
        alert('任务发布成功')
      } else {
        // 编辑任务
        const index = this.taskData.findIndex(item => item.id === this.taskForm.id)
        if (index > -1) {
          this.taskData[index] = {
            ...this.taskForm,
            repairman_name: this.repairmen.find(r => r.id == this.taskForm.repairman_id)?.name,
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
      this.taskForm = {
        id: null,
        inspection_id: '',
        repairman_id: '',
        result: '0',
        public_time: '',
        response_time: '',
        accomplish_time: '',
        feedback_information: ''
      }
    },
    
    closeDetailsDialog() {
      this.showDetailsDialog = false
      this.selectedTask = {}
    },
    
    formatDateTimeForInput(dateTime) {
      if (!dateTime) return ''
      return dateTime.substring(0, 16)
    },
    
    exportToExcel() {
      alert('导出Excel功能')
    }
  }
}
</script>

<style scoped>
.task-management {
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
  min-width: 150px;
}

.table-container {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  margin-bottom: 20px;
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 1200px;
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

.feedback-cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: help;
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

.status-published {
  color: #6c757d;
  font-weight: 500;
}

.status-accepted {
  color: #007bff;
  font-weight: 500;
}

.status-completed {
  color: #28a745;
  font-weight: 500;
}

.status-error {
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

.details-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.detail-item.full-width {
  grid-column: 1 / -1;
}

.detail-item label {
  font-weight: 500;
  color: #333;
}

.detail-item span {
  color: #666;
}

.feedback-content {
  background: #f8f9fa;
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #ddd;
  min-height: 60px;
  white-space: pre-wrap;
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
  
  .details-grid {
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