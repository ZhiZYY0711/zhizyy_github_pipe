# 动画与交互规范

本文档定义了管道管理系统的动画与交互规范，打造流畅、科技感的用户体验。

---

## 1. 动画原则

### 1.1 核心原则

1. **目的性** - 每个动画都有明确的目的（引导、反馈、过渡）
2. **流畅性** - 动画流畅自然，不卡顿
3. **克制性** - 不滥用动画，避免干扰用户
4. **一致性** - 相同交互使用相同的动画效果

### 1.2 动画分类

| 类型 | 时长 | 缓动函数 | 使用场景 |
|------|------|----------|----------|
| 快速 | 150ms | ease-out | 按钮点击、开关切换 |
| 基础 | 250ms | ease-in-out | 颜色变化、展开收起 |
| 慢速 | 350ms | ease-out | 抽屉滑出、模态框 |
| 更慢 | 500ms | ease-in-out | 页面切换、大型动画 |

---

## 2. 过渡动画

### 2.1 淡入淡出（Fade）

```css
/* 基础淡入淡出 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--duration-base) var(--ease-in-out);
}

.fade-enter,
.fade-leave-to {
  opacity: 0;
}

/* 快速淡入淡出 */
.fade-fast-enter-active,
.fade-fast-leave-active {
  transition: opacity var(--duration-fast) var(--ease-in-out);
}

.fade-fast-enter,
.fade-fast-leave-to {
  opacity: 0;
}

/* 慢速淡入淡出 */
.fade-slow-enter-active,
.fade-slow-leave-active {
  transition: opacity var(--duration-slow) var(--ease-in-out);
}

.fade-slow-enter,
.fade-slow-leave-to {
  opacity: 0;
}
```

### 2.2 滑动（Slide）

```css
/* 从右侧滑入 */
.slide-right-enter-active,
.slide-right-leave-active {
  transition: transform var(--duration-slow) var(--ease-out);
}

.slide-right-enter,
.slide-right-leave-to {
  transform: translateX(100%);
}

/* 从左侧滑入 */
.slide-left-enter-active,
.slide-left-leave-active {
  transition: transform var(--duration-slow) var(--ease-out);
}

.slide-left-enter,
.slide-left-leave-to {
  transform: translateX(-100%);
}

/* 从底部滑入 */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform var(--duration-slow) var(--ease-out);
}

.slide-up-enter,
.slide-up-leave-to {
  transform: translateY(100%);
}

/* 从顶部滑入 */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: transform var(--duration-slow) var(--ease-out);
}

.slide-down-enter,
.slide-down-leave-to {
  transform: translateY(-100%);
}
```

### 2.3 缩放（Scale）

```css
/* 缩放淡入 */
.scale-enter-active,
.scale-leave-active {
  transition: all var(--duration-base) var(--ease-out);
}

.scale-enter,
.scale-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

/* 弹性缩放 */
.scale-bounce-enter-active {
  animation: scale-bounce-in var(--duration-slow);
}

.scale-bounce-leave-active {
  animation: scale-bounce-out var(--duration-base);
}

@keyframes scale-bounce-in {
  0% {
    opacity: 0;
    transform: scale(0.3);
  }
  50% {
    transform: scale(1.05);
  }
  70% {
    transform: scale(0.9);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes scale-bounce-out {
  0% {
    opacity: 1;
    transform: scale(1);
  }
  100% {
    opacity: 0;
    transform: scale(0.3);
  }
}
```

### 2.4 组合动画

```css
/* 淡入 + 滑动 */
.fade-slide-right-enter-active,
.fade-slide-right-leave-active {
  transition: all var(--duration-slow) var(--ease-out);
}

.fade-slide-right-enter,
.fade-slide-right-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

/* 淡入 + 缩放 */
.fade-scale-enter-active,
.fade-scale-leave-active {
  transition: all var(--duration-base) var(--ease-out);
}

.fade-scale-enter,
.fade-scale-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
```

---

## 3. 交互动画

### 3.1 悬停效果（Hover）

```css
/* 卡片悬停 */
.card {
  transition: var(--transition-all);
}

.card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-primary);
}

/* 按钮悬停 */
.btn {
  transition: var(--transition-all);
}

.btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--glow-primary);
}

.btn:active {
  transform: translateY(0);
}

/* 图标悬停 */
.icon-button {
  transition: var(--transition-all);
}

.icon-button:hover {
  transform: scale(1.1);
  color: var(--color-primary);
}

.icon-button:active {
  transform: scale(0.95);
}
```

### 3.2 点击反馈

```css
/* 涟漪效果 */
.ripple {
  position: relative;
  overflow: hidden;
}

.ripple::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  background-color: rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  transition: width 0.6s, height 0.6s;
}

.ripple:active::after {
  width: 200%;
  height: 200%;
}

/* 脉冲效果 */
.pulse {
  animation: pulse-animation 1s ease-out;
}

@keyframes pulse-animation {
  0% {
    box-shadow: 0 0 0 0 var(--color-primary);
  }
  100% {
    box-shadow: 0 0 0 20px transparent;
  }
}
```

### 3.3 加载动画

```css
/* 旋转加载 */
.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 脉冲加载 */
.pulse-loader {
  width: 40px;
  height: 40px;
  background-color: var(--color-primary);
  border-radius: 50%;
  animation: pulse-loader 1.5s ease-in-out infinite;
}

@keyframes pulse-loader {
  0%, 100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  50% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 骨架屏加载 */
.skeleton {
  background: linear-gradient(
    90deg,
    var(--color-bg-elevated) 25%,
    var(--color-bg-surface) 50%,
    var(--color-bg-elevated) 75%
  );
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s ease-in-out infinite;
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
```

---

## 4. 数据流动动画

### 4.1 数据流线条

```css
/* 流动线条 */
.data-flow {
  position: relative;
  overflow: hidden;
}

.data-flow::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 2px;
  background: linear-gradient(
    90deg,
    transparent,
    var(--color-primary),
    transparent
  );
  animation: data-flow 2s linear infinite;
}

@keyframes data-flow {
  0% {
    left: -100%;
  }
  100% {
    left: 100%;
  }
}
```

### 4.2 数字滚动

```vue
<template>
  <span class="number-scroll">{{ displayValue }}</span>
</template>

<script>
export default {
  name: 'NumberScroll',
  props: {
    value: {
      type: Number,
      required: true
    },
    duration: {
      type: Number,
      default: 1000
    }
  },
  data() {
    return {
      displayValue: 0
    };
  },
  watch: {
    value(newVal, oldVal) {
      this.animateNumber(oldVal, newVal);
    }
  },
  mounted() {
    this.displayValue = this.value;
  },
  methods: {
    animateNumber(from, to) {
      const startTime = Date.now();
      const diff = to - from;
      
      const animate = () => {
        const elapsed = Date.now() - startTime;
        const progress = Math.min(elapsed / this.duration, 1);
        
        // 使用缓动函数
        const easeProgress = 1 - Math.pow(1 - progress, 3);
        
        this.displayValue = Math.round(from + diff * easeProgress);
        
        if (progress < 1) {
          requestAnimationFrame(animate);
        }
      };
      
      requestAnimationFrame(animate);
    }
  }
};
</script>

<style scoped>
.number-scroll {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}
</style>
```

### 4.3 实时数据更新

```css
/* 数据更新闪烁 */
.data-update {
  animation: data-flash 0.5s ease-out;
}

@keyframes data-flash {
  0% {
    background-color: var(--color-primary-light);
  }
  100% {
    background-color: transparent;
  }
}

/* 数据增长动画 */
.data-grow {
  animation: data-grow 0.3s ease-out;
}

@keyframes data-grow {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
  }
}
```

---

## 5. 页面切换动画

### 5.1 路由切换

```vue
<template>
  <transition :name="transitionName" mode="out-in">
    <router-view :key="$route.path" />
  </transition>
</template>

<script>
export default {
  data() {
    return {
      transitionName: 'page-fade'
    };
  },
  watch: {
    $route(to, from) {
      // 根据路由深度决定动画方向
      const toDepth = to.path.split('/').length;
      const fromDepth = from.path.split('/').length;
      
      if (toDepth > fromDepth) {
        this.transitionName = 'page-slide-left';
      } else if (toDepth < fromDepth) {
        this.transitionName = 'page-slide-right';
      } else {
        this.transitionName = 'page-fade';
      }
    }
  }
};
</script>

<style scoped>
/* 页面淡入淡出 */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity var(--duration-base) var(--ease-in-out);
}

.page-fade-enter,
.page-fade-leave-to {
  opacity: 0;
}

/* 页面向左滑入 */
.page-slide-left-enter-active,
.page-slide-left-leave-active {
  transition: all var(--duration-slow) var(--ease-out);
  position: absolute;
  width: 100%;
}

.page-slide-left-enter {
  opacity: 0;
  transform: translateX(30px);
}

.page-slide-left-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}

/* 页面向右滑入 */
.page-slide-right-enter-active,
.page-slide-right-leave-active {
  transition: all var(--duration-slow) var(--ease-out);
  position: absolute;
  width: 100%;
}

.page-slide-right-enter {
  opacity: 0;
  transform: translateX(-30px);
}

.page-slide-right-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>
```

---

## 6. 图表动画

### 6.1 ECharts 动画配置

```javascript
// 图表通用动画配置
const chartAnimation = {
  animation: true,
  animationDuration: 1000,
  animationEasing: 'cubicOut',
  animationDelay: (idx) => idx * 100,
  
  // 数据更新动画
  animationDurationUpdate: 500,
  animationEasingUpdate: 'cubicOut'
};

// 折线图动画
const lineChartOption = {
  ...chartAnimation,
  series: [{
    type: 'line',
    animationDuration: 2000,
    animationEasing: 'linear',
    animationDelay: (idx) => idx * 10
  }]
};

// 柱状图动画
const barChartOption = {
  ...chartAnimation,
  series: [{
    type: 'bar',
    animationDuration: 1000,
    animationEasing: 'elasticOut',
    animationDelay: (idx) => idx * 100
  }]
};

// 饼图动画
const pieChartOption = {
  ...chartAnimation,
  series: [{
    type: 'pie',
    animationType: 'expansion',
    animationDuration: 1000,
    animationEasing: 'cubicOut'
  }]
};
```

---

## 7. 微交互

### 7.1 输入框聚焦

```css
.input-wrapper {
  position: relative;
}

.input-wrapper::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  width: 0;
  height: 2px;
  background-color: var(--color-primary);
  transition: all var(--duration-base) var(--ease-out);
}

.input-wrapper:focus-within::after {
  left: 0;
  width: 100%;
}
```

### 7.2 开关切换

```css
.switch {
  position: relative;
  width: 44px;
  height: 24px;
  background-color: var(--color-border);
  border-radius: 12px;
  cursor: pointer;
  transition: var(--transition-all);
}

.switch::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  background-color: white;
  border-radius: 50%;
  transition: var(--transition-all);
  box-shadow: var(--shadow-sm);
}

.switch.active {
  background-color: var(--color-primary);
}

.switch.active::after {
  left: 22px;
}
```

### 7.3 复选框

```css
.checkbox {
  position: relative;
  width: 18px;
  height: 18px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition-all);
}

.checkbox.checked {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
}

.checkbox.checked::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 5px;
  width: 5px;
  height: 9px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
  animation: checkbox-check 0.2s ease-out;
}

@keyframes checkbox-check {
  0% {
    opacity: 0;
    transform: rotate(45deg) scale(0);
  }
  100% {
    opacity: 1;
    transform: rotate(45deg) scale(1);
  }
}
```

---

## 8. 性能优化

### 8.1 使用 will-change

```css
/* 预告浏览器即将发生的变化 */
.will-animate {
  will-change: transform, opacity;
}

/* 动画结束后移除 will-change */
.animation-complete {
  will-change: auto;
}
```

### 8.2 使用 transform 和 opacity

```css
/* 推荐：使用 transform 和 opacity（GPU 加速） */
.good-animation {
  transition: transform 0.3s, opacity 0.3s;
}

.good-animation:hover {
  transform: translateY(-4px);
  opacity: 0.8;
}

/* 不推荐：使用 left、top、width、height（触发重排） */
.bad-animation {
  transition: left 0.3s, top 0.3s;
}

.bad-animation:hover {
  left: 10px;
  top: 10px;
}
```

### 8.3 使用 requestAnimationFrame

```javascript
// 使用 requestAnimationFrame 进行动画
function animate(element, property, from, to, duration) {
  const startTime = performance.now();
  
  function update(currentTime) {
    const elapsed = currentTime - startTime;
    const progress = Math.min(elapsed / duration, 1);
    
    // 缓动函数
    const easeProgress = 1 - Math.pow(1 - progress, 3);
    
    const currentValue = from + (to - from) * easeProgress;
    element.style[property] = currentValue + 'px';
    
    if (progress < 1) {
      requestAnimationFrame(update);
    }
  }
  
  requestAnimationFrame(update);
}
```

---

## 9. 动画工具函数

### 9.1 缓动函数

```javascript
// src/utils/easing.js

export const easing = {
  linear: t => t,
  
  easeIn: t => t * t,
  
  easeOut: t => t * (2 - t),
  
  easeInOut: t => t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t,
  
  easeInCubic: t => t * t * t,
  
  easeOutCubic: t => (--t) * t * t + 1,
  
  easeInOutCubic: t => t < 0.5 ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1,
  
  easeInElastic: t => {
    const c4 = (2 * Math.PI) / 3;
    return t === 0 ? 0 : t === 1 ? 1 : -Math.pow(2, 10 * t - 10) * Math.sin((t * 10 - 10.75) * c4);
  },
  
  easeOutElastic: t => {
    const c4 = (2 * Math.PI) / 3;
    return t === 0 ? 0 : t === 1 ? 1 : Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * c4) + 1;
  },
  
  easeOutBounce: t => {
    const n1 = 7.5625;
    const d1 = 2.75;
    
    if (t < 1 / d1) {
      return n1 * t * t;
    } else if (t < 2 / d1) {
      return n1 * (t -= 1.5 / d1) * t + 0.75;
    } else if (t < 2.5 / d1) {
      return n1 * (t -= 2.25 / d1) * t + 0.9375;
    } else {
      return n1 * (t -= 2.625 / d1) * t + 0.984375;
    }
  }
};
```

---

## 10. 动画最佳实践

### 10.1 动画清单

在添加动画前，问自己：

- [ ] 这个动画有明确的目的吗？
- [ ] 动画时长是否合适？（不要太慢或太快）
- [ ] 缓动函数是否自然？
- [ ] 动画是否会影响性能？
- [ ] 动画是否一致？（相同交互使用相同动画）
- [ ] 是否考虑了无障碍访问？（用户可能关闭动画）

### 10.2 无障碍访问

```css
/* 尊重用户的动画偏好 */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
```
