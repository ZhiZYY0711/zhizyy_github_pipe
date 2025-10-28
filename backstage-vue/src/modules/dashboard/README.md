# 仪表板模块 (Dashboard Module)

## 模块概述

仪表板模块是系统的核心展示模块，提供数据可视化、实时监控和综合分析功能，为用户提供直观的数据展示界面。

## 目录结构

```
dashboard/
├── api/
│   └── index.js              # 仪表板API接口
├── store/
│   └── index.js              # 仪表板状态管理
└── views/
    ├── DataVisualization.vue # 数据可视化主页
    ├── PipelineDetails.vue   # 管道详情页
    └── AreaDetails.vue       # 区域详情页
```

## 功能特性

### 📊 数据可视化
- **实时监控大屏**: 综合数据展示界面
- **中国地图**: 管道分布和监测点可视化
- **图表展示**: 多种图表类型的数据展示
- **KPI指标**: 关键性能指标实时展示

### 🗺️ 地图功能
- **管道分布图**: 全国管道网络分布
- **监测点标记**: 传感器位置标记
- **区域钻取**: 点击区域查看详细信息
- **实时状态**: 设备状态实时更新

### 📈 图表分析
- **趋势分析**: 数据变化趋势图
- **对比分析**: 多维度数据对比
- **饼图统计**: 数据分布统计
- **柱状图**: 分类数据展示

## API接口

### 数据获取接口
```javascript
// 获取概览数据
getOverviewData()
// 返回: { totalPipelines, totalSensors, alerts, ... }

// 获取地图数据
getMapData()
// 返回: { regions: [], pipelines: [], sensors: [] }

// 获取流量趋势数据
getFlowTrendData(timeRange)
// 参数: { startTime, endTime }
// 返回: { timestamps: [], values: [] }

// 获取图表数据
getChartData(chartType, params)
// 参数: { type: 'pie'|'bar'|'line', filters: {} }
// 返回: 对应图表的数据格式
```

## 状态管理

### State
```javascript
{
  overviewData: {},      // 概览数据
  mapData: {},           // 地图数据
  flowTrendData: {},     // 流量趋势数据
  chartData: {},         // 图表数据
  loading: false         // 加载状态
}
```

### Mutations
- `SET_OVERVIEW_DATA`: 设置概览数据
- `SET_MAP_DATA`: 设置地图数据
- `SET_FLOW_TREND_DATA`: 设置流量趋势数据
- `SET_CHART_DATA`: 设置图表数据
- `SET_LOADING`: 设置加载状态

### Actions
- `fetchOverviewData`: 获取概览数据
- `fetchMapData`: 获取地图数据
- `fetchFlowTrendData`: 获取流量趋势数据
- `fetchChartData`: 获取图表数据

## 组件说明

### DataVisualization.vue
数据可视化主页，包含：
- 全局KPI指标卡
- 中国地图组件
- 流量趋势图表
- 各类统计图表
- 实时数据更新

### PipelineDetails.vue
管道详情页，包含：
- 管道基本信息
- 传感器列表
- 监测数据图表
- 历史数据查询

### AreaDetails.vue
区域详情页，包含：
- 区域概览信息
- 区域内管道列表
- 区域统计数据
- 区域地图展示

## 使用示例

### 在组件中使用仪表板数据
```javascript
import { mapState, mapActions } from 'vuex'

export default {
  computed: {
    ...mapState('dashboard', ['overviewData', 'loading'])
  },
  methods: {
    ...mapActions('dashboard', ['fetchOverviewData']),
    
    async loadData() {
      try {
        await this.fetchOverviewData()
      } catch (error) {
        this.$toast.error('数据加载失败')
      }
    }
  },
  mounted() {
    this.loadData()
  }
}
```

### 图表组件使用
```vue
<template>
  <div>
    <!-- 地图组件 -->
    <EChartsMap 
      :mapData="mapData" 
      width="100%" 
      height="500px"
    />
    
    <!-- 饼图组件 -->
    <EChartsPie 
      :pieData="pieData" 
      title="设备状态分布"
      width="400px" 
      height="300px"
    />
  </div>
</template>
```

## 配置说明

### 图表配置
- 使用ECharts进行数据可视化
- 支持主题配置和自定义样式
- 响应式设计适配不同屏幕尺寸

### 数据更新
- 支持实时数据更新
- 可配置数据刷新间隔
- 自动处理数据加载状态

### 路由配置
- 主页路由: `/dashboard`
- 管道详情: `/dashboard/pipeline/:id`
- 区域详情: `/dashboard/area/:id`