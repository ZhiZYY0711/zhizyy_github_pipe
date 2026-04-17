# 设计令牌（Design Tokens）

设计令牌是设计系统的原子单位，用于定义颜色、字体、间距、阴影等基础样式。

---

## 1. 颜色系统（Color System）

### 1.1 深色主题（Dark Theme）

```css
:root[data-theme="dark"] {
  /* === 主色调 === */
  --color-primary: #00D4FF;
  --color-primary-hover: #33DDFF;
  --color-primary-active: #00BBDD;
  --color-primary-light: rgba(0, 212, 255, 0.1);
  
  /* === 辅助色 === */
  --color-secondary: #00FF88;
  --color-secondary-hover: #33FF99;
  --color-secondary-active: #00DD77;
  
  /* === 功能色 === */
  --color-success: #00FF88;
  --color-warning: #FFB800;
  --color-danger: #FF6B6B;
  --color-info: #00D4FF;
  
  /* === 背景色 === */
  --color-bg-page: #0A0E27;
  --color-bg-surface: #1A1F3A;
  --color-bg-elevated: #252B4A;
  --color-bg-overlay: rgba(10, 14, 39, 0.8);
  
  /* === 文字色 === */
  --color-text-primary: #E2E8F0;
  --color-text-secondary: #94A3B8;
  --color-text-disabled: #64748B;
  --color-text-inverse: #0A0E27;
  
  /* === 边框色 === */
  --color-border: #334155;
  --color-border-light: #1E293B;
  --color-border-focus: #00D4FF;
  
  /* === 图表配色 === */
  --chart-1: #00D4FF;
  --chart-2: #00FF88;
  --chart-3: #FFB800;
  --chart-4: #FF6B6B;
  --chart-5: #A78BFA;
  --chart-6: #F472B6;
  --chart-7: #34D399;
  --chart-8: #FBBF24;
}
```

### 1.2 浅色主题（Light Theme）

```css
:root[data-theme="light"] {
  /* === 主色调 === */
  --color-primary: #1890FF;
  --color-primary-hover: #40A9FF;
  --color-primary-active: #096DD9;
  --color-primary-light: rgba(24, 144, 255, 0.1);
  
  /* === 辅助色 === */
  --color-secondary: #52C41A;
  --color-secondary-hover: #73D13D;
  --color-secondary-active: #389E0D;
  
  /* === 功能色 === */
  --color-success: #52C41A;
  --color-warning: #FAAD14;
  --color-danger: #FF4D4F;
  --color-info: #1890FF;
  
  /* === 背景色 === */
  --color-bg-page: #F0F2F5;
  --color-bg-surface: #FFFFFF;
  --color-bg-elevated: #FAFAFA;
  --color-bg-overlay: rgba(0, 0, 0, 0.45);
  
  /* === 文字色 === */
  --color-text-primary: #1E293B;
  --color-text-secondary: #64748B;
  --color-text-disabled: #94A3B8;
  --color-text-inverse: #FFFFFF;
  
  /* === 边框色 === */
  --color-border: #E2E8F0;
  --color-border-light: #F1F5F9;
  --color-border-focus: #1890FF;
  
  /* === 图表配色 === */
  --chart-1: #1890FF;
  --chart-2: #52C41A;
  --chart-3: #FAAD14;
  --chart-4: #FF4D4F;
  --chart-5: #722ED1;
  --chart-6: #EB2F96;
  --chart-7: #13C2C2;
  --chart-8: #FA8C16;
}
```

### 1.3 颜色使用规范

| 用途 | 深色主题 | 浅色主题 | 说明 |
|------|---------|---------|------|
| 主按钮背景 | `--color-primary` | `--color-primary` | 主要操作按钮 |
| 次按钮背景 | `--color-surface` | `--color-surface` | 次要操作按钮 |
| 成功状态 | `--color-success` | `--color-success` | 成功提示、完成状态 |
| 警告状态 | `--color-warning` | `--color-warning` | 警告提示、注意状态 |
| 危险状态 | `--color-danger` | `color-danger` | 错误提示、删除操作 |
| 链接文字 | `--color-primary` | `--color-primary` | 可点击链接 |
| 禁用状态 | `--color-text-disabled` | `--color-text-disabled` | 禁用元素 |

---

## 2. 字体系统（Typography）

### 2.1 字体家族

```css
:root {
  /* === 展示字体（标题、品牌） === */
  --font-display: 'Orbitron', 'Rajdhani', 'Noto Sans SC', sans-serif;
  
  /* === 正文字体 === */
  --font-body: 'Source Sans Pro', 'Noto Sans SC', -apple-system, sans-serif;
  
  /* === 等宽字体（代码、数据） === */
  --font-mono: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
}
```

### 2.2 字体大小

```css
:root {
  /* === 标题 === */
  --font-size-h1: 48px;      /* 大屏标题 */
  --font-size-h2: 36px;      /* 页面标题 */
  --font-size-h3: 28px;      /* 区块标题 */
  --font-size-h4: 22px;      /* 卡片标题 */
  --font-size-h5: 18px;      /* 小标题 */
  
  /* === 正文 === */
  --font-size-lg: 16px;      /* 大正文 */
  --font-size-base: 14px;    /* 基础正文 */
  --font-size-sm: 13px;      /* 小正文 */
  --font-size-xs: 12px;      /* 辅助文字 */
  
  /* === 数据展示 === */
  --font-size-data-lg: 42px; /* 大数据 */
  --font-size-data: 32px;    /* 数据展示 */
  --font-size-data-sm: 24px; /* 小数据 */
}
```

### 2.3 字重

```css
:root {
  --font-weight-light: 300;
  --font-weight-normal: 400;
  --font-weight-medium: 500;
  --font-weight-semibold: 600;
  --font-weight-bold: 700;
}
```

### 2.4 行高

```css
:root {
  --line-height-tight: 1.25;
  --line-height-base: 1.5;
  --line-height-relaxed: 1.75;
}
```

---

## 3. 间距系统（Spacing）

### 3.1 基础间距

```css
:root {
  --spacing-0: 0;
  --spacing-1: 4px;
  --spacing-2: 8px;
  --spacing-3: 12px;
  --spacing-4: 16px;
  --spacing-5: 20px;
  --spacing-6: 24px;
  --spacing-8: 32px;
  --spacing-10: 40px;
  --spacing-12: 48px;
  --spacing-16: 64px;
  --spacing-20: 80px;
}
```

### 3.2 使用规范

| 间距值 | 用途 |
|--------|------|
| `--spacing-1` (4px) | 图标与文字间距、紧凑元素间距 |
| `--spacing-2` (8px) | 组件内部间距、小元素间距 |
| `--spacing-4` (16px) | 卡片内边距、组件间距 |
| `--spacing-6` (24px) | 区块间距、段落间距 |
| `--spacing-8` (32px) | 大区块间距 |
| `--spacing-12` (48px) | 页面区块间距 |

---

## 4. 圆角系统（Border Radius）

```css
:root {
  --radius-none: 0;
  --radius-sm: 4px;          /* 小按钮、标签 */
  --radius-base: 8px;        /* 卡片、输入框 */
  --radius-lg: 12px;         /* 大卡片、模态框 */
  --radius-xl: 16px;         /* 特殊卡片 */
  --radius-full: 9999px;     /* 圆形、胶囊 */
}
```

---

## 5. 阴影系统（Shadows）

### 5.1 深色主题阴影

```css
:root[data-theme="dark"] {
  --shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.3);
  --shadow-base: 0 4px 16px rgba(0, 0, 0, 0.4);
  --shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.5);
  --shadow-xl: 0 16px 48px rgba(0, 0, 0, 0.6);
  
  /* 发光效果（科技感） */
  --glow-primary: 0 0 20px rgba(0, 212, 255, 0.3);
  --glow-success: 0 0 20px rgba(0, 255, 136, 0.3);
  --glow-danger: 0 0 20px rgba(255, 107, 107, 0.3);
}
```

### 5.2 浅色主题阴影

```css
:root[data-theme="light"] {
  --shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.08);
  --shadow-base: 0 4px 16px rgba(0, 0, 0, 0.12);
  --shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.16);
  --shadow-xl: 0 16px 48px rgba(0, 0, 0, 0.2);
  
  /* 发光效果 */
  --glow-primary: 0 0 20px rgba(24, 144, 255, 0.2);
  --glow-success: 0 0 20px rgba(82, 196, 26, 0.2);
  --glow-danger: 0 0 20px rgba(255, 77, 79, 0.2);
}
```

---

## 6. 过渡动画（Transitions）

```css
:root {
  /* === 时长 === */
  --duration-fast: 150ms;
  --duration-base: 250ms;
  --duration-slow: 350ms;
  --duration-slower: 500ms;
  
  /* === 缓动函数 === */
  --ease-in: cubic-bezier(0.4, 0, 1, 1);
  --ease-out: cubic-bezier(0, 0, 0.2, 1);
  --ease-in-out: cubic-bezier(0.4, 0, 0.2, 1);
  --ease-bounce: cubic-bezier(0.68, -0.55, 0.265, 1.55);
  
  /* === 常用过渡 === */
  --transition-colors: color var(--duration-base) var(--ease-in-out),
                       background-color var(--duration-base) var(--ease-in-out),
                       border-color var(--duration-base) var(--ease-in-out);
  
  --transition-transform: transform var(--duration-base) var(--ease-out);
  
  --transition-opacity: opacity var(--duration-base) var(--ease-in-out);
  
  --transition-all: all var(--duration-base) var(--ease-in-out);
}
```

---

## 7. Z-Index 层级

```css
:root {
  --z-index-dropdown: 1000;
  --z-index-sticky: 1020;
  --z-index-fixed: 1030;
  --z-index-modal-backdrop: 1040;
  --z-index-modal: 1050;
  --z-index-popover: 1060;
  --z-index-tooltip: 1070;
  --z-index-drawer: 1080;
}
```

---

## 8. 断点系统（Breakpoints）

```css
:root {
  --breakpoint-xs: 480px;    /* 手机 */
  --breakpoint-sm: 576px;    /* 大手机 */
  --breakpoint-md: 768px;    /* 平板 */
  --breakpoint-lg: 992px;    /* 小屏电脑 */
  --breakpoint-xl: 1200px;   /* 电脑 */
  --breakpoint-2xl: 1600px;  /* 大屏 */
}
```

---

## 9. CSS 变量使用示例

```css
/* 在组件中使用设计令牌 */
.card {
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-base);
  padding: var(--spacing-4);
  box-shadow: var(--shadow-base);
  transition: var(--transition-all);
}

.card:hover {
  box-shadow: var(--shadow-lg);
  border-color: var(--color-primary);
}

.card__title {
  font-family: var(--font-display);
  font-size: var(--font-size-h4);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-3);
}

.card__value {
  font-family: var(--font-mono);
  font-size: var(--font-size-data);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
}
```

---

## 10. JavaScript 中使用设计令牌

```javascript
// 在 JS 中获取 CSS 变量
const primaryColor = getComputedStyle(document.documentElement)
  .getPropertyValue('--color-primary');

// 动态设置 CSS 变量
document.documentElement.style.setProperty('--color-primary', '#00D4FF');

// 主题切换
function toggleTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
  localStorage.setItem('theme', theme);
}
```
