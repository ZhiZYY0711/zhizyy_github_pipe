# Dashboard Geo Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the dashboard Geo CDN bundle valid and reduce map interaction jank caused by dynamic boundary loading and always-on overlay animation.

**Architecture:** Keep the change frontend-only. The Geo bundle script must generate an index where every referenced path exists inside `dist/geo-cdn/v1`. The map component keeps its current ECharts architecture but loads detail layers later, loads fewer parent regions per zoom stage, and animates only warning/critical or selected pipeline flow.

**Tech Stack:** Node.js ESM scripts, Vue 3 Composition API, ECharts 6, Vitest/Node test runner, Playwright CLI for runtime sampling.

---

## File Structure

- Modify `frontend/scripts/build-geo-cdn-bundle.mjs`: map unmatched district entries to bundled province files when city-level matching is unavailable.
- Create `frontend/scripts/build-geo-cdn-bundle.test.mjs`: regression test that runs the script and verifies every generated index path exists.
- Modify `frontend/src/modules/dashboard/components/DashboardGeoMap.vue`: reduce detail layer churn and always-on animation cost.

## Tasks

### Task 1: Guard Geo Bundle Output

**Files:**
- Create: `frontend/scripts/build-geo-cdn-bundle.test.mjs`
- Modify: `frontend/scripts/build-geo-cdn-bundle.mjs`

- [ ] **Step 1: Write failing test**

Create a Node test that runs `build-geo-cdn-bundle.mjs`, reads `dist/geo-cdn/v1/index.json`, and asserts every `china`, `province`, `city`, and `district` entry points to a file inside the generated bundle.

- [ ] **Step 2: Verify failure**

Run: `node --test scripts/build-geo-cdn-bundle.test.mjs`

Expected before implementation: FAIL because municipality district paths still point to `/geo/admin/district/*.json`, which is not generated.

- [ ] **Step 3: Implement district fallback**

Build a fallback map from province GeoJSON features in addition to city GeoJSON features. Keep city entries preferred, then fallback to province entries for direct-admin municipality districts and other province-level district geometries.

- [ ] **Step 4: Verify pass**

Run: `node --test scripts/build-geo-cdn-bundle.test.mjs`

Expected after implementation: PASS and generated bundle remains about 364 JSON files.

### Task 2: Reduce Map Interaction Work

**Files:**
- Modify: `frontend/src/modules/dashboard/components/DashboardGeoMap.vue`

- [ ] **Step 1: Reduce detail update frequency**

Increase the detail layer debounce so wheel/pan interactions settle before fetching and registering child maps.

- [ ] **Step 2: Load fewer child boundaries per detail stage**

Lower visible parent limits for country-to-city and city-to-district expansion so a single zoom stage does not fetch/register too many GeoJSON files.

- [ ] **Step 3: Reduce always-on overlay animation**

Keep static pipeline lines visible, but animate flow only for selected, warning, or critical pipelines. This preserves operational emphasis and reduces canvas animation cost for normal lines.

### Task 3: Verify

**Files:**
- Verify only

- [ ] **Step 1: Run focused tests**

Run: `cd frontend && node --test scripts/build-geo-cdn-bundle.test.mjs`

- [ ] **Step 2: Run frontend build**

Run: `cd frontend && pnpm build`

- [ ] **Step 3: Sample runtime**

Use Playwright CLI on `/dashboard` and repeat the previous wheel sampling. Confirm there are no console errors and compare long-task/frame-delay shape against the earlier run.
