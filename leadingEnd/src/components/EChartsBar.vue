<template>
  <div class="echarts-bar">
    <div ref="barChart" class="bar-chart"></div>
  </div>
</template>

<script>
import * as echarts from 'echarts'

export default {
  name: 'EChartsBar',
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
      this.chart = echarts.init(this.$refs.barChart)
      this.setChartOption()
    },
    
    setChartOption() {
      const total = this.data.reduce((sum, item) => sum + item.count, 0)
      
      const option = {
        backgroundColor: 'transparent',

        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(0, 0, 0, 0.9)',
          borderColor: '#1976d2',
          borderWidth: 1,
          textStyle: {
            color: '#ffffff',
            fontSize: 12
          },
          formatter: (params) => {
            const param = params[0]
            const item = this.data.find(d => d.label === param.name)
            const percentage = ((param.value / total) * 100).toFixed(1)
            return `
              <div style="padding: 8px;">
                <div style="color: #64b5f6; font-size: 14px; font-weight: bold; margin-bottom: 8px;">
                  ${param.name}
                </div>
                <div style="display: flex; flex-direction: column; gap: 4px;">
                  <div style="display: flex; justify-content: space-between;">
                    <span style="color: #b0bec5;">数量:</span>
                    <span style="color: #ffffff; font-weight: bold;">${param.value}</span>
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
        grid: {
          left: '15%',
          right: '10%',
          top: '10%',
          bottom: '15%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: this.data.map(item => item.label),
          axisLine: {
            lineStyle: {
              color: '#64748b'
            }
          },
          axisLabel: {
            color: '#64748b',
            fontSize: 12,
            fontWeight: 500
          },
          axisTick: {
            alignWithLabel: true,
            lineStyle: {
              color: '#64748b'
            }
          }
        },
        yAxis: {
          type: 'value',
          axisLine: {
            lineStyle: {
              color: '#64748b'
            }
          },
          axisLabel: {
            color: '#64748b',
            fontSize: 11,
            formatter: '{value}'
          },
          splitLine: {
            lineStyle: {
              color: '#e2e8f0',
              type: 'dashed'
            }
          }
        },
        series: [
          {
            name: this.title,
            type: 'bar',
            data: this.getChartData(),
            barWidth: '60%',
            itemStyle: {
              borderRadius: [4, 4, 0, 0],
              borderWidth: 1,
              borderColor: '#fff'
            },
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)',
                borderRadius: [6, 6, 0, 0]
              }
            },
            label: {
              show: true,
              position: 'top',
              color: '#64748b',
              fontSize: 11,
              fontWeight: 600,
              formatter: (params) => {
                const percentage = ((params.value / total) * 100).toFixed(1)
                return `${params.value}\n(${percentage}%)`
              }
            },
            animationDelay: (idx) => {
              return idx * 100
            }
          }
        ],
        animationType: 'scale',
        animationEasing: 'elasticOut',
        animationDuration: 1000
      }
      
      this.chart.setOption(option)
    },
    
    getChartData() {
      return this.data.map(item => ({
        value: item.count,
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              {
                offset: 0,
                color: item.color
              },
              {
                offset: 1,
                color: this.lightenColor(item.color, 0.3)
              }
            ]
          }
        }
      }))
    },
    
    // 颜色变亮函数
    lightenColor(color, amount) {
      const usePound = color[0] === '#'
      const col = usePound ? color.slice(1) : color
      const num = parseInt(col, 16)
      let r = (num >> 16) + amount * 255
      let g = (num >> 8 & 0x00FF) + amount * 255
      let b = (num & 0x0000FF) + amount * 255
      r = r > 255 ? 255 : r < 0 ? 0 : r
      g = g > 255 ? 255 : g < 0 ? 0 : g
      b = b > 255 ? 255 : b < 0 ? 0 : b
      return (usePound ? '#' : '') + (r << 16 | g << 8 | b).toString(16).padStart(6, '0')
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
.echarts-bar {
  width: 100%;
  height: 100%;
  position: relative;
}

.bar-chart {
  width: 100%;
  height: 100%;
}
</style>