<template>
  <div ref="mapChart" :style="{ width: width, height: height }"></div>
</template>

<script>
import * as echarts from 'echarts'

export default {
  name: 'EChartsMap',
  props: {
    width: {
      type: String,
      default: '100%'
    },
    height: {
      type: String,
      default: '400px'
    },
    mapData: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      chart: null
    }
  },
  mounted() {
    this.initChart()
  },
  beforeDestroy() {
    if (this.chart) {
      this.chart.dispose()
    }
  },
  watch: {
    mapData: {
      handler() {
        this.updateChart()
      },
      deep: true
    }
  },
  methods: {
    initChart() {
      this.chart = echarts.init(this.$refs.mapChart)
      this.updateChart()
    },
    updateChart() {
      if (!this.chart) return
      
      const option = {
        title: {
          text: '管道分布地图',
          left: 'center'
        },
        tooltip: {
          trigger: 'item'
        },
        geo: {
          map: 'china',
          roam: true,
          itemStyle: {
            areaColor: '#e7e8ea'
          },
          emphasis: {
            itemStyle: {
              areaColor: '#389bb7'
            }
          }
        },
        series: [
          {
            name: '管道分布',
            type: 'scatter',
            coordinateSystem: 'geo',
            data: this.mapData.points || [],
            symbolSize: 8,
            itemStyle: {
              color: '#c23531'
            }
          }
        ]
      }
      
      this.chart.setOption(option)
    }
  }
}
</script>

<style scoped>
/* 地图组件样式 */
</style>