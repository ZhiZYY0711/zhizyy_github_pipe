<template>
  <div class="log-record">
    <div class="log-header">
      <h2>日志记录管理</h2>
      <div class="log-nav">
        <button 
          class="nav-btn"
          :class="{ 'active': currentTab === 'inspection' }"
          @click="switchTab('inspection')"
        >
          <i class="icon-monitor"></i>数据监测日志
        </button>
        <button 
          class="nav-btn"
          :class="{ 'active': currentTab === 'sensor' }"
          @click="switchTab('sensor')"
        >
          <i class="icon-device"></i>设备信息日志
        </button>
      </div>
    </div>

    <div class="log-content">
      <!-- 数据监测日志 -->
      <div v-if="currentTab === 'inspection'" class="tab-content">
        <InspectionLog />
      </div>

      <!-- 设备信息日志 -->
      <div v-if="currentTab === 'sensor'" class="tab-content">
        <SensorLog />
      </div>
    </div>
  </div>
</template>

<script>
import InspectionLog from './InspectionLog.vue'
import SensorLog from './SensorLog.vue'

export default {
  name: 'LogRecord',
  components: {
    InspectionLog,
    SensorLog
  },
  data() {
    return {
      currentTab: 'inspection'
    }
  },
  methods: {
    switchTab(tab) {
      this.currentTab = tab
    }
  }
}
</script>

<style scoped>
.log-record {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
}

.log-header {
  background: white;
  padding: 20px;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.log-header h2 {
  margin: 0 0 20px 0;
  color: #1E3A8A;
  font-size: 24px;
  font-weight: 600;
}

.log-nav {
  display: flex;
  gap: 0;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #f8f9fa;
}

.nav-btn {
  flex: 1;
  padding: 12px 20px;
  border: none;
  background: #f8f9fa;
  color: #6b7280;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  position: relative;
}

.nav-btn:hover {
  background: #e5e7eb;
  color: #374151;
}

.nav-btn.active {
  background: #1E3A8A;
  color: white;
  box-shadow: 0 2px 4px rgba(30, 58, 138, 0.3);
}

.nav-btn.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: #3B82F6;
}

.icon-monitor::before {
  content: '📊';
  margin-right: 5px;
}

.icon-device::before {
  content: '🔧';
  margin-right: 5px;
}

.log-content {
  flex: 1;
  overflow: hidden;
}

.tab-content {
  height: 100%;
  overflow: hidden;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .log-header {
    padding: 15px;
  }
  
  .log-header h2 {
    font-size: 20px;
    margin-bottom: 15px;
  }
  
  .log-nav {
    flex-direction: column;
  }
  
  .nav-btn {
    padding: 10px 15px;
    font-size: 13px;
  }
  
  .nav-btn.active::after {
    height: 2px;
  }
}

@media (max-width: 480px) {
  .log-header {
    padding: 10px;
  }
  
  .log-header h2 {
    font-size: 18px;
    margin-bottom: 10px;
  }
  
  .nav-btn {
    padding: 8px 12px;
    font-size: 12px;
  }
  
  .icon-monitor::before,
  .icon-device::before {
    margin-right: 3px;
  }
}
</style>