# Frontend Sample Pages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build Vue 3 sample pages for the new control-room UI direction, covering the page shell, dashboard, and login page without wiring live backend APIs.

**Architecture:** Keep the prototype inside `frontend/` as a self-contained Vue 3 app with a single root component and data-driven sample sections. Use static mock content to validate structure, typography, density, and layout before real routing or API integration. The page shell should switch between three prototype views so the user can review the design system in context.

**Tech Stack:** Vue 3, Vite, TypeScript, CSS variables, single-file components

---

### Task 1: Replace the starter app with a prototype shell

**Files:**
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/style.css`
- Remove usage from: `frontend/src/components/HelloWorld.vue`

- [ ] **Step 1: Define the prototype structure**

Create one root view switcher with three modes:

```ts
type ViewKey = 'shell' | 'dashboard' | 'login'
```

The root page must render:

- a compact top review bar to switch sample pages
- a `shell` sample for the global frame
- a `dashboard` sample for the map-centered overview
- a `login` sample for the entry experience

- [ ] **Step 2: Implement the root prototype component**

Build `frontend/src/App.vue` with:

- static sample data arrays for top modules, KPI cards, alerts, tasks, and summary blocks
- computed-free direct rendering since this is a prototype
- semantic sections for `top navigation`, `left workbench`, `map center`, `right situation panels`, and `login split layout`

- [ ] **Step 3: Remove starter-only dependencies from the view**

Delete the `HelloWorld` import from `frontend/src/App.vue` and stop rendering starter content.

- [ ] **Step 4: Keep the prototype focused**

Do not add:

- Vue Router
- Pinia/Vuex
- live API calls
- chart libraries

The prototype should stay static and visual-only.

### Task 2: Implement the design-system-driven global CSS

**Files:**
- Modify: `frontend/src/style.css`

- [ ] **Step 1: Declare layout intent before CSS**

Use this layout intent as the implementation contract:

- overall page shell: `100dvh flex column`
- review bar: fixed-height top strip for page switching
- sample canvas: `flex-1 min-height:0`
- shell page: `header + left summary rail + center map + right stacked panels`
- dashboard page: `single-screen, no page-level vertical scroll`
- login page: `left narrative column + right auth card`

- [ ] **Step 2: Establish the token layer**

Define CSS variables for:

- background, panel, module, overlay, line, text, muted text
- orange and cyan signal colors
- spacing scale
- font stacks for sans, serif accent, and mono data text

- [ ] **Step 3: Build the structural primitives**

Add reusable class patterns for:

- top control bar
- shell header
- summary rail blocks
- map stage
- stacked status panels
- KPI strip
- login split layout

- [ ] **Step 4: Enforce responsive safety**

At smaller widths:

- collapse the dashboard columns into a vertical stack
- keep each section readable without overlap
- allow internal section scrolling only where required, not whole-page overflow by default

### Task 3: Populate the sample content for design review

**Files:**
- Modify: `frontend/src/App.vue`

- [ ] **Step 1: Create shell sample content**

Render:

- top navigation with `总览 / 监测 / 资产 / 任务 / 应急 / 日志`
- two to three global status chips
- left summary blocks in the order already approved
- a center map stage placeholder with geographic overlays
- right-side risk and handling panels

- [ ] **Step 2: Create dashboard sample content**

Render:

- compressed KPI strip
- dominant map center
- right-side alert and task stacks
- no bottom trend area

- [ ] **Step 3: Create login sample content**

Render:

- left mission-led narrative with a restrained editorial accent
- right auth panel with username, password, captcha, and primary action
- short, formal, technical Chinese copy

### Task 4: Verify the prototype build

**Files:**
- Verify: `frontend/package.json`
- Verify output from: `frontend/src/App.vue`, `frontend/src/style.css`

- [ ] **Step 1: Run the build**

Run:

```bash
npm run build
```

in `frontend/`

- [ ] **Step 2: Confirm expected result**

Expected:

- `vue-tsc` passes
- Vite production build succeeds
- no missing import or CSS syntax errors

- [ ] **Step 3: Prepare review notes**

Summarize:

- what the user can review in each sample page
- which parts are deliberately static
- what should be validated before real module migration
