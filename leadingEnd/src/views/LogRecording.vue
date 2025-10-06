<template>
  <div class="log-recording-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">日志记录管理</h2>
      <div class="header-actions">
        <button class="btn btn-primary" @click="showAddModal = true">
          <i class="icon">+</i> 新建日志
        </button>
        <button class="btn btn-secondary" @click="exportLogs">
          <i class="icon">📤</i> 导出日志
        </button>
        <button class="btn btn-secondary" @click="refreshData">
          <i class="icon">↻</i> 刷新
        </button>
      </div>
    </div>

    <!-- 日志统计 -->
    <div class="stats-section">
      <div class="stat-card info">
        <div class="stat-number">{{ logStats.info }}</div>
        <div class="stat-label">信息日志</div>
      </div>
      <div class="stat-card warning">
        <div class="stat-number">{{ logStats.warning }}</div>
        <div class="stat-label">警告日志</div>
      </div>
      <div class="stat-card error">
        <div class="stat-number">{{ logStats.error }}</div>
        <div class="stat-label">错误日志</div>
      </div>
      <div class="stat-card debug">
        <div class="stat-number">{{ logStats.debug }}</div>
        <div class="stat-label">调试日志</div>
      </div>
      <div class="stat-card today">
        <div class="stat-number">{{ logStats.today }}</div>
        <div class="stat-label">今日日志</div>
      </div>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-item">
          <label>关键词：</label>
          <input v-model="searchForm.keyword" placeholder="请输入关键词" class="input-field">
        </div>
        <div class="filter-item">
          <label>日志级别：</label>
          <select v-model="searchForm.level" class="select-field">
            <option value="">全部级别</option>
            <option value="INFO">信息</option>
            <option value="WARNING">警告</option>
            <option value="ERROR">错误</option>
            <option value="DEBUG">调试</option>
          </select>
        </div>
        <div class="filter-item">
          <label>模块：</label>
          <select v-model="searchForm.module" class="select-field">
            <option value="">全部模块</option>
            <option value="系统">系统</option>
            <option value="用户">用户</option>
            <option value="数据">数据</option>
            <option value="安全">安全</option>
            <option value="网络">网络</option>
            <option value="设备">设备</option>
          </select>
        </div>
        <div class="filter-item">
          <label>开始时间：</label>
          <input v-model="searchForm.startTime" type="datetime-local" class="input-field">
        </div>
        <div class="filter-item">
          <label>结束时间：</label>
          <input v-model="searchForm.endTime" type="datetime-local" class="input-field">
        </div>
        <button class="btn btn-search" @click="searchLogs">搜索</button>
        <button class="btn btn-secondary" @click="clearSearch">清空</button>
      </div>
    </div>

    <!-- 日志列表 -->
    <div class="logs-list">
      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>时间</th>
              <th>级别</th>
              <th>模块</th>
              <th>用户</th>
              <th>操作</th>
              <th>IP地址</th>
              <th>状态</th>
              <th>详情</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in paginatedLogs" :key="log.id">
              <td>{{ log.timestamp }}</td>
              <td>
                <span class="level-badge" :class="getLevelClass(log.level)">
                  {{ getLevelText(log.level) }}
                </span>
              </td>
              <td>{{ log.module }}</td>
              <td>{{ log.user }}</td>
              <td>{{ log.action }}</td>
              <td>{{ log.ipAddress }}</td>
              <td>
                <span class="status-badge" :class="getStatusClass(log.status)">
                  {{ log.status }}
                </span>
              </td>
              <td class="details-cell">
                <span class="details-preview" :title="log.details">
                  {{ log.details.length > 30 ? log.details.substring(0, 30) + '...' : log.details }}
                </span>
              </td>
              <td>
                <button class="btn-action btn-view" @click="viewLog(log)">查看</button>
                <button class="btn-action btn-edit" @click="editLog(log)">编辑</button>
                <button class="btn-action btn-delete" @click="deleteLog(log)">删除</button>
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
          第 {{ currentPage }} 页，共 {{ totalPages }} 页，总计 {{ filteredLogs.length }} 条记录
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

    <!-- 添加/编辑日志模态框 -->
    <div v-if="showAddModal || showEditModal" class="modal-overlay" @click="closeModals">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ showAddModal ? '新建日志记录' : '编辑日志记录' }}</h3>
          <button class="close-btn" @click="closeModals">×</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveLog">
            <div class="form-row">
              <div class="form-group">
                <label>日志级别：</label>
                <select v-model="logForm.level" required class="select-field">
                  <option value="INFO">信息</option>
                  <option value="WARNING">警告</option>
                  <option value="ERROR">错误</option>
                  <option value="DEBUG">调试</option>
                </select>
              </div>
              <div class="form-group">
                <label>模块：</label>
                <select v-model="logForm.module" required class="select-field">
                  <option value="系统">系统</option>
                  <option value="用户">用户</option>
                  <option value="数据">数据</option>
                  <option value="安全">安全</option>
                  <option value="网络">网络</option>
                  <option value="设备">设备</option>
                </select>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>用户：</label>
                <input v-model="logForm.user" required class="input-field">
              </div>
              <div class="form-group">
                <label>IP地址：</label>
                <input v-model="logForm.ipAddress" required class="input-field" pattern="^(?:[0-9]{1,3}\.){3}[0-9]{1,3}$">
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>操作：</label>
                <input v-model="logForm.action" required class="input-field">
              </div>
              <div class="form-group">
                <label>状态：</label>
                <select v-model="logForm.status" required class="select-field">
                  <option value="成功">成功</option>
                  <option value="失败">失败</option>
                  <option value="处理中">处理中</option>
                </select>
              </div>
            </div>
            <div class="form-group">
              <label>详细信息：</label>
              <textarea v-model="logForm.details" rows="4" class="input-field" placeholder="请输入详细信息"></textarea>
            </div>
            <div class="form-group">
              <label>备注：</label>
              <textarea v-model="logForm.remarks" rows="3" class="input-field" placeholder="请输入备注信息"></textarea>
            </div>
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="closeModals">取消</button>
              <button type="submit" class="btn btn-primary">保存</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- 查看日志详情模态框 -->
    <div v-if="showViewModal" class="modal-overlay" @click="showViewModal = false">
      <div class="modal-content large-modal" @click.stop>
        <div class="modal-header">
          <h3>日志详情</h3>
          <button class="close-btn" @click="showViewModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="detail-info" v-if="selectedLog">
            <div class="detail-section">
              <h4>基本信息</h4>
              <div class="info-grid">
                <div class="info-row">
                  <span class="label">时间：</span>
                  <span class="value">{{ selectedLog.timestamp }}</span>
                </div>
                <div class="info-row">
                  <span class="label">级别：</span>
                  <span class="value level-badge" :class="getLevelClass(selectedLog.level)">
                    {{ getLevelText(selectedLog.level) }}
                  </span>
                </div>
                <div class="info-row">
                  <span class="label">模块：</span>
                  <span class="value">{{ selectedLog.module }}</span>
                </div>
                <div class="info-row">
                  <span class="label">用户：</span>
                  <span class="value">{{ selectedLog.user }}</span>
                </div>
                <div class="info-row">
                  <span class="label">操作：</span>
                  <span class="value">{{ selectedLog.action }}</span>
                </div>
                <div class="info-row">
                  <span class="label">IP地址：</span>
                  <span class="value">{{ selectedLog.ipAddress }}</span>
                </div>
                <div class="info-row">
                  <span class="label">状态：</span>
                  <span class="value status-badge" :class="getStatusClass(selectedLog.status)">
                    {{ selectedLog.status }}
                  </span>
                </div>
                <div class="info-row">
                  <span class="label">日志ID：</span>
                  <span class="value">{{ selectedLog.id }}</span>
                </div>
              </div>
            </div>
            
            <div class="detail-section">
              <h4>详细信息</h4>
              <div class="details-content">
                <pre>{{ selectedLog.details }}</pre>
              </div>
            </div>
            
            <div class="detail-section" v-if="selectedLog.remarks">
              <h4>备注信息</h4>
              <div class="remarks-content">
                <p>{{ selectedLog.remarks }}</p>
              </div>
            </div>

            <div class="detail-section" v-if="selectedLog.stackTrace">
              <h4>堆栈跟踪</h4>
              <div class="stack-trace">
                <pre>{{ selectedLog.stackTrace }}</pre>
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
  name: 'LogRecording',
  data() {
    return {
      searchForm: {
        keyword: '',
        level: '',
        module: '',
        startTime: '',
        endTime: ''
      },
      logsList: [
        {
          id: 'LOG001',
          timestamp: '2024-01-16 14:30:25',
          level: 'INFO',
          module: '用户',
          user: '张三',
          action: '用户登录',
          ipAddress: '192.168.1.100',
          status: '成功',
          details: '用户张三从IP地址192.168.1.100成功登录系统，使用Chrome浏览器',
          remarks: '正常登录操作'
        },
        {
          id: 'LOG002',
          timestamp: '2024-01-16 14:25:10',
          level: 'WARNING',
          module: '安全',
          user: '李四',
          action: '密码错误',
          ipAddress: '192.168.1.105',
          status: '失败',
          details: '用户李四尝试登录时输入错误密码，连续失败3次',
          remarks: '可能存在暴力破解风险',
          stackTrace: 'AuthenticationException: Invalid password\n  at AuthService.authenticate(AuthService.java:45)\n  at LoginController.login(LoginController.java:23)'
        },
        {
          id: 'LOG003',
          timestamp: '2024-01-16 14:20:15',
          level: 'ERROR',
          module: '数据',
          user: '系统',
          action: '数据备份',
          ipAddress: '127.0.0.1',
          status: '失败',
          details: '定时数据备份任务执行失败，磁盘空间不足',
          remarks: '需要清理磁盘空间',
          stackTrace: 'IOException: No space left on device\n  at BackupService.backup(BackupService.java:78)\n  at ScheduledTask.run(ScheduledTask.java:12)'
        },
        {
          id: 'LOG004',
          timestamp: '2024-01-16 14:15:30',
          level: 'DEBUG',
          module: '系统',
          user: '管理员',
          action: '系统配置',
          ipAddress: '192.168.1.1',
          status: '成功',
          details: '管理员修改了系统配置参数：max_connections=1000',
          remarks: '性能优化配置'
        },
        {
          id: 'LOG005',
          timestamp: '2024-01-16 14:10:45',
          level: 'INFO',
          module: '设备',
          user: '王五',
          action: '设备检查',
          ipAddress: '192.168.1.110',
          status: '成功',
          details: '完成设备EQ001的定期检查，所有参数正常',
          remarks: '设备运行良好'
        },
        {
          id: 'LOG006',
          timestamp: '2024-01-16 14:05:20',
          level: 'WARNING',
          module: '网络',
          user: '系统',
          action: '网络连接',
          ipAddress: '192.168.1.200',
          status: '处理中',
          details: '检测到网络延迟异常，平均延迟超过100ms',
          remarks: '正在监控网络状态'
        },
        {
          id: 'LOG007',
          timestamp: '2024-01-16 14:00:10',
          level: 'ERROR',
          module: '安全',
          user: '未知',
          action: '非法访问',
          ipAddress: '203.0.113.1',
          status: '失败',
          details: '检测到来自外部IP的非法访问尝试，已被防火墙拦截',
          remarks: '疑似攻击行为，已加入黑名单',
          stackTrace: 'SecurityException: Unauthorized access attempt\n  at SecurityFilter.doFilter(SecurityFilter.java:67)\n  at FilterChain.doFilter(FilterChain.java:23)'
        },
        {
          id: 'LOG008',
          timestamp: '2024-01-16 13:55:35',
          level: 'INFO',
          module: '数据',
          user: '赵六',
          action: '数据查询',
          ipAddress: '192.168.1.115',
          status: '成功',
          details: '用户赵六查询了2024年1月的管道监测数据，返回1250条记录',
          remarks: '常规数据查询操作'
        }
      ],
      currentPage: 1,
      pageSize: 10,
      showAddModal: false,
      showEditModal: false,
      showViewModal: false,
      selectedLog: null,
      logForm: {
        level: 'INFO',
        module: '系统',
        user: '',
        action: '',
        ipAddress: '',
        status: '成功',
        details: '',
        remarks: ''
      }
    }
  },
  computed: {
    logStats() {
      const info = this.logsList.filter(log => log.level === 'INFO').length
      const warning = this.logsList.filter(log => log.level === 'WARNING').length
      const error = this.logsList.filter(log => log.level === 'ERROR').length
      const debug = this.logsList.filter(log => log.level === 'DEBUG').length
      
      const today = new Date().toISOString().split('T')[0]
      const todayLogs = this.logsList.filter(log => log.timestamp.startsWith(today)).length
      
      return { info, warning, error, debug, today: todayLogs }
    },
    filteredLogs() {
      return this.logsList.filter(log => {
        const matchKeyword = !this.searchForm.keyword || 
          log.details.toLowerCase().includes(this.searchForm.keyword.toLowerCase()) ||
          log.action.toLowerCase().includes(this.searchForm.keyword.toLowerCase()) ||
          log.user.toLowerCase().includes(this.searchForm.keyword.toLowerCase())
        
        const matchLevel = !this.searchForm.level || log.level === this.searchForm.level
        const matchModule = !this.searchForm.module || log.module === this.searchForm.module
        
        const matchTimeRange = (!this.searchForm.startTime || log.timestamp >= this.searchForm.startTime) &&
                              (!this.searchForm.endTime || log.timestamp <= this.searchForm.endTime)
        
        return matchKeyword && matchLevel && matchModule && matchTimeRange
      })
    },
    paginatedLogs() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredLogs.slice(start, end)
    },
    totalPages() {
      return Math.ceil(this.filteredLogs.length / this.pageSize)
    }
  },
  methods: {
    searchLogs() {
      this.currentPage = 1
    },
    clearSearch() {
      this.searchForm = {
        keyword: '',
        level: '',
        module: '',
        startTime: '',
        endTime: ''
      }
      this.currentPage = 1
    },
    refreshData() {
      console.log('刷新日志数据')
    },
    exportLogs() {
      const csvContent = this.generateCSV()
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
      const link = document.createElement('a')
      const url = URL.createObjectURL(blob)
      link.setAttribute('href', url)
      link.setAttribute('download', `logs_${new Date().toISOString().split('T')[0]}.csv`)
      link.style.visibility = 'hidden'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    },
    generateCSV() {
      const headers = ['时间', '级别', '模块', '用户', '操作', 'IP地址', '状态', '详情']
      const csvRows = [headers.join(',')]
      
      this.filteredLogs.forEach(log => {
        const row = [
          log.timestamp,
          log.level,
          log.module,
          log.user,
          log.action,
          log.ipAddress,
          log.status,
          `"${log.details.replace(/"/g, '""')}"`
        ]
        csvRows.push(row.join(','))
      })
      
      return csvRows.join('\n')
    },
    viewLog(log) {
      this.selectedLog = log
      this.showViewModal = true
    },
    editLog(log) {
      this.logForm = { ...log }
      this.showEditModal = true
    },
    deleteLog(log) {
      if (confirm(`确定要删除日志 ${log.id} 吗？`)) {
        const index = this.logsList.findIndex(item => item.id === log.id)
        if (index > -1) {
          this.logsList.splice(index, 1)
        }
      }
    },
    saveLog() {
      if (this.showAddModal) {
        const newLog = {
          ...this.logForm,
          id: 'LOG' + String(Date.now()).slice(-6),
          timestamp: new Date().toLocaleString()
        }
        this.logsList.unshift(newLog)
      } else if (this.showEditModal) {
        const index = this.logsList.findIndex(item => item.id === this.logForm.id)
        if (index > -1) {
          this.logsList.splice(index, 1, { ...this.logForm })
        }
      }
      this.closeModals()
    },
    closeModals() {
      this.showAddModal = false
      this.showEditModal = false
      this.showViewModal = false
      this.logForm = {
        level: 'INFO',
        module: '系统',
        user: '',
        action: '',
        ipAddress: '',
        status: '成功',
        details: '',
        remarks: ''
      }
    },
    getLevelClass(level) {
      const levelMap = {
        'INFO': 'level-info',
        'WARNING': 'level-warning',
        'ERROR': 'level-error',
        'DEBUG': 'level-debug'
      }
      return levelMap[level] || 'level-info'
    },
    getLevelText(level) {
      const levelMap = {
        'INFO': '信息',
        'WARNING': '警告',
        'ERROR': '错误',
        'DEBUG': '调试'
      }
      return levelMap[level] || level
    },
    getStatusClass(status) {
      const statusMap = {
        '成功': 'status-success',
        '失败': 'status-failed',
        '处理中': 'status-processing'
      }
      return statusMap[status] || 'status-success'
    }
  }
}
</script>

<style scoped>
.log-recording-container {
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
  border-left: 4px solid #1E3A8A;
}

.stat-card.info {
  border-left-color: #3B82F6;
}

.stat-card.warning {
  border-left-color: #F59E0B;
}

.stat-card.error {
  border-left-color: #DC2626;
}

.stat-card.debug {
  border-left-color: #6B7280;
}

.stat-card.today {
  border-left-color: #10B981;
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

.logs-list {
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

.details-cell {
  max-width: 200px;
}

.details-preview {
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: help;
}

.level-badge, .status-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.level-info {
  background-color: #DBEAFE;
  color: #1E40AF;
}

.level-warning {
  background-color: #FEF3C7;
  color: #92400E;
}

.level-error {
  background-color: #FEE2E2;
  color: #991B1B;
}

.level-debug {
  background-color: #F3F4F6;
  color: #6B7280;
}

.status-success {
  background-color: #D1FAE5;
  color: #065F46;
}

.status-failed {
  background-color: #FEE2E2;
  color: #991B1B;
}

.status-processing {
  background-color: #FEF3C7;
  color: #92400E;
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
  background-color: #DC2626;
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

.large-modal {
  max-width: 900px;
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
  gap: 20px;
}

.detail-section h4 {
  margin: 0 0 15px 0;
  color: #374151;
  border-bottom: 2px solid #E5E7EB;
  padding-bottom: 5px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
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

.details-content, .remarks-content, .stack-trace {
  background-color: #F9FAFB;
  border-radius: 4px;
  padding: 15px;
  border: 1px solid #E5E7EB;
}

.details-content pre, .stack-trace pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.4;
}

.stack-trace {
  background-color: #FEF2F2;
  border-color: #FECACA;
}

.stack-trace pre {
  color: #991B1B;
}

.remarks-content p {
  margin: 0;
  line-height: 1.6;
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
  
  .info-grid {
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