# 管道管理系统设计系统

## 设计声明

```
【设计声明】
- 风格流派: Retro-Future / 复古未来（科技感克制版）+ Utilitarian / 实用主义
- 主色调: 深色主题 #00D4FF（科技蓝） / 浅色主题 #1890FF（科技蓝）
- 字体组合: 
  - 展示字体: 'Orbitron' / 'Rajdhani'（科技感）
  - 正文字体: 'Source Sans Pro' / 'Noto Sans SC'（中文）
  - 等宽字体: 'JetBrains Mono'（代码/数据）
- 圆角策略: 8px（现代科技感）
- 独特设计决策: 
  1. 数据流动动画：实时数据以流动线条形式展示
  2. 深浅双主题无缝切换，科技感贯穿始终
  3. 卡片式布局 + 抽屉式详情面板，告别传统左右布局
  4. 数据可视化优先，图表替代表格
- 使用的组件库: Element UI（深度定制主题）
```

---

## 目录结构

```
design-system/
├── README.md                    # 设计系统总览
├── design-tokens.md             # 设计令牌（颜色、字体、间距）
├── components.md                # 组件设计规范
├── layouts.md                   # 布局设计规范
├── themes.md                    # 主题切换系统
└── animations.md                # 动画与交互规范
```

---

## 核心设计原则

### 1. 数据优先
- 用图表、指标卡、数据流代替传统表格
- 实时数据以动态形式展示
- 关键指标一目了然

### 2. 科技感贯穿
- 深色主题为主，浅色主题为辅
- 流畅的动画过渡
- 数据流动的视觉隐喻

### 3. 用户友好
- 面向外部客户，界面直观易用
- 抽屉式详情，不离开当前页面
- 多层次页面切换，信息层次清晰

### 4. 响应式设计
- 适配不同屏幕尺寸
- 移动端优先考虑

---

## 禁用模式（Forbidden Patterns）

### ❌ 禁止使用

1. **颜色**
   - 纯蓝/纯紫渐变作为主色
   - Element UI 默认蓝色 `#409EFF`
   - 玻璃拟态作为主要设计元素

2. **字体**
   - Inter, Roboto, Arial, system-ui 作为展示字体

3. **布局**
   - 传统左侧导航 + 右侧表格
   - 千篇一律的卡片网格
   - 过度使用 `border-radius: 16px+`

4. **微模式**
   - 所有按钮都是蓝紫色
   - 标题都用渐变文字
   - 装饰性渐变球作为背景

---

## 设计令牌速查

### 深色主题（推荐）

```css
--color-primary: #00D4FF;        /* 科技蓝 */
--color-secondary: #00FF88;      /* 科技绿 */
--color-accent: #FF6B6B;         /* 警示红 */
--color-bg: #0A0E27;             /* 深蓝黑背景 */
--color-surface: #1A1F3A;        /* 卡片背景 */
--color-surface-elevated: #252B4A; /* 提升卡片 */
--color-ink: #E2E8F0;            /* 主文字 */
--color-muted: #94A3B8;          /* 次要文字 */
--color-border: #334155;         /* 边框 */
```

### 浅色主题

```css
--color-primary: #1890FF;        /* 科技蓝 */
--color-secondary: #52C41A;      /* 科技绿 */
--color-accent: #FF4D4F;         /* 警示红 */
--color-bg: #F0F2F5;             /* 浅灰背景 */
--color-surface: #FFFFFF;        /* 卡片背景 */
--color-surface-elevated: #FAFAFA; /* 提升卡片 */
--color-ink: #1E293B;            /* 主文字 */
--color-muted: #64748B;          /* 次要文字 */
--color-border: #E2E8F0;         /* 边框 */
```

### 图表配色

```css
--chart-1: #00D4FF;              /* 科技蓝 */
--chart-2: #00FF88;              /* 科技绿 */
--chart-3: #FFB800;              /* 警示黄 */
--chart-4: #FF6B6B;              /* 警示红 */
--chart-5: #A78BFA;              /* 科技紫 */
```

---

## 下一步

1. 查看 [design-tokens.md](./design-tokens.md) 了解详细的设计令牌定义
2. 查看 [components.md](./components.md) 了解组件设计规范
3. 查看 [layouts.md](./layouts.md) 了解布局设计规范
4. 查看 [themes.md](./themes.md) 了解主题切换实现
5. 查看 [animations.md](./animations.md) 了解动画与交互规范
