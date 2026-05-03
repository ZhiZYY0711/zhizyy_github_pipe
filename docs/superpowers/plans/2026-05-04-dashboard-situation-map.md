# Dashboard Situation Map Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the dashboard overview into a full-screen situation map with floating HUD panels, mock trunk pipelines, node level-of-detail, smoother centered wheel zoom, and icon map controls.

**Architecture:** Keep the change frontend-only and scoped to `frontend/src/modules/dashboard`. `DashboardPage.vue` owns the stage layout, layer toggles, and selected pipeline focus. `DashboardGeoMap.vue` owns ECharts rendering, region drill-down, pipeline overlay series, node LOD, and smooth centered zoom.

**Tech Stack:** Vue 3 Composition API, TypeScript, ECharts 6 map/effect scatter/lines series, Vite, existing dashboard services.

---

## File Structure

- Create `frontend/src/modules/dashboard/pipelineOverlayData.ts`: mock trunk pipeline and node metadata.
- Modify `frontend/src/modules/dashboard/types.ts`: add pipeline, node, layer, and focus types.
- Modify `frontend/src/modules/dashboard/DashboardPage.vue`: convert fixed board layout into map stage HUD layout; own layer state and selected pipeline state.
- Modify `frontend/src/modules/dashboard/components/DashboardGeoMap.vue`: add pipeline/node/alarm overlay series, LOD logic, pipeline events, icon toolbar, smoother centered zoom.

## Tasks

### Task 1: Add Pipeline Types And Mock Data

**Files:**
- Modify: `frontend/src/modules/dashboard/types.ts`
- Create: `frontend/src/modules/dashboard/pipelineOverlayData.ts`

- [ ] **Step 1: Add TypeScript contracts**

Add these exported types near the existing map-related types in `frontend/src/modules/dashboard/types.ts`:

```ts
export type DashboardMapLayerKey = 'regions' | 'pipelines' | 'nodes' | 'alarms'

export type PipelineStatus = 'normal' | 'warning' | 'critical'

export type PipelineNodeType = 'hub' | 'station' | 'valve' | 'offtake'

export type PipelineNode = {
  id: string
  name: string
  type: PipelineNodeType
  status: PipelineStatus
  coord: [number, number]
  priority: number
  provinceCode: string
}

export type TrunkPipeline = {
  id: string
  name: string
  status: PipelineStatus
  pressure: number
  flow: number
  riskCount: number
  coords: Array<[number, number]>
  nodes: PipelineNode[]
}

export type DashboardMapFocus =
  | { type: 'region'; code: string; name: string }
  | { type: 'pipeline'; pipeline: TrunkPipeline }
  | null
```

- [ ] **Step 2: Create mock trunk line data**

Create `frontend/src/modules/dashboard/pipelineOverlayData.ts` with five trunk pipelines and multiple nodes. Use approximate coordinates and keep comments clear that the geometry is visual mock data.

- [ ] **Step 3: Run type checking through build**

Run: `cd frontend && pnpm build`

Expected: build may fail only if later files do not yet consume the new types. Type syntax itself must be valid.

### Task 2: Convert Dashboard Layout To Situation Stage

**Files:**
- Modify: `frontend/src/modules/dashboard/DashboardPage.vue`

- [ ] **Step 1: Import mock pipeline data and layer types**

Import `trunkPipelines` from `./pipelineOverlayData` and add type imports for `DashboardMapLayerKey` and `TrunkPipeline`.

- [ ] **Step 2: Add layer and focus state**

Add state in `<script setup>`:

```ts
const visibleMapLayers = ref<Record<DashboardMapLayerKey, boolean>>({
  regions: true,
  pipelines: true,
  nodes: true,
  alarms: true,
})
const selectedPipeline = ref<TrunkPipeline | null>(null)

const selectedPipelineStatusText = computed(() => {
  if (!selectedPipeline.value) {
    return '区域态势'
  }

  return selectedPipeline.value.status === 'critical'
    ? '严重异常'
    : selectedPipeline.value.status === 'warning'
      ? '风险关注'
      : '运行正常'
})

function toggleMapLayer(layer: DashboardMapLayerKey) {
  visibleMapLayers.value = {
    ...visibleMapLayers.value,
    [layer]: !visibleMapLayers.value[layer],
  }
}

function handlePipelineClick(pipeline: TrunkPipeline) {
  selectedPipeline.value = pipeline
}
```

Update region selection and reset handlers to clear `selectedPipeline.value`.

- [ ] **Step 3: Pass pipeline props/events to map**

Pass `:pipelines="trunkPipelines"`, `:visible-layers="visibleMapLayers"`, `:selected-pipeline-id="selectedPipeline?.id"`, and `@pipeline-click="handlePipelineClick"` to `DashboardGeoMap`.

- [ ] **Step 4: Replace board markup with HUD stage**

Replace the existing board structure with a stage:

```vue
<div class="dashboard-page-module__stage">
  <DashboardGeoMap ... />
  <div class="dashboard-page-module__hud dashboard-page-module__hud--kpis">
    <KpiStrip :items="kpiItems" />
  </div>
  <div class="dashboard-page-module__hud dashboard-page-module__hud--filters">
    <AreaFilterPanel ... />
  </div>
  <div class="dashboard-page-module__hud dashboard-page-module__hud--events">
    <SideStack :panels="[dashboardPageModel.sidePanels[0]]" />
    <DashboardAlarmPanel :alarms="alarms" :loading="alarmLoading" />
  </div>
  <section class="dashboard-page-module__focus-card panel">
    <span class="eyebrow">Current Focus</span>
    <h2>{{ selectedPipeline?.name ?? activeAreaName }}</h2>
    <p>{{ selectedPipeline ? selectedPipelineStatusText : '行政区域监控态势' }}</p>
  </section>
  <section class="dashboard-page-module__layer-control panel">
    <button type="button" :class="{ 'is-active': visibleMapLayers.regions }" @click="toggleMapLayer('regions')">行政区</button>
    <button type="button" :class="{ 'is-active': visibleMapLayers.pipelines }" @click="toggleMapLayer('pipelines')">主干线</button>
    <button type="button" :class="{ 'is-active': visibleMapLayers.nodes }" @click="toggleMapLayer('nodes')">节点</button>
    <button type="button" :class="{ 'is-active': visibleMapLayers.alarms }" @click="toggleMapLayer('alarms')">告警</button>
  </section>
</div>
```

- [ ] **Step 5: Update CSS to full-screen stage**

Change `__main` to fill the available area, make `__stage` relative, and position HUD panels absolutely. Ensure `@media (max-width: 1179px)` stacks HUD panels in document flow.

- [ ] **Step 6: Build check**

Run: `cd frontend && pnpm build`

Expected: build fails until `DashboardGeoMap` accepts the new props/events. Continue to Task 3.

### Task 3: Add Pipeline And Node Rendering To DashboardGeoMap

**Files:**
- Modify: `frontend/src/modules/dashboard/components/DashboardGeoMap.vue`

- [ ] **Step 1: Extend component props/events**

Add props:

```ts
pipelines: readonly TrunkPipeline[]
visibleLayers: Record<DashboardMapLayerKey, boolean>
selectedPipelineId?: string
```

Add emit:

```ts
pipelineClick: [pipeline: TrunkPipeline]
```

- [ ] **Step 2: Import line and scatter ECharts modules**

Import `LinesChart`, `EffectScatterChart`, and register them with `use`.

- [ ] **Step 3: Add pipeline series builders**

Create helper functions:

```ts
function buildPipelineLineSeries()
function buildPipelineFlowSeries()
function buildPipelineNodeSeries()
function shouldShowNode(node: PipelineNode)
function pipelineColor(status: PipelineStatus)
```

`shouldShowNode` must use `props.region.level`, `currentZoom`, `node.priority`, `node.status`, and `props.selectedPipelineId`.

- [ ] **Step 4: Include overlay series in map option**

When `visibleLayers.pipelines` is true, add static line and animated flow line series. When `visibleLayers.nodes` is true, add node effect scatter series. Keep administrative map data controlled by `visibleLayers.regions`.

- [ ] **Step 5: Add pipeline hover/click handling**

In `ensureChart`, detect line series clicks by reading a pipeline id from `params.data`. Emit `pipelineClick` with the matching pipeline. Hover styles are handled through ECharts emphasis.

- [ ] **Step 6: Watch layer and selected pipeline changes**

Add a watcher on `props.visibleLayers` and `props.selectedPipelineId` that rebuilds the option without refetching geojson.

- [ ] **Step 7: Build check**

Run: `cd frontend && pnpm build`

Expected: PASS or style-only warnings. Any TypeScript error must be fixed before continuing.

### Task 4: Smooth Centered Wheel Zoom And Icon Toolbar

**Files:**
- Modify: `frontend/src/modules/dashboard/components/DashboardGeoMap.vue`

- [ ] **Step 1: Reduce wheel zoom step**

Change `wheelZoomStep` from `1.18` to a smaller value such as `1.07`.

- [ ] **Step 2: Keep zoom centered on current view center**

Update the wheel handler so it changes `currentZoom` and calls `chart.setOption({ geo: { zoom: nextZoom, center: readChartCenter() } })`. Do not derive center from the mouse cursor.

- [ ] **Step 3: Add center reader helper**

Add:

```ts
function readChartCenter() {
  const option = chart?.getOption() as { geo?: Array<{ center?: number[] }> } | undefined
  return option?.geo?.[0]?.center
}
```

- [ ] **Step 4: Replace toolbar text with icons**

Replace `+`, `-`, and `归位` with visually stable icon buttons. Use inline CSS shapes or text symbols only if no icon library exists in the frontend. Keep button `title` attributes: "放大地图", "缩小地图", "归位到全国视图".

- [ ] **Step 5: Build check**

Run: `cd frontend && pnpm build`

Expected: PASS.

### Task 5: Final Verification

**Files:**
- Verify only

- [ ] **Step 1: Run production build**

Run: `cd frontend && pnpm build`

Expected: PASS.

- [ ] **Step 2: Start dev server for visual check**

Run: `cd frontend && pnpm dev -- --host 0.0.0.0`

Expected: Vite prints a local URL, usually `http://localhost:5173/`.

- [ ] **Step 3: Manual checks**

Open `/dashboard` and verify:

- The map is the background stage under `TopBar`.
- HUD panels float without blocking the map center on desktop.
- Main trunk pipelines render and have animated flow.
- Nodes appear sparsely at country view and increase at deeper/zoomed views.
- Wheel zoom uses smaller increments and remains centered on the current view.
- Reset icon returns to national view and clears pipeline focus.
- Layer toggles do not reset region selection.

## Self-Review

The plan covers the approved first version: full-screen map stage, floating HUD, mock trunk lines, node LOD, centered smooth zoom, and icon reset control. It intentionally defers real pipeline backend integration and production-grade route geometry. The remaining work is implementation and verification.
