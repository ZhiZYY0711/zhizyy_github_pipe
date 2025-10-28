<template>
  <div class="echarts-map">
    <div ref="mapChart" class="map-chart"></div>
  </div>
</template>

<script>
import * as echarts from 'echarts'

export default {
  name: 'EChartsMap',
  data() {
    return {
      chart: null,
      // 模拟省份数据
      provinceData: [
        { name: '北京', value: [116.46, 39.92, 156], sensorCount: 156, abnormalCount: 8, highRiskAlerts: 2, dangerAlerts: 5, activeTasks: 12, overdueTasks: 1 },
        { name: '天津', value: [117.2, 39.13, 89], sensorCount: 89, abnormalCount: 3, highRiskAlerts: 1, dangerAlerts: 2, activeTasks: 7, overdueTasks: 0 },
        { name: '河北', value: [114.48, 38.03, 234], sensorCount: 234, abnormalCount: 15, highRiskAlerts: 4, dangerAlerts: 8, activeTasks: 18, overdueTasks: 3 },
        { name: '山西', value: [112.53, 37.87, 178], sensorCount: 178, abnormalCount: 12, highRiskAlerts: 3, dangerAlerts: 6, activeTasks: 14, overdueTasks: 2 },
        { name: '内蒙古', value: [111.65, 40.82, 312], sensorCount: 312, abnormalCount: 18, highRiskAlerts: 5, dangerAlerts: 12, activeTasks: 25, overdueTasks: 4 },
        { name: '辽宁', value: [123.38, 41.8, 198], sensorCount: 198, abnormalCount: 9, highRiskAlerts: 2, dangerAlerts: 4, activeTasks: 16, overdueTasks: 1 },
        { name: '吉林', value: [125.35, 43.88, 145], sensorCount: 145, abnormalCount: 7, highRiskAlerts: 1, dangerAlerts: 3, activeTasks: 11, overdueTasks: 1 },
        { name: '黑龙江', value: [126.63, 45.75, 267], sensorCount: 267, abnormalCount: 14, highRiskAlerts: 3, dangerAlerts: 7, activeTasks: 20, overdueTasks: 2 },
        { name: '上海', value: [121.48, 31.22, 98], sensorCount: 98, abnormalCount: 4, highRiskAlerts: 1, dangerAlerts: 2, activeTasks: 8, overdueTasks: 0 },
        { name: '江苏', value: [118.78, 32.04, 289], sensorCount: 289, abnormalCount: 16, highRiskAlerts: 4, dangerAlerts: 9, activeTasks: 22, overdueTasks: 3 },
        { name: '浙江', value: [120.19, 30.26, 245], sensorCount: 245, abnormalCount: 11, highRiskAlerts: 3, dangerAlerts: 6, activeTasks: 19, overdueTasks: 2 },
        { name: '安徽', value: [117.27, 31.86, 187], sensorCount: 187, abnormalCount: 10, highRiskAlerts: 2, dangerAlerts: 5, activeTasks: 15, overdueTasks: 1 },
        { name: '福建', value: [119.3, 26.08, 156], sensorCount: 156, abnormalCount: 8, highRiskAlerts: 2, dangerAlerts: 4, activeTasks: 12, overdueTasks: 1 },
        { name: '江西', value: [115.89, 28.68, 134], sensorCount: 134, abnormalCount: 6, highRiskAlerts: 1, dangerAlerts: 3, activeTasks: 10, overdueTasks: 1 },
        { name: '山东', value: [117, 36.65, 298], sensorCount: 298, abnormalCount: 17, highRiskAlerts: 5, dangerAlerts: 10, activeTasks: 24, overdueTasks: 3 },
        { name: '河南', value: [113.65, 34.76, 276], sensorCount: 276, abnormalCount: 15, highRiskAlerts: 4, dangerAlerts: 8, activeTasks: 21, overdueTasks: 2 },
        { name: '湖北', value: [114.31, 30.52, 203], sensorCount: 203, abnormalCount: 11, highRiskAlerts: 3, dangerAlerts: 6, activeTasks: 16, overdueTasks: 2 },
        { name: '湖南', value: [113, 28.21, 189], sensorCount: 189, abnormalCount: 9, highRiskAlerts: 2, dangerAlerts: 5, activeTasks: 15, overdueTasks: 1 },
        { name: '广东', value: [113.23, 23.16, 345], sensorCount: 345, abnormalCount: 20, highRiskAlerts: 6, dangerAlerts: 13, activeTasks: 28, overdueTasks: 4 },
        { name: '广西', value: [108.33, 22.84, 167], sensorCount: 167, abnormalCount: 8, highRiskAlerts: 2, dangerAlerts: 4, activeTasks: 13, overdueTasks: 1 },
        { name: '海南', value: [110.35, 20.02, 78], sensorCount: 78, abnormalCount: 3, highRiskAlerts: 1, dangerAlerts: 2, activeTasks: 6, overdueTasks: 0 },
        { name: '重庆', value: [106.54, 29.59, 123], sensorCount: 123, abnormalCount: 6, highRiskAlerts: 2, dangerAlerts: 3, activeTasks: 10, overdueTasks: 1 },
        { name: '四川', value: [104.06, 30.67, 234], sensorCount: 234, abnormalCount: 13, highRiskAlerts: 4, dangerAlerts: 7, activeTasks: 19, overdueTasks: 2 },
        { name: '贵州', value: [106.71, 26.57, 145], sensorCount: 145, abnormalCount: 7, highRiskAlerts: 2, dangerAlerts: 4, activeTasks: 12, overdueTasks: 1 },
        { name: '云南', value: [102.73, 25.04, 198], sensorCount: 198, abnormalCount: 10, highRiskAlerts: 3, dangerAlerts: 6, activeTasks: 16, overdueTasks: 2 },
        { name: '西藏', value: [91.11, 29.97, 89], sensorCount: 89, abnormalCount: 4, highRiskAlerts: 1, dangerAlerts: 2, activeTasks: 7, overdueTasks: 0 },
        { name: '陕西', value: [108.95, 34.27, 167], sensorCount: 167, abnormalCount: 8, highRiskAlerts: 2, dangerAlerts: 4, activeTasks: 13, overdueTasks: 1 },
        { name: '甘肃', value: [103.73, 36.03, 134], sensorCount: 134, abnormalCount: 6, highRiskAlerts: 2, dangerAlerts: 3, activeTasks: 11, overdueTasks: 1 },
        { name: '青海', value: [101.74, 36.56, 67], sensorCount: 67, abnormalCount: 3, highRiskAlerts: 1, dangerAlerts: 1, activeTasks: 5, overdueTasks: 0 },
        { name: '宁夏', value: [106.27, 38.47, 56], sensorCount: 56, abnormalCount: 2, highRiskAlerts: 0, dangerAlerts: 1, activeTasks: 4, overdueTasks: 0 },
        { name: '新疆', value: [87.68, 43.77, 278], sensorCount: 278, abnormalCount: 14, highRiskAlerts: 4, dangerAlerts: 8, activeTasks: 22, overdueTasks: 3 }
      ]
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
  methods: {
    initChart() {
      this.chart = echarts.init(this.$refs.mapChart)
      
      // 注册中国地图
      this.registerChinaMap()
    },
    
    async registerChinaMap() {
      try {
        // 使用内置的中国地图数据
        const chinaJson = await import('@/modules/shared/mapjson/中华人民共和国.json')
        echarts.registerMap('china', chinaJson.default)
        this.setChartOption()
      } catch (error) {
        console.error('加载地图数据失败:', error)
        // 如果加载失败，使用ECharts内置的中国地图
        this.setChartOption()
      }
    },
    
    setChartOption() {
      const option = {
        backgroundColor: '#0a1929',
        title: {
          text: '油气管道监测地图',
          left: 'center',
          top: '20px',
          textStyle: {
            color: '#ffffff',
            fontSize: 18,
            fontWeight: 'bold'
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
            if (params.seriesType === 'map') {
              const data = this.provinceData.find(item => item.name === params.name)
              if (data) {
                return `
                  <div style="padding: 8px;">
                    <div style="color: #64b5f6; font-size: 14px; font-weight: bold; margin-bottom: 8px; border-bottom: 1px solid #1976d2; padding-bottom: 4px;">
                      ${params.name}
                    </div>
                    <div style="display: flex; flex-direction: column; gap: 4px;">
                      <div style="display: flex; justify-content: space-between;">
                        <span style="color: #b0bec5;">传感器数量:</span>
                        <span style="color: #ffffff; font-weight: bold;">${data.sensorCount}</span>
                      </div>
                      <div style="display: flex; justify-content: space-between;">
                        <span style="color: #b0bec5;">异常/离线数量:</span>
                        <span style="color: #f44336; font-weight: bold;">${data.abnormalCount}</span>
                      </div>
                      <div style="display: flex; justify-content: space-between;">
                        <span style="color: #b0bec5;">今日高危报警:</span>
                        <span style="color: #ff5722; font-weight: bold;">${data.highRiskAlerts}</span>
                      </div>
                      <div style="display: flex; justify-content: space-between;">
                        <span style="color: #b0bec5;">今日危险报警:</span>
                        <span style="color: #ff9800; font-weight: bold;">${data.dangerAlerts}</span>
                      </div>
                      <div style="display: flex; justify-content: space-between;">
                        <span style="color: #b0bec5;">进行中任务:</span>
                        <span style="color: #ffffff; font-weight: bold;">${data.activeTasks}</span>
                      </div>
                      <div style="display: flex; justify-content: space-between;">
                        <span style="color: #b0bec5;">超时未处理:</span>
                        <span style="color: #ff9800; font-weight: bold;">${data.overdueTasks}</span>
                      </div>
                    </div>
                  </div>
                `
              }
            } else if (params.seriesType === 'scatter') {
              const data = params.data
              return `
                <div style="padding: 8px;">
                  <div style="color: #64b5f6; font-size: 14px; font-weight: bold; margin-bottom: 8px;">
                    ${data.name}
                  </div>
                  <div style="color: #ffffff;">
                    传感器数量: <span style="font-weight: bold;">${data.sensorCount}</span>
                  </div>
                </div>
              `
            }
            return params.name
          }
        },
        visualMap: {
          min: 0,
          max: 350,
          left: 'left',
          top: 'bottom',
          text: ['高', '低'],
          textStyle: {
            color: '#ffffff'
          },
          inRange: {
            color: ['#1976d2', '#388e3c', '#fbc02d', '#f57c00', '#d32f2f']
          },
          calculable: true
        },
        geo: {
          map: 'china',
          roam: true,
          zoom: 1.2,
          center: [104, 35],
          itemStyle: {
            areaColor: '#1565c0',
            borderColor: '#1976d2',
            borderWidth: 1
          },
          emphasis: {
            itemStyle: {
              areaColor: '#1976d2',
              borderColor: '#ffffff',
              borderWidth: 2
            }
          },
          regions: this.getRegionsConfig()
        },
        series: [
          {
            name: '传感器分布',
            type: 'map',
            map: 'china',
            geoIndex: 0,
            data: this.getMapData(),
            itemStyle: {
              borderColor: '#1976d2',
              borderWidth: 1
            },
            emphasis: {
              itemStyle: {
                areaColor: '#1976d2',
                borderColor: '#ffffff',
                borderWidth: 2
              }
            }
          },
          {
            name: '监测点',
            type: 'scatter',
            coordinateSystem: 'geo',
            data: this.getScatterData(),
            symbolSize: (val) => {
              return Math.max(8, Math.min(20, val[2] / 20))
            },
            itemStyle: {
              color: '#ffeb3b',
              borderColor: '#ff9800',
              borderWidth: 1
            },
            emphasis: {
              itemStyle: {
                color: '#ffc107',
                borderColor: '#ff5722',
                borderWidth: 2
              }
            }
          }
        ]
      }
      
      this.chart.setOption(option)
      
      // 添加点击事件
      this.chart.on('click', (params) => {
        if (params.componentType === 'geo' || params.seriesType === 'map') {
          this.$emit('province-click', { name: params.name })
        }
      })
    },
    
    getMapData() {
      return this.provinceData.map(item => ({
        name: item.name,
        value: item.sensorCount
      }))
    },
    
    getScatterData() {
      return this.provinceData.map(item => ({
        name: item.name,
        value: item.value,
        sensorCount: item.sensorCount,
        abnormalCount: item.abnormalCount,
        highRiskAlerts: item.highRiskAlerts,
        dangerAlerts: item.dangerAlerts,
        activeTasks: item.activeTasks,
        overdueTasks: item.overdueTasks
      }))
    },
    
    getRegionsConfig() {
      return this.provinceData.map(item => {
        const abnormalRate = item.abnormalCount / item.sensorCount
        const alertSeverity = (item.highRiskAlerts * 2 + item.dangerAlerts) / item.sensorCount
        const riskScore = abnormalRate * 0.6 + alertSeverity * 0.4
        
        let color = '#1976d2' // 正常 - 蓝色
        if (riskScore > 0.15) color = '#d32f2f' // 高风险 - 深红色
        else if (riskScore > 0.1) color = '#f57c00'  // 中高风险 - 橙色
        else if (riskScore > 0.05) color = '#fbc02d' // 中风险 - 黄色
        else if (riskScore > 0.02) color = '#388e3c' // 低风险 - 绿色
        
        return {
          name: item.name,
          itemStyle: {
            areaColor: color
          }
        }
      })
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
.echarts-map {
  width: 100%;
  height: 100%;
  position: relative;
}

.map-chart {
  width: 100%;
  height: 100%;
}
</style>