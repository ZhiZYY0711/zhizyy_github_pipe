<template>
  <div class="virtual-expert-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">虚拟专家系统</h2>
      <div class="header-actions">
        <button class="btn btn-primary" @click="startNewConsultation">
          <i class="icon">💬</i> 新建咨询
        </button>
        <button class="btn btn-secondary" @click="refreshData">
          <i class="icon">↻</i> 刷新
        </button>
      </div>
    </div>

    <!-- 专家系统统计 -->
    <div class="stats-section">
      <div class="stat-card">
        <div class="stat-number">{{ expertStats.totalConsultations }}</div>
        <div class="stat-label">总咨询次数</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ expertStats.activeExperts }}</div>
        <div class="stat-label">在线专家</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ expertStats.resolvedIssues }}</div>
        <div class="stat-label">已解决问题</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ expertStats.avgResponseTime }}</div>
        <div class="stat-label">平均响应时间(分钟)</div>
      </div>
    </div>

    <!-- 主要功能区域 -->
    <div class="main-content">
      <!-- 左侧：专家列表和咨询历史 -->
      <div class="left-panel">
        <!-- 专家列表 -->
        <div class="expert-list-section">
          <h3 class="section-title">可用专家</h3>
          <div class="expert-list">
            <div 
              v-for="expert in availableExperts" 
              :key="expert.id"
              class="expert-card"
              :class="{ active: selectedExpert?.id === expert.id }"
              @click="selectExpert(expert)"
            >
              <div class="expert-avatar">
                <img :src="expert.avatar" :alt="expert.name">
                <span class="status-indicator" :class="expert.status"></span>
              </div>
              <div class="expert-info">
                <div class="expert-name">{{ expert.name }}</div>
                <div class="expert-specialty">{{ expert.specialty }}</div>
                <div class="expert-rating">
                  <span class="stars">★★★★★</span>
                  <span class="rating-score">{{ expert.rating }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 咨询历史 -->
        <div class="history-section">
          <h3 class="section-title">咨询历史</h3>
          <div class="history-list">
            <div 
              v-for="consultation in consultationHistory" 
              :key="consultation.id"
              class="history-item"
              @click="loadConsultation(consultation)"
            >
              <div class="history-header">
                <span class="consultation-title">{{ consultation.title }}</span>
                <span class="consultation-time">{{ consultation.time }}</span>
              </div>
              <div class="consultation-expert">专家：{{ consultation.expertName }}</div>
              <div class="consultation-status" :class="consultation.status">
                {{ consultation.statusText }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：对话区域 -->
      <div class="right-panel">
        <div class="chat-container">
          <!-- 对话头部 -->
          <div class="chat-header" v-if="selectedExpert">
            <div class="expert-info-header">
              <img :src="selectedExpert.avatar" :alt="selectedExpert.name" class="expert-avatar-small">
              <div>
                <div class="expert-name-header">{{ selectedExpert.name }}</div>
                <div class="expert-specialty-header">{{ selectedExpert.specialty }}</div>
              </div>
            </div>
            <div class="chat-actions">
              <button class="btn btn-sm btn-secondary" @click="clearChat">清空对话</button>
              <button class="btn btn-sm btn-primary" @click="saveConsultation">保存咨询</button>
            </div>
          </div>

          <!-- 对话内容 -->
          <div class="chat-messages" ref="chatMessages">
            <div v-if="!selectedExpert" class="no-expert-selected">
              <div class="welcome-message">
                <h3>欢迎使用虚拟专家系统</h3>
                <p>请选择一位专家开始咨询，或查看历史咨询记录</p>
              </div>
            </div>
            <div v-else>
              <div 
                v-for="message in currentMessages" 
                :key="message.id"
                class="message"
                :class="message.type"
              >
                <div class="message-avatar">
                  <img 
                    :src="message.type === 'user' ? '/default-user-avatar.png' : selectedExpert.avatar" 
                    :alt="message.type === 'user' ? '用户' : selectedExpert.name"
                  >
                </div>
                <div class="message-content">
                  <div class="message-header">
                    <span class="message-sender">
                      {{ message.type === 'user' ? '您' : selectedExpert.name }}
                    </span>
                    <span class="message-time">{{ message.time }}</span>
                  </div>
                  <div class="message-text">{{ message.content }}</div>
                </div>
              </div>
              
              <!-- 专家正在输入指示器 -->
              <div v-if="expertTyping" class="message expert typing-indicator">
                <div class="message-avatar">
                  <img :src="selectedExpert.avatar" :alt="selectedExpert.name">
                </div>
                <div class="message-content">
                  <div class="typing-dots">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 输入区域 -->
          <div class="chat-input" v-if="selectedExpert">
            <div class="input-container">
              <textarea 
                v-model="newMessage"
                placeholder="请输入您的问题..."
                class="message-input"
                rows="3"
                @keydown.enter.prevent="sendMessage"
              ></textarea>
              <div class="input-actions">
                <button class="btn btn-primary" @click="sendMessage" :disabled="!newMessage.trim()">
                  发送
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新建咨询模态框 -->
    <div v-if="showNewConsultationModal" class="modal-overlay" @click="showNewConsultationModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>新建咨询</h3>
          <button class="close-btn" @click="showNewConsultationModal = false">×</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="createConsultation">
            <div class="form-group">
              <label>咨询主题：</label>
              <input v-model="consultationForm.title" required class="input-field" placeholder="请输入咨询主题">
            </div>
            <div class="form-group">
              <label>问题类型：</label>
              <select v-model="consultationForm.category" required class="select-field">
                <option value="">请选择问题类型</option>
                <option value="设备故障">设备故障</option>
                <option value="管道维护">管道维护</option>
                <option value="安全问题">安全问题</option>
                <option value="技术咨询">技术咨询</option>
                <option value="应急处理">应急处理</option>
              </select>
            </div>
            <div class="form-group">
              <label>优先级：</label>
              <select v-model="consultationForm.priority" required class="select-field">
                <option value="低">低</option>
                <option value="中">中</option>
                <option value="高">高</option>
                <option value="紧急">紧急</option>
              </select>
            </div>
            <div class="form-group">
              <label>问题描述：</label>
              <textarea v-model="consultationForm.description" rows="4" class="input-field" placeholder="请详细描述您遇到的问题"></textarea>
            </div>
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="showNewConsultationModal = false">取消</button>
              <button type="submit" class="btn btn-primary">创建咨询</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'VirtualExpert',
  data() {
    return {
      selectedExpert: null,
      currentMessages: [],
      newMessage: '',
      expertTyping: false,
      showNewConsultationModal: false,
      consultationForm: {
        title: '',
        category: '',
        priority: '中',
        description: ''
      },
      availableExperts: [
        {
          id: 1,
          name: '张工程师',
          specialty: '管道设计与维护',
          avatar: '/expert-avatar-1.png',
          status: 'online',
          rating: 4.8
        },
        {
          id: 2,
          name: '李专家',
          specialty: '设备故障诊断',
          avatar: '/expert-avatar-2.png',
          status: 'online',
          rating: 4.9
        },
        {
          id: 3,
          name: '王教授',
          specialty: '安全风险评估',
          avatar: '/expert-avatar-3.png',
          status: 'busy',
          rating: 4.7
        },
        {
          id: 4,
          name: '赵顾问',
          specialty: '应急响应处理',
          avatar: '/expert-avatar-4.png',
          status: 'online',
          rating: 4.6
        }
      ],
      consultationHistory: [
        {
          id: 1,
          title: '管道压力异常问题咨询',
          expertName: '张工程师',
          time: '2024-01-15 14:30',
          status: 'resolved',
          statusText: '已解决'
        },
        {
          id: 2,
          title: '传感器故障排查',
          expertName: '李专家',
          time: '2024-01-14 10:15',
          status: 'resolved',
          statusText: '已解决'
        },
        {
          id: 3,
          title: '应急预案制定咨询',
          expertName: '赵顾问',
          time: '2024-01-13 16:45',
          status: 'pending',
          statusText: '待回复'
        }
      ]
    }
  },
  computed: {
    expertStats() {
      return {
        totalConsultations: 156,
        activeExperts: this.availableExperts.filter(expert => expert.status === 'online').length,
        resolvedIssues: 142,
        avgResponseTime: 8
      }
    }
  },
  methods: {
    selectExpert(expert) {
      this.selectedExpert = expert
      this.currentMessages = []
      // 模拟专家欢迎消息
      this.addMessage('expert', `您好！我是${expert.name}，专业领域是${expert.specialty}。请问有什么可以帮助您的？`)
    },
    sendMessage() {
      if (!this.newMessage.trim()) return
      
      // 添加用户消息
      this.addMessage('user', this.newMessage)
      const userMessage = this.newMessage
      this.newMessage = ''
      
      // 模拟专家回复
      this.expertTyping = true
      setTimeout(() => {
        this.expertTyping = false
        this.generateExpertReply(userMessage)
      }, 2000)
    },
    addMessage(type, content) {
      const message = {
        id: Date.now() + Math.random(),
        type,
        content,
        time: new Date().toLocaleTimeString()
      }
      this.currentMessages.push(message)
      this.$nextTick(() => {
        this.scrollToBottom()
      })
    },
    generateExpertReply(userMessage) {
      // 简单的关键词回复逻辑
      let reply = ''
      if (userMessage.includes('压力') || userMessage.includes('泄漏')) {
        reply = '根据您描述的压力问题，建议首先检查管道连接处是否有松动，然后查看压力传感器读数是否正常。如果问题持续，可能需要进行管道完整性检测。'
      } else if (userMessage.includes('设备') || userMessage.includes('故障')) {
        reply = '设备故障需要系统性排查。请提供设备型号、故障现象和错误代码（如有），我会为您提供详细的故障诊断步骤。'
      } else if (userMessage.includes('安全') || userMessage.includes('应急')) {
        reply = '安全问题需要立即重视。请详细描述当前情况，我会为您提供相应的应急处理措施和安全预案。'
      } else {
        reply = '感谢您的咨询。请提供更多详细信息，包括具体的问题现象、发生时间和相关参数，这样我能为您提供更准确的建议。'
      }
      
      this.addMessage('expert', reply)
    },
    scrollToBottom() {
      const chatMessages = this.$refs.chatMessages
      if (chatMessages) {
        chatMessages.scrollTop = chatMessages.scrollHeight
      }
    },
    clearChat() {
      if (confirm('确定要清空当前对话吗？')) {
        this.currentMessages = []
        if (this.selectedExpert) {
          this.addMessage('expert', `您好！我是${this.selectedExpert.name}，专业领域是${this.selectedExpert.specialty}。请问有什么可以帮助您的？`)
        }
      }
    },
    saveConsultation() {
      if (this.currentMessages.length > 1) {
        alert('咨询记录已保存到历史记录中')
      }
    },
    loadConsultation(consultation) {
      // 加载历史咨询记录
      this.currentMessages = [
        {
          id: 1,
          type: 'user',
          content: '这是历史咨询记录的内容...',
          time: consultation.time
        }
      ]
    },
    startNewConsultation() {
      this.showNewConsultationModal = true
    },
    createConsultation() {
      // 创建新的咨询
      console.log('创建咨询:', this.consultationForm)
      this.showNewConsultationModal = false
      this.consultationForm = {
        title: '',
        category: '',
        priority: '中',
        description: ''
      }
    },
    refreshData() {
      console.log('刷新专家系统数据')
    }
  }
}
</script>

<style scoped>
.virtual-expert-container {
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

.main-content {
  display: grid;
  grid-template-columns: 350px 1fr;
  gap: 20px;
  height: 600px;
}

.left-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.expert-list-section, .history-section {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  overflow: hidden;
}

.expert-list-section {
  flex: 1;
}

.history-section {
  flex: 1;
}

.section-title {
  padding: 15px 20px;
  margin: 0;
  background-color: #F9FAFB;
  border-bottom: 1px solid #E5E7EB;
  font-size: 16px;
  font-weight: 600;
  color: #374151;
}

.expert-list {
  padding: 10px;
  max-height: 300px;
  overflow-y: auto;
}

.expert-card {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 8px;
}

.expert-card:hover {
  background-color: #F3F4F6;
}

.expert-card.active {
  background-color: #EBF8FF;
  border: 2px solid #3B82F6;
}

.expert-avatar {
  position: relative;
  margin-right: 12px;
}

.expert-avatar img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.status-indicator {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid white;
}

.status-indicator.online {
  background-color: #10B981;
}

.status-indicator.busy {
  background-color: #F59E0B;
}

.status-indicator.offline {
  background-color: #6B7280;
}

.expert-info {
  flex: 1;
}

.expert-name {
  font-weight: 600;
  color: #374151;
  margin-bottom: 2px;
}

.expert-specialty {
  font-size: 12px;
  color: #6B7280;
  margin-bottom: 4px;
}

.expert-rating {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stars {
  color: #F59E0B;
  font-size: 12px;
}

.rating-score {
  font-size: 12px;
  color: #6B7280;
}

.history-list {
  padding: 10px;
  max-height: 300px;
  overflow-y: auto;
}

.history-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 8px;
  border: 1px solid #E5E7EB;
}

.history-item:hover {
  background-color: #F3F4F6;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.consultation-title {
  font-weight: 500;
  color: #374151;
  font-size: 14px;
}

.consultation-time {
  font-size: 12px;
  color: #6B7280;
}

.consultation-expert {
  font-size: 12px;
  color: #6B7280;
  margin-bottom: 4px;
}

.consultation-status {
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
  display: inline-block;
}

.consultation-status.resolved {
  background-color: #D1FAE5;
  color: #065F46;
}

.consultation-status.pending {
  background-color: #FEF3C7;
  color: #92400E;
}

.right-panel {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  display: flex;
  flex-direction: column;
}

.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-header {
  padding: 15px 20px;
  border-bottom: 1px solid #E5E7EB;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.expert-info-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.expert-avatar-small {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.expert-name-header {
  font-weight: 600;
  color: #374151;
}

.expert-specialty-header {
  font-size: 12px;
  color: #6B7280;
}

.chat-actions {
  display: flex;
  gap: 8px;
}

.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #F9FAFB;
}

.no-expert-selected {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
}

.welcome-message h3 {
  color: #374151;
  margin-bottom: 10px;
}

.welcome-message p {
  color: #6B7280;
}

.message {
  display: flex;
  margin-bottom: 20px;
  align-items: flex-start;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  margin: 0 10px;
}

.message-avatar img {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.message-content {
  max-width: 70%;
  background: white;
  border-radius: 12px;
  padding: 12px 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.message.user .message-content {
  background: #3B82F6;
  color: white;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.message-sender {
  font-weight: 600;
  font-size: 12px;
}

.message.user .message-sender {
  color: rgba(255,255,255,0.9);
}

.message-time {
  font-size: 11px;
  color: #6B7280;
}

.message.user .message-time {
  color: rgba(255,255,255,0.7);
}

.message-text {
  line-height: 1.5;
}

.typing-indicator .message-content {
  padding: 16px;
}

.typing-dots {
  display: flex;
  gap: 4px;
}

.typing-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #6B7280;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-dots span:nth-child(1) { animation-delay: -0.32s; }
.typing-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.chat-input {
  padding: 20px;
  border-top: 1px solid #E5E7EB;
}

.input-container {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.message-input {
  flex: 1;
  padding: 12px;
  border: 1px solid #D1D5DB;
  border-radius: 8px;
  resize: none;
  font-family: inherit;
}

.message-input:focus {
  outline: none;
  border-color: #3B82F6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
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

.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
}

.btn-primary {
  background-color: #1E3A8A;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: #0F2C6B;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  background-color: #6B7280;
  color: white;
}

.btn-secondary:hover {
  background-color: #4B5563;
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
  max-width: 500px;
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

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #374151;
}

.input-field, .select-field {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #D1D5DB;
  border-radius: 4px;
  font-size: 14px;
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

.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #E5E7EB;
}

.icon {
  font-style: normal;
}

@media (max-width: 768px) {
  .main-content {
    grid-template-columns: 1fr;
    height: auto;
  }
  
  .left-panel {
    order: 2;
  }
  
  .right-panel {
    order: 1;
    height: 400px;
  }
}
</style>