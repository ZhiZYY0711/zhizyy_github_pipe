# Dashboard Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the dashboard map runtime with MapLibre GL JS + deck.gl and move dashboard backend reads onto summary/read-model APIs.

**Architecture:** The frontend uses MapLibre for vector-tile administrative boundaries and deck.gl for all business overlays. The backend adds a dashboard read service, scoped area resolver, current/metric/alarm read models, and merged dashboard APIs while keeping old API routes compatible.

**Tech Stack:** Vue 3, TypeScript, Vite, MapLibre GL JS, deck.gl, Node tile build scripts, Spring Boot 3.5, MyBatis, MySQL, JUnit 5, Mockito.

---

## File Structure

Frontend files:
- Modify `frontend/package.json`: add map/tile dependencies and scripts.
- Create `frontend/scripts/build-dashboard-map-tiles.mjs`: build MVT directory and `admin-index.json` from `public/geo`.
- Create `frontend/src/modules/dashboard/map/adminIndex.ts`: load dashboard admin metadata.
- Create `frontend/src/modules/dashboard/map/deckLayers.ts`: build deck.gl pipeline/node/alarm layers.
- Create `frontend/src/modules/dashboard/components/DashboardDeckMap.vue`: MapLibre + deck.gl map component.
- Modify `frontend/src/modules/dashboard/DashboardPage.vue`: switch dashboard map component and summary API loading.
- Modify `frontend/src/modules/dashboard/api.ts`: add dashboard summary and tooltip API calls.
- Modify `frontend/src/modules/dashboard/service.ts`: normalize summary/tooltip responses and keep old API fallback helpers.
- Modify `frontend/src/modules/dashboard/types.ts`: add summary, freshness, and admin metadata types.
- Test `frontend/src/modules/dashboard/service.test.ts`: summary normalization and fallback behavior.

Backend files:
- Create `database/mysql/04-dashboard-read-models.sql`: read model tables, indexes, and backfill SQL.
- Create `server-pojo/src/main/java/com/gxa/pipe/dataVsualization/dashboard/*.java`: dashboard DTOs.
- Create `server/server-web/src/main/java/com/gxa/pipe/dataVsualization/dashboard/*.java`: controller, mapper, service, scope resolver, cache.
- Create `server/server-web/src/main/resources/com/gxa/pipe/dataVsualization/DashboardMapper.xml`: MyBatis dashboard read queries.
- Modify `server/server-web/src/main/java/com/gxa/pipe/dataVsualization/total/TotalServiceImpl.java`: route legacy KPI/alarm reads through dashboard service where possible.
- Modify `server/server-web/src/main/java/com/gxa/pipe/dataVsualization/dataMonitoring/DataMonitoringServiceImpl.java`: route legacy area data through dashboard service where possible.
- Modify `server/server-web/src/main/resources/application.yml`: add dashboard read model config defaults.
- Test `server/server-web/src/test/java/com/gxa/pipe/dataVsualization/dashboard/*.java`: scope resolver, cache, service fallback-free behavior.

## Task 1: Frontend Dependencies And Tile Builder

**Files:**
- Modify: `frontend/package.json`
- Create: `frontend/scripts/build-dashboard-map-tiles.mjs`
- Test: command-level script verification

- [ ] **Step 1: Add dependencies**

Run:

```bash
cd frontend && pnpm add maplibre-gl deck.gl @deck.gl/core @deck.gl/layers @deck.gl/mapbox pmtiles
cd frontend && pnpm add -D geojson-vt vt-pbf @mapbox/tilebelt
```

Expected: `frontend/package.json` and `frontend/pnpm-lock.yaml` update.

- [ ] **Step 2: Add tile build script**

Add script:

```json
"geo:tiles": "node scripts/build-dashboard-map-tiles.mjs"
```

- [ ] **Step 3: Implement `build-dashboard-map-tiles.mjs`**

The script must:
- Read `public/geo/index.json`.
- Load all referenced GeoJSON files that exist under `public/geo`.
- Normalize feature properties to `code`, `name`, `level`, `parentCode`.
- Write `dist/geo-tiles/v1/admin-index.json` with `code`, `name`, `level`, `bbox`, `center`.
- Write MVT tiles to `dist/geo-tiles/v1/admin/{z}/{x}/{y}.pbf` for zoom 3-9.
- Fail if no features are found.

- [ ] **Step 4: Verify tile build**

Run:

```bash
cd frontend && pnpm geo:tiles
```

Expected: exit 0 and files under `frontend/dist/geo-tiles/v1/admin`.

## Task 2: Frontend Types And API Normalization

**Files:**
- Modify: `frontend/src/modules/dashboard/types.ts`
- Modify: `frontend/src/modules/dashboard/api.ts`
- Modify: `frontend/src/modules/dashboard/service.ts`
- Test: `frontend/src/modules/dashboard/service.test.ts`

- [ ] **Step 1: Write failing tests**

Add tests proving:
- `loadDashboardSummary()` accepts the new merged payload and returns KPI/alarm/trend fields.
- `loadMapTooltipData()` uses `/data_visualization/dashboard/area_tooltip` response shape.
- Existing `loadGeoIndex()` fallback remains intact.

Run:

```bash
cd frontend && pnpm vitest run src/modules/dashboard/service.test.ts
```

Expected: new tests fail because functions/types do not exist yet.

- [ ] **Step 2: Implement types and API functions**

Add `DashboardSummaryResponse`, `DashboardFreshness`, and `AdminBoundaryIndex` types. Add API functions for:
- `/data_visualization/dashboard/summary`
- `/data_visualization/dashboard/area_tooltip`

- [ ] **Step 3: Implement service normalization**

Add:
- `loadDashboardSummary(areaId, range)`
- update `loadMapTooltipData(areaId, areaName, range)` to prefer new tooltip endpoint and fall back to old combined calls if needed.

- [ ] **Step 4: Verify service tests**

Run:

```bash
cd frontend && pnpm vitest run src/modules/dashboard/service.test.ts
```

Expected: pass.

## Task 3: MapLibre + deck.gl Component

**Files:**
- Create: `frontend/src/modules/dashboard/map/adminIndex.ts`
- Create: `frontend/src/modules/dashboard/map/deckLayers.ts`
- Create: `frontend/src/modules/dashboard/components/DashboardDeckMap.vue`
- Modify: `frontend/src/modules/dashboard/DashboardPage.vue`

- [ ] **Step 1: Implement admin index loader**

Load `/geo-tiles/v1/admin-index.json` by default, supporting `VITE_GEO_TILES_BASE_URL`. Return empty-safe metadata if unavailable.

- [ ] **Step 2: Implement deck layer builder**

Build deck.gl layers:
- `PathLayer` for pipelines.
- `ScatterplotLayer` for nodes.
- `ScatterplotLayer` for alarms.
- Picking object must preserve `pipelineId` and node metadata.

- [ ] **Step 3: Implement `DashboardDeckMap.vue`**

The component must:
- Create MapLibre map with dark style and vector tile source `/geo-tiles/v1/admin/{z}/{x}/{y}.pbf`.
- Add fill, line, hover, focus, and label layers for administrative boundaries.
- Attach deck.gl `MapboxOverlay`.
- Emit `regionHover`, `regionClick`, and `pipelineClick`.
- Respect `visibleLayers` and `selectedPipelineId`.
- Dispose map and overlay on unmount.

- [ ] **Step 4: Wire into `DashboardPage.vue`**

Replace `DashboardGeoMap` import/use with `DashboardDeckMap`. Keep old component file as rollback code, but do not render it by default.

- [ ] **Step 5: Verify frontend build**

Run:

```bash
cd frontend && pnpm build
```

Expected: type check and Vite build pass.

## Task 4: Backend Dashboard Read Model DTOs And Scope

**Files:**
- Create: `server-pojo/src/main/java/com/gxa/pipe/dataVsualization/dashboard/DashboardFreshness.java`
- Create: `server-pojo/src/main/java/com/gxa/pipe/dataVsualization/dashboard/DashboardSummaryResponse.java`
- Create: `server-pojo/src/main/java/com/gxa/pipe/dataVsualization/dashboard/DashboardTooltipResponse.java`
- Create: `server/server-web/src/main/java/com/gxa/pipe/dataVsualization/dashboard/AreaScope.java`
- Create: `server/server-web/src/main/java/com/gxa/pipe/dataVsualization/dashboard/AreaScopeResolver.java`
- Test: `server/server-web/src/test/java/com/gxa/pipe/dataVsualization/dashboard/AreaScopeResolverTest.java`

- [ ] **Step 1: Write failing scope tests**

Test country, province, city, and district input:
- `null` -> `COUNTRY`, rollup `100000`
- `370000` -> `PROVINCE`, range `370000..379999`
- `370100` -> `CITY`, range `370100..370199`
- `370102` -> `DISTRICT`, exact range

Run:

```bash
cd server && mvn -pl server-web -Dtest=AreaScopeResolverTest test
```

Expected: fail because classes do not exist.

- [ ] **Step 2: Implement DTOs and resolver**

Use Java records where practical for internal types and Lombok-compatible POJOs for API responses.

- [ ] **Step 3: Verify scope tests**

Run:

```bash
cd server && mvn -pl server-web -Dtest=AreaScopeResolverTest test
```

Expected: pass.

## Task 5: Backend Mapper, Service, Controller, Cache

**Files:**
- Create: `server/server-web/src/main/java/com/gxa/pipe/dataVsualization/dashboard/DashboardMapper.java`
- Create: `server/server-web/src/main/java/com/gxa/pipe/dataVsualization/dashboard/DashboardReadService.java`
- Create: `server/server-web/src/main/java/com/gxa/pipe/dataVsualization/dashboard/DashboardReadServiceImpl.java`
- Create: `server/server-web/src/main/java/com/gxa/pipe/dataVsualization/dashboard/DashboardController.java`
- Create: `server/server-web/src/main/java/com/gxa/pipe/dataVsualization/dashboard/DashboardLocalCache.java`
- Create: `server/server-web/src/main/resources/com/gxa/pipe/dataVsualization/DashboardMapper.xml`
- Test: `server/server-web/src/test/java/com/gxa/pipe/dataVsualization/dashboard/DashboardReadServiceImplTest.java`

- [ ] **Step 1: Write failing service tests**

Test that summary:
- reads KPI current summary from mapper
- reads metric trend from mapper
- reads recent alarms from mapper
- includes freshness
- returns zero-value response when mapper returns null rows

Run:

```bash
cd server && mvn -pl server-web -Dtest=DashboardReadServiceImplTest test
```

Expected: fail because service does not exist.

- [ ] **Step 2: Implement mapper interface and XML**

Mapper methods:
- `selectCurrentSummary(areaId)`
- `selectMetricTrend(areaId, startDate, endDate)`
- `selectRecentAlarms(areaId, startTime, endTime, limit)`
- `selectLatestFreshness()`

- [ ] **Step 3: Implement service and cache**

Service methods:
- `getSummary(Long areaId, Long startTime, Long endTime)`
- `getTooltip(Long areaId, Long startTime, Long endTime)`

Cache should use `ConcurrentHashMap`, expiry millis, and immutable entries.

- [ ] **Step 4: Implement controller**

Routes:
- `GET /data_visualization/dashboard/summary`
- `GET /data_visualization/dashboard/area_tooltip`

- [ ] **Step 5: Verify service tests**

Run:

```bash
cd server && mvn -pl server-web -Dtest=DashboardReadServiceImplTest test
```

Expected: pass.

## Task 6: Database Migration

**Files:**
- Create: `database/mysql/04-dashboard-read-models.sql`

- [ ] **Step 1: Write idempotent SQL**

Create:
- `dashboard_area_metric_daily`
- `dashboard_area_current_summary`
- `dashboard_alarm_recent`
- `dashboard_read_model_refresh_state`

Add indexes named in the backend spec and backfill from existing aggregate/source tables.

- [ ] **Step 2: Verify SQL is parseable enough for review**

Run:

```bash
rg -n "CREATE TABLE IF NOT EXISTS|add_index_if_not_exists|dashboard_area_metric_daily|dashboard_alarm_recent" database/mysql/04-dashboard-read-models.sql
```

Expected: all required tables and indexes are present.

## Task 7: Legacy Service Compatibility

**Files:**
- Modify: `server/server-web/src/main/java/com/gxa/pipe/dataVsualization/total/TotalServiceImpl.java`
- Modify: `server/server-web/src/main/java/com/gxa/pipe/dataVsualization/dataMonitoring/DataMonitoringServiceImpl.java`
- Test: existing service tests plus new dashboard service tests

- [ ] **Step 1: Route legacy total service**

Use `DashboardReadService` for `getWholeKpi()` and `getRunningWaterAlarm()` when dashboard read model is available. Fall back to existing mapper if dashboard service throws.

- [ ] **Step 2: Route legacy area data**

Use `DashboardReadService` for area metric trend when possible. Preserve existing aggregate/raw fallback behavior.

- [ ] **Step 3: Verify backend tests**

Run:

```bash
cd server && mvn -pl server-web -am test
```

Expected: pass.

## Task 8: End-To-End Verification And Review

**Files:**
- No planned edits.

- [ ] **Step 1: Full frontend build**

Run:

```bash
cd frontend && pnpm build
```

Expected: pass.

- [ ] **Step 2: Backend compile/tests**

Run:

```bash
cd server && mvn -pl server-web -am test
```

Expected: pass.

- [ ] **Step 3: Start dev server for manual/Playwright smoke if builds pass**

Run:

```bash
cd frontend && pnpm dev
```

Expected: Vite dev URL available.

- [ ] **Step 4: Spawn subagents**

After implementation and local verification, spawn:
- Review agent: code-review stance over changed frontend/backend files.
- Test agent: run/inspect automated tests and report failures with command output.
