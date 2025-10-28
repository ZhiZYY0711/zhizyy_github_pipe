# 共享模块 (Shared Module)

## 模块概述

共享模块包含系统中可复用的组件、工具函数、API接口和常量定义，为其他模块提供基础支持和公共功能。

## 目录结构

```
shared/
├── api/
│   └── index.js              # 共享API接口
├── components/
│   ├── Layout.vue            # 主布局组件
│   ├── EChartsMap.vue        # 地图图表组件
│   └── EChartsPie.vue        # 饼图组件
├── utils/
│   └── request.js            # HTTP请求工具
└── constants/
    └── index.js              # 常量定义
```

## 功能特性

### 🏗️ 布局组件
- **主布局**: 系统主要布局框架
- **响应式设计**: 适配不同屏幕尺寸
- **侧边栏**: 可折叠的导航侧边栏
- **头部导航**: 用户信息和系统功能

### 📊 图表组件
- **地图组件**: 基于ECharts的地图可视化
- **饼图组件**: 数据分布饼图展示
- **可配置**: 支持多种配置选项
- **响应式**: 自适应容器大小

### 🔧 工具函数
- **HTTP请求**: 统一的API请求封装
- **错误处理**: 全局错误处理机制
- **拦截器**: 请求和响应拦截器
- **认证**: 自动添加认证头

### 📋 常量定义
- **API地址**: 统一的API端点定义
- **状态码**: 系统状态码常量
- **配置项**: 系统配置常量

## 组件说明

### Layout.vue
主布局组件，包含：
- 系统头部导航
- 侧边栏菜单
- 主内容区域
- 用户信息显示
- 天气日期组件

#### Props
```javascript
{
  sidebarCollapsed: Boolean  // 侧边栏折叠状态
}
```

#### Events
```javascript
{
  'toggle-sidebar': Function  // 切换侧边栏状态
}
```

### EChartsMap.vue
地图图表组件，包含：
- 中国地图展示
- 数据点标记
- 交互功能
- 自定义样式

#### Props
```javascript
{
  width: String,      // 组件宽度，默认'100%'
  height: String,     // 组件高度，默认'400px'
  mapData: Object     // 地图数据
}
```

### EChartsPie.vue
饼图组件，包含：
- 饼图数据展示
- 图例显示
- 交互提示
- 自定义配置

#### Props
```javascript
{
  width: String,      // 组件宽度，默认'100%'
  height: String,     // 组件高度，默认'300px'
  pieData: Array,     // 饼图数据
  title: String       // 图表标题，默认'饼图'
}
```

## 工具函数

### request.js
HTTP请求工具，提供：

```javascript
// 基础配置
const request = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL,
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(config => {
  // 添加认证token
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器
request.interceptors.response.use(
  response => response.data,
  error => {
    // 统一错误处理
    if (error.response?.status === 401) {
      // 处理未授权
      router.push('/login')
    }
    return Promise.reject(error)
  }
)
```

## 常量定义

### API端点
```javascript
export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: '/auth/login',
    LOGOUT: '/auth/logout',
    USER_INFO: '/auth/user'
  },
  DASHBOARD: {
    OVERVIEW: '/dashboard/overview',
    MAP_DATA: '/dashboard/map',
    CHART_DATA: '/dashboard/chart'
  },
  MONITORING: {
    REAL_TIME: '/monitoring/realtime',
    DEVICES: '/monitoring/devices',
    SENSORS: '/monitoring/sensors'
  }
}
```

### 状态码
```javascript
export const STATUS_CODES = {
  SUCCESS: 200,
  CREATED: 201,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  SERVER_ERROR: 500
}
```

### 系统配置
```javascript
export const SYSTEM_CONFIG = {
  PAGE_SIZE: 20,
  REFRESH_INTERVAL: 30000,
  CHART_COLORS: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C'],
  DATE_FORMAT: 'YYYY-MM-DD HH:mm:ss'
}
```

## 使用示例

### 使用布局组件
```vue
<template>
  <Layout 
    :sidebarCollapsed="sidebarCollapsed"
    @toggle-sidebar="handleToggleSidebar"
  >
    <router-view />
  </Layout>
</template>

<script>
import Layout from '@shared/components/Layout.vue'

export default {
  components: { Layout },
  data() {
    return {
      sidebarCollapsed: false
    }
  },
  methods: {
    handleToggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    }
  }
}
</script>
```

### 使用HTTP请求工具
```javascript
import request from '@shared/utils/request'

// 在API文件中使用
export const getUserList = (params) => {
  return request({
    url: '/users',
    method: 'get',
    params
  })
}

export const createUser = (data) => {
  return request({
    url: '/users',
    method: 'post',
    data
  })
}
```

### 使用图表组件
```vue
<template>
  <div>
    <!-- 地图组件 -->
    <EChartsMap 
      :mapData="mapData"
      width="800px"
      height="600px"
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

<script>
import { EChartsMap, EChartsPie } from '@shared/components'

export default {
  components: { EChartsMap, EChartsPie },
  data() {
    return {
      mapData: {
        points: [
          { name: '北京', value: [116.46, 39.92] },
          { name: '上海', value: [121.48, 31.22] }
        ]
      },
      pieData: [
        { name: '正常', value: 80 },
        { name: '异常', value: 15 },
        { name: '离线', value: 5 }
      ]
    }
  }
}
</script>
```

## 配置说明

### 路径别名
在vue.config.js中配置的别名：
- `@shared`: 指向shared模块
- `@shared/components`: 指向共享组件
- `@shared/utils`: 指向共享工具
- `@shared/api`: 指向共享API

### 依赖管理
共享模块的依赖：
- `axios`: HTTP请求库
- `echarts`: 图表库
- `element-ui`: UI组件库