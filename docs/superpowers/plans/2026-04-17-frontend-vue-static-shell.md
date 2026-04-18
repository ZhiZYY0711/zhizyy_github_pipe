# Frontend Vue Static Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the approved login page and map-centered dashboard as a static Vue 3 shell in `frontend/`, using the accepted tokens and layout spec without wiring real backend APIs yet.

**Architecture:** Keep the first implementation intentionally shallow: one app root that switches between two approved pages, one global token stylesheet, and a small set of focused presentational components. Recreate the accepted HTML/CSS prototype structure inside Vue, then verify the build before moving on to any real routing or API integration.

**Tech Stack:** Vue 3, Vite, TypeScript, Single File Components, CSS variables

---

## File Structure

### Files to create

- `frontend/src/data/mockShell.ts`
  - Static content for top nav items, dashboard KPIs, left rail summary blocks, risk items, task items, and login labels.
- `frontend/src/components/shell/TopBar.vue`
  - Dashboard top bar with system block, module nav, and global status chips.
- `frontend/src/components/shell/LeftRail.vue`
  - Left-side summary workbench wrapper.
- `frontend/src/components/shell/SummaryCard.vue`
  - Reusable summary block renderer for hero, bars, radar, and quick-switch variants.
- `frontend/src/components/shell/KpiStrip.vue`
  - Four-block KPI strip for the dashboard.
- `frontend/src/components/shell/MapPanel.vue`
  - Map-centered main panel and static callout overlays.
- `frontend/src/components/shell/SideStack.vue`
  - Right-side risk and task stack container.
- `frontend/src/components/login/LoginStory.vue`
  - Left narrative and graphic zone for the login page.
- `frontend/src/components/login/LoginAccessCard.vue`
  - Right-side login card.
- `frontend/src/styles/tokens.css`
  - CSS custom properties derived from the accepted tokens/layout spec.
- `frontend/src/styles/base.css`
  - Global resets, app-level layout helpers, shared utility classes, and responsive breakpoints.

### Files to modify

- `frontend/src/App.vue`
  - Replace starter content with a small view switcher and approved page shells.
- `frontend/src/main.ts`
  - Import `tokens.css` and `base.css` alongside the app entry.
- `frontend/src/style.css`
  - Remove the Vite starter styles and keep only an import shim or a minimal compatibility layer if needed.

### Files intentionally left untouched

- `frontend/src/components/HelloWorld.vue`
  - Leave unused for now unless cleanup is explicitly requested later.
- Any router, store, or API files
  - Out of scope for this static shell phase.

## Task 1: Replace the starter app with a two-page shell entry

**Files:**
- Modify: `frontend/src/App.vue`

- [ ] **Step 1: Define the root view model**

Write this minimal root switch type inside `frontend/src/App.vue`:

```ts
type ViewKey = 'dashboard' | 'login'
```

The root state should use a simple `ref<ViewKey>('dashboard')`.

- [ ] **Step 2: Replace the starter template with a review switcher**

The new root template should render:

- a compact top review switch bar for `dashboard` and `login`
- the dashboard shell when the current view is `dashboard`
- the login shell when the current view is `login`

Use this structure:

```vue
<template>
  <div class="app-shell">
    <header class="review-switch">
      <button
        v-for="view in views"
        :key="view.key"
        type="button"
        class="review-switch__button"
        :class="{ 'is-active': activeView === view.key }"
        @click="activeView = view.key"
      >
        {{ view.label }}
      </button>
    </header>

    <main class="review-canvas">
      <section v-if="activeView === 'dashboard'" class="dashboard-page">
        <!-- dashboard shell components -->
      </section>

      <section v-else class="login-page-shell">
        <!-- login shell components -->
      </section>
    </main>
  </div>
</template>
```

- [ ] **Step 3: Stop rendering starter content**

Delete the `HelloWorld` import and do not render any default Vite starter markup.

## Task 2: Move accepted design tokens into global CSS variables

**Files:**
- Create: `frontend/src/styles/tokens.css`
- Modify: `frontend/src/main.ts`
- Modify: `frontend/src/style.css`

- [ ] **Step 1: Write the token file**

Create `frontend/src/styles/tokens.css` and define these top-level token groups:

```css
:root {
  --color-bg: #04080d;
  --color-bg-elevated: #070d13;
  --color-panel: #0b131b;
  --color-panel-2: #101922;
  --color-panel-3: #15202b;
  --color-overlay: rgba(18, 28, 38, 0.92);
  --color-line: rgba(143, 166, 182, 0.14);
  --color-line-strong: rgba(143, 166, 182, 0.24);
  --color-text: #edf3f7;
  --color-text-muted: #90a0ab;
  --color-text-dim: #677786;
  --color-accent-orange: #e7682d;
  --color-accent-orange-soft: rgba(231, 104, 45, 0.15);
  --color-accent-cyan: #6ecad4;
  --color-accent-cyan-soft: rgba(110, 202, 212, 0.14);
  --color-warning: #ddb054;
  --color-success: #4ea786;

  --font-sans: "Microsoft YaHei UI", "PingFang SC", "Noto Sans SC", sans-serif;
  --font-serif: "Noto Serif SC", "Source Han Serif SC", "Songti SC", "STSong", serif;
  --font-mono: "JetBrains Mono", "IBM Plex Mono", "Consolas", "Courier New", monospace;

  --text-display-lg: 44px;
  --text-heading-lg: 28px;
  --text-heading-md: 24px;
  --text-section: 16px;
  --text-body-md: 15px;
  --text-body-sm: 13px;
  --text-meta: 12px;
  --text-micro: 10px;
  --text-data-lg: 48px;
  --text-data-md: 30px;
  --text-data-sm: 28px;
  --text-data-xs: 22px;

  --leading-display: 1.15;
  --leading-body: 1.6;
  --leading-relaxed: 1.7;
  --tracking-wide: 0.24em;
  --tracking-panel: 0.12em;
  --tracking-button: 0.14em;
  --tracking-tight: 0.04em;

  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 18px;
  --space-6: 20px;
  --space-7: 24px;
  --space-8: 32px;
  --space-9: 40px;
  --space-10: 72px;
  --space-11: 88px;

  --radius-none: 0;
  --radius-round: 999px;
  --shadow-shell: 0 20px 50px rgba(0, 0, 0, 0.42);
  --shadow-card: 0 28px 70px rgba(0, 0, 0, 0.42);
}
```

- [ ] **Step 2: Write the global import entry**

Update `frontend/src/main.ts` so it imports tokens and base styles before mounting:

```ts
import { createApp } from 'vue'
import App from './App.vue'
import './styles/tokens.css'
import './styles/base.css'

createApp(App).mount('#app')
```

- [ ] **Step 3: Remove the starter style dependency**

Replace the contents of `frontend/src/style.css` with either an empty file or a one-line comment so the Vite starter theme is no longer applied.

## Task 3: Build the shared base layout stylesheet

**Files:**
- Create: `frontend/src/styles/base.css`

- [ ] **Step 1: Add resets and app-shell primitives**

Create the global reset and app-level layout:

```css
*,
*::before,
*::after {
  box-sizing: border-box;
}

html,
body,
#app {
  margin: 0;
  min-height: 100%;
}

body {
  min-height: 100vh;
  font-family: var(--font-sans);
  color: var(--color-text);
  background:
    radial-gradient(circle at top left, rgba(110, 202, 212, 0.06), transparent 28%),
    radial-gradient(circle at bottom right, rgba(231, 104, 45, 0.07), transparent 32%),
    linear-gradient(180deg, #03070b 0%, #05090f 100%);
}

button,
input {
  font: inherit;
}
```

- [ ] **Step 2: Add review shell and shared utility classes**

Define:

- `.app-shell`
- `.review-switch`
- `.review-switch__button`
- `.review-canvas`
- `.eyebrow`
- `.panel-title`
- `.data-value`

Use this review shell contract:

```css
.app-shell {
  min-height: 100vh;
  display: grid;
  grid-template-rows: auto 1fr;
}

.review-switch {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-4);
  border-bottom: 1px solid var(--color-line);
  background: rgba(4, 8, 13, 0.86);
  position: sticky;
  top: 0;
  z-index: 20;
}

.review-canvas {
  min-height: 0;
}
```

- [ ] **Step 3: Add dashboard and login page-level responsive rules**

Create the breakpoint blocks for:

- `>= 1440px`
- `1180px - 1439px`
- `768px - 1179px`
- `< 720px`

Expected behaviors:

- dashboard desktop keeps the accepted single-screen hierarchy
- medium widths stack left rail above the map and move side panels below
- login page collapses from two columns to one below `1180px`

## Task 4: Add static mock data for presentational rendering

**Files:**
- Create: `frontend/src/data/mockShell.ts`

- [ ] **Step 1: Add top navigation and status data**

Export:

```ts
export const topNavItems = ['总览', '监测', '资产', '任务', '应急', '日志']

export const globalStatusItems = [
  { label: '高风险告警', value: '07', tone: 'alert' },
  { label: '系统状态', value: 'STABLE', tone: 'default' },
  { label: '当前时间', value: '17:24:36', tone: 'default' },
]
```

- [ ] **Step 2: Add dashboard summary and right-stack data**

Export arrays for:

- KPI blocks
- left rail summary block content
- risk items
- task items

These values should mirror the accepted HTML prototype and stay static.

- [ ] **Step 3: Add login copy and meta data**

Export:

- mission eyebrow
- mission title
- mission lead
- story tags
- access card labels
- access meta text

## Task 5: Build dashboard shell components

**Files:**
- Create: `frontend/src/components/shell/TopBar.vue`
- Create: `frontend/src/components/shell/LeftRail.vue`
- Create: `frontend/src/components/shell/SummaryCard.vue`
- Create: `frontend/src/components/shell/KpiStrip.vue`
- Create: `frontend/src/components/shell/MapPanel.vue`
- Create: `frontend/src/components/shell/SideStack.vue`

- [ ] **Step 1: Implement `TopBar.vue`**

Render:

- system block with eyebrow + system name
- six-item top nav
- three global status chips

Use props for nav items and status items, no internal fetch logic.

- [ ] **Step 2: Implement `SummaryCard.vue`**

Support four variants:

- `hero`
- `bars`
- `radar`
- `switches`

The component must only render approved structures, not a generic slot soup.

- [ ] **Step 3: Implement `LeftRail.vue`**

Render the four accepted summary sections in order:

1. 当前视角
2. 运行态势
3. 关注事项
4. 快速切换

- [ ] **Step 4: Implement `KpiStrip.vue`**

Render the four compressed KPI blocks with mono values and tone classes.

- [ ] **Step 5: Implement `MapPanel.vue`**

Recreate the prototype map stage:

- title bar
- synthetic map shape
- hot rings
- path lines
- two short callouts

Do not add real maps or chart libraries.

- [ ] **Step 6: Implement `SideStack.vue`**

Render exactly two panels:

- 重点风险
- 处置进度

Each panel should render three static items max.

## Task 6: Build login shell components

**Files:**
- Create: `frontend/src/components/login/LoginStory.vue`
- Create: `frontend/src/components/login/LoginAccessCard.vue`

- [ ] **Step 1: Implement `LoginStory.vue`**

Render:

- left background grid
- three orbit layers
- signal clusters
- one mission eyebrow
- one mission headline
- one short lead
- one ring summary module
- one tag row

Keep text intentionally sparse.

- [ ] **Step 2: Implement `LoginAccessCard.vue`**

Render:

- access eyebrow
- card title
- short access note
- account input
- password input
- captcha input + captcha box
- primary action button
- two access meta items

Do not add validation or real submit behavior yet.

## Task 7: Compose the final static Vue pages

**Files:**
- Modify: `frontend/src/App.vue`

- [ ] **Step 1: Compose the dashboard page**

Assemble the dashboard view with this structure:

```vue
<section class="dashboard-page">
  <div class="dashboard-frame">
    <TopBar ... />
    <div class="dashboard-workspace">
      <LeftRail ... />
      <section class="dashboard-main">
        <KpiStrip ... />
        <div class="dashboard-board">
          <MapPanel />
          <SideStack ... />
        </div>
      </section>
    </div>
  </div>
</section>
```

- [ ] **Step 2: Compose the login page**

Assemble the login page with:

```vue
<section class="login-page-shell">
  <LoginStory ... />
  <LoginAccessCard ... />
</section>
```

- [ ] **Step 3: Keep the implementation static**

Do not add:

- Vue Router
- Pinia
- API hooks
- form validation libraries
- chart libraries

## Task 8: Verify the static shell build

**Files:**
- Verify: `frontend/package.json`
- Verify: `frontend/src/App.vue`
- Verify: `frontend/src/styles/tokens.css`
- Verify: `frontend/src/styles/base.css`

- [ ] **Step 1: Run the production build**

Run:

```bash
npm.cmd run build
```

in `frontend/`

Expected:

- `vue-tsc` passes
- Vite build succeeds
- no unresolved component imports

- [ ] **Step 2: Review the built shell against the accepted prototype**

Check:

- dashboard stays map-centered
- dashboard has no bottom trend area
- left rail stays summary-first and text-light
- login page keeps the graphically led left side
- typography hierarchy matches the accepted spec

- [ ] **Step 3: Prepare post-build review notes**

Summarize:

- which parts are now live in Vue
- which parts remain static by design
- what the next migration step should be after approval

## Self-Review

Spec coverage check:

- Tokens, dashboard layout, login layout, typography, single-screen dashboard, and map-centered structure are all covered by Tasks 2 through 7.
- No backend integration requirement exists in the spec, so leaving APIs out is consistent.

Placeholder scan:

- No `TODO`, `TBD`, or “similar to above” placeholders remain.

Type consistency:

- Root view keys are consistently defined as `dashboard` and `login`.
- Shared naming follows the layout spec naming set: top bar, left rail, KPI strip, map panel, side stack, login story, access card.
