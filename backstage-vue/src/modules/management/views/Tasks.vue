<template>
  <div class="tasks-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">任务管理</h2>
      <div class="header-actions">
        <button class="btn btn-primary" @click="showAddModal = true">
          <i class="icon">+</i> 创建任务
        </button>
        <button class="btn btn-secondary" @click="refreshData">
          <i class="icon">↻</i> 刷新
        </button>
      </div>
    </div>

    <!-- 任务统计卡片 -->
    <div class="stats-section">
      <div class="stat-card">
        <div class="stat-number">{{ taskStats.total }}</div>
        <div class="stat-label">总任务数</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ taskStats.pending }}</div>
        <div class="stat-label">待处理</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ taskStats.inProgress }}</div>
        <div class="stat-label">进行中</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ taskStats.completed }}</div>
        <div class="stat-label">已完成</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ taskStats.urgent }}</div>
        <div class="stat-label">紧急任务</div>
      </div>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-item">
          <label>任务名称：</label>
          <input v-model="searchForm.title" placeholder="请输入任务名称" class="input-field">
        </div>
        <div class="filter-item">
          <label>任务类型：</label>
          <select v-model="searchForm.type" class="select-field">
            <option value="">全部类型</option>
            <option value="管道巡检">管道巡检</option>
            <option value="设备维护">设备维护</option>
            <option value="安全检查">安全检查</option>
            <option value="数据分析">数据分析</option>
            <option value="应急处理">应急处理</option>
          </select>
        </div>
        <div class="filter-item">
          <label>任务状态：</label>
          <select v-model="searchForm.status" class="select-field">
            <option value="">全部状态</option>
            <option value="待处理">待处理</option>
            <option value="进行中">进行中</option>
            <option value="已完成">已完成</option>
            <option value="已取消">已取消</option>
          </select>
        </div>
        <div class="filter-item">
          <label>优先级：</label>
          <select v-model="searchForm.priority" class="select-field">
            <option value="">全部优先级</option>
            <option value="紧急">紧急</option>
            <option value="高">高</option>
            <option value="中">中</option>
            <option value="低">低</option>
          </select>
        </div>
        <button class="btn btn-search" @click="searchTasks">搜索</button>
      </div>
    </div>

    <!-- 任务列表 -->
    <div class="tasks-list">
      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>任务编号</th>
              <th>任务名称</th>
              <th>任务类型</th>
              <th>管道/区域</th>
              <th>优先级</th>
              <th>状态</th>
              <th>负责人</th>
              <th>创建时间</th>
              <th>截止时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="task in paginatedTasks" :key="task.id">
              <td>{{ task.code }}</td>
              <td>{{ task.title }}</td>
              <td>{{ task.type }}</td>
              <td>{{ task.location }}</td>
              <td>
                <span class="priority-badge" :class="getPriorityClass(task.priority)">
                  {{ task.priority }}
                </span>
              </td>
              <td>
                <span class="status-badge" :class="getStatusClass(task.status)">
                  {{ task.status }}
                </span>
              </td>
              <td>{{ task.assignee }}</td>
              <td>{{ task.createTime }}</td>
              <td>{{ task.deadline }}</td>
              <td>
                <button class="btn-action btn-view" @click="viewTask(task)">查看</button>
                <button class="btn-action btn-edit" @click="editTask(task)">编辑</button>
                <button class="btn-action btn-delete" @click="deleteTask(task)">删除</button>
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
          第 {{ currentPage }} 页，共 {{ totalPages }} 页，总计 {{ filteredTasks.length }} 条记录
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

    <!-- 添加/编辑任务模态框 -->
    <div v-if="showAddModal || showEditModal" class="modal-overlay" @click="closeModals">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ showAddModal ? '创建任务' : '编辑任务' }}</h3>
          <button class="close-btn" @click="closeModals">×</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveTask">
            <div class="form-row">
              <div class="form-group">
                <label>任务编号：</label>
                <input v-model="taskForm.code" required class="form-input">
              </div>
              <div class="form-group">
                <label>任务名称：</label>
                <input v-model="taskForm.title" required class="form-input">
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>任务类型：</label>
                <select v-model="taskForm.type" required class="form-input">
                  <option value="">请选择类型</option>
                  <option value="管道巡检">管道巡检</option>
                  <option value="设备维护">设备维护</option>
                  <option value="安全检查">安全检查</option>
                  <option value="数据分析">数据分析</option>
                  <option value="应急处理">应急处理</option>
                </select>
              </div>
              <div class="form-group">
                <label>管道/区域：</label>
                <input v-model="taskForm.location" required class="form-input">
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>优先级：</label>
                <select v-model="taskForm.priority" required class="form-input">
                  <option value="紧急">紧急</option>
                  <option value="高">高</option>
                  <option value="中">中</option>
                  <option value="低">低</option>
                </select>
              </div>
              <div class="form-group">
                <label>负责人：</label>
                <input v-model="taskForm.assignee" required class="form-input">
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>联系电话：</label>
                <input v-model="taskForm.phone" class="form-input">
              </div>
              <div class="form-group">
                <label>截止时间：</label>
                <input v-model="taskForm.deadline" type="datetime-local" required class="form-input">
              </div>
            </div>
            <div class="form-group">
              <label>任务描述：</label>
              <textarea v-model="taskForm.description" rows="4" class="form-input"></textarea>
            </div>
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="closeModals">取消</button>
              <button type="submit" class="btn btn-primary">保存</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- 查看任务详情模态框 -->
    <div v-if="showViewModal" class="modal-overlay" @click="showViewModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>任务详情</h3>
          <button class="close-btn" @click="showViewModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="detail-grid" v-if="selectedTask">
            <div class="detail-item">
              <label>任务编号：</label>
              <span>{{ selectedTask.code }}</span>
            </div>
            <div class="detail-item">
              <label>任务名称：</label>
              <span>{{ selectedTask.title }}</span>
            </div>
            <div class="detail-item">
              <label>任务类型：</label>
              <span>{{ selectedTask.type }}</span>
            </div>
            <div class="detail-item">
              <label>管道/区域：</label>
              <span>{{ selectedTask.location }}</span>
            </div>
            <div class="detail-item">
              <label>优先级：</label>
              <span class="priority-badge" :class="getPriorityClass(selectedTask.priority)">
                {{ selectedTask.priority }}
              </span>
            </div>
            <div class="detail-item">
              <label>状态：</label>
              <span class="status-badge" :class="getStatusClass(selectedTask.status)">
                {{ selectedTask.status }}
              </span>
            </div>
            <div class="detail-item">
              <label>负责人：</label>
              <span>{{ selectedTask.assignee }}</span>
            </div>
            <div class="detail-item">
              <label>联系电话：</label>
              <span>{{ selectedTask.phone }}</span>
            </div>
            <div class="detail-item">
              <label>创建时间：</label>
              <span>{{ selectedTask.createTime }}</span>
            </div>
            <div class="detail-item">
              <label>截止时间：</label>
              <span>{{ selectedTask.deadline }}</span>
            </div>
            <div class="detail-item full-width">
              <label>任务描述：</label>
              <span>{{ selectedTask.description }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Tasks',
  data() {
    return {
      searchForm: {
        title: '',
        type: '',
        status: '',
        priority: ''
      },
      tasksList: [
        {
          id: 1,
          code: 'TSK001',
          title: '西气东输一线A段压力监测',
          type: '管道巡检',
          location: '西气东输一线A段',
          priority: '紧急',
          status: '进行中',
          assignee: '张工程师',
          phone: '13800138001',
          createTime: '2024-01-15 09:00',
          deadline: '2024-01-17 18:00',
          description: '对西气东输一线A段进行全面的压力监测，检查管道运行状态，确保安全运行。'
        },
        {
          id: 2,
          code: 'TSK002',
          title: '华北区域传感器设备维护',
          type: '设备维护',
          location: '华北区域',
          priority: '高',
          status: '待处理',
          assignee: '李技术员',
          phone: '13800138002',
          createTime: '2024-01-16 10:30',
          deadline: '2024-01-20 17:00',
          description: '对华北区域所有传感器设备进行定期维护和校准，确保数据准确性。'
        },
        {
          id: 3,
          code: 'TSK003',
          title: '川气东送管道安全检查',
          type: '安全检查',
          location: '川气东送管道',
          priority: '高',
          status: '已完成',
          assignee: '王检查员',
          phone: '13800138003',
          createTime: '2024-01-14 14:15',
          deadline: '2024-01-16 12:00',
          description: '对川气东送管道进行全面安全检查，包括管道完整性、阀门功能等。'
        },
        {
          id: 4,
          code: 'TSK004',
          title: '华东区域监测数据异常分析',
          type: '数据分析',
          location: '华东区域',
          priority: '中',
          status: '进行中',
          assignee: '赵分析师',
          phone: '13800138004',
          createTime: '2024-01-15 16:20',
          deadline: '2024-01-18 16:00',
          description: '分析华东区域近期监测数据中的异常情况，提供详细分析报告。'
        },
        {
          id: 5,
          code: 'TSK005',
          title: '中缅管道泄漏应急处理',
          type: '应急处理',
          location: '中缅天然气管道',
          priority: '紧急',
          status: '待处理',
          assignee: '陈队长',
          phone: '13800138005',
          createTime: '2024-01-16 11:45',
          deadline: '2024-01-16 20:00',
          description: '处理中缅天然气管道发现的轻微泄漏问题，确保管道安全运行。'
        },
        {
          id: 6,
          code: 'TSK006',
          title: '西南区域设备巡检',
          type: '设备维护',
          location: '西南区域',
          priority: '中',
          status: '待处理',
          assignee: '孙工程师',
          phone: '13800138006',
          createTime: '2024-01-16 15:30',
          deadline: '2024-01-22 18:00',
          description: '对西南区域所有监测设备进行例行巡检和维护。'
        }
      ],
      currentPage: 1,
      pageSize: 10,
      showAddModal: false,
      showEditModal: false,
      showViewModal: false,
      selectedTask: null,
      taskForm: {
        code: '',
        title: '',
        type: '',
        location: '',
        priority: '中',
        assignee: '',
        phone: '',
        deadline: '',
        description: ''
      }
    }
  },
  computed: {
    taskStats() {
      const total = this.tasksList.length
      const pending = this.tasksList.filter(task => task.status === '待处理').length
      const inProgress = this.tasksList.filter(task => task.status === '进行中').length
      const completed = this.tasksList.filter(task => task.status === '已完成').length
      const urgent = this.tasksList.filter(task => task.priority === '紧急').length
      return { total, pending, inProgress, completed, urgent }
    },
    filteredTasks() {
      return this.tasksList.filter(task => {
        return (!this.searchForm.title || task.title.includes(this.searchForm.title)) &&
               (!this.searchForm.type || task.type === this.searchForm.type) &&
               (!this.searchForm.status || task.status === this.searchForm.status) &&
               (!this.searchForm.priority || task.priority === this.searchForm.priority)
      })
    },
    paginatedTasks() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredTasks.slice(start, end)
    },
    totalPages() {
      return Math.ceil(this.filteredTasks.length / this.pageSize)
    }
  },
  methods: {
    searchTasks() {
      this.currentPage = 1
    },
    refreshData() {
      console.log('刷新任务数据')
    },
    viewTask(task) {
      this.selectedTask = task
      this.showViewModal = true
    },
    editTask(task) {
      this.taskForm = { ...task }
      this.showEditModal = true
    },
    deleteTask(task) {
      if (confirm(`确定要删除任务 "${task.title}" 吗？`)) {
        const index = this.tasksList.findIndex(item => item.id === task.id)
        if (index > -1) {
          this.tasksList.splice(index, 1)
        }
      }
    },
    saveTask() {
      if (this.showAddModal) {
        const newTask = {
          ...this.taskForm,
          id: Date.now(),
          status: '待处理',
          createTime: new Date().toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
          }).replace(/\//g, '-')
        }
        this.tasksList.push(newTask)
      } else if (this.showEditModal) {
        const index = this.tasksList.findIndex(item => item.id === this.taskForm.id)
        if (index > -1) {
          this.tasksList.splice(index, 1, { ...this.taskForm })
        }
      }
      this.closeModals()
    },
    closeModals() {
      this.showAddModal = false
      this.showEditModal = false
      this.showViewModal = false
      this.taskForm = {
        code: '',
        title: '',
        type: '',
        location: '',
        priority: '中',
        assignee: '',
        phone: '',
        deadline: '',
        description: ''
      }
    },
    getStatusClass(status) {
      const statusMap = {
        '待处理': 'status-pending',
        '进行中': 'status-progress',
        '已完成': 'status-completed',
        '已取消': 'status-cancelled'
      }
      return statusMap[status] || 'status-pending'
    },
    getPriorityClass(priority) {
      const priorityMap = {
        '紧急': 'priority-urgent',
        '高': 'priority-high',
        '中': 'priority-medium',
        '低': 'priority-low'
      }
      return priorityMap[priority] || 'priority-medium'
    }
  }
}
</script>

<style scoped>
.tasks-container {
  padding: 20px;
  background-color: #f8fafc;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 24px;
  background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: white;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  text-align: center;
  border-left: 4px solid #3b82f6;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.stat-number {
  font-size: 36px;
  font-weight: 700;
  color: #1e3a8a;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s ease;
}

.btn-primary {
  background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
  color: white;
}

.btn-primary:hover {
  background: linear-gradient(135deg, #1e40af 0%, #2563eb 100%);
  transform: translateY(-1px);
}

.btn-secondary {
  background-color: #64748b;
  color: white;
}

.btn-secondary:hover {
  background-color: #475569;
  transform: translateY(-1px);
}

.btn-search {
  background: linear-gradient(135deg, #059669 0%, #10b981 100%);
  color: white;
}

.btn-search:hover {
  background: linear-gradient(135deg, #047857 0%, #059669 100%);
  transform: translateY(-1px);
}

.filter-section {
  background: white;
  padding: 24px;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
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
  gap: 8px;
}

.filter-item label {
  font-weight: 500;
  color: #374151;
  font-size: 14px;
}

.input-field, .select-field {
  padding: 10px 14px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  min-width: 160px;
  transition: border-color 0.2s ease;
}

.input-field:focus, .select-field:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.tasks-list {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
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
  padding: 16px;
  text-align: left;
  border-bottom: 1px solid #f1f5f9;
}

.data-table th {
  background-color: #f8fafc;
  font-weight: 600;
  color: #374151;
  font-size: 14px;
}

.data-table tr:hover {
  background-color: #f8fafc;
}

.status-badge, .priority-badge {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.status-pending {
  background-color: #fef3c7;
  color: #92400e;
}

.status-progress {
  background-color: #dbeafe;
  color: #1e40af;
}

.status-completed {
  background-color: #d1fae5;
  color: #065f46;
}

.status-cancelled {
  background-color: #fee2e2;
  color: #991b1b;
}

.priority-urgent {
  background-color: #fee2e2;
  color: #991b1b;
}

.priority-high {
  background-color: #fed7d7;
  color: #c53030;
}

.priority-medium {
  background-color: #fef3c7;
  color: #92400e;
}

.priority-low {
  background-color: #e0e7ff;
  color: #3730a3;
}

.btn-action {
  padding: 6px 12px;
  margin: 0 4px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.btn-view {
  background-color: #3b82f6;
  color: white;
}

.btn-edit {
  background-color: #f59e0b;
  color: white;
}

.btn-delete {
  background-color: #ef4444;
  color: white;
}

.btn-action:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.pagination {
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f1f5f9;
  background-color: #f8fafc;
}

.page-btn {
  padding: 8px 16px;
  border: 2px solid #e2e8f0;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s ease;
}

.page-btn:hover:not(:disabled) {
  background-color: #f1f5f9;
  border-color: #3b82f6;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
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
  border-radius: 12px;
  width: 90%;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.modal-header {
  padding: 24px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
  color: white;
  border-radius: 12px 12px 0 0;
}

.modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: white;
  opacity: 0.8;
  transition: opacity 0.2s ease;
}

.close-btn:hover {
  opacity: 1;
}

.modal-body {
  padding: 24px;
}

.form-row {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.form-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-weight: 500;
  color: #374151;
  font-size: 14px;
}

.form-input {
  padding: 12px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s ease;
}

.form-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

textarea.form-input {
  resize: vertical;
  min-height: 100px;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f1f5f9;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-item.full-width {
  grid-column: 1 / -1;
}

.detail-item label {
  font-weight: 600;
  color: #374151;
  font-size: 14px;
}

.detail-item span {
  color: #64748b;
  font-size: 14px;
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
  
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>