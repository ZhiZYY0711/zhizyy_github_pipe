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
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-item">
          <label>任务名称：</label>
          <input v-model="searchForm.title" placeholder="请输入任务名称" class="input-field">
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
            <option value="高">高</option>
            <option value="中">中</option>
            <option value="低">低</option>
          </select>
        </div>
        <div class="filter-item">
          <label>负责人：</label>
          <input v-model="searchForm.assignee" placeholder="请输入负责人" class="input-field">
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
                <input v-model="taskForm.code" required class="input-field">
              </div>
              <div class="form-group">
                <label>任务名称：</label>
                <input v-model="taskForm.title" required class="input-field">
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>任务类型：</label>
                <select v-model="taskForm.type" required class="select-field">
                  <option value="">请选择类型</option>
                  <option value="设备检修">设备检修</option>
                  <option value="管道巡检">管道巡检</option>
                  <option value="数据分析">数据分析</option>
                  <option value="应急处理">应急处理</option>
                  <option value="系统维护">系统维护</option>
                </select>
              </div>
              <div class="form-group">
                <label>优先级：</label>
                <select v-model="taskForm.priority" required class="select-field">
                  <option value="高">高</option>
                  <option value="中">中</option>
                  <option value="低">低</option>
                </select>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>负责人：</label>
                <input v-model="taskForm.assignee" required class="input-field">
              </div>
              <div class="form-group">
                <label>截止时间：</label>
                <input v-model="taskForm.deadline" type="datetime-local" required class="input-field">
              </div>
            </div>
            <div class="form-group">
              <label>任务描述：</label>
              <textarea v-model="taskForm.description" rows="4" class="input-field"></textarea>
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
          <div class="detail-info" v-if="selectedTask">
            <div class="info-row">
              <span class="label">任务编号：</span>
              <span class="value">{{ selectedTask.code }}</span>
            </div>
            <div class="info-row">
              <span class="label">任务名称：</span>
              <span class="value">{{ selectedTask.title }}</span>
            </div>
            <div class="info-row">
              <span class="label">任务类型：</span>
              <span class="value">{{ selectedTask.type }}</span>
            </div>
            <div class="info-row">
              <span class="label">优先级：</span>
              <span class="value priority-badge" :class="getPriorityClass(selectedTask.priority)">
                {{ selectedTask.priority }}
              </span>
            </div>
            <div class="info-row">
              <span class="label">状态：</span>
              <span class="value status-badge" :class="getStatusClass(selectedTask.status)">
                {{ selectedTask.status }}
              </span>
            </div>
            <div class="info-row">
              <span class="label">负责人：</span>
              <span class="value">{{ selectedTask.assignee }}</span>
            </div>
            <div class="info-row">
              <span class="label">创建时间：</span>
              <span class="value">{{ selectedTask.createTime }}</span>
            </div>
            <div class="info-row">
              <span class="label">截止时间：</span>
              <span class="value">{{ selectedTask.deadline }}</span>
            </div>
            <div class="info-row">
              <span class="label">任务描述：</span>
              <span class="value">{{ selectedTask.description }}</span>
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
        status: '',
        priority: '',
        assignee: ''
      },
      tasksList: [
        {
          id: 1,
          code: 'TASK001',
          title: '管道段A-001压力检测',
          type: '管道巡检',
          priority: '高',
          status: '进行中',
          assignee: '张工程师',
          createTime: '2024-01-15 09:00',
          deadline: '2024-01-17 18:00',
          description: '对管道段A-001进行全面的压力检测，确保管道运行安全。'
        },
        {
          id: 2,
          code: 'TASK002',
          title: '传感器设备维护',
          type: '设备检修',
          priority: '中',
          status: '待处理',
          assignee: '李技术员',
          createTime: '2024-01-16 10:30',
          deadline: '2024-01-20 17:00',
          description: '对所有传感器设备进行定期维护和校准。'
        },
        {
          id: 3,
          code: 'TASK003',
          title: '数据异常分析报告',
          type: '数据分析',
          priority: '高',
          status: '已完成',
          assignee: '王分析师',
          createTime: '2024-01-14 14:15',
          deadline: '2024-01-16 12:00',
          description: '分析近期监测数据中的异常情况，提供详细报告。'
        },
        {
          id: 4,
          code: 'TASK004',
          title: '应急响应演练',
          type: '应急处理',
          priority: '中',
          status: '进行中',
          assignee: '赵队长',
          createTime: '2024-01-15 16:20',
          deadline: '2024-01-18 16:00',
          description: '组织进行管道泄漏应急响应演练。'
        },
        {
          id: 5,
          code: 'TASK005',
          title: '系统数据库优化',
          type: '系统维护',
          priority: '低',
          status: '待处理',
          assignee: '陈工程师',
          createTime: '2024-01-16 11:45',
          deadline: '2024-01-25 18:00',
          description: '优化系统数据库性能，提高查询效率。'
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
        priority: '中',
        assignee: '',
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
      return { total, pending, inProgress, completed }
    },
    filteredTasks() {
      return this.tasksList.filter(task => {
        return (!this.searchForm.title || task.title.includes(this.searchForm.title)) &&
               (!this.searchForm.status || task.status === this.searchForm.status) &&
               (!this.searchForm.priority || task.priority === this.searchForm.priority) &&
               (!this.searchForm.assignee || task.assignee.includes(this.searchForm.assignee))
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
          createTime: new Date().toLocaleString()
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
        priority: '中',
        assignee: '',
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

.tasks-list {
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

.status-badge, .priority-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-pending {
  background-color: #FEF3C7;
  color: #92400E;
}

.status-progress {
  background-color: #DBEAFE;
  color: #1E40AF;
}

.status-completed {
  background-color: #D1FAE5;
  color: #065F46;
}

.status-cancelled {
  background-color: #FEE2E2;
  color: #991B1B;
}

.priority-high {
  background-color: #FEE2E2;
  color: #991B1B;
}

.priority-medium {
  background-color: #FEF3C7;
  color: #92400E;
}

.priority-low {
  background-color: #E0E7FF;
  color: #3730A3;
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
  max-width: 700px;
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