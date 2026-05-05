# 总览大屏地图引擎迁移 Spec

## 决策

总览大屏地图底座从 ECharts GeoJSON 动态地图迁移到 MapLibre GL JS + deck.gl。MapLibre 负责地图相机、行政区矢量切片、底图样式和基础交互；deck.gl 负责管线、节点、告警、热力、飞线和后续高密度业务 overlay。

行政区边界不再以整包 GeoJSON 方式在前端加载、合并和 `registerMap`，而是预处理成 PMTiles 或标准 MVT 矢量切片，通过静态资源、OSS 或 CDN 按视口和 zoom 渐进加载。业务图层从第一阶段开始就使用 deck.gl 图层，避免先写一版 MapLibre 原生业务 layer 再二次迁移。

## 背景

当前 `frontend/src/modules/dashboard/components/DashboardGeoMap.vue` 使用 ECharts `MapChart`、`LinesChart` 和 `EffectScatterChart`。地图会按需加载行政区 GeoJSON，在缩放时动态合并可见下级边界，调用 `registerMap` 和 `setOption`，同时绘制管线、节点和告警动效。

排查结果显示性能问题不是单点参数导致的。`frontend/public/geo` 有 3183 个 JSON 文件，未压缩约 274 MB，`geo/index.json` 约 877 KB。现有 `frontend/scripts/build-geo-cdn-bundle.mjs` 可以把资源降到约 27 MB、364 个 JSON，这能明显改善网络传输、CDN 缓存和请求数量，但不能根治交互卡顿。原因是浏览器仍要在主线程解析 GeoJSON、合并 feature、注册地图、重绘 ECharts，并维持多组动画。

如果后续要继续深化大屏功能，继续在 ECharts map 上堆叠复杂 GIS 交互和高密度动态图层，上限不够。一步到位的优化方向应该同时替换渲染引擎和数据形态。

## 目标

第一个目标是把总览页地图底座迁到 MapLibre GL JS + deck.gl，并保留现有 `DashboardPage.vue` 对地图组件的业务契约。区域点击、区域 hover、管线点击、重置视图、图层开关、选中管线和 tooltip 数据语义都应保持一致。

第二个目标是把行政区 Geo 数据从整包 GeoJSON 改成矢量切片。浏览器只加载当前视口和 zoom 需要的瓦片，避免缩放时前端合并大量 feature，也避免反复 `registerMap`。

第三个目标是为后续大屏深化留出空间。地图底座要支持行政区分层样式、hover 高亮、点击钻取、业务图层过滤、告警优先级、选中态、后续热力、飞线、轨迹和更高密度点线数据。

第四个目标是建立可验证的性能验收。迁移后应通过构建、静态资源完整性、浏览器控制台、网络请求、长任务采样和核心交互回归。

## 非目标

本 spec 不迁移其他 ECharts 图表。KPI 条、侧栏统计、趋势图等非地图图表可以继续使用现有实现。

本 spec 不新增后端业务 API，不引入数据库迁移，也不要求引入 PostGIS。行政区切片先作为前端静态构建产物生成和部署。

本 spec 不做精确 GIS 编辑器能力。总览大屏需要态势展示、筛选、钻取和告警定位，不需要测绘级编辑、绘制、拓扑校验或复杂空间分析。

本 spec 不重构后端 KPI、告警和区域监测聚合查询。后端与数据库性能由独立 spec 约束，本文只定义地图渲染迁移。

## 架构方案

### 前端组件边界

新增或重写一个 MapLibre + deck.gl 版地图组件，建议命名为 `DashboardDeckMap.vue`，并尽量复用当前 `DashboardGeoMap.vue` 的 props 和 emits：

`region`、`focusCode`、`tooltipData`、`loading`、`pipelines`、`visibleLayers`、`selectedPipelineId` 继续由 `DashboardPage.vue` 传入。

`regionClick`、`regionHover`、`pipelineClick`、`resetView` 继续向父组件发出。这样父页面的区域筛选、tooltip 加载、告警联动和选中管线逻辑可以少改。

地图组件内部拆成五类模块：MapLibre 实例生命周期、行政区 source/layer、deck.gl overlay 生命周期、业务 layer 构建、交互事件适配。组件负责组装，具体样式和数据转换放到独立 TypeScript helper，避免重新形成一个超大组件。

### 行政区切片数据

新增一个静态构建脚本，例如 `frontend/scripts/build-dashboard-map-tiles.mjs`。脚本读取现有 `frontend/public/geo`，提取 `code`、`name`、`level`、`parentCode` 等属性，生成一个行政区边界 tileset。

首选产物是 `frontend/dist/geo-tiles/v1/admin-boundaries.pmtiles`。PMTiles 可以作为单文件静态资源部署到 OSS/CDN，通过 HTTP range request 按需读取。若生产环境更偏好传统瓦片目录，也可以输出 `{z}/{x}/{y}.pbf` MVT 目录，但前端接口应抽象成同一套 source 配置。

切片层建议按层级拆分为 `province`、`city`、`district` 三个 source-layer，或至少在 feature 属性中保留 `level` 以便 MapLibre style expression 过滤。不同 zoom 展示不同层级：低 zoom 显示省级边界，中 zoom 显示市级边界，高 zoom 显示区县边界。具体 zoom 阈值在 POC 后根据视觉效果微调。

原来的 `build-geo-cdn-bundle.mjs` 可以保留为 ECharts 回滚路线或调试工具，但新主链路不再依赖它为大屏地图提供运行时 GeoJSON。

### MapLibre 样式

总览页使用自定义暗色大屏 style，不依赖公网底图。底图由背景色、行政区 fill、边界 line、标签 symbol 和业务图层组成，确保内网和离线部署可用。

行政区图层分为基础面、边界线、hover 高亮、focus 高亮和标签层。hover 与 focus 优先使用 MapLibre `feature-state` 或 filter 更新，避免替换整份 source 数据。

地图交互关闭不必要能力。默认保留 scroll zoom、drag pan 和 double click zoom；禁用 rotation 和 pitch，除非后续明确要做 3D 态势。

### deck.gl 业务图层

业务 overlay 使用 deck.gl 的 MapLibre 集成方式接入，与 MapLibre 相机同步。MapLibre 只负责底图和行政区；所有业务点线面都由 deck.gl 管理，避免业务图层在两个渲染体系中来回迁移。

管线数据由现有 `trunkPipelines` 转成稳定的二进制友好结构或 GeoJSON。第一阶段使用 deck.gl `PathLayer` 渲染管线主线，按状态和选中态拆分 layer 或使用 accessor 控制颜色、宽度和透明度。正常管线保持静态；选中、预警和严重管线使用高优先级 layer 表达。

节点数据由管线节点展开为点集合。第一阶段使用 deck.gl `ScatterplotLayer` 或 `IconLayer` 渲染普通节点，使用单独的告警 layer 渲染危险和高危节点。告警闪烁应通过少量 uniform/state 更新实现，不应每帧重建 data 数组。

飞线、热力和轨迹能力直接预留在 deck.gl layer 架构内。后续可用 `ArcLayer` 表达跨区调度或抢修流向，用 `HeatmapLayer` 或 `ScreenGridLayer` 表达告警密度，用 `TripsLayer` 或自定义 layer 表达巡检/抢修轨迹。L7 不作为本次主依赖，只作为未来国产可视化生态备选。

deck.gl layer 的 data 引用必须稳定。筛选、选中、hover 等交互优先通过 accessors、`updateTriggers` 和小范围状态更新表达，避免 Vue reactive 深层变更导致大规模 GPU buffer 重建。

### 交互和数据流

区域 hover 使用 `queryRenderedFeatures` 读取当前鼠标下的行政区 feature，并通过 feature 属性拿到 `code` 和 `name`。触发父组件 `regionHover` 时继续沿用现有 tooltip 数据加载逻辑，但应保留 debounce 和过期请求保护，避免鼠标移动造成接口风暴。

区域 click 同样从 rendered feature 中读取行政区编码，向父组件发出 `regionClick`。父组件仍决定当前区域、下拉筛选和 KPI 刷新，地图只负责把用户地图操作翻译成业务事件。

管线 click 优先使用 deck.gl picking 命中业务 layer，命中后发出 `pipelineClick`。若同时命中行政区和管线，管线优先，因为它是更具体的目标。

`focusCode` 变化时，地图应 flyTo 或 fitBounds 到对应行政区。行政区 bounds 可以来自切片 feature 查询、构建时生成的 metadata，或保留一份轻量 `admin-index.json`。为了稳定，建议构建脚本同时输出 `admin-index.json`，包含 code、name、level、parentCode、bbox、center。

## 迁移策略

保留 ECharts 地图作为短期回滚，不作为长期双实现。可以用 `VITE_DASHBOARD_MAP_ENGINE=deck|echarts` 控制，默认切到 deck 版地图。验收稳定后删除 ECharts 地图组件中的运行时 GeoJSON 合并逻辑。

第一步先完成切片构建链路和一个最小 MapLibre 底图：暗色背景、省市区边界、hover 高亮、click 输出 code/name。

第二步接入 deck.gl overlay：管线 `PathLayer`、节点 `ScatterplotLayer/IconLayer`、告警 layer、选中态和图层开关。

第三步迁移视图联动：区域筛选后 fitBounds，点击地图后更新父页面筛选，hover tooltip 数据继续接入现有接口。

第四步做性能验收和视觉回归。通过后，旧 ECharts 地图只保留一个短窗口作为回滚路径。

## 验收标准

`frontend` 下执行地图切片构建脚本必须成功，并生成 `dist/geo-tiles/v1/admin-boundaries.pmtiles` 和 `dist/geo-tiles/v1/admin-index.json`。`admin-index.json` 中每个行政区必须包含可用的 `code`、`name`、`level`、`bbox` 和 `center`。

`pnpm build` 必须通过。

Dashboard 默认地图引擎为 deck 版地图时，浏览器打开 `/dashboard` 不应再请求 `/geo/admin/*.json` 或运行时加载大量行政区 GeoJSON。允许请求 PMTiles range 或 MVT 瓦片资源。

核心交互必须保持：区域 hover 展示加载态和区域指标，区域 click 更新当前区域，图层按钮能控制行政区、管线、节点、告警显示，deck.gl 管线 picking 能更新焦点卡片，筛选面板选择区域后地图能定位。

性能采样必须优于当前基线。此前连续 zoom 观察到多次约 70-200 ms long task。迁移后，在同一机器、同一浏览器、同一操作脚本下，重复滚轮缩放不应再出现连续的 100 ms 以上长任务；地图缩放和平移期间应保持可交互，不出现明显冻结。

视觉上需要保持大屏暗色指挥态势风格。允许和 ECharts 旧地图有细节差异，但行政区层级、业务点线、告警优先级和焦点态必须清晰。

## 风险和取舍

MapLibre 使用 WebMercator 地图模型，旧 ECharts map 的视觉投影和缩放手感会有差异。需要通过 style、初始 center/zoom、fitBounds padding 和中国范围约束来校准大屏观感。

切片构建会引入新的工具链。若使用 Tippecanoe、PMTiles CLI 或 Docker 生成瓦片，需要在前端构建文档和 CI 中明确依赖。这个复杂度是值得的，因为它把运行时主线程压力转移到构建期。

PMTiles 依赖 HTTP range request。部署到 OSS/CDN/Nginx 时必须确认 range 和缓存头工作正常。若生产 CDN 对 range 支持不理想，可切换为传统 MVT 瓦片目录。

deck.gl 引入第二个 WebGL overlay，工程复杂度高于 MapLibre-only 方案。这个复杂度是有意接受的，因为用户后续要继续深化大屏功能，提前统一业务图层到 deck.gl 可以避免二次迁移。

deck.gl 动画能力强，但不能滥用每帧 data 更新。大屏动效需要重新设计为状态驱动和 GPU 友好的 accessor 更新，而不是全量持续动画。这符合性能目标，但视觉会从“全场运动”转向“风险和选中优先”。

## 验证计划

先验证切片构建。构建脚本应输出 PMTiles 和 metadata，并检查 feature 数量、行政区编码覆盖率、bbox/center 完整性和产物大小。

再验证前端构建。执行 `pnpm build`，确保 TypeScript、Vite、MapLibre、PMTiles 和 deck.gl 新依赖都能通过生产构建。

然后做浏览器回归。使用 Playwright CLI 打开 `/dashboard`，注入开发 token，记录控制台错误、网络请求、canvas/WebGL 上下文、初始地图是否非空、区域 hover/click、deck.gl picking、图层开关和筛选定位。

最后做性能采样。复用当前基线脚本进行连续滚轮缩放，记录 long task、frame delay 和网络请求数量。验收重点是交互不卡死、无连续 100 ms 以上长任务、无重复拉取大 GeoJSON、无控制台错误。

## 参考资料

MapLibre GL JS 官方文档说明其使用 WebGL 在浏览器中渲染 vector tiles，并通过 style document 控制地图表现：https://maplibre.org/maplibre-gl-js/docs/

MapLibre 官方 PMTiles 示例说明可以通过 PMTiles protocol 使用单文件矢量切片：https://maplibre.org/maplibre-gl-js/docs/examples/pmtiles-source-and-protocol/

deck.gl 官方文档说明其面向 WebGPU/WebGL2 的大规模数据可视化，并可与 MapLibre 等底图集成：https://deck.gl/docs

L7 是 AntV 的 WebGL 地理空间可视化引擎，可作为后续高密度业务图层备选：https://github.com/antvis/L7
