<template>
  <div class="echarts-pie">
    <div ref="pieChart" class="pie-chart"></div>
  </div>
</template>

<script>
import * as echarts from 'echarts'

export default {
  name: 'EChartsPie',
  props: {
    data: {
      type: Array,
      default: () => [
        { level: 0, label: '安全', count: 120, color: '#10b981' },
        { level: 1, label: '良好', count: 80, color: '#3b82f6' },
        { level: 2, label: '危险', count: 30, color: '#f59e0b' },
        { level: 3, label: '高危', count: 15, color: '#dc2626' }
      ]
    },
    title: {
      type: String,
      default: '传感器状态分布'
    }
  },
  data() {
    return {
      chart: null
    }
  },
  mounted() {
    this.initChart()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    if (this.chart) {
      this.chart.dispose()
    }
    window.removeEventListener('resize', this.handleResize)
  },
  watch: {
    data: {
      handler() {
        this.updateChart()
      },
      deep: true
    }
  },
  methods: {
    initChart() {
      this.chart = echarts.init(this.$refs.pieChart)
      this.setChartOption()
    },
    
    setChartOption() {
      const total = this.data.reduce((sum, item) => sum + item.count, 0)
      
      const option = {
        backgroundColor: 'transparent',
        title: {
          text: this.title,
          left: 'center',
          top: '10px',
          textStyle: {
            color: '#64748b',
            fontSize: 14,
            fontWeight: 600
          }
        },
        tooltip: {
          trigger: 'item',
          backgroundColor: 'rgba(0, 0, 0, 0.9)',
          borderColor: '#1976d2',
          borderWidth: 1,
          textStyle: {
            color: '#ffffff',
            fontSize: 12
          },
          formatter: (params) => {
            const percentage = ((params.value / total) * 100).toFixed(1)
            return `
              <div style="padding: 8px;">
                <div style="color: #64b5f6; font-size: 14px; font-weight: bold; margin-bottom: 8px;">
                  ${params.name}
                </div>
                <div style="display: flex; flex-direction: column; gap: 4px;">
                  <div style="display: flex; justify-content: space-between;">
                    <span style="color: #b0bec5;">数量:</span>
                    <span style="color: #ffffff; font-weight: bold;">${params.value}</span>
                  </div>
                  <div style="display: flex; justify-content: space-between;">
                    <span style="color: #b0bec5;">占比:</span>
                    <span style="color: #ffffff; font-weight: bold;">${percentage}%</span>
                  </div>
                </div>
              </div>
            `
          }
        },
        legend: {
          orient: 'vertical',
          left: 'right',
          top: 'center',
          textStyle: {
            color: '#64748b',
            fontSize: 12
          },
          formatter: (name) => {
            const item = this.data.find(d => d.label === name)
            if (item) {
              const percentage = ((item.count / total) * 100).toFixed(1)
              return `${name} (${percentage}%)`
            }
            return name
          }
        },
        series: [
          {
            name: this.title,
            type: 'pie',
            radius: ['30%', '70%'],
            center: ['40%', '55%'],
            roseType: 'area',
            itemStyle: {
              borderRadius: 5,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: true,
              position: 'outside',
              color: '#64748b',
              fontSize: 11,
              formatter: (params) => {
                const percentage = ((params.value / total) * 100).toFixed(1)
                return `${params.name}\n${percentage}%`
              }
            },
            labelLine: {
              show: true,
              length: 15,
              length2: 10,
              lineStyle: {
                color: '#64748b'
              }
            },
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              },
              label: {
                fontSize: 12,
                fontWeight: 'bold'
              }
            },
            data: this.getChartData()
          }
        ],
        animationType: 'scale',
        animationEasing: 'elasticOut',
        animationDelay: (idx) => {
          return Math.random() * 200
        }
      }
      
      this.chart.setOption(option)
    },
    
    getChartData() {
      return this.data.map(item => ({
        name: item.label,
        value: item.count,
        itemStyle: {
          color: item.color
        }
      }))
    },
    
    updateChart() {
      if (this.chart) {
        this.setChartOption()
      }
    },
    
    handleResize() {
      if (this.chart) {
        this.chart.resize()
      }
    }
  }
}
</script>

<style scoped>
.echarts-pie {
  width: 100%;
  height: 100%;
  position: relative;
}

.pie-chart {
  width: 100%;
  height: 100%;
}
</style>