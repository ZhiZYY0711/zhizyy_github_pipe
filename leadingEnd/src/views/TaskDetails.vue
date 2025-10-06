<template>
  <div class="task-details">
    <div class="page-header">
      <h1>任务详情</h1>
      <p>查看和管理系统中的各类任务</p>
    </div>
    
    <div class="details-content">
      <!-- 任务统计卡片 -->
      <div class="task-stats">
        <div class="stat-card">
          <div class="stat-icon">📋</div>
          <div class="stat-content">
            <h3>总任务数</h3>
            <p class="stat-number">{{ taskStats.total }}</p>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-icon">🔄</div>
          <div class="stat-content">
            <h3>进行中</h3>
            <p class="stat-number">{{ taskStats.inProgress }}</p>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-icon">✅</div>
          <div class="stat-content">
            <h3>已完成</h3>
            <p class="stat-number">{{ taskStats.completed }}</p>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-icon">⏰</div>
          <div class="stat-content">
            <h3>超时任务</h3>
            <p class="stat-number">{{ taskStats.overdue }}</p>
          </div>
        </div>
      </div>
      
      <!-- 任务筛选器 -->
      <div class="task-filters">
        <div class="filter-group">
          <label>任务状态：</label>
          <select v-model="filters.status" @change="filterTasks">
            <option value="">全部</option>
            <option value="待分配">待分配</option>
            <option value="进行中">进行中</option>
            <option value="已完成">已完成</option>
            <option value="已超时">已超时</option>
          </select>
        </div>
        
        <div class="filter-group">
          <label>任务类型：</label>
          <select v-model="filters.type" @change="filterTasks">
            <option value="">全部</option>
            <option value="定期检测">定期检测</option>
            <option value="维护保养">维护保养</option>
            <option value="故障处理">故障处理</option>
            <option value="应急响应">应急响应</option>
          </select>
        </div>
        
        <div class="filter-group">
          <label>优先级：</label>
          <select v-model="filters.priority" @change="filterTasks">
            <option value="">全部</option>
            <option value="低">低</option>
            <option value="中">中</option>
            <option value="高">高</option>
            <option value="紧急">紧急</option>
          </select>
        </div>
        
        <div class="filter-group">
          <label>负责人：</label>
          <select v-model="filters.assignee" @change="filterTasks">
            <option value="">全部</option>
            <option value="张三">张三</option>
            <option value="李四">李四</option>
            <option value="王五">王五</option>
            <option value="赵六">赵六</option>
          </select>
        </div>
      </div>
      
      <!-- 任务列表 -->
      <div class="task-list">
        <h2>任务列表</h2>
        <div class="table-container">
          <table class="task-table">
            <thead>
              <tr>
                <th>任务ID</th>
                <th>任务名称</th>
                <th>类型</th>
                <th>优先级</th>
                <th>状态</th>
                <th>负责人</th>
                <th>创建时间</th>
                <th>截止时间</th>
                <th>进度</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="task in filteredTasks" :key="task.id" @click="selectTask(task)">
                <td>{{ task.id }}</td>
                <td>{{ task.name }}</td>
                <td>{{ task.type }}</td>
                <td>
                  <span :class="getPriorityClass(task.priority)">
                    {{ task.priority }}
                  </span>
                </td>
                <td>
                  <span :class="getStatusClass(task.status)">
                    {{ task.status }}
                  </span>
                </td>
                <td>{{ task.assignee }}</td>
                <td>{{ task.createTime }}</td>
                <td>{{ task.deadline }}</td>
                <td>
                  <div class="progress-bar">
                    <div 
                      class="progress-fill" 
                      :style="{ width: task.progress + '%' }"
                      :class="getProgressClass(task.progress)"
                    ></div>
                    <span class="progress-text">{{ task.progress }}%</span>
                  </div>
                </td>
                <td>
                  <button 
                    class="action-btn view-btn" 
                    @click.stop="viewTask(task)"
                  >
                    查看
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      
      <!-- 选中任务详情 -->
      <div class="selected-task-detail" v-if="selectedTask">
        <h2>任务详情 - {{ selectedTask.name }}</h2>
        <div class="task-detail-content">
          <div class="detail-section">
            <h3>基本信息</h3>
            <div class="detail-grid">
              <div class="detail-item">
                <strong>任务ID：</strong>{{ selectedTask.id }}
              </div>
              <div class="detail-item">
                <strong>任务名称：</strong>{{ selectedTask.name }}
              </div>
              <div class="detail-item">
                <strong>任务类型：</strong>{{ selectedTask.type }}
              </div>
              <div class="detail-item">
                <strong>优先级：</strong>
                <span :class="getPriorityClass(selectedTask.priority)">
                  {{ selectedTask.priority }}
                </span>
              </div>
              <div class="detail-item">
                <strong>状态：</strong>
                <span :class="getStatusClass(selectedTask.status)">
                  {{ selectedTask.status }}
                </span>
              </div>
              <div class="detail-item">
                <strong>负责人：</strong>{{ selectedTask.assignee }}
              </div>
              <div class="detail-item">
                <strong>创建时间：</strong>{{ selectedTask.createTime }}
              </div>
              <div class="detail-item">
                <strong>截止时间：</strong>{{ selectedTask.deadline }}
              </div>
            </div>
          </div>
          
          <div class="detail-section">
            <h3>任务描述</h3>
            <p>{{ selectedTask.description }}</p>
          </div>
          
          <div class="detail-section">
            <h3>执行记录</h3>
            <div class="execution-log">
              <div v-for="log in selectedTask.executionLog" :key="log.id" class="log-item">
                <div class="log-time">{{ log.time }}</div>
                <div class="log-content">{{ log.content }}</div>
                <div class="log-operator">{{ log.operator }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TaskDetails',
  data() {
    return {
      taskStats: {
        total: 0,
        inProgress: 0,
        completed: 0,
        overdue: 0
      },
      filters: {
        status: '',
        type: '',
        priority: '',
        assignee: ''
      },
      allTasks: [],
      filteredTasks: [],
      selectedTask: null
    }
  },
  mounted() {
    this.loadTaskData();
  },
  methods: {
    loadTaskData() {
      // 生成模拟任务数据
      this.allTasks = Array.from({ length: 20 }, (_, index) => {
        const statuses = ['待分配', '进行中', '已完成', '已超时'];
        const types = ['定期检测', '维护保养', '故障处理', '应急响应'];
        const priorities = ['低', '中', '高', '紧急'];
        const assignees = ['张三', '李四', '王五', '赵六'];
        
        const status = statuses[Math.floor(Math.random() * statuses.length)];
        const type = types[Math.floor(Math.random() * types.length)];
        const priority = priorities[Math.floor(Math.random() * priorities.length)];
        const assignee = assignees[Math.floor(Math.random() * assignees.length)];
        
        return {
          id: `T${String(index + 1).padStart(4, '0')}`,
          name: `${type}任务${index + 1}`,
          type,
          priority,
          status,
          assignee,
          createTime: new Date(Date.now() - Math.random() * 30 * 24 * 3600000).toLocaleDateString(),
          deadline: new Date(Date.now() + Math.random() * 30 * 24 * 3600000).toLocaleDateString(),
          progress: status === '已完成' ? 100 : Math.floor(Math.random() * 90) + 10,
          description: `这是一个${type}任务，需要对相关设备进行${type}操作，确保系统正常运行。`,
          executionLog: Array.from({ length: Math.floor(Math.random() * 5) + 1 }, (_, logIndex) => ({
            id: logIndex + 1,
            time: new Date(Date.now() - Math.random() * 7 * 24 * 3600000).toLocaleString(),
            content: `执行步骤${logIndex + 1}：完成相关操作`,
            operator: assignee
          }))
        };
      });
      
      this.filteredTasks = [...this.allTasks];
      this.updateTaskStats();
    },
    
    updateTaskStats() {
      this.taskStats.total = this.allTasks.length;
      this.taskStats.inProgress = this.allTasks.filter(task => task.status === '进行中').length;
      this.taskStats.completed = this.allTasks.filter(task => task.status === '已完成').length;
      this.taskStats.overdue = this.allTasks.filter(task => task.status === '已超时').length;
    },
    
    filterTasks() {
      this.filteredTasks = this.allTasks.filter(task => {
        return (!this.filters.status || task.status === this.filters.status) &&
               (!this.filters.type || task.type === this.filters.type) &&
               (!this.filters.priority || task.priority === this.filters.priority) &&
               (!this.filters.assignee || task.assignee === this.filters.assignee);
      });
    },
    
    selectTask(task) {
      this.selectedTask = task;
    },
    
    viewTask(task) {
      this.selectedTask = task;
      // 滚动到详情区域
      this.$nextTick(() => {
        const detailElement = document.querySelector('.selected-task-detail');
        if (detailElement) {
          detailElement.scrollIntoView({ behavior: 'smooth' });
        }
      });
    },
    
    getPriorityClass(priority) {
      const classMap = {
        '低': 'priority-low',
        '中': 'priority-medium',
        '高': 'priority-high',
        '紧急': 'priority-urgent'
      };
      return classMap[priority] || 'priority-low';
    },
    
    getStatusClass(status) {
      const classMap = {
        '待分配': 'status-pending',
        '进行中': 'status-progress',
        '已完成': 'status-completed',
        '已超时': 'status-overdue'
      };
      return classMap[status] || 'status-pending';
    },
    
    getProgressClass(progress) {
      if (progress >= 80) return 'progress-high';
      if (progress >= 50) return 'progress-medium';
      return 'progress-low';
    }
  }
}
</script>

<style scoped>
.task-details {
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
  max-width: 1400px;
  margin: 0 auto;
}

/* 任务统计卡片 */
.task-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background-color: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  font-size: 24px;
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f1f5f9;
  border-radius: 50%;
}

.stat-content h3 {
  color: #374151;
  font-size: 14px;
  margin-bottom: 5px;
  font-weight: 600;
}

.stat-number {
  color: #1e3a8a;
  font-size: 24px;
  font-weight: 700;
  margin: 0;
}

/* 任务筛选器 */
.task-filters {
  background-color: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-group label {
  font-weight: 600;
  color: #374151;
  white-space: nowrap;
}

.filter-group select {
  padding: 6px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  background-color: white;
  color: #374151;
}

/* 任务列表 */
.task-list {
  background-color: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.task-list h2 {
  color: #1e3a8a;
  font-size: 20px;
  margin-bottom: 20px;
  font-weight: 600;
}

.table-container {
  overflow-x: auto;
}

.task-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.task-table th,
.task-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.task-table th {
  background-color: #f8fafc;
  font-weight: 600;
  color: #374151;
}

.task-table tr:hover {
  background-color: #f8fafc;
  cursor: pointer;
}

/* 进度条 */
.progress-bar {
  position: relative;
  width: 80px;
  height: 20px;
  background-color: #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 10px;
  transition: width 0.3s ease;
}

.progress-low {
  background-color: #ef4444;
}

.progress-medium {
  background-color: #f59e0b;
}

.progress-high {
  background-color: #10b981;
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 10px;
  font-weight: 600;
  color: #374151;
}

/* 优先级样式 */
.priority-low {
  color: #10b981;
  font-weight: 600;
}

.priority-medium {
  color: #f59e0b;
  font-weight: 600;
}

.priority-high {
  color: #ef4444;
  font-weight: 600;
}

.priority-urgent {
  color: #dc2626;
  font-weight: 700;
  background-color: #fee2e2;
  padding: 2px 6px;
  border-radius: 4px;
}

/* 状态样式 */
.status-pending {
  color: #6b7280;
  font-weight: 600;
}

.status-progress {
  color: #3b82f6;
  font-weight: 600;
}

.status-completed {
  color: #10b981;
  font-weight: 600;
}

.status-overdue {
  color: #ef4444;
  font-weight: 600;
}

/* 操作按钮 */
.action-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.view-btn {
  background-color: #3b82f6;
  color: white;
}

.view-btn:hover {
  background-color: #2563eb;
}

/* 选中任务详情 */
.selected-task-detail {
  background-color: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.selected-task-detail h2 {
  color: #1e3a8a;
  font-size: 20px;
  margin-bottom: 20px;
  font-weight: 600;
}

.task-detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-section h3 {
  color: #374151;
  font-size: 16px;
  margin-bottom: 15px;
  font-weight: 600;
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 8px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 15px;
}

.detail-item {
  color: #374151;
  line-height: 1.6;
}

.execution-log {
  max-height: 300px;
  overflow-y: auto;
}

.log-item {
  display: flex;
  gap: 15px;
  padding: 10px;
  border-bottom: 1px solid #f3f4f6;
  align-items: center;
}

.log-time {
  color: #6b7280;
  font-size: 12px;
  white-space: nowrap;
  min-width: 120px;
}

.log-content {
  flex: 1;
  color: #374151;
  font-size: 14px;
}

.log-operator {
  color: #3b82f6;
  font-size: 12px;
  font-weight: 600;
  min-width: 60px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .task-filters {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-group {
    justify-content: space-between;
  }
  
  .task-table {
    font-size: 12px;
  }
  
  .task-table th,
  .task-table td {
    padding: 8px;
  }
  
  .detail-grid {
    grid-template-columns: 1fr;
  }
  
  .log-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 5px;
  }
}
</style>