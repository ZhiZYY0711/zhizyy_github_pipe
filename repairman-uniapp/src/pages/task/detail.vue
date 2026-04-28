<template>
  <view class="task-detail-container">
    <!-- 自定义导航栏 -->
    <view class="custom-navbar">
      <view class="navbar-left" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <text class="navbar-title">任务详情</text>
      <view class="navbar-right">
        <text class="share-icon" @click="shareTask">⋯</text>
      </view>
    </view>
    
    <!-- 页面内容 -->
    <scroll-view scroll-y="true" class="content">
      <!-- 任务基本信息 -->
      <view class="task-info-section">
        <view class="section-header">
          <view class="header-icon">📋</view>
          <text class="section-title">任务信息</text>
          <view class="task-status" :class="taskDetail.status">
            <text class="status-text">{{ getStatusText(taskDetail.status) }}</text>
          </view>
        </view>
        
        <view class="info-card">
          <view class="info-item">
            <text class="info-label">任务名称</text>
            <text class="info-value">{{ taskDetail.name }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">任务类型</text>
            <text class="info-value">{{ taskDetail.type }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">任务描述</text>
            <text class="info-value description">{{ taskDetail.description }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">任务地点</text>
            <text class="info-value location">📍 {{ taskDetail.location }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">开始时间</text>
            <text class="info-value">{{ formatDateTime(taskDetail.startTime) }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">结束时间</text>
            <text class="info-value">{{ formatDateTime(taskDetail.endTime) }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">负责人员</text>
            <text class="info-value">{{ taskDetail.assignee || '张三' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">优先级</text>
            <view class="priority-badge" :class="taskDetail.priority">
              <text class="priority-text">{{ getPriorityText(taskDetail.priority) }}</text>
            </view>
          </view>
        </view>
      </view>
      
      <!-- 执行步骤指引 -->
      <view class="steps-section">
        <view class="section-header">
          <view class="header-icon">📝</view>
          <text class="section-title">执行步骤</text>
        </view>
        
        <view class="steps-container">
          <view class="step-item" v-for="(step, index) in executionSteps" :key="index" :class="{ completed: step.completed, current: step.current }">
            <view class="step-number">
              <text class="step-text" v-if="!step.completed">{{ index + 1 }}</text>
              <text class="step-check" v-else>✓</text>
            </view>
            <view class="step-content">
              <text class="step-title">{{ step.title }}</text>
              <text class="step-desc">{{ step.description }}</text>
              <view class="step-actions" v-if="step.current && !step.completed">
                <view class="step-btn" @click="completeStep(index)">
                  <text class="btn-text">完成此步骤</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>
      
      <!-- 现场数据采集 -->
      <view class="data-collection-section">
        <view class="section-header">
          <view class="header-icon">📊</view>
          <text class="section-title">数据采集</text>
        </view>
        
        <view class="data-form">
          <view class="form-item" v-for="(field, index) in dataFields" :key="index">
            <text class="field-label">{{ field.label }}</text>
            <input 
              class="field-input" 
              :placeholder="field.placeholder"
              :type="field.type"
              v-model="collectedData[field.key]"
            />
            <text class="field-unit" v-if="field.unit">{{ field.unit }}</text>
          </view>
          
          <view class="data-actions">
            <view class="data-btn save" @click="saveData">
              <text class="btn-text">保存数据</text>
            </view>
            <view class="data-btn submit" @click="submitData">
              <text class="btn-text">提交数据</text>
            </view>
          </view>
        </view>
      </view>
      
      <!-- 图片/视频上传 -->
      <view class="media-upload-section">
        <view class="section-header">
          <view class="header-icon">📷</view>
          <text class="section-title">现场记录</text>
        </view>
        
        <view class="upload-container">
          <view class="upload-grid">
            <view class="upload-item" v-for="(media, index) in uploadedMedia" :key="index">
              <image class="media-preview" :src="media.url" mode="aspectFill" @click="previewMedia(media)"></image>
              <view class="media-delete" @click="deleteMedia(index)">
                <text class="delete-icon">×</text>
              </view>
            </view>
            <view class="upload-add" @click="chooseMedia" v-if="uploadedMedia.length < 9">
              <text class="add-icon">+</text>
              <text class="add-text">添加图片</text>
            </view>
          </view>
          
          <view class="upload-actions">
            <view class="upload-btn camera" @click="takePhoto">
              <text class="btn-text">📷 拍照</text>
            </view>
            <view class="upload-btn video" @click="recordVideo">
              <text class="btn-text">🎥 录像</text>
            </view>
            <view class="upload-btn album" @click="chooseFromAlbum">
              <text class="btn-text">🖼️ 相册</text>
            </view>
          </view>
        </view>
      </view>
      
      <!-- 任务备注 -->
      <view class="notes-section">
        <view class="section-header">
          <view class="header-icon">📝</view>
          <text class="section-title">任务备注</text>
        </view>
        
        <view class="notes-container">
          <textarea 
            class="notes-textarea" 
            placeholder="请输入任务执行过程中的备注信息..."
            v-model="taskNotes"
            maxlength="500"
          ></textarea>
          <view class="notes-counter">
            <text class="counter-text">{{ taskNotes.length }}/500</text>
          </view>
        </view>
      </view>
      
      <!-- 电子签名 -->
      <view class="signature-section">
        <view class="section-header">
          <view class="header-icon">✍️</view>
          <text class="section-title">电子签名</text>
        </view>
        
        <view class="signature-container">
          <canvas 
            class="signature-canvas" 
            canvas-id="signatureCanvas"
            @touchstart="startSignature"
            @touchmove="drawSignature"
            @touchend="endSignature"
          ></canvas>
          <view class="signature-actions">
            <view class="signature-btn clear" @click="clearSignature">
              <text class="btn-text">清除</text>
            </view>
            <view class="signature-btn save" @click="saveSignature">
              <text class="btn-text">保存签名</text>
            </view>
          </view>
        </view>
      </view>
      
      <!-- 底部操作按钮 -->
      <view class="bottom-actions">
        <view class="action-btn secondary" @click="saveDraft">
          <text class="btn-text">保存草稿</text>
        </view>
        <view class="action-btn primary" @click="submitTask">
          <text class="btn-text">提交任务</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
export default {
  name: 'TaskDetail',
  data() {
    return {
      taskId: '',
      taskDetail: {
        id: '',
        name: '',
        type: '',
        status: '',
        description: '',
        location: '',
        startTime: '',
        endTime: '',
        assignee: '',
        priority: ''
      },
      
      // 执行步骤
      executionSteps: [
        {
          title: '安全检查',
          description: '检查现场安全状况，确保作业环境安全',
          completed: true,
          current: false
        },
        {
          title: '设备准备',
          description: '准备检测设备和工具，确保设备正常运行',
          completed: true,
          current: false
        },
        {
          title: '现场检测',
          description: '按照标准流程进行现场检测作业',
          completed: false,
          current: true
        },
        {
          title: '数据记录',
          description: '记录检测数据和现场情况',
          completed: false,
          current: false
        },
        {
          title: '结果确认',
          description: '确认检测结果，完成任务报告',
          completed: false,
          current: false
        }
      ],
      
      // 数据采集字段
      dataFields: [
        { key: 'pressure', label: '管道压力', placeholder: '请输入压力值', type: 'number', unit: 'MPa' },
        { key: 'temperature', label: '环境温度', placeholder: '请输入温度值', type: 'number', unit: '°C' },
        { key: 'flow', label: '流量', placeholder: '请输入流量值', type: 'number', unit: 'm³/h' },
        { key: 'humidity', label: '湿度', placeholder: '请输入湿度值', type: 'number', unit: '%' },
        { key: 'remarks', label: '检测备注', placeholder: '请输入检测备注', type: 'text', unit: '' }
      ],
      
      // 采集的数据
      collectedData: {
        pressure: '',
        temperature: '',
        flow: '',
        humidity: '',
        remarks: ''
      },
      
      // 上传的媒体文件
      uploadedMedia: [],
      
      // 任务备注
      taskNotes: '',
      
      // 签名相关
      signatureContext: null,
      isDrawing: false,
      lastPoint: { x: 0, y: 0 }
    }
  },
  
  methods: {
    // 返回上一页
    goBack() {
      uni.navigateBack();
    },
    
    // 分享任务
    shareTask() {
      uni.showActionSheet({
        itemList: ['复制链接', '发送给同事', '导出报告'],
        success: (res) => {
          console.log('选择了第' + (res.tapIndex + 1) + '个按钮');
        }
      });
    },
    
    // 获取状态文本
    getStatusText(status) {
      const statusMap = {
        'published': '已发布',
        'accepted': '已接取',
        'waiting': '等待接取',
        'completed': '已完成',
        'overdue': '已超期'
      };
      return statusMap[status] || '未知';
    },
    
    // 获取优先级文本
    getPriorityText(priority) {
      const priorityMap = {
        'high': '高优先级',
        'medium': '中优先级',
        'low': '低优先级'
      };
      return priorityMap[priority] || '普通';
    },
    
    // 格式化日期时间
    formatDateTime(timeStr) {
      if (!timeStr) return '';
      const date = new Date(timeStr);
      const year = date.getFullYear();
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      const hours = date.getHours().toString().padStart(2, '0');
      const minutes = date.getMinutes().toString().padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}`;
    },
    
    // 完成步骤
    completeStep(index) {
      this.executionSteps[index].completed = true;
      this.executionSteps[index].current = false;
      
      // 设置下一步为当前步骤
      if (index + 1 < this.executionSteps.length) {
        this.executionSteps[index + 1].current = true;
      }
      
      uni.showToast({
        title: '步骤已完成',
        icon: 'success'
      });
    },
    
    // 保存数据
    saveData() {
      console.log('保存数据:', this.collectedData);
      uni.showToast({
        title: '数据已保存',
        icon: 'success'
      });
    },
    
    // 提交数据
    submitData() {
      // 验证必填字段
      const requiredFields = ['pressure', 'temperature', 'flow'];
      for (let field of requiredFields) {
        if (!this.collectedData[field]) {
          uni.showToast({
            title: `请填写${this.dataFields.find(f => f.key === field).label}`,
            icon: 'none'
          });
          return;
        }
      }
      
      console.log('提交数据:', this.collectedData);
      uni.showToast({
        title: '数据提交成功',
        icon: 'success'
      });
    },
    
    // 选择媒体文件
    chooseMedia() {
      uni.chooseImage({
        count: 9 - this.uploadedMedia.length,
        sizeType: ['original', 'compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          res.tempFilePaths.forEach(path => {
            this.uploadedMedia.push({
              url: path,
              type: 'image'
            });
          });
        }
      });
    },
    
    // 拍照
    takePhoto() {
      uni.chooseImage({
        count: 1,
        sourceType: ['camera'],
        success: (res) => {
          this.uploadedMedia.push({
            url: res.tempFilePaths[0],
            type: 'image'
          });
        }
      });
    },
    
    // 录像
    recordVideo() {
      uni.chooseVideo({
        sourceType: ['camera'],
        maxDuration: 60,
        success: (res) => {
          this.uploadedMedia.push({
            url: res.tempFilePath,
            type: 'video'
          });
        }
      });
    },
    
    // 从相册选择
    chooseFromAlbum() {
      uni.chooseImage({
        count: 9 - this.uploadedMedia.length,
        sourceType: ['album'],
        success: (res) => {
          res.tempFilePaths.forEach(path => {
            this.uploadedMedia.push({
              url: path,
              type: 'image'
            });
          });
        }
      });
    },
    
    // 预览媒体
    previewMedia(media) {
      if (media.type === 'image') {
        uni.previewImage({
          urls: this.uploadedMedia.filter(m => m.type === 'image').map(m => m.url),
          current: media.url
        });
      }
    },
    
    // 删除媒体
    deleteMedia(index) {
      uni.showModal({
        title: '确认删除',
        content: '确定要删除这个文件吗？',
        success: (res) => {
          if (res.confirm) {
            this.uploadedMedia.splice(index, 1);
          }
        }
      });
    },
    
    // 开始签名
    startSignature(e) {
      this.isDrawing = true;
      const touch = e.touches[0];
      this.lastPoint = {
        x: touch.x,
        y: touch.y
      };
    },
    
    // 绘制签名
    drawSignature(e) {
      if (!this.isDrawing) return;
      
      const touch = e.touches[0];
      const currentPoint = {
        x: touch.x,
        y: touch.y
      };
      
      this.signatureContext.beginPath();
      this.signatureContext.moveTo(this.lastPoint.x, this.lastPoint.y);
      this.signatureContext.lineTo(currentPoint.x, currentPoint.y);
      this.signatureContext.stroke();
      this.signatureContext.draw(true);
      
      this.lastPoint = currentPoint;
    },
    
    // 结束签名
    endSignature() {
      this.isDrawing = false;
    },
    
    // 清除签名
    clearSignature() {
      this.signatureContext.clearRect(0, 0, 300, 150);
      this.signatureContext.draw();
    },
    
    // 保存签名
    saveSignature() {
      uni.canvasToTempFilePath({
        canvasId: 'signatureCanvas',
        success: (res) => {
          console.log('签名保存成功:', res.tempFilePath);
          uni.showToast({
            title: '签名已保存',
            icon: 'success'
          });
        }
      });
    },
    
    // 保存草稿
    saveDraft() {
      const draftData = {
        taskId: this.taskId,
        collectedData: this.collectedData,
        taskNotes: this.taskNotes,
        uploadedMedia: this.uploadedMedia,
        executionSteps: this.executionSteps
      };
      
      console.log('保存草稿:', draftData);
      uni.showToast({
        title: '草稿已保存',
        icon: 'success'
      });
    },
    
    // 提交任务
    submitTask() {
      // 验证必要信息
      if (this.executionSteps.some(step => !step.completed)) {
        uni.showModal({
          title: '提示',
          content: '还有未完成的执行步骤，确定要提交吗？',
          success: (res) => {
            if (res.confirm) {
              this.doSubmitTask();
            }
          }
        });
        return;
      }
      
      this.doSubmitTask();
    },
    
    // 执行提交任务
    doSubmitTask() {
      const submitData = {
        taskId: this.taskId,
        collectedData: this.collectedData,
        taskNotes: this.taskNotes,
        uploadedMedia: this.uploadedMedia,
        executionSteps: this.executionSteps,
        submitTime: new Date().toISOString()
      };
      
      console.log('提交任务:', submitData);
      
      uni.showLoading({
        title: '提交中...'
      });
      
      // 模拟提交延迟
      setTimeout(() => {
        uni.hideLoading();
        uni.showToast({
          title: '任务提交成功',
          icon: 'success'
        });
        
        setTimeout(() => {
          uni.navigateBack();
        }, 1500);
      }, 2000);
    },
    
    // 加载任务详情
    loadTaskDetail() {
      // 模拟从API加载任务详情
      const mockData = {
        id: this.taskId,
        name: '西部管道A段巡检任务',
        type: '管道巡检',
        status: 'accepted',
        description: '对西部管道A段进行例行巡检，重点检查管道完整性、防腐层状况及周边环境安全。需要采集压力、温度、流量等关键数据，并拍摄现场照片记录管道状况。',
        location: '西部管道A段（桩号K10+500-K15+200）',
        startTime: '2024-01-15 09:00:00',
        endTime: '2024-01-15 17:00:00',
        assignee: '张三',
        priority: 'high'
      };
      
      this.taskDetail = mockData;
    }
  },
  
  onLoad(options) {
    this.taskId = options.id || 'T001';
    this.loadTaskDetail();
    
    // 初始化签名画布
    this.$nextTick(() => {
      this.signatureContext = uni.createCanvasContext('signatureCanvas');
      this.signatureContext.setStrokeStyle('#1A3366');
      this.signatureContext.setLineWidth(2);
      this.signatureContext.setLineCap('round');
      this.signatureContext.setLineJoin('round');
    });
  }
}
</script>

<style scoped>
.task-detail-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #F5F8FF 0%, #E8F2FF 100%);
}

.custom-navbar {
  height: 88rpx;
  background: linear-gradient(135deg, #1A3366 0%, #2B4A7A 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 44rpx 24rpx 0;
  box-shadow: 0 4rpx 20rpx rgba(26, 51, 102, 0.2);
  position: relative;
  z-index: 100;
}

.navbar-left, .navbar-right {
  width: 80rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-icon, .share-icon {
  color: #FFFFFF;
  font-size: 32rpx;
  font-weight: 600;
}

.navbar-title {
  color: #FFFFFF;
  font-size: 36rpx;
  font-weight: 600;
  font-family: PingFang SC, Microsoft YaHei, sans-serif;
  letter-spacing: 1rpx;
}

.content {
  height: calc(100vh - 88rpx);
  padding: 24rpx;
  padding-bottom: 120rpx;
}

/* 通用样式 */
.section-header {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
  padding: 0 8rpx;
}

.header-icon {
  font-size: 32rpx;
  margin-right: 12rpx;
}

.section-title {
  flex: 1;
  font-size: 32rpx;
  font-weight: 600;
  color: #1A3366;
}

.task-status {
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 24rpx;
  font-weight: 500;
}

.task-status.published {
  background: #E6F7FF;
  color: #1890FF;
  border: 1rpx solid #91D5FF;
}

.task-status.accepted {
  background: #F6FFED;
  color: #52C41A;
  border: 1rpx solid #B7EB8F;
}

.task-status.waiting {
  background: #FFFBE6;
  color: #FAAD14;
  border: 1rpx solid #FFE58F;
}

.task-status.completed {
  background: #E6FFFB;
  color: #13C2C2;
  border: 1rpx solid #87E8DE;
}

.task-status.overdue {
  background: #FFF1F0;
  color: #FF4D4F;
  border: 1rpx solid #FFCCC7;
}

/* 任务信息样式 */
.task-info-section {
  margin-bottom: 40rpx;
}

.info-card {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(26, 51, 102, 0.1);
}

.info-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 24rpx;
  min-height: 48rpx;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  width: 160rpx;
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
  flex-shrink: 0;
}

.info-value {
  flex: 1;
  font-size: 28rpx;
  color: #333;
  line-height: 1.5;
  word-break: break-all;
}

.info-value.description {
  line-height: 1.6;
}

.info-value.location {
  color: #4A90E2;
}

.priority-badge {
  padding: 6rpx 16rpx;
  border-radius: 16rpx;
  font-size: 24rpx;
  font-weight: 500;
}

.priority-badge.high {
  background: #FFF1F0;
  color: #FF4D4F;
  border: 1rpx solid #FFCCC7;
}

.priority-badge.medium {
  background: #FFFBE6;
  color: #FAAD14;
  border: 1rpx solid #FFE58F;
}

.priority-badge.low {
  background: #F6FFED;
  color: #52C41A;
  border: 1rpx solid #B7EB8F;
}

/* 执行步骤样式 */
.steps-section {
  margin-bottom: 40rpx;
}

.steps-container {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(26, 51, 102, 0.1);
}

.step-item {
  display: flex;
  margin-bottom: 32rpx;
  position: relative;
}

.step-item:last-child {
  margin-bottom: 0;
}

.step-item:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 32rpx;
  top: 64rpx;
  width: 2rpx;
  height: 40rpx;
  background: #E8E8E8;
}

.step-item.completed::after {
  background: #52C41A;
}

.step-item.current::after {
  background: #4A90E2;
}

.step-number {
  width: 64rpx;
  height: 64rpx;
  border-radius: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
  flex-shrink: 0;
  background: #F0F0F0;
  border: 2rpx solid #E8E8E8;
}

.step-item.completed .step-number {
  background: #52C41A;
  border-color: #52C41A;
}

.step-item.current .step-number {
  background: #4A90E2;
  border-color: #4A90E2;
}

.step-text {
  font-size: 24rpx;
  color: #666;
  font-weight: 600;
}

.step-item.completed .step-text,
.step-item.current .step-text {
  color: #FFFFFF;
}

.step-check {
  font-size: 28rpx;
  color: #FFFFFF;
  font-weight: 600;
}

.step-content {
  flex: 1;
  padding-top: 8rpx;
}

.step-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 8rpx;
}

.step-desc {
  font-size: 24rpx;
  color: #666;
  line-height: 1.5;
  display: block;
  margin-bottom: 16rpx;
}

.step-actions {
  margin-top: 16rpx;
}

.step-btn {
  background: linear-gradient(135deg, #4A90E2 0%, #357ABD 100%);
  border-radius: 12rpx;
  padding: 16rpx 32rpx;
  display: inline-block;
}

.step-btn .btn-text {
  color: #FFFFFF;
  font-size: 24rpx;
  font-weight: 500;
}

/* 数据采集样式 */
.data-collection-section {
  margin-bottom: 40rpx;
}

.data-form {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(26, 51, 102, 0.1);
}

.form-item {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
  min-height: 80rpx;
}

.form-item:last-child {
  margin-bottom: 32rpx;
}

.field-label {
  width: 160rpx;
  font-size: 26rpx;
  color: #333;
  flex-shrink: 0;
}

.field-input {
  flex: 1;
  background: #F8FAFF;
  border: 2rpx solid #E8F2FF;
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  font-size: 26rpx;
  margin-right: 16rpx;
}

.field-input:focus {
  border-color: #4A90E2;
  background: #F0F7FF;
}

.field-unit {
  width: 80rpx;
  font-size: 24rpx;
  color: #666;
  text-align: center;
}

.data-actions {
  display: flex;
  gap: 20rpx;
}

.data-btn {
  flex: 1;
  padding: 24rpx;
  border-radius: 12rpx;
  text-align: center;
  transition: all 0.3s ease;
}

.data-btn:active {
  transform: scale(0.95);
}

.data-btn.save {
  background: #F5F5F5;
  border: 2rpx solid #D9D9D9;
}

.data-btn.submit {
  background: linear-gradient(135deg, #4A90E2 0%, #357ABD 100%);
}

.data-btn.save .btn-text {
  color: #666;
}

.data-btn.submit .btn-text {
  color: #FFFFFF;
}

/* 媒体上传样式 */
.media-upload-section {
  margin-bottom: 40rpx;
}

.upload-container {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(26, 51, 102, 0.1);
}

.upload-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 32rpx;
}

.upload-item {
  width: 200rpx;
  height: 200rpx;
  position: relative;
  border-radius: 12rpx;
  overflow: hidden;
}

.media-preview {
  width: 100%;
  height: 100%;
  border-radius: 12rpx;
}

.media-delete {
  position: absolute;
  top: 8rpx;
  right: 8rpx;
  width: 48rpx;
  height: 48rpx;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.delete-icon {
  color: #FFFFFF;
  font-size: 32rpx;
  font-weight: 600;
}

.upload-add {
  width: 200rpx;
  height: 200rpx;
  border: 2rpx dashed #D9D9D9;
  border-radius: 12rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #FAFAFA;
}

.add-icon {
  font-size: 48rpx;
  color: #999;
  margin-bottom: 8rpx;
}

.add-text {
  font-size: 24rpx;
  color: #999;
}

.upload-actions {
  display: flex;
  gap: 16rpx;
}

.upload-btn {
  flex: 1;
  padding: 20rpx;
  background: linear-gradient(135deg, #4A90E2 0%, #357ABD 100%);
  border-radius: 12rpx;
  text-align: center;
}

.upload-btn .btn-text {
  color: #FFFFFF;
  font-size: 24rpx;
  font-weight: 500;
}

/* 备注样式 */
.notes-section {
  margin-bottom: 40rpx;
}

.notes-container {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(26, 51, 102, 0.1);
}

.notes-textarea {
  width: 100%;
  min-height: 200rpx;
  background: #F8FAFF;
  border: 2rpx solid #E8F2FF;
  border-radius: 12rpx;
  padding: 20rpx;
  font-size: 26rpx;
  line-height: 1.5;
  resize: none;
}

.notes-textarea:focus {
  border-color: #4A90E2;
  background: #F0F7FF;
}

.notes-counter {
  text-align: right;
  margin-top: 12rpx;
}

.counter-text {
  font-size: 22rpx;
  color: #999;
}

/* 签名样式 */
.signature-section {
  margin-bottom: 40rpx;
}

.signature-container {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(26, 51, 102, 0.1);
}

.signature-canvas {
  width: 100%;
  height: 300rpx;
  background: #F8FAFF;
  border: 2rpx solid #E8F2FF;
  border-radius: 12rpx;
  margin-bottom: 24rpx;
}

.signature-actions {
  display: flex;
  gap: 20rpx;
}

.signature-btn {
  flex: 1;
  padding: 20rpx;
  border-radius: 12rpx;
  text-align: center;
}

.signature-btn.clear {
  background: #F5F5F5;
  border: 2rpx solid #D9D9D9;
}

.signature-btn.save {
  background: linear-gradient(135deg, #4A90E2 0%, #357ABD 100%);
}

.signature-btn.clear .btn-text {
  color: #666;
}

.signature-btn.save .btn-text {
  color: #FFFFFF;
}

/* 底部操作按钮 */
.bottom-actions {
  display: flex;
  gap: 20rpx;
  padding: 32rpx 0;
}

.action-btn {
  flex: 1;
  padding: 32rpx;
  border-radius: 16rpx;
  text-align: center;
  font-weight: 600;
  transition: all 0.3s ease;
}

.action-btn:active {
  transform: scale(0.95);
}

.action-btn.secondary {
  background: #F5F5F5;
  border: 2rpx solid #D9D9D9;
}

.action-btn.primary {
  background: linear-gradient(135deg, #4A90E2 0%, #357ABD 100%);
  box-shadow: 0 6rpx 20rpx rgba(74, 144, 226, 0.3);
}

.action-btn.secondary .btn-text {
  color: #666;
  font-size: 32rpx;
}

.action-btn.primary .btn-text {
  color: #FFFFFF;
  font-size: 32rpx;
}

.btn-text {
  font-size: 28rpx;
  font-weight: 500;
}
</style>