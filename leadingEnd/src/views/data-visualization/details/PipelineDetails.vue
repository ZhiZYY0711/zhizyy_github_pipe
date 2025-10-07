<template>
  <div class="pipeline-details">
    <!-- 右上角返回按钮 -->
    <div class="back-button-container">
      <button class="back-button" @click="goBackToArea">
        <i class="back-icon">←</i>
        返回区域详情
      </button>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧地图区域 -->
      <div class="left-section">
        <div class="map-container">
          <div class="section-title">管道分布图</div>
          <div id="pipelineMap" class="map-chart"></div>
        </div>
        
        <!-- 条件查询模块移到地图下方 -->
        <div class="query-section">
          <div class="query-controls">
            <div class="query-item">
              <label>时间范围：</label>
              <select disabled>
                <option>最近24小时</option>
              </select>
            </div>
            <div class="query-item">
              <label>管道段：</label>
              <select disabled>
                <option>全部管道</option>
              </select>
            </div>
            <div class="query-item">
              <label>监测点：</label>
              <select disabled>
                <option>监测点A</option>
              </select>
            </div>
            <button class="query-btn" disabled>查询</button>
          </div>
        </div>
      </div>

      <!-- 右侧统计图表区域 -->
      <div class="right-section">

        <!-- 四个折线统计图 -->
        <div class="charts-grid">
          <div class="chart-item">
            <div class="chart-title">温度监测</div>
            <div id="temperatureChart" class="line-chart"></div>
          </div>
          <div class="chart-item">
            <div class="chart-title">流量监测</div>
            <div id="flowChart" class="line-chart"></div>
          </div>
          <div class="chart-item">
            <div class="chart-title">压力监测</div>
            <div id="pressureChart" class="line-chart"></div>
          </div>
          <div class="chart-item">
            <div class="chart-title">震动监测</div>
            <div id="vibrationChart" class="line-chart"></div>
          </div>
        </div>

        <!-- 指标卡区域 -->
        <div class="indicators-section">
          <div class="section-title">关键指标</div>
          <div class="indicators-grid">
            <div class="indicator-card">
              <div class="indicator-title">平均温度</div>
              <div class="indicator-value">{{ indicators.temperature }}°C</div>
              <div class="indicator-status" :class="indicators.temperatureStatus">{{ getStatusText(indicators.temperatureStatus) }}</div>
            </div>
            <div class="indicator-card">
              <div class="indicator-title">平均流量</div>
              <div class="indicator-value">{{ indicators.flow }}m³/h</div>
              <div class="indicator-status" :class="indicators.flowStatus">{{ getStatusText(indicators.flowStatus) }}</div>
            </div>
            <div class="indicator-card">
              <div class="indicator-title">平均压力</div>
              <div class="indicator-value">{{ indicators.pressure }}MPa</div>
              <div class="indicator-status" :class="indicators.pressureStatus">{{ getStatusText(indicators.pressureStatus) }}</div>
            </div>
            <div class="indicator-card">
              <div class="indicator-title">震动等级</div>
              <div class="indicator-value">{{ indicators.vibration }}级</div>
              <div class="indicator-status" :class="indicators.vibrationStatus">{{ getStatusText(indicators.vibrationStatus) }}</div>
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
  name: 'PipelineDetails',
  data() {
    return {
      mapChart: null,
      temperatureChart: null,
      flowChart: null,
      pressureChart: null,
      vibrationChart: null,
      indicators: {
        temperature: 28.5,
        temperatureStatus: 'normal',
        flow: 1250.8,
        flowStatus: 'normal',
        pressure: 4.2,
        pressureStatus: 'warning',
        vibration: 1,
        vibrationStatus: 'normal'
      }
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initCharts()
    })
  },
  beforeDestroy() {
    // 销毁图表实例
    if (this.mapChart) this.mapChart.dispose()
    if (this.temperatureChart) this.temperatureChart.dispose()
    if (this.flowChart) this.flowChart.dispose()
    if (this.pressureChart) this.pressureChart.dispose()
    if (this.vibrationChart) this.vibrationChart.dispose()
  },
  methods: {
    initCharts() {
      this.initMapChart()
      this.initLineCharts()
    },
    
    // 初始化地图
    async initMapChart() {
      const mapElement = document.getElementById('pipelineMap')
      if (!mapElement) return
      
      this.mapChart = echarts.init(mapElement)
      
      try {
        // 加载中华人民共和国地图数据
        const response = await fetch('/src/assets/mapjson/中华人民共和国.json')
        const chinaMapData = await response.json()
        
        // 注册地图
        echarts.registerMap('china', chinaMapData)
        
        const mapOption = {
          title: {
            text: '全国管道分布图',
            left: 'center',
            textStyle: {
              fontSize: 16,
              color: '#333',
              fontWeight: 'bold'
            }
          },
          tooltip: {
            trigger: 'item',
            formatter: function(params) {
              if (params.seriesType === 'map') {
                return `${params.name}<br/>管道数量: ${params.value || 0}条`
              } else if (params.seriesName === '重要管道') {
                return `${params.name}<br/>坐标: [${params.value[0]}, ${params.value[1]}]`
              }
              return params.name
            }
          },
          visualMap: {
            min: 0,
            max: 10,
            left: 'left',
            top: 'bottom',
            text: ['高', '低'],
            calculable: true,
            inRange: {
              color: ['#e0f3ff', '#006edd']
            }
          },
          series: [
            {
              name: '管道分布',
              type: 'map',
              map: 'china',
              roam: true,
              emphasis: {
                label: {
                  show: true
                }
              },
              data: [
                { name: '北京市', value: 8 },
                { name: '天津市', value: 6 },
                { name: '河北省', value: 9 },
                { name: '山西省', value: 7 },
                { name: '内蒙古自治区', value: 5 },
                { name: '辽宁省', value: 6 },
                { name: '吉林省', value: 4 },
                { name: '黑龙江省', value: 5 },
                { name: '上海市', value: 3 },
                { name: '江苏省', value: 8 },
                { name: '浙江省', value: 6 },
                { name: '安徽省', value: 5 },
                { name: '福建省', value: 4 },
                { name: '江西省', value: 3 },
                { name: '山东省', value: 9 },
                { name: '河南省', value: 7 },
                { name: '湖北省', value: 6 },
                { name: '湖南省', value: 5 },
                { name: '广东省', value: 8 },
                { name: '广西壮族自治区', value: 4 },
                { name: '海南省', value: 2 },
                { name: '重庆市', value: 5 },
                { name: '四川省', value: 6 },
                { name: '贵州省', value: 3 },
                { name: '云南省', value: 4 },
                { name: '西藏自治区', value: 1 },
                { name: '陕西省', value: 6 },
                { name: '甘肃省', value: 4 },
                { name: '青海省', value: 2 },
                { name: '宁夏回族自治区', value: 3 },
                { name: '新疆维吾尔自治区', value: 5 }
              ]
            },
            {
              name: '重要管道',
              type: 'scatter',
              coordinateSystem: 'geo',
              data: [
                { name: '西气东输', value: [108.948024, 34.263161] },
                { name: '川气东送', value: [106.504962, 29.533155] },
                { name: '陕京管道', value: [108.948024, 39.904989] },
                { name: '中缅管道', value: [102.712251, 25.040609] }
              ],
              symbolSize: 12,
              itemStyle: {
                color: '#ff4d4f'
              },
              emphasis: {
                itemStyle: {
                  color: '#ff7875'
                }
              }
            }
          ],
          geo: {
            map: 'china',
            roam: true,
            itemStyle: {
              areaColor: '#f3f3f3',
              borderColor: '#999'
            },
            emphasis: {
              itemStyle: {
                areaColor: '#e6f7ff'
              }
            }
          }
        }
        
        this.mapChart.setOption(mapOption)
      } catch (error) {
        console.error('地图数据加载失败:', error)
        // 如果地图数据加载失败，显示简单的示意图
        this.initFallbackMap()
      }
    },
    
    // 备用地图（如果JSON加载失败）
    initFallbackMap() {
      const mapOption = {
        title: {
          text: '管道网络分布',
          left: 'center',
          textStyle: {
            fontSize: 14,
            color: '#333'
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'value',
          min: 0,
          max: 100,
          axisLabel: {
            formatter: '{value}km'
          }
        },
        yAxis: {
          type: 'value',
          min: 0,
          max: 100,
          axisLabel: {
            formatter: '{value}km'
          }
        },
        series: [
          {
            name: '管道',
            type: 'line',
            data: [
              [10, 20], [20, 30], [30, 25], [40, 35], [50, 40],
              [60, 45], [70, 50], [80, 55], [90, 60]
            ],
            lineStyle: {
              color: '#1890ff',
              width: 4
            },
            symbol: 'circle',
            symbolSize: 8,
            itemStyle: {
              color: '#ff4d4f'
            }
          },
          {
            name: '监测点',
            type: 'scatter',
            data: [
              [25, 28], [45, 38], [65, 48], [85, 58]
            ],
            symbolSize: 12,
            itemStyle: {
              color: '#52c41a'
            }
          }
        ],
        legend: {
          data: ['管道', '监测点'],
          bottom: 10
        },
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            if (params.seriesName === '管道') {
              return `管道位置: (${params.data[0]}km, ${params.data[1]}km)`
            } else {
              return `监测点: (${params.data[0]}km, ${params.data[1]}km)`
            }
          }
        }
      }
      
      this.mapChart.setOption(mapOption)
    },
    
    // 返回区域详情页面
    goBackToArea() {
      this.$emit('switch-to-area')
    },
    
    getStatusText(status) {
      const statusMap = {
        'normal': '正常',
        'warning': '警告',
        'danger': '危险'
      }
      return statusMap[status] || '未知'
    },
    
    // 初始化折线图
    initLineCharts() {
      this.initTemperatureChart()
      this.initFlowChart()
      this.initPressureChart()
      this.initVibrationChart()
    },
    
    // 温度图表
    initTemperatureChart() {
      const element = document.getElementById('temperatureChart')
      if (!element) return
      
      this.temperatureChart = echarts.init(element)
      
      const option = {
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00']
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            formatter: '{value}°C'
          }
        },
        series: [{
          data: [25, 26, 28, 32, 30, 28, 26],
          type: 'line',
          smooth: true,
          lineStyle: {
            color: '#ff7875'
          },
          itemStyle: {
            color: '#ff7875'
          },
          areaStyle: {
            color: 'rgba(255, 120, 117, 0.1)'
          }
        }],
        tooltip: {
          trigger: 'axis',
          formatter: '{b}: {c}°C'
        }
      }
      
      this.temperatureChart.setOption(option)
    },
    
    // 流量图表
    initFlowChart() {
      const element = document.getElementById('flowChart')
      if (!element) return
      
      this.flowChart = echarts.init(element)
      
      const option = {
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00']
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            formatter: '{value}m³/h'
          }
        },
        series: [{
          data: [120, 115, 125, 140, 135, 130, 125],
          type: 'line',
          smooth: true,
          lineStyle: {
            color: '#40a9ff'
          },
          itemStyle: {
            color: '#40a9ff'
          },
          areaStyle: {
            color: 'rgba(64, 169, 255, 0.1)'
          }
        }],
        tooltip: {
          trigger: 'axis',
          formatter: '{b}: {c}m³/h'
        }
      }
      
      this.flowChart.setOption(option)
    },
    
    // 压力图表
    initPressureChart() {
      const element = document.getElementById('pressureChart')
      if (!element) return
      
      this.pressureChart = echarts.init(element)
      
      const option = {
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00']
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            formatter: '{value}MPa'
          }
        },
        series: [{
          data: [2.1, 2.0, 2.2, 2.3, 2.2, 2.1, 2.0],
          type: 'line',
          smooth: true,
          lineStyle: {
            color: '#73d13d'
          },
          itemStyle: {
            color: '#73d13d'
          },
          areaStyle: {
            color: 'rgba(115, 209, 61, 0.1)'
          }
        }],
        tooltip: {
          trigger: 'axis',
          formatter: '{b}: {c}MPa'
        }
      }
      
      this.pressureChart.setOption(option)
    },
    
    // 震动图表
    initVibrationChart() {
      const element = document.getElementById('vibrationChart')
      if (!element) return
      
      this.vibrationChart = echarts.init(element)
      
      const option = {
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00']
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            formatter: '{value}级'
          }
        },
        series: [{
          data: [1, 1, 2, 2, 1, 1, 1],
          type: 'line',
          smooth: true,
          lineStyle: {
            color: '#ffa940'
          },
          itemStyle: {
            color: '#ffa940'
          },
          areaStyle: {
            color: 'rgba(255, 169, 64, 0.1)'
          }
        }],
        tooltip: {
          trigger: 'axis',
          formatter: '{b}: {c}级'
        }
      }
      
      this.vibrationChart.setOption(option)
    }
  }
}
</script>

<style scoped>
.pipeline-details {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
  font-family: 'Microsoft YaHei', sans-serif;
  position: relative;
}

/* 返回按钮容器 */
.back-button-container {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 1000;
}

.back-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #1890ff, #40a9ff);
  color: white;
  border: none;
  border-radius: 25px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
  transition: all 0.3s ease;
}

.back-button:hover {
  background: linear-gradient(135deg, #0050b3, #1890ff);
  box-shadow: 0 6px 16px rgba(24, 144, 255, 0.4);
  transform: translateY(-2px);
}

.back-icon {
  font-size: 16px;
  font-weight: bold;
}

/* 查询区域样式 */
.query-section {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 15px 20px;
  margin-top: 15px;
}

.query-controls {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.query-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.query-item label {
  font-size: 14px;
  color: #333;
  white-space: nowrap;
}

.query-item select {
  padding: 6px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  background: #f5f5f5;
  color: #999;
}

.query-btn {
  padding: 6px 16px;
  background: #f5f5f5;
  color: #999;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  cursor: not-allowed;
  font-size: 14px;
}

/* 主要内容区域 */
.main-content {
  display: flex;
  gap: 20px;
  height: calc(100vh - 160px);
}

/* 左侧地图区域 */
.left-section {
  flex: 1;
  min-width: 400px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.map-container {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
  flex: 1;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 2px solid #1890ff;
}

.map-chart {
  height: 450px;
  min-height: 400px;
}

/* 右侧统计图表区域 */
.right-section {
  flex: 1.2;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 图表查询区域 */
.chart-query-section {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 15px 20px;
}

.chart-query-controls {
  display: flex;
  align-items: center;
  gap: 15px;
}

/* 图表网格 */
.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
  flex: 1;
}

.chart-item {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 15px;
  display: flex;
  flex-direction: column;
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
  text-align: center;
}

.line-chart {
  flex: 1;
  min-height: 180px;
  max-height: 220px;
}

/* 指标卡区域 */
.indicators-section {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
  height: 200px;
}

.indicators-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
  margin-top: 15px;
}

.indicator-card {
  background: #fafafa;
  border-radius: 6px;
  padding: 15px;
  text-align: center;
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
}

.indicator-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.indicator-title {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.indicator-value {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 5px;
}

.indicator-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  display: inline-block;
}

.indicator-status.normal {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.indicator-status.warning {
  background: #fff7e6;
  color: #fa8c16;
  border: 1px solid #ffd591;
}

.indicator-status.danger {
  background: #fff2f0;
  color: #ff4d4f;
  border: 1px solid #ffccc7;
}

.indicator-status.warning {
  background: #fff7e6;
  color: #fa8c16;
  border: 1px solid #ffd591;
}

.indicator-status.danger {
  background: #fff2f0;
  color: #ff4d4f;
  border: 1px solid #ffb3b3;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
    height: auto;
  }
  
  .left-section {
    min-width: auto;
  }
  
  .map-container {
    height: 400px;
  }
  
  .charts-grid {
    grid-template-columns: 1fr;
  }
  
  .indicators-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .pipeline-details {
    padding: 10px;
  }
  
  .top-query-section {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }
  
  .query-controls {
    flex-wrap: wrap;
  }
  
  .indicators-grid {
    grid-template-columns: 1fr;
  }
  
  .chart-query-controls {
    flex-wrap: wrap;
  }
}
</style>