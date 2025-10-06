<template>
  <div class="task-details">
    <div class="main-content">
      <!-- 左侧图表区域 -->
      <div class="left-charts">
        <!-- 任务状态分布饼图 - 占据左上大部分 -->
        <div class="status-pie-chart">
          <div class="chart-header">
            <h3>任务状态分布</h3>
            <div class="chart-filters">
              <label>区域筛选：</label>
              <select v-model="selectedArea" @change="updateCharts">
                <option value="">全部区域</option>
                <option v-for="area in areas" :key="area" :value="area">{{ area }}</option>
              </select>
            </div>
          </div>
          <div ref="statusPieChart" class="chart-container"></div>
        </div>
        
        <!-- 底部小图表区域 -->
        <div class="bottom-charts">
          <!-- 任务数量折线图 -->
          <div class="line-chart">
            <div class="chart-header">
              <h4>任务数量趋势</h4>
              <div class="chart-filters">
                <select v-model="selectedAreaForLine" @change="updateLineChart">
                  <option value="">选择区域</option>
                  <option v-for="area in areas" :key="area" :value="area">{{ area }}</option>
                </select>
                <select v-model="lineDays" @change="updateLineChart">
                  <option :value="7">7天</option>
                  <option :value="14">14天</option>
                  <option :value="30">30天</option>
                </select>
              </div>
            </div>
            <div ref="lineChart" class="chart-container"></div>
          </div>
          
          <!-- 任务数量柱状图 -->
          <div class="bar-chart">
            <div class="chart-header">
              <h4>区域任务对比</h4>
              <div class="chart-filters">
                <input type="date" v-model="selectedDate" @change="updateBarChart" />
              </div>
            </div>
            <div ref="barChart" class="chart-container"></div>
          </div>
        </div>
      </div>
      
      <!-- 右侧KPI榜区域 -->
      <div class="right-kpi">
        <div class="kpi-header">
          <h3>检修员工作KPI</h3>
          <div class="kpi-tabs">
            <button 
              v-for="tab in kpiTabs" 
              :key="tab.key"
              :class="['tab-btn', { active: activeKpiTab === tab.key }]"
              @click="switchKpiTab(tab.key)"
            >
              {{ tab.label }}
            </button>
          </div>
        </div>
        <div class="kpi-content">
          <div class="kpi-list">
            <div 
              v-for="(item, index) in currentKpiData" 
              :key="item.repairman_id"
              class="kpi-item"
              :class="{ 'top-performer': index < 3 }"
            >
              <div class="rank">{{ index + 1 }}</div>
              <div class="info">
                <div class="name">{{ item.name }}</div>
                <div class="value">{{ formatKpiValue(item.value) }}</div>
              </div>
              <div class="badge" v-if="index < 3">
                <span v-if="index === 0">🥇</span>
                <span v-else-if="index === 1">🥈</span>
                <span v-else-if="index === 2">🥉</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'

export default {
  name: 'TaskDetails',
  data() {
    return {
      // 图表实例
      chartInstances: {
        statusPie: null,
        line: null,
        bar: null
      },
      
      // 筛选条件
      selectedArea: '',
      selectedAreaForLine: '北京管道',
      lineDays: 7,
      selectedDate: new Date().toISOString().split('T')[0],
      
      // 区域列表
      areas: ['北京管道', '上海管道', '广州管道', '深圳管道', '成都管道'],
      
      // KPI相关
      activeKpiTab: 'completed',
      kpiTabs: [
        { key: 'completed', label: '完成任务数' },
        { key: 'response', label: '平均响应时间' },
        { key: 'completion', label: '平均完成时间' }
      ],
      
      // 模拟数据
      taskData: [],
      repairmanData: []
    }
  },
  
  computed: {
    currentKpiData() {
      const data = this.calculateKpiData()
      return data[this.activeKpiTab] || []
    }
  },
  
  mounted() {
    this.generateMockData()
    // 延迟初始化图表，确保DOM已完全渲染
    setTimeout(() => {
      this.initCharts()
      this.updateCharts()
    }, 100)
    window.addEventListener('resize', this.handleResize)
  },
  
  beforeDestroy() {
    this.disposeCharts()
    window.removeEventListener('resize', this.handleResize)
  },
  
  methods: {
    generateMockData() {
      // 生成检修员数据
      this.repairmanData = [
        { id: 1, name: '张三', phone: '13800138001' },
        { id: 2, name: '李四', phone: '13800138002' },
        { id: 3, name: '王五', phone: '13800138003' },
        { id: 4, name: '赵六', phone: '13800138004' },
        { id: 5, name: '钱七', phone: '13800138005' },
        { id: 6, name: '孙八', phone: '13800138006' },
        { id: 7, name: '周九', phone: '13800138007' },
        { id: 8, name: '吴十', phone: '13800138008' }
      ]
      
      // 生成任务数据
      this.taskData = []
      const statuses = [0, 1, 2, 3] // 已发布、已接取、已完成、异常
      const statusNames = ['已发布', '已接取', '已完成', '异常']
      
      for (let i = 1; i <= 200; i++) {
        const publicTime = new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000)
        const responseTime = Math.random() > 0.3 ? new Date(publicTime.getTime() + Math.random() * 2 * 60 * 60 * 1000) : null
        const accomplishTime = Math.random() > 0.5 && responseTime ? new Date(responseTime.getTime() + Math.random() * 8 * 60 * 60 * 1000) : null
        
        const status = statuses[Math.floor(Math.random() * statuses.length)]
        const repairman = this.repairmanData[Math.floor(Math.random() * this.repairmanData.length)]
        const area = this.areas[Math.floor(Math.random() * this.areas.length)]
        
        this.taskData.push({
          id: i,
          inspection_id: 1000 + i,
          repairman_id: repairman.id,
          repairman_name: repairman.name,
          result: status,
          result_name: statusNames[status],
          area: area,
          public_time: publicTime,
          response_time: responseTime,
          accomplish_time: accomplishTime,
          feedback_information: Math.random() > 0.7 ? `任务${i}的反馈信息` : null,
          create_time: publicTime,
          update_time: new Date()
        })
      }
    },
    
    initCharts() {
      this.$nextTick(() => {
        // 检查DOM元素是否存在且有有效的尺寸
        if (this.$refs.statusPieChart && this.$refs.statusPieChart.clientWidth > 0) {
          this.chartInstances.statusPie = echarts.init(this.$refs.statusPieChart)
        } else if (this.$refs.statusPieChart) {
          // 如果DOM存在但尺寸为0，延迟重试
          setTimeout(() => {
            if (this.$refs.statusPieChart && this.$refs.statusPieChart.clientWidth > 0) {
              this.chartInstances.statusPie = echarts.init(this.$refs.statusPieChart)
              this.updateStatusPieChart()
            }
          }, 200)
        }
        
        if (this.$refs.lineChart && this.$refs.lineChart.clientWidth > 0) {
          this.chartInstances.line = echarts.init(this.$refs.lineChart)
        } else if (this.$refs.lineChart) {
          setTimeout(() => {
            if (this.$refs.lineChart && this.$refs.lineChart.clientWidth > 0) {
              this.chartInstances.line = echarts.init(this.$refs.lineChart)
              this.updateLineChart()
            }
          }, 200)
        }
        
        if (this.$refs.barChart && this.$refs.barChart.clientWidth > 0) {
          this.chartInstances.bar = echarts.init(this.$refs.barChart)
        } else if (this.$refs.barChart) {
          setTimeout(() => {
            if (this.$refs.barChart && this.$refs.barChart.clientWidth > 0) {
              this.chartInstances.bar = echarts.init(this.$refs.barChart)
              this.updateBarChart()
            }
          }, 200)
        }
      })
    },
    
    disposeCharts() {
      Object.values(this.chartInstances).forEach(chart => {
        if (chart) chart.dispose()
      })
    },
    
    handleResize() {
      Object.values(this.chartInstances).forEach(chart => {
        if (chart) chart.resize()
      })
    },
    
    updateCharts() {
      // 延迟更新图表，确保图表实例已创建
      setTimeout(() => {
        this.updateStatusPieChart()
        this.updateLineChart()
        this.updateBarChart()
      }, 50)
    },
    
    updateStatusPieChart() {
      if (!this.chartInstances.statusPie) return
      
      const filteredData = this.selectedArea 
        ? this.taskData.filter(task => task.area === this.selectedArea)
        : this.taskData
      
      const statusCount = {}
      filteredData.forEach(task => {
        statusCount[task.result_name] = (statusCount[task.result_name] || 0) + 1
      })
      
      const pieData = Object.entries(statusCount).map(([name, value]) => ({ name, value }))
      
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
          bottom: 10,
          left: 'center'
        },
        series: [{
          name: '任务状态',
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['50%', '45%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: false,
            position: 'center'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: '18',
              fontWeight: 'bold'
            }
          },
          labelLine: {
            show: false
          },
          data: pieData
        }]
      }
      
      this.chartInstances.statusPie.setOption(option)
    },
    
    updateLineChart() {
      if (!this.chartInstances.line || !this.selectedAreaForLine) return
      
      const endDate = new Date()
      endDate.setHours(23, 59, 59, 999) // 设置为当天结束时间
      const startDate = new Date(endDate.getTime() - this.lineDays * 24 * 60 * 60 * 1000)
      startDate.setHours(0, 0, 0, 0) // 设置为开始日期的开始时间
      
      const filteredData = this.taskData.filter(task => {
        const taskDate = new Date(task.public_time)
        return task.area === this.selectedAreaForLine &&
               taskDate >= startDate &&
               taskDate <= endDate
      })
      
      const dailyCount = {}
      for (let d = new Date(startDate); d <= endDate; d.setDate(d.getDate() + 1)) {
        const dateStr = d.toISOString().split('T')[0]
        dailyCount[dateStr] = 0
      }
      
      filteredData.forEach(task => {
        const dateStr = task.public_time.toISOString().split('T')[0]
        if (dailyCount.hasOwnProperty(dateStr)) {
          dailyCount[dateStr]++
        }
      })
      
      const dates = Object.keys(dailyCount).sort()
      const counts = dates.map(date => dailyCount[date])
      
      const option = {
        title: {
          text: `${this.selectedAreaForLine} - 近${this.lineDays}天任务趋势`,
          textStyle: {
            fontSize: 14,
            fontWeight: 'normal'
          },
          left: 'center',
          top: 10
        },
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            const date = params[0].axisValue
            const count = params[0].value
            return `${date}<br/>任务数量: ${count}`
          }
        },
        grid: {
          top: 50,
          left: 40,
          right: 20,
          bottom: 30
        },
        xAxis: {
          type: 'category',
          data: dates.map(date => {
            const d = new Date(date)
            return `${d.getMonth() + 1}/${d.getDate()}`
          }),
          axisLabel: {
            fontSize: 10
          }
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            fontSize: 10
          }
        },
        series: [{
          name: '任务数量',
          type: 'line',
          smooth: true,
          data: counts,
          itemStyle: {
            color: '#5470c6'
          },
          areaStyle: {
            opacity: 0.3,
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [{
                offset: 0, color: 'rgba(84, 112, 198, 0.3)'
              }, {
                offset: 1, color: 'rgba(84, 112, 198, 0.1)'
              }]
            }
          }
        }]
      }
      
      this.chartInstances.line.setOption(option)
    },
    
    updateBarChart() {
      if (!this.chartInstances.bar) return
      
      const selectedDate = new Date(this.selectedDate)
      const nextDate = new Date(selectedDate.getTime() + 24 * 60 * 60 * 1000)
      
      const filteredData = this.taskData.filter(task => 
        task.public_time >= selectedDate && task.public_time < nextDate
      )
      
      const areaCount = {}
      this.areas.forEach(area => {
        areaCount[area] = 0
      })
      
      filteredData.forEach(task => {
        if (areaCount.hasOwnProperty(task.area)) {
          areaCount[task.area]++
        }
      })
      
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        xAxis: {
          type: 'category',
          data: this.areas,
          axisLabel: {
            rotate: 45
          }
        },
        yAxis: {
          type: 'value'
        },
        series: [{
          name: '任务数量',
          type: 'bar',
          data: this.areas.map(area => areaCount[area]),
          itemStyle: {
            color: '#91cc75'
          }
        }]
      }
      
      this.chartInstances.bar.setOption(option)
    },
    
    calculateKpiData() {
      const kpiData = {
        completed: [],
        response: [],
        completion: []
      }
      
      this.repairmanData.forEach(repairman => {
        const tasks = this.taskData.filter(task => task.repairman_id === repairman.id)
        const completedTasks = tasks.filter(task => task.result === 2)
        
        // 完成任务数
        kpiData.completed.push({
          repairman_id: repairman.id,
          name: repairman.name,
          value: completedTasks.length
        })
        
        // 平均响应时间（小时）
        const responseTimes = tasks
          .filter(task => task.response_time && task.public_time)
          .map(task => (task.response_time.getTime() - task.public_time.getTime()) / (1000 * 60 * 60))
        
        const avgResponseTime = responseTimes.length > 0 
          ? responseTimes.reduce((a, b) => a + b, 0) / responseTimes.length 
          : 0
        
        kpiData.response.push({
          repairman_id: repairman.id,
          name: repairman.name,
          value: avgResponseTime
        })
        
        // 平均完成时间（小时）
        const completionTimes = completedTasks
          .filter(task => task.accomplish_time && task.public_time)
          .map(task => (task.accomplish_time.getTime() - task.public_time.getTime()) / (1000 * 60 * 60))
        
        const avgCompletionTime = completionTimes.length > 0 
          ? completionTimes.reduce((a, b) => a + b, 0) / completionTimes.length 
          : 0
        
        kpiData.completion.push({
          repairman_id: repairman.id,
          name: repairman.name,
          value: avgCompletionTime
        })
      })
      
      // 排序
      kpiData.completed.sort((a, b) => b.value - a.value)
      kpiData.response.sort((a, b) => a.value - b.value) // 响应时间越短越好
      kpiData.completion.sort((a, b) => a.value - b.value) // 完成时间越短越好
      
      return kpiData
    },
    
    switchKpiTab(tabKey) {
      this.activeKpiTab = tabKey
    },
    
    formatKpiValue(value) {
      if (this.activeKpiTab === 'completed') {
        return `${value} 个`
      } else {
        return `${value.toFixed(1)} 小时`
      }
    }
  }
}
</script>

<style scoped>
.task-details {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.main-content {
  display: flex;
  gap: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 左侧图表区域 */
.left-charts {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.status-pie-chart {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  height: 400px;
}

.bottom-charts {
  display: flex;
  gap: 20px;
}

.line-chart,
.bar-chart {
  flex: 1;
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  height: 300px;
}

/* 右侧KPI区域 */
.right-kpi {
  width: 300px;
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  height: 740px;
}

/* 图表头部 */
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.chart-header h3 {
  color: #1e3a8a;
  font-size: 18px;
  margin: 0;
}

.chart-header h4 {
  color: #1e3a8a;
  font-size: 16px;
  margin: 0;
}

.chart-filters {
  display: flex;
  gap: 10px;
  align-items: center;
}

.chart-filters label {
  font-size: 14px;
  color: #64748b;
}

.chart-filters select,
.chart-filters input {
  padding: 5px 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
}

.chart-container {
  height: calc(100% - 60px);
}

/* KPI区域样式 */
.kpi-header {
  margin-bottom: 20px;
}

.kpi-header h3 {
  color: #1e3a8a;
  font-size: 18px;
  margin: 0 0 15px 0;
}

.kpi-tabs {
  display: flex;
  gap: 5px;
}

.tab-btn {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  background: #f9fafb;
  color: #64748b;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s ease;
}

.tab-btn.active {
  background: #1e3a8a;
  color: white;
  border-color: #1e3a8a;
}

.tab-btn:hover:not(.active) {
  background: #e5e7eb;
}

.kpi-content {
  height: calc(100% - 100px);
  overflow-y: auto;
}

.kpi-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.kpi-item {
  display: flex;
  align-items: center;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  transition: all 0.3s ease;
}

.kpi-item.top-performer {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  border-color: #f59e0b;
}

.kpi-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.rank {
  width: 30px;
  height: 30px;
  background: #1e3a8a;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 14px;
  margin-right: 12px;
}

.top-performer .rank {
  background: #f59e0b;
}

.info {
  flex: 1;
}

.name {
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.value {
  font-size: 14px;
  color: #64748b;
}

.badge {
  font-size: 20px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
  }
  
  .right-kpi {
    width: 100%;
    height: 400px;
  }
  
  .kpi-tabs {
    justify-content: center;
  }
  
  .kpi-list {
    flex-direction: row;
    flex-wrap: wrap;
  }
  
  .kpi-item {
    flex: 1;
    min-width: 200px;
  }
}

@media (max-width: 768px) {
  .bottom-charts {
    flex-direction: column;
  }
  
  .chart-header {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
  
  .chart-filters {
    flex-wrap: wrap;
  }
}
</style>