<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  width: 100%;
  overflow: hidden;
}

#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  height: 100%;
  width: 100%;
}

/* 全局页面切换动画：淡入淡出并轻微位移（适用于各处的 page-fade 过渡） */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
  will-change: opacity, transform;
}
.page-fade-enter {
  opacity: 0;
  transform: translateY(8px);
  filter: blur(1px);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
  filter: blur(1px);
}
</style>
