<template>
  <div class="flow-trend-chart">
    <div class="chart-header">
      <h3>各区域管道流量变化趋势</h3>
      <div class="chart-controls">
        <select v-model="selectedTimeRange" @change="updateChart" class="time-range-select">
          <option value="7">最近7天</option>
          <option value="30">最近30天</option>
          <option value="90">最近90天</option>
        </select>
      </div>
    </div>
    <div class="chart-container">
      <canvas ref="chartCanvas"></canvas>
    </div>
  </div>
</template>

<script>
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  LineController,
  Title,
  Tooltip,
  Legend,
} from 'chart.js'

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  LineController,
  Title,
  Tooltip,
  Legend
)

export default {
  name: 'FlowTrendChart',
  data() {
    return {
      selectedTimeRange: 7,
      chart: null,
      chartData: {
        labels: [],
        datasets: []
      },
      chartOptions: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: true,
            position: 'top'
          },
          tooltip: {
            mode: 'index',
            intersect: false,
            callbacks: {
              label: function(context) {
                return context.dataset.label + ': ' + context.parsed.y + ' m³/h'
              }
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              callback: function(value) {
                return value + ' m³/h'
              }
            },
            title: {
              display: true,
              text: '流量 (m³/h)'
            }
          },
          x: {
            title: {
              display: true,
              text: '时间'
            }
          }
        },
        interaction: {
          mode: 'nearest',
          intersect: false
        }
      }
    }
  },
  mounted() {
    this.initChart()
  },
  beforeDestroy() {
    if (this.chart) {
      this.chart.destroy()
    }
  },
  methods: {
    initChart() {
      this.generateMockData()
      this.createChart()
    },
    createChart() {
      const ctx = this.$refs.chartCanvas.getContext('2d')
      
      if (this.chart) {
        this.chart.destroy()
      }
      
      this.chart = new ChartJS(ctx, {
        type: 'line',
        data: this.chartData,
        options: this.chartOptions
      })
    },
    generateMockData() {
      // 生成模拟数据
      const areas = ['北区', '南区', '东区', '西区', '中心区']
      const colors = [
        'rgba(255, 99, 132, 1)',
        'rgba(54, 162, 235, 1)', 
        'rgba(255, 205, 86, 1)',
        'rgba(75, 192, 192, 1)',
        'rgba(153, 102, 255, 1)'
      ]
      const backgroundColors = [
        'rgba(255, 99, 132, 0.2)',
        'rgba(54, 162, 235, 0.2)',
        'rgba(255, 205, 86, 0.2)',
        'rgba(75, 192, 192, 0.2)',
        'rgba(153, 102, 255, 0.2)'
      ]

      // 生成时间标签
      const labels = []
      const now = new Date()
      for (let i = this.selectedTimeRange - 1; i >= 0; i--) {
        const date = new Date(now)
        date.setDate(date.getDate() - i)
        labels.push(date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }))
      }

      // 生成数据集
      const datasets = areas.map((area, index) => {
        const data = []
        let baseFlow = 80 + Math.random() * 40 // 基础流量 80-120
        
        for (let i = 0; i < this.selectedTimeRange; i++) {
          // 添加一些随机波动
          const variation = (Math.random() - 0.5) * 20
          baseFlow += variation * 0.1 // 缓慢变化
          baseFlow = Math.max(50, Math.min(150, baseFlow)) // 限制在合理范围内
          data.push(Math.round(baseFlow * 100) / 100)
        }

        return {
          label: area,
          data: data,
          borderColor: colors[index],
          backgroundColor: backgroundColors[index],
          borderWidth: 2,
          fill: false,
          tension: 0.1,
          pointRadius: 4,
          pointHoverRadius: 6
        }
      })

      this.chartData = {
        labels: labels,
        datasets: datasets
      }
    },
    updateChart() {
      this.generateMockData()
      if (this.chart) {
        this.chart.data = this.chartData
        this.chart.update()
      } else {
        this.createChart()
      }
    }
  }
}
</script>

<style scoped>
.flow-trend-chart {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  height: 100%;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e5e7eb;
}

.chart-header h3 {
  margin: 0;
  color: #1E3A8A;
  font-size: 18px;
  font-weight: 600;
}

.chart-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.time-range-select {
  padding: 6px 12px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: white;
  color: #374151;
  font-size: 14px;
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.time-range-select:hover {
  border-color: #1E3A8A;
}

.time-range-select:focus {
  outline: none;
  border-color: #1E3A8A;
  box-shadow: 0 0 0 2px rgba(30, 58, 138, 0.1);
}

.chart-container {
  position: relative;
  height: 300px;
  width: 100%;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chart-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .chart-header h3 {
    font-size: 16px;
  }
  
  .chart-container {
    height: 250px;
  }
}
</style>