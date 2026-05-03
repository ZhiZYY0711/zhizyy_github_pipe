# Virtual Expert Visual Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Polish the sidebar and composer UI of VirtualExpertPage.vue to match the Tactical Brutalism design system, replacing text buttons with icon+text (sidebar) and icon-only (composer), and restructuring the composer into a container-based layout.

**Architecture:** All changes are scoped to a single file — `VirtualExpertPage.vue`. Template gets icon+text toolbar buttons, togglable search, and a restructured composer. CSS is updated to follow the design system: 0px radius, `80ms step-end` transitions, tonal surface layering, ghost borders.

**Tech Stack:** Vue 3 Composition API (`<script setup>`), scoped CSS, inline SVG icons (no external icon library), existing design tokens from `tokens.css`.

---

### Task 1: Add `searchExpanded` state and prepare SVG icon markup

**Files:**
- Modify: `frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue:183-188` (add `searchExpanded` ref after existing sidebar state refs)

- [ ] **Step 1: Add `searchExpanded` ref**

In the `<script setup>` block, after line 188 (`const sessionMenuOpenId = ref<string | null>(null)`), add:

```ts
const searchExpanded = ref(false)
```

- [ ] **Step 2: Verify build still passes**

Run: `cd frontend && pnpm build`
Expected: Build succeeds with no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue
git commit -m "feat(expert-ui): add searchExpanded state for togglable sidebar search"
```

---

### Task 2: Refactor sidebar toolbar to vertical icon+text buttons

**Files:**
- Modify: `frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue:1727-1740` (toolbar template)
- Modify: `frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue:2370-2395` (toolbar CSS)

- [ ] **Step 1: Replace toolbar template**

Replace the entire `<div class="session-toolbar">` block (lines 1727-1740) with:

```html
<div class="session-toolbar">
  <button
    type="button"
    class="toolbar-btn"
    :title="sidebarCollapsed ? '展开列表' : '收起列表'"
    @click="sidebarCollapsed = !sidebarCollapsed"
  >
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
      <rect x="2" y="2" width="12" height="12" rx="0"/>
      <line x1="6" y1="2" x2="6" y2="14"/>
    </svg>
    <span>{{ sidebarCollapsed ? '展开列表' : '收起列表' }}</span>
  </button>
  <button
    type="button"
    class="toolbar-btn"
    title="开启新对话"
    :disabled="isRunning"
    @click="startNewConversation"
  >
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
      <line x1="8" y1="3" x2="8" y2="13"/>
      <line x1="3" y1="8" x2="13" y2="8"/>
    </svg>
    <span>新对话</span>
  </button>
  <button
    type="button"
    class="toolbar-btn"
    :class="{ 'is-active': searchExpanded }"
    title="搜索历史记录"
    @click="searchExpanded = !searchExpanded"
  >
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
      <circle cx="7" cy="7" r="4"/>
      <line x1="10" y1="10" x2="14" y2="14"/>
    </svg>
    <span>搜索</span>
  </button>
  <button
    type="button"
    class="toolbar-btn"
    title="归档聊天"
    @click="archivedOpen = !archivedOpen"
  >
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
      <rect x="2" y="3" width="12" height="3" rx="0"/>
      <path d="M3 6v7a1 1 0 001 1h8a1 1 0 001-1V6"/>
      <line x1="6" y1="9" x2="10" y2="9"/>
    </svg>
    <span>归档</span>
  </button>
  <button
    type="button"
    class="toolbar-btn"
    title="我的偏好"
    @click="memoryPanelOpen = true"
  >
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
      <line x1="4" y1="5" x2="12" y2="5"/>
      <line x1="4" y1="8" x2="12" y2="8"/>
      <line x1="4" y1="11" x2="12" y2="11"/>
      <circle cx="6" cy="5" r="1.5" fill="currentColor" stroke="none"/>
      <circle cx="10" cy="8" r="1.5" fill="currentColor" stroke="none"/>
    </svg>
    <span>偏好</span>
  </button>
</div>
```

- [ ] **Step 2: Replace search section with togglable version**

Replace the `<template v-if="!sidebarCollapsed">` block's search section. Change the search `<div class="session-search">` (lines 1743-1745) to:

```html
<div v-if="searchExpanded" class="session-search">
  <input
    ref="searchInputRef"
    v-model="sessionSearch"
    type="search"
    placeholder="搜索历史记录"
    aria-label="搜索历史记录"
    @keydown.esc="searchExpanded = false"
  >
</div>
```

- [ ] **Step 3: Replace toolbar CSS**

Replace the `.session-toolbar` and `.session-toolbar button` CSS rules (lines 2370-2388) with:

```css
.session-toolbar {
  display: grid;
  gap: 2px;
  margin-bottom: 8px;
}

.toolbar-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  inline-size: 100%;
  block-size: 36px;
  padding: 0 10px;
  border: 1px solid var(--color-line);
  color: var(--color-text-muted);
  background: transparent;
  font-size: var(--text-meta);
  text-align: left;
  transition: background 80ms step-end, border-color 80ms step-end;
}

.toolbar-btn:hover {
  background: var(--color-panel-3);
  border-color: rgba(110, 202, 212, 0.2);
}

.toolbar-btn:active,
.toolbar-btn.is-active {
  background: rgba(110, 202, 212, 0.12);
}

.toolbar-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.toolbar-btn svg {
  flex: 0 0 16px;
  width: 16px;
  height: 16px;
}
```

- [ ] **Step 4: Update search input CSS**

Replace the `.session-search` and `.session-search input` CSS (lines 2397-2409) with:

```css
.session-search {
  margin-bottom: 8px;
}

.session-search input {
  inline-size: 100%;
  min-height: 32px;
  border: 1px solid var(--color-line);
  color: var(--color-text);
  background: var(--color-bg-elevated);
  padding: 0 9px;
  font-size: var(--text-meta);
  transition: border-color 80ms step-end, box-shadow 80ms step-end;
}

.session-search input:focus {
  border-color: var(--color-accent-cyan);
  box-shadow: 0 0 6px rgba(110, 202, 212, 0.25);
  outline: none;
}
```

- [ ] **Step 5: Verify build**

Run: `cd frontend && pnpm build`
Expected: Build succeeds.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue
git commit -m "feat(expert-ui): refactor sidebar toolbar to vertical icon+text buttons with togglable search"
```

---

### Task 3: Polish session cards — 0px radius, 8px gap, tonal hover, active left-bar

**Files:**
- Modify: `frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue:2364-2368` (session-list gap)
- Modify: `frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue:2411-2434` (session-item styles)
- Modify: `frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue:2436-2448` (session-open)
- Modify: `frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue:2450-2461` (session-more)

- [ ] **Step 1: Update session-list gap**

Replace `.session-list` CSS (lines 2364-2368):

```css
.session-list {
  display: grid;
  align-content: start;
  gap: 8px;
}
```

- [ ] **Step 2: Update session-item styles**

Replace `.session-item`, `.session-item:hover`, `.session-item.is-active` CSS (lines 2411-2434):

```css
.session-item {
  position: relative;
  display: flex;
  align-items: center;
  inline-size: 100%;
  border: 1px solid var(--color-line);
  color: var(--color-text);
  background: rgba(4, 9, 14, 0.8);
  transition: background 80ms step-end, border-color 80ms step-end;
}

.session-item:hover {
  background: var(--color-panel-3);
  border-color: rgba(110, 202, 212, 0.2);
}

.session-item.is-active {
  border-left: 2px solid var(--color-accent-orange);
  background: var(--color-panel-2);
  border-color: var(--color-line);
  border-left-color: var(--color-accent-orange);
}
```

- [ ] **Step 3: Update session-more button to hide/show on hover**

Replace `.session-more` CSS (lines 2450-2461):

```css
.session-more {
  position: absolute;
  inset-block-start: 4px;
  inset-inline-end: 4px;
  min-inline-size: 34px;
  block-size: 24px;
  padding: 0 6px;
  border: 1px solid transparent;
  color: var(--color-text-muted);
  background: transparent;
  font-size: var(--text-micro);
  opacity: 0;
  transition: opacity 80ms step-end, background 80ms step-end;
}

.session-item:hover .session-more {
  opacity: 1;
}

.session-more:hover {
  background: var(--color-panel-3);
  border-color: var(--color-line);
}
```

- [ ] **Step 4: Verify build**

Run: `cd frontend && pnpm build`
Expected: Build succeeds.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue
git commit -m "feat(expert-ui): polish session cards with tonal hover and active left-bar indicator"
```

---

### Task 4: Polish session menu and archive area

**Files:**
- Modify: `frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue:2463-2488` (session-menu styles)
- Modify: `frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue:2494-2514` (archive styles)

- [ ] **Step 1: Update session-menu styles**

Replace `.session-menu` and `.session-menu button` CSS (lines 2463-2488):

```css
.session-menu {
  position: absolute;
  inset-block-start: 30px;
  inset-inline-end: 4px;
  z-index: 10;
  display: grid;
  gap: 8px;
  min-inline-size: 116px;
  padding: 6px;
  border: 1px solid var(--color-line);
  background: var(--color-panel-3);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
}

.session-menu button {
  min-height: 28px;
  padding: 0 8px;
  border: 0;
  color: var(--color-text-muted);
  background: transparent;
  text-align: left;
  font-size: var(--text-meta);
  transition: background 80ms step-end, color 80ms step-end;
}

.session-menu button:hover {
  color: var(--color-text);
  background: var(--color-panel-2);
}
```

- [ ] **Step 2: Update archive section styles**

Replace `.archive-section`, `.archive-toggle`, `.archive-list` CSS (lines 2494-2514):

```css
.archive-section {
  display: grid;
  gap: 8px;
  margin-top: 8px;
}

.archive-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 36px;
  padding: 0 10px;
  border: 1px solid var(--color-line);
  color: var(--color-text-muted);
  background: transparent;
  font-size: var(--text-meta);
  transition: background 80ms step-end, border-color 80ms step-end;
}

.archive-toggle:hover {
  background: var(--color-panel-3);
  border-color: rgba(110, 202, 212, 0.2);
}

.archive-list {
  display: grid;
  gap: 8px;
}
```

- [ ] **Step 3: Verify build**

Run: `cd frontend && pnpm build`
Expected: Build succeeds.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue
git commit -m "feat(expert-ui): polish session menu and archive area to match design system"
```

---

### Task 5: Restructure composer — container wrapper and full-width textarea

**Files:**
- Modify: `frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue:2204-2279` (composer template)
- Modify: `frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue:3165-3328` (composer CSS)

- [ ] **Step 1: Replace composer template**

Replace the entire `<div class="composer">` block (lines 2204-2279) with:

```html
<div class="composer-shell">
  <div class="composer">
    <div v-if="imageAttachments.length" class="attachment-list" aria-label="图片附件">
      <article v-for="attachment in imageAttachments" :key="attachment.id" class="attachment-chip">
        <img :src="attachment.previewUrl" :alt="attachment.name">
        <span>{{ attachment.name }}</span>
        <button type="button" aria-label="移除图片" @click="removeImageAttachment(attachment)">移除</button>
      </article>
    </div>
    <textarea
      v-model="input"
      aria-label="输入异常描述"
      :disabled="isRunning || isExporting"
      placeholder="输入异常现象、管段、设备或风险诉求，Enter 发送，Shift+Enter 换行"
      @keydown="handleComposerKeydown"
    />
    <div class="composer__tools">
      <button
        type="button"
        class="tool-icon-btn"
        :disabled="isRunning || isExporting || imageAttachments.length >= 4"
        title="上传图片"
        aria-label="上传图片"
        @click="triggerImageUpload"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <rect x="2" y="3" width="12" height="10" rx="0"/>
          <circle cx="5.5" cy="6.5" r="1.5"/>
          <path d="M14 11l-3-3-4 4-2-2-3 3"/>
        </svg>
      </button>
      <input
        ref="fileInput"
        class="composer-file-input"
        type="file"
        accept="image/*"
        multiple
        @change="handleImageUpload"
      >
      <button
        type="button"
        class="tool-icon-btn"
        :class="{ 'is-live': isListening }"
        :disabled="isRunning || isExporting || !speechSupported"
        :title="isListening ? '停止语音' : '语音输入'"
        :aria-label="isListening ? '停止语音' : '语音输入'"
        @click="toggleVoiceInput"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <rect x="5.5" y="2" width="5" height="8" rx="0"/>
          <path d="M3 7v1a5 5 0 0010 0V7"/>
          <line x1="8" y1="12" x2="8" y2="14"/>
        </svg>
      </button>
      <label class="model-select" :title="isRunning ? '将在下一次提问生效' : '选择本轮模型'">
        <span>{{ isRunning && activeRunModelLabel ? activeRunModelLabel : selectedModelLabel }}</span>
        <select v-model="selectedModelTier" aria-label="选择模型挡位">
          <option v-for="option in modelTierOptions" :key="option.tier" :value="option.tier">
            {{ option.label }} · {{ option.model }}
          </option>
        </select>
      </label>
      <div class="composer-spacer"></div>
      <button
        v-if="isRunning"
        type="button"
        class="tool-icon-btn is-stop"
        :disabled="!activeRunId"
        title="停止运行"
        aria-label="停止运行"
        @click="cancelRun"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" stroke="none">
          <rect x="3" y="3" width="10" height="10"/>
        </svg>
      </button>
      <button
        v-else
        type="button"
        class="tool-icon-btn is-send"
        :disabled="isRunning"
        title="发送"
        aria-label="发送"
        @click="submitAnalysis"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <line x1="8" y1="14" x2="8" y2="3"/>
          <polyline points="4 7 8 3 12 7"/>
        </svg>
      </button>
    </div>
    <div v-if="quickExportFormats.length" class="composer__exports">
      <button
        v-for="item in quickExportFormats"
        :key="item.format"
        class="tool-icon-btn is-export"
        :disabled="isRunning || isExporting || !selectedSessionId || selectedSessionId.startsWith('ana_demo')"
        :title="item.label"
        :aria-label="item.label"
        @click="exportSelectedSession(item.format)"
      >
        {{ exportFormatLabel(item.format) }}
      </button>
    </div>
  </div>
</div>
```

- [ ] **Step 2: Replace composer CSS**

Replace the entire composer CSS block (lines 3165-3328) with:

```css
.composer-shell {
  padding-top: var(--space-2);
  border-top: 1px solid var(--color-line);
}

.composer {
  display: grid;
  gap: 8px;
  padding: 12px;
  background: var(--color-panel-2);
  border: 1px solid var(--color-line);
}

.attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.attachment-chip {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 26px;
  align-items: center;
  gap: 8px;
  max-width: min(280px, 100%);
  min-height: 42px;
  padding: 4px 6px 4px 4px;
  border: 1px solid var(--color-line);
  background: var(--color-bg-elevated);
}

.attachment-chip img {
  width: 34px;
  height: 34px;
  object-fit: cover;
  border: 1px solid var(--color-line);
}

.attachment-chip span {
  min-width: 0;
  overflow: hidden;
  color: var(--color-text-muted);
  font-size: var(--text-micro);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-chip button {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border: 1px solid rgba(231, 78, 45, 0.24);
  color: var(--color-danger);
  background: rgba(231, 78, 45, 0.08);
}

.composer textarea {
  min-height: 78px;
  max-height: 128px;
  resize: vertical;
  padding: 12px;
  border: 1px solid var(--color-line);
  color: var(--color-text);
  background: var(--color-bg-elevated);
  transition: border-color 80ms step-end, box-shadow 80ms step-end;
}

.composer textarea:focus {
  border-color: var(--color-accent-cyan);
  box-shadow: 0 0 6px rgba(110, 202, 212, 0.25);
  outline: none;
}

.composer textarea::placeholder {
  color: var(--color-text-dim);
}

.composer-file-input {
  display: none;
}

.composer__tools {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.composer-spacer {
  flex: 1;
}

.tool-icon-btn {
  display: inline-grid;
  place-items: center;
  min-width: 32px;
  height: 32px;
  padding: 0 8px;
  border: 1px solid var(--color-line);
  color: var(--color-text-muted);
  background: transparent;
  font-size: var(--text-micro);
  transition: background 80ms step-end, border-color 80ms step-end, color 80ms step-end;
}

.tool-icon-btn:hover {
  background: var(--color-panel-3);
  border-color: rgba(110, 202, 212, 0.2);
}

.tool-icon-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.tool-icon-btn svg {
  width: 16px;
  height: 16px;
}

.tool-icon-btn.is-send {
  background: var(--color-accent-orange);
  border-color: var(--color-accent-orange);
  color: var(--color-text);
}

.tool-icon-btn.is-send:hover {
  box-shadow: 0 0 8px rgba(231, 104, 45, 0.4);
}

.tool-icon-btn.is-stop {
  border-color: rgba(221, 176, 84, 0.34);
  color: var(--color-warning);
  background: rgba(221, 176, 84, 0.08);
  animation: pulse-stop 1.2s ease-in-out infinite;
}

@keyframes pulse-stop {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.8; }
}

.tool-icon-btn.is-live {
  border-color: rgba(34, 197, 94, 0.34);
  color: var(--color-text);
  background: rgba(34, 197, 94, 0.1);
}

.tool-icon-btn.is-export {
  border-color: rgba(231, 104, 45, 0.32);
  color: var(--color-text);
  background: rgba(231, 104, 45, 0.08);
}

.model-select {
  position: relative;
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  max-width: 160px;
  border: 1px solid var(--color-line);
  color: var(--color-text-muted);
  background: var(--color-bg-elevated);
  padding: 0 10px;
  font-size: var(--text-micro);
}

.model-select span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-select select {
  position: absolute;
  inset: 0;
  inline-size: 100%;
  opacity: 0;
  cursor: pointer;
}

.composer__exports {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
```

- [ ] **Step 3: Update responsive breakpoint for new composer**

Replace the `@media (max-width: 819px)` `.composer` rule (line 3475-3477) with:

```css
  .composer__tools {
    flex-wrap: wrap;
  }
```

- [ ] **Step 4: Verify build**

Run: `cd frontend && pnpm build`
Expected: Build succeeds.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue
git commit -m "feat(expert-ui): restructure composer with container shell and icon-only toolbar"
```

---

### Task 6: Update E2E test for new UI structure

**Files:**
- Modify: `frontend/tests/virtual-expert-ui.spec.ts`

- [ ] **Step 1: Update test to match new button structure**

Replace the test file contents with:

```ts
import { expect, test } from '@playwright/test'

test('virtual expert sidebar and composer use text controls', async ({ page }) => {
  await page.route('**/api/manager/virtual-expert/agent/sessions?**', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        sessions: [
          {
            id: 'ana_test_001',
            title: '压力波动复核',
            status: 'completed',
            pinned: true,
            updatedAt: '2026-05-03T10:00:00+08:00',
          },
        ],
      }),
    })
  })
  await page.route('**/api/manager/virtual-expert/agent/memories?**', async (route) => {
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify({ items: [] }) })
  })
  await page.route('**/api/manager/virtual-expert/agent/sessions/ana_test_001/timeline?**', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        sessionId: 'ana_test_001',
        items: [],
        hasMoreBefore: false,
      }),
    })
  })

  await page.addInitScript(() => {
    window.localStorage.setItem('token', 'dev-token')
    window.localStorage.setItem('jwt', 'dev-token')
    window.localStorage.setItem('username', 'tester')
    window.localStorage.setItem('isLoggedIn', 'true')
  })

  await page.goto(`${process.env.PW_BASE_URL || 'http://127.0.0.1:5173'}/virtual-expert`)

  // Sidebar toolbar buttons (icon+text)
  await expect(page.getByRole('button', { name: /收起列表|展开列表/ })).toBeVisible()
  await expect(page.getByRole('button', { name: '新对话' })).toBeVisible()
  await expect(page.getByRole('button', { name: '搜索' })).toBeVisible()
  await expect(page.getByRole('button', { name: '归档' })).toBeVisible()
  await expect(page.getByRole('button', { name: '偏好' })).toBeVisible()

  // Session card
  await expect(page.getByText('置顶')).toBeVisible()
  await expect(page.getByRole('button', { name: /打开会话菜单/ })).toHaveText('更多')

  // Composer — icon buttons with aria-labels
  await expect(page.getByLabel('选择模型挡位')).toBeAttached()
  await expect(page.getByLabel('语音输入')).toBeAttached()
  await expect(page.getByLabel('上传图片')).toBeAttached()
  await expect(page.getByLabel('发送')).toBeAttached()
})
```

- [ ] **Step 2: Run E2E test**

Run: `cd frontend && npx playwright test tests/virtual-expert-ui.spec.ts`
Expected: Test passes.

- [ ] **Step 3: Verify full build**

Run: `cd frontend && pnpm build`
Expected: Build succeeds.

- [ ] **Step 4: Commit**

```bash
git add frontend/tests/virtual-expert-ui.spec.ts
git commit -m "test(expert-ui): update E2E test for icon-based toolbar and composer"
```

---

### Task 7: Final visual verification and cleanup

**Files:**
- Modify: `frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue` (if any fixes needed)

- [ ] **Step 1: Start dev server and visually verify sidebar**

Run: `cd frontend && pnpm dev`

Open `/virtual-expert` in browser. Verify:
- Sidebar toolbar: 5 buttons in a vertical column, each with icon + text
- All buttons have 0px radius
- Hover: background shifts to `--color-panel-3` (step-end, no ease)
- Search: click "搜索" toggles inline search input below toolbar
- Search input focus: cyan border + subtle glow

- [ ] **Step 2: Verify session cards**

- Cards have 8px gap between them
- 0px radius on all cards
- Hover: background changes tonally (no translateY)
- Active card: left 2px orange bar
- "更多" button hidden by default, appears on hover

- [ ] **Step 3: Verify composer**

- Composer wrapped in `--color-panel-2` container
- Textarea recessed with `--color-bg-elevated` background
- Textarea focus: cyan border + glow
- Bottom toolbar: icon buttons (image, voice, model select, send/stop)
- Send button: orange solid
- Stop button: yellow with pulse animation (only visible when running)
- Export buttons: separate row below toolbar

- [ ] **Step 4: Verify responsive breakpoints**

Resize to 1180px and 819px. Verify layout adapts correctly.

- [ ] **Step 5: Final commit if any fixes needed**

```bash
git add frontend/src/modules/virtual-expert/views/VirtualExpertPage.vue
git commit -m "fix(expert-ui): visual polish fixes from manual verification"
```
