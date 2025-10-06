<template>
  <div class="accident-response-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">事故响应管理</h2>
      <div class="header-actions">
        <button class="btn btn-danger" @click="showEmergencyModal = true">
          <i class="icon">🚨</i> 紧急响应
        </button>
        <button class="btn btn-primary" @click="showAddModal = true">
          <i class="icon">+</i> 新建事故
        </button>
        <button class="btn btn-secondary" @click="refreshData">
          <i class="icon">↻</i> 刷新
        </button>
      </div>
    </div>

    <!-- 事故响应统计 -->
    <div class="stats-section">
      <div class="stat-card emergency">
        <div class="stat-number">{{ accidentStats.emergency }}</div>
        <div class="stat-label">紧急事故</div>
      </div>
      <div class="stat-card high">
        <div class="stat-number">{{ accidentStats.high }}</div>
        <div class="stat-label">高级事故</div>
      </div>
      <div class="stat-card medium">
        <div class="stat-number">{{ accidentStats.medium }}</div>
        <div class="stat-label">中级事故</div>
      </div>
      <div class="stat-card low">
        <div class="stat-number">{{ accidentStats.low }}</div>
        <div class="stat-label">低级事故</div>
      </div>
      <div class="stat-card resolved">
        <div class="stat-number">{{ accidentStats.resolved }}</div>
        <div class="stat-label">已解决</div>
      </div>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-item">
          <label>事故编号：</label>
          <input v-model="searchForm.code" placeholder="请输入事故编号" class="input-field">
        </div>
        <div class="filter-item">
          <label>事故类型：</label>
          <select v-model="searchForm.type" class="select-field">
            <option value="">全部类型</option>
            <option value="管道泄漏">管道泄漏</option>
            <option value="设备故障">设备故障</option>
            <option value="压力异常">压力异常</option>
            <option value="火灾爆炸">火灾爆炸</option>
            <option value="人员伤亡">人员伤亡</option>
            <option value="环境污染">环境污染</option>
          </select>
        </div>
        <div class="filter-item">
          <label>严重程度：</label>
          <select v-model="searchForm.severity" class="select-field">
            <option value="">全部程度</option>
            <option value="紧急">紧急</option>
            <option value="高">高</option>
            <option value="中">中</option>
            <option value="低">低</option>
          </select>
        </div>
        <div class="filter-item">
          <label>状态：</label>
          <select v-model="searchForm.status" class="select-field">
            <option value="">全部状态</option>
            <option value="待响应">待响应</option>
            <option value="响应中">响应中</option>
            <option value="处理中">处理中</option>
            <option value="已解决">已解决</option>
            <option value="已关闭">已关闭</option>
          </select>
        </div>
        <button class="btn btn-search" @click="searchAccidents">搜索</button>
      </div>
    </div>

    <!-- 事故列表 -->
    <div class="accidents-list">
      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>事故编号</th>
              <th>事故类型</th>
              <th>严重程度</th>
              <th>发生位置</th>
              <th>状态</th>
              <th>响应人员</th>
              <th>发生时间</th>
              <th>响应时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="accident in paginatedAccidents" :key="accident.id">
              <td>{{ accident.code }}</td>
              <td>{{ accident.type }}</td>
              <td>
                <span class="severity-badge" :class="getSeverityClass(accident.severity)">
                  {{ accident.severity }}
                </span>
              </td>
              <td>{{ accident.location }}</td>
              <td>
                <span class="status-badge" :class="getStatusClass(accident.status)">
                  {{ accident.status }}
                </span>
              </td>
              <td>{{ accident.responder }}</td>
              <td>{{ accident.occurTime }}</td>
              <td>{{ accident.responseTime || '-' }}</td>
              <td>
                <button class="btn-action btn-view" @click="viewAccident(accident)">查看</button>
                <button class="btn-action btn-edit" @click="editAccident(accident)">编辑</button>
                <button class="btn-action btn-response" @click="responseAccident(accident)" v-if="accident.status === '待响应'">响应</button>
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
          第 {{ currentPage }} 页，共 {{ totalPages }} 页，总计 {{ filteredAccidents.length }} 条记录
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

    <!-- 紧急响应模态框 -->
    <div v-if="showEmergencyModal" class="modal-overlay" @click="showEmergencyModal = false">
      <div class="modal-content emergency-modal" @click.stop>
        <div class="modal-header emergency-header">
          <h3>🚨 紧急事故响应</h3>
          <button class="close-btn" @click="showEmergencyModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="emergency-actions">
            <button class="emergency-btn" @click="triggerEmergencyProtocol('fire')">
              <i class="icon">🔥</i>
              <span>火灾应急</span>
            </button>
            <button class="emergency-btn" @click="triggerEmergencyProtocol('leak')">
              <i class="icon">💧</i>
              <span>泄漏应急</span>
            </button>
            <button class="emergency-btn" @click="triggerEmergencyProtocol('explosion')">
              <i class="icon">💥</i>
              <span>爆炸应急</span>
            </button>
            <button class="emergency-btn" @click="triggerEmergencyProtocol('evacuation')">
              <i class="icon">🏃</i>
              <span>人员疏散</span>
            </button>
          </div>
          <div class="emergency-contacts">
            <h4>紧急联系人</h4>
            <div class="contact-list">
              <div class="contact-item">
                <span class="contact-role">应急指挥中心：</span>
                <span class="contact-phone">400-911-0001</span>
              </div>
              <div class="contact-item">
                <span class="contact-role">消防部门：</span>
                <span class="contact-phone">119</span>
              </div>
              <div class="contact-item">
                <span class="contact-role">医疗急救：</span>
                <span class="contact-phone">120</span>
              </div>
              <div class="contact-item">
                <span class="contact-role">安全负责人：</span>
                <span class="contact-phone">138-0013-8001</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加/编辑事故模态框 -->
    <div v-if="showAddModal || showEditModal" class="modal-overlay" @click="closeModals">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ showAddModal ? '新建事故记录' : '编辑事故记录' }}</h3>
          <button class="close-btn" @click="closeModals">×</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveAccident">
            <div class="form-row">
              <div class="form-group">
                <label>事故编号：</label>
                <input v-model="accidentForm.code" required class="input-field">
              </div>
              <div class="form-group">
                <label>事故类型：</label>
                <select v-model="accidentForm.type" required class="select-field">
                  <option value="">请选择类型</option>
                  <option value="管道泄漏">管道泄漏</option>
                  <option value="设备故障">设备故障</option>
                  <option value="压力异常">压力异常</option>
                  <option value="火灾爆炸">火灾爆炸</option>
                  <option value="人员伤亡">人员伤亡</option>
                  <option value="环境污染">环境污染</option>
                </select>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>严重程度：</label>
                <select v-model="accidentForm.severity" required class="select-field">
                  <option value="紧急">紧急</option>
                  <option value="高">高</option>
                  <option value="中">中</option>
                  <option value="低">低</option>
                </select>
              </div>
              <div class="form-group">
                <label>发生位置：</label>
                <input v-model="accidentForm.location" required class="input-field">
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>响应人员：</label>
                <input v-model="accidentForm.responder" required class="input-field">
              </div>
              <div class="form-group">
                <label>发生时间：</label>
                <input v-model="accidentForm.occurTime" type="datetime-local" required class="input-field">
              </div>
            </div>
            <div class="form-group">
              <label>事故描述：</label>
              <textarea v-model="accidentForm.description" rows="4" class="input-field" placeholder="请详细描述事故情况"></textarea>
            </div>
            <div class="form-group">
              <label>初步原因分析：</label>
              <textarea v-model="accidentForm.causeAnalysis" rows="3" class="input-field" placeholder="请分析可能的事故原因"></textarea>
            </div>
            <div class="form-group">
              <label>应急措施：</label>
              <textarea v-model="accidentForm.emergencyMeasures" rows="3" class="input-field" placeholder="请描述已采取的应急措施"></textarea>
            </div>
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="closeModals">取消</button>
              <button type="submit" class="btn btn-primary">保存</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- 查看事故详情模态框 -->
    <div v-if="showViewModal" class="modal-overlay" @click="showViewModal = false">
      <div class="modal-content large-modal" @click.stop>
        <div class="modal-header">
          <h3>事故详情</h3>
          <button class="close-btn" @click="showViewModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="detail-info" v-if="selectedAccident">
            <div class="detail-section">
              <h4>基本信息</h4>
              <div class="info-grid">
                <div class="info-row">
                  <span class="label">事故编号：</span>
                  <span class="value">{{ selectedAccident.code }}</span>
                </div>
                <div class="info-row">
                  <span class="label">事故类型：</span>
                  <span class="value">{{ selectedAccident.type }}</span>
                </div>
                <div class="info-row">
                  <span class="label">严重程度：</span>
                  <span class="value severity-badge" :class="getSeverityClass(selectedAccident.severity)">
                    {{ selectedAccident.severity }}
                  </span>
                </div>
                <div class="info-row">
                  <span class="label">发生位置：</span>
                  <span class="value">{{ selectedAccident.location }}</span>
                </div>
                <div class="info-row">
                  <span class="label">状态：</span>
                  <span class="value status-badge" :class="getStatusClass(selectedAccident.status)">
                    {{ selectedAccident.status }}
                  </span>
                </div>
                <div class="info-row">
                  <span class="label">响应人员：</span>
                  <span class="value">{{ selectedAccident.responder }}</span>
                </div>
                <div class="info-row">
                  <span class="label">发生时间：</span>
                  <span class="value">{{ selectedAccident.occurTime }}</span>
                </div>
                <div class="info-row">
                  <span class="label">响应时间：</span>
                  <span class="value">{{ selectedAccident.responseTime || '未响应' }}</span>
                </div>
              </div>
            </div>
            
            <div class="detail-section">
              <h4>事故描述</h4>
              <p class="description-text">{{ selectedAccident.description }}</p>
            </div>
            
            <div class="detail-section">
              <h4>原因分析</h4>
              <p class="description-text">{{ selectedAccident.causeAnalysis || '待分析' }}</p>
            </div>
            
            <div class="detail-section">
              <h4>应急措施</h4>
              <p class="description-text">{{ selectedAccident.emergencyMeasures || '无' }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AccidentResponse',
  data() {
    return {
      searchForm: {
        code: '',
        type: '',
        severity: '',
        status: ''
      },
      accidentsList: [
        {
          id: 1,
          code: 'ACC001',
          type: '管道泄漏',
          severity: '紧急',
          location: '管道段A-001',
          status: '响应中',
          responder: '张应急队长',
          occurTime: '2024-01-16 14:30',
          responseTime: '2024-01-16 14:35',
          description: '管道段A-001发生天然气泄漏，泄漏量约为50立方米/小时，现场有轻微异味。',
          causeAnalysis: '初步判断为管道接头老化导致密封失效。',
          emergencyMeasures: '已关闭上游阀门，疏散周边人员，现场警戒。'
        },
        {
          id: 2,
          code: 'ACC002',
          type: '设备故障',
          severity: '高',
          location: '压缩机站B',
          status: '处理中',
          responder: '李工程师',
          occurTime: '2024-01-15 10:15',
          responseTime: '2024-01-15 10:20',
          description: '压缩机站B主压缩机出现异常振动和噪音，压力输出不稳定。',
          causeAnalysis: '压缩机轴承磨损严重，需要更换。',
          emergencyMeasures: '切换至备用压缩机，安排维修人员检修。'
        },
        {
          id: 3,
          code: 'ACC003',
          type: '压力异常',
          severity: '中',
          location: '管道段C-005',
          status: '已解决',
          responder: '王技术员',
          occurTime: '2024-01-14 16:45',
          responseTime: '2024-01-14 16:50',
          description: '管道段C-005压力突然下降至正常值的70%。',
          causeAnalysis: '下游用户突然增加用气量导致压力下降。',
          emergencyMeasures: '调整上游压力，增加供气量。'
        },
        {
          id: 4,
          code: 'ACC004',
          type: '环境污染',
          severity: '高',
          location: '储气站D',
          status: '待响应',
          responder: '赵环保专员',
          occurTime: '2024-01-16 09:20',
          responseTime: '',
          description: '储气站D附近土壤检测发现轻微污染物超标。',
          causeAnalysis: '疑似历史泄漏造成土壤污染。',
          emergencyMeasures: '暂无'
        }
      ],
      currentPage: 1,
      pageSize: 10,
      showAddModal: false,
      showEditModal: false,
      showViewModal: false,
      showEmergencyModal: false,
      selectedAccident: null,
      accidentForm: {
        code: '',
        type: '',
        severity: '中',
        location: '',
        responder: '',
        occurTime: '',
        description: '',
        causeAnalysis: '',
        emergencyMeasures: ''
      }
    }
  },
  computed: {
    accidentStats() {
      const emergency = this.accidentsList.filter(acc => acc.severity === '紧急').length
      const high = this.accidentsList.filter(acc => acc.severity === '高').length
      const medium = this.accidentsList.filter(acc => acc.severity === '中').length
      const low = this.accidentsList.filter(acc => acc.severity === '低').length
      const resolved = this.accidentsList.filter(acc => acc.status === '已解决').length
      return { emergency, high, medium, low, resolved }
    },
    filteredAccidents() {
      return this.accidentsList.filter(accident => {
        return (!this.searchForm.code || accident.code.includes(this.searchForm.code)) &&
               (!this.searchForm.type || accident.type === this.searchForm.type) &&
               (!this.searchForm.severity || accident.severity === this.searchForm.severity) &&
               (!this.searchForm.status || accident.status === this.searchForm.status)
      })
    },
    paginatedAccidents() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredAccidents.slice(start, end)
    },
    totalPages() {
      return Math.ceil(this.filteredAccidents.length / this.pageSize)
    }
  },
  methods: {
    searchAccidents() {
      this.currentPage = 1
    },
    refreshData() {
      console.log('刷新事故响应数据')
    },
    viewAccident(accident) {
      this.selectedAccident = accident
      this.showViewModal = true
    },
    editAccident(accident) {
      this.accidentForm = { ...accident }
      this.showEditModal = true
    },
    responseAccident(accident) {
      const responseTime = new Date().toLocaleString()
      const index = this.accidentsList.findIndex(item => item.id === accident.id)
      if (index > -1) {
        this.accidentsList[index].status = '响应中'
        this.accidentsList[index].responseTime = responseTime
      }
      alert(`事故 ${accident.code} 已开始响应`)
    },
    saveAccident() {
      if (this.showAddModal) {
        const newAccident = {
          ...this.accidentForm,
          id: Date.now(),
          status: '待响应',
          responseTime: ''
        }
        this.accidentsList.push(newAccident)
      } else if (this.showEditModal) {
        const index = this.accidentsList.findIndex(item => item.id === this.accidentForm.id)
        if (index > -1) {
          this.accidentsList.splice(index, 1, { ...this.accidentForm })
        }
      }
      this.closeModals()
    },
    closeModals() {
      this.showAddModal = false
      this.showEditModal = false
      this.showViewModal = false
      this.accidentForm = {
        code: '',
        type: '',
        severity: '中',
        location: '',
        responder: '',
        occurTime: '',
        description: '',
        causeAnalysis: '',
        emergencyMeasures: ''
      }
    },
    triggerEmergencyProtocol(type) {
      const protocols = {
        fire: '火灾应急预案已启动！正在通知消防部门和疏散人员...',
        leak: '泄漏应急预案已启动！正在关闭相关阀门和通风系统...',
        explosion: '爆炸应急预案已启动！正在疏散人员和通知相关部门...',
        evacuation: '人员疏散预案已启动！正在广播疏散指令...'
      }
      alert(protocols[type])
      this.showEmergencyModal = false
    },
    getSeverityClass(severity) {
      const severityMap = {
        '紧急': 'severity-emergency',
        '高': 'severity-high',
        '中': 'severity-medium',
        '低': 'severity-low'
      }
      return severityMap[severity] || 'severity-medium'
    },
    getStatusClass(status) {
      const statusMap = {
        '待响应': 'status-pending',
        '响应中': 'status-responding',
        '处理中': 'status-processing',
        '已解决': 'status-resolved',
        '已关闭': 'status-closed'
      }
      return statusMap[status] || 'status-pending'
    }
  }
}
</script>

<style scoped>
.accident-response-container {
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

.stat-card.emergency {
  border-left-color: #DC2626;
}

.stat-card.high {
  border-left-color: #F59E0B;
}

.stat-card.medium {
  border-left-color: #3B82F6;
}

.stat-card.low {
  border-left-color: #10B981;
}

.stat-card.resolved {
  border-left-color: #6B7280;
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

.btn-danger {
  background-color: #DC2626;
  color: white;
}

.btn-danger:hover {
  background-color: #B91C1C;
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

.accidents-list {
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

.severity-badge, .status-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.severity-emergency {
  background-color: #FEE2E2;
  color: #991B1B;
}

.severity-high {
  background-color: #FEF3C7;
  color: #92400E;
}

.severity-medium {
  background-color: #DBEAFE;
  color: #1E40AF;
}

.severity-low {
  background-color: #D1FAE5;
  color: #065F46;
}

.status-pending {
  background-color: #FEF3C7;
  color: #92400E;
}

.status-responding {
  background-color: #DBEAFE;
  color: #1E40AF;
}

.status-processing {
  background-color: #E0E7FF;
  color: #3730A3;
}

.status-resolved {
  background-color: #D1FAE5;
  color: #065F46;
}

.status-closed {
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

.btn-response {
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

.emergency-modal {
  max-width: 600px;
}

.modal-header {
  padding: 20px;
  border-bottom: 1px solid #E5E7EB;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.emergency-header {
  background-color: #FEE2E2;
  color: #991B1B;
}

.modal-header h3 {
  margin: 0;
  color: #374151;
}

.emergency-header h3 {
  color: #991B1B;
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

.emergency-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  margin-bottom: 30px;
}

.emergency-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px;
  border: 2px solid #DC2626;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.emergency-btn:hover {
  background-color: #DC2626;
  color: white;
}

.emergency-btn .icon {
  font-size: 24px;
}

.emergency-contacts h4 {
  margin-bottom: 15px;
  color: #374151;
}

.contact-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.contact-item {
  display: flex;
  justify-content: space-between;
  padding: 10px;
  background-color: #F9FAFB;
  border-radius: 4px;
}

.contact-role {
  font-weight: 500;
  color: #374151;
}

.contact-phone {
  font-weight: 600;
  color: #DC2626;
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

.description-text {
  line-height: 1.6;
  color: #6B7280;
  margin: 0;
  padding: 15px;
  background-color: #F9FAFB;
  border-radius: 4px;
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
  
  .emergency-actions {
    grid-template-columns: 1fr;
  }
  
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>