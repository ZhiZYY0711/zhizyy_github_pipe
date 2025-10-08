<template>
  <div class="area-details">
    <!-- 顶部导航栏 -->
    <div class="top-navigation">
      <div class="nav-tabs">
        <div 
          v-for="tab in navigationTabs" 
          :key="tab.key"
          class="nav-tab"
          :class="{ active: activeTab === tab.key }"
          @click="setActiveTab(tab.key)"
        >
          <i :class="tab.icon"></i>
          <span>{{ tab.label }}</span>
        </div>
      </div>
      <!-- 切换按钮 -->
      <div class="switch-button">
        <button class="switch-btn no-sidebar-hide" @click="switchToPage('pipeline')">
          <i class="switch-icon">🔄</i>
          <span>管道详情</span>
        </button>
      </div>
    </div>

    <!-- 条件查询区域 -->
    <div class="filter-section">
      <!-- 时间划分 -->
      <div class="time-filter">
        <label>时间范围：</label>
        <div class="time-buttons">
          <button 
            v-for="period in timePeriods" 
            :key="period.value"
            :class="['time-btn', { active: selectedTimePeriod === period.value }]"
            @click="selectTimePeriod(period.value)"
          >
            {{ period.label }}
          </button>
        </div>
      </div>

      <!-- 区域划分 -->
      <div class="region-filter">
        <label>区域选择：</label>
        <div class="region-selects">
          <select v-model="selectedProvince" @change="onProvinceChange">
            <option value="">不限省份</option>
            <option v-for="province in provinces" :key="province.code" :value="province.code">
              {{ province.name }}
            </option>
          </select>
          
          <select v-model="selectedCity" @change="onCityChange" :disabled="!selectedProvince">
            <option value="">不限城市</option>
            <option v-for="city in cities" :key="city.code" :value="city.code">
              {{ city.name }}
            </option>
          </select>
          
          <select v-model="selectedDistrict" @change="onDistrictChange" :disabled="!selectedCity">
            <option value="">不限区县</option>
            <option v-for="district in districts" :key="district.code" :value="district.code">
              {{ district.name }}
            </option>
          </select>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-section">
      <!-- 主折线图 -->
      <div class="main-chart">
        <div class="chart-title">{{ getChartTitle() }}</div>
        <div ref="mainChart" class="chart-container main"></div>
      </div>

      <!-- 右侧区域 -->
      <div class="right-section">
        <!-- 三个子折线图 -->
        <div class="sub-charts">
          <div v-for="(chart, index) in getSubCharts()" :key="index" class="sub-chart">
            <div class="chart-title">{{ chart.title }}</div>
            <div :ref="`subChart${index}`" class="chart-container sub"></div>
          </div>
        </div>

        <!-- 地图 -->
        <div class="map-section">
          <div class="chart-title">管道分布图</div>
          <div ref="mapChart" class="chart-container map"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'

export default {
  name: 'AreaDetails',
  data() {
    return {
      // 导航栏数据
      navigationTabs: [
        { key: 'flow', label: '流量', icon: 'icon-flow' },
        { key: 'pressure', label: '压力', icon: 'icon-pressure' },
        { key: 'temperature', label: '温度', icon: 'icon-temperature' },
        { key: 'vibration', label: '震动', icon: 'icon-vibration' }
      ],
      activeTab: 'flow',

      // 时间周期数据
      timePeriods: [
        { value: 7, label: '7天' },
        { value: 30, label: '30天' },
        { value: 90, label: '90天' }
      ],
      selectedTimePeriod: 7,

      // 区域数据
      provinces: [
        { code: 'beijing', name: '北京市' },
        { code: 'shanghai', name: '上海市' },
        { code: 'guangdong', name: '广东省' },
        { code: 'shandong', name: '山东省' }
      ],
      cities: [],
      districts: [],
      selectedProvince: '',
      selectedCity: '',
      selectedDistrict: '',

      // 图表实例
      mainChartInstance: null,
      subChartInstances: [],
      mapChartInstance: null,
      mapRegistered: false,
      geoNodes: [],
      geoLinks: [],

      // 模拟数据
      chartData: {
        flow: { main: [], sub: [[], [], []] },
        pressure: { main: [], sub: [[], [], []] },
        temperature: { main: [], sub: [[], [], []] },
        vibration: { main: [], sub: [[], [], []] }
      }
    }
  },
  mounted() {
    // 先生成数据，再初始化图表。图表初始化完成后再统一触发更新，避免首次进入页面时图表未渲染。
    this.generateMockData()
    this.initCharts()
  },
  beforeDestroy() {
    this.destroyCharts()
  },
  methods: {
    // 切换导航标签
    setActiveTab(tabKey) {
      this.activeTab = tabKey
      this.updateCharts()
    },

    // 切换页面
    switchToPage(pageType) {
      this.$emit('switch-page', pageType)
    },

    // 选择时间周期
    selectTimePeriod(period) {
      this.selectedTimePeriod = period
      this.generateMockData()
      this.updateCharts()
    },

    // 省份变化
    onProvinceChange() {
      this.selectedCity = ''
      this.selectedDistrict = ''
      this.cities = this.getCitiesByProvince(this.selectedProvince)
      this.districts = []
      this.generateMockData()
      this.updateCharts()
    },

    // 城市变化
    onCityChange() {
      this.selectedDistrict = ''
      this.districts = this.getDistrictsByCity(this.selectedCity)
      this.generateMockData()
      this.updateCharts()
    },

    // 区县变化
    onDistrictChange() {
      this.generateMockData()
      this.updateCharts()
    },

    // 获取城市列表
    getCitiesByProvince(provinceCode) {
      const cityMap = {
        beijing: [{ code: 'dongcheng', name: '东城区' }, { code: 'xicheng', name: '西城区' }],
        shanghai: [{ code: 'huangpu', name: '黄浦区' }, { code: 'xuhui', name: '徐汇区' }],
        guangdong: [{ code: 'guangzhou', name: '广州市' }, { code: 'shenzhen', name: '深圳市' }],
        shandong: [{ code: 'jinan', name: '济南市' }, { code: 'qingdao', name: '青岛市' }]
      }
      return cityMap[provinceCode] || []
    },

    // 获取区县列表
    getDistrictsByCity(cityCode) {
      const districtMap = {
        dongcheng: [{ code: 'donghuamen', name: '东华门街道' }],
        xicheng: [{ code: 'xichang', name: '西长安街街道' }],
        huangpu: [{ code: 'nanjing', name: '南京东路街道' }],
        xuhui: [{ code: 'tianlin', name: '田林街道' }],
        guangzhou: [{ code: 'tianhe', name: '天河区' }],
        shenzhen: [{ code: 'futian', name: '福田区' }],
        jinan: [{ code: 'lixia', name: '历下区' }],
        qingdao: [{ code: 'shinan', name: '市南区' }]
      }
      return districtMap[cityCode] || []
    },

    // 获取图表标题
    getChartTitle() {
      const titleMap = {
        flow: '流量变化趋势',
        pressure: '压力变化趋势',
        temperature: '温度变化趋势',
        vibration: '震动变化趋势'
      }
      return titleMap[this.activeTab]
    },

    // 获取子图表配置
    getSubCharts() {
      const allCharts = [
        { key: 'flow', title: '流量监测' },
        { key: 'pressure', title: '压力监测' },
        { key: 'temperature', title: '温度监测' },
        { key: 'vibration', title: '震动监测' }
      ]
      return allCharts.filter(chart => chart.key !== this.activeTab)
    },

    // 初始化图表
    initCharts() {
      this.$nextTick(() => {
        // 主图表
        if (this.$refs.mainChart) {
          this.mainChartInstance = echarts.init(this.$refs.mainChart)
        }

        // 子图表（兼容 ref 返回值为元素或数组的情况）
        this.subChartInstances = []
        for (let i = 0; i < 3; i++) {
          const ref = this.$refs[`subChart${i}`]
          const el = Array.isArray(ref) ? ref && ref[0] : ref
          if (el) {
            this.subChartInstances[i] = echarts.init(el)
          }
        }

        // 地图
        if (this.$refs.mapChart) {
          this.mapChartInstance = echarts.init(this.$refs.mapChart)
        }

        // 监听窗口大小变化
        window.addEventListener('resize', this.handleResize)

        // 图表实例创建完成后再触发渲染，避免首次进入页面时不显示数据
        this.updateCharts()
      })
    },

    // 销毁图表
    destroyCharts() {
      if (this.mainChartInstance) {
        this.mainChartInstance.dispose()
      }
      this.subChartInstances.forEach(instance => {
        if (instance) instance.dispose()
      })
      if (this.mapChartInstance) {
        this.mapChartInstance.dispose()
      }
      window.removeEventListener('resize', this.handleResize)
    },

    // 处理窗口大小变化
    handleResize() {
      if (this.mainChartInstance) this.mainChartInstance.resize()
      this.subChartInstances.forEach(instance => {
        if (instance) instance.resize()
      })
      if (this.mapChartInstance) this.mapChartInstance.resize()
    },

    // 生成模拟数据
    generateMockData() {
      const days = this.selectedTimePeriod
      const categories = ['flow', 'pressure', 'temperature', 'vibration']
      
      categories.forEach(category => {
        // 主数据
        this.chartData[category].main = Array.from({ length: days }, (_, i) => {
          const date = new Date()
          date.setDate(date.getDate() - days + i + 1)
          return {
            date: date.toISOString().split('T')[0],
            value: Math.random() * 100 + 50
          }
        })

        // 子数据
        for (let j = 0; j < 3; j++) {
          this.chartData[category].sub[j] = Array.from({ length: days }, (_, i) => {
            const date = new Date()
            date.setDate(date.getDate() - days + i + 1)
            return {
              date: date.toISOString().split('T')[0],
              value: Math.random() * 80 + 20
            }
          })
        }
      })
    },

    // 更新图表
    updateCharts() {
      this.updateMainChart()
      this.updateSubCharts()
      this.updateMapChart()
    },

    // 更新主图表
    updateMainChart() {
      if (!this.mainChartInstance) return

      const data = this.chartData[this.activeTab].main
      const option = {
        title: {
          text: this.getChartTitle(),
          left: 'center'
        },
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          type: 'category',
          data: data.map(item => item.date)
        },
        yAxis: {
          type: 'value'
        },
        series: [{
          data: data.map(item => item.value),
          type: 'line',
          smooth: true,
          lineStyle: {
            color: '#1890ff'
          },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(24, 144, 255, 0.3)' },
                { offset: 1, color: 'rgba(24, 144, 255, 0.1)' }
              ]
            }
          }
        }]
      }
      this.mainChartInstance.setOption(option)
    },

    // 更新子图表
    updateSubCharts() {
      const subCharts = this.getSubCharts()
      
      subCharts.forEach((chart, index) => {
        if (!this.subChartInstances[index]) return

        const data = this.chartData[chart.key].sub[0]
        const option = {
          title: {
            text: chart.title,
            textStyle: { fontSize: 12 }
          },
          tooltip: {
            trigger: 'axis'
          },
          xAxis: {
            type: 'category',
            data: data.map(item => item.date),
            axisLabel: { fontSize: 10 }
          },
          yAxis: {
            type: 'value',
            axisLabel: { fontSize: 10 }
          },
          series: [{
            data: data.map(item => item.value),
            type: 'line',
            smooth: true,
            lineStyle: {
              color: ['#52c41a', '#faad14', '#f5222d'][index]
            }
          }]
        }
        this.subChartInstances[index].setOption(option)
      })
    },

    // 更新地图（使用中国地图 JSON 资源 + 随机 geo graph）
    async updateMapChart() {
      if (!this.mapChartInstance) return

      try {
        if (!this.mapRegistered) {
          const chinaJson = await import('@/assets/mapjson/中华人民共和国.json')
          const mapData = chinaJson.default || chinaJson
          // 注册中国地图到 ECharts
          if (typeof echarts !== 'undefined') {
            echarts.registerMap('china', mapData)
          }
          this.mapRegistered = true
          this.generateRandomGeoGraph(mapData)
        } else if (!this.geoNodes.length) {
          const chinaJson = await import('@/assets/mapjson/中华人民共和国.json')
          const mapData = chinaJson.default || chinaJson
          this.generateRandomGeoGraph(mapData)
        }
      } catch (e) {
        console.error('中国地图数据加载失败:', e)
      }

      const option = {
        title: {
          text: '管道分布图（随机示例）',
          left: 'center',
          textStyle: { fontSize: 12 }
        },
        tooltip: {
          trigger: 'item',
          formatter: (params) => {
            if (params.seriesType === 'graph') {
              const [lng, lat] = params.value || []
              return `${params.name}<br/>坐标: [${(lng||0).toFixed(3)}, ${(lat||0).toFixed(3)}]`
            }
            return params.name
          }
        },
        series: [
          {
            type: 'graph',
            coordinateSystem: 'geo',
            data: this.geoNodes,
            links: this.geoLinks,
            roam: true,
            label: { show: false },
            lineStyle: { color: '#1976d2', width: 2, opacity: 0.8 },
            emphasis: { focus: 'adjacency' }
          }
        ],
        geo: {
          map: 'china',
          roam: true,
          itemStyle: { areaColor: '#f3f3f3', borderColor: '#999' },
          emphasis: { itemStyle: { areaColor: '#e6f7ff' } }
        }
      }
      this.mapChartInstance.setOption(option)
    }
    ,
    // 基于地图 JSON 生成随机的节点与连线（参考 geo-graph 示例）
    generateRandomGeoGraph(mapJson) {
      const features = (mapJson && mapJson.features) ? mapJson.features : []
      const centers = features
        .map(f => ({
          name: f.properties && f.properties.name ? f.properties.name : '未知',
          coord: (f.properties && f.properties.center) ? f.properties.center : (f.properties && f.properties.centroid ? f.properties.centroid : null)
        }))
        .filter(c => Array.isArray(c.coord) && c.coord.length === 2)

      const count = Math.floor(Math.random() * 5) + 8 // 8-12 个节点
      const pool = centers.slice()
      const picked = []
      while (picked.length < count && pool.length) {
        const idx = Math.floor(Math.random() * pool.length)
        picked.push(pool.splice(idx, 1)[0])
      }

      this.geoNodes = picked.map((p, i) => ({
        name: p.name,
        value: p.coord,
        symbolSize: Math.floor(Math.random() * 6) + 8,
        itemStyle: { color: i % 2 === 0 ? '#64b5f6' : '#81c784' }
      }))

      this.geoLinks = this.geoNodes.slice(1).map((n, i) => ({
        source: this.geoNodes[i].name,
        target: n.name
      }))
    }
  }
}
</script>

<style scoped>
.area-details {
  padding: 20px;
  background-color: #f5f5f5;
  min-height: 100vh;
}

/* 顶部导航栏 */
.top-navigation {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nav-tabs {
  display: flex;
  gap: 20px;
  justify-content: center;
  flex: 1;
}

/* 切换按钮 */
.switch-button {
  margin-left: 20px;
}

.switch-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #1890ff, #40a9ff);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
  font-weight: 500;
}

.switch-btn:hover {
  background: linear-gradient(135deg, #40a9ff, #69c0ff);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
}

.switch-icon {
  font-size: 16px;
}

.nav-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  background: #f0f0f0;
  color: #666;
}

.nav-tab.active {
  background: #1890ff;
  color: white;
}

.nav-tab:hover {
  background: #40a9ff;
  color: white;
}

/* 条件查询区域 */
.filter-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  display: flex;
  gap: 40px;
  align-items: center;
  flex-wrap: wrap;
}

.time-filter, .region-filter {
  display: flex;
  align-items: center;
  gap: 12px;
}

.time-filter label, .region-filter label {
  font-weight: 600;
  color: #333;
}

.time-buttons {
  display: flex;
  gap: 8px;
}

.time-btn {
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
}

.time-btn.active {
  background: #1890ff;
  color: white;
  border-color: #1890ff;
}

.region-selects {
  display: flex;
  gap: 12px;
}

.region-selects select {
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: white;
  min-width: 120px;
}

/* 图表区域 */
.charts-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  height: 600px;
}

.main-chart {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.right-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sub-charts {
  display: flex;
  flex-direction: column;
  gap: 15px;
  flex: 1;
}

.sub-chart {
  background: white;
  border-radius: 8px;
  padding: 15px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  flex: 1;
}

.map-section {
  background: white;
  border-radius: 8px;
  padding: 15px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  height: 200px;
}

.chart-title {
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
  text-align: center;
}

.chart-container.main {
  height: 500px;
}

.chart-container.sub {
  height: 120px;
}

.chart-container.map {
  height: 150px;
}

/* 图标样式 */
.icon-flow::before {
  content: "💧";
  margin-right: 4px;
}

.icon-pressure::before {
  content: "⚡";
  margin-right: 4px;
}

.icon-temperature::before {
  content: "🌡️";
  margin-right: 4px;
}

.icon-vibration::before {
  content: "📳";
  margin-right: 4px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .charts-section {
    grid-template-columns: 1fr;
    height: auto;
  }
  
  .right-section {
    flex-direction: row;
    flex-wrap: wrap;
  }
  
  .sub-charts {
    flex-direction: row;
    flex: 2;
  }
  
  .map-section {
    flex: 1;
    min-width: 300px;
  }
}

@media (max-width: 768px) {
  .filter-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
  }
  
  .nav-tabs {
    flex-wrap: wrap;
    gap: 10px;
  }
  
  .nav-tab {
    padding: 8px 16px;
    font-size: 14px;
  }
  
  .region-selects {
    flex-direction: column;
    gap: 8px;
  }
  
  .sub-charts {
    flex-direction: column;
  }
  
  .right-section {
    flex-direction: column;
  }
}
</style>