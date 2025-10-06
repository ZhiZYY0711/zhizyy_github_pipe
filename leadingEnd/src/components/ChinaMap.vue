<template>
  <div class="china-map">
    <div class="map-container" ref="mapContainer">
      <svg 
        :width="mapWidth" 
        :height="mapHeight" 
        :viewBox="`0 0 ${mapWidth} ${mapHeight}`"
        class="map-svg"
      >
        <!-- 地图背景 -->
        <rect 
          :width="mapWidth" 
          :height="mapHeight" 
          fill="#0a1929" 
          class="map-background"
        />
        
        <!-- 省份路径 -->
        <g class="provinces-group">
          <path
            v-for="province in provinces"
            :key="province.properties.adcode"
            :d="province.path"
            :fill="getProvinceColor(province.properties.name)"
            :stroke="'#1976d2'"
            :stroke-width="1"
            class="province-path"
            @mouseover="handleMouseOver(province, $event)"
            @mousemove="handleMouseMove($event)"
            @mouseleave="handleMouseLeave"
            @click="handleProvinceClick(province)"
          />
        </g>
      </svg>
      
      <!-- 悬停提示框 -->
      <div 
        v-if="tooltip.visible" 
        class="tooltip"
        :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px' }"
      >
        <div class="tooltip-header">
          <h4>{{ tooltip.data.name }}</h4>
        </div>
        <div class="tooltip-content">
          <div class="tooltip-item">
            <span class="label">传感器数量:</span>
            <span class="value">{{ tooltip.data.sensorCount }}</span>
          </div>
          <div class="tooltip-item">
            <span class="label">异常/离线数量:</span>
            <span class="value error">{{ tooltip.data.abnormalCount }}</span>
          </div>
          <div class="tooltip-item">
            <span class="label">今日高危报警:</span>
            <span class="value danger">{{ tooltip.data.highRiskAlerts }}</span>
          </div>
          <div class="tooltip-item">
            <span class="label">今日危险报警:</span>
            <span class="value warning">{{ tooltip.data.dangerAlerts }}</span>
          </div>
          <div class="tooltip-item">
            <span class="label">进行中任务:</span>
            <span class="value">{{ tooltip.data.activeTasks }}</span>
          </div>
          <div class="tooltip-item">
            <span class="label">超时未处理:</span>
            <span class="value warning">{{ tooltip.data.overdueTasks }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import chinaMapData from '@/assets/mapjson/中华人民共和国.json'

export default {
  name: 'ChinaMap',
  data() {
    return {
      mapWidth: 800,
      mapHeight: 600,
      provinces: [],
      tooltip: {
        visible: false,
        x: 0,
        y: 0,
        data: {}
      },
      // 模拟省份数据
      provinceData: {
        '北京市': { sensorCount: 156, abnormalCount: 8, highRiskAlerts: 2, dangerAlerts: 5, activeTasks: 12, overdueTasks: 1 },
        '天津市': { sensorCount: 89, abnormalCount: 3, highRiskAlerts: 1, dangerAlerts: 2, activeTasks: 7, overdueTasks: 0 },
        '河北省': { sensorCount: 234, abnormalCount: 15, highRiskAlerts: 4, dangerAlerts: 8, activeTasks: 18, overdueTasks: 3 },
        '山西省': { sensorCount: 178, abnormalCount: 12, highRiskAlerts: 3, dangerAlerts: 6, activeTasks: 14, overdueTasks: 2 },
        '内蒙古自治区': { sensorCount: 312, abnormalCount: 18, highRiskAlerts: 5, dangerAlerts: 12, activeTasks: 25, overdueTasks: 4 },
        '辽宁省': { sensorCount: 198, abnormalCount: 9, highRiskAlerts: 2, dangerAlerts: 4, activeTasks: 16, overdueTasks: 1 },
        '吉林省': { sensorCount: 145, abnormalCount: 7, highRiskAlerts: 1, dangerAlerts: 3, activeTasks: 11, overdueTasks: 1 },
        '黑龙江省': { sensorCount: 267, abnormalCount: 14, highRiskAlerts: 3, dangerAlerts: 7, activeTasks: 20, overdueTasks: 2 },
        '上海市': { sensorCount: 98, abnormalCount: 4, highRiskAlerts: 1, dangerAlerts: 2, activeTasks: 8, overdueTasks: 0 },
        '江苏省': { sensorCount: 289, abnormalCount: 16, highRiskAlerts: 4, dangerAlerts: 9, activeTasks: 22, overdueTasks: 3 },
        '浙江省': { sensorCount: 245, abnormalCount: 11, highRiskAlerts: 3, dangerAlerts: 6, activeTasks: 19, overdueTasks: 2 },
        '安徽省': { sensorCount: 187, abnormalCount: 10, highRiskAlerts: 2, dangerAlerts: 5, activeTasks: 15, overdueTasks: 1 },
        '福建省': { sensorCount: 156, abnormalCount: 8, highRiskAlerts: 2, dangerAlerts: 4, activeTasks: 12, overdueTasks: 1 },
        '江西省': { sensorCount: 134, abnormalCount: 6, highRiskAlerts: 1, dangerAlerts: 3, activeTasks: 10, overdueTasks: 1 },
        '山东省': { sensorCount: 298, abnormalCount: 17, highRiskAlerts: 5, dangerAlerts: 10, activeTasks: 24, overdueTasks: 3 },
        '河南省': { sensorCount: 276, abnormalCount: 15, highRiskAlerts: 4, dangerAlerts: 8, activeTasks: 21, overdueTasks: 2 },
        '湖北省': { sensorCount: 203, abnormalCount: 11, highRiskAlerts: 3, dangerAlerts: 6, activeTasks: 16, overdueTasks: 2 },
        '湖南省': { sensorCount: 189, abnormalCount: 9, highRiskAlerts: 2, dangerAlerts: 5, activeTasks: 15, overdueTasks: 1 },
        '广东省': { sensorCount: 345, abnormalCount: 20, highRiskAlerts: 6, dangerAlerts: 13, activeTasks: 28, overdueTasks: 4 },
        '广西壮族自治区': { sensorCount: 167, abnormalCount: 8, highRiskAlerts: 2, dangerAlerts: 4, activeTasks: 13, overdueTasks: 1 },
        '海南省': { sensorCount: 78, abnormalCount: 3, highRiskAlerts: 1, dangerAlerts: 2, activeTasks: 6, overdueTasks: 0 },
        '重庆市': { sensorCount: 123, abnormalCount: 6, highRiskAlerts: 2, dangerAlerts: 3, activeTasks: 10, overdueTasks: 1 },
        '四川省': { sensorCount: 234, abnormalCount: 13, highRiskAlerts: 4, dangerAlerts: 7, activeTasks: 19, overdueTasks: 2 },
        '贵州省': { sensorCount: 145, abnormalCount: 7, highRiskAlerts: 2, dangerAlerts: 4, activeTasks: 12, overdueTasks: 1 },
        '云南省': { sensorCount: 198, abnormalCount: 10, highRiskAlerts: 3, dangerAlerts: 6, activeTasks: 16, overdueTasks: 2 },
        '西藏自治区': { sensorCount: 89, abnormalCount: 4, highRiskAlerts: 1, dangerAlerts: 2, activeTasks: 7, overdueTasks: 0 },
        '陕西省': { sensorCount: 167, abnormalCount: 8, highRiskAlerts: 2, dangerAlerts: 4, activeTasks: 13, overdueTasks: 1 },
        '甘肃省': { sensorCount: 134, abnormalCount: 6, highRiskAlerts: 2, dangerAlerts: 3, activeTasks: 11, overdueTasks: 1 },
        '青海省': { sensorCount: 67, abnormalCount: 3, highRiskAlerts: 1, dangerAlerts: 1, activeTasks: 5, overdueTasks: 0 },
        '宁夏回族自治区': { sensorCount: 56, abnormalCount: 2, highRiskAlerts: 0, dangerAlerts: 1, activeTasks: 4, overdueTasks: 0 },
        '新疆维吾尔自治区': { sensorCount: 278, abnormalCount: 14, highRiskAlerts: 4, dangerAlerts: 8, activeTasks: 22, overdueTasks: 3 },
        '台湾省': { sensorCount: 123, abnormalCount: 5, highRiskAlerts: 1, dangerAlerts: 3, activeTasks: 9, overdueTasks: 1 },
        '香港特别行政区': { sensorCount: 45, abnormalCount: 2, highRiskAlerts: 0, dangerAlerts: 1, activeTasks: 3, overdueTasks: 0 },
        '澳门特别行政区': { sensorCount: 23, abnormalCount: 1, highRiskAlerts: 0, dangerAlerts: 0, activeTasks: 2, overdueTasks: 0 }
      }
    }
  },
  mounted() {
    this.initMap()
    this.handleResize()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    initMap() {
      this.processMapData()
    },
    
    processMapData() {
      if (!chinaMapData || !chinaMapData.features) return
      
      // 计算地图边界
      const bounds = this.calculateBounds(chinaMapData.features)
      const scale = this.calculateScale(bounds)
      
      this.provinces = chinaMapData.features.map(feature => {
        const path = this.generatePath(feature.geometry, bounds, scale)
        return {
          ...feature,
          path
        }
      })
    },
    
    calculateBounds(features) {
      let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
      
      features.forEach(feature => {
        this.traverseCoordinates(feature.geometry.coordinates, (coord) => {
          const [x, y] = coord
          minX = Math.min(minX, x)
          minY = Math.min(minY, y)
          maxX = Math.max(maxX, x)
          maxY = Math.max(maxY, y)
        })
      })
      
      return { minX, minY, maxX, maxY }
    },
    
    calculateScale(bounds) {
      const mapAspectRatio = this.mapWidth / this.mapHeight
      const boundsWidth = bounds.maxX - bounds.minX
      const boundsHeight = bounds.maxY - bounds.minY
      const boundsAspectRatio = boundsWidth / boundsHeight
      
      let scale
      if (boundsAspectRatio > mapAspectRatio) {
        scale = (this.mapWidth * 0.9) / boundsWidth
      } else {
        scale = (this.mapHeight * 0.9) / boundsHeight
      }
      
      return scale
    },
    
    generatePath(geometry, bounds, scale) {
      const offsetX = (this.mapWidth - (bounds.maxX - bounds.minX) * scale) / 2
      const offsetY = (this.mapHeight - (bounds.maxY - bounds.minY) * scale) / 2
      
      let pathData = ''
      
      if (geometry.type === 'Polygon') {
        pathData = this.polygonToPath(geometry.coordinates, bounds, scale, offsetX, offsetY)
      } else if (geometry.type === 'MultiPolygon') {
        geometry.coordinates.forEach(polygon => {
          pathData += this.polygonToPath(polygon, bounds, scale, offsetX, offsetY)
        })
      }
      
      return pathData
    },
    
    polygonToPath(coordinates, bounds, scale, offsetX, offsetY) {
      let pathData = ''
      
      coordinates.forEach((ring, ringIndex) => {
        ring.forEach((coord, coordIndex) => {
          const x = (coord[0] - bounds.minX) * scale + offsetX
          const y = this.mapHeight - ((coord[1] - bounds.minY) * scale + offsetY)
          
          if (coordIndex === 0) {
            pathData += `M ${x} ${y} `
          } else {
            pathData += `L ${x} ${y} `
          }
        })
        pathData += 'Z '
      })
      
      return pathData
    },
    
    traverseCoordinates(coordinates, callback) {
      if (Array.isArray(coordinates[0])) {
        if (Array.isArray(coordinates[0][0])) {
          coordinates.forEach(coord => this.traverseCoordinates(coord, callback))
        } else {
          coordinates.forEach(callback)
        }
      } else {
        callback(coordinates)
      }
    },
    
    getProvinceColor(provinceName) {
      const data = this.provinceData[provinceName]
      if (!data) return '#1565c0'
      
      // 计算异常率
      const abnormalRate = data.abnormalCount / data.sensorCount
      // 计算报警严重程度
      const alertSeverity = (data.highRiskAlerts * 2 + data.dangerAlerts) / data.sensorCount
      
      // 综合评分
      const riskScore = abnormalRate * 0.6 + alertSeverity * 0.4
      
      if (riskScore > 0.15) return '#d32f2f' // 高风险 - 深红色
      if (riskScore > 0.1) return '#f57c00'  // 中高风险 - 橙色
      if (riskScore > 0.05) return '#fbc02d' // 中风险 - 黄色
      if (riskScore > 0.02) return '#388e3c' // 低风险 - 绿色
      return '#1976d2' // 正常 - 蓝色
    },
    
    handleMouseOver(province, event) {
      const provinceName = province.properties.name
      const data = this.provinceData[provinceName] || {
        sensorCount: 0, abnormalCount: 0, highRiskAlerts: 0, 
        dangerAlerts: 0, activeTasks: 0, overdueTasks: 0
      }
      
      this.tooltip = {
        visible: true,
        x: event.clientX + 10,
        y: event.clientY - 10,
        data: {
          name: provinceName,
          ...data
        }
      }
    },
    
    handleMouseMove(event) {
      if (this.tooltip.visible) {
        this.tooltip.x = event.clientX + 10
        this.tooltip.y = event.clientY - 10
      }
    },
    
    handleMouseLeave() {
      this.tooltip.visible = false
    },
    
    handleProvinceClick(province) {
      this.$emit('province-click', province.properties)
    },
    
    handleResize() {
      if (this.$refs.mapContainer) {
        const container = this.$refs.mapContainer
        const containerWidth = container.clientWidth
        const containerHeight = container.clientHeight
        
        if (containerWidth > 0 && containerHeight > 0) {
          this.mapWidth = containerWidth
          this.mapHeight = containerHeight
          this.$nextTick(() => {
            this.processMapData()
          })
        }
      }
    }
  }
}
</script>

<style scoped>
.china-map {
  width: 100%;
  height: 100%;
  position: relative;
}

.map-container {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.map-svg {
  width: 100%;
  height: 100%;
  display: block;
}

.map-background {
  fill: #0a1929;
}

.province-path {
  cursor: pointer;
  transition: all 0.3s ease;
  stroke-width: 1;
  stroke: #1976d2;
}

.province-path:hover {
  stroke: #ffffff;
  stroke-width: 2;
  filter: brightness(1.2);
}

.tooltip {
  position: fixed;
  background: rgba(0, 0, 0, 0.9);
  color: white;
  padding: 12px;
  border-radius: 8px;
  font-size: 12px;
  z-index: 1000;
  pointer-events: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  border: 1px solid #1976d2;
  min-width: 200px;
}

.tooltip-header {
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid #1976d2;
}

.tooltip-header h4 {
  margin: 0;
  color: #64b5f6;
  font-size: 14px;
  font-weight: bold;
}

.tooltip-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tooltip-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tooltip-item .label {
  color: #b0bec5;
  font-size: 11px;
}

.tooltip-item .value {
  color: #ffffff;
  font-weight: bold;
  font-size: 12px;
}

.tooltip-item .value.error {
  color: #f44336;
}

.tooltip-item .value.danger {
  color: #ff5722;
}

.tooltip-item .value.warning {
  color: #ff9800;
}
</style>