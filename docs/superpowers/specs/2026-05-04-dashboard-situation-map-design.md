# Dashboard Situation Map Design

## Goal

Transform the dashboard overview into a high-impact command screen. The map becomes the underlying situation canvas, while KPI, filters, alarms, focus details, and layer controls float above it as HUD panels.

## Scope

This first version covers frontend-only visual and interaction work for the overview page. It keeps the existing administrative map drill-down, existing dashboard KPI/alarm APIs, and existing tooltip loading behavior. It adds a mock trunk pipeline overlay so the page can show an active oil and gas network before real pipeline coordinates are available.

This version does not add backend APIs, database migrations, real pipeline asset binding, or production-grade route geometry.

## Layout

The `TopBar` remains the stable global header. Below it, the dashboard workspace becomes a full-screen situation stage. `DashboardGeoMap` fills the stage as the background map layer instead of sitting inside a conventional grid card.

The current page controls become floating HUD surfaces:

- A KPI strip floats near the top of the map stage.
- The area and time filter panel floats on the left.
- The risk and alarm content forms a narrow right-side event stack.
- A focus card sits near the lower-left area and summarizes the current selected region or pipeline.
- A layer control sits near the lower-right area and toggles administrative areas, trunk pipelines, nodes, and alarms.

The center of the map must remain visually open. HUD panels can overlap the map, but they should not cover the core national/provincial shape on common desktop viewports.

## Map Visual System

The map uses a dark command-room substrate with cyan administrative boundaries and orange risk accents. Administrative areas keep the current country/province/city/district drill-down behavior. Focused areas receive an orange lock highlight and a brief mechanical lock animation when selected.

The stage background includes restrained motion: drifting grid lines, a low-frequency scan beam, and a subtle vignette. Motion must support the command-screen feeling without making map labels or alarm data hard to read.

The old `+`, `-`, and text reset controls are replaced by an icon toolbar. Reset uses a target/location-style icon and keeps a tooltip of "归位到全国视图".

## Trunk Pipeline Overlay

The first version introduces a frontend mock data file for trunk pipelines. It contains roughly five national trunk lines:

- 西气东输主干线
- 陕京输气线
- 川气东送主干线
- 中俄东线天然气管道
- 华南联络支线

Each pipeline includes `id`, `name`, `status`, `pressure`, `flow`, `riskCount`, `coords`, and `nodes`. Coordinates are approximate longitude/latitude pairs suitable for visual overlay only.

Pipeline styling uses cyan-white glow for normal flow, amber/orange for warnings, and orange-red for critical segments. Each visible line has a low-frequency animated flow effect. Hovering a line increases glow and flow emphasis. Clicking a line locks it as the current focus and updates the focus card.

## Node Level Of Detail

Nodes must not appear as a fixed nationwide scatter. They are displayed by map level and zoom:

- Country view shows only 5-10 major hub nodes.
- Province view shows the relevant province's key nodes, generally 10-30 when available.
- City and district views can show finer stations and valve rooms, but ordinary nodes should fade in only at higher zoom.
- Warning and critical nodes always outrank normal nodes.
- Node transitions use opacity and scale changes so nodes do not pop abruptly during zoom.

The initial mock data can include more nodes than are visible at once. Visibility is computed from administrative level, zoom ratio, node priority, node status, and whether the node belongs to the current focused pipeline.

## Zoom Interaction

Mouse wheel zoom must feel smoother than the current implementation. The wheel handler should use smaller zoom increments and continue to use animation-frame batching.

Zooming is centered on the current map view center, not on the mouse cursor. This makes the command-screen view feel stable and avoids the map drifting under floating HUD panels.

The zoom ratio display remains available. At the minimum and maximum zoom limits, the map should clamp cleanly without harsh jumps.

## Layer Controls

Layer controls are a compact HUD control group, not a form-heavy panel. First-version toggles:

- 行政区
- 主干线
- 节点
- 告警

Toggling a layer updates only the visual overlay. It must not reset the selected province/city/district, selected pipeline, or loaded dashboard data.

## Focus And Event Flow

The focus model supports either an administrative region or a trunk pipeline. Region focus keeps existing drill-down behavior. Pipeline focus is frontend-only in the first version.

When a pipeline is selected:

- The selected line receives a lock highlight.
- Its nodes become higher priority in LOD calculations.
- The lower-left focus card displays pipeline name, status, pressure, flow, and risk count.
- The right event stack can visually indicate that the current focus is pipeline-related, while still using existing alarm data until real pipeline alarm binding exists.

When reset is clicked, the page clears pipeline focus and returns to the national region view.

## Testing And Verification

Verification should include:

- `cd frontend && pnpm build`
- Manual desktop check around 1440px and 1920px widths.
- Manual mobile/tablet check that HUD panels stack without unreadable overlap.
- Wheel zoom check that zoom changes in smaller increments and remains centered on the current view.
- Layer toggle check that administrative map drill-down still works.
- Pipeline hover/click check that focus and HUD updates work.

## Open Decisions Resolved

The first version uses mock frontend pipeline coordinates and mock node metadata. Real pipeline asset data is explicitly deferred. Zoom is centered on the current view center, not the mouse position.
