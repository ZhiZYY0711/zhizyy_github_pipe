export type PipelineVisualStatus = 'normal' | 'warning' | 'critical'
export type PipelineVisualNodeType = 'hub' | 'station' | 'valve' | 'offtake'

export type PipelineVisualNode = {
  id: string
  name: string
  type: PipelineVisualNodeType
  status: PipelineVisualStatus
  coord: [number, number]
  priority: number
}

export type PipelineVisualRoute = {
  id: string
  name: string
  status: PipelineVisualStatus
  coords: Array<[number, number]>
}

export const trunkPipelineRoutes: PipelineVisualRoute[] = [
  {
    id: 'west-east-main',
    name: '西气东输主干线',
    status: 'normal',
    coords: [
      [87.6, 43.8],
      [95.3, 40.8],
      [101.8, 36.5],
      [108.9, 34.3],
      [114.3, 34.8],
      [119.2, 32.2],
      [121.5, 31.2],
    ],
  },
  {
    id: 'north-china-ring',
    name: '华北环网主干线',
    status: 'warning',
    coords: [
      [112.5, 37.9],
      [114.5, 38.0],
      [116.4, 39.9],
      [117.2, 39.1],
      [118.2, 39.6],
      [119.6, 39.9],
    ],
  },
  {
    id: 'coastal-energy-corridor',
    name: '东部沿海能源走廊',
    status: 'normal',
    coords: [
      [121.5, 31.2],
      [120.2, 30.3],
      [119.3, 26.1],
      [113.3, 23.1],
      [114.1, 22.6],
    ],
  },
  {
    id: 'southwest-branch',
    name: '西南联络干线',
    status: 'critical',
    coords: [
      [104.1, 30.7],
      [106.6, 29.6],
      [108.3, 30.6],
      [111.3, 30.7],
      [114.3, 30.6],
    ],
  },
]

export const pipelineVisualNodes: PipelineVisualNode[] = [
  { id: 'urumqi-hub', name: '乌鲁木齐枢纽', type: 'hub', status: 'normal', coord: [87.6, 43.8], priority: 3 },
  { id: 'lanzhou-hub', name: '兰州压气站', type: 'station', status: 'normal', coord: [103.8, 36.1], priority: 2 },
  { id: 'xian-valve', name: '西安分输阀室', type: 'valve', status: 'normal', coord: [108.9, 34.3], priority: 1 },
  { id: 'beijing-hub', name: '北京调度枢纽', type: 'hub', status: 'warning', coord: [116.4, 39.9], priority: 3 },
  { id: 'tangshan-station', name: '唐山压气站', type: 'station', status: 'warning', coord: [118.2, 39.6], priority: 2 },
  { id: 'shanghai-offtake', name: '上海门站', type: 'offtake', status: 'normal', coord: [121.5, 31.2], priority: 3 },
  { id: 'wuhan-hub', name: '武汉联络枢纽', type: 'hub', status: 'critical', coord: [114.3, 30.6], priority: 3 },
  { id: 'chongqing-station', name: '重庆压气站', type: 'station', status: 'critical', coord: [106.6, 29.6], priority: 2 },
  { id: 'guangzhou-offtake', name: '广州门站', type: 'offtake', status: 'normal', coord: [113.3, 23.1], priority: 2 },
]
